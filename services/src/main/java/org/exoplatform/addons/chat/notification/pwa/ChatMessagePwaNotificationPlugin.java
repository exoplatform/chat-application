/*
 * Copyright (C) 2024 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.exoplatform.addons.chat.notification.pwa;

import io.meeds.portal.permlink.model.PermanentLinkObject;
import io.meeds.portal.permlink.service.PermanentLinkService;
import io.meeds.pwa.model.PwaNotificationMessage;
import io.meeds.pwa.plugin.PwaNotificationPlugin;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.ResourceBundleService;

import static org.exoplatform.addons.chat.utils.NotificationUtils.CHAT_MESSAGE_RECEIVED_NOTIFICATION_PLUGIN;

public class ChatMessagePwaNotificationPlugin implements PwaNotificationPlugin {

	private ResourceBundleService resourceBundleService;
	private PermanentLinkService permanentLinkService;

	private static final String   TITLE_LABEL_KEY = "pwa.notification.ChatMessageReceivedNotificationPlugin.title";


	public ChatMessagePwaNotificationPlugin(ResourceBundleService resourceBundleService, PermanentLinkService permanentLinkService) {
		 this.permanentLinkService = permanentLinkService;
		 this.resourceBundleService = resourceBundleService;
	}

	@Override
	public String getId() {
		return CHAT_MESSAGE_RECEIVED_NOTIFICATION_PLUGIN ;
	}

	@Override
	public PwaNotificationMessage process(NotificationInfo notification, LocaleConfig localeConfig) {
		PwaNotificationMessage notificationMessage = new PwaNotificationMessage();

		String title = resourceBundleService.getSharedString(TITLE_LABEL_KEY, localeConfig.getLocale()).replace("{0}",notification.getValueOwnerParameter("senderFullName"))
																				.replace("{1}",notification.getValueOwnerParameter("roomName"));

		notificationMessage.setTitle(title);
		notificationMessage.setUrl(permanentLinkService.getPermanentLink(new PermanentLinkObject("chatRoom",
																																														 notification.getValueOwnerParameter("roomId"))));
		return notificationMessage;
	}

}
