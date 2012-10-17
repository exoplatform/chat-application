package org.benjp.portlet.chat;

import juzu.Path;
import juzu.View;
import juzu.request.HttpContext;
import juzu.template.Template;

import javax.inject.Inject;
import javax.servlet.http.Cookie;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class ChatApplication extends juzu.Controller
{

  @Inject
  @Path("index.gtmpl")
  Template index;

  @Inject
  @Path("users.gtmpl")
  Template users;

  @View
  public void index()
  {
    String remoteUser = renderContext.getSecurityContext().getRemoteUser();
    String sessionId = getSessionId(renderContext.getHttpContext());
    index.with().set("user", remoteUser).set("room", "noroom")
            .set("sessionId", sessionId).render();
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
