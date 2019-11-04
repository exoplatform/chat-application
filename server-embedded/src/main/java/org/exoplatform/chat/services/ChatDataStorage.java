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

public interface ChatDataStorage
{
  public void write(String message, String user, String room, String isSystem);

  public void write(String message, String user, String room, String isSystem, String options);

  public String save(String message, String user, String room, String isSystem, String options);

  public void delete(String room, String user, String messageId);

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
   */
  public void deleteTeamRoom(String roomId, String user);

  public void edit(String room, String user, String messageId, String message);

  public String read(String room);

  public String read(String room, boolean isTextOnly, Long fromTimestamp);

  /**
   * Read messages from room from a dedicated timestamp to another with limit.
   * 
   * @param room
   * @param isTextOnly
   * @param fromTimestamp
   * @param toTimestamp
   * @param limitToLoad
   * @return
   */
  public String read(String room, boolean isTextOnly, Long fromTimestamp, Long toTimestamp, int limitToLoad);

  public MessageBean getMessage(String roomId, String messageId);

  public String getSpaceRoom(String space);

  public String getSpaceRoomByName(String name);

  public String getTeamRoom(String team, String user);

  public String getExternalRoom(String identifier);

  public String getTeamCreator(String room);

  public void setRoomName(String room, String name);

  /**
   * Retrieve a Room by its ID
   * @param roomId the ID of the room
   * @return the room or null if the room doesn't exists
   */
  public RoomBean getTeamRoomById(String roomId);

  public String getRoom(List<String> users);
  
  public String getTypeRoomChat(String roomId);

  /**
   * @param user
   * @param withPublic
   * @param isAdmin
   * @param notificationService
   * @param tokenService
   *
   * @return All existing personal rooms of given <code>user</code>
   */
  public List<RoomBean> getExistingRooms(String user, boolean withPublic, boolean isAdmin, NotificationService notificationService, TokenService tokenService);

  public RoomsBean getRooms(String user, List<String> onlineUsers, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, int limit, NotificationService notificationService, TokenService tokenService);

  public int getNumberOfRooms();

  public int getNumberOfMessages();

  /**
   * Return rooms by name
   * 
   * @param teamName
   * @return
   */
  public List<RoomBean> getTeamRoomByName(String teamName);

}
