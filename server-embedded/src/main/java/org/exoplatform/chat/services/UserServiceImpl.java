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
import javax.inject.Singleton;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Named("userService")
@ApplicationScoped
@Singleton
public class UserServiceImpl implements UserService {
  private static final Logger LOG = Logger.getLogger(UserServiceImpl.class.getName());

  @Inject
  private UserDataStorage userStorage;

  @Inject
  private RealTimeMessageService realTimeMessageService;

  public void toggleFavorite(String user, String targetUser) {
    if (isFavorite(user, targetUser)) {
      removeFavorite(user, targetUser);
    } else {
      addFavorite(user, targetUser);
    }
  }

  @Override
  public void addFavorite(String user, String room) {
    userStorage.addFavorite(user, room);

    // Deliver the saved message to sender's subscribed channel itself.
    RealTimeMessageBean messageBean = new RealTimeMessageBean(
            RealTimeMessageBean.EventType.FAVORITE_ADDED,
            room,
            user,
            new Date(),
            null);
    realTimeMessageService.sendMessage(messageBean, user);
  }

  @Override
  public void removeFavorite(String user, String room) {
    userStorage.removeFavorite(user, room);

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
  public void setPreferredNotification(String user, String notifManner) throws Exception {
    userStorage.setPreferredNotification(user, notifManner);
  }

  /**
   * This methode is responsible for setting a notification triggers for a specific user
   * available triggers :
   *  -mention
   *  -even-on-do-not-disturb
   *
   */
  public void setNotificationTrigger(String user, String notifCond) throws Exception {
    userStorage.setNotificationTrigger(user, notifCond);
  }

  /**
   * This methode is responsible for setting a notification triggers for a specific user in a specific room
   * available triggers :
   *  -mention
   *  -key-words
   *
   */
  public void setRoomNotificationTrigger(String user, String room, String notifCondition, String notifConditionType, long time) throws Exception {
    userStorage.setRoomNotificationTrigger(user, room, notifCondition, notifConditionType, time);
  }

  /*
  * This method is responsible for getting all desktop settings in a single object
  */
  public NotificationSettingsBean getUserDesktopNotificationSettings(String user) throws JSONException {
    return userStorage.getUserDesktopNotificationSettings(user);
  }

  public boolean isFavorite(String user, String targetUser) {
    return userStorage.isFavorite(user, targetUser);
  }

  public void addUserFullName(String user, String fullname) {
    userStorage.addUserFullName(user, fullname);
  }

  public void addUserEmail(String user, String email) {
    userStorage.addUserEmail(user, email);
  }

  public void deleteUser(String user) {
    userStorage.deleteUser(user);
  }

  public void setEnabledUser(String user, Boolean isEnabled) {
    userStorage.setEnabledUser(user, isEnabled);
  }

  public void setSpaces(String user, List<SpaceBean> spaces) {
    userStorage.setSpaces(user, spaces);
  }

  public void addTeamRoom(String user, String teamRoomId) {
    userStorage.addTeamRoom(user, teamRoomId);

    // Send a websocket message of type 'room-member-joined' to the new room member
    JSONObject data = getRoom(user, teamRoomId).toJSONObject();
    RealTimeMessageBean joinRoomMessage = new RealTimeMessageBean(
        RealTimeMessageBean.EventType.ROOM_MEMBER_JOIN,
        teamRoomId,
        user,
        new Date(),
        data);
    realTimeMessageService.sendMessage(joinRoomMessage, user);
  }

  public void addTeamUsers(String teamRoomId, List<String> users) {
    for (String user:users) {
      this.addTeamRoom(user, teamRoomId);
    }
  }

  public void removeTeamUsers(String teamRoomId, List<String> users) {
    userStorage.removeTeamUsers(teamRoomId, users);
  }

  public List<RoomBean> getTeams(String user) {
    return userStorage.getTeams(user);
  }

  public RoomBean getRoom(String user, String roomId) {
    return userStorage.getRoom(user, roomId);
  }

  public List<SpaceBean> getSpaces(String user) {
    return userStorage.getSpaces(user);
  }

  public List<UserBean> getUsersInRoomChatOneToOne(String roomId) {
    return userStorage.getUsersInRoomChatOneToOne(roomId);
  }

  public List<UserBean> getUsers(String roomId) {
    return userStorage.getUsers(roomId, null, 0);
  }

  public List<UserBean> getUsers(String filter, boolean fullBean) {
    return userStorage.getUsers(null, filter, 0);
  }

  public List<UserBean> getUsers(String roomId, String filter, int limit) {
    return userStorage.getUsers(roomId, filter, limit);
  }

  public List<UserBean> getUsers(String roomId, List<String> onlineUsers, String filter, int limit) {
    return userStorage.getUsers(roomId, onlineUsers, filter, limit);
  }

  public String setStatus(String user, String status) {
    return userStorage.setStatus(user, status);
  }

  public void setAsAdmin(String user, boolean isAdmin) {
    userStorage.setAsAdmin(user, isAdmin);
  }

  public boolean isAdmin(String user) {
    return userStorage.isAdmin(user);
  }

  public String getStatus(String user) {
    return userStorage.getStatus(user);
  }

  public String getUserFullName(String user)
  {
    return userStorage.getUserFullName(user);
  }

  public UserBean getUser(String user)
  {
    return getUser(user, false);
  }

  public UserBean getUser(String user, boolean withFavorites) {
    return userStorage.getUser(user, withFavorites);
  }

  public List<String> getUsersFilterBy(String user, String room, String type) {
    return userStorage.getUsersFilterBy(user, room, type);
  }

  public int getNumberOfUsers() {
    return userStorage.getNumberOfUsers();
  }

  @Override
  public long getUsersCount(String room, String filter) {
    return userStorage.getUsersCount(room, filter);
  }
}
