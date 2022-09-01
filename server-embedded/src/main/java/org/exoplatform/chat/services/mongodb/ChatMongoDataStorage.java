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
import org.bson.types.ObjectId;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.model.*;
import org.exoplatform.chat.services.*;
import org.exoplatform.chat.utils.ChatUtils;
import org.exoplatform.chat.utils.PropertyManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.exoplatform.chat.services.ChatService.*;
import static org.exoplatform.chat.services.UserDataStorage.STATUS_OFFLINE;
import static org.exoplatform.chat.services.mongodb.UserMongoDataStorage.M_USERS_COLLECTION;

@Named("chatStorage")
@ApplicationScoped
@Singleton
public class ChatMongoDataStorage implements ChatDataStorage {

  private static final Logger LOG = Logger.getLogger("ChatMongoDataStorage");

  public static final String M_ROOM_PREFIX = "messages_room_";
  public static final String M_ROOMS_COLLECTION = "rooms";

  private int readTotalJson, readTotalTxt;

  @Inject
  private UserDataStorage userDataStorage;

  private SimpleDateFormat formatterDate = new SimpleDateFormat("dd/MM/yyyy hh:mm aaa");

  public ChatMongoDataStorage() {
    readTotalJson = Integer.parseInt(PropertyManager.getProperty(PropertyManager.PROPERTY_READ_TOTAL_JSON));
    readTotalTxt = Integer.parseInt(PropertyManager.getProperty(PropertyManager.PROPERTY_READ_TOTAL_TXT));
  }

  private DB db() {
    return ConnectionManager.getInstance().getDB();
  }

  public void write(String message, String user, String room, String isSystem) {
    write(message, user, room, isSystem, null);
  }

  public void write(String message, String user, String room, String isSystem, String options) {
    save(message, user, room, isSystem, options);
  }

  public String save(String message, String user, String room, String isSystem, String options) {
    String roomType = getTypeRoomChat(room);
    DBCollection coll = db().getCollection(M_ROOM_PREFIX + roomType);

    message = StringUtils.chomp(message);
    message = message.replaceAll("&", "&#38");
    message = message.replaceAll("<", "&lt;");
    message = message.replaceAll(">", "&gt;");
    message = message.replaceAll("\"", "&quot;");
    message = message.replaceAll("\n", "<br/>");
    message = message.replaceAll("\\\\", "&#92");
    message = message.replaceAll("\t", "  ");

    BasicDBObject doc = new BasicDBObject();
    doc.put("user", user);
    doc.put("message", message);
    doc.put("timestamp", System.currentTimeMillis());
    doc.put("isSystem", isSystem);
    doc.put("roomId", room);
    if (options != null) {
      options = options.replaceAll("<", "&lt;");
      options = options.replaceAll(">", "&gt;");
      doc.put("options", options);
    }
    coll.insert(doc);

    this.updateRoomTimestamp(room);

    return doc.get("_id").toString();
  }

