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
import org.json.simple.parser.JSONParser;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

import static org.exoplatform.chat.services.ChatService.*;

@Named("chatStorage")
@ApplicationScoped
public class ChatMongoDataStorage implements ChatDataStorage {

  private static final Logger LOG = Logger.getLogger("ChatMongoDataStorage");

  public static final String M_ROOM_PREFIX = "messages_room_";
  public static final String M_ROOMS_COLLECTION = "rooms";

  private long readMillis;
  private int readTotalJson, readTotalTxt;

  @Inject
  private UserDataStorage userDataStorage;

  public ChatMongoDataStorage() {
    long readDays = Long.parseLong(PropertyManager.getProperty(PropertyManager.PROPERTY_READ_DAYS));
    readMillis = readDays * 24 * 60 * 60 * 1000;
    readTotalJson = Integer.parseInt(PropertyManager.getProperty(PropertyManager.PROPERTY_READ_TOTAL_JSON));
    readTotalTxt = Integer.parseInt(PropertyManager.getProperty(PropertyManager.PROPERTY_READ_TOTAL_TXT));
  }

  private DB db(String dbName) {
    if (StringUtils.isEmpty(dbName)) {
      return ConnectionManager.getInstance().getDB();
    } else {
      return ConnectionManager.getInstance().getDB(dbName);
    }
  }

  public void write(String message, String user, String room, String isSystem, String dbName) {
    write(message, user, room, isSystem, null, dbName);
  }

  public void write(String message, String user, String room, String isSystem, String options, String dbName) {
    save(message, user, room, isSystem, options, dbName);
  }

  public String save(String message, String user, String room, String isSystem, String options, String dbName) {
    String roomType = getTypeRoomChat(room, dbName);
    DBCollection coll = db(dbName).getCollection(M_ROOM_PREFIX + roomType);

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

    this.updateRoomTimestamp(room, dbName);

    return doc.get("_id").toString();
  }

