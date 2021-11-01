package org.exoplatform.addons.chat.utils;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

public class ChatRoomUtils {

    public static String getUserAvatar(String userName) {
        IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
        if (StringUtils.isNotBlank(userName)) {
            try {
                Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userName);
                if (identity != null) {
                    Profile profile = identity.getProfile();
                    return profile.getAvatarUrl();
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public static String getSpaceAvatar(String prettyName) {
        SpaceService spaceService = SpaceUtils.getSpaceService();
        Space space = spaceService.getSpaceByPrettyName(prettyName);
        if (space != null) {
            return space.getAvatarUrl();
        }
        return null;
    }
}
