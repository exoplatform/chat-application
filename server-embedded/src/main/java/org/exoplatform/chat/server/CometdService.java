package org.exoplatform.chat.server;

import org.apache.commons.lang3.StringUtils;
import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.exoplatform.chat.listener.GuiceManager;
import org.exoplatform.chat.model.MessageBean;
import org.exoplatform.chat.model.RealTimeMessageBean;
import org.exoplatform.chat.services.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.exoplatform.chat.services.CometdMessageServiceImpl.COMETD_CHANNEL_NAME;

/**
 * This service is used to receive all Cometd messages and then publish the messages to the right clients.
 * A Cometd service channel is used so the messages sent by clients on this channel are only sent to the server (so
 * by this service) and not to all the subscribed clients. It is up to the server to forward the messages to
 * the right clients.
 */
@Service
public class CometdService {

  private static final Logger LOG = Logger.getLogger(CometdService.class.getName());

  UserService userService;
  NotificationService notificationService;
  RealTimeMessageService realTimeMessageService;
  TokenService tokenService;

  public CometdService() {
    userService = GuiceManager.getInstance().getInstance(UserService.class);
    notificationService = GuiceManager.getInstance().getInstance(NotificationService.class);
    realTimeMessageService = GuiceManager.getInstance().getInstance(RealTimeMessageService.class);
    tokenService = GuiceManager.getInstance().getInstance(TokenService.class);
  }

  /**
   * This method receives all the websocket messages sent to the channel COMETD_CHANNEL_NAME
   * and process them accordingly to their type
   * @param remoteSession
   * @param message
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Listener(COMETD_CHANNEL_NAME)
  public void onMessageReceived(final ServerSession remoteSession, final ServerMessage message) {

    try {
      JSONParser jsonParser = new JSONParser();
      JSONObject jsonMessage = (JSONObject) jsonParser.parse((String) message.getData());
      LOG.log(Level.FINE, "Cometd message received on {0} : {1}", new Object[] { COMETD_CHANNEL_NAME, jsonMessage.toString() });

      String sender = (String) jsonMessage.get("sender");
      String token = (String) jsonMessage.get("token");
      if (!tokenService.hasUserWithToken(sender, token))
      {
        return;
      }

      RealTimeMessageBean.EventType eventType = RealTimeMessageBean.EventType.get((String) jsonMessage.get("event"));

      ChatService chatService = GuiceManager.getInstance().getInstance(ChatService.class);

      if (eventType.equals(RealTimeMessageBean.EventType.USER_STATUS_CHANGED)) {
        // forward the status change to all connected users
        RealTimeMessageBean realTimeMessageBean = new RealTimeMessageBean(
                RealTimeMessageBean.EventType.USER_STATUS_CHANGED,
                (String) jsonMessage.get("room"),
                (String) jsonMessage.get("sender"),
                new Date(),
                (Map) jsonMessage.get("data"));
        realTimeMessageService.sendMessageToAll(realTimeMessageBean);

        // update data
        userService.setStatus((String) jsonMessage.get("sender"),
                (String) ((JSONObject) jsonMessage.get("data")).get("status"));
      } else if (eventType.equals(RealTimeMessageBean.EventType.MESSAGE_READ)) {
        String room = (String) jsonMessage.get("room");

        String category = "room";
        if (StringUtils.isBlank(room)) {
          room = null;
          category = null;
        }

        notificationService.setNotificationsAsRead(sender, "chat", category, room);
        if (userService.isAdmin(sender))
        {
          notificationService.setNotificationsAsRead(UserService.SUPPORT_USER, "chat", category, room);
        }

        // send real time message to all others clients of the same user
        RealTimeMessageBean realTimeMessageBean = new RealTimeMessageBean(RealTimeMessageBean.EventType.MESSAGE_READ, room, sender, new Date(), null);
        realTimeMessageService.sendMessage(realTimeMessageBean, sender);
      } else if (eventType.equals(RealTimeMessageBean.EventType.MESSAGE_SENT)) {

        JSONObject data = (JSONObject) jsonMessage.get("data");
        String room = data.get("room").toString();
        String clientId = data.get("clientId").toString();
        String msg = data.get("msg") != null ? data.get("msg").toString() : "";
        String isSystem = data.get("isSystem") == null ? "false" : data.get("isSystem").toString();
        String options = data.get("options") != null ? data.get("options").toString() : null;

        try {
          chatService.write(clientId, msg, sender, room, isSystem, options);
        } catch (ChatException e) {
          // Should response a message somehow in websocket.
        }
      } else if (eventType.equals(RealTimeMessageBean.EventType.MESSAGE_UPDATED)) {
        String room = jsonMessage.get("room").toString();
        String messageId = ((JSONObject)jsonMessage.get("data")).get("msgId").toString();
        // Only author of the message can edit it
        MessageBean currentMessage = chatService.getMessage(room, messageId);
        if (currentMessage == null || !currentMessage.getUser().equals(sender)) {
          return;
        }

        String msg = ((JSONObject)jsonMessage.get("data")).get("msg").toString();
        chatService.edit(room, sender, messageId, msg);
      } else if (eventType.equals(RealTimeMessageBean.EventType.MESSAGE_DELETED)) {
        String room = jsonMessage.get("room").toString();
        String messageId = ((JSONObject)jsonMessage.get("data")).get("msgId").toString();

        // Only author of the message can delete it
        MessageBean currentMessage = chatService.getMessage(room, messageId);
        if (currentMessage == null || !currentMessage.getUser().equals(sender)) {
          return;
        }

        chatService.delete(room, sender, messageId);
      } else if (eventType.equals(RealTimeMessageBean.EventType.ROOM_DELETED)) {
        String room = jsonMessage.get("room").toString();

        chatService.deleteTeamRoom(room, sender);
      } else if (eventType.equals(RealTimeMessageBean.EventType.ROOM_MEMBER_LEAVE_REQUESTED)) {
        String room = jsonMessage.get("room").toString();
        String clientId = jsonMessage.get("clientId").toString();
        String msg = "";
        String isSystem = "true";
        JSONObject options = jsonMessage.get("options") != null ? (JSONObject) jsonMessage.get("options") : new JSONObject();
        options.put("type", RealTimeMessageBean.EventType.ROOM_MEMBER_LEFT.toString());

        try {
          chatService.write(clientId, msg, sender, room, isSystem, options.toString());
        } catch (ChatException e) {
          // Should response a message somehow in websocket.
        }

        userService.removeTeamUsers(room, Collections.singletonList(sender));

        List<String> usersToBeNotified = userService.getUsersFilterBy(sender, room, ChatService.TEAM_PREFIX);
        if (usersToBeNotified == null) {
          usersToBeNotified = Collections.singletonList(sender);
        } else {
          usersToBeNotified.add(sender);
        }

        // Send a websocket message of type 'room-member-left' to all the room members
        RealTimeMessageBean leaveRoomMessage = new RealTimeMessageBean(
            RealTimeMessageBean.EventType.ROOM_MEMBER_LEFT,
            room,
            sender,
            new Date(),
            options);
        realTimeMessageService.sendMessage(leaveRoomMessage, usersToBeNotified);
        notificationService.setNotificationsAsRead(sender, "chat", "room", room);
      }
    } catch (ParseException e) {
      LOG.log(Level.SEVERE, "Error while processing Cometd message : " + e.getMessage(), e);
    }
  }

}
