package org.benjp.services;

import com.mongodb.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.net.UnknownHostException;

@Named("chatService")
@ApplicationScoped
public class ChatService
{
  private static Mongo m;

  public ChatService() throws UnknownHostException
  {
    m = new Mongo("localhost");
    m.setWriteConcern(WriteConcern.SAFE);
  }

  private DB db()
  {
    return m.getDB("chat");
  }

  public void write(String message, String user, String room)
  {
    DBCollection coll = db().getCollection(room);

    BasicDBObject doc = new BasicDBObject();
    doc.put("user", user);
    doc.put("message", message);

    coll.insert(doc);
  }

  public String read(String room)
  {
    StringBuffer sb = new StringBuffer();
    DBCollection coll = db().getCollection(room);

    DBCursor cursor = coll.find();
    while(cursor.hasNext()) {
      DBObject dbo = cursor.next();
      sb.append("<div class='msgln'><b>");
      sb.append(dbo.get("user"));
      sb.append("</b>: ");
      sb.append(dbo.get("message"));
      sb.append("<br></div>");
    }

    return sb.toString();
  }



}
