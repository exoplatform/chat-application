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
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
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
  public static final String BR = "<br/>";
  public static final String MESSAGE = "message";
  public static final String TIMESTAMP = "timestamp";
  public static final String USER = "user";
  public static final String IS_SYSTEM = "isSystem";
  public static final String ROOM_ID = "roomId";
  public static final String OPTIONS = "options";
  public static final String LAST_UPDATED_TIMESTAMP = "lastUpdatedTimestamp";
  public static final String MEETING_STARTED = "meetingStarted";
  public static final String START_TIME = "startTime";
  public static final String IS_ENABLED = "isEnabled";
  public static final String USERS = "users";
  public static final String TYPE = "type";

  private final int readTotalJson;
  private final int readTotalTxt;

  @Inject
  private UserDataStorage userDataStorage;

  private final SimpleDateFormat formatterDate = new SimpleDateFormat("dd/MM/yyyy hh:mm aaa");

  public ChatMongoDataStorage() {
    readTotalJson = Integer.parseInt(PropertyManager.getProperty(PropertyManager.PROPERTY_READ_TOTAL_JSON));
    readTotalTxt = Integer.parseInt(PropertyManager.getProperty(PropertyManager.PROPERTY_READ_TOTAL_TXT));
  }

  private MongoDatabase db() {
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
    MongoCollection<Document> coll = db().getCollection(M_ROOM_PREFIX + roomType);

    message = StringUtils.chomp(message);
    message = message.replace("&", "&#38");
    message = message.replace("<", "&lt;");
    message = message.replace(">", "&gt;");
    message = message.replace("\"", "&quot;");
    message = message.replace("\n", BR);
    message = message.replace("\\\\", "&#92");
    message = message.replace("\t", "  ");

    Document doc = new Document();
    doc.put(USER, user);
    doc.put(MESSAGE, message);
    doc.put(TIMESTAMP, System.currentTimeMillis());
    doc.put(IS_SYSTEM, isSystem);
    doc.put(ROOM_ID, room);
    if (options != null) {
      options = options.replace("<", "&lt;");
      options = options.replace(">", "&gt;");
      doc.put(OPTIONS, options);
    }
    coll.insertOne(doc);

    this.updateRoomTimestamp(room);

    return doc.get("_id").toString();
  }

  public void delete(String room, String user, String messageId) {
    String roomType = getTypeRoomChat(room);
    MongoCollection<Document> coll = db().getCollection(M_ROOM_PREFIX + roomType);
    Document query = new Document();
    query.put("_id", new ObjectId(messageId));
    query.put(USER, user);
    query.put(ROOM_ID, room);
    FindIterable<Document> cursor = coll.find(query);
    cursor.forEach(document -> {
      document.put(MESSAGE, TYPE_DELETED);
      document.put(TYPE, TYPE_DELETED);
      document.put(LAST_UPDATED_TIMESTAMP, System.currentTimeMillis());
      coll.insertOne(document);
    });
  }

  @Override
  public List<RoomBean> getTeamRoomByName(String teamName) {
    if (StringUtils.isBlank(teamName))
      return null;
    MongoCollection<Document> cRooms = db().getCollection(M_ROOMS_COLLECTION);
    Document qRoom = new Document();
    qRoom.put("team", teamName);
    qRoom.put(TYPE, TYPE_ROOM_TEAM);
    List<RoomBean> roomBeans = new ArrayList<>();
    FindIterable<Document> roomsCursor = cRooms.find(qRoom);
    roomsCursor.forEach(dbRoom -> {
      RoomBean room = new RoomBean();
      room.setRoom((String) dbRoom.get("_id"));
      room.setFullName((String) dbRoom.get("team"));
      room.setUser((String) dbRoom.get(USER));
      room.setType((String) dbRoom.get(TYPE));
      if (dbRoom.containsKey(MEETING_STARTED)) {
        room.setMeetingStarted((Boolean) dbRoom.get(MEETING_STARTED));
      }
      if (dbRoom.containsKey(START_TIME)) {
        room.setStartTime((String) dbRoom.get(START_TIME));
      }
      if (StringUtils.isNotBlank(room.getUser())) {
        room.setAdmins(new String[]{room.getUser()});
      }
      if (dbRoom.containsKey(TIMESTAMP)) {
        room.setTimestamp((Long) dbRoom.get(TIMESTAMP));
      }
      roomBeans.add(room);
    });
    return roomBeans;
  }

  public RoomBean getTeamRoomById(String roomId) {
    if (roomId == null || roomId.isEmpty())
      return null;
    MongoCollection<Document> cRooms = db().getCollection(M_ROOMS_COLLECTION);
    Document qRoom = new Document();
    qRoom.put("_id", roomId);
    qRoom.put(TYPE, TYPE_ROOM_TEAM);
    Document document = cRooms.find(qRoom).first();
    if(document != null) {
      RoomBean room = new RoomBean();
      room.setRoom((String) document.get("_id"));
      room.setFullName((String) document.get("team"));
      room.setUser((String) document.get(USER));
      room.setType((String) document.get(TYPE));
      if (document.containsKey(MEETING_STARTED)) {
        room.setMeetingStarted((Boolean) document.get(MEETING_STARTED));
      }
      if (document.containsKey(START_TIME)) {
        room.setStartTime((String) document.get(START_TIME));
      }
      if (StringUtils.isNotBlank(room.getUser())) {
        room.setAdmins(new String[]{room.getUser()});
      }
      if (document.containsKey(TIMESTAMP)) {
        room.setTimestamp((Long) document.get(TIMESTAMP));
      }
      return room;
    } else {
      return null;
    }
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
    if (!room.getUser().equals(user)) {
      LOG.warning("The user [" + user + "] is not the owner of the room with id [" + roomId + "] so this room won't be deleted.");
      return;
    }

    // Delete all message of the Team Chat Room
    MongoCollection<Document> cMessages = db().getCollection(M_ROOM_PREFIX + TYPE_ROOM_TEAM);
    Bson filter = Filters.eq(ROOM_ID, roomId);
    cMessages.deleteMany(filter);
    LOG.info("Messages of room [" + roomId + "] deleted");

    // Remove the Team Chat Room from all the users
    List<String> users = userDataStorage.getUsersFilterBy(null, roomId, TYPE_ROOM_TEAM);
    userDataStorage.removeTeamUsers(roomId, users);
    LOG.info("All users removed from the team room [" + roomId + "]");

    // Delete the Team Chat Room
    MongoCollection<Document> cRooms = db().getCollection(M_ROOMS_COLLECTION);
    Bson qRoom = Filters.eq("_id", roomId);
    cRooms.deleteMany(qRoom);
    LOG.info("Team room [" + roomId + "] deleted");
  }

  public void edit(String room, String user, String messageId, String message) {
    String roomType = getTypeRoomChat(room);
    MongoCollection<Document> coll = db().getCollection(M_ROOM_PREFIX + roomType);

    message = StringUtils.chomp(message);
    message = message.replace("&", "&#38");
    message = message.replace("<", "&lt;");
    message = message.replace(">", "&gt;");
    message = message.replace("\"", "&quot;");
    message = message.replace("\n", BR);
    message = message.replace("\\\\", "&#92");

    Bson query = Filters.and(Filters.eq("_id", new ObjectId(messageId)), Filters.eq(USER, user), Filters.eq(ROOM_ID, room));
    Document updateDocument = new Document().append(MESSAGE, message).append(TYPE, TYPE_EDITED).append(LAST_UPDATED_TIMESTAMP, System.currentTimeMillis());
    coll.findOneAndUpdate(query, updateDocument);
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
    MongoCollection<Document> coll = db().getCollection(M_ROOM_PREFIX + roomType);

    BasicDBObject query = new BasicDBObject();
    query.put(ROOM_ID, room);

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
      BasicDBObject ts = new BasicDBObject(TIMESTAMP, duration);
      BasicDBObject updts = new BasicDBObject(LAST_UPDATED_TIMESTAMP, duration);
      query.put("$or", new BasicDBObject[]{ts, updts});
    }

    BasicDBObject sort = new BasicDBObject();
    sort.put(TIMESTAMP, -1);
    int limit = limitToLoad > 0 ? limitToLoad : (isTextOnly) ? readTotalTxt : readTotalJson;
    StringBuilder sb = new StringBuilder();
    try (MongoCursor<Document> messagesCursor = coll.find(query).sort(sort).limit(limit).cursor()){
      if (!messagesCursor.hasNext()) {
        if (isTextOnly) {
          sb.append("no messages");
        } else {
          sb.append("{\"room\": \"").append(room).append("\",\"messages\": []}");
        }
      } else {
        // Just being used as a local cache
        Map<String, UserBean> users = new HashMap<>();

        boolean first = true;
        JSONObject data = new JSONObject();
        while (messagesCursor.hasNext()) {
          Document dbo = messagesCursor.next();
          String timestamp = dbo.get(TIMESTAMP).toString();
          if (first && !isTextOnly) {
            data.put("room", room);
            data.put(TIMESTAMP, timestamp);
            data.put("messages", new JSONArray());
          }

          String user = dbo.get(USER).toString();
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
            String message = dbo.get(MESSAGE).toString();
            if (TYPE_DELETED.equals(message)) message = TYPE_DELETED;
            if ("true".equals(dbo.get(IS_SYSTEM))) {
              line.append("System Message: ");
              if (message.endsWith(BR)) message = message.substring(0, message.length() - 5);
              line.append(message).append("\n");
            } else {
              line.append(fullName).append(": ");
              message = message.replace(BR, "\n");
              line.append(message).append("\n");
            }
            sb.insert(0, line);
          } else {
            MessageBean msg = toMessageBean(dbo);
            msg.setFullName(fullName);
            msg.setEnabledUser(userBean.isEnabledUser());
            msg.setExternal(userBean.isExternal());

            ((JSONArray) data.get("messages")).add(msg.toJSONObject());
          }

          first = false;
        }

        if (!isTextOnly) {
          sb.append(data.toJSONString());
        }
      }
    }

    return sb.toString();
  }

  public MessageBean getMessage(String roomId, String messageId) {

    String roomType = getTypeRoomChat(roomId);
    MongoCollection<Document> coll = db().getCollection(M_ROOM_PREFIX + roomType);

    Bson query = Filters.and(Filters.eq(ROOM_ID, roomId), Filters.eq("_id", new ObjectId(messageId)));
    Document object = coll.find(query).first();
    if (object != null) {
      return toMessageBean(object);
    } else {
      return null;
    }
  }

  private void updateRoomTimestamp(String room) {
    MongoCollection<Document> coll = db().getCollection(M_ROOMS_COLLECTION);
    Bson query = Filters.eq("_id", room);
    coll.findOneAndUpdate(query, new Document().append(TIMESTAMP, System.currentTimeMillis()));
  }

  public String getSpaceRoom(String space) {
    String room = ChatUtils.getRoomId(space);
    MongoCollection<Document> spaceCollection = db().getCollection(M_ROOMS_COLLECTION);

    Bson filter = Filters.eq("_id", room);

    try(MongoCursor<Document> spacesIterator = spaceCollection.find(filter).cursor()) {
      if (!spacesIterator.hasNext()) {
        try {
          Document document = new Document();
          document.put("space", space);
          document.put(TYPE, TYPE_ROOM_SPACE);
          document.put(MEETING_STARTED, false);
          document.put(START_TIME, "");
          spaceCollection.insertOne(document);
        } catch (MongoException me) {
          LOG.warning(me.getCode() + " : " + room + " : " + me.getMessage());
        }
      }
    }
    return room;
  }

  public String getSpaceRoomByName(String name) {
    String room = null;
    MongoCollection<Document> coll = db().getCollection(M_ROOMS_COLLECTION);
    Bson filter = Filters.eq("shortName", name);

    try(MongoCursor<Document> roomIterator = coll.find(filter).cursor()) {
      if (roomIterator.hasNext()) {
        Document doc = roomIterator.next();
        room = doc.get("_id").toString();
      }
    }

    return room;
  }

  public String getTeamRoom(String team, String user) {
    String room = ChatUtils.getRoomId(team, user);
    MongoCollection<Document> teamRoomCollection = db().getCollection(M_ROOMS_COLLECTION);

    Bson filterById = Filters.eq("_id", room);

    try(MongoCursor<Document> teamRoomiterator = teamRoomCollection.find(filterById).cursor()) {
      if (!teamRoomiterator.hasNext()) {
        try {
          Document document = new Document();
          document.put("team", team);
          document.put(USER, user);
          document.put(TYPE, TYPE_ROOM_TEAM);
          document.put(MEETING_STARTED, false);
          document.put(START_TIME, "");
          document.put(TIMESTAMP, System.currentTimeMillis());
          document.put(IS_ENABLED, true);
          teamRoomCollection.insertOne(document);
        } catch (MongoException me) {
          LOG.warning(me.getCode() + " : " + room + " : " + me.getMessage());
        }
      }
    }
    return room;
  }

  public String getExternalRoom(String identifier) {
    String room = ChatUtils.getExternalRoomId(identifier);
    MongoCollection<Document> externalRoomsCollection = db().getCollection(M_ROOMS_COLLECTION);

    Bson filter = Filters.eq("_id", room);

    try(MongoCursor<Document> iterator = externalRoomsCollection.find(filter).cursor()) {
      if (!iterator.hasNext()) {
        try {
          Document document = new Document();
          document.put("identifier", identifier);
          document.put(TYPE, TYPE_ROOM_EXTERNAL);
          document.put(IS_ENABLED, true);
          externalRoomsCollection.insertOne(document);
        } catch (MongoException me) {
          LOG.warning(me.getCode() + " : " + room + " : " + me.getMessage());
        }
      }
    }
    return room;
  }

  public String getTeamCreator(String room) {
    if (room.indexOf(ChatService.TEAM_PREFIX) == 0) {
      room = room.substring(ChatService.TEAM_PREFIX.length());
    }
    MongoCollection<Document> teamCreatorCollection = db().getCollection(M_ROOMS_COLLECTION);

    String creator = "";
    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    try(MongoCursor<Document> teamCreatorsIterator = teamCreatorCollection.find(basicDBObject).cursor()) {
      if (teamCreatorsIterator.hasNext()) {
        try {
          Document dbo = teamCreatorsIterator.next();
          creator = dbo.get(USER).toString();
        } catch (MongoException me) {
          LOG.warning(me.getCode() + " : " + room + " : " + me.getMessage());
        }
      }
    }

    return creator;
  }

  public void setRoomName(String room, String name) {
    MongoCollection<Document> roomsCollection = db().getCollection(M_ROOMS_COLLECTION);

    Bson filterById = Filters.eq("_id", room);

    try(MongoCursor<Document> roomsIterator = roomsCollection.find(filterById).cursor()) {
      if (roomsIterator.hasNext()) {
        roomsCollection.updateOne(filterById, new Document().append("team", name));
      }
    }
  }

  @Override
  public boolean isRoomEnabled(String room) {
    boolean isEnabled = true;
    MongoCollection<Document> coll = db().getCollection(M_ROOMS_COLLECTION);
    Bson filterById = Filters.eq("_id", room);

    try (MongoCursor<Document> roomsIterator = coll.find(filterById).cursor()) {
      if (roomsIterator.hasNext()) {
        try {
          Document doc = roomsIterator.next();
          if (doc.get(IS_ENABLED) != null) {
            isEnabled = (StringUtils.equals(doc.get(IS_ENABLED).toString(), "true"));
          } else {
            coll.updateOne(filterById, new Document().append(IS_ENABLED, true));
          }
        } catch (MongoException me) {
          LOG.severe(me.getCode() + " : " + room + " : " + me.getMessage());
        }
      }
    }

    return isEnabled;
  }

  @Override
  public void setRoomEnabled(String room, boolean enabled) {
    MongoCollection<Document> roomCollection = db().getCollection(M_ROOMS_COLLECTION);
    Bson filterById = Filters.eq("_id", room);

    try(MongoCursor<Document> cursor = roomCollection.find(filterById).cursor()) {
      if (cursor.hasNext()) {
        roomCollection.updateOne(filterById, new Document().append(IS_ENABLED, enabled));
      }
    }
  }

  @Override
  public void setRoomMeetingStatus(String room, boolean start, String startTime) {
    MongoCollection<Document> roomCollection = db().getCollection(M_ROOMS_COLLECTION);
    Bson filterById = Filters.eq("_id", room);

    try(MongoCursor<Document> roomIterator = roomCollection.find(filterById).cursor()) {
      if (roomIterator.hasNext()) {
        Document updateDocument = new Document();
        updateDocument.put(MEETING_STARTED, start);
        updateDocument.put(START_TIME, startTime);
        roomCollection.updateOne(filterById, updateDocument);
      }
    }
  }

  public String getRoom(List<String> users) {
    Collections.sort(users);
    String room = ChatUtils.getRoomId(users);
    MongoCollection<Document> roomCollection = db().getCollection(M_ROOMS_COLLECTION);

    Bson filterById = Filters.eq("_id", room);

    try(MongoCursor<Document> roomIterator = roomCollection.find(filterById).iterator()) {
      if (!roomIterator.hasNext()) {
        try {
          Document document = new Document();
          document.put(USERS, users);
          document.put(TYPE, TYPE_ROOM_USER);
          document.put(IS_ENABLED, true);
          roomCollection.insertOne(document);
        } catch (MongoException me) {
          LOG.warning(me.getCode() + " : " + room + " : " + me.getMessage());
        }
      }
    }

    return room;
  }

  public String getTypeRoomChat(String roomId) {
    MongoCollection<Document> roomsCollection = db().getCollection(M_ROOMS_COLLECTION);
    Bson filterById = Filters.eq("_id", roomId);

    Iterator<Document> roomsIterator = roomsCollection.find(filterById).iterator();
    String roomType = "";
    while (roomsIterator.hasNext()) {
      Document doc = roomsIterator.next();
      roomType = (String) doc.get(TYPE);
    }
    return roomType;
  }

  public List<RoomBean> getExistingRooms(String user, boolean withPublic, boolean isAdmin, NotificationService notificationService, TokenService tokenService) {
    List<RoomBean> rooms = new ArrayList<>();
    String roomId;
    MongoCollection<Document> coll = db().getCollection(M_ROOMS_COLLECTION);

    Bson filterByUsers = Filters.eq(USERS, user);

    try (MongoCursor<Document> cursor = coll.find(filterByUsers).cursor()) {
      while (cursor.hasNext()) {
        Document dbo = cursor.next();
        roomId = dbo.get("_id").toString();
        long timestamp = -1;
        if (dbo.containsKey(TIMESTAMP)) {
          timestamp = (Long) dbo.get(TIMESTAMP);
        }
        List<String> users = ((List<String>) dbo.get(USERS));
        users.remove(user);
        if (!users.isEmpty() && !user.equals(users.get(0))) {
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
            roomBean.setType((String) dbo.get(TYPE));
            if (dbo.containsKey(IS_ENABLED)) {
              roomBean.setEnabledRoom((StringUtils.equals(dbo.get(IS_ENABLED).toString(), "true")));
            }
            if (dbo.containsKey(MEETING_STARTED)) {
              roomBean.setMeetingStarted((Boolean) dbo.get(MEETING_STARTED));
            }
            if (dbo.containsKey(START_TIME)) {
              roomBean.setStartTime((String) dbo.get(START_TIME));
            }
            String creator = (String) dbo.get(USER);
            if (StringUtils.isNotBlank(creator)) {
              roomBean.setAdmins(new String[]{creator});
            }
            rooms.add(roomBean);
          }
        }
      }
    }

    return rooms;
  }

  public RoomsBean getRooms(String user, List<String> onlineUsers, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, int limit, NotificationService notificationService, TokenService tokenService) {
    List<RoomBean> rooms;
    UserBean userBean = userDataStorage.getUser(user, true);
    int unreadOffline = 0, unreadOnline = 0;

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

    List<RoomBean> finalRooms = new ArrayList<>();
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

    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    Bson filterByUser = Filters.eq(USER, user);
    try (MongoCursor<Document> userRooms = coll.find(filterByUser).cursor()) {
      if (userRooms.hasNext()) {
        List<String> roomsIds = new ArrayList<>();
        List<BasicDBObject> andList = new ArrayList<>();
        List<BasicDBObject> orList = new ArrayList<>();
        Document doc = userRooms.next();

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
            orList.add(new BasicDBObject(USERS, user));
          }
        }

        Bson roomsQuery = new BasicDBObject("$and", andList);
        List<BasicDBObject> enabledRoomOrList = new ArrayList<>();
        enabledRoomOrList.add(new BasicDBObject(IS_ENABLED, true));
        enabledRoomOrList.add(new BasicDBObject(IS_ENABLED, new BasicDBObject("$exists", false)));
        andList.add(new BasicDBObject("$or", enabledRoomOrList));
        andList.add(new BasicDBObject("$or", orList));

        roomsCount = db().getCollection(M_ROOMS_COLLECTION).find(roomsQuery).cursor().available();
        MongoCursor<Document> roomsCursor;
        if (StringUtils.isBlank(filter)) {
          roomsCursor = db().getCollection(M_ROOMS_COLLECTION).find(roomsQuery)
                  .sort(Sorts.descending(TIMESTAMP)).skip(offset).limit(limit).cursor();
        } else {
          // There is no way to do a Join in MongoDB, data structure should be altered to make it possible to search for rooms and get user fullNames
          // we added a hard limit 100 to load the latest 100 rooms of the user and then search their display names
          roomsCursor = db().getCollection(M_ROOMS_COLLECTION).find(roomsQuery)
                  .sort(Sorts.descending(TIMESTAMP)).limit(100).cursor();
        }
        while (roomsCursor.hasNext()) {
          Document room = roomsCursor.next();
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
  private RoomBean convertToBean(UserBean userBean, List<String> onlineUsers, Document room, NotificationService notificationService) {
    String type = room.get(TYPE).toString();
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
          roomBean.setAdmins(new String[]{room.get(USER).toString()});
        }
        break;
      }
      case "u": {
        List<String> users = ((List<String>)room.get(USERS));
        users.remove(userBean.getName());
        if(!users.isEmpty()) {
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
        if (room.containsKey("prettyName")) {
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

    if (room.containsKey(MEETING_STARTED)) {
      roomBean.setMeetingStarted((Boolean) room.get(MEETING_STARTED));
    }
    if (room.containsKey(START_TIME)) {
      roomBean.setStartTime((String) room.get(START_TIME));
    }
    if (room.containsKey(TIMESTAMP)) {
      roomBean.setTimestamp((Long) room.get(TIMESTAMP));
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
    MongoCollection<Document> coll = db().getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    FindIterable<Document> rooms = coll.find(query);
    return rooms.cursor().available();
  }

  public int getNumberOfMessages() {
    int nb = 0;
    String[] roomTypes = {TYPE_ROOM_USER, TYPE_ROOM_SPACE, TYPE_ROOM_TEAM, TYPE_ROOM_EXTERNAL};
    for (String type : roomTypes) {
      MongoCollection<Document> collaboration = db().getCollection(M_ROOM_PREFIX + type);
      Bson queryMessages = new Document();
      MongoCursor<Document> messagesCursor = collaboration.find(queryMessages).cursor();
      nb += messagesCursor.available();
    }

    return nb;
  }

  private MessageBean toMessageBean(Document dbo) {
    MessageBean msg = new MessageBean();
    msg.setId(dbo.get("_id").toString());
    msg.setUser(dbo.get(USER).toString());
    msg.setMessage(dbo.get(MESSAGE).toString());
    msg.setTimestamp(Long.parseLong(dbo.get(TIMESTAMP).toString()));
    if (dbo.containsKey(LAST_UPDATED_TIMESTAMP)) {
      msg.setLastUpdatedTimestamp(Long.parseLong(dbo.get(LAST_UPDATED_TIMESTAMP).toString()));
    }
    msg.setSystem(Boolean.parseBoolean(dbo.get(IS_SYSTEM).toString()));
    if (dbo.containsKey(OPTIONS)) {
      msg.setOptions(dbo.get(OPTIONS).toString());
    }
    if (dbo.containsKey(TYPE)) {
      msg.setType(dbo.get(TYPE).toString());
    }

    return msg;
  }
}
