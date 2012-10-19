package org.benjp.portlet.notification;

import juzu.Controller;
import juzu.Path;
import juzu.View;
import juzu.request.HttpContext;
import juzu.template.Template;

import javax.inject.Inject;
import javax.portlet.PortletPreferences;
import javax.servlet.http.Cookie;
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
    String sessionId = getSessionId(renderContext.getHttpContext());
    String remoteUser = renderContext.getSecurityContext().getRemoteUser();
    index.with().set("user", remoteUser).set("sessionId", sessionId).set("chatServerURL", chatServerURL).render();
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
