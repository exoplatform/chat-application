package org.exoplatform.chat.server;

import juzu.Response;
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
import org.exoplatform.chat.services.UserService;

import java.util.Date;
import java.util.Map;
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
  @Listener(COMETD_CHANNEL_NAME)
  public void onMessageReceived(final ServerSession remoteSession, final ServerMessage message) {
    LOG.log(Level.FINE, "Cometd message received on {0} : {1}", new Object[]{COMETD_CHANNEL_NAME, message.getJSON()});

    try {
      JSONParser jsonParser = new JSONParser();
      JSONObject jsonMessage = (JSONObject) jsonParser.parse((String) message.getData());

      String sender = (String) jsonMessage.get("sender");
      String token = (String) jsonMessage.get("token");
      String dbName = (String) jsonMessage.get("dbName");
      if (!tokenService.hasUserWithToken(sender, token, dbName))
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
        userService.setStatus((String) jsonMessage.get("room"),
                (String) ((JSONObject) jsonMessage.get("data")).get("status"),
                (String) jsonMessage.get("dbName"));
      } else if (eventType.equals(RealTimeMessageBean.EventType.MESSAGE_READ)) {
        String room = (String) jsonMessage.get("room");

        notificationService.setNotificationsAsRead(sender, "chat", "room", room, dbName);
        if (userService.isAdmin(sender, dbName))
        {
          notificationService.setNotificationsAsRead(UserService.SUPPORT_USER, "chat", "room", room, dbName);
        }

        // send real time message to all others clients of the same user
        RealTimeMessageBean realTimeMessageBean = new RealTimeMessageBean(RealTimeMessageBean.EventType.MESSAGE_READ, room, sender, new Date(), null);
        realTimeMessageService.sendMessage(realTimeMessageBean, sender);
      } else if (eventType.equals(RealTimeMessageBean.EventType.MESSAGE_SENT)) {
        String room = (String) jsonMessage.get("room");
        String isSystem = jsonMessage.get("isSystem").toString();
        String options = jsonMessage.get("options").toString();
        String msg = (String) ((JSONObject)jsonMessage.get("data")).get("msg");
        String targetUser = (String) jsonMessage.get("targetUser");

        try {
          chatService.write(msg, sender, room, isSystem, options, dbName, targetUser);
        } catch (ChatException e) {
          // Should response a message somehow in websocket.
        }
      } else if (eventType.equals(RealTimeMessageBean.EventType.MESSAGE_UPDATED)) {
        String room = jsonMessage.get("room").toString();
        String messageId = ((JSONObject)jsonMessage.get("data")).get("msgId").toString();
        // Only author of the message can edit it
        MessageBean currentMessage = chatService.getMessage(room, messageId, dbName);
        if (currentMessage == null || !currentMessage.getUser().equals(sender)) {
          return;
        }

        String msg = ((JSONObject)jsonMessage.get("data")).get("msg").toString();
        chatService.edit(room, sender, messageId, msg, dbName);
      } else if (eventType.equals(RealTimeMessageBean.EventType.MESSAGE_DELETED)) {
        String room = jsonMessage.get("room").toString();
        String messageId = ((JSONObject)jsonMessage.get("data")).get("msgId").toString();

        // Only author of the message can delete it
        MessageBean currentMessage = chatService.getMessage(room, messageId, dbName);
        if (currentMessage == null || !currentMessage.getUser().equals(sender)) {
          return;
        }

        chatService.delete(room, sender, messageId, dbName);
      } else if (eventType.equals(RealTimeMessageBean.EventType.ROOM_DELETED)) {
        String room = jsonMessage.get("room").toString();

        chatService.deleteTeamRoom(room, sender, dbName);
      }
    } catch (ParseException e) {
      LOG.log(Level.SEVERE, "Error while processing Cometd message : " + e.getMessage(), e);
    }
  }

}