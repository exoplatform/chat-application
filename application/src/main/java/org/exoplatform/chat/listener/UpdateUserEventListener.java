package org.exoplatform.chat.listener;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.chat.utils.PropertyManager;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.security.ConversationState;

import java.util.logging.Logger;

public class UpdateUserEventListener extends UserEventListener {

  private static final Logger LOG = Logger.getLogger(UpdateUserEventListener.class.getName());

  public void postSave(User user, boolean isNew) throws Exception {
    try {
      String dbName = "";
      String prefixDB = PropertyManager.getProperty(PropertyManager.PROPERTY_DB_NAME);
      ConversationState currentState = ConversationState.getCurrent();
      if (currentState != null) {
        dbName = (String) currentState.getAttribute("currentTenant");
      }
      if (StringUtils.isEmpty(dbName)) {
        dbName = prefixDB;
      } else {
        StringBuilder sb = new StringBuilder()
                                      .append(prefixDB)
                                      .append("_")
                                      .append(dbName);
        dbName = sb.toString();
      }
      String currentUserId = currentState.getIdentity().getUserId();
      if (!isNew && user.getUserName().equals(currentUserId)) {
        String fullName = user.getFirstName().concat(" ").concat(user.getLastName());
        ServerBootstrap.addUserFullNameAndEmail(currentUserId, fullName, user.getEmail(), dbName);
      }
    } catch (Exception e) {
      LOG.warning("Can not update firstName/lastName to chatServer");
    }
  }
}
