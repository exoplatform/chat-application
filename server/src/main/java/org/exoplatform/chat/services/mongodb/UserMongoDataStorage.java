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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@Named("userStorage")
@ApplicationScoped
public class UserMongoDataStorage implements UserDataStorage {

  private static final Logger LOG = Logger.getLogger(UserMongoDataStorage.class.getName());

  public static final String M_USERS_COLLECTION = "users";
  public static final String M_ROOMS_COLLECTION = "rooms";

  private DB db(String dbName)
  {
    if (StringUtils.isEmpty(dbName)) {
      return ConnectionManager.getInstance().getDB();
    } else {
      return ConnectionManager.getInstance().getDB(dbName);
    }
  }

  @Override
  public void addFavorite(String user, String targetUser, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
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
  public void removeFavorite(String user, String targetUser, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
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
  public void setPreferredNotification(String user, String notifManner, String dbName) throws Exception {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext()) {
      DBObject doc = cursor.next();
      if(ChatService.BIP.equals(notifManner) || ChatService.DESKTOP_NOTIFICATION.equals(notifManner) || ChatService.ON_SITE.equals(notifManner)) {
        BasicDBObject settings = (BasicDBObject) doc.get("notificationsSettings");
        Object prefNotif = null;
        Object prefTriger = null;
        Object existingRoomNotif =null;
        if(settings!=null) {
          prefNotif =  settings.get("preferredNotification");
          prefTriger = settings.get("preferredNotificationTrigger");
          existingRoomNotif = settings.get(PREFERRED_ROOM_NOTIFICATION_TRIGGER);
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
        Map childs = new HashMap();

        if(existingPrefNotif!=null) {
          childs.put("preferredNotification",existingPrefNotif);
        }
        if(prefTriger!=null) {
          childs.put("preferredNotificationTrigger", prefTriger);
        }
        if(existingRoomNotif!=null){
          childs.put(PREFERRED_ROOM_NOTIFICATION_TRIGGER, existingRoomNotif);
        }

        doc.put("notificationsSettings",childs);

        coll.save(doc, WriteConcern.SAFE);
      } else {
        throw new Exception("Wrong Params, operation not done");
      }
    } else {
      throw new Exception("Doc not found, operation not done");
    }
  }

  @Override
  public void setNotificationTrigger(String user, String notifCond, String dbName) throws Exception {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext()) {
      DBObject doc = cursor.next();
      if(ChatService.NOTIFY_ME_EVEN_NOT_DISTRUB.equals(notifCond) || ChatService.NOTIFY_ME_WHEN_MENTION.equals(notifCond)) {
        BasicDBObject settings = (BasicDBObject) doc.get("notificationsSettings");
        Object prefNotif = null;
        Object prefTriger = null;
        Object existingRoomNotif =null;
        if(settings!=null) {
          prefNotif =  settings.get("preferredNotificationTrigger");
          prefTriger = settings.get("preferredNotification");
          existingRoomNotif = settings.get(PREFERRED_ROOM_NOTIFICATION_TRIGGER);
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

        Map childs = new HashMap();
        if(existingPrefNotif!=null) {
          childs.put("preferredNotificationTrigger", existingPrefNotif);
        }
        if(prefTriger!=null) {
          childs.put("preferredNotification",prefTriger);
        }
        if(existingRoomNotif!=null){
          childs.put(PREFERRED_ROOM_NOTIFICATION_TRIGGER, existingRoomNotif);
        }

        doc.put("notificationsSettings",childs);

        coll.save(doc, WriteConcern.SAFE);
      } else {
        throw new Exception("Wrong Params, operation not done");
      }
    }  else {
      throw new Exception("Doc not found, operation not done");
    }
  }

  @Override
  public void setRoomNotificationTrigger(String user, String room, String notifCond, String notifConditionType, String dbName, long time) throws Exception {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext()) {
      DBObject doc = cursor.next();

      if(ChatService.NOTIFY_ME_ON_ROOM_NORMAL.equals(notifConditionType) || ChatService.DO_NOT_NOTIFY_ME_ON_ROOM.equals(notifConditionType) || notifConditionType.startsWith(ChatService.NOTIFY_ME_ON_ROOM_KEY_WORD)) {
        BasicDBObject settings = (BasicDBObject) doc.get("notificationsSettings");
        Object prefNotif = null;
        Object prefTriger = null;
        DBObject existingRoomNotif =null;
        if(settings!=null) {
          prefTriger =  settings.get("preferredNotificationTrigger");
          prefNotif = settings.get("preferredNotification");
          existingRoomNotif = (DBObject)settings.get(PREFERRED_ROOM_NOTIFICATION_TRIGGER);
        }

        if(existingRoomNotif == null) {
          existingRoomNotif = new BasicDBObject();
        }

        DBObject notifData = (DBObject)existingRoomNotif.get(room);
        if (notifData == null) {
          notifData = new BasicDBObject();
        }
        
        if (notifData.get("time") == null || (long)notifData.get("time") < time) {
          notifData.put("notifCond", notifConditionType);
          notifData.put("time", time);
          if(UserDataStorage.ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD.equals(notifConditionType)){
            notifData.put(UserDataStorage.ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD, notifCond);
          }
        }

        Map childs = new HashMap();
        if(prefTriger!=null) {
          childs.put("preferredNotificationTrigger", prefTriger);
        }

        if(prefNotif!=null) {
          childs.put("preferredNotification",prefNotif);
        }

        if(existingRoomNotif!=null){
          existingRoomNotif.put(room, notifData);
          childs.put(PREFERRED_ROOM_NOTIFICATION_TRIGGER, existingRoomNotif);
        }

        doc.put("notificationsSettings",childs);
        coll.save(doc, WriteConcern.SAFE);
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
  public NotificationSettingsBean getUserDesktopNotificationSettings(String user, String dbName) throws JSONException {
    NotificationSettingsBean settings = new NotificationSettingsBean();
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext()) {
      DBObject doc = cursor.next();
      BasicDBObject wrapperDoc = ((BasicDBObject) doc.get("notificationsSettings"));
      if(wrapperDoc==null){//when there is no settings - first start of the server
        wrapperDoc = new BasicDBObject();
      }

      if(wrapperDoc.get(UserDataStorage.PREFERRED_NOTIFICATION)!=null){
        settings.setEnabledChannels(wrapperDoc.get(UserDataStorage.PREFERRED_NOTIFICATION).toString());
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
  public boolean isFavorite(String user, String targetUser, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
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
  public void addUserFullName(String user, String fullname, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (!cursor.hasNext())
    {
      BasicDBObject doc = new BasicDBObject();
      doc.put("_id", user);
      doc.put("user", user);
      doc.put("fullname", fullname);
      coll.insert(doc);
    }
    else
    {
      DBObject doc = cursor.next();
      doc.put("fullname", fullname);
      coll.save(doc);

    }
  }

  @Override
  public void addUserEmail(String user, String email, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (!cursor.hasNext())
    {
      BasicDBObject doc = new BasicDBObject();
      doc.put("_id", user);
      doc.put("user", user);
      doc.put("email", email);
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
  public void setSpaces(String user, List<SpaceBean> spaces, String dbName)
  {
    List<String> spaceIds = new ArrayList<String>();
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);
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
        doc.put("type", ChatService.TYPE_ROOM_SPACE);
        coll.insert(doc);
      }
      else
      {
        DBObject doc = cursor.next();
        String displayName = doc.get("displayName").toString();
        if (!bean.getDisplayName().equals(displayName))
        {
          doc.put("_id", room);
          doc.put("displayName", bean.getDisplayName());
          doc.put("groupId", bean.getGroupId());
          doc.put("shortName", bean.getShortName());
          coll.save(doc);
        }
      }


    }
    coll = db(dbName).getCollection(M_USERS_COLLECTION);
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
      coll.insert(doc);
    }
  }

  @Override
  public void addTeamRoom(String user, String teamRoomId, String dbName) {
    List<String> teamIds = new ArrayList<String>();
    teamIds.add(teamRoomId);
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
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
  public void removeTeamUsers(String teamRoomId, List<String> users, String dbName) {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
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

  private RoomBean getTeam(String teamId, String dbName)
  {
    RoomBean roomBean = null;
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("_id", teamId);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      roomBean = new RoomBean();
      roomBean.setRoom(teamId);
      roomBean.setUser(doc.get("user").toString());
      roomBean.setFullname(doc.get("team").toString());
      roomBean.setType(doc.get("type").toString());
      if (doc.containsField("timestamp"))
      {
        roomBean.setTimestamp(((Long) doc.get("timestamp")).longValue());
      }
    }

    return roomBean;
  }

  @Override
  public List<RoomBean> getTeams(String user, String dbName) {
    List<RoomBean> rooms = new ArrayList<RoomBean>();
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
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
          rooms.add(getTeam(room, dbName));
        }
      }

    }
    return rooms;
  }

  @Override
  public RoomBean getRoom(String user, String roomId, String dbName) {
    RoomBean roomBean = new RoomBean();
    roomBean.setRoom(roomId);
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("_id", roomId);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
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
        roomBean.setFullname(doc.get("displayName").toString());
      }
      else if (ChatService.TYPE_ROOM_TEAM.equals(type))
      {
        roomBean.setUser(ChatService.TEAM_PREFIX+roomId);
        roomBean.setFullname(doc.get("team").toString());
      }
      else if (ChatService.TYPE_ROOM_USER.equals(type))
      {
        List<String> users = ((List<String>)doc.get("users"));
        users.remove(user);
        String targetUser = users.get(0);
        roomBean.setUser(targetUser);
        roomBean.setFullname(this.getUserFullName(targetUser, dbName));
      }
      else if (ChatService.TYPE_ROOM_EXTERNAL.equals(type))
      {
        roomBean.setUser(ChatService.EXTERNAL_PREFIX+roomId);
        roomBean.setFullname(doc.get("identifier").toString());
      }
    }

    return roomBean;
  }

  private SpaceBean getSpace(String roomId, String dbName)
  {
    SpaceBean spaceBean = null;
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);
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
      if (doc.containsField("timestamp"))
      {
        spaceBean.setTimestamp(((Long)doc.get("timestamp")).longValue());
      }
    }

