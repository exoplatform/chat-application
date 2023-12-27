package org.exoplatform.addons.chat.notification.builder;

import groovy.text.GStringTemplateEngine;
import groovy.text.Template;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.Writer;

import static org.exoplatform.addons.chat.utils.NotificationUtils.buildTemplateParameters;

public class ChatTemplateBuilder extends AbstractTemplateBuilder {

    private static final Log   LOG = ExoLogger.getLogger(ChatTemplateBuilder.class);

    private TemplateProvider   templateProvider;

    private ExoContainer       container;

    private boolean            isPushNotification;

    private PluginKey          key;

    public ChatTemplateBuilder(TemplateProvider templateProvider,
                               ExoContainer container,
                               PluginKey key,
                               boolean pushNotification) {
        this.templateProvider = templateProvider;
        this.container = container;
        this.isPushNotification = pushNotification;
        this.key = key;
    }

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
        NotificationInfo notification = ctx.getNotificationInfo();

        RequestLifeCycle.begin(container);
        try {
            TemplateContext templateContext = buildTemplateParameters(templateProvider, notification);
            if(this.isPushNotification)
            	templateContext.put("NOTIF_TYPE",this.key.getId());
            String subject = templateContext.get("CHAT_URL").toString();
            String body = TemplateUtils.processGroovy(templateContext);
            //binding the exception throws by processing template
            ctx.setException(templateContext.getException());
            MessageInfo messageInfo = new MessageInfo();
            return messageInfo.subject(subject).body(body).end();
        } catch (Throwable e) {
            ctx.setException(e);
            logException(notification, e);
            return null;
        } finally {
            RequestLifeCycle.end();
        }
    }

    @Override
    protected boolean makeDigest(NotificationContext notificationContext, Writer writer) {
        return false;
    }

    private void logException(NotificationInfo notification, Throwable e) {
        if (e != null) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("Error building notification content: {}", notification, e);
            } else {
                LOG.warn("Error building notification content: {}, error: {}", notification, e.getMessage());
            }
        }
    }

    @Override
    public Template getTemplateEngine() {
        String templatePath = null;
        try {
            templatePath = templateProvider.getTemplateFilePathConfigs().get(key);
            String template = TemplateUtils.loadGroovyTemplate(templatePath);
            if (StringUtils.isBlank(template)) {
                throw new IllegalStateException("Template with path " + templatePath + " wasn't found");
            }
            return new GStringTemplateEngine().createTemplate(template);
        } catch (Exception e) {
            LOG.warn("Error while compiling template {}", templatePath, e);
            try {
                return new GStringTemplateEngine().createTemplate("");
            } catch (Exception e1) {
                return null;
            }
        }
    }
}