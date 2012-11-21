package org.benjp.services;

import com.mongodb.*;
import org.bson.types.ObjectId;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;

@Named("chatService")
@ApplicationScoped
public class ChatService
{
  private static final String M_ROOM_PREFIX = "room_";
  private static final String M_ROOMS_COLLECTION = "rooms";

  public static final String SPACE_PREFIX = "space-";

  private DB db()
  {
    return MongoBootstrap.getDB();
  }

  public void write(String message, String user, String room)
  {
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+room);
    if (coll.count()==0) {
      coll.ensureIndex("timestamp");
    }


    BasicDBObject doc = new BasicDBObject();
    doc.put("user", user);
    doc.put("message", message);
    doc.put("time", new Date());
    doc.put("timestamp", System.currentTimeMillis());

    coll.insert(doc);
  }


  public String read(String room, UserService userService)
  {
    StringBuffer sb = new StringBuffer();

    SimpleDateFormat formatter = new SimpleDateFormat("hh:mm aaa");
    SimpleDateFormat formatterDate = new SimpleDateFormat("dd/MM/yyyy hh:mm aaa");
    // formatter.format();
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    Date today = calendar.getTime();

    DBCollection coll = db().getCollection(M_ROOM_PREFIX+room);

    BasicDBObject query = new BasicDBObject();
    query.put("timestamp", new BasicDBObject("$gt", System.currentTimeMillis()-7*24*60*60*1000));

    BasicDBObject sort = new BasicDBObject();
    sort.put("timestamp", -1);

    DBCursor cursor = coll.find(query).sort(sort).limit(200);
    String prevUser = "";
    if (!cursor.hasNext())
    {
      sb.append("{\"messages\": []}");
    }
    else
    {
      Map<String, String> users = new HashMap<String, String>();

      List<DBObject> listdbo = new ArrayList<DBObject>();
      String mostRecentTimestamp = null;
      while (cursor.hasNext())
      {
        /** sorting and unsorting on cursor doesn't work, we need to reverse using a 2nd loop
         * not good in term of performance, we should find a better way for this to work with
         * sorting and limit in the query
         */
        DBObject dbo = cursor.next();
        if (mostRecentTimestamp==null)
        {
          mostRecentTimestamp = dbo.get("timestamp").toString();
        }
        listdbo.add(0, dbo);
      }
      sb.append("{\"room\": \"").append(room).append("\",");
      sb.append("\"timestamp\": \"").append(mostRecentTimestamp).append("\",");
      sb.append("\"messages\": [");
      boolean first = true;
      for(DBObject dbo:listdbo)
      {
        String user = dbo.get("user").toString();
        String fullname = users.get(user);
        if (fullname==null)
        {
          fullname = userService.getUserFullName(user);
          users.put(user, fullname);
        }

        String date = "";
        try
        {
          if (dbo.containsField("time"))
          {
            Date date1 = (Date)dbo.get("time");
            if (date1.before(today))
              date = formatterDate.format(date1);
            else
              date = formatter.format(date1);

          }
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }

        if (!first)sb.append(",");
        sb.append("{\"user\": \"").append(user).append("\",");
        sb.append("\"fullname\": \"").append(fullname).append("\",");
        sb.append("\"date\": \"").append(date).append("\",");
        sb.append("\"message\": \"").append(dbo.get("message")).append("\"}");
        first = false;
      }

      sb.append("]}");

    }

    return sb.toString();
  }

  public String getSpaceRoom(String space)
  {
    String room = null;
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("space", space);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext())
    {
      DBObject dbo = cursor.next();
      room = ((ObjectId)dbo.get("_id")).toString();
    }
    else
    {
      coll.insert(basicDBObject);
      room = getSpaceRoom(space);
    }

    return room;
  }

  public String getRoom(List<String> users)
  {
    Collections.sort(users);
    String room = null;
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("users", users);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext())
    {
      DBObject dbo = cursor.next();
      room = ((ObjectId)dbo.get("_id")).toString();
    }
    else
    {
      coll.insert(basicDBObject);
      room = getRoom(users);
    }

    return room;
  }

  public List<RoomBean> getExistingRooms(String user, NotificationService notificationService)
  {
    List<RoomBean> rooms = new ArrayList<RoomBean>();
    String roomId = null;
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("users", user);

    DBCursor cursor = coll.find(basicDBObject);
    while (cursor.hasNext())
    {
      DBObject dbo = cursor.next();
      roomId = ((ObjectId)dbo.get("_id")).toString();
      List<String> users = ((List<String>)dbo.get("users"));
      users.remove(user);
      if (users.size()>0 && !user.equals(users.get(0)))
      {
        RoomBean roomBean = new RoomBean();
        roomBean.setRoom(roomId);
        roomBean.setUnreadTotal(notificationService.getUnreadNotificationsTotal(user, "chat", "room", roomId));
        roomBean.setUser(users.get(0));
        rooms.add(roomBean);
      }
    }

    return rooms;
  }

  public boolean hasRoom(List<String> users)
  {
    Collections.sort(users);
    String room = null;
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("users", users);

    DBCursor cursor = coll.find(basicDBObject);
    return cursor.hasNext();
  }

  public List<RoomBean> getRooms(String user, NotificationService notificationService, UserService userService)
  {
    return getRooms(user, null, true, true, notificationService, userService);
  }

  public List<RoomBean> getRooms(String user, String filter, boolean withUsers, boolean withSpaces, NotificationService notificationService, UserService userService)
  {
    List<RoomBean> rooms = new ArrayList<RoomBean>();

    if (withUsers)
    {
      Collection<String> availableUsers = userService.getUsersFilterBy(user);
      rooms = this.getExistingRooms(user, notificationService);

      for (RoomBean roomBean:rooms) {
        String targetUser = roomBean.getUser();
        roomBean.setFullname(userService.getUserFullName(targetUser));
        roomBean.setFavorite(userService.isFavorite(user, roomBean.getUser()));
        if (availableUsers.contains(targetUser))
        {
          roomBean.setAvailableUser(true);
          roomBean.setStatus(userService.getStatus(targetUser));
          availableUsers.remove(targetUser);
        }
        else
        {
          roomBean.setAvailableUser(false);
        }

      }

      for (String availableUser: availableUsers)
      {
        RoomBean roomBean = new RoomBean();
        roomBean.setUser(availableUser);
        roomBean.setFullname(userService.getUserFullName(availableUser));
        roomBean.setStatus(userService.getStatus(availableUser));
        roomBean.setAvailableUser(true);
        roomBean.setFavorite(userService.isFavorite(user, roomBean.getUser()));
        rooms.add(roomBean);
      }
    }

    if (withSpaces)
    {
      List<SpaceBean> spaces = userService.getSpaces(user);
      for (SpaceBean space:spaces)
      {
        RoomBean roomBeanS = new RoomBean();
        roomBeanS.setUser(SPACE_PREFIX+space.getId());
        roomBeanS.setFullname(space.getDisplayName());
        roomBeanS.setStatus(UserService.STATUS_SPACE);
        roomBeanS.setAvailableUser(true);
        roomBeanS.setSpace(true);
        roomBeanS.setUnreadTotal(notificationService.getUnreadNotificationsTotal(user, "chat", "room", getSpaceRoom(SPACE_PREFIX + space.getId())));
        roomBeanS.setFavorite(userService.isFavorite(user, roomBeanS.getUser()));
        rooms.add(roomBeanS);

      }
    }


    List<RoomBean> finalRooms = new ArrayList<RoomBean>();
    for (RoomBean roomBean:rooms) {
      String targetUser = roomBean.getFullname();
      if (filter(targetUser, filter))
        finalRooms.add(roomBean);
    }

    Collections.sort(finalRooms);

    return finalRooms;

  }

  private boolean filter(String user, String filter)
  {
    if (user==null || filter==null || "".equals(filter)) return true;

    String[] args = filter.toLowerCase().split(" ");
    String s = user.toLowerCase();
    int ind;
    for (String arg:args)
    {
      ind = s.indexOf(arg);
      if (ind == -1)
        return false;
      else
        s = s.substring(ind);
    }
    return true;
  }

}