  public void delete(String room, String user, String messageId, String dbName) {
    String roomType = getTypeRoomChat(room, dbName);
    DBCollection coll = db(dbName).getCollection(M_ROOM_PREFIX + roomType);
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

  public RoomBean getTeamRoomById(String roomId, String dbName) {
    if (roomId == null || roomId.isEmpty())
      return null;
    DBCollection cRooms = db(dbName).getCollection(M_ROOMS_COLLECTION);
    BasicDBObject qRoom = new BasicDBObject();
    qRoom.put("_id", roomId);
    qRoom.put("type", TYPE_ROOM_TEAM);
    DBObject dbRoom = cRooms.findOne(qRoom);
    if (dbRoom == null)
      return null;
    RoomBean room = new RoomBean();
    room.setRoom((String) dbRoom.get("_id"));
    room.setFullname((String) dbRoom.get("team"));
    room.setUser((String) dbRoom.get("user"));
    room.setType((String) dbRoom.get("type"));
    long timestamp = -1;
    if (dbRoom.containsField("timestamp")) {
      room.setTimestamp((Long) dbRoom.get("timestamp"));
    }
    return room;
  }

  public void deleteTeamRoom(String roomId, String user, String dbName) {
    RoomBean room = getTeamRoomById(roomId, dbName);
    if (room == null) {
      LOG.warning("No room with id [" + roomId + "] available to delete");
      return;
    }
    if (!room.getType().equals("t")) {
      LOG.warning("The room with id [" + roomId + "] is not a Team Room so it won't be deleted.");
      return;
    }
    LOG.info("Deleting Team Chat Room [" + room.getFullname() + "] (id:" + room.getRoom() + ")");
    // Check if the requester is the owner of the Team Chat Room
    if (user == null || room.getUser().equals(user) == false) {
      LOG.warning("The user [" + user + "] is not the owner of the room with id [" + roomId + "] so this room won't be deleted.");
      return;
    }

    // Delete all message of the Team Chat Room
    DBCollection cMessages = db(dbName).getCollection(M_ROOM_PREFIX + TYPE_ROOM_TEAM);
    BasicDBObject qMessages = new BasicDBObject();
    qMessages.put("roomId", roomId);
    cMessages.remove(qMessages, WriteConcern.ACKNOWLEDGED);
    LOG.info("Messages of room [" + roomId + "] deleted");

    // Remove the Team Chat Room from all the users
    List<String> users = userDataStorage.getUsersFilterBy(null, roomId, TYPE_ROOM_TEAM, dbName);
    userDataStorage.removeTeamUsers(roomId, users, dbName);
    LOG.info("All users removed from the team room [" + roomId + "]");

    // Delete the Team Chat Room
    DBCollection cRooms = db(dbName).getCollection(M_ROOMS_COLLECTION);
    BasicDBObject qRoom = new BasicDBObject();
    qRoom.put("_id", roomId);
    cRooms.remove(qRoom, WriteConcern.ACKNOWLEDGED);
    LOG.info("Team room [" + roomId + "] deleted");
  }

  public void edit(String room, String user, String messageId, String message, String dbName) {
    String roomType = getTypeRoomChat(room, dbName);
    DBCollection coll = db(dbName).getCollection(M_ROOM_PREFIX + roomType);

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

  public String read(String room, String dbName) {
    return read(room, false, null, null, dbName);
  }

  public String read(String room, boolean isTextOnly, Long fromTimestamp, String dbName) {
    return read(room, isTextOnly, fromTimestamp, null, dbName);
  }

  public String read(String room, boolean isTextOnly, Long fromTimestamp, Long toTimestamp, String dbName) {
    StringBuilder sb = new StringBuilder();

    SimpleDateFormat formatter = new SimpleDateFormat("hh:mm aaa");
    SimpleDateFormat formatterDate = new SimpleDateFormat("dd/MM/yyyy hh:mm aaa");
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    Date today = calendar.getTime();

    String roomType = getTypeRoomChat(room, dbName);
    DBCollection coll = db(dbName).getCollection(M_ROOM_PREFIX + roomType);

    BasicDBObject query = new BasicDBObject();
    query.put("roomId", room);
    long from = (fromTimestamp != null) ? fromTimestamp : System.currentTimeMillis() - readMillis;
    BasicDBObject tsobj = new BasicDBObject("$gt", from);
    if (toTimestamp != null) {
      tsobj.append("$lt", toTimestamp);
    }
    BasicDBObject ts = new BasicDBObject("timestamp", tsobj);
    BasicDBObject updts = new BasicDBObject("lastUpdatedTimestamp", tsobj);
    query.put("$or", new BasicDBObject[]{ts, updts});

    BasicDBObject sort = new BasicDBObject();
    sort.put("timestamp", -1);
    int limit = (isTextOnly) ? readTotalTxt : readTotalJson;
    DBCursor cursor = coll.find(query).sort(sort).limit(limit);
    if (!cursor.hasNext()) {
      if (isTextOnly)
        sb.append("no messages");
      else
        sb.append("{\"room\": \"").append(room).append("\",\"messages\": []}");
    } else {
      // Just being used as a local cache
      Map<String, UserBean> users = new HashMap<String, UserBean>();

      String timestamp, user, fullname, msgId, date;
      boolean first = true;

      while (cursor.hasNext()) {
        DBObject dbo = cursor.next();
        timestamp = dbo.get("timestamp").toString();
        if (first) //first element (most recent one)
        {
          if (!isTextOnly) {
            sb.append("{\"room\": \"").append(room).append("\",");
            sb.append("\"timestamp\": \"").append(timestamp).append("\",");
            sb.append("\"messages\": [");
          }
        }

        user = dbo.get("user").toString();
        msgId = dbo.get("_id").toString();

        UserBean userBean = users.get(user);
        if (userBean == null) {
          userBean = userDataStorage.getUser(user, dbName);
          users.put(user, userBean);
        }
        fullname = userBean.getFullname();

        date = "";
        try {
          Date date1 = new Date(Long.parseLong(timestamp));
          if (date1.before(today) || isTextOnly) {
            date = formatterDate.format(date1);
          } else {
            date = formatter.format(date1);
          }
        } catch (Exception e) {
          LOG.info("Message Date Format Error : " + e.getMessage());
        }

        if (isTextOnly) {
          StringBuilder line = new StringBuilder();
          line.append("[").append(date).append("] ");
          String message = dbo.get("message").toString();
          if (TYPE_DELETED.equals(message)) message = TYPE_DELETED;
          if ("true".equals(dbo.get("isSystem"))) {
            line.append("System Message: ");
            if (message.endsWith("<br/>")) message = message.substring(0, message.length() - 5);
            line.append(message).append("\n");
          } else {
            line.append(fullname).append(": ");
            message = message.replaceAll("<br/>", "\n");
            line.append(message).append("\n");
          }
          sb.insert(0, line);
        } else {
          if (!first) sb.append(",");
          MessageBean msg = new MessageBean();
          msg.setId(msgId);
          msg.setTimestamp(Long.parseLong(timestamp));
          if (dbo.containsField("lastUpdatedTimestamp")) {
            msg.setLastUpdatedTimestamp(Long.parseLong(dbo.get("lastUpdatedTimestamp").toString()));
          }
          msg.setUser(user);
          msg.setFullName(fullname);
          msg.setMessage(dbo.get("message").toString());
          if (dbo.containsField("options")) {
            JSONParser parser = new JSONParser();
            msg.setOptions(dbo.get("options").toString());
          }
          if (dbo.containsField("type")) {
            msg.setType(dbo.get("type").toString());
          }
          msg.setSystem(Boolean.parseBoolean(dbo.get("isSystem").toString()));
          sb.append(msg.toJSONObject().toJSONString());
        }

        first = false;
      }

      if (!isTextOnly) {
        sb.append("]}");
      }
    }

    return sb.toString();
  }

  public MessageBean getMessage(String roomId, String messageId, String dbName) {
    MessageBean message = null;

    String roomType = getTypeRoomChat(roomId, dbName);

    DBCollection coll = db(dbName).getCollection(M_ROOM_PREFIX + roomType);

    BasicDBObject query = new BasicDBObject();
    query.put("roomId", roomId);
    query.put("_id", new ObjectId(messageId));

    DBObject object = coll.findOne(query);
    if (object != null) {
      message = new MessageBean();
      message.setId(messageId);
      message.setUser(object.get("user").toString());
      message.setMessage(object.get("message").toString());
      message.setTimestamp(Long.parseLong(object.get("timestamp").toString()));
      message.setSystem(Boolean.parseBoolean(object.get("isSystem").toString()));
      if (object.containsField("options")) {
        JSONParser parser = new JSONParser();
        message.setOptions(object.get("options").toString());
      }
      if (object.containsField("type")) {
        message.setType(object.get("type").toString());
      }
    }

    return message;
  }

  private void updateRoomTimestamp(String room, String dbName) {
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext()) {
      DBObject dbo = cursor.next();
      dbo.put("timestamp", System.currentTimeMillis());
      coll.save(dbo, WriteConcern.UNACKNOWLEDGED);
    }

  }

  private void ensureIndexInRoom(String type, String dbName) {
    DBCollection coll = db(dbName).getCollection(M_ROOM_PREFIX + type);
    BasicDBObject doc = new BasicDBObject();
    doc.put("timestamp", System.currentTimeMillis());
    coll.insert(doc);
    ConnectionManager.getInstance().ensureIndexesInRoom(type);
    coll.remove(doc);
  }

  public String getSpaceRoom(String space, String dbName) {
    String room = ChatUtils.getRoomId(space);
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (!cursor.hasNext()) {
      try {
        basicDBObject.put("space", space);
        basicDBObject.put("type", TYPE_ROOM_SPACE);
        coll.insert(basicDBObject);
        ensureIndexInRoom(TYPE_ROOM_SPACE, dbName);
      } catch (MongoException me) {
        LOG.warning(me.getCode() + " : " + room + " : " + me.getMessage());
      }
    }

    return room;
  }

  public String getSpaceRoomByName(String name, String dbName) {
    String room = null;
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("shortName", name);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext()) {
      DBObject doc = cursor.next();
      room = doc.get("_id").toString();
    }

    return room;
  }

