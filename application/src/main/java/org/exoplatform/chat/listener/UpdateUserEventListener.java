package org.exoplatform.chat.listener;

import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.security.ConversationState;

import java.util.logging.Logger;

public class UpdateUserEventListener extends UserEventListener {

  private static final Logger LOG = Logger.getLogger(UpdateUserEventListener.class.getName());

  public void postSave(User user, boolean isNew) throws Exception {
    try {
      String currentUserId = ConversationState.getCurrent().getIdentity().getUserId();
      if (!isNew && user.getUserName().equals(currentUserId)) {
        String fullName = user.getFirstName().concat(" ").concat(user.getLastName());
        ServerBootstrap.addUserFullNameAndEmail(currentUserId, fullName, user.getEmail());
      }
    } catch (Exception e) {
      LOG.warning("Can not update firstName/lastName to chatServer");
    }
  }
}
