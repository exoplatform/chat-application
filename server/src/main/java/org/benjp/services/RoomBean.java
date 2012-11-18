package org.benjp.services;

public class RoomBean implements Comparable<RoomBean>
{
  String user = "";
  String fullname = "";
  String room = "";
  int unreadTotal = -1;
  boolean isAvailableUser = false;
  String status = UserService.STATUS_INVISIBLE;
  boolean isSpace = false;
  boolean isFavorite = false;

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

  public void setFullname(String fullname) {
    this.fullname = fullname;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public boolean isSpace() {
    return isSpace;
  }

  public void setSpace(boolean space) {
    isSpace = space;
  }

  public boolean isFavorite() {
    return isFavorite;
  }

  public void setFavorite(boolean favorite) {
    isFavorite = favorite;
  }

  @Override
  public int compareTo(RoomBean roomBean) {
    String l = ((isFavorite)?"0":"1")+fullname;
    String r = ((roomBean.isFavorite())?"0":"1")+roomBean.getFullname();
    return l.compareTo(r);
  }
}
