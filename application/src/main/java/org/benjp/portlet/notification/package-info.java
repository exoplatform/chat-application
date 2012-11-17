@Application(defaultController = NotificationApplication.class)
@Portlet(name="NotificationPortlet")
@Bindings(
  {
    @Binding(value = org.exoplatform.services.organization.OrganizationService.class, implementation=GateInMetaProvider.class),
    @Binding(value = org.exoplatform.social.core.space.spi.SpaceService.class, implementation=GateInMetaProvider.class)
  }
)

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
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.portlet.Portlet;
