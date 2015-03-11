package org.exoplatform.chat.listener;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.chat.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.security.ConversationState;

import java.util.logging.Logger;

import javax.jcr.RepositoryException;

public class UpdateUserEventListener extends UserEventListener {

  private static final Logger LOG = Logger.getLogger(UpdateUserEventListener.class.getName());

  public void postSave(User user, boolean isNew) throws Exception {
    try {
      RepositoryService repoService = (RepositoryService) ExoContainerContext.getCurrentContainer()
                                                                      .getComponentInstanceOfType(RepositoryService.class);
      String dbName = "";
      try {
        dbName = repoService.getCurrentRepository().getConfiguration().getName();
      } catch(RepositoryException e) {
        LOG.warning("Cannot get current repository " + e.getMessage());
      }
      if (StringUtils.isEmpty(dbName)) {
        dbName = PropertyManager.getProperty(PropertyManager.PROPERTY_DB_NAME);
      }
      String currentUserId = ConversationState.getCurrent().getIdentity().getUserId();
      if (!isNew && user.getUserName().equals(currentUserId)) {
        String fullName = user.getFirstName().concat(" ").concat(user.getLastName());
        ServerBootstrap.addUserFullNameAndEmail(currentUserId, fullName, user.getEmail(), dbName);
      }
    } catch (Exception e) {
      LOG.warning("Can not update firstName/lastName to chatServer");
    }
  }
}
