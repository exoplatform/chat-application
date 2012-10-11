package org.benjp.portlet.notification;

import juzu.*;
import juzu.template.Template;
import org.benjp.services.ChatService;

import javax.inject.Inject;
import java.io.IOException;

public class NotificationApplication extends Controller
{

  @Inject
  @Path("index.gtmpl")
  Template index;

  @View
  public void index() throws IOException
  {
    String remoteUser = renderContext.getSecurityContext().getRemoteUser();
    index.with().set("user", remoteUser).render();
  }

  @Resource
  public Response.Content notification(String user) throws IOException
  {

    String data = "id: "+System.currentTimeMillis()+"\n";
    data += "data: "+ "OK" +"\n\n";


    return Response.ok(data).withMimeType("text/event-stream").withHeader("Cache-Control", "no-cache");
  }

}
