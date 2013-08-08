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
import org.benjp.model.RoomsBean;

import java.util.List;

public interface ChatService
{
  public static final String M_ROOM_PREFIX = "room_";
  public static final String M_ROOMS_COLLECTION = "rooms";

  public static final String SPACE_PREFIX = "space-";
  public static final String TEAM_PREFIX = "team-";

  public static final String TYPE_ROOM_USER = "u";
  public static final String TYPE_ROOM_SPACE = "s";
  public static final String TYPE_ROOM_TEAM = "t";

  public static final String TYPE_DELETED = "DELETED";
  public static final String TYPE_EDITED = "EDITED";

  public void write(String message, String user, String room, String isSystem);

  public void write(String message, String user, String room, String isSystem, String options);

  public void delete(String room, String user, String messageId);

  public void edit(String room, String user, String messageId, String message);

  public String read(String room, UserService userService);

  public String read(String room, UserService userService, boolean isTextOnly, Long fromTimestamp);

  public String getSpaceRoom(String space);

  public String getTeamRoom(String team, String user);

  public void setRoomName(String room, String name);

  public String getRoom(List<String> users);

  public List<RoomBean> getExistingRooms(String user, boolean withPublic, boolean isAdmin, NotificationService notificationService, TokenService tokenService);

  public RoomsBean getRooms(String user, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, NotificationService notificationService, UserService userService, TokenService tokenService);

  public int getNumberOfRooms();

  public int getNumberOfMessages();

}
