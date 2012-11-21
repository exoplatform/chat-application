package org.benjp.services;

import com.mongodb.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.net.UnknownHostException;

@Named("notificationService")
@ApplicationScoped
public class NotificationService
{
  private static final String M_USERS = "users";

  private DB db()
  {
    return MongoBootstrap.getDB();
  }


  public void addNotification(String user, String type, String category, String categoryId, String content, String link)
  {
    DBCollection coll = db().getCollection(M_USERS);
    if (coll.count()==0) {
      coll.ensureIndex("user");
      coll.ensureIndex("type");
      coll.ensureIndex("category");
      coll.ensureIndex("categoryId");
      coll.ensureIndex("isRead");
    }

    BasicDBObject doc = new BasicDBObject();
    doc.put("timestamp", System.currentTimeMillis());
    doc.put("user", user);
    doc.put("type", type);
    doc.put("category", category);
    doc.put("categoryId", categoryId);
    doc.put("content", content);
    doc.put("link", link);
    doc.put("isRead", false);

    coll.insert(doc);
  }

  public void setNotificationsAsRead(String user, String type, String category, String categoryId)
  {
    DBCollection coll = db().getCollection(M_USERS);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    query.put("type", type);
    query.put("category", category);
    query.put("categoryId", categoryId);
    query.put("isRead", false);
    DBCursor cursor = coll.find(query);
    while (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      doc.put("isRead", true);
      coll.save(doc, WriteConcern.SAFE);
    }

  }

  public int getUnreadNotificationsTotal(String user)
  {
    return getUnreadNotificationsTotal(user, null, null, null);
  }


  public int getUnreadNotificationsTotal(String user, String type, String category, String categoryId)
  {
    int total = -1;
    DBCollection coll = db().getCollection(M_USERS);
    BasicDBObject query = new BasicDBObject();

    query.put("user", user);
    if (type!=null) query.put("type", type);
    if (category!=null) query.put("category", category);
    if (categoryId!=null) query.put("categoryId", categoryId);
    query.put("isRead", false);
    DBCursor cursor = coll.find(query);
    total = cursor.size();

    return total;
  }



}
