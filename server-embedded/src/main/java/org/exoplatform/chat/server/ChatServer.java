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

package org.exoplatform.chat.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.activation.DataHandler;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import juzu.request.ApplicationContext;
import juzu.request.UserContext;

import org.exoplatform.chat.model.*;
import org.exoplatform.chat.services.*;

import juzu.MimeType;
import juzu.Path;
import juzu.Resource;
import juzu.Response;
import juzu.Route;
import juzu.View;
import juzu.impl.common.Tools;
import juzu.template.Template;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.LocalePolicy;
import org.exoplatform.services.resources.ResourceBundleService;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.exoplatform.chat.listener.GuiceManager;
import org.exoplatform.chat.utils.ChatUtils;
import org.exoplatform.chat.utils.PropertyManager;


@ApplicationScoped
public class ChatServer
{
  private static final String MIME_TYPE_JSON =  "application/json";	
  private static final Logger LOG = Logger.getLogger("ChatServer");

  @Inject
  @Path("index.gtmpl")
  Template index;

  ChatService chatService;
  UserService userService;
  TokenService tokenService;
  NotificationService notificationService;
  RealTimeMessageService realTimeMessageService;

  @Inject
  ChatTools chatTools;

  public ChatServer()
  {
    chatService = GuiceManager.getInstance().getInstance(ChatService.class);
    userService = GuiceManager.getInstance().getInstance(UserService.class);
    tokenService = GuiceManager.getInstance().getInstance(TokenService.class);
    notificationService = GuiceManager.getInstance().getInstance(NotificationService.class);
    realTimeMessageService = GuiceManager.getInstance().getInstance(RealTimeMessageService.class);
  }

  @View
  @Route("/")
  public Response.Content index() throws IOException
  {
    return index.ok();
  }
  
  @Resource
  @Route("/filterOutSilentUsers")
  public Response.Content getFilteredList(String user, String roomId, String token)
  {
	if  (!tokenService.hasUserWithToken(user, token))
	{
	  return Response.notFound("Petit malin !");
	}
	List<String> receivers =  new ArrayList<>();
	List<UserBean> roomParticipants = userService.getUsers(roomId);
	if  (roomParticipants.size() == 0)
		roomParticipants = userService.getUsersInRoomChatOneToOne(roomId);
	for(UserBean roomUser :roomParticipants) {
		if  (!roomUser.getName().equals(user) && !notificationService.isRoomSilentForUser(roomUser.getName(), roomId)) {
			receivers.add(roomUser.getName());
		}
	}
	  return Response.ok(JSONArray.toJSONString(receivers)).withMimeType(MIME_TYPE_JSON ).withHeader
	            ("Cache-Control", "no-cache").withCharset(Tools.UTF_8);
  }
  
  @Resource
  @Route("/userRooms")
  public Response.Content getUserRooms(String user, String onlineUsers, String token, String filter, String offset, String limit, String roomType)
  {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }
    int limitValue = 20;
    try {
      if (limit != null && !"".equals(limit)) {
        limitValue = Integer.parseInt(limit);
      }
    } catch (NumberFormatException nfe) {
      LOG.info("limit is not a valid Integer number");
    }

    int offsetValue = 0;
    try {
      if (StringUtils.isNotBlank(offset)) {
        offsetValue = Integer.parseInt(offset);
      }
    } catch (NumberFormatException nfe) {
      LOG.info("offset is not a valid Integer number");
    }

    List<String> limitUsers = Arrays.asList(onlineUsers.split(","));

    RoomsBean roomsBean = chatService.getUserRooms(user, limitUsers, filter, offsetValue, limitValue,
            notificationService, tokenService, roomType);


    return Response.ok(roomsBean.roomsToJSON()).withMimeType(MIME_TYPE_JSON ).withHeader
            ("Cache-Control", "no-cache").withCharset(Tools.UTF_8);
  }
