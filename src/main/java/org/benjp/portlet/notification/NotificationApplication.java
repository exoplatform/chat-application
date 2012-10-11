package org.benjp.portlet.notification;

import juzu.*;
import juzu.template.Template;
import org.benjp.services.ChatService;
import org.benjp.services.NotificationBean;
import org.benjp.services.NotificationService;

import javax.inject.Inject;
import java.io.IOException;

public class NotificationApplication extends Controller
{

  @Inject
  @Path("index.gtmpl")
  Template index;

  @Inject
  NotificationService notificationService;

  @View
  public void index() throws IOException
  {
    String remoteUser = renderContext.getSecurityContext().getRemoteUser();
    Long ts = notificationService.getLastReadNotificationTimestamp(remoteUser);
    index.with().set("user", remoteUser).set("lastRead", ts).render();
  }

  @Resource
  public Response.Content notification(String user) throws IOException
  {
    NotificationBean last = notificationService.getLastNotification(user);
    String data = "id: "+last.getTimestamp()+"\n";
    data += "data: "+ last.getTimestamp() +"\n\n";


    return Response.ok(data).withMimeType("text/event-stream").withHeader("Cache-Control", "no-cache");
  }

}
