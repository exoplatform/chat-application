package org.exoplatform.addons.chat.notification.provider;

import org.exoplatform.addons.chat.notification.builder.ChatTemplateBuilder;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import static org.exoplatform.addons.chat.utils.NotificationUtils.CHAT_MENTION_KEY;
import static org.exoplatform.addons.chat.utils.NotificationUtils.CHAT_MENTION_NOTIFICATION_PLUGIN;

@TemplateConfigs(templates = {
        @TemplateConfig(pluginId = CHAT_MENTION_NOTIFICATION_PLUGIN, template = "war:/conf/chat/templates/notification/mail/ChatMailPlugin.gtmpl") })
public class MailTemplateProvider extends TemplateProvider {
    protected static Log log = ExoLogger.getLogger(MailTemplateProvider.class);
    public MailTemplateProvider(ExoContainer container, InitParams initParams) {
        super(initParams);
        this.templateBuilders.put(CHAT_MENTION_KEY, new ChatTemplateBuilder(this, container, CHAT_MENTION_KEY, false));
    }
}
