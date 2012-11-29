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

import juzu.*;
import juzu.request.HttpContext;
import juzu.template.Template;
import org.benjp.listener.ServerBootstrap;
import org.benjp.services.SpaceBean;
import org.benjp.utils.PropertyManager;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import javax.inject.Inject;
import javax.portlet.PortletPreferences;
import javax.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SessionScoped
public class ChatApplication extends juzu.Controller
{

  @Inject
  @Path("index.gtmpl")
  Template index;

  String sessionId = null;

  OrganizationService organizationService_;

  SpaceService spaceService_;

  @Inject
  public ChatApplication(OrganizationService organizationService, SpaceService spaceService)
  {
    organizationService_ = organizationService;
    spaceService_ = spaceService;
  }


  @View
  public void index()
  {
    String remoteUser = renderContext.getSecurityContext().getRemoteUser();
    String sessionId = getSessionId(renderContext.getHttpContext());
    String chatServerURL = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_SERVER_URL);
    String fullname = ServerBootstrap.getUserService().getUserFullName(remoteUser);
    if (fullname==null) fullname=remoteUser;
    index.with().set("user", remoteUser).set("room", "noroom")
            .set("sessionId", sessionId).set("chatServerURL", chatServerURL)
            .set("fullname", fullname).render();
  }

  private String getSessionId(HttpContext httpContext)
  {
    for (Cookie cookie:httpContext.getCookies())
    {
      if("JSESSIONID".equals(cookie.getName()))
      {
        return cookie.getValue();
      }
    }
    return null;

  }

  @Resource
  @Route("/maintainSession")
  public Response.Content maintainSession()
  {
    getSessionId(resourceContext.getHttpContext());
    return Response.ok("OK").withMimeType("text/html; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/initChatProfile")
  public Response.Content initChatProfile()
  {
    String  out = "nothing to update";
    if (this.sessionId==null)
    {
      try
      {
        sessionId = getSessionId(resourceContext.getHttpContext());
        String remoteUser = resourceContext.getSecurityContext().getRemoteUser();

        // Add User in the DB
        addUser(remoteUser, sessionId);

        // Set user's Full Name in the DB
        saveFullName(remoteUser);

        // Set user's Spaces in the DB
        saveSpaces(remoteUser);

        out = "updated";
      }
      catch (Exception e)
      {
        e.printStackTrace();
        sessionId = null;
        return Response.notFound("Error during init, try later");
      }
    }

    return Response.ok(out).withMimeType("text/html; charset=UTF-8").withHeader("Cache-Control", "no-cache");

  }


  protected void addUser(String remoteUser, String sessionId)
  {
    ServerBootstrap.getUserService().addUser(remoteUser, sessionId);
  }

  protected String saveFullName(String username)
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

  protected void saveSpaces(String username)
  {
    try
    {
      ListAccess<Space> spacesListAccess = spaceService_.getAccessibleSpacesWithListAccess(username);
      List<Space> spaces = Arrays.asList(spacesListAccess.load(0, spacesListAccess.getSize()));
      List<SpaceBean> beans = new ArrayList<SpaceBean>();
      for (Space space:spaces)
      {
        SpaceBean spaceBean = new SpaceBean();
        spaceBean.setDisplayName(space.getDisplayName());
        spaceBean.setGroupId(space.getGroupId());
        spaceBean.setId(space.getId());
        spaceBean.setShortName(space.getShortName());
        beans.add(spaceBean);
      }
      ServerBootstrap.getUserService().setSpaces(username, beans);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

  }

}
