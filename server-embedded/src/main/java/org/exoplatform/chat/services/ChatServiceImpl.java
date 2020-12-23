/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.chat.services;

import org.exoplatform.chat.model.*;
import org.exoplatform.chat.utils.PropertyManager;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Named("chatService")
@ApplicationScoped
@Singleton
public class ChatServiceImpl implements ChatService
{
  private static final Logger LOG = Logger.getLogger("ChatService");

  private static final Pattern TAG_HREF_REGEX = Pattern.compile("<a\\s+[^>]*href=(['\"])(.*?)\\1[^>]*>", Pattern.DOTALL);

  @Inject
  private ChatDataStorage chatStorage;

  @Inject
  private UserService userService;

  @Inject
  private NotificationService notificationService;

  @Inject
  private RealTimeMessageService realTimeMessageService;

  public void write(String message, String user, String room, String isSystem)
  {
    write(null, message, user, room, isSystem, null);
  }

  @SuppressWarnings("unchecked")
  public void write(String clientId, String message, String sender, String room, String isSystem, String options)
  {
    if (!isMemberOfRoom(sender, room)) {
      throw new ChatException(403, "Petit malin !");
    }

    if (isSystem == null) isSystem = "false";

    String msgId = chatStorage.save(message, sender, room, isSystem, options);
    List<UserBean> participants = userService.getUsers(room);
    List<String> mentionedUsers = new ArrayList<>();
    if (message.indexOf("@") > -1) {
      List<String> userNames = new ArrayList<>();
      final Matcher matcher = TAG_HREF_REGEX.matcher(message);
      while (matcher.find()) {
        String href = matcher.group(2);
        userNames.add(href.substring(href.lastIndexOf("/") + 1));
      }
      if (userNames.size() > 0) {
        for (String username : userNames) {
          UserBean userBean = participants.stream()
                  .filter(user -> username.equals(user.getName()))
                  .findAny()
                  .orElse(null);
          mentionedUsers.add(userBean.getName());
        }
      }
    }

    RoomBean roomBean = userService.getRoom(sender, room);
    String roomType = roomBean.getType();
    if (!ChatService.TYPE_ROOM_EXTERNAL.equals(roomType))
    {
      List<String> usersToBeNotified = null;
      if (ChatService.TYPE_ROOM_USER.equals(roomType)) {
        usersToBeNotified = new ArrayList<>();//Collections.singletonList(sender);
        usersToBeNotified.add(roomBean.getUser());
      } else {
        usersToBeNotified = userService.getUsersFilterBy(sender, room, roomType);
      }

      MessageBean msg = chatStorage.getMessage(room, msgId);
      UserBean user = userService.getUser(sender);
      msg.setFullName(user.getFullname());
      if(user.isExternal() != null && user.isExternal().equals("true") ){
        msg.setExternal(user.isExternal());
      }

      if (mentionedUsers.size() > 0) {
        JSONObject type = new JSONObject();
        type.put("type" ,"type-mention");
        JSONObject mentionData = msg.toJSONObject();
        mentionData.put("clientId", clientId);
        mentionData.put("roomType", roomType);
        mentionData.put("room", room);
        mentionData.put("options", type);
        mentionData.put("roomDisplayName", roomBean.getFullName());
        RealTimeMessageBean mentionMessage = new RealTimeMessageBean(
                RealTimeMessageBean.EventType.MESSAGE_SENT,
                room,
                user.getName(),
                new Date(),
                mentionData);
        realTimeMessageService.sendMessage(mentionMessage, mentionedUsers);
      }
      JSONObject data = msg.toJSONObject();
      data.put("clientId", clientId);
      data.put("roomType", roomType);
      data.put("room", room);
      if (ChatService.TYPE_ROOM_USER.equals(roomType)) {
        data.put("roomDisplayName", user.getFullname());
      } else {
        data.put("roomDisplayName", roomBean.getFullName());
      }

      // Deliver the saved message to sender.
      RealTimeMessageBean messageBean = new RealTimeMessageBean(
          RealTimeMessageBean.EventType.MESSAGE_READ,
          room,
          user.getName(),
          new Date(),
          data);
      realTimeMessageService.sendMessage(messageBean, sender);

      // Deliver the saved message to sender's subscribed channel itself.
      messageBean = new RealTimeMessageBean(
          RealTimeMessageBean.EventType.MESSAGE_SENT,
          room,
          user.getName(),
          new Date(),
          data);
      realTimeMessageService.sendMessage(messageBean, usersToBeNotified);

      String intranetPage = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_PORTAL_PAGE);
      String content = ((message.length() > 30) ? message.substring(0, 29) + "..." : message);
      for (String receiver: usersToBeNotified) {
        if (!StringUtils.equals(receiver, sender)) {
          notificationService.addNotification(receiver, sender, "chat", "room", room, content,
                                              intranetPage + "?room=" + room, options);
        }
      }
      notificationService.setNotificationsAsRead(sender, "chat", "room", room);
    }
  }

  public String save(String message, String user, String room, String isSystem, String options) {
    return chatStorage.save(message, user, room, isSystem, options);
  }

  public void delete(String room, String sender, String messageId)
  {
    chatStorage.delete(room, sender, messageId);

    String roomType = getTypeRoomChat(room);

    if (!roomType.equals("e")) {
      Set<String> usersToBeNotified = new HashSet<>();
      if (roomType.equals("s")) {
        usersToBeNotified = new HashSet<>(userService.getUsersFilterBy(sender, room, ChatService.TYPE_ROOM_SPACE));
      } else if (roomType.equals("t")) {
        usersToBeNotified = new HashSet<>(userService.getUsersFilterBy(sender, room, ChatService.TYPE_ROOM_TEAM));
      } else {
        UserBean userBean = userService.getUser(room);
        String username = userBean.getName();
        if (username == null) {
          List<UserBean> users = userService.getUsersInRoomChatOneToOne(room)
                                            .stream()
                                            .filter(user -> user.isEnabled())
                                            .collect(Collectors.toList());
          for (UserBean targetUserBean : users) {
            usersToBeNotified.add(targetUserBean.getName());
          }
        }
      }
      usersToBeNotified.add(sender);

      MessageBean msg = chatStorage.getMessage(room, messageId);

      // Deliver the saved message to sender's subscribed channel itself.
      RealTimeMessageBean messageBean = new RealTimeMessageBean(
          RealTimeMessageBean.EventType.MESSAGE_DELETED,
          room,
          sender,
          new Date(),
          msg.toJSONObject());
      realTimeMessageService.sendMessage(messageBean, new ArrayList<>(usersToBeNotified));
    }
  }

  public RoomBean getTeamRoomById(String roomId) {
    return chatStorage.getTeamRoomById(roomId);
  }

  @Override
  public List<RoomBean> getTeamRoomsByName(String teamName) {
    return chatStorage.getTeamRoomByName(teamName);
  }

  public void deleteTeamRoom(String room, String user) {
    List<String> usersToBeNotified = userService.getUsersFilterBy(user, room, ChatService.TYPE_ROOM_TEAM);

    chatStorage.deleteTeamRoom(room, user);

    // Deliver the saved message to sender's subscribed channel itself.
    RealTimeMessageBean messageBean = new RealTimeMessageBean(
            RealTimeMessageBean.EventType.ROOM_DELETED,
            room,
            user,
            new Date(),
            null);
    realTimeMessageService.sendMessage(messageBean, user);
    realTimeMessageService.sendMessage(messageBean, usersToBeNotified);
  }

  public void edit(String room, String sender, String messageId, String message)
  {
    chatStorage.edit(room, sender, messageId, message);

    RoomBean roomBean = userService.getRoom(sender, room);
    String roomType = roomBean.getType();

    if (!ChatService.TYPE_ROOM_EXTERNAL.equals(roomType)) {
      List<String> usersToBeNotified = new ArrayList<String>();
      if (ChatService.TYPE_ROOM_USER.equals(roomType)) {
        usersToBeNotified.add(roomBean.getUser());
      } else {
        usersToBeNotified = userService.getUsersFilterBy(sender, room, roomType);
      }

      MessageBean msg = chatStorage.getMessage(room, messageId);
      UserBean user = userService.getUser(sender);
      msg.setFullName(user.getFullname());

      // Deliver the saved message to sender's subscribed channel itself.
      RealTimeMessageBean messageBean = new RealTimeMessageBean(
          RealTimeMessageBean.EventType.MESSAGE_UPDATED,
          room,
          sender,
          new Date(),
          msg.toJSONObject());
      realTimeMessageService.sendMessage(messageBean, sender);
      realTimeMessageService.sendMessage(messageBean, usersToBeNotified);
    }
  }

  public String read(String user, String room)
  {
    return read(user, room, false, null, null, 0);
  }

  @Override
  public String read(String user, String room, boolean isTextOnly, Long fromTimestamp)
  {
    return read(user, room, isTextOnly, fromTimestamp, null, 0);
  }

  @Override
  public String read(String user, String room, boolean isTextOnly, Long fromTimestamp, Long toTimestamp, int limit) {
    // Only members of the room can view the messages
    if (!isMemberOfRoom(user, room)) {
      throw new ChatException(403, "Petit malin !");
    }

    return chatStorage.read(room, isTextOnly, fromTimestamp, toTimestamp, limit);
  }

  public MessageBean getMessage(String roomId, String messageId) {
    return chatStorage.getMessage(roomId, messageId);
  }

  public String getSpaceRoom(String space)
  {
    return chatStorage.getSpaceRoom(space);
  }

  public String getSpaceRoomByName(String name) {
    return chatStorage.getSpaceRoomByName(name);
  }

  public String getTeamRoom(String team, String user) {
    return chatStorage.getTeamRoom(team, user);
  }

  public String getExternalRoom(String identifier) {
    return chatStorage.getExternalRoom(identifier);
  }

  public String getTeamCreator(String room) {
    return chatStorage.getTeamCreator(room);
  }

  public void setRoomName(String room, String name) {
    chatStorage.setRoomName(room, name);

    List<String> users = userService.getUsersFilterBy(null, room, ChatService.TYPE_ROOM_TEAM);
    JSONObject data = new JSONObject();
    data.put("title", name);
    JSONArray array = new JSONArray();
    array.addAll(users);
    data.put("members", array);
    RealTimeMessageBean updatedRoomMessage = new RealTimeMessageBean(
        RealTimeMessageBean.EventType.ROOM_UPDATED,
        room,
        null,
        null,
        data);
    for (String u: users) {
      realTimeMessageService.sendMessage(updatedRoomMessage, u);
    }
  }

  @Override
  public boolean isRoomEnabled(String room) {
    return chatStorage.isRoomEnabled(room);
  }

  @Override
  public void setRoomEnabled(String room, boolean enabled) {
    chatStorage.setRoomEnabled(room, enabled);
    // set all room Notifications as read
    List<UserBean> userBeans = userService.getUsers(room);
    userBeans.stream().forEach(userBean -> notificationService.setNotificationsAsRead(userBean.getName(), "chat", "room", room));
  }

  @Override
  public void setRoomMeetingStatus(String room, boolean start, String startTime) {
    chatStorage.setRoomMeetingStatus(room, start, startTime);
  }

  public String getRoom(List<String> users)
  {
    return chatStorage.getRoom(users);
  }

  public String getTypeRoomChat(String roomId){
    return chatStorage.getTypeRoomChat(roomId);
  }

  public List<RoomBean> getExistingRooms(String user, boolean withPublic, boolean isAdmin, NotificationService notificationService, TokenService tokenService)
  {
    return chatStorage.getExistingRooms(user, withPublic, isAdmin, notificationService, tokenService);
  }

  public RoomsBean getRooms(String user, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, NotificationService notificationService, TokenService tokenService) {
    return getRooms(user, new ArrayList<>(), filter, withUsers, withSpaces, withPublic, withOffline, isAdmin, 0, notificationService, tokenService);
  }

  public RoomsBean getRooms(String user, List<String> onlineUsers, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, int limit, NotificationService notificationService, TokenService tokenService)
  {
    return chatStorage.getRooms(user, onlineUsers, filter, withUsers, withSpaces, withPublic, withOffline, isAdmin, limit, notificationService, tokenService);
  }

  public int getNumberOfRooms()
  {
    return chatStorage.getNumberOfRooms();
  }

  public int getNumberOfMessages()
  {
    return chatStorage.getNumberOfMessages();
  }


  /**
   * Check if an user is member of a room
   * @param username Username of the user
   * @param roomId Id of the room
   * @return true if the user is member of the room
   */
  public boolean isMemberOfRoom(String username, String roomId) {
    List<String> roomMembers;
    RoomBean room = userService.getRoom(username, roomId);
    if(room == null) {
      LOG.warning("Cannot check if user " + username + " is member of room " + roomId + " since the room does not exist.");
      return false;
    }
    if (room.getType().equals(ChatService.TYPE_ROOM_TEAM)) {
      roomMembers = userService.getUsersFilterBy(null, roomId, ChatService.TYPE_ROOM_TEAM);
    } else if(room.getType().equals(ChatService.TYPE_ROOM_SPACE)) {
      roomMembers = userService.getUsersFilterBy(null, roomId, ChatService.TYPE_ROOM_SPACE);
    } else {
      roomMembers = new ArrayList<>();
      List<UserBean> userBeans = userService.getUsersInRoomChatOneToOne(roomId)
                                            .stream()
                                            .filter(UserBean::isEnabledUser)
                                            .collect(Collectors.toList());
      if(userBeans != null) {
        for (UserBean userBean : userBeans) {
          roomMembers.add(userBean.getName());
        }
      }
    }
    if (roomMembers == null || !roomMembers.contains(username)) {
      return false;
    }
    return true;
  }
}
