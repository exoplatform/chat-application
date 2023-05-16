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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mongodb.*;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ConnectionPoolSettings;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.mongodb.utils.ConnectionHelper;
import org.exoplatform.chat.utils.PropertyManager;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

public class MongoBootstrap
{
  private static MongodExecutable mongodExe;
  private static MongodProcess mongod;
  private MongoClient m;
  private MongoDatabase db;
  private static final Logger LOG = Logger.getLogger("MongoBootstrap");

  private MongoClient mongo()
  {
    if (m==null)
    {
      try
      {
        if (PropertyManager.PROPERTY_SERVER_TYPE_EMBED.equals(PropertyManager.getProperty(PropertyManager.PROPERTY_SERVER_TYPE)))
        {
          LOG.warning("WE WILL NOW USE MONGODB IN EMBED MODE...");
          LOG.warning("BE AWARE...");
          LOG.warning("EMBED MODE SHOULD NEVER BE USED IN PRODUCTION!");
          setupEmbedMongo();
        }

        MongoClientSettings settings = MongoClientSettings.builder().applyToConnectionPoolSettings((ConnectionPoolSettings.builder()
                .maxSize(200)
                .maxConnectionIdleTime(60000, TimeUnit.SECONDS).build())).builder().build();
        boolean authenticate = "true".equals(PropertyManager.getProperty(PropertyManager.PROPERTY_DB_AUTHENTICATION));
        if (authenticate) {
          MongoCredential credential = MongoCredential.createCredential(
              PropertyManager.getProperty(PropertyManager.PROPERTY_DB_USER),
              PropertyManager.getProperty(PropertyManager.PROPERTY_DB_NAME),
              PropertyManager.getProperty(PropertyManager.PROPERTY_DB_PASSWORD).toCharArray());
          m = new MongoClient(ConnectionHelper.getMongoServerAdresses(), Arrays.asList(credential), options);

        } else {
          m = new MongoClient(ConnectionHelper.getMongoServerAdresses(), options);
        }
        m.setWriteConcern(WriteConcern.SAFE);
      }
      catch (Exception e)
      {
        LOG.log(Level.SEVERE, "Error occur when get Mongo server adresses .", e);
      }
    }
    return m;
  }

  public void close() {
    try {
      if (mongod != null) {
        mongod.stop();
        mongodExe.stop();
      }
      if (m!=null)
        m.close();
    } catch (NullPointerException e) {
      return;
    }
  }

  public void initialize() {
    this.close();
    this.m = null;
    this.mongo();
  }

  public void dropDB(String dbName)
  {
    LOG.info("---- Dropping DB " + dbName);
    mongo().dropDatabase(dbName);
    LOG.info("-------- DB " + dbName + " dropped!");

  }

  public DB getDB()
  {
    return getDB(null);
  }

  public DB getDB(String dbName)
  {
    if (db==null || dbName!=null)
    {
      if (dbName!=null)
        db = mongo().getDB(dbName);
      else
        db = mongo().getDB(PropertyManager.getProperty(PropertyManager.PROPERTY_DB_NAME));

      initCollection("notifications");
      initCollection(ChatMongoDataStorage.M_ROOMS_COLLECTION);
      initCollection("users");
      dropTokenCollectionIfExists();

    }
    return db;
  }

  private static void setupEmbedMongo() throws Exception {
    MongodStarter runtime = MongodStarter.getDefaultInstance();
    List<ServerAddress> mongoServerAdresses = ConnectionHelper.getMongoServerAdresses();
    if(mongoServerAdresses == null || mongoServerAdresses.isEmpty()) {
      throw new Exception("No mongodb server host and port defined");
    } else if(mongoServerAdresses.size() > 1) {
      throw new Exception("Several mongodb server host and port defined, embedded mode supports only one mongodb server");
    }
    ServerAddress mongdbServer = mongoServerAdresses.get(0);
    IMongodConfig mongodConfig = new MongodConfigBuilder()
            .version(Version.V3_6_0)
            .net(new Net(mongdbServer.getPort(), Network.localhostIsIPv6()))
            .build();
    mongodExe = runtime.prepare(mongodConfig);
    mongod = mongodExe.start();
  }

  public void initCappedCollection(String name, int size)
  {
    initCollection(name, true, size);
  }

  private void initCollection(String name)
  {
    initCollection(name, false, 0);
  }

  private void initCollection(String name, boolean isCapped, int size)
  {
    if (getDB().collectionExists(name)) return;

    BasicDBObject doc = new BasicDBObject();
    doc.put("capped", isCapped);
    if (isCapped)
      doc.put("size", size);
    getDB().createCollection(name, doc);

  }

