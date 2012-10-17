@Application(defaultController = ChatServer.class)
@Portlet(name="ChatPortlet")

@Assets(
        location = AssetLocation.SERVER,
        scripts = {
                @Script(src = "js/jquery-1.7.1.min.js"),
                @Script(src = "js/chat.js")
        },
        stylesheets = {
                @Stylesheet(src = "/org/benjp/assets/bootstrap.css", location = AssetLocation.CLASSPATH),
                @Stylesheet(src = "css/chat.css")
        }
)


package org.benjp.portlet.chat;

import juzu.Application;
import juzu.asset.AssetLocation;
import juzu.plugin.asset.Assets;
import juzu.plugin.asset.Script;
import juzu.plugin.asset.Stylesheet;
import juzu.plugin.portlet.Portlet;
