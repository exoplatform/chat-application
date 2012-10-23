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
  private static final String M_DB = "chat";
  private static final String M_ROOM_PREFIX = "room_";
  private static final String M_ROOMS_COLLECTION = "rooms";
  private static final String M_UNREAD_PREFIX = "unread_";

  private DB db()
  {
    return MongoBootstrap.mongo().getDB(M_DB);
  }

  public void write(String message, String user, String room)
  {
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+room);

    BasicDBObject doc = new BasicDBObject();
    doc.put("user", user);
    doc.put("message", message);
    doc.put("time", new Date());
    doc.put("timestamp", System.currentTimeMillis());

    coll.insert(doc);
  }


  public String read(String room)
  {
    StringBuffer sb = new StringBuffer();

    SimpleDateFormat formatter = new SimpleDateFormat("h:mm aaa");
    // formatter.format();

    DBCollection coll = db().getCollection(M_ROOM_PREFIX+room);

    BasicDBObject query = new BasicDBObject();
    query.put("timestamp", new BasicDBObject("$gt", System.currentTimeMillis()-7*24*60*60*1000));

    BasicDBObject sort = new BasicDBObject();
    sort.put("timestamp", -1);

    DBCursor cursor = coll.find(query).sort(sort).limit(200);
    String prevUser = "";
    if (!cursor.hasNext())
    {
      sb.append("<div class='msgln' style='padding:20px 0px;'><b><center>No messages yet.</center></b></div>");
    }
    else
    {
      List<DBObject> listdbo = new ArrayList<DBObject>();
      while (cursor.hasNext())
      {
        /** sorting and unsorting on cursor doesn't work, we need to reverse using a 2nd loop
         * not good in term of performance, we should find a better way for this to work with
         * sorting and limit in the query
         */
        DBObject dbo = cursor.next();
        listdbo.add(0, dbo);
      }
      for(DBObject dbo:listdbo)
      {
        String user = dbo.get("user").toString();
        String date = "";
        try
        {
          if (dbo.containsField("time"))
          {
            Date date1 = (Date)dbo.get("time");
            date = formatter.format(date1);
          }
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }

        if (!prevUser.equals(user))
        {
          if (!prevUser.equals(""))
            sb.append("</div>");
          sb.append("<div class='msgln'><b>");
          sb.append(user);
          sb.append("</b><br/>");
        }
        else
        {
          sb.append("<hr style='margin:0px;'>");
        }
        sb.append("<div><span style='float:left'>"+dbo.get("message")+"</span>" +
                "<span style='float:right;color:#CCC;font-size:10px'>"+date+"</span></div>" +
                "<div style='clear:both;'></div>");
        prevUser = user;
      }
    }

    return sb.toString();
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
      RoomBean roomBean = new RoomBean();
      roomBean.setRoom(roomId);
      roomBean.setUnreadTotal(notificationService.getUnreadNotificationsTotal(user, "chat", "room", roomId));
      roomBean.setUser(users.get(0));
      rooms.add(roomBean);
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
    Collection<String> availableUsers = userService.getUsersFilterBy(user);
    List<RoomBean> rooms = this.getExistingRooms(user, notificationService);

    for (RoomBean roomBean:rooms) {
      String targetUser = roomBean.getUser();
      if (availableUsers.contains(targetUser))
      {
        roomBean.setAvailableUser(true);
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
      roomBean.setAvailableUser(true);
      rooms.add(roomBean);
    }

    Collections.sort(rooms);

    return rooms;

  }

}
