/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.benjp.portlet.chat;

import juzu.Path;
import juzu.View;
import juzu.request.HttpContext;
import juzu.template.Template;
import org.benjp.listener.ServerBootstrap;
import org.benjp.utils.PropertyManager;

import javax.inject.Inject;
import javax.portlet.PortletPreferences;
import javax.servlet.http.Cookie;

public class ChatApplication extends juzu.Controller
{

  @Inject
  @Path("index.gtmpl")
  Template index;

  @View
  public void index()
  {
    String remoteUser = renderContext.getSecurityContext().getRemoteUser();
    String sessionId = getSessionId(renderContext.getHttpContext());
    String chatServerURL = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_SERVER_URL);
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
