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

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import juzu.MimeType;
import juzu.Path;
import juzu.Resource;
import juzu.Response;
import juzu.Route;
import juzu.View;
import juzu.impl.common.Tools;
import juzu.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.chat.listener.GuiceManager;
import org.exoplatform.chat.model.NotificationBean;
import org.exoplatform.chat.model.ReportBean;
import org.exoplatform.chat.model.RoomBean;
import org.exoplatform.chat.model.RoomsBean;
import org.exoplatform.chat.model.SpaceBean;
import org.exoplatform.chat.model.UserBean;
import org.exoplatform.chat.model.UsersBean;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.NotificationService;
import org.exoplatform.chat.services.TokenService;
import org.exoplatform.chat.services.UserService;
import org.exoplatform.chat.utils.ChatUtils;
import org.exoplatform.chat.utils.PropertyManager;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserProfile;
import org.json.JSONObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

@ApplicationScoped
public class ChatServer
{
  private static final Logger LOG = Logger.getLogger("ChatServer");

  @Inject
  @Path("index.gtmpl")
  Template index;
  @Inject
  @Path("users.gtmpl")
  Template users;
  ChatService chatService;
  UserService userService;
  TokenService tokenService;
  NotificationService notificationService;
  @Inject
  ChatTools chatTools;

  public ChatServer()
  {
    chatService = GuiceManager.getInstance().getInstance(ChatService.class);
    userService = GuiceManager.getInstance().getInstance(UserService.class);
    tokenService = GuiceManager.getInstance().getInstance(TokenService.class);
    notificationService = GuiceManager.getInstance().getInstance(NotificationService.class);
  }

  @View
  @Route("/")
  public Response.Content index() throws IOException
  {
    return index.ok();
  }

