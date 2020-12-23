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

  private DB db()
  {
    return ConnectionManager.getInstance().getDB();
  }

  @Override
  public void addFavorite(String user, String targetUser)
  {
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      List<String> favorites = new ArrayList<String>();
      if (doc.containsField("favorites")) {
        favorites = (List<String>)doc.get("favorites");
      }
      if (!favorites.contains(targetUser)) {
        favorites.add(targetUser);
        doc.put("favorites", favorites);
        coll.save(doc, WriteConcern.SAFE);
      }
    }
  }

  @Override
  public void removeFavorite(String user, String targetUser)
  {
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      List<String> favorites = new ArrayList<String>();
      if (doc.containsField("favorites")) {
        favorites = (List<String>)doc.get("favorites");
        if (favorites.contains(targetUser)) {
          favorites.remove(targetUser);
          doc.put("favorites", favorites);
          coll.save(doc, WriteConcern.SAFE);
        }
      }
    }
  }

  @Override
  public void setPreferredNotification(String user, String notifManner) throws Exception {
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext()) {
      DBObject doc = cursor.next();
      if(ChatService.BIP.equals(notifManner) || ChatService.DESKTOP_NOTIFICATION.equals(notifManner) || ChatService.ON_SITE.equals(notifManner)) {
        BasicDBObject settings = (BasicDBObject) doc.get(NOTIFICATIONS_SETTINGS);
        Object prefNotif = null;
        Object prefTriger = null;
        Object existingRoomNotif =null;
        if (settings != null) {
          prefNotif = settings.get(PREFERRED_NOTIFICATION);
          prefTriger = settings.get(PREFERRED_NOTIFICATION_TRIGGER);
          existingRoomNotif = settings.get(PREFERRED_ROOM_NOTIFICATION_TRIGGER);
        } else {
          settings = new BasicDBObject();
        }
        List<String> existingPrefNotif = null;
        if(prefNotif==null) {
          existingPrefNotif = new ArrayList<String>();
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

        if(existingPrefNotif!=null) {
          settings.put(PREFERRED_NOTIFICATION,existingPrefNotif);
        }
        if(prefTriger!=null) {
          settings.put(PREFERRED_NOTIFICATION_TRIGGER, prefTriger);
        }
        if(existingRoomNotif!=null){
          settings.put(PREFERRED_ROOM_NOTIFICATION_TRIGGER, existingRoomNotif);
        }

        doc.put(NOTIFICATIONS_SETTINGS, settings);

        coll.save(doc, WriteConcern.ACKNOWLEDGED);
      } else {
        throw new Exception("Wrong Params, operation not done");
      }
    } else {
      throw new Exception("Doc not found, operation not done");
    }
  }

  @Override
  public void setNotificationTrigger(String user, String notifCond) throws Exception {
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext()) {
      DBObject doc = cursor.next();
      if(ChatService.NOTIFY_ME_EVEN_NOT_DISTURB.equals(notifCond) || ChatService.NOTIFY_ME_WHEN_MENTION.equals(notifCond)) {
        BasicDBObject settings = (BasicDBObject) doc.get(NOTIFICATIONS_SETTINGS);
        Object prefNotif = null;
        Object prefTriger = null;
        Object existingRoomNotif =null;
        if (settings != null) {
          prefNotif = settings.get(PREFERRED_NOTIFICATION_TRIGGER);
          prefTriger = settings.get(PREFERRED_NOTIFICATION);
          existingRoomNotif = settings.get(PREFERRED_ROOM_NOTIFICATION_TRIGGER);
        } else {
          settings = new BasicDBObject();
        }
        List<String> existingPrefNotif = null;
        if(prefNotif==null) {
          existingPrefNotif = new ArrayList<String>();
        } else {
          existingPrefNotif = ((List<String>)prefNotif);
        }

        if(existingPrefNotif.contains(notifCond)) {
          existingPrefNotif.remove(notifCond);
        } else {
          existingPrefNotif.add(notifCond);
        }

        if (existingPrefNotif != null) {
          settings.put(PREFERRED_NOTIFICATION_TRIGGER, existingPrefNotif);
        }
        if (prefTriger != null) {
          settings.put(PREFERRED_NOTIFICATION, prefTriger);
        }
        if (existingRoomNotif != null) {
          settings.put(PREFERRED_ROOM_NOTIFICATION_TRIGGER, existingRoomNotif);
        }

        doc.put(NOTIFICATIONS_SETTINGS, settings);

        coll.save(doc, WriteConcern.ACKNOWLEDGED);
      } else {
        throw new Exception("Wrong Params, operation not done");
      }
    }  else {
      throw new Exception("Doc not found, operation not done");
    }
  }

  @Override
  public void setRoomNotificationTrigger(String user, String room, String notifCond, String notifConditionType, long time) throws Exception {
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext()) {
      DBObject doc = cursor.next();

      if(ChatService.NOTIFY_ME_ON_ROOM_NORMAL.equals(notifConditionType) || ChatService.DO_NOT_NOTIFY_ME_ON_ROOM.equals(notifConditionType) || notifConditionType.startsWith(ChatService.NOTIFY_ME_ON_ROOM_KEY_WORD)) {
        BasicDBObject settings = (BasicDBObject) doc.get(NOTIFICATIONS_SETTINGS);
        Object prefNotif = null;
        Object prefTriger = null;
        DBObject existingRoomNotif =null;
        if(settings!=null) {
          prefTriger =  settings.get(PREFERRED_NOTIFICATION_TRIGGER);
          prefNotif = settings.get(PREFERRED_NOTIFICATION);
          existingRoomNotif = (DBObject)settings.get(PREFERRED_ROOM_NOTIFICATION_TRIGGER);
        } else {
          settings = new BasicDBObject();
        }

        if(existingRoomNotif == null) {
          existingRoomNotif = new BasicDBObject();
        }

        DBObject notifData = (DBObject)existingRoomNotif.get(room);
        if (notifData == null) {
          notifData = new BasicDBObject();
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

        doc.put(NOTIFICATIONS_SETTINGS, settings);
        coll.save(doc, WriteConcern.ACKNOWLEDGED);
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
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext()) {
      DBObject doc = cursor.next();
      BasicDBObject wrapperDoc = ((BasicDBObject) doc.get(NOTIFICATIONS_SETTINGS));
      if(wrapperDoc==null){//when there is no settings - first start of the server
        wrapperDoc = new BasicDBObject();
      }

      if(wrapperDoc.get(UserDataStorage.PREFERRED_NOTIFICATION)!=null){
        settings.setEnabledChannels(wrapperDoc.get(UserDataStorage.PREFERRED_NOTIFICATION).toString());
      } else {
        //default values to the untouched settings
        settings.setEnabledChannels(DEFAULT_ENABLED_CHANNELS);
      }
      if(wrapperDoc.get(UserDataStorage.PREFERRED_NOTIFICATION_TRIGGER)!=null){
        settings.setEnabledTriggers(wrapperDoc.get(UserDataStorage.PREFERRED_NOTIFICATION_TRIGGER).toString());
      }
      if(wrapperDoc.get(PREFERRED_ROOM_NOTIFICATION_TRIGGER) != null) {
        settings.setEnabledRoomTriggers(wrapperDoc.get(PREFERRED_ROOM_NOTIFICATION_TRIGGER).toString());
      }
    }
    return settings;
  }

  @Override
  public boolean isFavorite(String user, String targetUser)
  {
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      if (doc.containsField("favorites")) {
        List<String> favorites = (List<String>)doc.get("favorites");
        if (favorites.contains(targetUser))
          return true;
      }
    }
    return false;
  }

  @Override
  public void addUserFullName(String user, String fullname)
  {
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (!cursor.hasNext())
    {
      BasicDBObject doc = new BasicDBObject();
      doc.put("_id", user);
      doc.put("user", user);
      doc.put("fullname", fullname);
      doc.put("isEnabled", Boolean.TRUE.toString());
      doc.put("isDeleted", Boolean.FALSE.toString());
      coll.insert(doc);
    }
    else
    {
      DBObject doc = cursor.next();
      doc.put("fullname", fullname);
      coll.save(doc, WriteConcern.ACKNOWLEDGED);

    }
  }

  @Override
  public void addUserEmail(String user, String email)
  {
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (!cursor.hasNext())
    {
      BasicDBObject doc = new BasicDBObject();
      doc.put("_id", user);
      doc.put("user", user);
      doc.put("email", email);
      doc.put("isEnabled", Boolean.TRUE.toString());
      doc.put("isDeleted", Boolean.FALSE.toString());
      coll.insert(doc);
    }
    else
    {
      DBObject doc = cursor.next();
      doc.put("email", email);
      coll.save(doc);

    }
  }

  @Override
  public void deleteUser(String user)
  {
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext()) {
      DBObject doc = cursor.next();
      doc.put("isDeleted", Boolean.TRUE.toString());
      doc.put("isEnabled", Boolean.FALSE.toString());
      coll.save(doc);
    }
  }

  @Override
  public void setEnabledUser(String user, Boolean isEnabled)
  {
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext()) {
      DBObject doc = cursor.next();
      doc.put("isDeleted", Boolean.FALSE.toString());
      doc.put("isEnabled", isEnabled.toString());
      coll.save(doc);
    }
  }

  @Override
  public void setSpaces(String user, List<SpaceBean> spaces)
  {
    List<String> spaceIds = new ArrayList<String>();
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);
    for (SpaceBean bean:spaces)
    {
      String room = ChatUtils.getRoomId(bean.getId());
      spaceIds.add(room);


      BasicDBObject query = new BasicDBObject();
      query.put("_id", room);
      DBCursor cursor = coll.find(query);
      if (!cursor.hasNext())
      {
        BasicDBObject doc = new BasicDBObject();
        doc.put("_id", room);
        doc.put("space_id", bean.getId());
        doc.put("displayName", bean.getDisplayName());
        doc.put("groupId", bean.getGroupId());
        doc.put("shortName", bean.getShortName());
        doc.put("prettyName", bean.getPrettyName());
        doc.put("type", ChatService.TYPE_ROOM_SPACE);
        coll.insert(doc);
      }
      else
      {
        DBObject doc = cursor.next();
        String displayName = doc.get("displayName").toString();
        Object prettyName = doc.get("prettyName");
        if (!bean.getDisplayName().equals(displayName) || prettyName == null || !bean.getPrettyName().equals(prettyName.toString()))
        {
          doc.put("_id", room);
          doc.put("displayName", bean.getDisplayName());
          doc.put("groupId", bean.getGroupId());
          doc.put("shortName", bean.getShortName());
          doc.put("prettyName", bean.getPrettyName());
          coll.save(doc);
        }
      }


    }
    coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      doc.put("spaces", spaceIds);
      coll.save(doc, WriteConcern.SAFE);
    }
    else
    {
      BasicDBObject doc = new BasicDBObject();
      doc.put("_id", user);
      doc.put("user", user);
      doc.put("spaces", spaceIds);
      doc.put("isEnabled", Boolean.toString(true));
      doc.put("isDeleted", Boolean.toString(false));
      coll.insert(doc);
    }
  }

  @Override
  public void addTeamRoom(String user, String teamRoomId) {
    List<String> teamIds = new ArrayList<String>();
    teamIds.add(teamRoomId);
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      if (doc.containsField("teams"))
      {
        List<String> existingTeams = ((List<String>)doc.get("teams"));
        if (!existingTeams.contains(teamRoomId))
          existingTeams.add(teamRoomId);
        doc.put("teams", existingTeams);
      }
      else
      {
        doc.put("teams", teamIds);
      }
      coll.save(doc, WriteConcern.SAFE);
    }
    else
    {
      BasicDBObject doc = new BasicDBObject();
      doc.put("_id", user);
      doc.put("user", user);
      doc.put("teams", teamIds);
      coll.insert(doc);
    }
  }

  @Override
  public void removeTeamUsers(String teamRoomId, List<String> users) {
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    for (String user:users)
    {
      BasicDBObject query = new BasicDBObject();
      query.put("user", user);
      DBCursor cursor = coll.find(query);
      if (cursor.hasNext())
      {
        DBObject doc = cursor.next();
        if (doc.containsField("teams"))
        {
          List<String> teams = (List<String>)doc.get("teams");
          if (teams.contains(teamRoomId))
          {
            teams.remove(teamRoomId);
            doc.put("teams", teams);
            coll.save(doc, WriteConcern.SAFE);
          }
        }
      }

    }
  }

  private RoomBean getTeam(String teamId)
  {
    RoomBean roomBean = null;
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("_id", teamId);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      roomBean = new RoomBean();
      roomBean.setRoom(teamId);
      roomBean.setUser(doc.get("user").toString());
      roomBean.setFullName(doc.get("team").toString());
      roomBean.setType(doc.get("type").toString());
      if (doc.containsField("meetingStarted")) {
        roomBean.setMeetingStarted((Boolean) doc.get("meetingStarted"));
      }
      if (doc.containsField("startTime")) {
        roomBean.setStartTime((String) doc.get("startTime"));
      }
      if (doc.containsField("timestamp"))
      {
        roomBean.setTimestamp(((Long) doc.get("timestamp")).longValue());
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
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();

      List<String> listrooms = ((List<String>)doc.get("teams"));
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
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("_id", roomId);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      roomBean = new RoomBean();
      roomBean.setRoom(roomId);

      DBObject doc = cursor.next();
      if (doc.containsField("timestamp"))
      {
        roomBean.setTimestamp(((Long) doc.get("timestamp")).longValue());
      }
      String type = doc.get("type").toString();
      roomBean.setType(type);
      if (ChatService.TYPE_ROOM_SPACE.equals(type))
      {
        roomBean.setUser(ChatService.SPACE_PREFIX+roomId);
        roomBean.setFullName(doc.get("displayName").toString());
        if (doc.containsField("meetingStarted")) {
          roomBean.setMeetingStarted((Boolean) doc.get("meetingStarted"));
        }
        if (doc.containsField("startTime")) {
          roomBean.setStartTime((String) doc.get("startTime"));
        }
      }
      else if (ChatService.TYPE_ROOM_TEAM.equals(type))
      {
        roomBean.setUser(ChatService.TEAM_PREFIX+roomId);
        roomBean.setFullName(doc.get("team").toString());
        String creator = (String) doc.get("user");
        roomBean.setAdmins(new String[]{creator});
        if (doc.containsField("meetingStarted")) {
          roomBean.setMeetingStarted((Boolean) doc.get("meetingStarted"));
        }
        if (doc.containsField("startTime")) {
          roomBean.setStartTime((String) doc.get("startTime"));
        }
      }
      else if (ChatService.TYPE_ROOM_USER.equals(type))
      {
        List<String> users = ((List<String>)doc.get("users"));
        users.remove(user);
        String targetUser = users.get(0);
        roomBean.setUser(targetUser);
        roomBean.setFullName(this.getUserFullName(targetUser));
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
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("_id", roomId);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      spaceBean = new SpaceBean();
      spaceBean.setRoom(roomId);
      spaceBean.setId(doc.get("space_id").toString());
      spaceBean.setDisplayName(doc.get("displayName").toString());
      spaceBean.setGroupId(doc.get("groupId").toString());
      spaceBean.setShortName(doc.get("shortName").toString());
      if (doc.containsField("prettyName")) {
        spaceBean.setPrettyName(doc.get("prettyName").toString());
      }
      if (doc.containsField("meetingStarted")) {
        spaceBean.setMeetingStarted((Boolean) doc.get("meetingStarted"));
      }
      if (doc.containsField("startTime")) {
        spaceBean.setStartTime((String) doc.get("startTime"));
      }
      if (doc.containsField("timestamp"))
      {
        spaceBean.setTimestamp(((Long)doc.get("timestamp")).longValue());
      }
    }

    return spaceBean;
  }

  @Override
  public List<SpaceBean> getSpaces(String user)
  {
    List<SpaceBean> spaces = new ArrayList<SpaceBean>();
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();

      List<String> listspaces = ((List<String>)doc.get("spaces"));
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
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("_id", roomId);
    DBCursor cursor = coll.find(query);
    while (cursor.hasNext()) {
      DBObject doc = cursor.next();
      Object objectUsers = doc.get("users");
      ArrayList myArrayList = (ArrayList) objectUsers;
      for (int i = 0; i < myArrayList.size(); i++) {
        users.add(getUser(myArrayList.get(i).toString()));
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

    List<BasicDBObject> andList = new ArrayList<>();
    if (StringUtils.isNotBlank(roomId)) {
      // removing "space-" and "team-" prefix
      if (roomId.indexOf(ChatService.SPACE_PREFIX) == 0) {
        roomId = roomId.substring(ChatService.SPACE_PREFIX.length());
      } else if (roomId.indexOf(ChatService.TEAM_PREFIX) == 0) {
        roomId = roomId.substring(ChatService.TEAM_PREFIX.length());
      }

      ArrayList<BasicDBObject> orList = new ArrayList<>();
      orList.add(new BasicDBObject("spaces", roomId));
      orList.add(new BasicDBObject("teams", roomId));

      andList.add(new BasicDBObject("$or", orList));
    }

    if (filter != null) {
      filter = filter.replaceAll(" ", ".*");
      Pattern regex = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);
      ArrayList<BasicDBObject> orList = new ArrayList<>();
      orList.add(new BasicDBObject("user", regex));
      orList.add(new BasicDBObject("fullname", regex));

      andList.add(new BasicDBObject("$or", orList));
    }

    List<UserBean> users = null;

    // Load online users
    if(onlineUsers != null && !onlineUsers.isEmpty()) {
      BasicDBObject fetchOnlineUsers = new BasicDBObject("user", new BasicDBObject("$in", onlineUsers));
      List<BasicDBObject> clonedAndList = new ArrayList<>(andList);
      clonedAndList.add(fetchOnlineUsers);
      DBObject query = new BasicDBObject("$and", clonedAndList);
      DBCollection coll = db().getCollection(M_USERS_COLLECTION);
      DBCursor cursor = coll.find(query).limit(limit);
      users = new ArrayList<>();
      while (cursor.hasNext()) {
        DBObject doc = cursor.next();
        UserBean userBean = new UserBean();
        userBean.setName(doc.get("user").toString());
        Object prop = doc.get("fullname");
        userBean.setFullname((prop != null) ? prop.toString() : "");
        prop = doc.get("email");
        userBean.setEmail((prop != null) ? prop.toString() : "");
        prop = doc.get("status");
        userBean.setStatus((prop != null) ? prop.toString() : "");
        if (doc.get("isEnabled") != null) {
          userBean.setEnabled(StringUtils.equals(doc.get("isEnabled").toString(), "true"));
        }
        if (doc.get("isDeleted") != null) {
          userBean.setDeleted(StringUtils.equals(doc.get("isDeleted").toString(), "true"));
        }
        users.add(userBean);
      }
    }
    
    users = users == null ? new ArrayList<>() : users;

    if(!onlyOnlineUsers) {
      int usersLeft = limit > 0 ? limit - users.size() : -1;

      if (usersLeft > 0 || (limit >= 0 && users.isEmpty())) {
        usersLeft = usersLeft > 0 ? usersLeft : limit;
        BasicDBObject escapeOnlineUsers;
        if (onlineUsers != null && !onlineUsers.isEmpty()) {
          escapeOnlineUsers = new BasicDBObject("user", new BasicDBObject("$nin", onlineUsers));
          andList.add(escapeOnlineUsers);
        }
        DBObject query = new BasicDBObject("$and", andList);
        DBCollection coll = db().getCollection(M_USERS_COLLECTION);
        DBCursor cursor = coll.find(query).limit(usersLeft);
        while (cursor.hasNext()) {
          DBObject doc = cursor.next();
          UserBean userBean = new UserBean();
          userBean.setName(doc.get("user").toString());
          Object prop = doc.get("fullname");
          userBean.setFullname((prop != null) ? prop.toString() : "");
          prop = doc.get("email");
          userBean.setEmail((prop != null) ? prop.toString() : "");
          prop = doc.get("status");
          userBean.setStatus((prop != null) ? prop.toString() : "");
          if (doc.get("isEnabled") != null) {
            userBean.setEnabled(StringUtils.equals(doc.get("isEnabled").toString(), "true"));
          }
          if (doc.get("isDeleted") != null) {
            userBean.setDeleted(StringUtils.equals(doc.get("isDeleted").toString(), "true"));
          }
          users.add(userBean);
        }
      }
    }

    users = users.stream().filter(UserBean::isEnabledUser).collect(Collectors.toList());
    return users;
  }

  public Long getUsersCount(String roomId, String filter) {
    if (roomId == null && filter == null) {
      throw new IllegalArgumentException();
    }

    List<BasicDBObject> andList = new ArrayList<>();
    if (StringUtils.isNotBlank(roomId)) {
      // removing "space-" and "team-" prefix
      if (roomId.indexOf(ChatService.SPACE_PREFIX) == 0) {
        roomId = roomId.substring(ChatService.SPACE_PREFIX.length());
      } else if (roomId.indexOf(ChatService.TEAM_PREFIX) == 0) {
        roomId = roomId.substring(ChatService.TEAM_PREFIX.length());
      }

      ArrayList<BasicDBObject> orList = new ArrayList<>();
      orList.add(new BasicDBObject("spaces", roomId));
      orList.add(new BasicDBObject("teams", roomId));

      andList.add(new BasicDBObject("$or", orList));
    }

    if (filter != null) {
      filter = filter.replaceAll(" ", ".*");
      Pattern regex = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);
      ArrayList<BasicDBObject> orList = new ArrayList<>();
      orList.add(new BasicDBObject("user", regex));
      orList.add(new BasicDBObject("fullname", regex));

      andList.add(new BasicDBObject("$or", orList));
    }
    andList.add(new BasicDBObject("isDeleted", "false"));
    List<BasicDBObject> orList = new ArrayList<>();
    orList.add(new BasicDBObject("isEnabled", "true"));
    //some entries were added with boolean values
    orList.add(new BasicDBObject("isEnabled", true));
    andList.add(new BasicDBObject("$or", orList));

    DBObject query = new BasicDBObject("$and", andList);
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    return (long) coll.find(query).count();
  }

  @Override
  public String setStatus(String user, String status)
  {
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      doc.put("status", status);
      coll.save(doc, WriteConcern.SAFE);
    }
    else
    {
      BasicDBObject doc = new BasicDBObject();
      doc.put("_id", user);
      doc.put("user", user);
      doc.put("status", status);
      coll.insert(doc);
    }
    return status;
  }

  @Override
  public void setAsAdmin(String user, boolean isAdmin)
  {
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      doc.put("isSupportAdmin", isAdmin);
      coll.save(doc, WriteConcern.SAFE);
    }
    else
    {
      BasicDBObject doc = new BasicDBObject();
      doc.put("_id", user);
      doc.put("user", user);
      doc.put("isSupportAdmin", isAdmin);
      coll.insert(doc);
    }
  }

  @Override
  public boolean isAdmin(String user)
  {
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      Object isAdmin = doc.get("isSupportAdmin");
      return (isAdmin!=null && "true".equals(isAdmin.toString()));
    }
    return false;
  }

  @Override
  public String getStatus(String user)
  {
    String status = STATUS_NONE;
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      if (doc.containsField("status"))
        status = doc.get("status").toString();
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
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      if (doc.get("fullname")!=null)
        fullname = doc.get("fullname").toString();
    }

    return fullname;
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
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      userBean.setName(user);
      if (doc.get("fullname")!=null)
        userBean.setFullname( doc.get("fullname").toString() );
      if (doc.get("email")!=null)
        userBean.setEmail(doc.get("email").toString());
      if (doc.get("status")!=null)
        userBean.setStatus(doc.get("status").toString());
      if (doc.get("isEnabled") != null) {
        userBean.setEnabled(StringUtils.equals(doc.get("isEnabled").toString(), "true"));
      }
      if (doc.get("isDeleted") != null) {
        userBean.setDeleted(StringUtils.equals(doc.get("isDeleted").toString(), "true"));
      }
      if (withFavorites)
      {
        if (doc.containsField("favorites")) {
          userBean.setFavorites ((List<String>) doc.get("favorites"));
        }
      }
    }

    return userBean;
  }

  @Override
  public List<String> getUsersFilterBy(String user, String room, String type)
  {
    ArrayList<String> users = new ArrayList<String>();
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    if (ChatService.TYPE_ROOM_SPACE.equals(type))
      query.put("spaces", room);
    else
      query.put("teams", room);
    DBCursor cursor = coll.find(query);
    while (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      String target = doc.get("user").toString();
      if (user==null || !user.equals(target))
        users.add(target);
    }

    return users;
  }

  @Override
  public int getNumberOfUsers()
  {
    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    DBCursor cursor = coll.find(query);
    return cursor.count();
  }
}
