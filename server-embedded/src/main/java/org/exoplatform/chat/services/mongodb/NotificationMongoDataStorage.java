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
import org.exoplatform.chat.model.NotificationSettingsBean;
import org.exoplatform.chat.model.RoomBean;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.NotificationDataStorage;
import org.exoplatform.chat.services.UserService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Named("notificationStorage")
@ApplicationScoped
@Singleton
public class NotificationMongoDataStorage implements NotificationDataStorage
{
  private static final Log LOG = ExoLogger.getLogger(NotificationMongoDataStorage.class);

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

  public void addNotification(String receiver, String sender, String type, String category, String categoryId,
                              String content, String link) {
    addNotification(receiver, sender, type, category, categoryId, content, link, null);
  }

  public void addNotification(String receiver, String sender, String type, String category, String categoryId,
                              String content, String link, String options) {
    // Do not set notification for some message type to avoid duplication with manual meeting (type-meeting-start, type-meeting-stop)
    if (options != null && (options.contains("call-on") || options.contains("call-off") || options.contains("call-proceed"))) {
      return;
    }

    DBCollection coll = db().getCollection(M_NOTIFICATIONS);
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

  public void setNotificationsAsRead(String user, String type, String category, String categoryId)
  {
    DBCollection coll = db().getCollection(M_NOTIFICATIONS);
    BasicDBObject query = buildQuery(user, type, category, categoryId);
    coll.remove(query);
  }

  @Override
  public List<NotificationBean> getUnreadNotifications(String user, UserService userService) {
    return getUnreadNotifications(user, userService, null, null, null);
  }

  @Override
  public List<NotificationBean> getUnreadNotifications(String user, UserService userService, String type, String category, String categoryId) {
    List<NotificationBean> notifications = new ArrayList<NotificationBean>();

    DBCursor cursor = find(user, type, category, categoryId);

    while (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      NotificationBean notificationBean = new NotificationBean();
      notificationBean.setTimestamp((Long)doc.get("timestamp"));
      notificationBean.setUser(user);
      if (doc.containsField("from")) {
        notificationBean.setFrom(doc.get("from").toString());
        notificationBean.setFromFullName(userService.getUser(notificationBean.getFrom()).getFullname());
      }
      notificationBean.setCategory(doc.get("category").toString());
      notificationBean.setCategoryId(doc.get("categoryId").toString());
      notificationBean.setType(doc.get("type").toString());
      notificationBean.setContent(doc.get("content").toString());
      if (doc.containsField("options"))
      {
        notificationBean.setOptions(doc.get("options").toString());
      }
      RoomBean roomBean = userService.getRoom(user, notificationBean.getCategoryId());
      if (roomBean != null) {
        notificationBean.setRoomType(roomBean.getType());
        if (roomBean.getType().equals(ChatService.TYPE_ROOM_SPACE) || roomBean.getType().equals(ChatService.TYPE_ROOM_TEAM)) {
          notificationBean.setRoomDisplayName(roomBean.getFullName());
        }
      }
      notificationBean.setLink(doc.get("link").toString());

      notifications.add(notificationBean);
    }
    notifications = filterNotifications(notifications, userService, user);

    return notifications;
  }

  private List<NotificationBean> filterNotifications(List<NotificationBean> notifications, UserService userService, String receiver) {
    List<NotificationBean> notificationBeans = new ArrayList<>();
    try {
      if (notifications != null && !notifications.isEmpty()) {
        NotificationSettingsBean settings = userService.getUserDesktopNotificationSettings(receiver);
        if (settings != null && settings.getEnabledRoomTriggers() != null) {
          String bean = settings.getEnabledRoomTriggers();
          JSONParser parser = new JSONParser();
          JSONObject json = (JSONObject) parser.parse(bean);
          for (NotificationBean notification : notifications) {
            JSONObject roomSettings = (JSONObject) json.get(notification.getCategoryId());
            if (roomSettings != null && roomSettings.get("notifCond") != null && roomSettings.get("notifCond").equals("silence")
                    && notification.getCategory().equals("room")) {
              continue;
            }
            notificationBeans.add(notification);
          }
        } else {
          return notifications;
        }
      }
    } catch (ParseException | JSONException e) {
      LOG.error("error parsing chat notifications data", e);
    }
    return notificationBeans;
  }

  public int getUnreadNotificationsTotal(String user)
  {
    return getUnreadNotificationsTotal(user, null, null, null);
  }


  public int getUnreadNotificationsTotal(String user, String type, String category, String categoryId)
  {
    DBCursor cursor = find(user, type, category, categoryId);
    int total = cursor.size();
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
    DBCursor cursor = coll.find(query);
    return cursor.count();
  }

  private DBCursor find(String user, String type, String category, String categoryId) {
    DBCollection coll = db().getCollection(M_NOTIFICATIONS);
    BasicDBObject query = buildQuery(user, type, category, categoryId);
    return coll.find(query);
  }

  private BasicDBObject buildQuery(String user, String type, String category, String categoryId) {
    BasicDBObject query = new BasicDBObject();

    query.put("user", user);
    if (type != null) query.put("type", type);
    if (category != null) query.put("category", category);
    if (categoryId != null) query.put("categoryId", categoryId);
    return query;
  }
}
