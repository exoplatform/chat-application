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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.model.*;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.UserDataStorage;
import org.exoplatform.chat.utils.ChatUtils;
import org.json.JSONException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Named("userStorage")
@ApplicationScoped
@Singleton
public class UserMongoDataStorage implements UserDataStorage {

  private static final Logger LOG = Logger.getLogger(UserMongoDataStorage.class.getName());

  public static final String M_USERS_COLLECTION = "users";
  public static final String M_ROOMS_COLLECTION = "rooms";

  public static final String DEFAULT_ENABLED_CHANNELS  = "[ \"desktop\" , \"on-site\" , \"bip\"]";
  public static final String FAVORITES = "favorites";
  public static final String USER = "user";
  public static final String FULLNAME = "fullname";
  public static final String IS_ENABLED = "isEnabled";
  public static final String IS_DELETED = "isDeleted";
  public static final String ID = "_id";
  public static final String EMAIL = "email";
  public static final String IS_EXTERNAL = "isExternal";
  public static final String SPACE_ID = "space_id";
  public static final String DISPLAY_NAME = "displayName";
  public static final String GROUP_ID = "groupId";
  public static final String SHORT_NAME = "shortName";
  public static final String PRETTY_NAME = "prettyName";
  public static final String TYPE = "type";
  public static final String SPACES = "spaces";
  public static final String MEETING_STARTED = "meetingStarted";
  public static final String START_TIME = "startTime";
  public static final String TIMESTAMP = "timestamp";
  public static final String USERS = "users";
  public static final String TEAMS = "teams";
  public static final String STATUS = "status";
  public static final String TRUE = "true";
  public static final String EMPTY_STRING = "";
  public static final String FALSE = "false";
  public static final String IS_SUPPORT_ADMIN = "isSupportAdmin";

  private MongoDatabase db()
  {
    return ConnectionManager.getInstance().getDB();
  }

