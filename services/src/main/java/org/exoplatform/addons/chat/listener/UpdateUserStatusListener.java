package org.exoplatform.addons.chat.listener;

import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.security.*;
import org.exoplatform.services.security.web.HttpSessionStateKey;
import org.exoplatform.services.user.UserStateModel;
import org.exoplatform.services.user.UserStateService;
import org.exoplatform.web.login.LogoutControl;

/**
 * This listener will be used to update chat status of current user in the case
 * session destroyed.
 */
public class UpdateUserStatusListener extends Listener<PortalContainer, HttpSessionEvent> {
  public static final String STATUS_OFFLINE = "offline";

  @Override
  public void onEvent(Event<PortalContainer, HttpSessionEvent> event) {
    ConversationState conversationState = ConversationState.getCurrent();

    String userId = conversationState == null
        || conversationState.getIdentity() == null ? null : conversationState.getIdentity().getUserId();

    if (userId == null) {
      return;
    }

    PortalContainer portalContainer = event.getSource();
    if (LogoutControl.isLogoutRequired() && portalContainer.isStarted()) {
      // Send logout message to all sessions of the given user in case of a
      // logout, not in platform stop.
      String token = ServerBootstrap.getToken(userId);

      ConversationRegistry conversationRegistry = portalContainer.getComponentInstanceOfType(ConversationRegistry.class);
      List<StateKey> stateKeys = conversationRegistry.getStateKeys(userId);
      stateKeys.remove(conversationState);

      HttpSessionEvent sessionEvent = event.getData();
      HttpSession httpSession = sessionEvent.getSession();
      StateKey httpStateKey = new HttpSessionStateKey(httpSession);
      String sessionId = httpSession.getId();
      boolean isSingleSession = stateKeys.stream().filter(stateKey -> !stateKey.equals(httpStateKey)).count() == 0;
      ServerBootstrap.logout(userId, token, sessionId, isSingleSession);
      if (isSingleSession) {
        UserStateService userStateService = portalContainer.getComponentInstanceOfType(UserStateService.class);
        UserStateModel userStateModel = userStateService.getUserState(userId);
        if (userStateModel != null) {
          userStateModel.setLastActivity(Calendar.getInstance().getTimeInMillis() - userStateService.getDelay());
          userStateModel.setStatus(STATUS_OFFLINE);
          userStateService.save(userStateModel);
        }
      }
    }
  }
}