  @Resource
  @Route("/whoIsOnline")
  public Response.Content whoIsOnline(String user, String token, String filter, String isAdmin, String limit)
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }
    Integer ilimit = 0;
    try {
      if (limit!=null && !"".equals(limit))
        ilimit = Integer.parseInt(limit);
    } catch (NumberFormatException nfe) {
      LOG.info("limit is not a valid Integer number");
    }

    RoomsBean roomsBean = chatService.getRooms(user, filter, true, true, false, true, "true".equals(isAdmin), ilimit,
            notificationService, userService, tokenService);
    return Response.ok(roomsBean.roomsToJSON()).withMimeType("application/json").withHeader
            ("Cache-Control", "no-cache").withCharset(Tools.UTF_8);
  }

  @Resource
  @Route("/send")
  public Response.Content send(String user, String token, String targetUser, String message, String room,
                               String isSystem, String options) throws IOException {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }

    try
    {
      if (message!=null)
      {
        // Only member can send chat message in team
        if (targetUser.startsWith(ChatService.TEAM_PREFIX)) {
          List<String> roomMembers = userService.getUsersFilterBy(null, room, ChatService.TYPE_ROOM_TEAM);
          if (!roomMembers.contains(user)) {
            return Response.content(403, "Petit malin !");
          }
        }        
        try {
          message = URLDecoder.decode(message,"UTF-8");
          options = URLDecoder.decode(options,"UTF-8");
        } catch (UnsupportedEncodingException e) {
          // Chat server cannot do anything in this case
          // Get original value
        }
        if (isSystem==null) isSystem="false";
        chatService.write(message, user, room, isSystem, options);
        if (!targetUser.startsWith(ChatService.EXTERNAL_PREFIX))
        {
          String content = ((message.length()>30)?message.substring(0,29)+"...":message);
          String intranetPage = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_PORTAL_PAGE);


          if (targetUser.startsWith(ChatService.SPACE_PREFIX))
          {
            List<String> users = userService.getUsersFilterBy(user, targetUser.substring(ChatService.SPACE_PREFIX
                    .length()), ChatService.TYPE_ROOM_SPACE);
            for (String tuser:users)
            {
              notificationService.addNotification(tuser, user, "chat", "room", room, content,
                      intranetPage + "?room=" + room, options);
            }
          }
          else if (targetUser.startsWith(ChatService.TEAM_PREFIX))
          {
            List<String> users = userService.getUsersFilterBy(user, targetUser.substring(ChatService.TEAM_PREFIX
                    .length()), ChatService.TYPE_ROOM_TEAM);
            for (String tuser:users)
            {
              notificationService.addNotification(tuser, user, "chat", "room", room, content,
                      intranetPage + "?room=" + room, options);
            }
          }
          else
          {
            notificationService.addNotification(targetUser, user, "chat", "room", room, content,
                    intranetPage + "?room=" + room, options);
          }

          notificationService.setNotificationsAsRead(user, "chat", "room", room);
        }
      }

    }
    catch (Exception e)
    {
      return Response.notFound("Problem on Chat server. Please, try later").withMimeType("text/event-stream");
    }

    return Response.ok("ok").withMimeType("application/json; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/read")
  public Response.Content read(String user, String token, String room, String fromTimestamp,
                               String isTextOnly) throws IOException {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }

    Long from = null;
    try {
      if (fromTimestamp!=null && !"".equals(fromTimestamp))
        from = Long.parseLong(fromTimestamp);
    } catch (NumberFormatException nfe) {
      LOG.info("fromTimestamp is not a valid Long number");
    }

    // Only member can view chat message in a team
    if (userService.getRoom(user, room).isTeam()) {
      List<String> roomMembers = userService.getUsersFilterBy(null, room, ChatService.TYPE_ROOM_TEAM);
      if (!roomMembers.contains(user)) {
        return Response.content(403, "Petit malin !");
      }
    }

    String data = chatService.read(room, userService, "true".equals(isTextOnly), from);

    return Response.ok(data).withMimeType("application/json").withHeader("Cache-Control", "no-cache")
                   .withCharset(Tools.UTF_8);
  }

  @Resource
  @Route("/sendMeetingNotes")
  public Response.Content sendMeetingNotes(String user, String token, String room, String fromTimestamp,
                                           String toTimestamp) throws IOException {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }

    Long from = null;
    Long to = null;
    String html = "";
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
    String data = chatService.read(room, userService, false, from, to);
    BasicDBObject datao = (BasicDBObject)JSON.parse(data);
    String roomType = chatService.getTypeRoomChat(room);
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    String date = formatter.format(new GregorianCalendar().getTime());
    String title = "";
    String roomName = "";
    
    List<UserBean> users = new ArrayList<UserBean>();
    if (datao.containsField("messages")) {
      if (ChatService.TYPE_ROOM_USER.equalsIgnoreCase(roomType)) {
        users = userService.getUsersInRoomChatOneToOne(room);
        title = "Meeting Notes ["+date+"]";
      } else {
        users = userService.getUsers(room);
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
            roomName = roomBean.getFullname();
          }
        }
        title = roomName+" : Meeting Notes ["+date+"]";
      }
      ReportBean reportBean = new ReportBean();

      reportBean.fill((BasicDBList) datao.get("messages"), users);

      ArrayList<String> tos = new ArrayList<String>();
      String senderFullname = user;
      for (UserBean userBean:users)
      {
        if (!"".equals(userBean.getEmail()))
        {
          tos.add(userBean.getEmail());
        }
        if (user.equals(userBean.getName()))
        {
          senderFullname = userBean.getFullname();
        }
      }
      html = reportBean.getAsHtml(title);
      
      try {
        sendMailWithAuth(senderFullname, tos, html.toString(), title);
      } catch (Exception e) {
        LOG.info(e.getMessage());
      }

    }

    return Response.ok("sent").withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/getMeetingNotes")
  public Response.Content getMeetingNotes(String user, String token, String room, String fromTimestamp,
                                          String toTimestamp, String serverBase) throws IOException {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }

    Long from = null;
    Long to = null;
    String xwiki = "";
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
    String data = chatService.read(room, userService, false, from, to);
    String typeRoom = chatService.getTypeRoomChat(room);
    BasicDBObject datao = (BasicDBObject)JSON.parse(data);
    if (datao.containsField("messages")) {
      if(ChatService.TYPE_ROOM_USER.equalsIgnoreCase(typeRoom)) {
        users = userService.getUsersInRoomChatOneToOne(room);
      }
      else {
        users = userService.getUsers(room);
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
            roomName = roomBean.getFullname();
          }
        }
      }
      ReportBean reportBean = new ReportBean();
      reportBean.fill((BasicDBList) datao.get("messages"), users);
      ArrayList<String> usersInGroup = new ArrayList<String>();
      xwiki = reportBean.getAsXWiki(serverBase);
      try {
        for (UserBean userBean : users) {
          if (!"".equals(userBean.getName())) {
            usersInGroup.add(userBean.getName());
          }
        }
        jsonObject.put("users", usersInGroup);
        jsonObject.put("xwiki", xwiki);
        jsonObject.put("typeRoom", typeRoom);
      } catch (Exception e) {
        LOG.warning(e.getMessage());
        return Response.notFound("No Room yet");
      }
    }

    return Response.ok(jsonObject.toString()).withMimeType("application/json").withHeader("Cache-Control", "no-cache")
                   .withCharset(Tools.UTF_8);
  }

  @Resource
  @MimeType("text/plain")
  @Route("/delete")
  public Response.Content delete(String user, String token, String room, String messageId) throws IOException
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }
    try
    {
      chatService.delete(room, user,  messageId);
    }
    catch (Exception e)
    {
      return Response.notFound("Oups");
    }
    return Response.ok("Updated!");

  }

  @Resource
  @MimeType("text/plain")
  @Route("/edit")
  public Response.Content edit(String user, String token, String room, String messageId, String message) throws IOException
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
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
  public Response.Content toggleFavorite(String user, String token, String targetUser)
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }
    try
    {
      userService.toggleFavorite(user, targetUser);
    }
    catch (Exception e)
    {
      LOG.log(java.util.logging.Level.WARNING, e.getMessage());
      return Response.notFound("Oups");
    }
    return Response.ok("Updated!");
  }

  @Resource
  @Route("/getRoom")
  public Response.Content getRoom(String user, String token, String targetUser, String isAdmin, String withDetail,
                                  String type) {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }
    String room = targetUser;
    RoomBean roomBean = null;
    try
    {
      if (type != null) {
        if ("room-id".equals(type))
        {
          room = targetUser;
        }
        else if ("space-name".equals(type))
        {
          room = chatService.getSpaceRoomByName(targetUser);
        }
        else if ("space-id".equals(type))
        {
          room = ChatUtils.getRoomId(targetUser);
        }
        else if ("username".equals(type))
        {
          List<String> users = new ArrayList<String>();
          users.add(user);
          users.add(targetUser);
          room = chatService.getRoom(users);
        }
        else if ("external".equals(type))
        {
          room = chatService.getExternalRoom(targetUser);
        }
      }
      else if (targetUser.startsWith(ChatService.SPACE_PREFIX))
      {
        room = chatService.getSpaceRoom(targetUser);

      }
      else
      if (targetUser.startsWith(ChatService.TEAM_PREFIX))
      {
        room = chatService.getTeamRoom(targetUser, user);

      }
      else
      if (targetUser.startsWith(ChatService.EXTERNAL_PREFIX))
      {
        room = chatService.getExternalRoom(targetUser);
      }
      else
      {
        String finalUser = ("true".equals(isAdmin) && !user.startsWith(UserService.ANONIM_USER) && targetUser
                .startsWith(UserService.ANONIM_USER)) ? UserService.SUPPORT_USER : user;

        ArrayList<String> users = new ArrayList<String>(2);
        users.add(finalUser);
        users.add(targetUser);
        room = chatService.getRoom(users);
      }
      if ("true".equals(withDetail))
      {
        roomBean = userService.getRoom(user, room);
      }
      notificationService.setNotificationsAsRead(user, "chat", "room", room);
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

    return Response.ok(out).withMimeType("application/json").withCharset(Tools.UTF_8).withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/saveTeamRoom")
  public Response.Content saveTeamRoom(String user, String token, String teamName, String room, String users)
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }
    JSONObject jsonObject = new JSONObject();

    try
    {
      try {
    	teamName = URLDecoder.decode(teamName,"UTF-8");
      } catch (UnsupportedEncodingException e) {
        LOG.info("Cannot decode message: " + teamName);
      }
      if (room==null || "".equals(room) || "---".equals(room))
      {
        room = chatService.getTeamRoom(teamName, user);
        userService.addTeamRoom(user, room);
      }
      else
      {
        // Only creator can edit members or set team name
        String creator = chatService.getTeamCreator(room);
        if (!user.equals(creator)) {
          return Response.notFound("Petit malin !");
        }

        if (room.startsWith(ChatService.TEAM_PREFIX) && room.length()>ChatService.TEAM_PREFIX.length()+1)
        {
          room = room.substring(ChatService.TEAM_PREFIX.length());
        }
        chatService.setRoomName(room, teamName);
      }

      if (users!=null && !"".equals(users)) {
        String[] ausers = users.split(",");
        List<String> usersNew = Arrays.asList(ausers);
        List<String> usersToAdd = new ArrayList<String>(usersNew);
        List<String> usersToRemove = new ArrayList<String>();
        List<String> usersExisting = userService.getUsersFilterBy(null, room, ChatService.TYPE_ROOM_TEAM);

        for (String userExist:usersExisting)
        {
          if (!usersNew.contains(userExist))
          {
            usersToRemove.add(userExist);
          }
          else if (usersNew.contains(userExist))
          {
            usersToAdd.remove(userExist);
          }
        }
        if (usersToRemove.size()>0)
        {
          userService.removeTeamUsers(room, usersToRemove);
          StringBuilder sbUsers = new StringBuilder();
          boolean first = true;
          for (String usert:usersToRemove)
          {
            if (!first) sbUsers.append("; ");
            sbUsers.append(userService.getUserFullName(usert));
            first = false;
            notificationService.setNotificationsAsRead(usert, "chat", "room", room);
          }
          String removeTeamUserOptions
                  = "{\"type\":\"type-remove-team-user\",\"users\":\"" + sbUsers + "\", " +
                  "\"fullname\":\"" + userService.getUserFullName(user) + "\"}";
          this.send(user, token, ChatService.TEAM_PREFIX+room, StringUtils.EMPTY, room, "true", removeTeamUserOptions);
        }
        if (usersToAdd.size()>0)
        {
          userService.addTeamUsers(room, usersToAdd);
          StringBuilder sbUsers = new StringBuilder();
          boolean first = true;
          for (String usert:usersToAdd)
          {
            if (!first) sbUsers.append("; ");
            sbUsers.append(userService.getUserFullName(usert));
            first = false;
          }
          String addTeamUserOptions
                  = "{\"type\":\"type-add-team-user\",\"users\":\"" + sbUsers + "\", " +
                  "\"fullname\":\"" + userService.getUserFullName(user) + "\"}";
          this.send(user, token, ChatService.TEAM_PREFIX+room, StringUtils.EMPTY, room, "true", addTeamUserOptions);
        }

      }

      jsonObject.put("name", teamName);
      jsonObject.put("room", room);

    }
    catch (Exception e)
    {
      LOG.warning(e.getMessage());
      return Response.notFound("No Room yet");
    }
    return Response.ok(jsonObject.toString()).withMimeType("application/json").withHeader("Cache-Control", "no-cache")
                   .withCharset(Tools.UTF_8);
  }

  @Resource
  @MimeType("text/plain")
  @Route("/updateUnreadMessages")
  public Response.Content updateUnreadMessages(String room, String user, String token)
  {
    if (!tokenService.hasUserWithToken(user,  token))
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
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }
    boolean detailed = ("true".equals(withDetails));
    int totalUnread = 0;
    List<NotificationBean> notifications = null;
    if (!detailed)
    {
      // GETTING TOTAL NOTIFICATION WITHOUT DETAILS
      totalUnread = notificationService.getUnreadNotificationsTotal(user);
      if (userService.isAdmin(user))
      {
        totalUnread += notificationService.getUnreadNotificationsTotal(UserService.SUPPORT_USER);
      }
    }
    else {
      // GETTING ALL NOTIFICATION DETAILS
      notifications = notificationService.getUnreadNotifications(user, userService);
      totalUnread = notifications.size();
    }

    String data = "{\"total\": \""+totalUnread+"\"";
    if (detailed && notifications!=null)
    {
      data += ","+NotificationBean.notificationstoJSON(notifications);
    }
    data += "}";
    if (event!=null && event.equals("1"))
    {
      data = "id: "+totalUnread+"\n";
      data += "data: {\"total\": "+totalUnread+"}\n\n";
    }

    return Response.ok(data).withMimeType("application/json").withCharset(Tools.UTF_8).withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @MimeType("text/plain")
  @Route("/getStatus")
  public Response.Content getStatus(String user, String token, String targetUser)
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }
    String status = UserService.STATUS_INVISIBLE;
    try
    {
      if (targetUser!=null)
      {
        boolean online = tokenService.isUserOnline(targetUser);
        if (online)
          status = userService.getStatus(targetUser);
        else
          status = UserService.STATUS_OFFLINE;
      }
      else
      {
        status = userService.getStatus(user);
        tokenService.updateValidity(user, token);
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
    if (!tokenService.hasUserWithToken(user,  token))
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
    if (!tokenService.hasUserWithToken(user,  token))
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
  public Response.Content getUsers(String user, String token, String room, String filter)
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }

    List<UserBean> users;
    if (room!=null && !"".equals(room))
    {
      users = userService.getUsers(room);
    }
    else
    {
      users = userService.getUsers(filter, true);
    }

    for (UserBean userBean:users)
    {
      boolean online = tokenService.isUserOnline(userBean.getName());
      if (!online) userBean.setStatus(UserService.STATUS_OFFLINE);
    }


    UsersBean usersBean = new UsersBean();
    usersBean.setUsers(users);
    return Response.ok(usersBean.usersToJSON()).withMimeType("application/json").withHeader
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

    return Response.ok(data.toString()).withMimeType("application/json").withCharset(Tools.UTF_8).withHeader("Cache-Control", "no-cache");
  }

  public void sendMailWithAuth(String senderFullname, List<String> toList, String htmlBody, String subject) throws Exception {
	  
    String host = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_HOST);
    String user = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_USER);
    String password = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_PASSWORD);
    String port = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_PORT);

    Properties props = System.getProperties();

    props.put("mail.smtp.user",user);
    props.put("mail.smtp.password", password);
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", port);
    //props.put("mail.debug", "true");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable","true");
    props.put("mail.smtp.EnableSSL.enable","true");

    Session session = Session.getInstance(props, null);
    //session.setDebug(true);
    
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(user, senderFullname));

    // To get the array of addresses
    for (String to: toList) {
      message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
    }
    
    message.setSubject(subject, "UTF-8");
    // Create a message part to represent the body text
    BodyPart messageBodyPart = new MimeBodyPart();
    messageBodyPart.setContent(htmlBody, "text/html; charset=UTF-8");
    // use a MimeMultipart as we need to handle the file attachments
    Multipart multipart = new MimeMultipart();
    // add the message body to the mime message
    multipart.addBodyPart(messageBodyPart);
    // Put all message parts in the message
    message.setContent(multipart);
    
    Transport transport = session.getTransport("smtps");
    try {
      transport.connect(host, user, password);
      message.saveChanges();
      transport.sendMessage(message, message.getAllRecipients());
    } finally {
      transport.close();
    }
  }
}
