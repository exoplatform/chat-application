package org.exoplatform.addons.chat.listener;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipEventListener;

public class ExternalUserListener extends MembershipEventListener {

  private static final Log LOG = ExoLogger.getLogger(ExternalUserListener.class);
  private static final String PLATFORM_EXTERNALS_GROUP  = "/platform/externals";


  @Override
  public void postSave(Membership m, boolean isNew) {
    if (m.getGroupId().equals(PLATFORM_EXTERNALS_GROUP)) {
      try {
        ServerBootstrap.setExternalUser(m.getUserName(),"true");
      } catch (Exception e) {
        LOG.error("Error while saving the external property for user  {}", m.getUserName(), e);
      }
    }
  }

  @Override
  public void postDelete(Membership m) {
    if (m.getGroupId().equals(PLATFORM_EXTERNALS_GROUP)) {
      try {
        ServerBootstrap.setExternalUser(m.getUserName(),"false");
      } catch (Exception e) {
        LOG.error("Error while saving the external property for user  {}", m.getUserName(), e);
      }
    }
  }

}
