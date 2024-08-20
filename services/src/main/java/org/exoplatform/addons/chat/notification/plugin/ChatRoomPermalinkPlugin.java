package org.exoplatform.addons.chat.notification.plugin;

import io.meeds.portal.permlink.model.PermanentLinkObject;
import io.meeds.portal.permlink.plugin.PermanentLinkPlugin;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.security.Identity;

public class ChatRoomPermalinkPlugin  implements PermanentLinkPlugin {

  public static final String      OBJECT_TYPE = "chatRoom";
  public static final String                  URL_FORMAT  = "/portal/%s/?chatRoomId=%s";
  private             UserPortalConfigService portalConfigService;

  public ChatRoomPermalinkPlugin(UserPortalConfigService userPortalConfigService) {
    this.portalConfigService = userPortalConfigService;
  }
  @Override
  public String getObjectType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean canAccess(PermanentLinkObject permanentLinkObject, Identity identity) throws ObjectNotFoundException {
    return true;
  }

  @Override
  public String getDirectAccessUrl(PermanentLinkObject permanentLinkObject) throws ObjectNotFoundException {
    String roomId = permanentLinkObject.getObjectId();
    return String.format(URL_FORMAT, portalConfigService.getMetaPortal(), roomId);
  }
}
