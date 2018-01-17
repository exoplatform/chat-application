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

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.portlet.PortletPreferences;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addons.chat.listener.ServerBootstrap;
import org.exoplatform.chat.utils.PropertyManager;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.ws.frameworks.cometd.ContinuationService;

import juzu.Path;
import juzu.Response;
import juzu.SessionScoped;
import juzu.View;
import juzu.impl.common.Tools;
import juzu.request.ApplicationContext;
import juzu.request.SecurityContext;
import juzu.request.UserContext;
import juzu.template.Template;

@SessionScoped
public class NotificationApplication
{

  private static final Logger LOG = Logger.getLogger(NotificationApplication.class.getName());

  @Inject
  @Path("index.gtmpl")
  Template index;

  boolean standaloneChatServer;
  String token_ = "---";
  String remoteUser_ = null;
  boolean profileInitialized_ = false;
  String chatCometDServerUrl;
  String chatServerURI;
  String chatPage;
  String plfUserStatusUpdateUrl;

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
    dbName = ServerBootstrap.getDBName();
    standaloneChatServer = Boolean.valueOf(PropertyManager.getProperty("standaloneChatServer"));
    chatServerURI = ServerBootstrap.getServerURI();
    chatPage = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_PORTAL_PAGE);
    plfUserStatusUpdateUrl = PropertyManager.getProperty(PropertyManager.PROPERTY_PLF_USER_STATUS_UPDATE_URL);
    if (standaloneChatServer) {
      chatCometDServerUrl = chatServerURI;
    } else {
      chatCometDServerUrl = "/cometd";
    }
  }

  @View
  public Response.Content index(ApplicationContext applicationContext, SecurityContext securityContext, UserContext userContext) throws IOException
  {
    remoteUser_ = securityContext.getRemoteUser();

    initUserProfile();

    PortletPreferences portletPreferences = providerPreferences.get();
    String title = portletPreferences.getValue("title", "---");
    Locale locale = userContext.getLocale();
    ResourceBundle bundle= applicationContext.resolveBundle(locale);
    String messages = bundleService_.getBundle("chatBundleData", bundle, locale);
    Space space = SpaceUtils.getSpaceByContext();
    String spaceId = space == null ? StringUtils.EMPTY : space.getId();

    String portalURI = Util.getPortalRequestContext().getPortalURI();

    String cometdToken;
    if (standaloneChatServer) {
      cometdToken = token_;
    } else {
      cometdToken = continuationService.getUserToken(remoteUser_);
    }

    return index.with().set("user", remoteUser_).set("token", token_)
        .set("standalone", standaloneChatServer)
        .set("chatCometDServerUrl", chatCometDServerUrl)
        .set("cometdToken", cometdToken)
        .set("chatServerURL", chatServerURI).set("chatPage", chatPage)
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

  public void initUserProfile()
  {
    if (!profileInitialized_)
    {
      try
      {
        // Generate and store token if doesn't exist yet.
        token_ = ServerBootstrap.getToken(remoteUser_);

        // Add User in the DB
        ServerBootstrap.addUser(remoteUser_, token_, dbName);

        // Set user's Full Name in the DB
        saveFullNameAndEmail(remoteUser_, dbName);

        // Set user's Spaces in the DB
        ServerBootstrap.saveSpaces(remoteUser_, dbName);

        profileInitialized_ = true;
      }
      catch (Exception e)
      {
        profileInitialized_ = false;
      }
    }
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
}
