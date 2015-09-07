package org.exoplatform.chat.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.chat.utils.PropertyManager;
import org.exoplatform.services.security.ConversationState;

public class ChatUtils {

  /**
   * Get mongo database name for current tenant if on cloud environment
   */
  public static String getDBName() {
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
    return dbName;
  }
}
