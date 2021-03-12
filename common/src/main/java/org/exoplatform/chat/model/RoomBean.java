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

import org.exoplatform.chat.services.UserService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;

public class RoomBean implements Comparable<RoomBean>
{
  String prettyName;
  String user = "";
  String fullname = "";
  String room = "";
  int unreadTotal = 0;
  boolean isAvailableUser = false;
  Boolean enabledUser;
  boolean enabledRoom = true;
  String status = UserService.STATUS_INVISIBLE;
  String type = null;
  boolean meetingStarted = false;
  String startTime = "";
  boolean isFavorite = false;
  String[] admins = null;
  long timestamp = -1;
  org.json.JSONObject lastMessage;
  String groupId;

  public String getPrettyName() {
    return prettyName;
  }

  public void setPrettyName(String prettyName) {
    this.prettyName = prettyName;
  }

  public org.json.JSONObject getLastMessage() {
    return lastMessage;
  }

  public void setLastMessage(org.json.JSONObject lastMessage) {
    this.lastMessage = lastMessage;
  }

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

  public void setAvailableUser(boolean availableUser) {
    isAvailableUser = availableUser;
  }

  public boolean isActive() {
    return (isAvailableUser || (!"".equals(room)));
  }

  public boolean isEnabledUser() {
    return enabledUser;
  }

  public void setEnabledUser(Boolean enabledUser) {
    this.enabledUser = enabledUser;
  }

  public String getFullName() {
    return fullname;
  }

  public void setFullName(String fullname) {
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

  public String[] getAdmins() {
    return admins;
  }

  public void setAdmins(String[] admins) {
    this.admins = admins;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  @Override
  public int compareTo(RoomBean roomBean) {
    String l = ((isFavorite) ? "0" : "1") + fullname;
    String r = ((roomBean.isFavorite()) ? "0" : "1") + roomBean.getFullName();
    return l.compareTo(r);
  }

  public boolean isMeetingStarted() {
    return meetingStarted;
  }

  public void setMeetingStarted(boolean meetingStarted) {
    this.meetingStarted = meetingStarted;
  }

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public boolean isEnabledRoom() {
    return enabledRoom;
  }

  public void setEnabledRoom(boolean enabledRoom) {
    this.enabledRoom = enabledRoom;
  }

  @SuppressWarnings("unchecked")
  public JSONObject toJSONObject() {
    JSONObject obj = new JSONObject();
    obj.put("fullName", this.getFullName());
    obj.put("room", this.getRoom());
    obj.put("status", this.getStatus());
    obj.put("user", this.getUser());
    obj.put("timestamp", this.getTimestamp());
    obj.put("unreadTotal", this.getUnreadTotal());
    obj.put("isActive", String.valueOf(this.isActive()));
    obj.put("isEnabledUser", String.valueOf(this.enabledUser));
    obj.put("isEnabledRoom", String.valueOf(this.enabledRoom));
    obj.put("isFavorite", this.isFavorite());
    obj.put("type", this.getType());
    obj.put("meetingStarted", this.isMeetingStarted());
    obj.put("startTime", this.getStartTime());
    obj.put("lastMessage", this.getLastMessage());
    obj.put("prettyName", this.getPrettyName());
    if(this.getGroupId() != null && !this.getGroupId().isEmpty()) {
      obj.put("groupId", this.getGroupId());
    }
    if (this.getAdmins() != null) {
      try {
        obj.put("admins", new JSONArray(this.getAdmins()));
      } catch (JSONException e) {
        throw new RuntimeException(e);
      }
    }
    return obj;
  }

  public String toJSON()
  {
    return toJSONObject().toString();
  }
}
