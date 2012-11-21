package org.benjp.services;

import com.mongodb.*;

import java.net.UnknownHostException;

public class MongoBootstrap {
  private static Mongo m;
  private static DB db;

  private static Mongo mongo()
  {
    if (m==null)
    {
      try {
        MongoOptions options = new MongoOptions();
        options.connectionsPerHost = 50;
        options.connectTimeout = 60000;
        options.threadsAllowedToBlockForConnectionMultiplier = 10;
        m = new Mongo(new ServerAddress("localhost", 27017), options);
        m.setWriteConcern(WriteConcern.SAFE);
      } catch (UnknownHostException e) {
      }
    }
    return m;
  }

  public static DB getDB() {
    if (db==null)
    {
      db = mongo().getDB("chat");
      //db.authenticate("admin", "".toCharArray());

      initCollection("notifications");
      initCollection("room_rooms");
      initCollection("sessions");
      initCollection("spaces");
      initCollection("users");

    }
    return db;
  }

  public static void initCappedCollection(String name, int size) {
    initCollection(name, true, size);
  }

  public static void initCollection(String name) {
    initCollection(name, false, 0);
  }

  private static void initCollection(String name, boolean isCapped, int size) {
    if (getDB().collectionExists(name)) return;

    BasicDBObject doc = new BasicDBObject();
    doc.put("capped", isCapped);
    if (isCapped)
      doc.put("size", size);
    getDB().createCollection(name, doc);

  }
}