  public void delete(String room, String user, String messageId) {
    String roomType = getTypeRoomChat(room);
    DBCollection coll = db().getCollection(M_ROOM_PREFIX + roomType);
    BasicDBObject query = new BasicDBObject();
    query.put("_id", new ObjectId(messageId));
    query.put("user", user);
    query.put("roomId", room);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext()) {
      DBObject dbo = cursor.next();
      dbo.put("message", TYPE_DELETED);
      dbo.put("type", TYPE_DELETED);
      dbo.put("lastUpdatedTimestamp", System.currentTimeMillis());
      coll.save(dbo, WriteConcern.UNACKNOWLEDGED);
    }
  }

  @Override
  public List<RoomBean> getTeamRoomByName(String teamName) {
    if (StringUtils.isBlank(teamName))
      return null;
    DBCollection cRooms = db().getCollection(M_ROOMS_COLLECTION);
    BasicDBObject qRoom = new BasicDBObject();
    qRoom.put("team", teamName);
    qRoom.put("type", TYPE_ROOM_TEAM);
    List<RoomBean> roomBeans = new ArrayList<>();
    DBCursor roomsCursor = cRooms.find(qRoom);
    while (roomsCursor.hasNext()) {
      DBObject dbRoom = roomsCursor.next();
      RoomBean room = new RoomBean();
      room.setRoom((String) dbRoom.get("_id"));
      room.setFullName((String) dbRoom.get("team"));
      room.setUser((String) dbRoom.get("user"));
      room.setType((String) dbRoom.get("type"));
      if (dbRoom.containsField("meetingStarted")) {
        room.setMeetingStarted((Boolean) dbRoom.get("meetingStarted"));
      }
      if (dbRoom.containsField("startTime")) {
        room.setStartTime((String) dbRoom.get("startTime"));
      }
      if (StringUtils.isNotBlank(room.getUser())) {
        room.setAdmins(new String[]{room.getUser()});
      }
      if (dbRoom.containsField("timestamp")) {
        room.setTimestamp((Long) dbRoom.get("timestamp"));
      }
      roomBeans.add(room);
    }
    return roomBeans;
  }

  public RoomBean getTeamRoomById(String roomId) {
    if (roomId == null || roomId.isEmpty())
      return null;
    DBCollection cRooms = db().getCollection(M_ROOMS_COLLECTION);
    BasicDBObject qRoom = new BasicDBObject();
    qRoom.put("_id", roomId);
    qRoom.put("type", TYPE_ROOM_TEAM);
    DBObject dbRoom = cRooms.findOne(qRoom);
    if (dbRoom == null)
      return null;
    RoomBean room = new RoomBean();
    room.setRoom((String) dbRoom.get("_id"));
    room.setFullName((String) dbRoom.get("team"));
    room.setUser((String) dbRoom.get("user"));
    room.setType((String) dbRoom.get("type"));
    if (dbRoom.containsField("meetingStarted")) {
      room.setMeetingStarted((Boolean) dbRoom.get("meetingStarted"));
    }
    if (dbRoom.containsField("startTime")) {
      room.setStartTime((String) dbRoom.get("startTime"));
    }
    if (StringUtils.isNotBlank(room.getUser())) {
      room.setAdmins(new String[]{room.getUser()});
    }
    if (dbRoom.containsField("timestamp")) {
      room.setTimestamp((Long) dbRoom.get("timestamp"));
    }
    return room;
  }

  public void deleteTeamRoom(String roomId, String user) {
    RoomBean room = getTeamRoomById(roomId);
    if (room == null) {
      LOG.warning("No room with id [" + roomId + "] available to delete");
      return;
    }
    if (!room.getType().equals(ChatService.TYPE_ROOM_TEAM)) {
      LOG.warning("The room with id [" + roomId + "] is not a Team Room so it won't be deleted.");
      return;
    }
    LOG.info("Deleting Team Chat Room [" + room.getFullName() + "] (id:" + room.getRoom() + ")");
    // Check if the requester is the owner of the Team Chat Room
    if (user == null || room.getUser().equals(user) == false) {
      LOG.warning("The user [" + user + "] is not the owner of the room with id [" + roomId + "] so this room won't be deleted.");
      return;
    }

    // Delete all message of the Team Chat Room
    DBCollection cMessages = db().getCollection(M_ROOM_PREFIX + TYPE_ROOM_TEAM);
    BasicDBObject qMessages = new BasicDBObject();
    qMessages.put("roomId", roomId);
    cMessages.remove(qMessages, WriteConcern.ACKNOWLEDGED);
    LOG.info("Messages of room [" + roomId + "] deleted");

    // Remove the Team Chat Room from all the users
    List<String> users = userDataStorage.getUsersFilterBy(null, roomId, TYPE_ROOM_TEAM);
    userDataStorage.removeTeamUsers(roomId, users);
    LOG.info("All users removed from the team room [" + roomId + "]");

    // Delete the Team Chat Room
    DBCollection cRooms = db().getCollection(M_ROOMS_COLLECTION);
    BasicDBObject qRoom = new BasicDBObject();
    qRoom.put("_id", roomId);
    cRooms.remove(qRoom, WriteConcern.ACKNOWLEDGED);
    LOG.info("Team room [" + roomId + "] deleted");
  }

  public void edit(String room, String user, String messageId, String message) {
    String roomType = getTypeRoomChat(room);
    DBCollection coll = db().getCollection(M_ROOM_PREFIX + roomType);

    message = StringUtils.chomp(message);
    message = message.replaceAll("&", "&#38");
    message = message.replaceAll("<", "&lt;");
    message = message.replaceAll(">", "&gt;");
    message = message.replaceAll("\"", "&quot;");
    message = message.replaceAll("\n", "<br/>");
    message = message.replaceAll("\\\\", "&#92");

    BasicDBObject query = new BasicDBObject();
    query.put("_id", new ObjectId(messageId));
    query.put("user", user);
    query.put("roomId", room);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext()) {
      DBObject dbo = cursor.next();
      dbo.put("message", message);
      dbo.put("type", TYPE_EDITED);
      dbo.put("lastUpdatedTimestamp", System.currentTimeMillis());
      coll.save(dbo, WriteConcern.UNACKNOWLEDGED);
    }
  }

  public String read(String room) {
    return read(room, false, null, null, 0);
  }

  public String read(String room, boolean isTextOnly, Long fromTimestamp) {
    return read(room, isTextOnly, fromTimestamp, null, 0);
  }

  public String read(String room, boolean isTextOnly, Long fromTimestamp, Long toTimestamp, int limitToLoad) {
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);

    String roomType = getTypeRoomChat(room);
    DBCollection coll = db().getCollection(M_ROOM_PREFIX + roomType);

    BasicDBObject query = new BasicDBObject();
    query.put("roomId", room);

    BasicDBObject duration = null;
    if (fromTimestamp != null) {
      duration = new BasicDBObject("$gt", fromTimestamp);
    }

    if (toTimestamp != null) {
      if (duration == null) {
        duration = new BasicDBObject("$lt", toTimestamp);
      } else {
        duration.append("$lt", toTimestamp);
      }
    }

    if (duration != null) {
      BasicDBObject ts = new BasicDBObject("timestamp", duration);
      BasicDBObject updts = new BasicDBObject("lastUpdatedTimestamp", duration);
      query.put("$or", new BasicDBObject[]{ts, updts});
    }

    BasicDBObject sort = new BasicDBObject();
    sort.put("timestamp", -1);
    int limit = limitToLoad > 0 ? limitToLoad : (isTextOnly) ? readTotalTxt : readTotalJson;
    DBCursor cursor = coll.find(query).sort(sort).limit(limit);
    StringBuilder sb = new StringBuilder();
    if (!cursor.hasNext()) {
      if (isTextOnly) {
        sb.append("no messages");
      } else {
        sb.append("{\"room\": \"").append(room).append("\",\"messages\": []}");
      }
    } else {
      // Just being used as a local cache
      Map<String, UserBean> users = new HashMap<String, UserBean>();

      boolean first = true;
      JSONObject data = new JSONObject();
      while (cursor.hasNext()) {
        DBObject dbo = cursor.next();
        String timestamp = dbo.get("timestamp").toString();
        if (first) //first element (most recent one)
        {
          if (!isTextOnly) {
            data.put("room", room);
            data.put("timestamp", timestamp);
            data.put("messages", new JSONArray());
          }
        }

        String user = dbo.get("user").toString();
        UserBean userBean = users.get(user);
        if (userBean == null) {
          userBean = userDataStorage.getUser(user);
          users.put(user, userBean);
        }
        String fullName = userBean.getFullname();

        if (isTextOnly) {
          String date = "";
          try {
            Date date1 = new Date(Long.parseLong(timestamp));
            date = formatterDate.format(date1);
          } catch (Exception e) {
            LOG.info("Message Date Format Error : " + e.getMessage());
          }

          StringBuilder line = new StringBuilder();
          line.append("[").append(date).append("] ");
          String message = dbo.get("message").toString();
          if (TYPE_DELETED.equals(message)) message = TYPE_DELETED;
          if ("true".equals(dbo.get("isSystem"))) {
            line.append("System Message: ");
            if (message.endsWith("<br/>")) message = message.substring(0, message.length() - 5);
            line.append(message).append("\n");
          } else {
            line.append(fullName).append(": ");
            message = message.replaceAll("<br/>", "\n");
            line.append(message).append("\n");
          }
          sb.insert(0, line);
        } else {
          MessageBean msg = toMessageBean(dbo);
          msg.setFullName(fullName);
          msg.setEnabledUser(userBean.isEnabledUser());
          msg.setExternal(userBean.isExternal());

          ((JSONArray)data.get("messages")).add(msg.toJSONObject());
        }

        first = false;
      }

      if (!isTextOnly) {
        sb.append(data.toJSONString());
      }
    }

    return sb.toString();
  }

  public MessageBean getMessage(String roomId, String messageId) {

    String roomType = getTypeRoomChat(roomId);

    DBCollection coll = db().getCollection(M_ROOM_PREFIX + roomType);

    BasicDBObject query = new BasicDBObject();
    query.put("roomId", roomId);
    query.put("_id", new ObjectId(messageId));

    DBObject object = coll.findOne(query);
    if (object != null) {
      return toMessageBean(object);
    } else {
      return null;
    }
  }

  private void updateRoomTimestamp(String room) {
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext()) {
      DBObject dbo = cursor.next();
      dbo.put("timestamp", System.currentTimeMillis());
      coll.save(dbo, WriteConcern.UNACKNOWLEDGED);
    }

  }

  private void ensureIndexInRoom(String type) {
    DBCollection coll = db().getCollection(M_ROOM_PREFIX + type);
    BasicDBObject doc = new BasicDBObject();
    doc.put("timestamp", System.currentTimeMillis());
    coll.insert(doc);
    ConnectionManager.getInstance().ensureIndexesInRoom(type);
    coll.remove(doc);
  }

  public String getSpaceRoom(String space) {
    String room = ChatUtils.getRoomId(space);
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (!cursor.hasNext()) {
      try {
        basicDBObject.put("space", space);
        basicDBObject.put("type", TYPE_ROOM_SPACE);
        basicDBObject.put("meetingStarted", false);
        basicDBObject.put("startTime", "");
        coll.insert(basicDBObject);
        ensureIndexInRoom(TYPE_ROOM_SPACE);
      } catch (MongoException me) {
        LOG.warning(me.getCode() + " : " + room + " : " + me.getMessage());
      }
    }

    return room;
  }

  public String getSpaceRoomByName(String name) {
    String room = null;
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("shortName", name);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext()) {
      DBObject doc = cursor.next();
      room = doc.get("_id").toString();
    }

    return room;
  }

  public String getTeamRoom(String team, String user) {
    String room = ChatUtils.getRoomId(team, user);
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (!cursor.hasNext()) {
      try {
        basicDBObject.put("team", team);
        basicDBObject.put("user", user);
        basicDBObject.put("type", TYPE_ROOM_TEAM);
        basicDBObject.put("meetingStarted", false);
        basicDBObject.put("startTime", "");
        basicDBObject.put("timestamp", System.currentTimeMillis());
        basicDBObject.put("isEnabled", true);
        coll.insert(basicDBObject);
        ensureIndexInRoom(TYPE_ROOM_TEAM);
      } catch (MongoException me) {
        LOG.warning(me.getCode() + " : " + room + " : " + me.getMessage());
      }
    }

    return room;
  }

  public String getExternalRoom(String identifier) {
    String room = ChatUtils.getExternalRoomId(identifier);
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (!cursor.hasNext()) {
      try {
        basicDBObject.put("identifier", identifier);
        basicDBObject.put("type", TYPE_ROOM_EXTERNAL);
        basicDBObject.put("isEnabled", true);
        coll.insert(basicDBObject);
        ensureIndexInRoom(TYPE_ROOM_EXTERNAL);
      } catch (MongoException me) {
        LOG.warning(me.getCode() + " : " + room + " : " + me.getMessage());
      }
    }

    return room;
  }

  public String getTeamCreator(String room) {
    if (room.indexOf(ChatService.TEAM_PREFIX) == 0) {
      room = room.substring(ChatService.TEAM_PREFIX.length());
    }
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);

    String creator = "";
    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext()) {
      try {
        DBObject dbo = cursor.next();
        creator = dbo.get("user").toString();
      } catch (MongoException me) {
        LOG.warning(me.getCode() + " : " + room + " : " + me.getMessage());
      }
    }

    return creator;
  }

  public void setRoomName(String room, String name) {
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext()) {
      DBObject dbo = cursor.next();
      dbo.put("team", name);
      coll.save(dbo, WriteConcern.UNACKNOWLEDGED);
    }
  }

  @Override
  public boolean isRoomEnabled(String room) {
    boolean isEnabled = true;
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext()) {
      try {
        DBObject doc = cursor.next();
        if (doc.get("isEnabled") != null) {
          isEnabled = (StringUtils.equals(doc.get("isEnabled").toString(), "true"));
        } else {
          doc.put("isEnabled", true);
          coll.save(doc, WriteConcern.UNACKNOWLEDGED);
        }
      } catch (MongoException me) {
        LOG.severe(me.getCode() + " : " + room + " : " + me.getMessage());
      }
    }

    return isEnabled;
  }

  @Override
  public void setRoomEnabled(String room, boolean enabled) {
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext()) {
      DBObject dbo = cursor.next();
      dbo.put("isEnabled", enabled);
      coll.save(dbo, WriteConcern.UNACKNOWLEDGED);
    }
  }

  @Override
  public void setRoomMeetingStatus(String room, boolean start, String startTime) {
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext()) {
      DBObject dbo = cursor.next();
      dbo.put("meetingStarted", start);
      dbo.put("startTime", startTime);
      coll.save(dbo, WriteConcern.UNACKNOWLEDGED);
    }    
  }

  public String getRoom(List<String> users) {
    Collections.sort(users);
    String room = ChatUtils.getRoomId(users);
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (!cursor.hasNext()) {
      try {
        basicDBObject.put("users", users);
        basicDBObject.put("type", TYPE_ROOM_USER);
        basicDBObject.put("isEnabled", true);
        coll.insert(basicDBObject);
        ensureIndexInRoom(TYPE_ROOM_USER);
      } catch (MongoException me) {
        LOG.warning(me.getCode() + " : " + room + " : " + me.getMessage());
      }
    }

    return room;
  }

  public String getTypeRoomChat(String roomId) {
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("_id", roomId);
    DBCursor cursor = coll.find(query);
    Object roomType = null;
    while (cursor.hasNext()) {
      DBObject doc = cursor.next();
      roomType = doc.get("type");
    }
    return roomType == null ? "" : roomType.toString();
  }

  public List<RoomBean> getExistingRooms(String user, boolean withPublic, boolean isAdmin, NotificationService notificationService, TokenService tokenService) {
    List<RoomBean> rooms = new ArrayList<RoomBean>();
    String roomId = null;
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("users", user);

    DBCursor cursor = coll.find(basicDBObject);
    while (cursor.hasNext()) {
      DBObject dbo = cursor.next();
      roomId = dbo.get("_id").toString();
      long timestamp = -1;
      if (dbo.containsField("timestamp")) {
        timestamp = ((Long) dbo.get("timestamp")).longValue();
      }
      List<String> users = ((List<String>) dbo.get("users"));
      users.remove(user);
      if (users.size() > 0 && !user.equals(users.get(0))) {
        String targetUser = users.get(0);
        UserBean targetUserBean = userDataStorage.getUser(targetUser);
        boolean isDemoUser = tokenService.isDemoUser(targetUser);
        if (!isAdmin || (isAdmin && ((!withPublic && !isDemoUser) || (withPublic && isDemoUser)))) {
          RoomBean roomBean = new RoomBean();
          roomBean.setRoom(roomId);
          roomBean.setEnabledUser(targetUserBean.isEnabled());
          roomBean.setExternal(targetUserBean.isExternal());
          roomBean.setUnreadTotal(notificationService.getUnreadNotificationsTotal(user, "chat", "room", roomId));
          roomBean.setUser(users.get(0));
          roomBean.setTimestamp(timestamp);
          roomBean.setType((String) dbo.get("type"));
          if (dbo.containsField("isEnabled")) {
            roomBean.setEnabledRoom((StringUtils.equals(dbo.get("isEnabled").toString(), "true")));
          }
          if (dbo.containsField("meetingStarted")) {
            roomBean.setMeetingStarted((Boolean) dbo.get("meetingStarted"));
          }
          if (dbo.containsField("startTime")) {
            roomBean.setStartTime((String) dbo.get("startTime"));
          }
          String creator = (String) dbo.get("user");
          if (StringUtils.isNotBlank(creator)) {
            roomBean.setAdmins(new String[]{creator});
          }
          rooms.add(roomBean);
        }
      }
    }

    return rooms;
  }

  public RoomsBean getRooms(String user, List<String> onlineUsers, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, int limit, NotificationService notificationService, TokenService tokenService) {
    List<RoomBean> rooms;
    UserBean userBean = userDataStorage.getUser(user, true);
    int unreadOffline = 0, unreadOnline = 0, totalRooms = 0;

    if (withUsers) {
      rooms = this.getExistingRooms(user, withPublic, isAdmin, notificationService, tokenService);
      if (isAdmin) {
        rooms.addAll(this.getExistingRooms(UserService.SUPPORT_USER, withPublic, isAdmin, notificationService, tokenService));
      }

      Map<String, UserBean> availableUsers = tokenService.getActiveUsersFilterBy(user, onlineUsers, withUsers, withPublic, isAdmin, limit);
      List<RoomBean> roomsOffline = new ArrayList<>();

      for (RoomBean roomBean : rooms) {
        String targetUser = roomBean.getUser();
        roomBean.setFavorite(userBean.isFavorite(roomBean.getRoom()));

        if (availableUsers.containsKey(targetUser)) {
          UserBean targetUserBean = availableUsers.remove(targetUser);
          roomBean.setFullName(targetUserBean.getFullname());
          roomBean.setExternal(targetUserBean.isExternal());
          roomBean.setStatus(targetUserBean.getStatus());
          roomBean.setAvailableUser(true);
          if (roomBean.getUnreadTotal() > 0)
            unreadOnline += roomBean.getUnreadTotal();
        } else {
          UserBean targetUserBean = userDataStorage.getUser(targetUser);
          roomBean.setFullName(targetUserBean.getFullname());
          roomBean.setExternal(targetUserBean.isExternal());
          roomBean.setAvailableUser(false);

          if (!withOffline) {
            roomsOffline.add(roomBean);
          }
          if (roomBean.getUnreadTotal() > 0) {
            unreadOffline += roomBean.getUnreadTotal();
          }
        }
      }

      if (!withOffline) {
        for (RoomBean roomBean : roomsOffline) {
          rooms.remove(roomBean);
        }
      }

      for (UserBean availableUser : availableUsers.values()) {
        String status = availableUser.getStatus();
        if (withOffline || (!withOffline && !UserMongoDataStorage.STATUS_INVISIBLE.equals(status) && !UserMongoDataStorage.STATUS_OFFLINE.equals(status))) {
          RoomBean roomBean = new RoomBean();
          roomBean.setUser(availableUser.getName());
          roomBean.setFullName(availableUser.getFullname());
          roomBean.setExternal(availableUser.isExternal());
          roomBean.setStatus(availableUser.getStatus());
          roomBean.setAvailableUser(true);
          roomBean.setType(ChatService.TYPE_ROOM_USER);
          rooms.add(roomBean);
        }
      }
    } else {
      rooms = new ArrayList<RoomBean>();
    }

    int unreadSpaces = 0;
    List<SpaceBean> spaces = userDataStorage.getSpaces(user);
    for (SpaceBean space : spaces) {
      RoomBean room = new RoomBean();
      room.setUser(SPACE_PREFIX + space.getRoom());
      room.setRoom(space.getRoom());
      room.setFullName(space.getDisplayName());
      room.setStatus(UserService.STATUS_SPACE);
      room.setTimestamp(space.getTimestamp());
      room.setAvailableUser(true);
      room.setType(ChatService.TYPE_ROOM_SPACE);
      room.setPrettyName(space.getPrettyName());
      room.setGroupId(space.getGroupId());

      String spaceRoomId = getSpaceRoom(SPACE_PREFIX + space.getRoom());
      room.setEnabledRoom(isRoomEnabled(spaceRoomId));

      room.setUnreadTotal(notificationService.getUnreadNotificationsTotal(user, "chat", "room", spaceRoomId));
      if (room.getUnreadTotal() > 0)
        unreadSpaces += room.getUnreadTotal();
      room.setFavorite(userBean.isFavorite(room.getRoom()));
      if (withSpaces) {
        rooms.add(room);
      }
    }

    int unreadTeams = 0;
    List<RoomBean> teams = userDataStorage.getTeams(user);
    for (RoomBean team : teams) {
      RoomBean room = new RoomBean();
      room.setUser(TEAM_PREFIX + team.getRoom());
      room.setRoom(team.getRoom());
      room.setFullName(team.getFullName());
      room.setExternal(team.isExternal());
      room.setStatus(UserService.STATUS_TEAM);
      room.setTimestamp(team.getTimestamp());
      room.setAvailableUser(true);
      room.setType(team.getType());
      room.setMeetingStarted(team.isMeetingStarted());
      room.setEnabledRoom(team.isEnabledRoom());
      room.setStartTime(team.getStartTime());
      room.setAdmins(team.getAdmins());
      room.setEnabledRoom(team.isEnabledRoom());

      room.setUnreadTotal(notificationService.getUnreadNotificationsTotal(user, "chat", "room", team.getRoom()));
      if (room.getUnreadTotal() > 0)
        unreadTeams += room.getUnreadTotal();
      room.setFavorite(userBean.isFavorite(room.getRoom()));
      if (withSpaces) {
        rooms.add(room);
      }

    }

    List<RoomBean> finalRooms = new ArrayList<RoomBean>();
    if (StringUtils.isNotBlank(filter)) {
      for (RoomBean roomBean : rooms) {
        String targetUser = roomBean.getFullName();
        if (filter(targetUser, filter))
          finalRooms.add(roomBean);
      }
    } else {
      finalRooms = rooms;
    }
    //get rid of disabled rooms
    finalRooms = finalRooms.stream().filter(roomBean -> roomBean.isEnabledRoom()).collect(Collectors.toList());

    RoomsBean roomsBean = new RoomsBean();
    roomsBean.setRooms(finalRooms);
    roomsBean.setUnreadOffline(unreadOffline);
    roomsBean.setUnreadOnline(unreadOnline);
    roomsBean.setUnreadSpaces(unreadSpaces);
    roomsBean.setUnreadTeams(unreadTeams);
    roomsBean.setRoomsCount(finalRooms.size());

    return roomsBean;

  }


  /**
   * This will load all user rooms with pagination
   * @param user the user for whom roomw will be loaded
   * @param onlineUsers list of online users
   * @param filter the filter used to filter rooms
   * @param offset the current offset
   * @param limit the limit of rooms
   * @param notificationService service storing rooms notifications
   * @param tokenService service storing tokens
   * @return RoomsBean containing all rooms with unread messages
   */
  public RoomsBean getUserRooms(String user, List<String> onlineUsers, String filter, int offset, int limit, NotificationService notificationService, TokenService tokenService) {
    return getUserRooms(user, onlineUsers, filter, offset, limit, notificationService, tokenService, null);
  }

  /**
   * This will load all user rooms with pagination
   * @param user the user for whom roomw will be loaded
   * @param onlineUsers list of online users
   * @param filter the filter used to filter rooms
   * @param offset the current offset
   * @param limit the limit of rooms
   * @param notificationService service storing rooms notifications
   * @param tokenService service storing tokens
   * @param roomType type of the room : u for one to one , t for team room or s for space rooms
   * @return RoomsBean containing all rooms with unread messages
   */
  public RoomsBean getUserRooms(String user, List<String> onlineUsers, String filter, int offset, int limit, NotificationService notificationService, TokenService tokenService, String roomType) {
    List<RoomBean> rooms = new ArrayList<>();
    int unreadOffline = 0;
    int unreadOnline = 0;
    int unreadSpaces = 0;
    int unreadTeams = 0;
    int unreadSilentRooms = 0;
    int roomsCount = 0;
    UserBean userBean = userDataStorage.getUser(user, true);

    DBCollection coll = db().getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext()) {
      List<String> roomsIds = new ArrayList<>();
      List<BasicDBObject> andList = new ArrayList<>();
      List<BasicDBObject> orList = new ArrayList<>();
      DBObject doc = cursor.next();

      if (TYPE_ROOM_FAVORITE.equals(roomType)) {
        List<String> favoriteRoomsIds = userBean.getFavorites();
        if (favoriteRoomsIds != null) {
          orList.add(new BasicDBObject("_id", new BasicDBObject("$in", favoriteRoomsIds)));
        }
      } else {
        if (StringUtils.isBlank(roomType) || TYPE_ROOM_SPACE.equals(roomType)) {
          BasicDBList spaces = (BasicDBList) doc.get("spaces");
          if (spaces != null) {
            for (Object room : spaces) {
              roomsIds.add((String) room);
            }
          }
        }
        if (StringUtils.isBlank(roomType) || TYPE_ROOM_TEAM.equals(roomType)) {
          BasicDBList teams = (BasicDBList) doc.get("teams");
          if (teams != null) {
            for (Object room : teams) {
              roomsIds.add((String) room);
            }
          }
        }
        // Add spaces and teams rooms
        orList.add(new BasicDBObject("_id", new BasicDBObject("$in", roomsIds)));
        if (StringUtils.isBlank(roomType) || TYPE_ROOM_USER.equals(roomType)) {
          // Add user to user rooms
          orList.add(new BasicDBObject("users", user));
        }
      }

      DBObject roomsQuery = new BasicDBObject("$and", andList);
      List<BasicDBObject> enabledRoomOrList = new ArrayList<>();
      enabledRoomOrList.add(new BasicDBObject("isEnabled", true));
      enabledRoomOrList.add(new BasicDBObject("isEnabled", new BasicDBObject("$exists", false)));
      andList.add(new BasicDBObject("$or", enabledRoomOrList));
      andList.add(new BasicDBObject("$or", orList));

      roomsCount = db().getCollection(M_ROOMS_COLLECTION).find(roomsQuery).count();
      DBCursor roomsCursor;
      if (StringUtils.isBlank(filter)) {
        roomsCursor = db().getCollection(M_ROOMS_COLLECTION).find(roomsQuery)
                .sort(new BasicDBObject("timestamp", -1)).skip(offset).limit(limit);
      } else {
        // There is no way to do a Join in MongoDB, data structure should be altered to make it possible to search for rooms and get user fullNames
        // we added a hard limit 100 to load the latest 100 rooms of the user and then search their display names
        roomsCursor = db().getCollection(M_ROOMS_COLLECTION).find(roomsQuery)
                .sort(new BasicDBObject("timestamp", -1)).limit(100);
      }
      while (roomsCursor.hasNext()) {
        DBObject room = roomsCursor.next();
        RoomBean roomBean = convertToBean(userBean, onlineUsers, room, notificationService);

        if (roomBean.getUnreadTotal() > 0) {
          switch (roomBean.getType()) {
            case "u":
              if (roomBean.isActive()) {
                unreadOnline += roomBean.getUnreadTotal();
              } else {
                unreadOffline += roomBean.getUnreadTotal();
              }
              break;
            case "s":
              unreadSpaces += roomBean.getUnreadTotal();
              if (roomBean.isRoomSilent()) {
                unreadSilentRooms += roomBean.getUnreadTotal();
              }
              break;
            case "t":
              unreadTeams += roomBean.getUnreadTotal();
              if (roomBean.isRoomSilent()) {
                unreadSilentRooms += roomBean.getUnreadTotal();
              }
              break;
          }
        }
        rooms.add(roomBean);
      }
    }

    List<RoomBean> finalRooms = new ArrayList<>();
    if (StringUtils.isNotBlank(filter)) {
      roomsCount = 0;
      for (RoomBean roomBean : rooms) {
        String targetUser = roomBean.getFullName();
        if (filter(targetUser, filter))
          finalRooms.add(roomBean);
        roomsCount++;
      }
    } else {
      finalRooms = rooms;
    }

    RoomsBean roomsBean = new RoomsBean();
    roomsBean.setRooms(finalRooms);
    roomsBean.setUnreadOffline(unreadOffline);
    roomsBean.setUnreadOnline(unreadOnline);
    roomsBean.setUnreadSpaces(unreadSpaces);
    roomsBean.setUnreadTeams(unreadTeams);
    roomsBean.setRoomsCount(roomsCount);
    roomsBean.setUnreadSilentRooms(unreadSilentRooms);
    return roomsBean;
  }

  /**
   * This function creates a RoomBean directly from a room stored in MongoDB
   * @param userBean
   * @param onlineUsers
   * @param room
   * @param notificationService
   * @return a RoomBean representing the loaded room from Database
   */
  private RoomBean convertToBean(UserBean userBean, List<String> onlineUsers, DBObject room, NotificationService notificationService) {
    String type = room.get("type").toString();
    String roomId = room.get("_id").toString();
    RoomBean roomBean = new RoomBean();
    switch (type) {
      case "t": {
        roomBean.setUser(TEAM_PREFIX + roomId);
        roomBean.setStatus(UserService.STATUS_TEAM);
        roomBean.setAvailableUser(true);
        roomBean.setFullName(room.get("team").toString());
        roomBean.setType(TYPE_ROOM_TEAM);
        roomBean.setFavorite(userBean.isFavorite(roomId));
        if (StringUtils.isNotBlank(roomBean.getUser())) {
          roomBean.setAdmins(new String[]{room.get("user").toString()});
        }
        break;
      }
      case "u": {
        List<String> users = ((List<String>)room.get("users"));
        users.remove(userBean.getName());
        if(users.size() > 0) {
          String targetUser = users.get(0);
          UserBean targetUserBean = userDataStorage.getUser(targetUser);
          roomBean.setFullName(targetUserBean.getFullname());
          roomBean.setFavorite(userBean.isFavorite(room.get("_id").toString()));
          roomBean.setEnabledUser(targetUserBean.isEnabledUser());
          roomBean.setExternal(targetUserBean.isExternal());
          if(onlineUsers.contains(targetUser)) {
            roomBean.setAvailableUser(true);
            roomBean.setStatus(userDataStorage.getStatus(targetUser));
          } else {
            roomBean.setAvailableUser(false);
            roomBean.setStatus(STATUS_OFFLINE);
          }
          roomBean.setUser(targetUser);
          roomBean.setType(TYPE_ROOM_USER);
        }
        break;
      }
      case "s": {
        roomBean.setUser(SPACE_PREFIX + roomId);
        roomBean.setFullName(room.get("displayName").toString());
        roomBean.setStatus(UserService.STATUS_SPACE);
        roomBean.setType(TYPE_ROOM_SPACE);
        roomBean.setAvailableUser(true);
        roomBean.setType(ChatService.TYPE_ROOM_SPACE);
        roomBean.setGroupId(room.get("groupId").toString());
        if (room.containsField("prettyName")) {
          roomBean.setPrettyName(room.get("prettyName").toString());
        }

        roomBean.setFavorite(userBean.isFavorite(roomId));
        break;
      }
    }
    roomBean.setRoom(roomId);
    roomBean.setUnreadTotal(notificationService.getUnreadNotificationsTotal(userBean.getName(), "chat", "room", roomId));
    boolean isSilent = notificationService.isRoomSilentForUser(userBean.getName(), roomId);
    roomBean.setRoomSilent(isSilent);

    if (room.containsField("meetingStarted")) {
      roomBean.setMeetingStarted((Boolean) room.get("meetingStarted"));
    }
    if (room.containsField("startTime")) {
      roomBean.setStartTime((String) room.get("startTime"));
    }
    if (room.containsField("timestamp")) {
      roomBean.setTimestamp((Long) room.get("timestamp"));
    }
    return roomBean;
  }

  private boolean filter(String user, String filter) {
    if (user == null || filter == null || "".equals(filter)) return true;

    String[] args = filter.toLowerCase().split(" ");
    String s = user.toLowerCase();
    int ind;
    for (String arg : args) {
      ind = s.indexOf(arg);
      if (ind == -1)
        return false;
      else
        s = s.substring(ind);
    }
    return true;
  }

  public int getNumberOfRooms() {
    DBCollection coll = db().getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    DBCursor cursor = coll.find(query);
    return cursor.count();
  }

  public int getNumberOfMessages() {
    int nb = 0;
    String[] roomTypes = {TYPE_ROOM_USER, TYPE_ROOM_SPACE, TYPE_ROOM_TEAM, TYPE_ROOM_EXTERNAL};
    for (String type : roomTypes) {
      DBCollection collr = db().getCollection(M_ROOM_PREFIX + type);
      BasicDBObject queryr = new BasicDBObject();
      DBCursor cursorr = collr.find(queryr);
      nb += cursorr.count();
    }

    return nb;
  }

  private MessageBean toMessageBean(DBObject dbo) {
    MessageBean msg = new MessageBean();
    msg.setId(dbo.get("_id").toString());
    msg.setUser(dbo.get("user").toString());
    msg.setMessage(dbo.get("message").toString());
    msg.setTimestamp(Long.parseLong(dbo.get("timestamp").toString()));
    if (dbo.containsField("lastUpdatedTimestamp")) {
      msg.setLastUpdatedTimestamp(Long.parseLong(dbo.get("lastUpdatedTimestamp").toString()));
    }
    msg.setSystem(Boolean.parseBoolean(dbo.get("isSystem").toString()));
    if (dbo.containsField("options")) {
      msg.setOptions(dbo.get("options").toString());
    }
    if (dbo.containsField("type")) {
      msg.setType(dbo.get("type").toString());
    }

    return msg;
  }
}
