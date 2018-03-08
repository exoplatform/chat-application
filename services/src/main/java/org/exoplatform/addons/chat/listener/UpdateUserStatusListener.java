package org.exoplatform.addons.chat.listener;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.user.UserStateModel;
import org.exoplatform.services.user.UserStateService;
import org.exoplatform.web.login.LogoutControl;

import java.util.Calendar;


/**
 * This listener will be used to update chat status of current user in the case session destroyed.
 */
public class UpdateUserStatusListener extends Listener<ConversationRegistry, ConversationState> {
    public static final String STATUS_OFFLINE = "offline";

    @Override
    public void onEvent(Event<ConversationRegistry, ConversationState> event) throws Exception {
        UserStateService userStateService = CommonsUtils.getService(UserStateService.class);
        ConversationState data = event.getData();
        if (data != null) {
            Identity identity = data.getIdentity();
            if (identity != null) {
                String userId = identity.getUserId();
                UserStateModel userStateModel = userStateService.getUserState(userId);
                if (userStateModel == null) return;
                userStateModel.setLastActivity(Calendar.getInstance().getTimeInMillis() - userStateService.getDelay());
                userStateModel.setStatus(STATUS_OFFLINE);
                userStateService.save(userStateModel);

                if(LogoutControl.isLogoutRequired() && PortalContainer.getInstance().isStarted()) {
                    // Send logout message to all sessions of the given user in case of a logout, not in platform stop.
                    String token = ServerBootstrap.getToken(userId);
                    String dbName = ServerBootstrap.getDBName();
                    ServerBootstrap.logout(userId, token, dbName);
                }
            }
        }

    }
}
