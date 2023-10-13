package org.exoplatform.addons.chat.notification.plugin;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.addons.chat.model.MentionModel;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;

import static org.exoplatform.addons.chat.utils.NotificationUtils.*;

public class ChatNotificationPlugin extends BaseNotificationPlugin {

    public ChatNotificationPlugin(InitParams initParams) {
        super(initParams);
        ValueParam notificationIdParam = initParams.getValueParam("notification.id");
        if (notificationIdParam == null || StringUtils.isBlank(notificationIdParam.getValue())) {
            throw new IllegalStateException("'notification.id' parameter is mandatory");
        }
    }

    @Override
    public String getId() {
        return CHAT_MENTION_NOTIFICATION_PLUGIN;
    }

    @Override
    public boolean isValid(NotificationContext ctx) {
        return true;
    }

    @Override
    public NotificationInfo makeNotification(NotificationContext ctx) {
        MentionModel mention = ctx.value(MENTION_MODEL);
        NotificationInfo notification = NotificationInfo.instance();
        notification.key(getId());
        setNotificationRecipients(notification, mention.getMentionedUsers());

        if (notification.getSendToUserIds() == null || notification.getSendToUserIds().isEmpty()) {
            return null;
        } else {
            notification.with("roomId", String.valueOf(mention.getRoomId()));
            notification.with("sender", String.valueOf(mention.getSender()));
            notification.with("senderFullName", String.valueOf(mention.getSenderFullName()));
            notification.with("roomName", String.valueOf(mention.getRoomName()));
            notification.with("chatUrl", getRoomURL(mention.getRoomId()));
            notification.with("avatar", ChatService.USER_AVATAR_URL.replace("{}", mention.getSender()));
            return notification;
        }
    }
}
