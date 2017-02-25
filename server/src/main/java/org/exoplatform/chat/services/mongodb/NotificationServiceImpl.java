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

package org.exoplatform.chat.services.mongodb;

import com.mongodb.*;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.model.NotificationBean;
import org.exoplatform.chat.model.RoomBean;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.UserService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

@Named("notificationService")
@ApplicationScoped
public class NotificationServiceImpl implements org.exoplatform.chat.services.NotificationService
{

  private DB db(String dbName)
  {
    if (StringUtils.isEmpty(dbName)) {
      return ConnectionManager.getInstance().getDB();
    } else {
      return ConnectionManager.getInstance().getDB(dbName);
    }
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

  public void addNotification(String receiver, String sender, String type, String category, String categoryId,
                              String content, String link, String dbName) {
    addNotification(receiver, sender, type, category, categoryId, content, link, null, dbName);
  }

  public void addNotification(String receiver, String sender, String type, String category, String categoryId,
                              String content, String link, String options, String dbName) {
    // Do not set notification for some message type to avoid duplication with manual meeting (type-meeting-start, type-meeting-stop)
    if (options != null && (options.contains("call-on") || options.contains("call-off") || options.contains("call-proceed"))) {
      return;
    }

    DBCollection coll = db(dbName).getCollection(M_NOTIFICATIONS);
    BasicDBObject doc = new BasicDBObject();

    content = StringUtils.chomp(content);
    content = content.replaceAll("&", "&#38");
    content = content.replaceAll("<", "&lt;");
    content = content.replaceAll(">", "&gt;");
    content = content.replaceAll("\"", "&quot;");
    content = content.replaceAll("\n", "<br/>");
    content = content.replaceAll("\\\\", "&#92");
    content = content.replaceAll("\t", "  ");

    doc.put("timestamp", System.currentTimeMillis());
    doc.put("user", receiver);
    doc.put("from", sender);
    doc.put("type", type);
    doc.put("category", category);
    doc.put("categoryId", categoryId);
    doc.put("content", content);
    if (options != null) {
      options = options.replaceAll("<", "&lt;");
      options = options.replaceAll(">", "&gt;");
      options = options.replaceAll("'", "\\\\\"");
//      options = options.replaceAll("\"", "&quot;");
//      options = options.replaceAll("\\\\", "&#92");
      doc.put("options", options);
    }
    doc.put("link", link);
    doc.put("isRead", false);

    coll.insert(doc);
  }

  public void setNotificationsAsRead(String user, String type, String category, String categoryId, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_NOTIFICATIONS);
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

  @Override
  public List<NotificationBean> getUnreadNotifications(String user, UserService userService, String dbName) {
    return getUnreadNotifications(user, userService, null, null, null, dbName);
  }

  @Override
  public List<NotificationBean> getUnreadNotifications(String user, UserService userService, String type, String category, String categoryId, String dbName) {
    List<NotificationBean> notifications = new ArrayList<NotificationBean>();

    DBCollection coll = db(dbName).getCollection(M_NOTIFICATIONS);
    BasicDBObject query = new BasicDBObject();

    query.put("user", user);
//    query.put("isRead", false);
    if (type!=null) query.put("type", type);
    if (category!=null) query.put("category", category);
    if (categoryId!=null) query.put("categoryId", categoryId);
    DBCursor cursor = coll.find(query);

    while (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      NotificationBean notificationBean = new NotificationBean();
      notificationBean.setTimestamp((Long)doc.get("timestamp"));
      notificationBean.setUser(user);
      if (doc.containsField("from")) {
        notificationBean.setFrom(doc.get("from").toString());
        notificationBean.setFromFullName(userService.getUser(notificationBean.getFrom(), dbName).getFullname());
      }
      notificationBean.setCategory(doc.get("category").toString());
      notificationBean.setCategoryId(doc.get("categoryId").toString());
      notificationBean.setType(doc.get("type").toString());
      notificationBean.setContent(doc.get("content").toString());
      if (doc.containsField("options"))
      {
        notificationBean.setOptions(doc.get("options").toString());
      }
      RoomBean roomBean = userService.getRoom(user, notificationBean.getCategoryId(), dbName);
      notificationBean.setRoomType(roomBean.getType());
      if (roomBean.getType().equals(ChatService.TYPE_ROOM_SPACE) || roomBean.getType().equals(ChatService.TYPE_ROOM_TEAM)) {
        notificationBean.setRoomDisplayName(roomBean.getFullname());
      }
      notificationBean.setLink(doc.get("link").toString());

      notifications.add(notificationBean);
    }

    return notifications;
  }

  public int getUnreadNotificationsTotal(String user, String dbName)
  {
    return getUnreadNotificationsTotal(user, null, null, null, dbName);
  }


  public int getUnreadNotificationsTotal(String user, String type, String category, String categoryId, String dbName)
  {
    int total = -1;
    DBCollection coll = db(dbName).getCollection(M_NOTIFICATIONS);
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

  public int getNumberOfNotifications(String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_NOTIFICATIONS);
    BasicDBObject query = new BasicDBObject();
    DBCursor cursor = coll.find(query);
    return cursor.count();
  }

  public int getNumberOfUnreadNotifications(String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_NOTIFICATIONS);
    BasicDBObject query = new BasicDBObject();
//    query.put("isRead", false);
    DBCursor cursor = coll.find(query);
    return cursor.count();
  }


}
