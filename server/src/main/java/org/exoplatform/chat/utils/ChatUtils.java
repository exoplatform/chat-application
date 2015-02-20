package org.exoplatform.chat.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.icu.text.Transliterator;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.resources.ResourceBundleService;

public class ChatUtils {

  public static String getRoomId(String roomName, String user)
  {
    if (roomName.startsWith(ChatService.TEAM_PREFIX) && roomName.length()>ChatService.TEAM_PREFIX.length()+1)
    {
      return roomName.substring(ChatService.TEAM_PREFIX.length());
    }

    StringBuilder sb = new StringBuilder();
    sb.append("1-team-room;").append(user).append(";").append(roomName).append(";");

    return MessageDigester.getHash(sb.toString());
  }

  public static String getRoomId(String roomName)
  {
    if (roomName.startsWith(ChatService.SPACE_PREFIX) && roomName.length()>ChatService.SPACE_PREFIX.length()+1)
    {
      return roomName.substring(ChatService.SPACE_PREFIX.length());
    }

    StringBuilder sb = new StringBuilder();
    sb.append("1-space-room;").append(roomName).append(";");

    return MessageDigester.getHash(sb.toString());
  }

  public static String getExternalRoomId(String identifier)
  {
    if (identifier.startsWith(ChatService.EXTERNAL_PREFIX) && identifier.length()>ChatService.EXTERNAL_PREFIX.length()+1)
    {
      return identifier.substring(ChatService.EXTERNAL_PREFIX.length());
    }

    StringBuilder sb = new StringBuilder();
    sb.append("1-external-room;").append(identifier).append(";");

    return MessageDigester.getHash(sb.toString());
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

  /**
   * Clean string.
   *
   * @param str the str
   *
   * @return the string
   */
  public static String cleanString(String str) {
    Transliterator accentsconverter = Transliterator.getInstance("Latin; NFD; [:Nonspacing Mark:] Remove; NFC;");
    str = accentsconverter.transliterate(str);
    //the character ? seems to not be changed to d by the transliterate function
    StringBuffer cleanedStr = new StringBuffer(str.trim());
    // delete special character
    for(int i = 0; i < cleanedStr.length(); i++) {
      char c = cleanedStr.charAt(i);
      if(c == ' ') {
        if (i > 0 && cleanedStr.charAt(i - 1) == '-') {
          cleanedStr.deleteCharAt(i--);
        } else {
          c = '-';
          cleanedStr.setCharAt(i, c);
        }
        continue;
      }
      if(i > 0 && !(Character.isLetterOrDigit(c) || c == '-')) {
        cleanedStr.deleteCharAt(i--);
        continue;
      }
      if(i > 0 && c == '-' && cleanedStr.charAt(i-1) == '-')
        cleanedStr.deleteCharAt(i--);
    }
    while (StringUtils.isNotEmpty(cleanedStr.toString()) && !Character.isLetterOrDigit(cleanedStr.charAt(0))) {
      cleanedStr.deleteCharAt(0);
    }
    String clean = cleanedStr.toString().toLowerCase();
    if (clean.endsWith("-")) {
      clean = clean.substring(0, clean.length()-1);
    }

    return clean;
  }

  public static String appRes(String key,Locale locale) {
    ResourceBundleService bundleService = CommonsUtils.getService(ResourceBundleService.class);
    ResourceBundle res = bundleService.getResourceBundle("locale.chat.server.Resource", locale);
    if (res == null || res.containsKey(key) == false) {
      return key;
    }

    return res.getString(key);

  }
}
