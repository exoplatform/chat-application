/*
 * Copyright (C) 2023 eXo Platform SAS.
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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
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
  public static final String TIMESTAMP = "timestamp";
  public static final String USER = "user";
  public static final String FROM = "from";
  public static final String TYPE = "type";
  public static final String CATEGORY = "category";
  public static final String CATEGORY_ID = "categoryId";
  public static final String CONTENT = "content";
  public static final String OPTIONS = "options";

  private MongoDatabase db()
  {
    return ConnectionManager.getInstance().getDB();
  }

  public static void cleanupNotifications() {
    MongoCollection<Document> coll = ConnectionManager.getInstance().getDB().getCollection(M_NOTIFICATIONS);
    Bson query = Filters.lt(TIMESTAMP, System.currentTimeMillis() - 24*60*60*1000);
    coll.deleteMany(query);
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

    MongoCollection<Document> notificationsCollection = db().getCollection(M_NOTIFICATIONS);
    Document doc = new Document();

    content = StringUtils.chomp(content);
    content = content.replace("&", "&#38");
    content = content.replace("<", "&lt;");
    content = content.replace(">", "&gt;");
    content = content.replace("\"", "&quot;");
    content = content.replace("\n", "<br/>");
    content = content.replace("\\\\", "&#92");
    content = content.replace("\t", "  ");

    doc.put(TIMESTAMP, System.currentTimeMillis());
    doc.put(USER, receiver);
    doc.put(FROM, sender);
    doc.put(TYPE, type);
    doc.put(CATEGORY, category);
    doc.put(CATEGORY_ID, categoryId);
    doc.put(CONTENT, content);
    if (options != null) {
      options = options.replace("<", "&lt;");
      options = options.replace(">", "&gt;");
      options = options.replace("'", "\\\\\"");
      doc.put(OPTIONS, options);
    }
    doc.put("link", link);
    doc.put("isRead", false);

    notificationsCollection.insertOne(doc);
  }

  public void setNotificationsAsRead(String user, String type, String category, String categoryId)
  {
    MongoCollection<Document> coll = db().getCollection(M_NOTIFICATIONS);
    Bson query = buildQuery(user, type, category, categoryId);
    coll.deleteMany(query);
  }

  @Override
  public List<NotificationBean> getUnreadNotifications(String user, UserService userService) {
    return getUnreadNotifications(user, userService, null, null, null);
  }

  @Override
  public List<NotificationBean> getUnreadNotifications(String user, UserService userService, String type, String category, String categoryId) {
    List<NotificationBean> notifications = new ArrayList<>();

    try(MongoCursor<Document> cursor = find(user, type, category, categoryId)) {

      while (cursor.hasNext()) {
        Document doc = cursor.next();
        NotificationBean notificationBean = new NotificationBean();
        notificationBean.setTimestamp((Long) doc.get(TIMESTAMP));
        notificationBean.setUser(user);
        if (doc.containsKey(FROM)) {
          notificationBean.setFrom(doc.get(FROM).toString());
          notificationBean.setFromFullName(userService.getUser(notificationBean.getFrom()).getFullname());
        }
        notificationBean.setCategory(doc.get(CATEGORY).toString());
        notificationBean.setCategoryId(doc.get(CATEGORY_ID).toString());
        notificationBean.setType(doc.get(TYPE).toString());
        notificationBean.setContent(doc.get(CONTENT).toString());
        if (doc.containsKey(OPTIONS)) {
          notificationBean.setOptions(doc.get(OPTIONS).toString());
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
    }

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
    try (MongoCursor<Document> cursor = find(user, type, category, categoryId)) {
      return cursor.available();
    }
  }

  public int getNumberOfNotifications()
  {
    MongoCollection<Document> coll = db().getCollection(M_NOTIFICATIONS);
    try(MongoCursor<Document> cursor = coll.find(new Document()).cursor()){
      return cursor.available();
    }
  }

  public int getNumberOfUnreadNotifications()
  {
    MongoCollection<Document> coll = db().getCollection(M_NOTIFICATIONS);
    try(MongoCursor<Document> cursor = coll.find(new Document()).cursor()) {
      return cursor.available();
    }
  }

  private MongoCursor<Document> find(String user, String type, String category, String categoryId) {
    MongoCollection<Document> coll = db().getCollection(M_NOTIFICATIONS);
    Bson query = buildQuery(user, type, category, categoryId);
    return coll.find(query).cursor();
  }

  private Bson buildQuery(String user, String type, String category, String categoryId) {
    Bson query = Filters.eq(USER, user);
    if (type != null) query = Filters.and(query, Filters.eq(TYPE, type));
    if (category != null) query = Filters.and(query, Filters.eq(CATEGORY, category));
    if (categoryId != null) query = Filters.and(query, Filters.eq(CATEGORY_ID, categoryId));
    return query;
  }
}
