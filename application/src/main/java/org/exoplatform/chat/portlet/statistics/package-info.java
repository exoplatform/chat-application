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

@Application(defaultController = StatisticsApplication.class)
@Portlet(name="StatisticsPortlet")

@Scripts({
        @Script(value = "js/jquery-1.8.3.min.js", id = "jquery", location = AssetLocation.SERVER),
        @Script(value = "js/stats.js", depends = "jquery", location = AssetLocation.SERVER)
})
@Stylesheets({
        @Stylesheet(value = "/org/exoplatform/chat/portlet/statistics/assets/statistics.css", location = AssetLocation.APPLICATION)
})
/*@Assets(
        location = AssetLocation.SERVER,
        scripts = {
                @Script(src = "js/jquery-1.8.3.min.js", id = "jquery"),
                @Script(src = "js/stats.js", depends = "jquery")
        },
        stylesheets = {
                @Stylesheet(src = "/org/exoplatform/chat/portlet/statistics/assets/statistics.css", location = AssetLocation.APPLICATION)
        }
)*/

@Less(value = "statistics.less", minify = true)
@Assets("*")
package org.exoplatform.chat.portlet.statistics;

import juzu.Application;
import juzu.asset.AssetLocation;
import juzu.plugin.asset.*;
import juzu.plugin.less.Less;
import juzu.plugin.portlet.Portlet;
