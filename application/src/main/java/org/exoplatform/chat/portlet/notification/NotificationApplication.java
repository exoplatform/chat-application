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

package org.exoplatform.chat.portlet.notification;

import juzu.Path;
import juzu.Resource;
import juzu.Response;
import juzu.SessionScoped;
import juzu.View;
import juzu.impl.common.Tools;
import juzu.plugin.ajax.Ajax;
import juzu.request.ApplicationContext;
import juzu.request.SecurityContext;
import juzu.request.UserContext;
import juzu.template.Template;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.chat.common.utils.ChatUtils;
import org.exoplatform.chat.listener.ServerBootstrap;
import org.exoplatform.chat.model.SpaceBean;
import org.exoplatform.chat.model.SpaceBeans;
import org.exoplatform.chat.services.UserService;
import org.exoplatform.chat.utils.PropertyManager;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.application.RequestNavigationData;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.common.router.ExoRouter;
import org.exoplatform.social.common.router.ExoRouter.Route;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.ws.frameworks.cometd.ContinuationService;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.portlet.PortletPreferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

@SessionScoped
public class NotificationApplication
{

  private static final Logger LOG = Logger.getLogger(NotificationApplication.class.getName());

  @Inject
  @Path("index.gtmpl")
  Template index;

  String token_ = "---";
  String cometdToken = null;
  String remoteUser_ = null;
  boolean profileInitialized_ = false;

  OrganizationService organizationService_;

  SpaceService spaceService_;

  String dbName;

  @Inject
  BundleService bundleService_;

  @Inject
  ContinuationService continuationService;

  @Inject
  Provider<PortletPreferences> providerPreferences;

  @Inject
  public NotificationApplication(OrganizationService organizationService, SpaceService spaceService)
  {
    organizationService_ = organizationService;
    spaceService_ = spaceService;
    dbName = ChatUtils.getDBName();
  }

  @View
  public Response.Content index(ApplicationContext applicationContext, SecurityContext securityContext, UserContext userContext) throws IOException
  {
    String chatServerURL = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_SERVER_URL);
    String chatPage = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_PORTAL_PAGE);
    remoteUser_ = securityContext.getRemoteUser();
    String chatIntervalChat = PropertyManager.getProperty(PropertyManager.PROPERTY_INTERVAL_CHAT);
    String chatIntervalNotif = PropertyManager.getProperty(PropertyManager.PROPERTY_INTERVAL_NOTIF);
    String plfUserStatusUpdateUrl = PropertyManager.getProperty(PropertyManager.PROPERTY_PLF_USER_STATUS_UPDATE_URL);


    PortletPreferences portletPreferences = providerPreferences.get();
    String title = portletPreferences.getValue("title", "---");
    Locale locale = userContext.getLocale();
    ResourceBundle bundle= applicationContext.resolveBundle(locale) ;
    String messages = bundleService_.getBundle("chatBundleData", bundle, locale);
    String spaceId = getCurrentSpaceId();

    String portalURI = Util.getPortalRequestContext().getPortalURI();
    cometdToken = continuationService.getUserToken(remoteUser_);

    return index.with().set("user", remoteUser_).set("token", token_)
            .set("cometdToken", cometdToken)
            .set("chatServerURL", chatServerURL).set("chatPage", chatPage)
            .set("chatIntervalChat", chatIntervalChat)
            .set("chatIntervalNotif", chatIntervalNotif)
            .set("plfUserStatusUpdateUrl", plfUserStatusUpdateUrl)
            .set("title", title)
            .set("messages", messages)
            .set("spaceId", spaceId)
            .set("sessionId", Util.getPortalRequestContext().getRequest().getSession().getId())
            .set("dbName", dbName)
            .set("portalURI", portalURI)
            .ok()
            .withCharset(Tools.UTF_8);
  }

  @Ajax
  @Resource
  public Response.Content initUserProfile()
  {
    String out = "{\"token\": \""+token_+"\", \"msg\": \"nothing to update\"}";
    if (!profileInitialized_)
    {
      try
      {
        // Generate and store token if doesn't exist yet.
        token_ = ServerBootstrap.getToken(remoteUser_);

        // Add User in the DB
        addUser(remoteUser_, token_, dbName);

        // Set user's Full Name in the DB
        saveFullNameAndEmail(remoteUser_, dbName);

        // Set user's Spaces in the DB
        saveSpaces(remoteUser_, dbName);

        out = "{\"token\": \""+token_+"\", \"msg\": \"updated\"}";

        profileInitialized_ = true;
      }
      catch (Exception e)
      {
        profileInitialized_ = false;
        return Response.notFound("Error during init, try later");
      }
    }

    if (!UserService.ANONIM_USER.equals(remoteUser_))
    {
      // Set user's Spaces in the DB
      saveSpaces(remoteUser_, dbName);
    }

    return Response.ok(out).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache")
                   .withCharset(Tools.UTF_8);

  }

  protected void addUser(String remoteUser, String token, String dbName)
  {
    ServerBootstrap.addUser(remoteUser, token, dbName);
  }

  protected String saveFullNameAndEmail(String username, String dbName)
  {
    String fullname = username;
    try
    {

      fullname = ServerBootstrap.getUserFullName(username, dbName);
      if (fullname==null || fullname.isEmpty())
      {
        User user = organizationService_.getUserHandler().findUserByName(username);
        fullname = user.getFirstName()+" "+user.getLastName();
        ServerBootstrap.addUserFullNameAndEmail(username, fullname, user.getEmail(), dbName);
      }

    } catch (Exception e) {
      LOG.warning(e.getMessage());
    }
    return fullname;
  }

  protected void saveSpaces(String username, String dbName)
  {
    try
    {
      ListAccess<Space> spacesListAccess = spaceService_.getAccessibleSpacesWithListAccess(username);
      List<Space> spaces = Arrays.asList(spacesListAccess.load(0, spacesListAccess.getSize()));
      ArrayList<SpaceBean> beans = new ArrayList<SpaceBean>();
      for (Space space:spaces)
      {
        SpaceBean spaceBean = new SpaceBean();
        spaceBean.setDisplayName(space.getDisplayName());
        spaceBean.setGroupId(space.getGroupId());
        spaceBean.setId(space.getId());
        spaceBean.setShortName(space.getShortName());
        beans.add(spaceBean);
      }
      ServerBootstrap.setSpaces(username, new SpaceBeans(beans), dbName);
    }
    catch (Exception e)
    {
      LOG.warning(e.getMessage());
    }
  }

  protected String getCurrentSpaceId() {
    Space currSpace = getSpaceByContext();
    if (currSpace != null) {
      return currSpace.getId();
    } else {
      return StringUtils.EMPTY;
    }
  }

  private static Space getSpaceByContext() {
    //
    PortalRequestContext pcontext = Util.getPortalRequestContext();
    String requestPath = pcontext.getControllerContext().getParameter(RequestNavigationData.REQUEST_PATH);

    if (!pcontext.getSiteType().equals(SiteType.GROUP) ||
        !pcontext.getSiteName().startsWith(SpaceUtils.SPACE_GROUP)) {
      return null;
    }

    Route route = ExoRouter.route(requestPath);
    if (route == null) return null;

    //
    String spacePrettyName = route.localArgs.get("spacePrettyName");
    SpaceService spaceService = (SpaceService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SpaceService.class);
    return spaceService.getSpaceByPrettyName(spacePrettyName);
  }
}
