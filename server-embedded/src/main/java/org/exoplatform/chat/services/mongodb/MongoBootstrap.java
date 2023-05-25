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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.mongodb.*;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.utils.PropertyManager;



import static org.exoplatform.chat.services.mongodb.utils.ConnectionHelper.getMongoServerAdresses;

public class MongoBootstrap
{
  private MongoClient m;
  private MongoDatabase db;

  private static final Logger LOG = Logger.getLogger("MongoBootstrap");
  private MongoClient mongo()
  {
    if (m==null)
    {
      try
      {
        StringBuilder connectionString = new StringBuilder().append(getMongoServerAdresses().stream().map(s -> s.getHost() + ":" + s.getPort()).collect(Collectors.joining(",")))
                .append("/?")
                .append("maxPoolSize=200")
                .append("&")
                .append("minPoolSize=10")
                .append("&")
                .append("connectTimeoutMS=60000")
                .append("&")
                .append("w=majority");

        boolean authenticate = "true".equals(PropertyManager.getProperty(PropertyManager.PROPERTY_DB_AUTHENTICATION)) 
                && PropertyManager.getProperty(PropertyManager.PROPERTY_DB_PASSWORD) != null 
                && !PropertyManager.getProperty(PropertyManager.PROPERTY_DB_PASSWORD).isEmpty();
        if (authenticate) {
          connectionString = new StringBuilder("mongodb://").append(PropertyManager.getProperty(PropertyManager.PROPERTY_DB_USER))
                  .append(":")
                  .append(URLEncoder.encode(PropertyManager.getProperty(PropertyManager.PROPERTY_DB_PASSWORD), StandardCharsets.UTF_8))
                  .append("@").append(connectionString)
                  .append("&authSource=")
                  .append(PropertyManager.getProperty(PropertyManager.PROPERTY_DB_NAME));
        } else {
          connectionString = new StringBuilder("mongodb://")
                  .append(connectionString);
        }
        if (PropertyManager.PROPERTY_SERVER_TYPE_EMBED.equals(PropertyManager.getProperty(PropertyManager.PROPERTY_SERVER_TYPE)))
        {
          LOG.warning("WE WILL NOW USE MONGODB IN EMBED MODE...");
          LOG.warning("BE AWARE...");
          LOG.warning("EMBED MODE SHOULD NEVER BE USED IN PRODUCTION!");
        }
        m = MongoClients.create(connectionString.toString());
      }
      catch (Exception e)
      {
        LOG.log(Level.SEVERE, "Error occur when get MongoDB server addresses .", e);
      }
    }
    return m;
  }

  public MongoDatabase getDB()
  {
    return getDB(null);
  }

  public MongoDatabase getDB(String dbName)
  {
    if (db==null || dbName!=null)
    {
      if (dbName!=null)
        db = mongo().getDatabase(dbName);
      else
        db = mongo().getDatabase(PropertyManager.getProperty(PropertyManager.PROPERTY_DB_NAME));

      initCollection("notifications");
      initCollection(ChatMongoDataStorage.M_ROOMS_COLLECTION);
      initCollection("users");
      dropTokenCollectionIfExists();

    }
    return db;
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
   try {
     BasicDBObject doc = new BasicDBObject();
     doc.put("capped", isCapped);
     if (isCapped)
       doc.put("size", size);
     getDB().createCollection(name);
   } catch (Exception e) {
     // Nothing to do, collection already exists
   }

  }

  private void dropTokenCollectionIfExists()
  {
    getDB().getCollection("tokens").drop();
  }

  public void ensureIndexesInRoom(String type) {
    IndexOptions notUnique = new IndexOptions();
    notUnique.unique(false);
    notUnique.background(true);
    notUnique.name("roomId_1_timestamp_-1");

    MongoCollection<Document> roomsCollection = getDB().getCollection(ChatMongoDataStorage.M_ROOM_PREFIX + type);
    roomsCollection.createIndex(Indexes.compoundIndex(Indexes.ascending("roomId"), Indexes.descending("timestamp")), notUnique);
    LOG.info("##### room index in "+ChatMongoDataStorage.M_ROOM_PREFIX + type);
  }

  public void ensureIndexes()
  {
    String dbName = this.getDB().getName();
    LOG.info("### ensureIndexes in " + dbName);
    IndexOptions unique = new IndexOptions();
    unique.unique(true);
    unique.background(true);
    IndexOptions notUnique = new IndexOptions();
    notUnique.unique(false);
    notUnique.background(true);

    MongoCollection<Document> notifications = getDB().getCollection("notifications");
    notifications.dropIndexes();
    notifications.createIndex(Indexes.ascending("user"), notUnique.name("user_1"));

    notifications.createIndex(Indexes.ascending("isRead"), notUnique.name("isRead_1"));
    BasicDBObject index = new BasicDBObject();
    index.put("user", 1);
    index.put("categoryId", 1);
    index.put("category", 1);
    index.put("type", 1);
    notifications.createIndex(index, notUnique.name("user_1_type_1_category_1_categoryId_1"));
    LOG.info("### notifications indexes in "+getDB().getName());

    MongoCollection<Document> rooms = getDB().getCollection(ChatMongoDataStorage.M_ROOMS_COLLECTION);
    rooms.dropIndexes();
    rooms.createIndex(Indexes.ascending("space"), notUnique.name("space_1"));
    rooms.createIndex(Indexes.ascending("users"), notUnique.name("users_1"));
    rooms.createIndex(Indexes.ascending("shortName"), notUnique.name("shortName_1"));
    LOG.info("### rooms indexes in "+getDB().getName());

    String[] roomTypes = {ChatService.TYPE_ROOM_USER, ChatService.TYPE_ROOM_SPACE, ChatService.TYPE_ROOM_TEAM, ChatService.TYPE_ROOM_EXTERNAL};
    for (String type : roomTypes) {
      MongoCollection<Document> roomCollection = getDB().getCollection(ChatMongoDataStorage.M_ROOM_PREFIX + type);
      roomCollection.createIndex(Indexes.compoundIndex(Indexes.ascending("roomId"), Indexes.descending("timestamp")), notUnique.name("roomId_1_timestamp_-1"));
      LOG.info("##### room index in " + type);
    }

    MongoCollection<Document> users = getDB().getCollection("users");
    users.dropIndexes();
    users.createIndex(Indexes.ascending("token"), notUnique.name("token_1"));
    users.createIndex(Indexes.compoundIndex(Indexes.ascending("user"), Indexes.ascending("token")), unique.name("user_1_token_1"));
    index = new BasicDBObject();
    index.put("user", 1);
    index.put("validity", -1);
    users.createIndex(Indexes.compoundIndex(Indexes.ascending("user"), Indexes.descending("validity")), unique.name("user_1_validity_m1"));

    users.createIndex(Indexes.ascending("user"), unique.name("user_1"));
    users.createIndex(Indexes.ascending("spaces"), notUnique.name("spaces_1"));
    LOG.info("### users indexes in " + getDB().getName());

    LOG.info("### Indexes creation completed in " + getDB().getName());
  }
}
