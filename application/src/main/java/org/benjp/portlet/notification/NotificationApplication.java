package org.benjp.portlet.notification;

import juzu.Controller;
import juzu.Path;
import juzu.View;
import juzu.template.Template;

import javax.inject.Inject;
import javax.portlet.PortletPreferences;
import java.io.IOException;

public class NotificationApplication extends Controller
{

  @Inject
  PortletPreferences portletPreferences;

  @Inject
  @Path("index.gtmpl")
  Template index;

  @View
  public void index() throws IOException
  {
    String chatServerURL = portletPreferences.getValue("chatServerURL", "/chatServer");
    String remoteUser = renderContext.getSecurityContext().getRemoteUser();
    index.with().set("user", remoteUser).set("chatServerURL", chatServerURL).render();
  }

}
