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
  private static Mongo m;

  private static final String M_DB = "chat";
  private static final String M_ROOM_PREFIX = "room_";
  private static final String M_ROOMS_COLLECTION = "rooms";
  private static final String M_UNREAD_PREFIX = "unread_";

  public ChatService() throws UnknownHostException
  {
    m = new Mongo("localhost");
    m.setWriteConcern(WriteConcern.SAFE);
  }

  private DB db()
  {
    return m.getDB(M_DB);
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

    this.updateLastReadMessage(user, room);
  }

  public void updateLastReadMessage(String user, String room)
  {
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+room);
    BasicDBObject query = new BasicDBObject();
    query.put("timestamp", -1);
    DBCursor cursor = coll.find().sort(query);
    Long ts = null;
    if (cursor.hasNext())
    {
      DBObject dbo = cursor.next();
      ts = (Long)dbo.get("timestamp");
    }

    setLastReadNotification(user,  ts,  room);
  }


  private void setLastReadNotification(String user, Long timestamp, String room)
  {
    DBCollection coll = db().getCollection(M_UNREAD_PREFIX+room);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      doc.put("timestamp", timestamp);
      coll.save(doc, WriteConcern.SAFE);
    }
    else
    {
      BasicDBObject doc = new BasicDBObject();
      doc.put("user", user);
      doc.put("timestamp", timestamp);
      coll.insert(doc);
    }

  }

  public Long getLastReadMessageTimestamp(String user, String room)
  {
    Long ts = new Long(-1);
    DBCollection coll = db().getCollection(M_UNREAD_PREFIX+room);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      ts = (Long)doc.get("timestamp");
    }

    return ts;
  }

  public int getUnreadMessagesTotal(String user, String room)
  {
    int total = -1;
    Long lastRead = getLastReadMessageTimestamp(user,  room);
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+room);
    BasicDBObject query = new BasicDBObject();

    query.put("timestamp", new BasicDBObject("$gt", lastRead));
    DBCursor cursor = coll.find(query);
    total = cursor.size();

    return total;
  }


  public String read(String room)
  {
    StringBuffer sb = new StringBuffer();

    SimpleDateFormat formatter = new SimpleDateFormat("h:mm aaa");
    // formatter.format();

    DBCollection coll = db().getCollection(M_ROOM_PREFIX+room);

    DBCursor cursor = coll.find();
    String prevUser = "";
    while(cursor.hasNext()) {
      DBObject dbo = cursor.next();
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



}
