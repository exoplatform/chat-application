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

import com.mongodb.*;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.model.NotificationBean;
import org.exoplatform.chat.model.RealTimeMessageBean;
import org.exoplatform.chat.model.RoomBean;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.RealTimeMessageService;
import org.exoplatform.chat.services.UserService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.*;

@Named("notificationService")
@ApplicationScoped
@Singleton
public class NotificationServiceImpl implements org.exoplatform.chat.services.NotificationService
{
  @Inject
  private RealTimeMessageService realTimeMessageService;

  @Inject
  private NotificationDataStorage storage;

  public void addNotification(String receiver, String sender, String type, String category, String categoryId,
                              String content, String link, String dbName) {
    storage.addNotification(receiver, sender, type, category, categoryId, content, link, null, dbName);
  }

  public void addNotification(String receiver, String sender, String type, String category, String categoryId,
                              String content, String link, String options, String dbName) {
    storage.addNotification(receiver, sender, type, category, categoryId, content, link, options, dbName);

    sendNotification(receiver, dbName);
  }

  public void setNotificationsAsRead(String user, String type, String category, String categoryId, String dbName)
  {
    storage.setNotificationsAsRead(user, type, category, categoryId, dbName);

    sendNotification(user, dbName);
  }

  @Override
  public List<NotificationBean> getUnreadNotifications(String user, UserService userService, String dbName) {
    return getUnreadNotifications(user, userService, null, null, null, dbName);
  }

  @Override
  public List<NotificationBean> getUnreadNotifications(String user, UserService userService, String type, String category, String categoryId, String dbName) {
    return storage.getUnreadNotifications(user, userService, type, category, categoryId, dbName);
  }

  public int getUnreadNotificationsTotal(String user, String dbName)
  {
    return getUnreadNotificationsTotal(user, null, null, null, dbName);
  }


  public int getUnreadNotificationsTotal(String user, String type, String category, String categoryId, String dbName)
  {
    return storage.getUnreadNotificationsTotal(user, type, category, categoryId, dbName);
  }

  public int getNumberOfNotifications(String dbName)
  {
    return storage.getNumberOfNotifications(dbName);
  }

  public int getNumberOfUnreadNotifications(String dbName)
  {
    return storage.getNumberOfUnreadNotifications(dbName);
  }

  private void sendNotification(String receiver, String dbName) {
    Map<String, Object> data = new HashMap<>();
    data.put("totalUnreadMsg", getUnreadNotificationsTotal(receiver, dbName));

    // Deliver the saved message to sender's subscribed channel itself.
    RealTimeMessageBean messageBean = new RealTimeMessageBean(
        RealTimeMessageBean.EventType.NOTIFICATION_COUNT_UPDATED,
        null,
        receiver,
        null,
        data);
    realTimeMessageService.sendMessage(messageBean, receiver);
  }
}
