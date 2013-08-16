package org.benjp.model;

import org.benjp.utils.MessageDigester;
import org.benjp.utils.PropertyManager;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class RoomsBean {

  List<RoomBean> rooms;
  int unreadOffline = 0;
  int unreadOnline = 0;
  int unreadSpaces = 0;
  int unreadTeams = 0;

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

  public String roomsToJSON()
  {
    StringBuffer sb = new StringBuffer();
    sb.append("{");
    sb.append("\"unreadOffline\": \""+unreadOffline+"\",");
    sb.append("\"unreadOnline\": \""+unreadOnline+"\",");
    sb.append("\"unreadSpaces\": \""+unreadSpaces+"\",");
    sb.append("\"unreadTeams\": \""+unreadTeams+"\",");
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
    sb.append("\"timestamp\": \""+System.currentTimeMillis()+"\"");
    sb.append("}");


    return sb.toString();
  }

}