    return spaceBean;
  }

  @Override
  public List<SpaceBean> getSpaces(String user, String dbName)
  {
    List<SpaceBean> spaces = new ArrayList<SpaceBean>();
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
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
          spaces.add(getSpace(space, dbName));
        }
      }

    }
    return spaces;
  }

  @Override
  public List<UserBean> getUsers(String roomId, String dbName)
  {
    //removing "space-" prefix
    if (roomId.indexOf(ChatService.SPACE_PREFIX)==0)
    {
      roomId = roomId.substring(ChatService.SPACE_PREFIX.length());
    }
    //removing "team-" prefix
    if (roomId.indexOf(ChatService.TEAM_PREFIX)==0)
    {
      roomId = roomId.substring(ChatService.TEAM_PREFIX.length());
    }
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);

    BasicDBObject spaces = new BasicDBObject("spaces", roomId);
    BasicDBObject teams = new BasicDBObject("teams", roomId);
    ArrayList<BasicDBObject> orList = new ArrayList<BasicDBObject>();
    orList.add(spaces);
    orList.add(teams);
    BasicDBObject query = new BasicDBObject("$or", orList);

    DBCursor cursor = coll.find(query);
    List<UserBean> users = new ArrayList<UserBean>();
    while (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      UserBean userBean = new UserBean();
      userBean.setName(doc.get("user").toString());
      Object prop = doc.get("fullname");
      userBean.setFullname((prop!=null)?prop.toString():"");
      prop = doc.get("email");
      userBean.setEmail((prop!=null)?prop.toString():"");
      prop = doc.get("status");
      userBean.setStatus((prop!=null)?prop.toString():"");
      users.add(userBean);
    }
    return users;
  }
  
  @Override
  public List<UserBean> getUsersInRoomChatOneToOne(String roomId, String dbName) {
    List<UserBean> users = new ArrayList<UserBean>();
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("_id", roomId);
    DBCursor cursor = coll.find(query);
    while (cursor.hasNext()) {
      DBObject doc = cursor.next();
      Object objectUsers = doc.get("users");
      ArrayList myArrayList = (ArrayList) objectUsers;
      for (int i = 0; i < myArrayList.size(); i++) {
        users.add(getUser(myArrayList.get(i).toString(), dbName));
      }
    }
    return users;
  }
  
  @Override
  public List<UserBean> getUsers(String filter, boolean fullBean, String dbName) {
    filter = filter.replaceAll(" ", ".*");
    List<UserBean> users = new ArrayList<UserBean>();
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    Pattern regex = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);

    BasicDBObject un = new BasicDBObject("user", regex);
    BasicDBObject fn = new BasicDBObject("fullname", regex);
    ArrayList<BasicDBObject> orList = new ArrayList<BasicDBObject>();
    orList.add(un);
    orList.add(fn);
    BasicDBObject query = new BasicDBObject("$or", orList);

    DBCursor cursor = coll.find(query);
    while (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      UserBean userBean = new UserBean();
      userBean.setName(doc.get("user").toString());
      Object prop = doc.get("fullname");
      userBean.setFullname((prop!=null)?prop.toString():"");
      prop = doc.get("email");
      userBean.setEmail((prop!=null)?prop.toString():"");
      prop = doc.get("status");
      userBean.setStatus((prop!=null)?prop.toString():"");
      users.add(userBean);
    }
    return users;
  }

  @Override
  public String setStatus(String user, String status, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
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
  public void setAsAdmin(String user, boolean isAdmin, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
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
  public boolean isAdmin(String user, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
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
  public String getStatus(String user, String dbName)
  {
    String status = STATUS_NONE;
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      if (doc.containsField("status"))
        status = doc.get("status").toString();
      else
        status = setStatus(user, STATUS_AVAILABLE, dbName);
    }
    else
    {
      status = setStatus(user, STATUS_AVAILABLE, dbName);
    }

    return status;
  }

  @Override
  public String getUserFullName(String user, String dbName)
  {
    String fullname = null;
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
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
  public UserBean getUser(String user, String dbName)
  {
    return getUser(user, false, dbName);
  }

  @Override
  public UserBean getUser(String user, boolean withFavorites, String dbName)
  {
    UserBean userBean = new UserBean();
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
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
  public List<String> getUsersFilterBy(String user, String room, String type, String dbName)
  {
    ArrayList<String> users = new ArrayList<String>();
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
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
  public int getNumberOfUsers(String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    DBCursor cursor = coll.find(query);
    return cursor.count();
  }
}
