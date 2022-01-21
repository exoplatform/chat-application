package org.exoplatform.addons.chat.listener;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.security.ConversationState;

public class UpdateUserEventListener extends UserEventListener {

  private static final Log LOG = ExoLogger.getLogger(UpdateUserEventListener.class.getName());

  private PortalContainer  container;

  public UpdateUserEventListener(PortalContainer container) {
    this.container = container;
  }

  public void postSave(User user, boolean isNew) {
    if (!this.container.isStarted()) {
      return;
    }
    try {
      ServerBootstrap.addUserFullNameAndEmail(user.getUserName(), user.getDisplayName(), user.getEmail());
    } catch (Exception e) {
      LOG.warn("Can not update firstName/lastName to chatServer", e);
    }
  }

  @Override
  public void postDelete(User user) throws Exception {
    if (!this.container.isStarted()) {
      return;
    }
    try {
      ServerBootstrap.deleteUser(user.getUserName());
    } catch (Exception e) {
      LOG.warn("Can not delete user {} from chatServer", user.getUserName(), e);
    }
  }

  @Override
  public void postSetEnabled(User user) throws Exception {
    if (!this.container.isStarted()) {
      return;
    }
    Boolean enabled = user.isEnabled();
    try {
      ServerBootstrap.setEnabledUser(user.getUserName(), enabled);
    } catch (Exception e) {
      LOG.warn("Can not delete user {} from chatServer", user.getUserName(), e);
    }
  }
}
