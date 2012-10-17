@Application(defaultController = NotificationApplication.class)
@Portlet(name="NotificationPortlet")

@Assets(
        location = AssetLocation.SERVER,
        scripts = {
                @Script(src = "js/jquery-1.7.1.min.js"),
                @Script(src = "js/notif.js")
        },
        stylesheets = {
                @Stylesheet(src = "css/notif.css")
        }
)


package org.benjp.portlet.notification;

import juzu.Application;
import juzu.asset.AssetLocation;
import juzu.plugin.asset.Assets;
import juzu.plugin.asset.Script;
import juzu.plugin.asset.Stylesheet;
import juzu.plugin.portlet.Portlet;
import org.benjp.portlet.chat.ChatApplication;
