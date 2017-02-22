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

import org.exoplatform.chat.model.*;
import org.json.JSONException;
import org.json.simple.JSONObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Named("userService")
@ApplicationScoped
public class UserServiceImpl implements UserService {
  private static final Logger LOG = Logger.getLogger(UserServiceImpl.class.getName());

  @Inject
  private UserDataStorage userStorage;

  @Inject
  private RealTimeMessageService realTimeMessageService;

  public void toggleFavorite(String user, String targetUser, String dbName) {
    if (isFavorite(user, targetUser, dbName)) {
      removeFavorite(user, targetUser, dbName);
    } else {
      addFavorite(user, targetUser, dbName);
    }
  }

  @Override
  public void addFavorite(String user, String room, String dbname) {
    userStorage.addFavorite(user, room, dbname);

    // Deliver the saved message to sender's subscribed channel itself.
    RealTimeMessageBean messageBean = new RealTimeMessageBean(
            RealTimeMessageBean.EventType.FAVOTITE_ADDED,
            room,
            user,
            new Date(),
            null);
    realTimeMessageService.sendMessage(messageBean, user);
  }

  @Override
  public void removeFavorite(String user, String room, String dbName) {
    userStorage.removeFavorite(user, room, dbName);

    // Deliver the saved message to sender's subscribed channel itself.
    RealTimeMessageBean messageBean = new RealTimeMessageBean(
            RealTimeMessageBean.EventType.FAVORITE_REMOVED,
            room,
            user,
            new Date(),
            null);
    realTimeMessageService.sendMessage(messageBean, user);
  }

  /**
   * This methode is responsible for setting a notification channel for a specific user
   * available channels :
   *  -on-site
   *  -desktop
   *  -bip
   */
  public void setPreferredNotification(String user, String notifManner, String dbName) throws Exception {
    userStorage.setPreferredNotification(user, notifManner, dbName);
  }

  /**
   * This methode is responsible for setting a notification triggers for a specific user
   * available triggers :
   *  -mention
   *  -even-on-do-not-distrub
   *
   */
  public void setNotificationTrigger(String user, String notifCond, String dbName) throws Exception {
    userStorage.setNotificationTrigger(user, notifCond, dbName);
  }

  /**
   * This methode is responsible for setting a notification triggers for a specific user in a specific room
   * available triggers :
   *  -mention
   *  -key-words
   *
   */
  public void setRoomNotificationTrigger(String user, String room, String notifCondition, String notifConditionType, String dbName, long time) throws Exception {
    userStorage.setRoomNotificationTrigger(user, room, notifCondition, notifConditionType, dbName, time);

    JSONObject settings = new JSONObject();
    settings.put("notifCondition", notifCondition);
    settings.put("notifConditionType", notifConditionType);

    JSONObject data = new JSONObject();
    data.put("settings", settings);

    // Deliver the saved message to sender's subscribed channel itself.
    RealTimeMessageBean messageBean = new RealTimeMessageBean(
        RealTimeMessageBean.EventType.ROOM_SETTINGS_UPDATED,
        room,
        user,
        null,
        data);
    realTimeMessageService.sendMessage(messageBean, user);
  }

  /*
  * This method is responsible for getting all desktop settings in a single object
  */
  public NotificationSettingsBean getUserDesktopNotificationSettings(String user, String dbName) throws JSONException {
    return userStorage.getUserDesktopNotificationSettings(user, dbName);
  }

  public boolean isFavorite(String user, String targetUser, String dbName) {
    return userStorage.isFavorite(user, targetUser, dbName);
  }

  public void addUserFullName(String user, String fullname, String dbName) {
    userStorage.addUserFullName(user, fullname, dbName);
  }

  public void addUserEmail(String user, String email, String dbName) {
    userStorage.addUserEmail(user, email, dbName);
  }

  public void setSpaces(String user, List<SpaceBean> spaces, String dbName) {
    userStorage.setSpaces(user, spaces, dbName);
  }

  public void addTeamRoom(String user, String teamRoomId, String dbName) {
    userStorage.addTeamRoom(user, teamRoomId, dbName);
  }

  public void addTeamUsers(String teamRoomId, List<String> users, String dbName) {
    for (String user:users) {
      this.addTeamRoom(user, teamRoomId, dbName);
    }
  }

  public void removeTeamUsers(String teamRoomId, List<String> users, String dbName) {
    userStorage.removeTeamUsers(teamRoomId, users, dbName);
  }

  private RoomBean getTeam(String teamId, String dbName) {
    return getTeam(teamId, dbName);
  }

  public List<RoomBean> getTeams(String user, String dbName) {
    return userStorage.getTeams(user, dbName);
  }

  public RoomBean getRoom(String user, String roomId, String dbName) {
    return userStorage.getRoom(user, roomId, dbName);
  }

  public List<SpaceBean> getSpaces(String user, String dbName) {
    return userStorage.getSpaces(user, dbName);
  }

  public List<UserBean> getUsers(String roomId, String dbName) {
    return userStorage.getUsers(roomId, dbName);
  }
  
  public List<UserBean> getUsersInRoomChatOneToOne(String roomId, String dbName) {
    return userStorage.getUsersInRoomChatOneToOne(roomId, dbName);
  }
  
  public List<UserBean> getUsers(String filter, boolean fullBean, String dbName) {
    return userStorage.getUsers(filter, fullBean, dbName);
  }

  public String setStatus(String user, String status, String dbName) {
    return userStorage.setStatus(user, status, dbName);
  }

  public void setAsAdmin(String user, boolean isAdmin, String dbName) {
    userStorage.setAsAdmin(user, isAdmin, dbName);
  }

  public boolean isAdmin(String user, String dbName) {
    return userStorage.isAdmin(user, dbName);
  }

  public String getStatus(String user, String dbName) {
    return userStorage.getStatus(user, dbName);
  }

  public String getUserFullName(String user, String dbName)
  {
    return userStorage.getUserFullName(user, dbName);
  }

  public UserBean getUser(String user, String dbName)
  {
    return getUser(user, false, dbName);
  }

  public UserBean getUser(String user, boolean withFavorites, String dbName) {
    return userStorage.getUser(user, withFavorites, dbName);
  }

  public List<String> getUsersFilterBy(String user, String room, String type, String dbName) {
    return userStorage.getUsersFilterBy(user, room, type, dbName);
  }

  public int getNumberOfUsers(String dbName) {
    return userStorage.getNumberOfUsers(dbName);
  }
}
