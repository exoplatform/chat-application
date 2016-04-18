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

import java.util.List;
import java.util.Map;

import org.exoplatform.chat.model.NotificationBean;

public interface NotificationService
{
  public static final String M_NOTIFICATIONS = "notifications";

  public void addNotification(String user, String from, String type, String category, String categoryId, String content, String link, String dbName);

  public void addNotification(String user, String from, String type, String category, String categoryId, String content, String link, String options, String dbName);

  public void setNotificationsAsRead(String user, String type, String category, String categoryId, String dbName);

  public List<NotificationBean> getUnreadNotifications(String user, UserService userService, String dbName);

  public List<NotificationBean> getUnreadNotifications(String user, UserService userService, String type, String category, String categoryId, String dbName);

  public int getUnreadNotificationsTotal(String user, String dbName);

  public int getUnreadNotificationsTotal(String user, String type, String category, String categoryId, String dbName);

  public Map<String, Integer> getUnreadNotificationsTotalByCat(String user, String type, String category, String[] categoryIds, String dbName);

  public int getNumberOfNotifications(String dbName);

  public int getNumberOfUnreadNotifications(String dbName);

}
