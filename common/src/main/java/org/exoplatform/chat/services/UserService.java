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

import org.json.JSONException;

import org.exoplatform.chat.model.NotificationSettingsBean;
import org.exoplatform.chat.model.RoomBean;
import org.exoplatform.chat.model.SpaceBean;
import org.exoplatform.chat.model.UserBean;

public interface UserService
{
  public static final String STATUS_AVAILABLE = "available";
  public static final String STATUS_DONOTDISTURB = "donotdisturb";
  public static final String STATUS_AWAY = "away";
  public static final String STATUS_INVISIBLE = "invisible";
  public static final String STATUS_OFFLINE = "offline";
  public static final String STATUS_SPACE = "space";
  public static final String STATUS_TEAM = "team";

  public static final String ANONIM_USER = "__anonim_";
  public static final String SUPPORT_USER = "__support_";

  public static final String PREFERRED_ROOM_NOTIFICATION_TRIGGER = "preferredRoomNotificationTrigger";
  public static final String PREFERRED_NOTIFICATION = "preferredNotification";
  public static final String PREFERRED_NOTIFICATION_TRIGGER = "preferredNotificationTrigger";

  /**
   * @deprecated use {@link #addFavorite(String, String)} and {@link #removeFavorite(String, String)} instead
   *
   * @param user
   * @param targetUser
   */
  @Deprecated
  public void toggleFavorite(String user, String targetUser);

  public void addFavorite(String user, String room);

  public void removeFavorite(String user, String room);

  public void setPreferredNotification(String user, String notifManner) throws Exception;

  public void setNotificationTrigger(String user, String notifCond) throws Exception;
  public void setRoomNotificationTrigger(String user, String room,String notifCond,String notifConditionType, long time) throws Exception;

  public NotificationSettingsBean getUserDesktopNotificationSettings(String user) throws JSONException;

  public boolean isFavorite(String user, String targetUser);

  public void addUserFullName(String user, String fullname);

  public void addUserEmail(String user, String email);

  default public void deleteUser(String user) {
    // No default implementation to add
    throw new UnsupportedOperationException("This operation is not supported using current implementation of service UserService");
  }

  default public void setEnabledUser(String user, Boolean isEnabled) {
    // No default implementation to add
    throw new UnsupportedOperationException("This operation is not supported using current implementation of service UserService");
  }

  default public void setExternalUser(String user, String isExternal) {
    // No default implementation to add
    throw new UnsupportedOperationException("This operation is not supported using current implementation of service UserService");
  }

  public void setSpaces(String user, List<SpaceBean> spaces);

  public void addTeamRoom(String user, String teamRoomId);

  public void addTeamUsers(String teamRoomId, List<String> users);

  public void removeTeamUsers(String teamRoomId, List<String> users);

  public List<RoomBean> getTeams(String user);

  public RoomBean getRoom(String user, String roomId);

  public List<SpaceBean> getSpaces(String user);

  public List<UserBean> getUsersInRoomChatOneToOne(String roomId);

  public List<UserBean> getUsers(String roomId);

  public List<UserBean> getUsers(String filter, boolean fullBean);

  public List<UserBean> getUsers(String roomId, String filter, int limit);

  public List<UserBean> getUsers(String roomId, List<String> onlineUsers, String filter, int limit, boolean onlyOnlineUsers);

  public String setStatus(String user, String status);

  public void setAsAdmin(String user, boolean isAdmin);

  public boolean isAdmin(String user);

  public String getStatus(String user);

  public String getUserFullName(String user);

  public UserBean getUser(String user);

  public UserBean getUser(String user, boolean withFavorites);

  public List<String> getUsersFilterBy(String user, String room, String type);

  public int getNumberOfUsers();

  public long getUsersCount(String room, String filter);

  long getActiveUsersCount(String room, String filter);
}
