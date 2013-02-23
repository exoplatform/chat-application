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

package org.benjp.portlet.statistics;

import juzu.*;
import juzu.plugin.ajax.Ajax;
import juzu.request.HttpContext;
import juzu.request.RenderContext;
import juzu.template.Template;
import org.benjp.listener.ServerBootstrap;
import org.benjp.model.SpaceBean;
import org.benjp.utils.PropertyManager;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SessionScoped
public class StatisticsApplication
{

  @Inject
  @Path("index.gtmpl")
  Template index;

  @View
  public void index(RenderContext renderContext) throws IOException
  {
    String chatServerURL = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_SERVER_URL);

    index.with().set("chatServerURL", chatServerURL).render();
  }

}
