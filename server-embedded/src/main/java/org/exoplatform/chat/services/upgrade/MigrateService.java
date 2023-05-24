package org.exoplatform.chat.services.upgrade;

import java.util.ArrayList;
import java.util.regex.Pattern;

import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.exoplatform.chat.services.mongodb.ChatMongoDataStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.chat.services.mongodb.MongoBootstrap;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class MigrateService {

  private static final Log LOG = ExoLogger.getLogger(MigrateService.class);
  public static final String ROOM_ROOMS = "room_rooms";

  private MongoDatabase db;

  public MigrateService() {
    db = (new MongoBootstrap()).getDB();
  }

  public void migrate() {
    // Detect if migration has been done before by checking if rooms "room_{roomId}" exist
    MongoCollection<Document> namespacesCol = db.getCollection("system.namespaces");
    BasicDBObject getRoomNamespaces = new BasicDBObject();
    Pattern regex = Pattern.compile("^"+db.getName()+".room_");
    getRoomNamespaces.append("name", regex);
    MongoCursor<Document> roomNamespaces = namespacesCol.find(getRoomNamespaces).cursor();
    if (roomNamespaces.available() > 0) {
      String[] roomTypes = {"u", "s", "t", "e"};
      for (String type : roomTypes) {
        migrateRoom(type);
      }
  
      if (db.listCollectionNames().into(new ArrayList<>()).contains(ROOM_ROOMS)) {
        MongoCollection<Document> roomsCol = db.getCollection(ROOM_ROOMS);
        if (!db.listCollectionNames().into(new ArrayList<>()).contains(ChatMongoDataStorage.M_ROOMS_COLLECTION)) {
          roomsCol.renameCollection(new MongoNamespace(db.getName(), ChatMongoDataStorage.M_ROOMS_COLLECTION));
        } else {
          MongoCollection<Document> newRoomsCol = db.getCollection(ChatMongoDataStorage.M_ROOMS_COLLECTION);
          MongoCursor<Document> rooms = roomsCol.find().cursor();
          while (rooms.hasNext()) {
            Document room = rooms.next();
            newRoomsCol.insertOne(room);
          }
          roomsCol.drop();
        }
        LOG.info("Finished to migrate room_rooms collection");
      }
    }
  }

  private void migrateRoom(String roomType) {
    if (!db.listCollectionNames().into(new ArrayList<>()).contains(ChatMongoDataStorage.M_ROOM_PREFIX+roomType)) {
      db.createCollection(ChatMongoDataStorage.M_ROOM_PREFIX+roomType, null);
    }

    MongoCollection<Document> roomsCol = db.getCollection(ROOM_ROOMS);
    Bson findRoomsByType = Filters.eq("type", roomType);
    MongoCursor<Document> cursor = roomsCol.find(findRoomsByType).cursor();
    while (cursor.hasNext()) {
      Document dbo = cursor.next();
      String roomId = dbo.get("_id").toString();
      String roomName = "room_" + roomId;
      if (db.listCollectionNames().into(new ArrayList<>()).contains(roomName)) {
        MongoCollection<Document> roomCol = db.getCollection(roomName);

        // Add roomId field to all messages of a room 
        Document addRoomIdToMessages = new Document().append("roomId", roomId);
        roomCol.updateMany(new BasicDBObject(), addRoomIdToMessages);

        // Move all message of a room to messages_room_{roomType} collection
        MongoCursor<Document> allMessages = roomCol.find().cursor();
        MongoCollection<Document> newRoomCol = db.getCollection(ChatMongoDataStorage.M_ROOM_PREFIX + roomType);
        while (allMessages.hasNext()) {
          Document message = allMessages.next();
          message.remove("time");
          newRoomCol.insertOne(message);
        }

        // Drop migrated room
        roomCol.drop();
      }
    }
    LOG.info("Finished to migrate rooms with type : {}", roomType);
  }
}
