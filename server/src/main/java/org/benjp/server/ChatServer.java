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

package org.benjp.server;

import juzu.*;
import juzu.template.Template;
import org.benjp.listener.GuiceManager;
import org.benjp.model.RoomsBean;
import org.benjp.model.UserBean;
import org.benjp.model.UsersBean;
import org.benjp.services.ChatService;
import org.benjp.services.NotificationService;
import org.benjp.services.TokenService;
import org.benjp.services.UserService;
import org.benjp.utils.ChatUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class ChatServer
{
  Logger log = Logger.getLogger("ChatServer");

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
  public void index() throws IOException
  {
    index.render();
  }

  @Resource
  @Route("/whoIsOnline")
  public Response.Content whoIsOnline(String user, String token, String filter, String withUsers, String withSpaces, String withPublic, String withOffline, String isAdmin)
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }

//    RoomsBean roomsBean = chatService.getRooms(user, filter, "true".equals(withUsers), "true".equals(withSpaces), "true".equals(withPublic), "true".equals(withOffline), "true".equals(isAdmin), notificationService, userService, tokenService);
    RoomsBean roomsBean = chatService.getRooms(user, filter, true, true, false, true, "true".equals(isAdmin), notificationService, userService, tokenService);
    return Response.ok(roomsBean.roomsToJSON()).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/send")
  public Response.Content send(String user, String token, String targetUser, String message, String room, String isSystem, String options) throws IOException
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }
    try
    {
      //System.out.println(user + "::" + message + "::" + room);
      if (message!=null)
      {
//        System.out.println(user + "::" + message + "::" + room);
        if (isSystem==null) isSystem="false";
        chatService.write(message, user, room, isSystem, options);
        String content = "New message from "+user+" : "+((message.length()>15)?message.substring(0,14)+"...":message);

        if (targetUser.startsWith(ChatService.SPACE_PREFIX))
        {
          List<String> users = userService.getUsersFilterBy(user, targetUser.substring(ChatService.SPACE_PREFIX.length()), ChatService.TYPE_ROOM_SPACE);
          for (String tuser:users)
          {
            notificationService.addNotification(tuser, "chat", "room", room, content, "/portal/default/chat?room="+room);
          }
        }
        else if (targetUser.startsWith(ChatService.TEAM_PREFIX))
        {
          List<String> users = userService.getUsersFilterBy(user, targetUser.substring(ChatService.TEAM_PREFIX.length()), ChatService.TYPE_ROOM_TEAM);
          for (String tuser:users)
          {
            notificationService.addNotification(tuser, "chat", "room", room, content, "/portal/default/chat?room="+room);
          }
        }
        else
        {
          notificationService.addNotification(targetUser, "chat", "room", room, content, "/portal/default/chat?room="+room);
        }

        //notificationService.setNotificationsAsRead(user, "chat", "room", room);
      }

    }
    catch (Exception e)
    {
      //e.printStackTrace();
      return Response.notFound("Problem on Chat server. Please, try later").withMimeType("text/event-stream");
    }


    return Response.ok("ok").withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/read")
  public Response.Content read(String user, String token, String room, String fromTimestamp, String isTextOnly) throws IOException
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }

    Long from = null;
    try {
      if (fromTimestamp!=null && !"".equals(fromTimestamp))
        from = Long.parseLong(fromTimestamp);
    } catch (NumberFormatException nfe) {
      log.info("fromTimestamp is not a valid Long number");
    }
    String data = chatService.read(room, userService, "true".equals(isTextOnly), from);

    return Response.ok(data).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
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
  @Route("/edit")
  public Response.Content edit(String user, String token, String room, String messageId, String message) throws IOException
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }
    try
    {
      chatService.edit(room, user, messageId, message);
    }
    catch (Exception e)
    {
      return Response.notFound("Oups");
    }
    return Response.ok("Updated!");

  }

  @Resource
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
      e.printStackTrace();
      return Response.notFound("Oups");
    }
    return Response.ok("Updated!");
  }

  @Resource
  @Route("/getRoom")
  public Response.Content getRoom(String user, String token, String targetUser, String isAdmin)
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }
    String room;
    try
    {
      if (targetUser.startsWith(ChatService.SPACE_PREFIX))
      {
        room = chatService.getSpaceRoom(targetUser);

      }
      else
      if (targetUser.startsWith(ChatService.TEAM_PREFIX))
      {
        room = chatService.getTeamRoom(targetUser, user);

      }
      else
      {
        String finalUser = ("true".equals(isAdmin) && !user.startsWith(UserService.ANONIM_USER) && targetUser.startsWith(UserService.ANONIM_USER))?UserService.SUPPORT_USER:user;

        ArrayList<String> users = new ArrayList<String>(2);
        users.add(finalUser);
        users.add(targetUser);
        room = chatService.getRoom(users);
      }
      notificationService.setNotificationsAsRead(user, "chat", "room", room);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return Response.notFound("No Room yet");
    }
    return Response.ok(room);
  }

  @Resource
  @Route("/saveTeamRoom")
  public Response.Content saveTeamRoom(String user, String token, String teamName, String room, String users)
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }
    String data = "{}";

    try
    {
      if (room==null || "".equals(room) || "---".equals(room))
      {
        room = chatService.getTeamRoom(teamName, user);
        userService.addTeamRoom(user, room);
      }
      else
      {
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
          StringBuilder sb = new StringBuilder();
          boolean first = true;
          for (String usert:usersToRemove)
          {
            if (!first) sb.append(" ; ");
            sb.append(userService.getUserFullName(usert));
            first = false;
            notificationService.setNotificationsAsRead(usert, "chat", "room", room);
          }
          this.send(user, token, ChatService.TEAM_PREFIX+room, "Users Removed: "+sb.toString(), room, "true", null);
        }
        if (usersToAdd.size()>0)
        {
          userService.addTeamUsers(room, usersToAdd);
          StringBuilder sb = new StringBuilder();
          boolean first = true;
          for (String usert:usersToAdd)
          {
            if (!first) sb.append(" ; ");
            sb.append(userService.getUserFullName(usert));
            first = false;
          }
          this.send(user, token, ChatService.TEAM_PREFIX+room, "Users Added: "+sb.toString(), room, "true", null);
        }

      }

      data = "{\"name\": \""+teamName+"\", \"room\": \""+room+"\"}";

    }
    catch (Exception e)
    {
      e.printStackTrace();
      return Response.notFound("No Room yet");
    }
    return Response.ok(data).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
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
      e.printStackTrace();
      return Response.notFound("Server Not Available yet");
    }
    return Response.ok("Updated.");
  }


  @Resource
  @Route("/notification")
  public Response.Content notification(String user, String token, String event) throws IOException
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }
    int totalUnread = notificationService.getUnreadNotificationsTotal(user);

    if (userService.isAdmin(user))
    {
      totalUnread += notificationService.getUnreadNotificationsTotal(UserService.SUPPORT_USER);
    }

    String data = "{\"total\": \""+totalUnread+"\"}";
    if (event!=null && event.equals("1"))
    {
      data = "id: "+totalUnread+"\n";
      data += "data: {\"total\": "+totalUnread+"}\n\n";
    }

    return Response.ok(data).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
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
      e.printStackTrace();
      return Response.notFound(status);
    }
    return Response.ok(status);
  }

  @Resource
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
      e.printStackTrace();
      return Response.notFound("No Status for this User");
    }
    return Response.ok(status);
  }

  @Resource
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
      e.printStackTrace();
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
    return Response.ok(usersBean.usersToJSON()).withMimeType("application/json; charset=UTF-8").withHeader("Cache-Control", "no-cache");
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

    return Response.ok(data.toString()).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }


}
