/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.benjp.services.mongodb;

import com.mongodb.*;
import org.benjp.listener.ConnectionManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@Named("notificationService")
@ApplicationScoped
public class NotificationServiceImpl implements org.benjp.services.NotificationService
{

  private DB db()
  {
    return ConnectionManager.getInstance().getDB();
  }

  public static void cleanupNotifications()
  {
    DBCollection coll = ConnectionManager.getInstance().getDB().getCollection(M_NOTIFICATIONS);
    BasicDBObject query = new BasicDBObject();
    query.put("timestamp", new BasicDBObject("$lt", System.currentTimeMillis()-24*60*60*1000));
//    query.put("isRead", true);
    DBCursor cursor = coll.find(query);
    while (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      coll.remove(doc);
    }
  }

  public void addNotification(String user, String type, String category, String categoryId, String content, String link)
  {
    DBCollection coll = db().getCollection(M_NOTIFICATIONS);
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
    DBCollection coll = db().getCollection(M_NOTIFICATIONS);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    if (categoryId!=null) query.put("categoryId", categoryId);
    if (category!=null) query.put("category", category);
    if (type!=null) query.put("type", type);
//    query.put("isRead", false);
    coll.remove(query);
//    DBCursor cursor = coll.find(query);
//    while (cursor.hasNext())
//    {
//      DBObject doc = cursor.next();
//      doc.put("isRead", true);
//      coll.save(doc, WriteConcern.SAFE);
//    }

  }

  public int getUnreadNotificationsTotal(String user)
  {
    return getUnreadNotificationsTotal(user, null, null, null);
  }


  public int getUnreadNotificationsTotal(String user, String type, String category, String categoryId)
  {
    int total = -1;
    DBCollection coll = db().getCollection(M_NOTIFICATIONS);
    BasicDBObject query = new BasicDBObject();

    query.put("user", user);
//    query.put("isRead", false);
    if (type!=null) query.put("type", type);
    if (category!=null) query.put("category", category);
    if (categoryId!=null) query.put("categoryId", categoryId);
    DBCursor cursor = coll.find(query);
    total = cursor.size();

    return total;
  }

  public int getNumberOfNotifications()
  {
    DBCollection coll = db().getCollection(M_NOTIFICATIONS);
    BasicDBObject query = new BasicDBObject();
    DBCursor cursor = coll.find(query);
    return cursor.count();
  }

  public int getNumberOfUnreadNotifications()
  {
    DBCollection coll = db().getCollection(M_NOTIFICATIONS);
    BasicDBObject query = new BasicDBObject();
//    query.put("isRead", false);
    DBCursor cursor = coll.find(query);
    return cursor.count();
  }


}
