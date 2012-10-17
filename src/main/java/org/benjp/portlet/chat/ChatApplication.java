package org.benjp.portlet.chat;

import juzu.Path;
import juzu.Resource;
import juzu.Response;
import juzu.View;
import juzu.request.HttpContext;
import juzu.template.Template;
import org.benjp.services.ChatService;
import org.benjp.services.NotificationService;
import org.benjp.services.RoomBean;
import org.benjp.services.UserService;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class ChatApplication extends juzu.Controller
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
  public void index() throws IOException
  {
    String remoteUser = renderContext.getSecurityContext().getRemoteUser();
    String sessionId = getSessionId(renderContext.getHttpContext());
    index.with().set("user", remoteUser).set("room", "noroom")
            .set("sessionId", sessionId).render();
  }

  @Resource
  public void whoIsOnline(String user, String sessionId)
  {
    if (userService.hasUserWithSession(user,  sessionId))
    {
      Collection<String> usersc = userService.getUsersFilterBy(user);
      ArrayList<RoomBean> rooms = new ArrayList<RoomBean>(usersc.size());
      for (String tuser: usersc)
      {
        ArrayList<String> userslist = new ArrayList<String>(2);
        userslist.add(user);
        userslist.add(tuser);
        RoomBean room = new RoomBean();
        room.setUser(tuser);
        String roomId = null;
        if (chatService.hasRoom(userslist))
        {
          roomId = chatService.getRoom(userslist);
        }
        if (roomId!=null)
        {
          room.setRoom(roomId);
          room.setUnreadTotal(chatService.getUnreadMessagesTotal(user, roomId));
        }
//      System.out.print("ROOM FOR "+user+" :: "+tuser+" ; "+roomId+" ; ");
        rooms.add(room);
      }

      users.with().set("rooms", rooms).render();
    }
  }

  @Resource
  public Response.Content send(String user, String sessionId, String targetUser, String message, String room) throws IOException
  {
    try
    {
      //System.out.println(user + "::" + message + "::" + room);
      if (message!=null && user!=null)
      {
        if (!userService.hasUserWithSession(user,  sessionId))
        {
          return Response.notFound("Petit malin !");
        }


        chatService.write(message, user, room);
        notificationService.addNotification(targetUser, "chat", "new message");
        notificationService.setLastReadNotification(user, notificationService.getLastNotification(user).getTimestamp());
      }

    }
    catch (Exception e)
    {
      return Response.notFound("Problem on Chat server. Please, try later").withMimeType("text/event-stream");
    }
    String data = "id: "+System.currentTimeMillis()+"\n";
    data += "data: "+chatService.read(room) +"\n\n";


    return Response.ok(data).withMimeType("text/event-stream").withHeader("Cache-Control", "no-cache");
  }

  @Resource
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
      chatService.updateLastReadMessage(user, room);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return Response.notFound("No Room yet");
    }
    return Response.ok(room);
  }

  @Resource
  public Response.Content updateUnreadMessages(String room, String user, String sessionId)
  {
    if (!userService.hasUserWithSession(user,  sessionId))
    {
      return Response.notFound("Petit malin !");
    }
    try
    {
      chatService.updateLastReadMessage(user,  room);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return Response.notFound("Server Not Available yet");
    }
    return Response.ok("Updated.");
  }


  private String getSessionId(HttpContext httpContext)
  {
    for (Cookie cookie:renderContext.getHttpContext().getCookies())
    {
      if("JSESSIONID".equals(cookie.getName()))
      {
        return cookie.getValue();
      }
    }
    return null;

  }
}
