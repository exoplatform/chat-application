package org.exoplatform.chat.utils;

import java.security.NoSuchAlgorithmException;

public class MessageDigester
{
  public static String getHash(String message)
  {
    StringBuffer ticket = new StringBuffer();
    try
    {
      java.security.MessageDigest msgDigest = java.security.MessageDigest.getInstance("SHA-1");
      msgDigest.update(message.getBytes());
      byte[] aMessageDigest = msgDigest.digest();
      String tmp = null;
      for (int i = 0; i < aMessageDigest.length; i++)
      {
          tmp = Integer.toHexString(0xFF & aMessageDigest[i]);
          if (tmp.length() == 2)
          {
              ticket.append(tmp);
          }
          else
          {
              ticket.append("0");
              ticket.append(tmp);
          }
      }
    }
    catch (NoSuchAlgorithmException nsae)
    {
      //SHA-1 exists, no exception should be raised here.
    }
    return ticket.toString();
  }

}
