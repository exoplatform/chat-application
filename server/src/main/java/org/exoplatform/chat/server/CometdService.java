package org.exoplatform.chat.server;

import juzu.Response;
import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.exoplatform.chat.listener.GuiceManager;
import org.exoplatform.chat.model.MessageBean;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.NotificationService;
import org.exoplatform.chat.services.UserService;
import org.exoplatform.container.PortalContainer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;
import org.exoplatform.chat.listener.GuiceManager;
import org.exoplatform.chat.services.UserService;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

/**
 * This service is used to receive all Cometd messages and then publish the messages to the right clients.
 * A Cometd service channel is used so the messages sent by clients on this channel are only sent to the server (so
 * by this service) and not to all the subscribed clients. It is up to the server to forward the messages to
 * the right clients.
 */
@Service
public class CometdService {

  public static final String COMETD_CHANNEL_NAME = "/service/chat";
  UserService userService;
  NotificationService notificationService;

  @Inject
  private BayeuxServer bayeuxServer;

  public CometdService() {
    userService = GuiceManager.getInstance().getInstance(UserService.class);
    notificationService = GuiceManager.getInstance().getInstance(NotificationService.class);
  }

  @Listener(COMETD_CHANNEL_NAME)
  public void onMessageReceived(final ServerSession remoteSession, final ServerMessage message) {
    System.out.println(">>>>>>>>> message received on " + COMETD_CHANNEL_NAME + " : " + message.getJSON());

    //TODO need to verify authorization of the sender.

    try {
      EXoContinuationBayeux bayeux = PortalContainer.getInstance().getComponentInstanceOfType(EXoContinuationBayeux.class);

      JSONParser jsonParser = new JSONParser();
      JSONObject jsonMessage = (JSONObject) jsonParser.parse((String) message.getData());

      String event = (String) jsonMessage.get("event");

      //TODO read each message data and send it each room member. It requires to use 'deliver' instead of 'publish'
      //to avoid broadcasting the message to all connected clients (even the ones not members of the target room)

      ChatService chatService = GuiceManager.getInstance().getInstance(ChatService.class);

      if (event.equals("user-status-changed")) {
        // forward the status change to all connected users
        bayeux.getSessions().stream().forEach(s -> s.deliver(s, (ServerMessage.Mutable) message));

        // update data
        userService.setStatus((String) jsonMessage.get("room"),
                (String) ((JSONObject) jsonMessage.get("data")).get("status"),
                (String) jsonMessage.get("dbName"));
      } else if (event.equals("message-read")) {
        String room = (String) jsonMessage.get("room");
        String sender = (String) jsonMessage.get("sender");
        String dbName = (String) jsonMessage.get("dbName");

        notificationService.setNotificationsAsRead(sender, "chat", "room", room, dbName);
        if (userService.isAdmin(sender, dbName))
        {
          notificationService.setNotificationsAsRead(UserService.SUPPORT_USER, "chat", "room", room, dbName);
        }

        JSONObject data = new JSONObject();
        data.put("event", "message-read");
        data.put("room", room);

        if (bayeux.isPresent(sender)) {
          bayeux.sendMessage(sender, CometdService.COMETD_CHANNEL_NAME, data, null);
        }
      } else if (event.equals("message-sent")) {
        // TODO store message in db
        String room = (String) jsonMessage.get("room");
        String isSystem = jsonMessage.get("isSystem").toString();
        String dbName = (String) jsonMessage.get("dbName");
        String options = jsonMessage.get("options").toString();
        String sender = (String) jsonMessage.get("sender");
        String msg = (String) ((JSONObject)jsonMessage.get("data")).get("msg");
        String targetUser = (String) jsonMessage.get("targetUser");

        chatService.write(msg, sender, room, isSystem, options, dbName, targetUser);
      } else if (event.equals("message-updated")) {
        String room = jsonMessage.get("room").toString();
        String messageId = ((JSONObject)jsonMessage.get("data")).get("msgId").toString();
        String sender = jsonMessage.get("sender").toString();
        String dbName = jsonMessage.get("dbName").toString();
        // Only author of the message can edit it
        MessageBean currentMessage = chatService.getMessage(room, messageId, dbName);
        if (currentMessage == null || !currentMessage.getUser().equals(sender)) {
          return;
        }

        String msg = ((JSONObject)jsonMessage.get("data")).get("msg").toString();
        chatService.edit(room, sender, messageId, msg, dbName);
      } else if (event.equals("message-deleted")) {
        String room = jsonMessage.get("room").toString();
        String messageId = ((JSONObject)jsonMessage.get("data")).get("msgId").toString();
        String sender = jsonMessage.get("sender").toString();
        String dbName = jsonMessage.get("dbName").toString();

        // Only author of the message can delete it
        MessageBean currentMessage = chatService.getMessage(room, messageId, dbName);
        if (currentMessage == null || !currentMessage.getUser().equals(sender)) {
          return;
        }

        chatService.delete(room, sender, messageId, dbName);
      } else if (event.equals("favorite-added")) {
        String sender = jsonMessage.get("sender").toString();
        String targetUser = jsonMessage.get("targetUser").toString();
        String dbName = jsonMessage.get("dbName").toString();
        userService.addFavorite(sender, targetUser, dbName);
      } else if (event.equals("favorite-removed")) {
        String sender = jsonMessage.get("sender").toString();
        String targetUser = jsonMessage.get("targetUser").toString();
        String dbName = jsonMessage.get("dbName").toString();
        userService.removeFavorite(sender, targetUser, dbName);
      } else if (event.equals("room-deleted")) {
        String room = jsonMessage.get("room").toString();
        String sender = jsonMessage.get("sender").toString();
        String dbName = jsonMessage.get("dbName").toString();

        chatService.deleteTeamRoom(room, sender, dbName);
      }
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

}