package org.benjp.portlet.notification;

import juzu.Path;
import juzu.View;
import juzu.request.HttpContext;
import juzu.template.Template;
import org.benjp.listener.ServerBootstrap;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;

import javax.inject.Inject;
import javax.portlet.PortletPreferences;
import javax.servlet.http.Cookie;
import java.io.IOException;

public class NotificationApplication extends juzu.Controller
{

  @Inject
  PortletPreferences portletPreferences;

  @Inject
  @Path("index.gtmpl")
  Template index;

  OrganizationService organizationService_;

  @Inject
  public NotificationApplication(OrganizationService organizationService)
  {
    organizationService_ = organizationService;
  }

  @View
  public void index() throws IOException
  {
    String chatServerURL = portletPreferences.getValue("chatServerURL", "/chatServer");
    String sessionId = getSessionId(renderContext.getHttpContext());
    String remoteUser = renderContext.getSecurityContext().getRemoteUser();
    String fullname = getFullName(remoteUser);

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

  protected String getFullName(String username)
  {
    String fullname = username;
    try
    {

      fullname = ServerBootstrap.getUserService().getUserFullName(username);
      if (fullname==null)
      {
        User user = organizationService_.getUserHandler().findUserByName(username);
        fullname = user.getFirstName()+" "+user.getLastName();
        ServerBootstrap.getUserService().addUserFullName(username, fullname);
      }


    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return fullname;
  }

}
