package org.benjp.model;

import org.benjp.utils.MessageDigester;
import org.benjp.utils.PropertyManager;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class RoomsBean {

  List<RoomBean> rooms;

  public List<RoomBean> getRooms() {
    return rooms;
  }

  public void setRooms(List<RoomBean> rooms) {
    this.rooms = rooms;
  }

  public String roomsToJSON()
  {
    StringBuffer sb = new StringBuffer();
    sb.append("{");
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
