package org.benjp.services;

public class RoomBean implements Comparable<RoomBean>
{
  String user = "";
  String room = "";
  int unreadTotal = -1;
  boolean isAvailableUser = false;

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

  @Override
  public int compareTo(RoomBean roomBean) {
    return user.compareTo(roomBean.getUser());
  }
}
