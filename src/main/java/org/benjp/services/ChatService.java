package org.benjp.services;

import com.mongodb.*;
import org.bson.types.ObjectId;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Named("chatService")
@ApplicationScoped
public class ChatService
{
  private static Mongo m;

  private static final String M_DB = "chat";
  private static final String M_ROOM_PREFIX = "room_";
  private static final String M_ROOMS_COLLECTION = "rooms";
  private static final String M_USER_PREFIX = "user_";

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

    coll.insert(doc);
  }

  public String read(String room)
  {
    StringBuffer sb = new StringBuffer();
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+room);

    DBCursor cursor = coll.find();
    String prevUser = "";
    while(cursor.hasNext()) {
      DBObject dbo = cursor.next();
      String user = dbo.get("user").toString();
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
      sb.append(dbo.get("message"));
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



}
