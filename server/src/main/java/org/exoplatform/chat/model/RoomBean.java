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

package org.exoplatform.chat.model;

import org.apache.commons.lang3.StringEscapeUtils;
import org.exoplatform.chat.services.UserService;
import org.json.JSONException;
import org.json.JSONObject;

public class RoomBean implements Comparable<RoomBean>
{
  String user = "";
  String fullname = "";
  String room = "";
  int unreadTotal = -1;
  boolean isAvailableUser = false;
  String status = UserService.STATUS_INVISIBLE;
  String type = null;
  boolean isFavorite = false;
  long timestamp = -1;

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getRoom() {
    return room;
  }

  public void setRoom(String room) {
    this.room = room;
  }

  public int getUnreadTotal() {
    return unreadTotal;
  }

  public void setUnreadTotal(int unreadTotal) {
    this.unreadTotal = unreadTotal;
  }

  public boolean isAvailableUser() {
    return isAvailableUser;
  }

  public void setAvailableUser(boolean availableUser) {
    isAvailableUser = availableUser;
  }

  public boolean isActive() {
    return (isAvailableUser || (!"".equals(room)));
  }

  public String getFullname() {
    return fullname;
  }

  public String getEscapedFullname() {
    return StringEscapeUtils.escapeHtml4(this.fullname);
  }

  public void setFullname(String fullname) {
    this.fullname = fullname;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isFavorite() {
    return isFavorite;
  }

  public void setFavorite(boolean favorite) {
    isFavorite = favorite;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public int compareTo(RoomBean roomBean) {
    String l = ((isFavorite)?"0":"1")+fullname;
    String r = ((roomBean.isFavorite())?"0":"1")+roomBean.getFullname();
    return l.compareTo(r);
  }

  public String toJSON()
  {
    JSONObject obj = new org.json.JSONObject();
    try {
      obj.put("escapedFullname", this.getEscapedFullname());
      obj.put("room", this.getRoom());
      obj.put("status", this.getStatus());
      obj.put("user", this.getUser());
      obj.put("timestamp", this.getTimestamp());
      obj.put("unreadTotal", String.valueOf(this.getUnreadTotal()));
      obj.put("isActive", String.valueOf(this.isActive()));
      obj.put("isAvailableUser", String.valueOf(this.isAvailableUser()));
      obj.put("isFavorite", String.valueOf(this.isFavorite()));
      obj.put("type", this.getType());
    } catch (JSONException e) {
      return obj.toString();
    }
    return obj.toString();
  }}