  @Override
  public void addFavorite(String user, String targetUser) {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext())
    {
     Document doc = cursor.next();
      List<String> favorites = new ArrayList<>();
      if (doc.containsKey(FAVORITES)) {
        favorites = (List<String>)doc.get(FAVORITES);
      }
      if (!favorites.contains(targetUser)) {
        favorites.add(targetUser);
        Bson update = Updates.set(FAVORITES, favorites);
        coll.updateOne(query, update);
      }
    }
  }

  @Override
  public void removeFavorite(String user, String targetUser)
  {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext())
    {
     Document doc = cursor.next();
     List<String> favorites;
      if (doc.containsKey(FAVORITES)) {
        favorites = (List<String>)doc.get(FAVORITES);
        if (favorites.contains(targetUser)) {
          favorites.remove(targetUser);
          Bson update = Updates.set(FAVORITES, favorites);
          coll.updateOne(query, update);
        }
      }
    }
  }

  @Override
  public void setPreferredNotification(String user, String notifManner) throws Exception {
    MongoCollection<Document> usersCollection = db().getCollection(M_USERS_COLLECTION);
    Bson query = Filters.eq(USER, user);
    MongoCursor<Document> cursor = usersCollection.find(query).cursor();
    if (cursor.hasNext()) {
      Document doc = cursor.next();
      if(ChatService.BIP.equals(notifManner) || ChatService.DESKTOP_NOTIFICATION.equals(notifManner) || ChatService.ON_SITE.equals(notifManner)) {
        Document settings = (Document) doc.get(NOTIFICATIONS_SETTINGS);
        Object prefNotif = null;
        Object prefTriger = null;
        Object existingRoomNotif =null;
        if (settings != null) {
          prefNotif = settings.get(PREFERRED_NOTIFICATION);
          prefTriger = settings.get(PREFERRED_NOTIFICATION_TRIGGER);
          existingRoomNotif = settings.get(PREFERRED_ROOM_NOTIFICATION_TRIGGER);
        } else {
          settings = new Document();
        }
        List<String> existingPrefNotif = null;
        if(prefNotif==null) {
          existingPrefNotif = new ArrayList<>();
          //default values to the untouched settings
          existingPrefNotif.add("on-site");
          existingPrefNotif.add("desktop");
          existingPrefNotif.add("bip");
        } else {
          existingPrefNotif = ((List<String>)prefNotif);
        }
        if(existingPrefNotif.contains(notifManner)) {
          existingPrefNotif.remove(notifManner);
        } else {
          existingPrefNotif.add(notifManner);
        }

        settings.put(PREFERRED_NOTIFICATION,existingPrefNotif);
        if(prefTriger!=null) {
          settings.put(PREFERRED_NOTIFICATION_TRIGGER, prefTriger);
        }
        if(existingRoomNotif!=null){
          settings.put(PREFERRED_ROOM_NOTIFICATION_TRIGGER, existingRoomNotif);
        }

        Bson update = Updates.set(NOTIFICATIONS_SETTINGS, settings);

        usersCollection.updateOne(query, update);
      } else {
        throw new Exception("Wrong Params, operation not done");
      }
    } else {
      throw new Exception("Doc not found, operation not done");
    }
  }

  @Override
  public void setNotificationTrigger(String user, String notifCond) throws Exception {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    Bson query = Filters.eq(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext()) {
     Document doc = cursor.next();
      if(ChatService.NOTIFY_ME_EVEN_NOT_DISTURB.equals(notifCond) || ChatService.NOTIFY_ME_WHEN_MENTION.equals(notifCond)) {
        Document settings = (Document) doc.get(NOTIFICATIONS_SETTINGS);
        Object prefNotif = null;
        Object prefTriger = null;
        Object existingRoomNotif =null;
        if (settings != null) {
          prefNotif = settings.get(PREFERRED_NOTIFICATION_TRIGGER);
          prefTriger = settings.get(PREFERRED_NOTIFICATION);
          existingRoomNotif = settings.get(PREFERRED_ROOM_NOTIFICATION_TRIGGER);
        } else {
          settings = new Document();
        }
        List<String> existingPrefNotif;
        if(prefNotif==null) {
          existingPrefNotif = new ArrayList<>();
        } else {
          existingPrefNotif = ((List<String>)prefNotif);
        }

        if(existingPrefNotif.contains(notifCond)) {
          existingPrefNotif.remove(notifCond);
        } else {
          existingPrefNotif.add(notifCond);
        }

        settings.put(PREFERRED_NOTIFICATION_TRIGGER, existingPrefNotif);
        if (prefTriger != null) {
          settings.put(PREFERRED_NOTIFICATION, prefTriger);
        }
        if (existingRoomNotif != null) {
          settings.put(PREFERRED_ROOM_NOTIFICATION_TRIGGER, existingRoomNotif);
        }
        Bson update = Updates.set(NOTIFICATIONS_SETTINGS, settings);

        coll.updateOne(query, update);
      } else {
        throw new Exception("Wrong Params, operation not done");
      }
    }  else {
      throw new Exception("Doc not found, operation not done");
    }
  }

  @Override
  public void setRoomNotificationTrigger(String user, String room, String notifCond, String notifConditionType, long time) throws Exception {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    Bson query = Filters.eq(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext()) {
     Document doc = cursor.next();

      if(ChatService.NOTIFY_ME_ON_ROOM_NORMAL.equals(notifConditionType) || ChatService.DO_NOT_NOTIFY_ME_ON_ROOM.equals(notifConditionType) || notifConditionType.startsWith(ChatService.NOTIFY_ME_ON_ROOM_KEY_WORD)) {
        Document settings = (Document) doc.get(NOTIFICATIONS_SETTINGS);
        Object prefNotif = null;
        Object prefTriger = null;
        Document existingRoomNotif =null;
        if(settings!=null) {
          prefTriger =  settings.get(PREFERRED_NOTIFICATION_TRIGGER);
          prefNotif = settings.get(PREFERRED_NOTIFICATION);
          existingRoomNotif = (Document) settings.get(PREFERRED_ROOM_NOTIFICATION_TRIGGER);
        } else {
          settings = new Document();
        }

        if(existingRoomNotif == null) {
          existingRoomNotif = new Document();
        }

        Document notifData = (Document) existingRoomNotif.get(room);
        if (notifData == null) {
          notifData = new Document();
        }

        if (notifData.get("time") == null || (long) notifData.get("time") < time) {
          notifData.put("notifCond", notifConditionType);
          notifData.put("time", time);
          if (UserDataStorage.ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD.equals(notifConditionType)) {
            notifData.put(UserDataStorage.ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD, notifCond);
          }
        }

        if(prefTriger!=null) {
          settings.put(PREFERRED_NOTIFICATION_TRIGGER, prefTriger);
        }

        if(prefNotif!=null) {
          settings.put(PREFERRED_NOTIFICATION,prefNotif);
        }

        if(existingRoomNotif!=null){
          existingRoomNotif.put(room, notifData);
          settings.put(PREFERRED_ROOM_NOTIFICATION_TRIGGER, existingRoomNotif);
        }

        coll.updateOne(query, Updates.set(NOTIFICATIONS_SETTINGS, settings)                                                                                                                                                                    );
      } else {
        throw new Exception("Wrong Params, operation not done");
      }
    } else {
      throw new Exception("Doc not found, operation not done");
    }
  }
  /*
  * This methode is responsible for getting all desktop settings in a single object
  */
  @Override
  public NotificationSettingsBean getUserDesktopNotificationSettings(String user) throws JSONException {
    NotificationSettingsBean settings = new NotificationSettingsBean();
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext()) {
      Document doc = cursor.next();
      Document wrapperDoc = (Document) doc.get(NOTIFICATIONS_SETTINGS);
      if(wrapperDoc == null){//when there is no settings - first start of the server
        wrapperDoc = new Document();
      }

      if(wrapperDoc.get(UserDataStorage.PREFERRED_NOTIFICATION) != null){
        settings.setEnabledChannels(wrapperDoc.get(UserDataStorage.PREFERRED_NOTIFICATION).toString());
      } else {
        //default values to the untouched settings
        settings.setEnabledChannels(DEFAULT_ENABLED_CHANNELS);
      }
      if(wrapperDoc.get(UserDataStorage.PREFERRED_NOTIFICATION_TRIGGER) != null){
        Document preferredNotificationsTrigger = (Document)(wrapperDoc.get(UserDataStorage.PREFERRED_NOTIFICATION_TRIGGER));
        settings.setEnabledTriggers(preferredNotificationsTrigger.toJson());
      }
      if(wrapperDoc.get(PREFERRED_ROOM_NOTIFICATION_TRIGGER) != null) {
        Document preferredRoomNotificationTrigger = (Document) wrapperDoc.get(PREFERRED_ROOM_NOTIFICATION_TRIGGER);
        settings.setEnabledRoomTriggers(preferredRoomNotificationTrigger.toJson());
      }
    }
    return settings;
  }

  @Override
  public boolean isFavorite(String user, String targetUser) {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext())
    {
      Document doc = cursor.next();
      if (doc.containsKey(FAVORITES)) {
        List<String> favorites = (List<String>)doc.get(FAVORITES);
        if (favorites.contains(targetUser))
          return true;
      }
    }
    return false;
  }

  @Override
  public void addUserFullName(String user, String fullName) {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    Bson query = Filters.eq(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (!cursor.hasNext()) {
      Document doc = new Document();
      doc.put(ID, user);
      doc.put(USER, user);
      doc.put(FULLNAME, fullName);
      doc.put(IS_ENABLED, Boolean.TRUE.toString());
      doc.put(IS_DELETED, Boolean.FALSE.toString());
      coll.insertOne(doc);
    } else {
      Bson update = Updates.set(FULLNAME, fullName);
      coll.updateOne(query, update);
    }
  }

  @Override
  public void addUserEmail(String user, String email) {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    Bson query = Filters.eq(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (!cursor.hasNext()) {
      Document doc = new Document();
      doc.put(ID, user);
      doc.put(USER, user);
      doc.put(EMAIL, email);
      doc.put(IS_ENABLED, Boolean.TRUE.toString());
      doc.put(IS_DELETED, Boolean.FALSE.toString());
      coll.insertOne(doc);
    } else {
      Bson update = Updates.set(EMAIL, email);
      coll.updateOne(query, update);
    }
  }

  @Override
  public void deleteUser(String user) {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    Bson query = Filters.eq(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext()) {
      Bson update = Updates.combine(Updates.set(IS_DELETED, Boolean.TRUE.toString()), Updates.set(IS_ENABLED, Boolean.FALSE.toString()));
      coll.updateOne(query, update);
    }
  }

  @Override
  public void setEnabledUser(String user, Boolean isEnabled) {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    Bson query = Filters.eq(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext()) {
      Bson update = Updates.combine(Updates.set(IS_DELETED, Boolean.FALSE.toString()), Updates.set(IS_ENABLED, isEnabled.toString()));
      coll.updateOne(query, update);
    }
  }

  @Override
  public void setExternalUser(String user, String isExternal) {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    Bson query = Filters.eq(USER, user);
    Bson update = Updates.set(IS_EXTERNAL, isExternal);
    coll.updateOne(query, update);
  }

  @Override
  public void setSpaces(String user, List<SpaceBean> spaces) {
    List<String> spaceIds = new ArrayList<>();
    MongoCollection<Document> roomsCollection = db().getCollection(M_ROOMS_COLLECTION);
    for (SpaceBean bean:spaces) {
      String room = ChatUtils.getRoomId(bean.getId());
      spaceIds.add(room);
      Bson query = Filters.eq(ID, room);
      MongoCursor<Document> cursor = roomsCollection.find(query).cursor();
      if (!cursor.hasNext()) {
        Document doc = new Document();
        doc.put(ID, room);
        doc.put(SPACE_ID, bean.getId());
        doc.put(DISPLAY_NAME, bean.getDisplayName());
        doc.put(GROUP_ID, bean.getGroupId());
        doc.put(SHORT_NAME, bean.getShortName());
        doc.put(PRETTY_NAME, bean.getPrettyName());
        doc.put(TYPE, ChatService.TYPE_ROOM_SPACE);
        doc.put(IS_ENABLED, true);
        roomsCollection.insertOne(doc);
      } else {
        Document doc = cursor.next();
        String displayName = doc.get(DISPLAY_NAME).toString();
        Object prettyName = doc.get(PRETTY_NAME);
        if (!bean.getDisplayName().equals(displayName) || prettyName == null || !bean.getPrettyName().equals(prettyName.toString())) {
          Bson update = Updates.combine(Updates.set(ID, room),
                                        Updates.set(DISPLAY_NAME, bean.getDisplayName()),
                                        Updates.set(GROUP_ID, bean.getGroupId()),
                                        Updates.set(SHORT_NAME, bean.getShortName()),
                                        Updates.set(PRETTY_NAME, bean.getPrettyName()),
                                        Updates.set(IS_ENABLED, true));
          roomsCollection.updateOne(query, update);
        }
      }
    }
    MongoCollection<Document> usersCollection = db().getCollection(M_USERS_COLLECTION);
    Bson query = Filters.eq(USER, user);
    MongoCursor<Document> cursor = usersCollection.find(query).cursor();
    if (cursor.hasNext()) {
      Bson doc = Updates.set(SPACES, spaceIds);
      usersCollection.updateOne(query, doc);
    } else {
      Document doc = new Document();
      doc.append(ID, user);
      doc.append(USER, user);
      doc.append(SPACES, spaceIds);
      doc.append(IS_ENABLED, Boolean.toString(true));
      doc.append(IS_DELETED, Boolean.toString(false));
      usersCollection.insertOne(doc);
    }
  }

  @Override
  public void addTeamRoom(String user, String teamRoomId) {
    List<String> teamIds = new ArrayList<String>();
    teamIds.add(teamRoomId);
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext()) {
      Document doc = cursor.next();
      Bson updateDoc;
      if (doc.containsKey(TEAMS)) {
        List<String> existingTeams = ((List<String>)doc.get(TEAMS));
        if (!existingTeams.contains(teamRoomId))
          existingTeams.add(teamRoomId);
        updateDoc = Updates.set(TEAMS, existingTeams);
      } else {
        updateDoc = Updates.set(TEAMS, teamIds);
      }
      coll.updateOne(query, updateDoc);
    } else {
      Document doc = new Document();
      doc.put(ID, user);
      doc.put(USER, user);
      doc.put(TEAMS, teamIds);
      coll.insertOne(doc);
    }
  }

  @Override
  public void removeTeamUsers(String teamRoomId, List<String> users) {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    for (String user:users) {
      BasicDBObject query = new BasicDBObject();
      query.put(USER, user);
      MongoCursor<Document> cursor = coll.find(query).cursor();
      if (cursor.hasNext()) {
        Document doc = cursor.next();
        if (doc.containsKey(TEAMS)) {
          List<String> teams = (List<String>)doc.get(TEAMS);
          if (teams.contains(teamRoomId)) {
            teams.remove(teamRoomId);
            doc.put(TEAMS, teams);
            coll.replaceOne(query, doc);
          }
        }
      }
    }
  }

  private RoomBean getTeam(String teamId) {
    RoomBean roomBean = null;
    MongoCollection<Document> coll = db().getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put(ID, teamId);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext())
    {
      Document doc = cursor.next();
      roomBean = new RoomBean();
      roomBean.setRoom(teamId);
      roomBean.setUser(doc.get(USER).toString());
      roomBean.setFullName(doc.get("team").toString());
      roomBean.setType(doc.get(TYPE).toString());
      if (doc.containsKey(IS_ENABLED)) {
        roomBean.setEnabledRoom((StringUtils.equals(doc.get(IS_ENABLED).toString(), TRUE)));
      }
      if (doc.containsKey(MEETING_STARTED)) {
        roomBean.setMeetingStarted((Boolean) doc.get(MEETING_STARTED));
      }
      if (doc.containsKey(START_TIME)) {
        roomBean.setStartTime((String) doc.get(START_TIME));
      }
      if (doc.containsKey(TIMESTAMP))
      {
        roomBean.setTimestamp(((Long) doc.get(TIMESTAMP)).longValue());
      }
      if (StringUtils.isNotBlank(roomBean.getUser())) {
        roomBean.setAdmins(new String[]{roomBean.getUser()});
      }
    }

    return roomBean;
  }

  @Override
  public List<RoomBean> getTeams(String user) {
    List<RoomBean> rooms = new ArrayList<RoomBean>();
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext())
    {
      Document doc = cursor.next();

      List<String> listrooms = ((List<String>)doc.get(TEAMS));
      if (listrooms!=null)
      {
        for (String room:listrooms)
        {
          rooms.add(getTeam(room));
        }
      }

    }
    return rooms;
  }

  @Override
  public RoomBean getRoom(String user, String roomId) {
    RoomBean roomBean = null;
    MongoCollection<Document> coll = db().getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put(ID, roomId);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext())
    {
      roomBean = new RoomBean();
      roomBean.setRoom(roomId);

      Document doc = cursor.next();
      if (doc.containsKey(TIMESTAMP))
      {
        roomBean.setTimestamp(((Long) doc.get(TIMESTAMP)).longValue());
      }
      String type = doc.get(TYPE).toString();
      roomBean.setType(type);
      if (doc.containsKey(IS_ENABLED)) {
        roomBean.setEnabledRoom((StringUtils.equals(doc.get(IS_ENABLED).toString(), TRUE)));
      }
      if (ChatService.TYPE_ROOM_SPACE.equals(type))
      {
        roomBean.setPrettyName(doc.get(PRETTY_NAME).toString());
        roomBean.setUser(ChatService.SPACE_PREFIX+roomId);
        roomBean.setFullName(doc.get(DISPLAY_NAME).toString());
        if (doc.containsKey(MEETING_STARTED)) {
          roomBean.setMeetingStarted((Boolean) doc.get(MEETING_STARTED));
        }
        if (doc.containsKey(START_TIME)) {
          roomBean.setStartTime((String) doc.get(START_TIME));
        }
      }
      else if (ChatService.TYPE_ROOM_TEAM.equals(type))
      {
        roomBean.setUser(ChatService.TEAM_PREFIX+roomId);
        roomBean.setFullName(doc.get("team").toString());
        String creator = (String) doc.get(USER);
        roomBean.setAdmins(new String[]{creator});
        if (doc.containsKey(MEETING_STARTED)) {
          roomBean.setMeetingStarted((Boolean) doc.get(MEETING_STARTED));
        }
        if (doc.containsKey(START_TIME)) {
          roomBean.setStartTime((String) doc.get(START_TIME));
        }
      }
      else if (ChatService.TYPE_ROOM_USER.equals(type))
      {
        List<String> users = ((List<String>)doc.get(USERS));
        users.remove(user);
        String targetUser = users.get(0);
        roomBean.setUser(targetUser);
        roomBean.setFullName(this.getUserFullName(targetUser));
        roomBean.setExternal(this.getExternalValue(targetUser));
      }
      else if (ChatService.TYPE_ROOM_EXTERNAL.equals(type))
      {
        roomBean.setUser(ChatService.EXTERNAL_PREFIX+roomId);
        roomBean.setFullName(doc.get("identifier").toString());
      }
    }

    return roomBean;
  }

  private SpaceBean getSpace(String roomId)
  {
    SpaceBean spaceBean = null;
    MongoCollection<Document> coll = db().getCollection(M_ROOMS_COLLECTION);
    Bson query = Filters.eq(ID, roomId);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext())
    {
      Document doc = cursor.next();
      spaceBean = new SpaceBean();
      spaceBean.setRoom(roomId);
      spaceBean.setId(doc.get(SPACE_ID).toString());
      spaceBean.setDisplayName(doc.get(DISPLAY_NAME).toString());
      spaceBean.setGroupId(doc.get(GROUP_ID).toString());
      spaceBean.setShortName(doc.get(SHORT_NAME).toString());
      if (doc.containsKey(PRETTY_NAME)) {
        spaceBean.setPrettyName(doc.get(PRETTY_NAME).toString());
      }
      if (doc.containsKey(MEETING_STARTED)) {
        spaceBean.setMeetingStarted((Boolean) doc.get(MEETING_STARTED));
      }
      if (doc.containsKey(START_TIME)) {
        spaceBean.setStartTime((String) doc.get(START_TIME));
      }
      if (doc.containsKey(TIMESTAMP))
      {
        spaceBean.setTimestamp(((Long)doc.get(TIMESTAMP)).longValue());
      }
    }

    return spaceBean;
  }

  @Override
  public List<SpaceBean> getSpaces(String user)
  {
    List<SpaceBean> spaces = new ArrayList<>();
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    Bson query = Filters.eq(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext())
    {
      Document doc = cursor.next();

      List<String> listspaces = ((List<String>)doc.get(SPACES));
      if (listspaces!=null)
      {
        for (String space:listspaces)
        {
          spaces.add(getSpace(space));
        }
      }

    }
    return spaces;
  }

  @Override
  public List<UserBean> getUsersInRoomChatOneToOne(String roomId) {
    List<UserBean> users = new ArrayList<UserBean>();
    MongoCollection<Document> coll = db().getCollection(M_ROOMS_COLLECTION);
    Bson query = Filters.eq(ID, roomId);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    while (cursor.hasNext()) {
      Document doc = cursor.next();
      Object objectUsers = doc.get(USERS);
      ArrayList myArrayList = (ArrayList) objectUsers;
      for (Object o : myArrayList) {
        users.add(getUser(o.toString()));
      }
    }
    return users;
  }

  public List<UserBean> getUsers(String roomId, String filter, int limit) {
    return getUsers(roomId, null, filter, limit, false);
  }

  /**
   *
   * @param roomId room ID
   * @param onlineUsers list of online users
   * @param filter text to filter users by fullname or username
   * @param limit the limit of users to load
   * @return
   */
  public List<UserBean> getUsers(String roomId, List<String> onlineUsers, String filter, int limit, boolean onlyOnlineUsers) {
    if (roomId == null && filter == null) {
      throw new IllegalArgumentException();
    }

    List<Bson> andList = new ArrayList<>();
    if (StringUtils.isNotBlank(roomId)) {
      // removing "space-" and "team-" prefix
      if (roomId.indexOf(ChatService.SPACE_PREFIX) == 0) {
        roomId = roomId.substring(ChatService.SPACE_PREFIX.length());
      } else if (roomId.indexOf(ChatService.TEAM_PREFIX) == 0) {
        roomId = roomId.substring(ChatService.TEAM_PREFIX.length());
      }

      ArrayList<Bson> orList = new ArrayList<>();
      orList.add(Filters.eq(SPACES, roomId));
      orList.add(Filters.eq(TEAMS, roomId));

      andList.add(Filters.or(orList));
    }

    if (filter != null) {
      filter = filter.replace(" ", ".*");
      Pattern regex = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);
      ArrayList<Bson> orList = new ArrayList<>();
      orList.add(Filters.eq(USER, regex));
      orList.add(Filters.eq(FULLNAME, regex));

      andList.add(Filters.or(orList));
    }

    List<UserBean> users = null;

    // Load online users
    if(onlineUsers != null && !onlineUsers.isEmpty()) {
      Bson fetchOnlineUsers = Filters.in(USER, onlineUsers);
      List<Bson> clonedAndList = new ArrayList<>(andList);
      clonedAndList.add(fetchOnlineUsers);
      Bson query = Filters.and(clonedAndList);
      MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
      MongoCursor<Document> cursor = coll.find(query).limit(limit).cursor();
      users = new ArrayList<>();
      while (cursor.hasNext()) {
        Document doc = cursor.next();
        UserBean userBean = new UserBean();
        userBean.setName(doc.get(USER).toString());
        Object prop = doc.get(FULLNAME);
        userBean.setFullname((prop != null) ? prop.toString() : EMPTY_STRING);
        prop = doc.get(EMAIL);
        userBean.setEmail((prop != null) ? prop.toString() : EMPTY_STRING);
        prop = doc.get(STATUS);
        userBean.setStatus((prop != null) ? prop.toString() : EMPTY_STRING);
        if (doc.get(IS_ENABLED) != null) {
          userBean.setEnabled(StringUtils.equals(doc.get(IS_ENABLED).toString(), TRUE));
        }
        if (doc.get(IS_DELETED) != null) {
          userBean.setDeleted(StringUtils.equals(doc.get(IS_DELETED).toString(), TRUE));
        }
        if (doc.get(IS_EXTERNAL) != null) {
          userBean.setExternal(doc.get(IS_EXTERNAL).toString());
        }
        users.add(userBean);
      }
    }

    users = users == null ? new ArrayList<>() : users;

    if(!onlyOnlineUsers) {
      int usersLeft = limit > 0 ? limit - users.size() : -1;

      if (usersLeft > 0 || (limit >= 0 && users.isEmpty())) {
        usersLeft = usersLeft > 0 ? usersLeft : limit;
        Bson escapeOnlineUsers;
        if (onlineUsers != null && !onlineUsers.isEmpty()) {
          escapeOnlineUsers = Filters.nin(USER, onlineUsers);
          andList.add(escapeOnlineUsers);
        }
        Bson query = Filters.and(andList);
        MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
        MongoCursor<Document> cursor = coll.find(query).limit(usersLeft).cursor();
        while (cursor.hasNext()) {
          Document doc = cursor.next();
          UserBean userBean = new UserBean();
          userBean.setName(doc.get(USER).toString());
          Object prop = doc.get(FULLNAME);
          userBean.setFullname((prop != null) ? prop.toString() : EMPTY_STRING);
          prop = doc.get(EMAIL);
          userBean.setEmail((prop != null) ? prop.toString() : EMPTY_STRING);
          prop = doc.get(STATUS);
          userBean.setStatus((prop != null) ? prop.toString() : EMPTY_STRING);
          if (doc.get(IS_ENABLED) != null) {
            userBean.setEnabled(StringUtils.equals(doc.get(IS_ENABLED).toString(), TRUE));
          }
          if (doc.get(IS_DELETED) != null) {
            userBean.setDeleted(StringUtils.equals(doc.get(IS_DELETED).toString(), TRUE));
          }
          if (doc.get(IS_EXTERNAL) != null) {
            userBean.setExternal(doc.get(IS_EXTERNAL).toString());
          }
          users.add(userBean);
        }
      }
    }

    users = users.stream().filter(UserBean::isEnabledUser).collect(Collectors.toList());
    return users;
  }

  @Override
  public Long getUsersCount(String roomId, String filter) {
    return getUsersCount(roomId, filter, false);
  }

  @Override
  public Long getUsersCount(String roomId, String filter, boolean activeUsers) {
    if (roomId == null && filter == null) {
      throw new IllegalArgumentException();
    }

    List<Bson> andList = new ArrayList<>();
    if (StringUtils.isNotBlank(roomId)) {
      // removing "space-" and "team-" prefix
      if (roomId.indexOf(ChatService.SPACE_PREFIX) == 0) {
        roomId = roomId.substring(ChatService.SPACE_PREFIX.length());
      } else if (roomId.indexOf(ChatService.TEAM_PREFIX) == 0) {
        roomId = roomId.substring(ChatService.TEAM_PREFIX.length());
      }

      ArrayList<Bson> orList = new ArrayList<>();
      orList.add(Filters.eq(SPACES, roomId));
      orList.add(Filters.eq(TEAMS, roomId));

      andList.add(Filters.or(orList));
    }

    if (filter != null) {
      filter = filter.replace(" ", ".*");
      Pattern regex = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);
      ArrayList<Bson> orList = new ArrayList<>();
      orList.add(Filters.eq(USER, regex));
      orList.add(Filters.eq(FULLNAME, regex));

      andList.add(Filters.or(orList));
    }
    andList.add(Filters.eq(IS_DELETED, FALSE));
    if (activeUsers) {
      andList.add(Filters.eq(STATUS, STATUS_AVAILABLE));
    }
    List<Bson> orList = new ArrayList<>();
    orList.add(Filters.eq(IS_ENABLED, TRUE));
    //some entries were added with boolean values
    orList.add(Filters.eq(IS_ENABLED, true));
    andList.add(Filters.or(orList));
    Bson query = Filters.and(andList);
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    return (long) coll.find(query).cursor().available();
  }

  @Override
  public String setStatus(String user, String status) {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    Bson query = Filters.eq(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext()) {
      Document doc = cursor.next();
      doc.put(STATUS, status);
      coll.replaceOne(query, doc);
    } else {
      Document doc = new Document();
      doc.put(ID, user);
      doc.put(USER, user);
      doc.put(STATUS, status);
      coll.insertOne(doc);
    }
    return status;
  }

  @Override
  public void setAsAdmin(String user, boolean isAdmin) {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    Bson query = Filters.eq(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext()) {
      Document doc = cursor.next();
      doc.put(IS_SUPPORT_ADMIN, isAdmin);
      coll.replaceOne(query, doc);
    } else {
      Document doc = new Document();
      doc.put(ID, user);
      doc.put(USER, user);
      doc.put(IS_SUPPORT_ADMIN, isAdmin);
      coll.insertOne(doc);
    }
  }

  @Override
  public boolean isAdmin(String user)
  {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext()) {
      Document doc = cursor.next();
      Object isAdmin = doc.get(IS_SUPPORT_ADMIN);
      return (isAdmin != null && TRUE.equals(isAdmin.toString()));
    }
    return false;
  }

  @Override
  public String getStatus(String user)
  {
    String status = STATUS_NONE;
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext())
    {
      Document doc = cursor.next();
      if (doc.containsKey(STATUS))
        status = doc.get(STATUS).toString();
      else
        status = setStatus(user, STATUS_AVAILABLE);
    }
    else
    {
      status = setStatus(user, STATUS_AVAILABLE);
    }

    return status;
  }

  @Override
  public String getUserFullName(String user)
  {
    String fullname = null;
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext())
    {
      Document doc = cursor.next();
      if (doc.get(FULLNAME)!=null)
        fullname = doc.get(FULLNAME).toString();
    }

    return fullname;
  }


  @Override
  public String getExternalValue(String user)
  {
    String isExternal = null;
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext())
    {
      Document doc = cursor.next();
      if (doc.get(IS_EXTERNAL) != null)
        isExternal = doc.get(IS_EXTERNAL).toString();
    }

    return isExternal;
  }

  @Override
  public UserBean getUser(String user)
  {
    return getUser(user, false);
  }

  @Override
  public UserBean getUser(String user, boolean withFavorites)
  {
    UserBean userBean = new UserBean();
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put(USER, user);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    if (cursor.hasNext())
    {
      Document doc = cursor.next();
      userBean.setName(user);
      if (doc.get(FULLNAME)!=null)
        userBean.setFullname( doc.get(FULLNAME).toString() );
      if (doc.get(EMAIL)!=null)
        userBean.setEmail(doc.get(EMAIL).toString());
      if (doc.get(STATUS)!=null)
        userBean.setStatus(doc.get(STATUS).toString());
      if (doc.get(IS_ENABLED) != null) {
        userBean.setEnabled(StringUtils.equals(doc.get(IS_ENABLED).toString(), TRUE));
      }
      if (doc.get(IS_DELETED) != null) {
        userBean.setDeleted(StringUtils.equals(doc.get(IS_DELETED).toString(), TRUE));
      }
      if (doc.get(IS_EXTERNAL) != null) {
        userBean.setExternal(doc.get(IS_EXTERNAL).toString());
      }
      if (withFavorites && (doc.containsKey(FAVORITES))) {
          userBean.setFavorites ((List<String>) doc.get(FAVORITES));
      }
    }

    return userBean;
  }

  @Override
  public List<String> getUsersFilterBy(String user, String room, String type)
  {
    ArrayList<String> users = new ArrayList<String>();
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    if (ChatService.TYPE_ROOM_SPACE.equals(type))
      query.put(SPACES, room);
    else
      query.put(TEAMS, room);
    MongoCursor<Document> cursor = coll.find(query).cursor();
    while (cursor.hasNext())
    {
      Document doc = cursor.next();
      String target = doc.get(USER).toString();
      if (user==null || !user.equals(target))
        users.add(target);
    }

    return users;
  }

  @Override
  public int getNumberOfUsers()
  {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    MongoCursor<Document> cursor = coll.find(query).cursor();
    return cursor.available();
  }
}
