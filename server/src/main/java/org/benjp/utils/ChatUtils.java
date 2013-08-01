package org.benjp.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatUtils {

  public static String getRoomId(String space)
  {
    if (space.startsWith("space-") && space.length()>7)
      return space.substring(6);

    ArrayList<String> spaces = new ArrayList<String>();
    spaces.add("1-space-room");
    spaces.add(space);
    return getRoomId(spaces);
  }

  public static String getRoomId(List<String> users)
  {
    Collections.sort(users);
    StringBuilder sb = new StringBuilder();
    for (String user:users)
    {
      sb.append(user).append(";");
    }

    String roomId = MessageDigester.getHash(sb.toString());
    return roomId;
  }


}
