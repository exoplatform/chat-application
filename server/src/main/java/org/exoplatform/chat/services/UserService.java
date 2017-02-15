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

import org.exoplatform.chat.model.NotificationSettingsBean;
import org.exoplatform.chat.model.RoomBean;
import org.exoplatform.chat.model.SpaceBean;
import org.exoplatform.chat.model.UserBean;
import org.json.JSONException;
import org.json.simple.JSONObject;

import java.util.List;

public interface UserService
{
  public static final String STATUS_AVAILABLE = "available";
  public static final String STATUS_DONOTDISTURB = "donotdisturb";
  public static final String STATUS_AWAY = "away";
  public static final String STATUS_INVISIBLE = "invisible";
  public static final String STATUS_OFFLINE = "offline";
  public static final String STATUS_NONE = "none";
  public static final String STATUS_SPACE = "space";
  public static final String STATUS_TEAM = "team";

  public static final String ANONIM_USER = "__anonim_";
  public static final String SUPPORT_USER = "__support_";

  public static final String PREFERRED_ROOM_NOTIFICATION_TRIGGER = "preferredRoomNotificationTrigger";
  public static final String PREFERRED_NOTIFICATION = "preferredNotification";
  public static final String PREFERRED_NOTIFICATION_TRIGGER = "preferredNotificationTrigger";
  public static final String ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD = "keywords";



  public void toggleFavorite(String user, String targetUser, String dbName);

  public void setPreferredNotification(String user, String notifManner, String dbName) throws Exception;

  public void setNotificationTrigger(String user, String notifCond, String dbName) throws Exception;
  public void setRoomNotificationTrigger(String user, String room,String notifCond,String notifConditionType, String dbName, long time) throws Exception;

  public NotificationSettingsBean getUserDesktopNotificationSettings(String user, String dbName) throws JSONException;

  public boolean isFavorite(String user, String targetUser, String dbName);

  public void addUserFullName(String user, String fullname, String dbName);

  public void addUserEmail(String user, String email, String dbName);

  public void setSpaces(String user, List<SpaceBean> spaces, String dbName);

  public void addTeamRoom(String user, String teamRoomId, String dbName);

  public void addTeamUsers(String teamRoomId, List<String> users, String dbName);

  public void removeTeamUsers(String teamRoomId, List<String> users, String dbName);

  public List<RoomBean> getTeams(String user, String dbName);

  public RoomBean getRoom(String user, String roomId, String dbName);

  public List<SpaceBean> getSpaces(String user, String dbName);

  public List<UserBean> getUsersInRoomChatOneToOne(String roomId, String dbName);

  public List<UserBean> getUsers(String roomId, String dbName);

  public List<UserBean> getUsers(String filter, boolean fullBean, String dbName);

  public String setStatus(String user, String status, String dbName);

  public void setAsAdmin(String user, boolean isAdmin, String dbName);

  public boolean isAdmin(String user, String dbName);

  public String getStatus(String user, String dbName);

  public String getUserFullName(String user, String dbName);

  public UserBean getUser(String user, String dbName);

  public UserBean getUser(String user, boolean withFavorites, String dbName);

  public List<String> getUsersFilterBy(String user, String room, String type, String dbName);

  public int getNumberOfUsers(String dbName);

}
