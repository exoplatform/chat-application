package org.benjp.chat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class UserService
{

  private static final HashMap<String, String> users = new HashMap<String, String>(); // <session, user>

  public static boolean hasSession(String session)
  {
    return users.containsKey(session);
  }

  public static boolean hasUser(String user)
  {
    return users.containsValue(user);
  }

  public static void addUser(String user, String session)
  {
    synchronized (users)
    {
      if (!users.containsValue(user))
      {
        System.out.println("USER SERVICE :: ADDING :: " + user + " : " + session);
        users.put(session, user);
      }
    }
  }

  public static void removeSession(String session)
  {
    synchronized (users)
    {
      System.out.println("USER SERVICE :: REMOVING :: "+users.get(session)+" : "+session);
      users.remove(session);
    }
  }

  public static Collection<String> getUsers()
  {
    return users.values();
  }

  public static Collection<String> getUsersFilterBy(String user)
  {
    Collection<String> dest = new ArrayList<String>();
    Collection<String> usersc = UserService.getUsers();
    for (String userc: usersc)
    {
      if (!userc.equals(user))
        dest.add(userc);
    }
    return dest;
  }

}