  private void dropTokenCollectionIfExists()
  {
    if (getDB().collectionExists("tokens")) {
      DBCollection tokens = getDB().getCollection("tokens");
      tokens.drop();
    }
  }

  public void ensureIndexesInRoom(String type)
  {
    String dbName = this.getDB().getName();
    BasicDBObject unique = new BasicDBObject();
    unique.put("unique", true);
    unique.put("background", true);
    BasicDBObject notUnique = new BasicDBObject();
    notUnique.put("unique", false);
    notUnique.put("background", true);

    DBCollection collr = getDB().getCollection(ChatMongoDataStorage.M_ROOM_PREFIX+type);
    collr.createIndex(new BasicDBObject("roomId", 1).append("timestamp", -1), notUnique.append("name", "roomId_1_timestamp_-1").append("ns", dbName+"."+ChatMongoDataStorage.M_ROOM_PREFIX+type));
    LOG.info("##### room index in "+ChatMongoDataStorage.M_ROOM_PREFIX+type);
  }

  public void ensureIndexes()
  {
    String dbName = this.getDB().getName();
    LOG.info("### ensureIndexes in "+dbName);
    BasicDBObject unique = new BasicDBObject();
    unique.put("unique", true);
    unique.put("background", true);
    BasicDBObject notUnique = new BasicDBObject();
    notUnique.put("unique", false);
    notUnique.put("background", true);

    DBCollection notifications = getDB().getCollection("notifications");
    notifications.dropIndexes();
    notifications.createIndex(new BasicDBObject("user", 1), notUnique.append("name", "user_1").append("ns", dbName + ".notifications"));
    notifications.createIndex(new BasicDBObject("isRead", 1), notUnique.append("name", "isRead_1").append("ns", dbName + ".notifications"));
    BasicDBObject index = new BasicDBObject();
    index.put("user", 1);
    index.put("categoryId", 1);
    index.put("category", 1);
    index.put("type", 1);
    // index.put("isRead", 1);
    notifications.createIndex(index, notUnique.append("name", "user_1_type_1_category_1_categoryId_1").append("ns", dbName + ".notifications"));
    LOG.info("### notifications indexes in "+getDB().getName());

    DBCollection rooms = getDB().getCollection(ChatMongoDataStorage.M_ROOMS_COLLECTION);
    rooms.dropIndexes();
    rooms.createIndex(new BasicDBObject("space", 1), notUnique.append("name", "space_1").append("ns", dbName + "." + ChatMongoDataStorage.M_ROOMS_COLLECTION));
    rooms.createIndex(new BasicDBObject("users", 1), notUnique.append("name", "users_1").append("ns", dbName + "." + ChatMongoDataStorage.M_ROOMS_COLLECTION));
    rooms.createIndex(new BasicDBObject("shortName", 1), notUnique.append("name", "shortName_1").append("ns", dbName + "." + ChatMongoDataStorage.M_ROOMS_COLLECTION));
    LOG.info("### rooms indexes in "+getDB().getName());

    String[] roomTypes = {ChatService.TYPE_ROOM_USER, ChatService.TYPE_ROOM_SPACE, ChatService.TYPE_ROOM_TEAM, ChatService.TYPE_ROOM_EXTERNAL};
    for (String type : roomTypes) {
      DBCollection collr = getDB().getCollection(ChatMongoDataStorage.M_ROOM_PREFIX+type);
      collr.createIndex(new BasicDBObject("roomId", 1).append("timestamp", -1), notUnique.append("name", "roomId_1_timestamp_-1").append("ns", dbName+"."+ChatMongoDataStorage.M_ROOM_PREFIX+type));
      LOG.info("##### room index in "+type);
    }

    DBCollection users = getDB().getCollection("users");
    users.dropIndexes();
    users.createIndex(new BasicDBObject("token", 1), notUnique.append("name", "token_1").append("ns", dbName + ".users"));
    index = new BasicDBObject();
    index.put("user", 1);
    index.put("token", 1);
    users.createIndex(index, unique.append("name", "user_1_token_1").append("ns", dbName + ".users"));
    index = new BasicDBObject();
    index.put("user", 1);
    index.put("validity", -1);
    users.createIndex(index, unique.append("name", "user_1_validity_m1").append("ns", dbName + ".users"));

    users.createIndex(new BasicDBObject("user", 1), unique.append("name", "user_1").append("ns", dbName+".users"));
    users.createIndex(new BasicDBObject("spaces", 1), notUnique.append("name", "spaces_1").append("ns", dbName+".users"));
    LOG.info("### users indexes in "+getDB().getName());

    LOG.info("### Indexes creation completed in "+getDB().getName());
  }
}
