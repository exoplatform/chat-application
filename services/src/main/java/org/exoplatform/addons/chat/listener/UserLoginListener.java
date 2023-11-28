package org.exoplatform.addons.chat.listener;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.services.listener.*;
import org.exoplatform.services.security.*;
/**
 * This listener will be used to update current user chat rooms list on login.
 */
@Asynchronous
public class UserLoginListener extends Listener<ConversationRegistry, ConversationState> {
  @Override
  public void onEvent(Event<ConversationRegistry, ConversationState> event) throws Exception {
    ConversationState conversationState = event.getData();

    String userId = conversationState == null
        || conversationState.getIdentity() == null ? null : conversationState.getIdentity().getUserId();

    if (StringUtils.isBlank(userId) || StringUtils.equalsIgnoreCase(userId, IdentityConstants.ANONIM)) {
      return;
    }
    ServerBootstrap.saveSpaces(userId);
    if (Boolean.parseBoolean(ServerBootstrap.shouldUpdate(userId))) {
      ServerBootstrap.setEnabledUser(userId, true);
    }
  }
}