  public String getTeamRoom(String team, String user, String dbName) {
    String room = ChatUtils.getRoomId(team, user);
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (!cursor.hasNext()) {
      try {
        basicDBObject.put("team", team);
        basicDBObject.put("user", user);
        basicDBObject.put("type", TYPE_ROOM_TEAM);
        coll.insert(basicDBObject);
        ensureIndexInRoom(TYPE_ROOM_TEAM, dbName);
      } catch (MongoException me) {
        LOG.warning(me.getCode() + " : " + room + " : " + me.getMessage());
      }
    }

    return room;
  }

  public String getExternalRoom(String identifier, String dbName) {
    String room = ChatUtils.getExternalRoomId(identifier);
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (!cursor.hasNext()) {
      try {
        basicDBObject.put("identifier", identifier);
        basicDBObject.put("type", TYPE_ROOM_EXTERNAL);
        coll.insert(basicDBObject);
        ensureIndexInRoom(TYPE_ROOM_EXTERNAL, dbName);
      } catch (MongoException me) {
        LOG.warning(me.getCode() + " : " + room + " : " + me.getMessage());
      }
    }

    return room;
  }

  public String getTeamCreator(String room, String dbName) {
    if (room.indexOf(ChatService.TEAM_PREFIX) == 0) {
      room = room.substring(ChatService.TEAM_PREFIX.length());
    }
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);

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

