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
    @Binding(value = org.exoplatform.social.core.space.spi.SpaceService.class)
  }
)

@Assets(
        location = AssetLocation.SERVER,
        scripts = {
                @Script(src = "js/jquery-1.8.3.min.js", id = "jquery"),
                @Script(src = "js/taffy-min.js", id="taffy"),
                @Script(src = "js/snack-min.js", id = "snack"),
                @Script(src = "js/jquery-juzu-utils-0.1.0.js", depends = "jquery", id = "juzu-utils"),
                @Script(src = "js/Modal.js", depends = "jquery"),
                @Script(src = "js/notif.js", id = "notif", depends = {"jquery", "snack", "juzu-utils", "taffy"}),
                @Script(src = "js/jquery.filedrop.js", id = "filedrop", depends = "notif")
        },
        stylesheets = {
                @Stylesheet(src = "css/webrtc.css"),
                @Stylesheet(src = "/org/exoplatform/chat/portlet/notification/assets/notif.css", location = AssetLocation.APPLICATION)
        }
)

@Less(value = "notif.less", minify = true)

package org.exoplatform.chat.portlet.notification;

import juzu.Application;
import juzu.asset.AssetLocation;
import juzu.plugin.asset.Assets;
import juzu.plugin.asset.Script;
import juzu.plugin.asset.Stylesheet;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.less.Less;
import juzu.plugin.portlet.Portlet;

