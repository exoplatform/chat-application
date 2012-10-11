package org.benjp.portlet.chat;

import juzu.Path;
import juzu.Resource;
import juzu.Response;
import juzu.View;
import juzu.template.Template;
import org.benjp.services.ChatService;
import org.benjp.services.UserService;

import javax.inject.Inject;
import java.io.*;

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

  @View
  public void index() throws IOException
  {
    String remoteUser = renderContext.getSecurityContext().getRemoteUser();
    index.with().set("user", remoteUser).set("room", "roomname").render();
  }

  @Resource
  public void whoIsOnline(String user)
  {
    users.with().set("users", UserService.getUsersFilterBy(user)).render();
  }

  @Resource
  public Response.Content send(String user, String message, String room) throws IOException
  {
    try
    {
      //System.out.println(user + "::" + message + "::" + room);
      if (message!=null && user!=null)
      {
        chatService.write(message, user, room);
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


}
