package org.benjp.portlet.chat;

import juzu.Path;
import juzu.View;
import juzu.request.HttpContext;
import juzu.template.Template;
import org.benjp.listener.ServerBootstrap;

import javax.inject.Inject;
import javax.portlet.PortletPreferences;
import javax.servlet.http.Cookie;

public class ChatApplication extends juzu.Controller
{


  @Inject
  PortletPreferences portletPreferences;

  @Inject
  @Path("index.gtmpl")
  Template index;

  @View
  public void index()
  {
    String remoteUser = renderContext.getSecurityContext().getRemoteUser();
    String sessionId = getSessionId(renderContext.getHttpContext());
    String chatServerURL = portletPreferences.getValue("chatServerURL", "/chatServer");
    String fullname = ServerBootstrap.getUserService().getUserFullName(remoteUser);
    index.with().set("user", remoteUser).set("room", "noroom")
            .set("sessionId", sessionId).set("chatServerURL", chatServerURL)
            .set("fullname", fullname).render();
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
