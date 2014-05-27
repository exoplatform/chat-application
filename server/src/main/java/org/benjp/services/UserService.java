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

package org.benjp.services;

import org.benjp.model.RoomBean;
import org.benjp.model.SpaceBean;
import org.benjp.model.UserBean;

import java.util.List;

public interface UserService
{

  public static final String M_USERS_COLLECTION = "users";
  public static final String M_ROOMS_COLLECTION = "room_rooms";

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


  public void toggleFavorite(String user, String targetUser);

  public boolean isFavorite(String user, String targetUser);

  public void addUserFullName(String user, String fullname);

  public void addUserEmail(String user, String email);

  public void setSpaces(String user, List<SpaceBean> spaces);

  public void addTeamRoom(String user, String teamRoomId);

  public void addTeamUsers(String teamRoomId, List<String> users);

  public void removeTeamUsers(String teamRoomId, List<String> users);

  public List<RoomBean> getTeams(String user);

  public RoomBean getRoom(String user, String roomId);

  public List<SpaceBean> getSpaces(String user);

  public List<UserBean> getUsers(String spaceId);

  public List<UserBean> getUsers(String filter, boolean fullBean);

  public String setStatus(String user, String status);

  public void setAsAdmin(String user, boolean isAdmin);

  public boolean isAdmin(String user);

  public String getStatus(String user);

  public String getUserFullName(String user);

  public UserBean getUser(String user);

  public UserBean getUser(String user, boolean withFavorites);

  public List<String> getUsersFilterBy(String user, String room, String type);

  public int getNumberOfUsers();

}
