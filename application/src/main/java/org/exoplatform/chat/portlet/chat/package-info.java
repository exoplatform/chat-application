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



@Application(defaultController = ChatApplication.class)
@Portlet(name="ChatPortlet")
@Bindings(
        {
                @Binding(value = org.exoplatform.services.jcr.RepositoryService.class),
                @Binding(value = org.exoplatform.services.jcr.ext.app.SessionProviderService.class),
                @Binding(value = org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator.class),
                @Binding(value = org.exoplatform.services.organization.OrganizationService.class),
                @Binding(value = org.exoplatform.social.core.space.spi.SpaceService.class),
                @Binding(value = org.exoplatform.calendar.service.CalendarService.class),
                @Binding(value = org.exoplatform.ws.frameworks.cometd.ContinuationService.class),
                @Binding(value = org.exoplatform.wiki.service.WikiService.class),
                @Binding(value = org.exoplatform.services.listener.ListenerService.class),
                @Binding(value = org.exoplatform.commons.api.ui.PlugableUIService.class)
        }
)
@Scripts(location = AssetLocation.SERVER,
        value =  {
            @Script(value = "js/jquery-1.8.3.min.js", id = "jquery", location = AssetLocation.SERVER),
            @Script(value = "js/jquery-juzu-utils-0.2.0.js", depends = "jquery", id = "juzu-utils", location = AssetLocation.SERVER),
            @Script(value = "js/taffy-min.js", id="taffy"),
            @Script(value = "js/UIUserProfilePopup.js", id="chatUserPopupPlugin", depends = {"jquery"} ),
            @Script(value = "js/chat.js", id="chatJs", depends = {"chat-modules", "chat_notif", "chatUserPopupPlugin", "jquery", "taffy"} ),
            @Script(value = "js/chat-modules.js", id="chat-modules", depends = {"jquery", "desktopNotification", "taffy"} ),
            @Script(value = "js/switch_button.js", id="chat_notif", depends = {"jquery"} ), 
            @Script(value = "js/desktopNotification.js", id="desktopNotification", depends = {"jquery"} ),
            @Script(value = "js/sh_main.min.js", id="shMain"),
            @Script(value = "js/sh_html.min.js", id="shHtml"),
            @Script(value = "js/sh_java.min.js", id="shJava"),
            @Script(value = "js/sh_javascript.min.js", id="shJs"),
            @Script(value = "js/sh_css.min.js", id="shCss")
        }
)
@Stylesheets({
        @Stylesheet(value = "/org/exoplatform/chat/portlet/chat/assets/chat.css", location = AssetLocation.APPLICATION, id = "chat"),
        @Stylesheet(value = "css/sh_style.css", location = AssetLocation.SERVER, id="shStyle"),
        @Stylesheet(value = "/org/exoplatform/chat/portlet/chat/assets/chat-normal.css", location = AssetLocation.APPLICATION, id = "chat-normal", depends = "chat"),
        @Stylesheet(value = "/org/exoplatform/chat/portlet/chat/assets/chat-public.css", location = AssetLocation.APPLICATION, id = "chat-public", depends = "chat"),
        @Stylesheet(value = "/org/exoplatform/chat/portlet/chat/assets/chat-responsive.css", location = AssetLocation.APPLICATION, id = "chat-responsive", depends = "chat")
})

@Less(value = {"chat.less", "chat-normal.less", "chat-responsive.less", "chat-public.less"}, minify = true)
@Assets({"chatJs", "shMain", "shHtml", "shJava", "shJs", "shCss", "chat", "shStyle"})
package org.exoplatform.chat.portlet.chat;

import juzu.Application;
import juzu.asset.AssetLocation;
import juzu.plugin.asset.*;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.less.Less;
import juzu.plugin.portlet.Portlet;
import juzu.plugin.asset.Stylesheet;

