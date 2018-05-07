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
@Stylesheets({
        @Stylesheet(value = "/org/exoplatform/chat/portlet/chat/assets/chat.css", location = AssetLocation.APPLICATION, id = "chat"),
        @Stylesheet(value = "css/sh_style.css", location = AssetLocation.SERVER, id="shStyle"),
        @Stylesheet(value = "/org/exoplatform/chat/portlet/chat/assets/chat-normal.css", location = AssetLocation.APPLICATION, id = "chat-normal", depends = "chat"),
        @Stylesheet(value = "/org/exoplatform/chat/portlet/chat/assets/chat-public.css", location = AssetLocation.APPLICATION, id = "chat-public", depends = "chat"),
        @Stylesheet(value = "/org/exoplatform/chat/portlet/chat/assets/chat-responsive.css", location = AssetLocation.APPLICATION, id = "chat-responsive", depends = "chat")
})

@Less(value = {"chat.less", "chat-normal.less", "chat-responsive.less", "chat-public.less"}, minify = true)
@Assets({"chat", "shStyle"})
package org.exoplatform.chat.portlet.chat;

import juzu.Application;
import juzu.asset.AssetLocation;
import juzu.plugin.asset.*;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.less.Less;
import juzu.plugin.portlet.Portlet;
import juzu.plugin.asset.Stylesheet;

