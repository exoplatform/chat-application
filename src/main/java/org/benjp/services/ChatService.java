package org.benjp.services;

import com.mongodb.*;
import juzu.SessionScoped;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.UnknownHostException;

@Named("chatService")
@SessionScoped
public class ChatService
{
  DB db=null;

  public ChatService() throws UnknownHostException
  {
    Mongo m = new Mongo("localhost");
    m.setWriteConcern(WriteConcern.SAFE);
    db = m.getDB("chat");
  }

  public void write(String message, String user, String room)
  {
    DBCollection coll = db.getCollection(room);

    BasicDBObject doc = new BasicDBObject();
    doc.put("user", user);
    doc.put("message", message);

    coll.insert(doc);
  }

  public String read(String room)
  {
    StringBuffer sb = new StringBuffer();
    DBCollection coll = db.getCollection(room);

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
