/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.chat.services;

import org.exoplatform.chat.model.NotificationBean;
import org.exoplatform.chat.model.NotificationSettingsBean;
import org.exoplatform.chat.model.RealTimeMessageBean;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named("notificationService")
@ApplicationScoped
@Singleton
public class NotificationServiceImpl implements org.exoplatform.chat.services.NotificationService
{

  @Inject
  private RealTimeMessageService realTimeMessageService;

  @Inject
  private NotificationDataStorage storage;

  @Inject
  private UserService userService;

  public void addNotification(String receiver, String sender, String type, String category, String categoryId,
                              String content, String link) {
    storage.addNotification(receiver, sender, type, category, categoryId, content, link, null);
  }

  public void addNotification(String receiver, String sender, String type, String category, String categoryId,
                              String content, String link, String options) {
    storage.addNotification(receiver, sender, type, category, categoryId, content, link, options);

    sendNotification(receiver);
  }

  public void setNotificationsAsRead(String user, String type, String category, String categoryId)
  {
    storage.setNotificationsAsRead(user, type, category, categoryId);

    sendNotification(user);
  }

  @Override
  public List<NotificationBean> getUnreadNotifications(String user, UserService userService) {
    return getUnreadNotifications(user, userService, null, null, null);
  }

  @Override
  public List<NotificationBean> getUnreadNotifications(String user, UserService userService, String type, String category, String categoryId) {
    return storage.getUnreadNotifications(user, userService, type, category, categoryId);
  }

  public int getUnreadNotificationsTotal(String user)
  {
    return getUnreadNotificationsTotal(user, null, null, null);
  }


  public int getUnreadNotificationsTotal(String user, String type, String category, String categoryId)
  {
    return storage.getUnreadNotificationsTotal(user, type, category, categoryId);
  }

  public int getNumberOfNotifications()
  {
    return storage.getNumberOfNotifications();
  }

  public int getNumberOfUnreadNotifications()
  {
    return storage.getNumberOfUnreadNotifications();
  }

  private void sendNotification(String receiver) {
    Map<String, Object> data = new HashMap<>();

    data.put("totalUnreadMsg", getUnreadNotifications(receiver, userService).size());

    // Deliver the saved message to sender's subscribed channel itself.
    RealTimeMessageBean messageBean = new RealTimeMessageBean(
            RealTimeMessageBean.EventType.NOTIFICATION_COUNT_UPDATED,
            null,
            receiver,
            null,
            data);
    realTimeMessageService.sendMessage(messageBean, receiver);
  }

  public boolean isRoomSilentForUser(String user, String roomId) {
    NotificationSettingsBean settings = null;
    try {
      settings = userService.getUserDesktopNotificationSettings(user);
      if (settings != null && settings.getEnabledRoomTriggers() != null) {
        String bean = settings.getEnabledRoomTriggers();
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(bean);
        JSONObject room = (JSONObject) json.get(roomId);
        if (room != null && room.get("notifCond") != null && room.get("notifCond").equals("silence")) {
          return true;
        }
      }
    } catch (JSONException | ParseException e) {
      return false;
    }
    return false;
  }
}
