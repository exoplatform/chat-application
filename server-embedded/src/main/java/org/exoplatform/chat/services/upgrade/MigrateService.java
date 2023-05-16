package org.exoplatform.chat.services.upgrade;

import java.util.regex.Pattern;

import org.exoplatform.chat.services.mongodb.ChatMongoDataStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.chat.services.mongodb.MongoBootstrap;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class MigrateService {

  private static final Log LOG = ExoLogger.getLogger(MigrateService.class);

  private DB db;

  public MigrateService() {
    db = (new MongoBootstrap()).getDB();
  }

  public void migrate() {
    // Detect if migration has been done before by checking if rooms "room_{roomId}" exist
    DBCollection namespacesCol = db.getCollection("system.namespaces");
    BasicDBObject getRoomNamespaces = new BasicDBObject();
    Pattern regex = Pattern.compile("^"+db.getName()+".room_");
    getRoomNamespaces.append("name", regex);
    DBCursor roomNamespaces = namespacesCol.find(getRoomNamespaces);
    if (roomNamespaces.count() > 0) {
      String roomTypes[] = {"u", "s", "t", "e"};
      for (String type : roomTypes) {
        migrateRoom(type);
      }
  
      if (db.collectionExists("room_rooms")) {
        DBCollection roomsCol = db.getCollection("room_rooms");
        if (!db.collectionExists(ChatMongoDataStorage.M_ROOMS_COLLECTION)) {
          roomsCol.rename(ChatMongoDataStorage.M_ROOMS_COLLECTION);
        } else {
          DBCollection newRoomsCol = db.getCollection(ChatMongoDataStorage.M_ROOMS_COLLECTION);
          DBCursor rooms = roomsCol.find();
          while (rooms.hasNext()) {
            DBObject room = rooms.next();
            newRoomsCol.insert(room);
          }
          roomsCol.drop();
        }
        LOG.info("Finished to migrate room_rooms collection");
      }
    }
  }

  private void migrateRoom(String roomType) {
    if (!db.collectionExists(ChatMongoDataStorage.M_ROOM_PREFIX+roomType)) {
      db.createCollection(ChatMongoDataStorage.M_ROOM_PREFIX+roomType, null);
    }

    DBCollection roomsCol = db.getCollection("room_rooms");
    BasicDBObject findRoomsByType = new BasicDBObject();
    findRoomsByType.put("type", roomType);
    DBCursor cursor = roomsCol.find(findRoomsByType);
    while (cursor.hasNext()) {
      DBObject dbo = cursor.next();
      String roomId = dbo.get("_id").toString();
      String roomName = "room_" + roomId;
      if (db.collectionExists(roomName)) {
        DBCollection roomCol = db.getCollection(roomName);

        // Add roomId field to all messages of a room 
        BasicDBObject addRoomIdToMessages = new BasicDBObject();
        addRoomIdToMessages.append("$set", new BasicDBObject().append("roomId", roomId));
        roomCol.updateMulti(new BasicDBObject(), addRoomIdToMessages);

        // Move all message of a room to messages_room_{roomType} collection
        DBCursor allMessages = roomCol.find();
        DBCollection newRoomCol = db.getCollection(ChatMongoDataStorage.M_ROOM_PREFIX+roomType);
        while (allMessages.hasNext()) {
          DBObject message = allMessages.next();
          message.removeField("time");
          newRoomCol.insert(message);
        }

        // Drop migrated room
        roomCol.drop();
      }
    }
    LOG.info("Finished to migrate rooms with type : {}", roomType);
  }
}
