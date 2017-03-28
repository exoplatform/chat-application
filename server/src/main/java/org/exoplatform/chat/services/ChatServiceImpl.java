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

import juzu.Response;
import org.exoplatform.chat.model.*;
import org.exoplatform.chat.utils.PropertyManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.logging.Logger;

@Named("chatService")
@ApplicationScoped
public class ChatServiceImpl implements ChatService
{
  private static final Logger LOG = Logger.getLogger("ChatService");

  @Inject
  private ChatDataStorage chatStorage;

  @Inject
  private UserService userService;

  @Inject
  private NotificationService notificationService;

  @Inject
  private RealTimeMessageService realTimeMessageService;

  public void write(String message, String user, String room, String isSystem, String dbName)
  {
    chatStorage.write(message, user, room, isSystem, null, dbName);
  }

  public void write(String message, String sender, String room, String isSystem, String options, String dbName, String targetUser)
  {
    if (!isMemberOfRoom(sender, room, dbName)) {
      throw new ChatException(403, "Petit malin !");
    }

    if (isSystem == null) isSystem = "false";

    String msgId = chatStorage.save(message, sender, room, isSystem, options, dbName);

    RoomBean roomBean = userService.getRoom(sender, room, dbName);
    String roomType = roomBean.getType();
    if (!ChatService.TYPE_ROOM_EXTERNAL.equals(roomType))
    {
      String intranetPage = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_PORTAL_PAGE);

      List<String> usersToBeNotified = new ArrayList<String>();
      if (ChatService.TYPE_ROOM_USER.equals(roomType)) {
        usersToBeNotified.add(roomBean.getUser());
      } else {
        usersToBeNotified = userService.getUsersFilterBy(sender, room, roomType, dbName);
      }

      MessageBean msg = chatStorage.getMessage(room, msgId, dbName);
      UserBean user = userService.getUser(sender, dbName);
      msg.setFullName(user.getFullname());

      JSONObject data = msg.toJSONObject();
      data.put("roomType", roomType);
      if (ChatService.TYPE_ROOM_USER.equals(roomType)) {
        data.put("roomDisplayName", user.getFullname());
      } else {
        data.put("roomDisplayName", roomBean.getFullName());
      }

      // Deliver the saved message to sender's subscribed channel itself.
      RealTimeMessageBean messageBean = new RealTimeMessageBean(
          RealTimeMessageBean.EventType.MESSAGE_SENT,
          room,
          user.getName(),
          new Date(),
          data);
      realTimeMessageService.sendMessage(messageBean, sender);

      String content = ((message.length() > 30) ? message.substring(0, 29) + "..." : message);
      for (String receiver: usersToBeNotified) {
        notificationService.addNotification(receiver, sender, "chat", "room", room, content,
            intranetPage + "?room=" + room, options, dbName);

        realTimeMessageService.sendMessage(messageBean, receiver);
      }

      notificationService.setNotificationsAsRead(sender, "chat", "room", room, dbName);
    }
  }

  public String save(String message, String user, String room, String isSystem, String options, String dbName) {
    return chatStorage.save(message, user, room, isSystem, options, dbName);
  }

  public void delete(String room, String sender, String messageId, String dbName)
  {
    chatStorage.delete(room, sender, messageId, dbName);

    String roomType = getTypeRoomChat(room, dbName);

    if (!roomType.equals("e")) {
      List<String> usersToBeNotified = new ArrayList<String>();
      if (roomType.equals("s")) {
        usersToBeNotified = userService.getUsersFilterBy(sender, room, ChatService.TYPE_ROOM_SPACE, dbName);
      } else if (roomType.equals("t")) {
        usersToBeNotified = userService.getUsersFilterBy(sender, room, ChatService.TYPE_ROOM_TEAM, dbName);
      } else {
        usersToBeNotified.add(room);
      }


      MessageBean msg = chatStorage.getMessage(room, messageId, dbName);

      // Deliver the saved message to sender's subscribed channel itself.
      RealTimeMessageBean messageBean = new RealTimeMessageBean(
          RealTimeMessageBean.EventType.MESSAGE_DELETED,
          room,
          sender,
          new Date(),
          msg.toJSONObject());
      realTimeMessageService.sendMessage(messageBean, sender);
      realTimeMessageService.sendMessage(messageBean, usersToBeNotified);
    }
  }

  public RoomBean getTeamRoomById(String roomId, String dbName) {
    return chatStorage.getTeamRoomById(roomId, dbName);
  }

  public void deleteTeamRoom(String room, String user, String dbName) {
    List<String> usersToBeNotified = userService.getUsersFilterBy(user, room, ChatService.TYPE_ROOM_TEAM, dbName);

    chatStorage.deleteTeamRoom(room, user, dbName);

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

  public void edit(String room, String sender, String messageId, String message, String dbName)
  {
    chatStorage.edit(room, sender, messageId, message, dbName);

    String roomType = getTypeRoomChat(room, dbName);

    if (!roomType.equals("e")) {
      List<String> usersToBeNotified = new ArrayList<String>();
      if (roomType.equals("s")) {
        usersToBeNotified = userService.getUsersFilterBy(sender, room, ChatService.TYPE_ROOM_SPACE, dbName);
      } else if (roomType.equals("t")) {
        usersToBeNotified = userService.getUsersFilterBy(sender, room, ChatService.TYPE_ROOM_TEAM, dbName);
      } else {
        usersToBeNotified.add(room);
      }


      MessageBean msg = chatStorage.getMessage(room, messageId, dbName);

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

  public String read(String user, String room, String dbName)
  {
    return read(user, room, false, null, null, dbName);
  }

  @Override
  public String read(String user, String room, boolean isTextOnly, Long fromTimestamp, String dbName)
  {
    return read(user, room, isTextOnly, fromTimestamp, null, dbName);
  }

  @Override
  public String read(String user, String room, boolean isTextOnly, Long fromTimestamp, Long toTimestamp, String dbName) {
    // Only members of the room can view the messages
    if (!isMemberOfRoom(user, room, dbName)) {
      throw new ChatException(403, "Petit malin !");
    }

    return chatStorage.read(room, isTextOnly, fromTimestamp, toTimestamp, dbName);
  }

  public MessageBean getMessage(String roomId, String messageId, String dbName) {
    return chatStorage.getMessage(roomId, messageId, dbName);
  }

  public String getSpaceRoom(String space, String dbName)
  {
    return chatStorage.getSpaceRoom(space, dbName);
  }

  public String getSpaceRoomByName(String name, String dbName) {
    return chatStorage.getSpaceRoomByName(name, dbName);
  }

  public String getTeamRoom(String team, String user, String dbName) {
    return chatStorage.getTeamRoom(team, user, dbName);
  }

  public String getExternalRoom(String identifier, String dbName) {
    return chatStorage.getExternalRoom(identifier, dbName);
  }

  public String getTeamCreator(String room, String dbName) {
    return chatStorage.getTeamCreator(room, dbName);
  }

  public void setRoomName(String room, String name, String dbName) {
    chatStorage.setRoomName(room, name, dbName);

    List<String> users = userService.getUsersFilterBy(null, room, ChatService.TYPE_ROOM_TEAM, dbName);
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

  public String getRoom(List<String> users, String dbName)
  {
    return chatStorage.getRoom(users, dbName);
  }

  public String getTypeRoomChat(String roomId, String dbName){
    return chatStorage.getTypeRoomChat(roomId, dbName);
  }

  public List<RoomBean> getExistingRooms(String user, boolean withPublic, boolean isAdmin, NotificationService notificationService, TokenService tokenService, String dbName)
  {
    return chatStorage.getExistingRooms(user, withPublic, isAdmin, notificationService, tokenService, dbName);
  }

  public RoomsBean getRooms(String user, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, NotificationService notificationService, TokenService tokenService, String dbName) {
    return getRooms(user, filter, withUsers, withSpaces, withPublic, withOffline, isAdmin, 0, notificationService, tokenService, dbName);
  }

  public RoomsBean getRooms(String user, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, int limit, NotificationService notificationService, TokenService tokenService, String dbName)
  {
    return chatStorage.getRooms(user, filter, withUsers, withSpaces, withPublic, withOffline, isAdmin, limit, notificationService, tokenService, dbName);
  }

  public int getNumberOfRooms(String dbName)
  {
    return chatStorage.getNumberOfRooms(dbName);
  }

  public int getNumberOfMessages(String dbName)
  {
    return chatStorage.getNumberOfMessages(dbName);
  }


  /**
   * Check if an user is member of a room
   * @param username Username of the user
   * @param roomId Id of the room
   * @param dbName Databse name
   * @return true if the user is member of the room
   */
  private boolean isMemberOfRoom(String username, String roomId, String dbName) {
    List<String> roomMembers;
    RoomBean room = userService.getRoom(username, roomId, dbName);
    if(room == null) {
      LOG.warning("Cannot check if user " + username + " is member of room " + roomId + " since the room does not exist.");
      return false;
    }
    if (room.getType().equals(ChatService.TYPE_ROOM_TEAM)) {
      roomMembers = userService.getUsersFilterBy(null, roomId, ChatService.TYPE_ROOM_TEAM, dbName);
    } else if(room.getType().equals(ChatService.TYPE_ROOM_SPACE)) {
      roomMembers = userService.getUsersFilterBy(null, roomId, ChatService.TYPE_ROOM_SPACE, dbName);
    } else {
      roomMembers = new ArrayList<>();
      List<UserBean> userBeans = userService.getUsersInRoomChatOneToOne(roomId, dbName);
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
