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

import java.util.List;

public interface NotificationService
{
  public static final String M_NOTIFICATIONS = "notifications";

  public void addNotification(String receiver, String sender, String type, String category, String categoryId, String content, String link);

  public void addNotification(String receiver, String sender, String type, String category, String categoryId, String content, String link, String options);

  public void setNotificationsAsRead(String user, String type, String category, String categoryId);

  public List<NotificationBean> getUnreadNotifications(String user, UserService userService);

  public List<NotificationBean> getUnreadNotifications(String user, UserService userService, String type, String category, String categoryId);

  public int getUnreadNotificationsTotal(String user);

  public int getUnreadNotificationsTotal(String user, String type, String category, String categoryId);

  public int getNumberOfNotifications();

  public int getNumberOfUnreadNotifications();

  public boolean isRoomSilentForUser(String user, String roomId);
}
