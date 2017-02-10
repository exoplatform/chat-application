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

package org.exoplatform.chat.services.mongodb;

import org.exoplatform.chat.model.MessageBean;
import org.exoplatform.chat.model.RoomBean;
import org.exoplatform.chat.model.RoomsBean;
import org.exoplatform.chat.model.UserBean;
import org.exoplatform.chat.server.CometdService;
import org.exoplatform.chat.services.*;
import org.exoplatform.chat.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Named("chatService")
@ApplicationScoped
public class ChatServiceImpl implements org.exoplatform.chat.services.ChatService
{
  private static final Logger LOG = Logger.getLogger("ChatService");

  @Inject
  private ChatDataStorage chatStorage;

  @Inject
  private UserService userService;

  @Inject
  private NotificationService notificationService;

  public void write(String message, String user, String room, String isSystem, String dbName)
  {
    chatStorage.write(message, user, room, isSystem, null, dbName);
  }

  public void write(String message, String sender, String room, String isSystem, String options, String dbName, String targetUser)
  {
    String msgId = chatStorage.save(message, sender, room, isSystem, options, dbName);
    MessageBean msg = chatStorage.getMessage(room, msgId, dbName);
    UserBean user = userService.getUser(sender, dbName);
    msg.setFullName(user.getFullname());
    if (!targetUser.startsWith(ChatService.EXTERNAL_PREFIX))
    {
      String content = ((message.length()>30)?message.substring(0,29)+"...":message);
      String intranetPage = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_PORTAL_PAGE);

      List<String> usersToBeNotified = new ArrayList<String>();
      if (targetUser.startsWith(ChatService.SPACE_PREFIX))
      {
        usersToBeNotified = userService.getUsersFilterBy(sender, targetUser.substring(ChatService.SPACE_PREFIX
            .length()), ChatService.TYPE_ROOM_SPACE, dbName);
      }
      else if (targetUser.startsWith(ChatService.TEAM_PREFIX))
      {
        usersToBeNotified = userService.getUsersFilterBy(sender, targetUser.substring(ChatService.TEAM_PREFIX
            .length()), ChatService.TYPE_ROOM_TEAM, dbName);
      }
      else
      {
        usersToBeNotified.add(targetUser);
      }


      String data = new StringBuilder("{\"room\": \"").append(room).append("\",")
          .append("\"messages\": [").append(msg.toJSONString()).append("]}").toString();

      EXoContinuationBayeux bayeux = PortalContainer.getInstance().getComponentInstanceOfType(EXoContinuationBayeux.class);

      // Deliver the saved message to sender's subscribed channel itself.
      if(bayeux.isPresent(sender)) {
        bayeux.sendMessage(sender, CometdService.COMETD_CHANNEL_NAME, data, null);
      }

      for (String receiver: usersToBeNotified) {
        notificationService.addNotification(receiver, sender, "chat", "room", room, content,
            intranetPage + "?room=" + room, options, dbName);

        if(bayeux.isPresent(receiver)) {
          bayeux.sendMessage(receiver, CometdService.COMETD_CHANNEL_NAME, data, null);
        }
      }

      notificationService.setNotificationsAsRead(sender, "chat", "room", room, dbName);
    }
  }

  public String save(String message, String user, String room, String isSystem, String options, String dbName) {
    return chatStorage.save(message, user, room, isSystem, options, dbName);
  }

  public void delete(String room, String user, String messageId, String dbName)
  {
    chatStorage.delete(room, user, messageId, dbName);
  }

  public RoomBean getTeamRoomById(String roomId, String dbName) {
    return chatStorage.getTeamRoomById(roomId, dbName);
  }

  public void deleteTeamRoom(String roomId, String user, String dbName) {
    chatStorage.deleteTeamRoom(roomId, user, dbName);
  }

  public void edit(String room, String user, String messageId, String message, String dbName)
  {
    chatStorage.edit(room, user, messageId, message, dbName);
  }

  public String read(String room, UserService userService, String dbName)
  {
    return read(room, userService, false, null, null, dbName);
  }

  public String read(String room, UserService userService, boolean isTextOnly, Long fromTimestamp, String dbName)
  {
    return read(room, userService, isTextOnly, fromTimestamp, null, dbName);
  }

  public String read(String room, UserService userService, boolean isTextOnly, Long fromTimestamp, Long toTimestamp, String dbName) {
    return chatStorage.read(room, userService, isTextOnly, fromTimestamp, toTimestamp, dbName);
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

  public RoomsBean getRooms(String user, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, NotificationService notificationService, UserService userService, TokenService tokenService, String dbName) {
    return getRooms(user, filter, withUsers, withSpaces, withPublic, withOffline, isAdmin, 0, notificationService, userService, tokenService, dbName);
  }

  public RoomsBean getRooms(String user, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, int limit, NotificationService notificationService, UserService userService, TokenService tokenService, String dbName)
  {
    return chatStorage.getRooms(user, filter, withUsers, withSpaces, withPublic, withOffline, isAdmin, limit, notificationService, userService, tokenService, dbName);
  }

  public int getNumberOfRooms(String dbName)
  {
    return chatStorage.getNumberOfRooms(dbName);
  }

  public int getNumberOfMessages(String dbName)
  {
    return chatStorage.getNumberOfMessages(dbName);
  }
}
