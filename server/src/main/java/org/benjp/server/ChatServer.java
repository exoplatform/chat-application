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
import org.benjp.model.RoomsBean;
import org.benjp.services.*;
import org.benjp.model.RoomBean;
import org.benjp.utils.PropertyManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ChatServer
{

  @Inject
  @Path("index.gtmpl")
  Template index;

  @Inject
  @Path("users.gtmpl")
  Template users;

  @Inject
  ChatService chatService;

  @Inject
  UserService userService;

  @Inject
  TokenService tokenService;

  @Inject
  NotificationService notificationService;

  @Inject
  SchedulerService schedulerService;


  @View
  @Route("/")
  public void index() throws IOException
  {
    index.render();
  }

  @Resource
  @Route("/whoIsOnline")
  public Response.Content whoIsOnline(String user, String token, String filter, String withUsers, String withSpaces, String withPublic, String isAdmin)
  {
/*
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
*/
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }

    List<RoomBean> rooms = chatService.getRooms(user, filter, "true".equals(withUsers), "true".equals(withSpaces), "true".equals(withPublic), "true".equals(isAdmin), notificationService, userService, tokenService);
    RoomsBean roomsBean = new RoomsBean();
    roomsBean.setRooms(rooms);
//    return users.with().set("rooms", rooms).ok().withMimeType("text/html; charset=UTF-8").withHeader("Cache-Control", "no-cache");
    return Response.ok(roomsBean.roomsToJSON()).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/send")
  public Response.Content send(String user, String token, String targetUser, String message, String room, String event) throws IOException
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }
    try
    {
      //System.out.println(user + "::" + message + "::" + room);
      if (message!=null && user!=null)
      {
//        System.out.println(user + "::" + message + "::" + room);
        chatService.write(message, user, room);
        String content = "New message from "+user+" : "+((message.length()>15)?message.substring(0,14)+"...":message);

        if (!targetUser.startsWith(ChatService.SPACE_PREFIX))
          notificationService.addNotification(targetUser, "chat", "room", room, content, "/portal/default/chat?room="+room);
        else
        {
          List<String> users = userService.getUsersFilterBy(user, targetUser.substring(ChatService.SPACE_PREFIX.length()));
          for (String tuser:users)
          {
            notificationService.addNotification(tuser, "chat", "room", room, content, "/portal/default/chat?room="+room);
          }
        }

        notificationService.setNotificationsAsRead(user, "chat", "room", room);
      }

    }
    catch (Exception e)
    {
      //e.printStackTrace();
      return Response.notFound("Problem on Chat server. Please, try later").withMimeType("text/event-stream");
    }

    String data = chatService.read(room, userService);
    if (event!=null && event.equals("1"))
    {
      data = "id: "+System.currentTimeMillis()+"\n"+"data: "+data+"\n\n";
    }


    return Response.ok(data).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
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
  public Response.Content getStatus(String user, String token)
  {
    if (!tokenService.hasUserWithToken(user,  token))
    {
      return Response.notFound("Petit malin !");
    }
    String status = UserService.STATUS_INVISIBLE;
    try
    {
      status = userService.getStatus(user);
      tokenService.updateValidity(user, token);
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

  @Resource
  @Route("/createDemoUser")
  public Response.Content createDemoUser(String username, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    if (username == null)
    {
      return Response.notFound("{ \"message\": \"username is null\"}");
    }

    String token = tokenService.getToken(username);
    tokenService.addUser(username, token);
    userService.addUserFullName(username, username);

    StringBuffer data = new StringBuffer();
    data.append("{");
    data.append(" \"message\": \"OK\", ");
    data.append(" \"token\": \""+token+"\", ");
    data.append(" \"user\": \""+ username+"\" ");
    data.append("}");

    return Response.ok(data.toString()).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/getToken")
  public Response.Content getToken(String username, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    if (username == null)
    {
      return Response.notFound("{ \"message\": \"username is null\"}");
    }

    String token = tokenService.getToken(username);

    StringBuffer data = new StringBuffer();
    data.append("{");
    data.append(" \"message\": \"OK\", ");
    data.append(" \"token\": \""+token+"\", ");
    data.append(" \"user\": \""+ username+"\" ");
    data.append("}");

    return Response.ok(data.toString()).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/initDB")
  public Response.Content initDB(String db, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    if (db == null)
    {
      return Response.notFound("{ \"message\": \"db is null\"}");
    }

    MongoBootstrap.getDB(db);

    StringBuffer data = new StringBuffer();
    data.append("{");
    data.append(" \"message\": \"using db="+db+"\"");
    data.append("}");

    return Response.ok(data.toString()).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/dropDB")
  public Response.Content dropDB(String db, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    if (db == null)
    {
      return Response.notFound("{ \"message\": \"db is null\"}");
    }

    MongoBootstrap.dropDB(db);

    StringBuffer data = new StringBuffer();
    data.append("{");
    data.append(" \"message\": \"deleting db="+db+"\"");
    data.append("}");

    return Response.ok(data.toString()).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/ensureIndexes")
  public Response.Content ensureIndexes(String db, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    if (db == null)
    {
      return Response.notFound("{ \"message\": \"db is null\"}");
    }

    if (!db.equals(MongoBootstrap.getDB().getName()))
    {
      return Response.notFound("{ \"message\": \"db name doesn't match\"}");
    }

    MongoBootstrap.ensureIndexes();

    StringBuffer data = new StringBuffer();
    data.append("{");
    data.append(" \"message\": \"indexes created or updated on db="+db+"\"");
    data.append("}");

    return Response.ok(data.toString()).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

}