  public void setRoomName(String room, String name, String dbName) {
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext()) {
      DBObject dbo = cursor.next();
      dbo.put("team", name);
      coll.save(dbo, WriteConcern.UNACKNOWLEDGED);
    }
  }

  public String getRoom(List<String> users, String dbName) {
    Collections.sort(users);
    String room = ChatUtils.getRoomId(users);
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (!cursor.hasNext()) {
      try {
        basicDBObject.put("users", users);
        basicDBObject.put("type", TYPE_ROOM_USER);
        coll.insert(basicDBObject);
        ensureIndexInRoom(TYPE_ROOM_USER, dbName);
      } catch (MongoException me) {
        LOG.warning(me.getCode() + " : " + room + " : " + me.getMessage());
      }
    }

    return room;
  }

  public String getTypeRoomChat(String roomId, String dbName) {
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("_id", roomId);
    DBCursor cursor = coll.find(query);
    Object roomType = null;
    while (cursor.hasNext()) {
      DBObject doc = cursor.next();
      roomType = doc.get("type");
    }
    return roomType.toString();
  }

  public List<RoomBean> getExistingRooms(String user, boolean withPublic, boolean isAdmin, NotificationService notificationService, TokenService tokenService, String dbName) {
    List<RoomBean> rooms = new ArrayList<RoomBean>();
    String roomId = null;
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);

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
        boolean isDemoUser = tokenService.isDemoUser(targetUser);
        if (!isAdmin || (isAdmin && ((!withPublic && !isDemoUser) || (withPublic && isDemoUser)))) {
          RoomBean roomBean = new RoomBean();
          roomBean.setRoom(roomId);
          roomBean.setUnreadTotal(notificationService.getUnreadNotificationsTotal(user, "chat", "room", roomId, dbName));
          roomBean.setUser(users.get(0));
          roomBean.setTimestamp(timestamp);
          roomBean.setType((String) dbo.get("type"));
          rooms.add(roomBean);
        }
      }
    }

    return rooms;
  }

  public RoomsBean getRooms(String user, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, int limit, NotificationService notificationService, TokenService tokenService, String dbName) {
    List<RoomBean> rooms = new ArrayList<RoomBean>();
    List<RoomBean> roomsOffline = new ArrayList<RoomBean>();
    UserBean userBean = userDataStorage.getUser(user, true, dbName);
    int unreadOffline = 0, unreadOnline = 0, unreadSpaces = 0, unreadTeams = 0;

    HashMap<String, UserBean> availableUsers = tokenService.getActiveUsersFilterBy(user, dbName, withUsers, withPublic, isAdmin, limit);

    rooms = this.getExistingRooms(user, withPublic, isAdmin, notificationService, tokenService, dbName);
    if (isAdmin)
      rooms.addAll(this.getExistingRooms(UserService.SUPPORT_USER, withPublic, isAdmin, notificationService, tokenService, dbName));

    for (RoomBean roomBean : rooms) {
      String targetUser = roomBean.getUser();
      roomBean.setFavorite(userBean.isFavorite(targetUser));

      if (availableUsers.keySet().contains(targetUser)) {
        UserBean targetUserBean = availableUsers.get(targetUser);
        roomBean.setFullname(targetUserBean.getFullname());
        roomBean.setStatus(targetUserBean.getStatus());
        roomBean.setAvailableUser(true);
        availableUsers.remove(targetUser);
        if (roomBean.getUnreadTotal() > 0)
          unreadOnline += roomBean.getUnreadTotal();
      } else {
        UserBean targetUserBean = userDataStorage.getUser(targetUser, dbName);
        roomBean.setFullname(targetUserBean.getFullname());
        roomBean.setAvailableUser(false);
        if (!withOffline)
          roomsOffline.add(roomBean);
        if (roomBean.getUnreadTotal() > 0)
          unreadOffline += roomBean.getUnreadTotal();

      }
    }

    if (withUsers) {
      if (!withOffline) {
        for (RoomBean roomBean : roomsOffline) {
          rooms.remove(roomBean);
        }
      }

      for (UserBean availableUser : availableUsers.values()) {
        RoomBean roomBean = new RoomBean();
        roomBean.setUser(availableUser.getName());
        roomBean.setFullname(availableUser.getFullname());
        roomBean.setStatus(availableUser.getStatus());
        roomBean.setAvailableUser(true);
        roomBean.setFavorite(userBean.isFavorite(roomBean.getUser()));
        roomBean.setType("u");
        String status = roomBean.getStatus();
        if (withOffline || (!withOffline && !UserMongoDataStorage.STATUS_INVISIBLE.equals(roomBean.getStatus()) && !UserMongoDataStorage.STATUS_OFFLINE.equals(roomBean.getStatus()))) {
          rooms.add(roomBean);
        }
      }
    } else {
      rooms = new ArrayList<RoomBean>();
    }

    List<SpaceBean> spaces = userDataStorage.getSpaces(user, dbName);
    for (SpaceBean space : spaces) {
      RoomBean roomBeanS = new RoomBean();
      roomBeanS.setUser(SPACE_PREFIX + space.getRoom());
      roomBeanS.setRoom(space.getRoom());
      roomBeanS.setFullname(space.getDisplayName());
      roomBeanS.setStatus(UserService.STATUS_SPACE);
      roomBeanS.setTimestamp(space.getTimestamp());
      roomBeanS.setAvailableUser(true);
      roomBeanS.setType("s");
      roomBeanS.setUnreadTotal(notificationService.getUnreadNotificationsTotal(user, "chat", "room", getSpaceRoom(SPACE_PREFIX + space.getRoom(), dbName), dbName));
      if (roomBeanS.getUnreadTotal() > 0)
        unreadSpaces += roomBeanS.getUnreadTotal();
      roomBeanS.setFavorite(userBean.isFavorite(roomBeanS.getUser()));
      if (withSpaces) {
        rooms.add(roomBeanS);
      }

    }

    List<RoomBean> teams = userDataStorage.getTeams(user, dbName);
    for (RoomBean team : teams) {
      RoomBean roomBeanS = new RoomBean();
      roomBeanS.setUser(TEAM_PREFIX + team.getRoom());
      roomBeanS.setRoom(team.getRoom());
      roomBeanS.setFullname(team.getFullname());
      roomBeanS.setStatus(UserService.STATUS_TEAM);
      roomBeanS.setTimestamp(team.getTimestamp());
      roomBeanS.setAvailableUser(true);
      roomBeanS.setType(team.getType());
      roomBeanS.setUnreadTotal(notificationService.getUnreadNotificationsTotal(user, "chat", "room", team.getRoom(), dbName));
      if (roomBeanS.getUnreadTotal() > 0)
        unreadTeams += roomBeanS.getUnreadTotal();
      roomBeanS.setFavorite(userBean.isFavorite(roomBeanS.getUser()));
      if (withSpaces) {
        rooms.add(roomBeanS);
      }

    }

    List<RoomBean> finalRooms = new ArrayList<RoomBean>();
    if (filter != null) {
      for (RoomBean roomBean : rooms) {
        String targetUser = roomBean.getFullname();
        if (filter(targetUser, filter))
          finalRooms.add(roomBean);
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

    return roomsBean;

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

  public int getNumberOfRooms(String dbName) {
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    DBCursor cursor = coll.find(query);
    return cursor.count();
  }

  public int getNumberOfMessages(String dbName) {
    int nb = 0;
    String[] roomTypes = {TYPE_ROOM_USER, TYPE_ROOM_SPACE, TYPE_ROOM_TEAM, TYPE_ROOM_EXTERNAL};
    for (String type : roomTypes) {
      DBCollection collr = db(dbName).getCollection(M_ROOM_PREFIX + type);
      BasicDBObject queryr = new BasicDBObject();
      DBCursor cursorr = collr.find(queryr);
      nb += cursorr.count();
    }

    return nb;
  }
}
