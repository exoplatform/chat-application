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

import org.exoplatform.chat.model.MessageBean;
import org.exoplatform.chat.model.RoomBean;
import org.exoplatform.chat.model.RoomsBean;

import java.util.List;

public interface ChatService
{
  public static final String M_ROOM_PREFIX = "messages_room_";
  public static final String M_ROOMS_COLLECTION = "rooms";

  public static final String SPACE_PREFIX = "space-";
  public static final String TEAM_PREFIX = "team-";
  public static final String EXTERNAL_PREFIX = "external-";

  public static final String TYPE_ROOM_USER = "u";
  public static final String TYPE_ROOM_SPACE = "s";
  public static final String TYPE_ROOM_TEAM = "t";
  public static final String TYPE_ROOM_EXTERNAL = "e";

  public static final String TYPE_DELETED = "DELETED";
  public static final String TYPE_EDITED = "EDITED";

  public static final String DESKTOP_NOTIFICATION = "desktop";
  public static final String ON_SITE = "on-site";
  public static final String BIP = "bip";

  public static final String NOTIFY_ME_WHEN_MENTION= "notify-when-mention";
  public static final String NOTIFY_ME_EVEN_NOT_DISTRUB = "notify-even-not-distrub";

  public static final String NOTIFY_ME_ON_ROOM_KEY_WORD = "keywords";
  public static final String DO_NOT_NOTIFY_ME_ON_ROOM = "silence";
  public static final String NOTIFY_ME_ON_ROOM_NORMAL = "normal";

  public void write(String message, String user, String room, String isSystem, String dbName);

  public void write(String message, String user, String room, String isSystem, String options, String dbName);

  public void delete(String room, String user, String messageId, String dbName);

  /**
   * Delete a Team Room by its corresponding ID.<br>
   * Nothing happen if : <br>
   * <ul>
   * <li>the roomId doesn't exists</li>
   * <li>the roomId doesn't correspond to a Team Room</li>
   * <li>the specified user is not the owner of the Team Room</li>
   * </ul>
   *
   * @param roomId the team room ID to delete
   * @param user   the owner of the team room
   * @param dbName the database to use for the query
   */
  public void deleteTeamRoom(String roomId, String user, String dbName);

  public void edit(String room, String user, String messageId, String message, String dbName);

  public String read(String room, UserService userService, String dbName);

  public String read(String room, UserService userService, boolean isTextOnly, Long fromTimestamp, String dbName);

  public String read(String room, UserService userService, boolean isTextOnly, Long fromTimestamp, Long toTimestamp, String dbName);

  public MessageBean getMessage(String roomId, String messageId, String dbName);

  public String getSpaceRoom(String space, String dbName);

  public String getSpaceRoomByName(String name, String dbName);

  public String getTeamRoom(String team, String user, String dbName);

  public String getExternalRoom(String identifier, String dbName);

  public String getTeamCreator(String room, String dbName);

  public void setRoomName(String room, String name, String dbName);

  /**
   * Retrieve a Room by its ID
   * @param roomId the ID of the room
   * @param dbName the database to use for the query
   * @return the room or null if the room doesn't exists
   */
  public RoomBean getTeamRoomById(String roomId, String dbName);

  public String getRoom(List<String> users, String dbName);
  
  public String getTypeRoomChat(String roomId, String dbName);

  public List<RoomBean> getExistingRooms(String user, boolean withPublic, boolean isAdmin, NotificationService notificationService, TokenService tokenService, String dbName);

  public RoomsBean getRooms(String user, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, NotificationService notificationService, UserService userService, TokenService tokenService, String dbName);

  public RoomsBean getRooms(String user, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, int limit, NotificationService notificationService, UserService userService, TokenService tokenService, String dbName);

  public int getNumberOfRooms(String dbName);

  public int getNumberOfMessages(String dbName);

}
