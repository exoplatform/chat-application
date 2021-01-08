package org.exoplatform.chat.model;

import org.exoplatform.chat.utils.MessageDigester;

import java.util.List;

public class RoomsBean {

  List<RoomBean> rooms;
  int unreadOffline = 0;
  int unreadOnline = 0;
  int unreadSpaces = 0;
  int unreadTeams = 0;
  int roomsCount = 0;

  public List<RoomBean> getRooms() {
    return rooms;
  }

  public void setRooms(List<RoomBean> rooms) {
    this.rooms = rooms;
  }

  public int getUnreadOffline() {
    return unreadOffline;
  }

  public void setUnreadOffline(int unreadOffline) {
    this.unreadOffline = unreadOffline;
  }

  public int getUnreadOnline() {
    return unreadOnline;
  }

  public void setUnreadOnline(int unreadOnline) {
    this.unreadOnline = unreadOnline;
  }

  public int getUnreadSpaces() {
    return unreadSpaces;
  }

  public void setUnreadSpaces(int unreadSpaces) {
    this.unreadSpaces = unreadSpaces;
  }

  public int getUnreadTeams() {
    return unreadTeams;
  }

  public void setUnreadTeams(int unreadTeams) {
    this.unreadTeams = unreadTeams;
  }

  public int getRoomsCount() {
    return roomsCount;
  }

  public void setRoomsCount(int roomsCount) {
    this.roomsCount = roomsCount;
  }

  public String roomsToJSON()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"unreadOffline\": \"").append(unreadOffline).append("\",");
    sb.append("\"unreadOnline\": \"").append(unreadOnline).append("\",");
    sb.append("\"unreadSpaces\": \"").append(unreadSpaces).append("\",");
    sb.append("\"unreadTeams\": \"").append(unreadTeams).append("\",");
    sb.append("\"roomsCount\": \"").append(roomsCount).append("\",");
    sb.append("\"rooms\": [");
    boolean first=true;
    for (RoomBean roomBean:this.getRooms()) {
      if (!first) {
        sb.append(",");
      } else {
        first=false;
      }

      sb.append(roomBean.toJSON());

    }
    sb.append("],");
    sb.append("\"md5\": \"").append(MessageDigester.getHash(sb.toString())).append("\",");
    sb.append("\"timestamp\": \"").append(System.currentTimeMillis()).append("\"");
    sb.append("}");


    return sb.toString();
  }

}
