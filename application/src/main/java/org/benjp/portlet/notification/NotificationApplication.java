package org.benjp.portlet.notification;

import juzu.Controller;
import juzu.Path;
import juzu.View;
import juzu.template.Template;

import javax.inject.Inject;
import java.io.IOException;

public class NotificationApplication extends Controller
{

  @Inject
  @Path("index.gtmpl")
  Template index;

  //@Inject
  //NotificationService notificationService;

  @View
  public void index() throws IOException
  {
    String remoteUser = renderContext.getSecurityContext().getRemoteUser();
    Long ts = new Long(0);//notificationService.getLastReadNotificationTimestamp(remoteUser);
//    System.out.println("NOTIFAPP::"+remoteUser+"::"+ts);
    index.with().set("user", remoteUser).set("lastRead", ts).render();
  }

  /*
  @Resource
  public Response.Content notification(String user) throws IOException
  {
    NotificationBean last = notificationService.getLastNotification(user);
    Long lastRead = notificationService.getLastReadNotificationTimestamp(user);
    int totalUnread = notificationService.getUnreadNotificationsTotal(user);
    String data = "id: "+last.getTimestamp()+":"+lastRead+"\n";
    data += "data: {\"last\": "+ last.getTimestamp() +", \"lastRead\": "+lastRead+", \"total\": "+totalUnread+"}\n\n";


    return Response.ok(data).withMimeType("text/event-stream").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  public Response.Content readNotification(String user)
  {
    try
    {
      notificationService.setLastReadNotification(user, notificationService.getLastNotification(user).getTimestamp());
    }
    catch (Exception e)
    {
      return Response.notFound("Server not available");
    }
    return Response.ok("Updated.");
  }
  */

}
