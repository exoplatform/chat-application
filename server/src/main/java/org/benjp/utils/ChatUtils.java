package org.benjp.utils;

import org.apache.commons.codec.binary.Base64;

import java.io.*;
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

  /** Read the object from Base64 string. */
  public static Object fromString( String s ) throws IOException ,
          ClassNotFoundException {
    byte [] data = Base64.decodeBase64( s );
    ObjectInputStream ois = new ObjectInputStream(
            new ByteArrayInputStream(  data ) );
    Object o  = ois.readObject();
    ois.close();
    return o;
  }

  /** Write the object to a Base64 string. */
  public static String toString( Serializable o ) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream( baos );
    oos.writeObject( o );
    oos.close();
    return new String( Base64.encodeBase64( baos.toByteArray() ) );
  }
}
