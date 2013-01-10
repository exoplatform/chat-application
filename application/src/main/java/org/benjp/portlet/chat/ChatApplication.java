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
import juzu.plugin.ajax.Ajax;
import juzu.request.HttpContext;
import juzu.request.RenderContext;
import juzu.template.Template;
import org.benjp.listener.ServerBootstrap;
import org.benjp.services.SpaceBean;
import org.benjp.services.UserService;
import org.benjp.utils.PropertyManager;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SessionScoped
public class ChatApplication
{

  @Inject
  @Path("index.gtmpl")
  Template index;

  @Inject
  @Path("indexDemo.gtmpl")
  Template indexDemo;

  String sessionId_ = null;
  String remoteUser_ = null;
  boolean profileInitialized_ = false;


  OrganizationService organizationService_;

  SpaceService spaceService_;

  @Inject
  public ChatApplication(OrganizationService organizationService, SpaceService spaceService)
  {
    organizationService_ = organizationService;
    spaceService_ = spaceService;
  }


  @View
  public void index(RenderContext renderContext)
  {
    remoteUser_ = renderContext.getSecurityContext().getRemoteUser();
    boolean isPublic = (remoteUser_==null);
    if (isPublic) remoteUser_ = UserService.ANONIM_USER;
    sessionId_ = getSessionId(renderContext.getHttpContext());
    String chatServerURL = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_SERVER_URL);
    String chatIntervalChat = PropertyManager.getProperty(PropertyManager.PROPERTY_INTERVAL_CHAT);
    String chatIntervalSession = PropertyManager.getProperty(PropertyManager.PROPERTY_INTERVAL_SESSION);
    String chatIntervalStatus = PropertyManager.getProperty(PropertyManager.PROPERTY_INTERVAL_STATUS);
    String chatIntervalUsers = PropertyManager.getProperty(PropertyManager.PROPERTY_INTERVAL_USERS);

    String fullname = remoteUser_;
    if (!UserService.ANONIM_USER.equals(remoteUser_))
    {
      fullname = ServerBootstrap.getUserService().getUserFullName(remoteUser_);
      if (fullname==null) fullname=remoteUser_;
    }

    index.with().set("user", remoteUser_).set("room", "noroom")
            .set("sessionId", sessionId_).set("chatServerURL", chatServerURL)
            .set("fullname", fullname)
            .set("chatIntervalChat", chatIntervalChat).set("chatIntervalSession", chatIntervalSession)
            .set("chatIntervalStatus", chatIntervalStatus).set("chatIntervalUsers", chatIntervalUsers)
            .set("publicMode", isPublic)
            .render()
            .withMetaTag("viewport", "width=device-width, initial-scale=1.0");

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

  @Ajax
  @Resource
  public Response.Content maintainSession()
  {
    return Response.ok("OK").withMimeType("text/html; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Ajax
  @Resource
  public Response.Content initChatProfile()
  {
    String  out = "nothing to update";
    if (!profileInitialized_ && !UserService.ANONIM_USER.equals(remoteUser_))
    {
      try
      {
        // Add User in the DB
        addUser(remoteUser_, sessionId_);

        // Set user's Full Name in the DB
        saveFullName(remoteUser_);

        // Set user's Spaces in the DB
        saveSpaces(remoteUser_);

        out = "updated";
        profileInitialized_ = true;
      }
      catch (Exception e)
      {
        e.printStackTrace();
        profileInitialized_ = false;
        return Response.notFound("Error during init, try later");
      }
    }

    return Response.ok(out).withMimeType("text/html; charset=UTF-8").withHeader("Cache-Control", "no-cache");

  }

  @Ajax
  @Resource
  public Response.Content createDemoUser(String fullname, String email)
  {
    String out = "created";

    String username = UserService.ANONIM_USER + fullname.trim().toLowerCase().replace(" ", "-");
    remoteUser_ = username;
    addUser(remoteUser_, sessionId_);
    UserService userService = ServerBootstrap.getUserService();
    userService.addUserFullName(username, fullname);
    userService.addUserEmail(username, email);
    saveDemoSpace(username);

    return Response.ok(username).withMimeType("text/html; charset=UTF-8").withHeader("Cache-Control", "no-cache");
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
        if (user!=null)
        {
          fullname = user.getFirstName()+" "+user.getLastName();
          ServerBootstrap.getUserService().addUserFullName(username, fullname);
        }
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

  protected void saveDemoSpace(String username)
  {
    try
    {
      List<SpaceBean> beans = new ArrayList<SpaceBean>();
      SpaceBean spaceBean = new SpaceBean();
      spaceBean.setDisplayName("Welcome Space");
      spaceBean.setGroupId("/public");
      spaceBean.setId("welcome_space");
      spaceBean.setShortName("welcome_space");
      beans.add(spaceBean);

      ServerBootstrap.getUserService().setSpaces(username, beans);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

  }

}
