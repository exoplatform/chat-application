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
