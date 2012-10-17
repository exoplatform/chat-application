package org.benjp.portlet.chat;

import juzu.Path;
import juzu.Resource;
import juzu.Response;
import juzu.View;
import juzu.template.Template;
import org.benjp.services.ChatService;
import org.benjp.services.NotificationService;
import org.benjp.services.RoomBean;
import org.benjp.services.UserService;

import javax.inject.Inject;
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
  NotificationService notificationService;

  @View
  public void index() throws IOException
  {
    String remoteUser = renderContext.getSecurityContext().getRemoteUser();
    index.with().set("user", remoteUser).set("room", "noroom").render();
  }

  @Resource
  public void whoIsOnline(String user)
  {
    Collection<String> usersc = UserService.getUsersFilterBy(user);
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

  @Resource
  public Response.Content send(String user, String targetUser, String message, String room) throws IOException
  {
    try
    {
      //System.out.println(user + "::" + message + "::" + room);
      if (message!=null && user!=null)
      {
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
  public Response.Content getRoom(String user)
  {
    String remoteUser = resourceContext.getSecurityContext().getRemoteUser();
    String room = "";
    try
    {
      ArrayList<String> users = new ArrayList<String>(2);
      users.add(user);
      users.add(remoteUser);

      room = chatService.getRoom(users);
      chatService.updateLastReadMessage(remoteUser, room);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return Response.notFound("No Room yet");
    }
    return Response.ok(room);
  }

  @Resource
  public Response.Content updateUnreadMessages(String room)
  {
    String remoteUser = resourceContext.getSecurityContext().getRemoteUser();
    try
    {
      chatService.updateLastReadMessage(remoteUser,  room);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return Response.notFound("Server Not Available yet");
    }
    return Response.ok("Updated.");
  }

}
