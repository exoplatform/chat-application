package org.benjp.services;

import com.mongodb.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.net.UnknownHostException;

@Named("notificationService")
@ApplicationScoped
public class NotificationService
{
  private static Mongo m;

  public NotificationService() throws UnknownHostException
  {
    m = new Mongo("localhost");
    m.setWriteConcern(WriteConcern.SAFE);
  }

  private DB db()
  {
    return m.getDB("notifications");
  }


  public void addNotification(String user, String type, String info)
  {
    DBCollection coll = db().getCollection(user);

    BasicDBObject doc = new BasicDBObject();
    doc.put("timestamp", System.currentTimeMillis());
    doc.put("type", type);
    doc.put("info", info);

    coll.insert(doc);
  }

  public void setLastReadNotification(String user, Long timestamp)
  {
    DBCollection coll = db().getCollection("lastNotifications");
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

  public Long getLastReadNotificationTimestamp(String user)
  {
    Long ts = new Long(-1);
    DBCollection coll = db().getCollection("lastNotifications");
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

  public NotificationBean getLastNotification(String user)
  {
    NotificationBean bean = new NotificationBean();

    DBCollection coll = db().getCollection(user);
    BasicDBObject query = new BasicDBObject();
    query.put("timestamp", -1);
    DBCursor cursor = coll.find().sort(query);
    if (cursor.hasNext())
    {
      DBObject dbo = cursor.next();
      bean.setInfo((String)dbo.get("info"));
      bean.setUser(user);
      bean.setTimestamp((Long)dbo.get("timestamp"));
      bean.setType((String)dbo.get("type"));
    }

    return bean;
  }

  public int getUnreadNotificationsTotal(String user)
  {
    int total = -1;
    Long lastRead = getLastReadNotificationTimestamp(user);
    DBCollection coll = db().getCollection(user);
    BasicDBObject query = new BasicDBObject();

    query.put("timestamp", new BasicDBObject("$gt", lastRead));
    DBCursor cursor = coll.find(query);
    total = cursor.size();

    return total;
  }



}
