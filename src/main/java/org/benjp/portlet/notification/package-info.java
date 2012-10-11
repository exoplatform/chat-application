@Application(defaultController = ChatApplication.class)
@Portlet(name="ChatPortlet")

@Assets(
        location = AssetLocation.SERVER,
        scripts = {
                @Script(src = "js/jquery-1.7.1.min.js"),
                @Script(src = "js/main.js")
        },
        stylesheets = {
                @Stylesheet(src = "css/main.css")
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
