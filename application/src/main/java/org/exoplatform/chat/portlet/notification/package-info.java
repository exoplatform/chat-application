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

@Application(defaultController = NotificationApplication.class)
@Portlet(name="NotificationPortlet")
@Bindings(
  {
    @Binding(value = org.exoplatform.services.organization.OrganizationService.class),
    @Binding(value = org.exoplatform.social.core.space.spi.SpaceService.class),
    @Binding(value = org.exoplatform.ws.frameworks.cometd.ContinuationService.class)
  }
)
@Scripts({
        @Script(value = "js/jquery-1.8.3.min.js", id = "jquery", location = AssetLocation.SERVER),
        @Script(value = "js/taffy-min.js", id="taffy", location = AssetLocation.SERVER),
        @Script(value = "js/snack-min.js", id = "snack", location = AssetLocation.SERVER),
        @Script(value = "js/jquery-juzu-utils-0.2.0.js", depends = "jquery", id = "juzu-utils", location = AssetLocation.SERVER),
        @Script(value = "js/desktopNotification.js", id="desktopNotification", depends = {"jquery"}, location = AssetLocation.SERVER ),
        @Script(value = "js/notif.js", id = "notif", location = AssetLocation.SERVER, depends = {"desktopNotification","jquery", "snack", "juzu-utils", "taffy"})
})
@Stylesheets({
        @Stylesheet(value = "/org/exoplatform/chat/portlet/notification/assets/notif.css", location = AssetLocation.APPLICATION)
})
@Less(value = "notif.less", minify = true)
@Assets("*")
package org.exoplatform.chat.portlet.notification;

import juzu.Application;
import juzu.asset.AssetLocation;
import juzu.plugin.asset.*;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.less.Less;
import juzu.plugin.portlet.Portlet;

