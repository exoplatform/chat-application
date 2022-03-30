/*
* Copyright (C) 2022 eXo Platform SAS.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see .
*/

package org.exoplatform.addons.chat.notification.plugin;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.addons.chat.model.MessageReceivedModel;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import static org.exoplatform.addons.chat.utils.NotificationUtils.*;

public class ChatMessageNotificationPlugin extends BaseNotificationPlugin{

	public ChatMessageNotificationPlugin(InitParams initParams) {
		 super(initParams);
	     ValueParam notificationIdParam = initParams.getValueParam("notification.id");
	     if (notificationIdParam == null || StringUtils.isBlank(notificationIdParam.getValue())) {
	        throw new IllegalStateException("'notification.id' parameter is mandatory");
	     }
	}

	@Override
	public String getId() {
		return CHAT_MESSAGE_RECEIVED_NOTIFICATION_PLUGIN ;
	}

	@Override
	public boolean isValid(NotificationContext ctx) {
		return true;
	}

	@Override
	protected NotificationInfo makeNotification(NotificationContext ctx) {
		 MessageReceivedModel messageModel = ctx.value(MESSAGE_RECEIVED_MODEL);  
	     NotificationInfo notification = NotificationInfo.instance();
	     notification.key(getId());
	     notification.to(messageModel.getReceivers());
	     if (notification.getSendToUserIds() == null || notification.getSendToUserIds().isEmpty()) {
	            return null;
	        } else {
	        	notification.with("isGroupeChat",String.valueOf(messageModel.getReceivers().size()>1));
	            notification.with("roomId", String.valueOf(messageModel.getRoomId()));
	            notification.with("sender", String.valueOf(messageModel.getSender()));
	            notification.with("senderFullName", String.valueOf(messageModel.getSenderFullName()));
	            notification.with("roomName", String.valueOf(messageModel.getRoomName()));
	            notification.with("message",String.valueOf(messageModel.getMessage()));
	            return notification;
	        }
	        
	}

}
