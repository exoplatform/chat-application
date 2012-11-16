package org.benjp.portlet.chat;

import juzu.*;
import juzu.request.HttpContext;
import juzu.template.Template;
import org.benjp.services.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.Cookie;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class ChatServer extends juzu.Controller
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
  NotificationService notificationService;




  @View
  @Route("/")
  public void index() throws IOException
  {
    index.render();
  }

  @Resource
  @Route("/whoIsOnline")
  public void whoIsOnline(String user, String sessionId)
  {
    if (userService.hasUserWithSession(user,  sessionId))
    {
      List<RoomBean> rooms = chatService.getRooms(user, notificationService, userService);
      users.with().set("rooms", rooms).render();
    }
  }

  @Resource
  @Route("/send")
  public Response.Content send(String user, String sessionId, String targetUser, String message, String room, String event) throws IOException
  {
    if (!userService.hasUserWithSession(user,  sessionId))
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
        notificationService.addNotification(targetUser, "chat", "room", room, content, "/portal/default/chat?room="+room);
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
  @Route("/getRoom")
  public Response.Content getRoom(String user, String sessionId, String targetUser)
  {
    if (!userService.hasUserWithSession(user,  sessionId))
    {
      return Response.notFound("Petit malin !");
    }
    String room = "";
    try
    {
      ArrayList<String> users = new ArrayList<String>(2);
      users.add(user);
      users.add(targetUser);

      room = chatService.getRoom(users);
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
  public Response.Content updateUnreadMessages(String room, String user, String sessionId)
  {
    if (!userService.hasUserWithSession(user,  sessionId))
    {
      return Response.notFound("Petit malin !");
    }
    try
    {
      notificationService.setNotificationsAsRead(user, "chat", "room", room);
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
  public Response.Content notification(String user, String sessionId, String event) throws IOException
  {
    if (!userService.hasUserWithSession(user,  sessionId))
    {
      return Response.notFound("Petit malin !");
    }
    int totalUnread = notificationService.getUnreadNotificationsTotal(user);

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
  public Response.Content getStatus(String user, String sessionId)
  {
    if (!userService.hasUserWithSession(user,  sessionId))
    {
      return Response.notFound("Petit malin !");
    }
    String status = UserService.STATUS_INVISIBLE;
    try
    {
      status = userService.getStatus(user);
/*
      if (UserService.STATUS_NONE.equals(status))
      {
        status = userService.setStatus(user, UserService.STATUS_AVAILABLE);
      }
*/
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
  public Response.Content setStatus(String user, String sessionId, String status)
  {
    if (!userService.hasUserWithSession(user,  sessionId))
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


}