@Resource
  @Route("/whoIsOnline")
  public Response.Content whoIsOnline(String user, String onlineUsers, String token, String filter, String limit)
  {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }
    int ilimit = 20;
    try {
      if (limit != null && !"".equals(limit)) {
        ilimit = Integer.parseInt(limit);
      }
    } catch (NumberFormatException nfe) {
      LOG.info("limit is not a valid Integer number");
    }

    List<String> limitUsers = Arrays.asList(onlineUsers.split(","));
    RoomsBean roomsBean = chatService.getRooms(user, limitUsers, filter, true, true, true, true, false, ilimit,
            notificationService, tokenService);

    return Response.ok(roomsBean.roomsToJSON()).withMimeType(MIME_TYPE_JSON ).withHeader
            ("Cache-Control", "no-cache").withCharset(Tools.UTF_8);
  }

  @Resource
  @Route("/updateUser")
  public Response.Content updateUser(String user, String token, String targetUser, String isDeleted, String isEnabled, String isExternal)
  {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }
    userService.setExternalUser(targetUser, isExternal);
    if (Boolean.valueOf(isDeleted)) {
      userService.deleteUser(targetUser);
    } else {
      userService.setEnabledUser(targetUser, Boolean.valueOf(isEnabled));
    }
    
    return Response.ok("Updated!");
  }

  @Resource
  @Route("/setExternal")
  public Response.Content setExternal(String user, String targetUser, String token, String isExternal)
  {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }

    userService.setExternalUser(targetUser, isExternal);

    return Response.ok("Updated!");
  }

  @Resource
  @Route("/send")
  public Response.Content send(String sender, String token, String message, String room,
                               String isSystem, String options) throws IOException {
    if (!tokenService.hasUserWithToken(sender, token))
    {
      return Response.notFound("Petit malin !");
    }

    if (message != null) {
      try {
        message = URLDecoder.decode(message, "UTF-8");
        options = URLDecoder.decode(options, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        // Chat server cannot do anything in this case
        // Get original value
      }

      try {
        chatService.write(null, message, sender, room, isSystem, options);
      } catch (ChatException e) {
        return Response.content(e.getStatus(), e.getMessage());
      }
    }

    return Response.ok("ok").withMimeType("application/json; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/read")
  public Response.Content read(String user, String token, String room, String fromTimestamp, String toTimestamp,
                               String isTextOnly, String limit) throws IOException {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }

    Long from = null;
    Long to = null;
    try {
      if (fromTimestamp != null && !fromTimestamp.isEmpty()) {
        from = Long.parseLong(fromTimestamp);
      }
      if (toTimestamp != null && !toTimestamp.isEmpty()) {
        to = Long.parseLong(toTimestamp);
      }
    } catch (NumberFormatException nfe) {
      LOG.info("fromTimestamp is not a valid Long number");
    }

    Integer ilimit = 0;
    try {
      if (limit != null && !"".equals(limit)) {
        ilimit = Integer.parseInt(limit);
      }
    } catch (NumberFormatException nfe) {
      LOG.info("limit is not a valid Integer number");
    }

    String data = null;
    try {
      data = chatService.read(user, room, "true".equals(isTextOnly), from, to, ilimit);
      notificationService.setNotificationsAsRead(user, "chat", "room", room);
    } catch (Exception e) {
      if (e instanceof ChatException) {
        return Response.content(((ChatException)e).getStatus(), e.getMessage());
      } else {
        return Response.content(500, e.getMessage());
      }
    }

    return Response.ok(data).withMimeType(MIME_TYPE_JSON ).withHeader("Cache-Control", "no-cache")
                   .withCharset(Tools.UTF_8);
  }

  @Resource
  @Route("/sendMeetingNotes")
  public Response.Content sendMeetingNotes(String user, String token, String room, String fromTimestamp,
                                           String toTimestamp, ApplicationContext applicationContext, UserContext userContext) throws IOException {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }

    Long from = null;
    Long to = null;

    try {
      if (fromTimestamp!=null && !"".equals(fromTimestamp))
        from = Long.parseLong(fromTimestamp);
    } catch (NumberFormatException nfe) {
      LOG.info("fromTimestamp is not a valid Long number");
    }
    try {
      if (toTimestamp!=null && !"".equals(toTimestamp))
        to = Long.parseLong(toTimestamp);
    } catch (NumberFormatException nfe) {
      LOG.info("fromTimestamp is not a valid Long number");
    }
    String data = chatService.read(user, room, false, from, to, 0);
    BasicDBObject datao = (BasicDBObject)JSON.parse(data);
    String roomType = chatService.getTypeRoomChat(room);
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    String date = formatter.format(new GregorianCalendar().getTime());
    String title = "";
    String roomName = "";
    
    List<UserBean> users;
    Locale locale = userContext.getLocale();
    ResourceBundle res = applicationContext.resolveBundle(locale);

    if (datao.containsField("messages")) {
      if (ChatService.TYPE_ROOM_USER.equalsIgnoreCase(roomType)) {
        users = userService.getUsersInRoomChatOneToOne(room)
                           .stream()
                           .filter(UserBean::isEnabledUser)
                           .collect(Collectors.toList());
        title = res.getString("exoplatform.chat.meetingnotes") + " [" + date + "]";
      } else {
        users = userService.getUsers(room).stream().filter(UserBean::isEnabledUser).collect(Collectors.toList());
        List<SpaceBean> spaces = userService.getSpaces(user);
        for (SpaceBean spaceBean:spaces)
        {
          if (room.equals(spaceBean.getRoom()))
          {
            roomName = spaceBean.getDisplayName();
          }
        }
        List<RoomBean> roomBeans = userService.getTeams(user);
        for (RoomBean roomBean:roomBeans)
        {
          if (room.equals(roomBean.getRoom()))
          {
            roomName = roomBean.getFullName();
          }
        }
        title = roomName+" : "+ res.getString("exoplatform.chat.meetingnotes") + " ["+date+"]";
      }
      ReportBean reportBean = new ReportBean(res);
      reportBean.fill((BasicDBList) datao.get("messages"), users);

      ArrayList<String> tos = new ArrayList<String>();
      String senderName = user;
      String senderMail = "";
      for (UserBean userBean:users)
      {
        if (!"".equals(userBean.getEmail()))
        {
          tos.add(userBean.getEmail());
        }
        if (user.equals(userBean.getName()))
        {
          senderName = userBean.getFullname();
          senderMail = userBean.getEmail();
        }
      }
      String serverBase = ChatUtils.getServerBase();
      String html = reportBean.getAsHtml(title, serverBase, locale);

      // inline images
      String prevUser = "";
      int index = 0;
      Map<String, String> inlineImages = new HashMap<String, String>();
      for(MessageBean messageBean : reportBean.getMessages()) {
        if(!messageBean.getUser().equals(prevUser)) {
          String keyAvatar = messageBean.getUser() + index;
          String valueAvatar = serverBase + ChatService.USER_AVATAR_URL.replace("{}", messageBean.getUser());
          inlineImages.put(keyAvatar,valueAvatar);
          index ++;
        }
        prevUser = messageBean.getUser();
      }

      try {
        sendMailWithAuth(senderName,senderMail , tos, html, title, inlineImages);
      } catch (Exception e) {
        LOG.info(e.getMessage());
      }

    }

    return Response.ok("sent").withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/getMeetingNotes")
  public Response.Content getMeetingNotes(String user, String token, String room, String fromTimestamp,
                                          String toTimestamp, String portalURI, ApplicationContext applicationContext, UserContext userContext) throws IOException {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }

    Long from = null;
    Long to = null;
    String wikiPageContent = "";
    String roomName = "";
    List<UserBean> users = new ArrayList<UserBean>();
    JSONObject jsonObject = new JSONObject();
    try {
      if (fromTimestamp!=null && !"".equals(fromTimestamp))
        from = Long.parseLong(fromTimestamp);
    } catch (NumberFormatException nfe) {
      LOG.info("fromTimestamp is not a valid Long number");
    }
    try {
      if (toTimestamp!=null && !"".equals(toTimestamp))
        to = Long.parseLong(toTimestamp);
    } catch (NumberFormatException nfe) {
      LOG.info("fromTimestamp is not a valid Long number");
    }
    String data = chatService.read(user, room, false, from, to, 0);
    String typeRoom = chatService.getTypeRoomChat(room);
    BasicDBObject datao = (BasicDBObject)JSON.parse(data);
    if (datao.containsField("messages")) {
      if(ChatService.TYPE_ROOM_USER.equalsIgnoreCase(typeRoom)) {
        users = userService.getUsersInRoomChatOneToOne(room)
                           .stream()
                           .filter(UserBean::isEnabledUser)
                           .collect(Collectors.toList());
      }
      else {
        users = userService.getUsers(room).stream().filter(UserBean::isEnabledUser).collect(Collectors.toList());
        List<SpaceBean> spaces = userService.getSpaces(user);
        for (SpaceBean spaceBean:spaces)
        {
          if (room.equals(spaceBean.getRoom()))
          {
            roomName = spaceBean.getDisplayName();
          }
        }
        List<RoomBean> roomBeans = userService.getTeams(user);
        for (RoomBean roomBean:roomBeans)
        {
          if (room.equals(roomBean.getRoom()))
          {
            roomName = roomBean.getFullName();
          }
        }
      }
      Locale locale = userContext.getLocale();
      ResourceBundle res = applicationContext.resolveBundle(locale);

      ReportBean reportBean = new ReportBean(res);

      reportBean.fill((BasicDBList) datao.get("messages"), users);
      ArrayList<String> usersInGroup = new ArrayList<String>();
      String serverBase = ChatUtils.getServerBase();
      wikiPageContent = reportBean.getWikiPageContent(serverBase, portalURI);
      try {
        for (UserBean userBean : users) {
          if (!"".equals(userBean.getName())) {
            usersInGroup.add(userBean.getName());
          }
        }
        jsonObject.put("users", usersInGroup);
        jsonObject.put("wikiPageContent", wikiPageContent);
        jsonObject.put("typeRoom", typeRoom);
      } catch (Exception e) {
        LOG.warning(e.getMessage());
        return Response.notFound("No Room yet");
      }
    }

    return Response.ok(jsonObject.toString()).withMimeType(MIME_TYPE_JSON ).withHeader("Cache-Control", "no-cache")
                   .withCharset(Tools.UTF_8);
  }

  @Resource
  @MimeType("text/plain")
  @Route("/delete")
  public Response.Content delete(String user, String token, String room, String messageId) throws IOException
  {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }

    // Only author of the message can delete it
    MessageBean message = chatService.getMessage(room, messageId);
    if (message == null || !message.getUser().equals(user)) {
      return Response.notFound("");
    }

    try
    {
      chatService.delete(room, user, messageId);
    }
    catch (Exception e)
    {
      return Response.notFound("Oups");
    }
    return Response.ok("Updated!");

  }

  @Resource
  @MimeType("text/plain")
  @Route("/deleteTeamRoom")
  public Response.Content deleteTeamRoom(String user, String token, String room) throws IOException {
    if (!tokenService.hasUserWithToken(user, token)) {
      return Response.notFound("Petit malin !");
    }
    try {
      String creator = chatService.getTeamCreator(room);
      if (!creator.equals(user)) {
        return Response.notFound("");
      } else {
        chatService.deleteTeamRoom(room, user);
      }
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Impossible to delete Team Room [" + room + "] : " + e.getMessage(), e);
      return Response.content(500, "Oups!");
    }
    return Response.ok("Updated!");
  }

  @Resource
  @MimeType("text/plain")
  @Route("/edit")
  public Response.Content edit(String user, String token, String room, String messageId, String message) throws IOException
  {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }

    // Only author of the message can edit it
    MessageBean currentMessage = chatService.getMessage(room, messageId);
    if (currentMessage == null || !currentMessage.getUser().equals(user)) {
      return Response.notFound("");
    }

    try
    {
      try {
        message = URLDecoder.decode(message,"UTF-8");
      } catch (UnsupportedEncodingException e) {
        // Chat server cannot do anything in this case
        // Get original value
      }
      chatService.edit(room, user, messageId, message);
    }
    catch (Exception e)
    {
      return Response.notFound("Oups");
    }
    return Response.ok("Updated!");

  }

  @Resource
  @MimeType("text/plain")
  @Route("/toggleFavorite")
  public Response.Content toggleFavorite(String user, String token, String targetUser, String favorite)
  {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }
    if (Boolean.valueOf(favorite)) {
      userService.addFavorite(user, targetUser);
    } else {
      userService.removeFavorite(user, targetUser);
    }
    return Response.ok("Updated!");
  }
  
  @Resource
  @MimeType("text/plain")
  @Route("/isFavorite")
  public Response.Content isFavorite(String user, String token, String targetUser) {
    if (!tokenService.hasUserWithToken(user, token)) {
      return Response.notFound("Petit malin !");
    }
    boolean isFavorite = false;
    try {
      isFavorite = userService.isFavorite(user, targetUser);
    } catch (Exception e) {
      LOG.log(java.util.logging.Level.WARNING, e.getMessage());
      return Response.notFound("Oups");
    }
    return Response.ok(String.valueOf(isFavorite));
  }

  @Resource
  @MimeType(MIME_TYPE_JSON )
  @Route("/getUserDesktopNotificationSettings")
  public Response.Content getUserDesktopNotificationSettings(String user, String token) throws JSONException {
    if (!tokenService.hasUserWithToken(user, token)) {
      return Response.notFound("Something is wrong.");
    }

    JSONObject response = new JSONObject();
    JSONObject res =  userService.getUserDesktopNotificationSettings(user).toJSON();
    if(res==null || res.isEmpty()){
      response.put("done",false);
    } else {
      response.put("done",true);
    }
    response.put("userDesktopNotificationSettings",res);

    return Response.ok(response.toString()).withHeader("Cache-Control", "no-cache")
            .withCharset(Tools.UTF_8);
  }

  @Resource
  @MimeType("text/plain")
  @Route("/setPreferredNotification")
  public Response.Content setPreferredNotification(String user, String token, String notifManner) throws JSONException {
    if (!tokenService.hasUserWithToken(user, token)) {
      return Response.notFound("Something is wrong.");
    }

    JSONObject response = new JSONObject();
    try {
      userService.setPreferredNotification(user, notifManner);
      response.put("done",true);
    } catch (Exception e) {
      response.put("done",false);
    }

    return Response.ok(response.toString()).withMimeType(MIME_TYPE_JSON ).withHeader("Cache-Control", "no-cache")
            .withCharset(Tools.UTF_8);
  }

  @Resource
  @MimeType("text/plain")
  @Route("/setRoomNotificationTrigger")
  public Response.Content setRoomNotificationTrigger(String user, String token, String room, String notifCondition,String notifConditionType, Long time) throws JSONException {
    if (!tokenService.hasUserWithToken(user, token)) {
      return Response.notFound("Something is wrong.");
    }

    try{
      userService.setRoomNotificationTrigger(user, room, notifCondition, notifConditionType, time);
      Map<String, Object> data = new HashMap<>();

      data.put("notificationTrigger", notifConditionType);
      data.put("targetRoom", room);

      // Deliver the saved message to sender's subscribed channel itself.
      RealTimeMessageBean messageBean = new RealTimeMessageBean(
              RealTimeMessageBean.EventType.ROOM_NOTIFICATION_SETTINGS_UPDATED,
              null,
              user,
              null,
              data);
      realTimeMessageService.sendMessage(messageBean, user);
    } catch(Exception e) {
    }

    return getUserDesktopNotificationSettings(user, token);
  }

  @Resource
  @MimeType("text/plain")
  @Route("/setNotificationSettings")
  public Response.Content setNotificationSettings(String user, String token, String room, String[] notifConditions, String[] notifManners, Long time) throws JSONException {
    if (!tokenService.hasUserWithToken(user, token)) {
      return Response.notFound("Something is wrong.");
    }
    
    if(notifConditions != null) {
      for (String notifCondition : notifConditions) {
        try {
          userService.setPreferredNotification(user, notifCondition);
        } catch (Exception e) {
        }
      }
    }
    if(notifManners != null) {
      for (String notifManner : notifManners) {
        try {
          userService.setNotificationTrigger(user, notifManner);
        } catch (Exception e) {
        }
      }
    }

    return getUserDesktopNotificationSettings(user, token);
  }

  @Resource
  @MimeType("text/plain")
  @Route("/setNotificationTrigger")
  public Response.Content setNotificationTrigger(String user, String token, String notifCondition) throws JSONException {
    if (!tokenService.hasUserWithToken(user, token)) {
      return Response.notFound("Something is wrong.");
    }

    try {
      userService.setNotificationTrigger(user, notifCondition);
    } catch (Exception e) {
    }

    return getUserDesktopNotificationSettings(user, token);
  }

  @Resource
  @Route("/getRoom")
  public Response.Content getRoom(String user, String token, String targetUser, String isAdmin, String withDetail,
                                  String type) {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }
    String room = targetUser;
    RoomBean roomBean = null;
    try
    {
      if (type != null) {
        if ("room-id".equals(type)) {
          room = targetUser;
        } else if ("space-name".equals(type)) {
          room = chatService.getSpaceRoomByName(targetUser);
        } else if ("space-id".equals(type)) {
          room = ChatUtils.getRoomId(targetUser);
        } else if ("username".equals(type)) {
          List<String> users = new ArrayList<String>();
          users.add(user);
          users.add(targetUser);
          room = chatService.getRoom(users);
        } else if ("external".equals(type)) {
          room = chatService.getExternalRoom(targetUser);
        }
      } else if (targetUser.startsWith(ChatService.SPACE_PREFIX)) {
        room = chatService.getSpaceRoom(targetUser);

      } else if (targetUser.startsWith(ChatService.TEAM_PREFIX)) {
        room = chatService.getTeamRoom(targetUser, user);

      } else if (targetUser.startsWith(ChatService.EXTERNAL_PREFIX)) {
        room = chatService.getExternalRoom(targetUser);
      } else {
        ArrayList<String> users = new ArrayList<String>(2);
        users.add(user);
        users.add(targetUser);
        room = chatService.getRoom(users);
      }

      if ("true".equals(withDetail)) {
        roomBean = userService.getRoom(user, room);
      }
    }
    catch (Exception e)
    {
      LOG.log(java.util.logging.Level.WARNING, e.getMessage());
      return Response.notFound("No Room yet");
    }
    String out = room;
    if (roomBean!=null)
    {
      out = roomBean.toJSON();
    }

    return Response.ok(out).withMimeType(MIME_TYPE_JSON ).withCharset(Tools.UTF_8).withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/saveTeamRoom")
  public Response.Content saveTeamRoom(String user, String token, String teamName, String room, String users)
  {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }

    try {
      teamName = URLDecoder.decode(teamName,"UTF-8");
      teamName = teamName.trim();
    } catch (UnsupportedEncodingException e) {
      LOG.info("Cannot decode message: " + teamName);
    }

    JSONObject jsonObject = new JSONObject();
    try
    {
      if("".equals(teamName)) return Response.content(400, "Data is invalid!");

      // Room creation
      if (room==null || "".equals(room) || "---".equals(room))
      {
        List<RoomBean> roomBeans =  chatService.getTeamRoomsByName(teamName);
        if (roomBeans != null && !roomBeans.isEmpty()) {
          for (RoomBean roomBean : roomBeans) {
            if (user.equals(roomBean.getUser())) {
              return Response.content(400, "roomAlreadyExists.creator");
            } else {
              List<String> existingUsers = userService.getUsersFilterBy(null, roomBean.getRoom(), ChatService.TYPE_ROOM_TEAM);
              if (existingUsers != null && existingUsers.contains(user)) {
                return Response.content(400, "roomAlreadyExists.notCreator");
              }
            }
          }
        }
        room = chatService.getTeamRoom(teamName, user);
      }
      // Only creator can edit members or set team name
      String creator = chatService.getTeamCreator(room);
      if (!user.equals(creator)) {
        return Response.notFound("Petit malin !");
      }

      List<String> usersToNotifyForAdd = new JSONArray();
      List<String> usersToAdd = new JSONArray();

      if (users != null && !users.isEmpty()) {
        List<String> existingUsers = userService.getUsersFilterBy(null, room, ChatService.TYPE_ROOM_TEAM);

        List<String> usersNew = Arrays.asList(users.split(","));

        usersToAdd.addAll(usersNew);
        List<String> usersToRemove = new JSONArray();

        for (String u: existingUsers) {
          if (usersNew.contains(u)) {
            usersToNotifyForAdd.add(u);
            usersToAdd.remove(u);
          } else {
            usersToRemove.add(u);
          }
        }

        if (usersToRemove.size() > 0) {
          userService.removeTeamUsers(room, usersToRemove);

          StringBuilder sbUsers = new StringBuilder();
          boolean first = true;
          for (String u: usersToRemove)
          {
            if (!first) sbUsers.append("; ");
            UserBean userBean = userService.getUser(u);
            String fullName = userBean.isExternal() != null && userBean.isExternal().equals("true") ? userService.getUserFullName(u) + " (" + getResourceBundleLabel(new Locale(getCurrentUserLanguage(user)), "external.label.tag") + ")" : userService.getUserFullName(u);
            sbUsers.append(fullName);
            first = false;
            notificationService.setNotificationsAsRead(u, "chat", "room", room);
          }

          // Send members removal message in the room
          String removeTeamUserOptions
              = "{\"type\":\"type-remove-team-user\",\"users\":\"" + sbUsers + "\", " +
              "\"fullname\":\"" + userService.getUserFullName(user) + "\"}";
          this.send(user, token, StringUtils.EMPTY, room, "true", removeTeamUserOptions);
        }

        chatService.setRoomName(room, teamName);

        if (usersToAdd.size() > 0) {
          userService.addTeamUsers(room, usersToAdd);

          StringBuilder sbUsers = new StringBuilder();
          boolean first = true;
          for (String usert: usersToAdd)
          {
            if(usert.equals(creator)) {
              continue;
            }
            if (!first) sbUsers.append("; ");
            UserBean userBean = userService.getUser(usert);
            String fullName = userBean.isExternal() != null && userBean.isExternal().equals("true") ? userService.getUserFullName(usert) + " (" + getResourceBundleLabel(new Locale(getCurrentUserLanguage(user)), "external.label.tag") + ")" : userService.getUserFullName(usert);
            sbUsers.append(fullName);
            first = false;
          }
          String addTeamUserOptions
              = "{\"type\":\"type-add-team-user\",\"users\":\"" + sbUsers + "\", " +
              "\"fullname\":\"" + userService.getUserFullName(user) + "\"}";
          this.send(user, token, StringUtils.EMPTY, room, "true", addTeamUserOptions);
        }

      }

      RoomBean roomBean = userService.getRoom(user, room);
      JSONObject data = roomBean.toJSONObject();
      data.put("title", teamName);
      data.put("participants", usersToAdd);

      RealTimeMessageBean updatedRoomMessage = new RealTimeMessageBean(
          RealTimeMessageBean.EventType.ROOM_UPDATED,
          room,
          user,
          null,
          data);

      if (!usersToNotifyForAdd.contains(creator)) {
        usersToNotifyForAdd.add(creator);
      }
      realTimeMessageService.sendMessage(updatedRoomMessage, usersToNotifyForAdd);

      jsonObject.putAll(data);
      jsonObject.put("name", teamName);
      jsonObject.put("room", room);
    }
    catch (Exception e)
    {
      LOG.log(Level.WARNING, "Error while saving room " + teamName, e);
      return Response.notFound("No Room yet");
    }
    return Response.ok(jsonObject.toString()).withMimeType(MIME_TYPE_JSON ).withHeader("Cache-Control", "no-cache")
                   .withCharset(Tools.UTF_8);
  }

  @Resource
  @Route("/updateRoomMeetingStatus")
  public Response.Content updateRoomMeetingStatus(String user, String token, String start, String room, String startTime) {
    if (!tokenService.hasUserWithToken(user, token)) {
      return Response.notFound("Petit malin !");
    }
  
    //only member of a room can updateRoomMeetingStatus
    if (chatService.isMemberOfRoom(user,room)) {
      chatService.setRoomMeetingStatus(room, Boolean.parseBoolean(start), startTime);
      return Response.ok("Updated.");
    } else {
      return Response.notFound("");
    }
  }

  @Resource
  @Route("/isRoomEnabled")
  public Response.Content isRoomEnabled(String user, String token, String spaceId) {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }

    String room = ChatUtils.getRoomId(spaceId);
    Boolean isEnabled = chatService.isRoomEnabled(room);

    return Response.ok(isEnabled.toString());
  }

  @Resource
  @Route("/updateRoomEnabled")
  public Response.Content setRoomEnabled(String user, String token, String spaceId, Boolean enabled) {
    if (!tokenService.hasUserWithToken(user, token)) {
      return Response.notFound("Petit malin !");
    }

    String room = ChatUtils.getRoomId(spaceId);
    chatService.setRoomEnabled(room, enabled);

    return Response.ok("Updated.");
  }

  @Resource
  @MimeType("text/plain")
  @Route("/updateUnreadMessages")
  public Response.Content updateUnreadMessages(String room, String user, String token)
  {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }
    try
    {
      notificationService.setNotificationsAsRead(user, "chat", "room", room);
      if (userService.isAdmin(user))
      {
        notificationService.setNotificationsAsRead(UserService.SUPPORT_USER, "chat", "room", room);
      }

    }
    catch (Exception e)
    {
      LOG.warning(e.getMessage());
      return Response.notFound("Server Not Available yet");
    }
    return Response.ok("Updated.");
  }

  @Resource
  @Route("/notification")
  public Response.Content notification(String user, String token, String event, String withDetails) throws IOException
  {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }

    boolean detailed = Boolean.valueOf(withDetails);
    int totalUnread = 0;
    List<NotificationBean> notifications = null;
    if (!detailed)
    {
      // GETTING TOTAL NOTIFICATION WITHOUT DETAILS
      List<NotificationBean> notificationBeans = notificationService.getUnreadNotifications(user, userService);
      totalUnread = notificationBeans.size();
      if (userService.isAdmin(user)) {
        totalUnread += notificationService.getUnreadNotificationsTotal(UserService.SUPPORT_USER);
      }
    } else {
      // GETTING ALL NOTIFICATION DETAILS
      notifications = notificationService.getUnreadNotifications(user, userService);
      totalUnread = notifications.size();
    }


    String data;
    if (event!=null && event.equals("1")) {
      data = "id: "+totalUnread+"\n";
      data += "data: {\"total\": "+totalUnread+"}\n\n";
    } else {
      JSONObject json = new JSONObject();
      json.put("total", totalUnread);
      if (detailed && notifications != null) {
        JSONArray notifies = new JSONArray();
        for (NotificationBean o: notifications) {
          notifies.add(o.toJSONObject());
        }

        json.put("notifications", notifies);
      }
      data = json.toJSONString();
    }

    return Response.ok(data).withMimeType(MIME_TYPE_JSON ).withCharset(Tools.UTF_8).withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @MimeType("text/plain")
  @Route("/getStatus")
  public Response.Content getStatus(String user, String token, String targetUser)
  {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }
    String status = UserService.STATUS_INVISIBLE;
    try
    {
      if (targetUser != null) {
        status = userService.getStatus(targetUser);
      }
    }
    catch (Exception e)
    {
      LOG.warning(e.getMessage());
      return Response.notFound(status);
    }
    return Response.ok(status).withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @MimeType("text/plain")
  @Route("/setStatus")
  public Response.Content setStatus(String user, String token, String status)
  {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }
    try
    {
      userService.setStatus(user, status);
    }
    catch (Exception e)
    {
      LOG.warning(e.getMessage());
      return Response.notFound("No Status for this User");
    }
    return Response.ok(status);
  }

  @Resource
  @MimeType("text/plain")
  @Route("/getCreator")
  public Response.Content getCreator(String user, String token, String room)
  {
    String creator = "";
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }
    try
    {
      creator = chatService.getTeamCreator(room);
    }
    catch (Exception e)
    {
      LOG.warning(e.getMessage());
      return Response.notFound("No Status for this User");
    }
    return Response.ok(creator);
  }

  @Resource
  @Route("/users")
  public Response.Content getUsers(String user, String token, String onlineUsers, String room, String filter, String limit, String onlineOnly)
  {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }

    boolean showOnlyOnlineUsers = StringUtils.isNotBlank(onlineOnly) && "true".equals(onlineOnly);
    int limit_ = 0;
    try {
      if (limit != null) {
        limit_ = Integer.parseInt(limit);
      }
    } catch (NumberFormatException e) {
      return Response.status(400).content("The 'limit' parameter value is invalid");
    }

    List<String> onlineUserList = StringUtils.isNotBlank(onlineUsers) ? Arrays.asList(onlineUsers.split(",")) : null;
    List<UserBean> users = userService.getUsers(room, onlineUserList, filter, limit_, showOnlyOnlineUsers);
    if(StringUtils.isNotBlank(user)) {
      UserBean currentUser = userService.getUser(user);
      users.remove(currentUser);
    }
    UsersBean usersBean = new UsersBean();
    usersBean.setUsers(users);
    return Response.ok(usersBean.usersToJSON()).withMimeType(MIME_TYPE_JSON ).withHeader
            ("Cache-Control", "no-cache").withCharset(Tools.UTF_8);
  }

  @Resource
  @Route("/usersCount")
  public Response.Content getUsersCount(String user, String token, String room, String filter)
  {
    if (!tokenService.hasUserWithToken(user, token))
    {
      return Response.notFound("Petit malin !");
    }

    StringBuffer data = new StringBuffer();
    data.append("{");
    data.append(" \"usersCount\": ").append(userService.getUsersCount(room, filter)).append(",");
    data.append(" \"activeUsersCount\": ").append(userService.getActiveUsersCount(room, filter));
    data.append("}");

    return Response.ok(data).withMimeType(MIME_TYPE_JSON ).withHeader
            ("Cache-Control", "no-cache").withCharset(Tools.UTF_8);
  }

  @Resource
  @Route("/statistics")
  public Response.Content getStatistics()
  {
    StringBuffer data = new StringBuffer();
    data.append("{");
    data.append(" \"users\": "+userService.getNumberOfUsers()+", ");
    data.append(" \"rooms\": "+chatService.getNumberOfRooms()+", ");
    data.append(" \"messages\": "+ chatService.getNumberOfMessages()+", ");
    data.append(" \"notifications\": "+notificationService.getNumberOfNotifications()+", ");
    data.append(" \"notificationsUnread\": "+notificationService.getNumberOfUnreadNotifications());
    data.append("}");

    return Response.ok(data.toString()).withMimeType(MIME_TYPE_JSON ).withCharset(Tools.UTF_8).withHeader("Cache-Control", "no-cache");
  }

  private Session getMailSession() {
    String protocal = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_PROTOCAL);
    String host = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_HOST);
    final String user = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_USER);
    final String password = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_PASSWORD);
    String port = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_PORT);
    String auth = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_AUTH);
    String starttlsEnable = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_STARTTLS_ENABLE);
    String enableSSL = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_ENABLE_SSL_ENABLE);
    String smtpAuth = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_AUTH);
    String socketFactoryPort = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_SOCKET_FACTORY_PORT);
    String socketFactoryClass = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_SOCKET_FACTORY_CLASS);
    String socketFactoryFallback = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_SOCKET_FACTORY_FALLBACK);
    
    Properties props = new Properties();

    // SMTP protocol properties
    props.put("mail.transport.protocol",protocal);
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", port);
    props.put("mail.smtp.auth", auth);

    if (Boolean.parseBoolean(smtpAuth)) {
      props.put("mail.smtp.socketFactory.port",socketFactoryPort);
      props.put("mail.smtp.socketFactory.class",socketFactoryClass);
      props.put("mail.smtp.socketFactory.fallback",socketFactoryFallback);
      props.put("mail.smtp.starttls.enable",starttlsEnable);
      props.put("mail.smtp.ssl.enable",enableSSL);
      return Session.getInstance(props, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(user, password);
        }
      });
    } else {
      return Session.getInstance(props);
    }
  }
  
  public void sendMailWithAuth(String senderFullname, String senderMail, List<String> toList, String htmlBody, String subject, Map<String, String> inlineImages) throws Exception {

    Session session = getMailSession();
    
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(senderMail, senderFullname, "UTF-8"));

    // To get the array of addresses
    for (String to: toList) {
      message.addRecipients(Message.RecipientType.BCC, InternetAddress.parse(to));
    }
    
    message.setSubject(subject, "UTF-8");
    // use a MimeMultipart as we need to handle the file attachments
    Multipart multipart = new MimeMultipart();
    // Create a message part to represent the body text
    MimeBodyPart messageBodyPart = new MimeBodyPart();
    messageBodyPart.setContent(htmlBody, "text/html; charset=UTF-8");
    // add the message body to the mime message
    multipart.addBodyPart(messageBodyPart);
    
    // Part2: get user's avatar
    
    if (inlineImages != null && inlineImages.size() > 0) {
      Set<String> setImageID = inlineImages.keySet();
      for (String contentId : setImageID) {
        messageBodyPart = new MimeBodyPart();
        String imageFilePath = inlineImages.get(contentId);
        URL url = new URL(imageFilePath);
        URLConnection con = url.openConnection();
        con.setDoOutput(true);
        InputStream is = con.getInputStream();
        ByteArrayDataSource byteArrayDataSource = new ByteArrayDataSource(is, con.getContentType());
        messageBodyPart.setDataHandler(new DataHandler(byteArrayDataSource));
        messageBodyPart.setContentID("<" + contentId + ">");
        messageBodyPart.setDisposition(MimeBodyPart.INLINE);
        multipart.addBodyPart(messageBodyPart);
      }
    }
    // Put all message parts in the message
    message.setContent(multipart);
    
    try {
      Transport.send(message);
    } catch(Exception e){
      LOG.info(e.getMessage());
    }
  }

  /**
   * Get the ressource bundle label.
   *
   * @return the ressource bundle label
   */
  public static String getResourceBundleLabel(Locale locale, String label) {
    ResourceBundleService resourceBundleService =  ExoContainerContext.getService(ResourceBundleService.class);
    return resourceBundleService.getResourceBundle(resourceBundleService.getSharedResourceBundleNames(), locale).getString(label);
  }

  /**
   * Gets platform language of current user. In case of any errors return null.
   *
   * @return the platform language
   */
  public static String getCurrentUserLanguage(String userId) {
    LocaleContextInfo localeCtx = LocaleContextInfoUtils.buildLocaleContextInfo(userId);
    LocalePolicy localePolicy = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(LocalePolicy.class);
    String lang = Locale.getDefault().getLanguage();
    if(localePolicy != null) {
      Locale locale = localePolicy.determineLocale(localeCtx);
      lang = locale.toString();
    }
    return lang;
  }
}
