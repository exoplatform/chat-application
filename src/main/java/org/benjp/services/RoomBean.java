package org.benjp.services;

public class RoomBean
{
  String user = "";
  String room = "";
  int unreadTotal = -1;

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
}
