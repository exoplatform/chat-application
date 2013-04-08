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

package org.benjp.services;

import com.mongodb.*;
import org.benjp.utils.PropertyManager;

import java.net.UnknownHostException;
import java.util.logging.Logger;

public class MongoBootstrap
{
  private Mongo m;
  private DB db;
  private static Logger log = Logger.getLogger("MongoBootstrap");

  private Mongo mongo()
  {
    if (m==null)
    {
      try
      {
        MongoOptions options = new MongoOptions();
        options.connectionsPerHost = 200;
        options.connectTimeout = 60000;
        options.threadsAllowedToBlockForConnectionMultiplier = 10;
        String host = PropertyManager.getProperty(PropertyManager.PROPERTY_SERVER_HOST);
        int port = Integer.parseInt(PropertyManager.getProperty(PropertyManager.PROPERTY_SERVER_PORT));
        m = new Mongo(new ServerAddress(host, port), options);
        m.setWriteConcern(WriteConcern.SAFE);
      }
      catch (UnknownHostException e)
      {
      }
    }
    return m;
  }

  public void close() {
    try {
      if (m!=null)
        m.close();
    } catch (NullPointerException e) {}
  }

  public void initialize() {
    this.close();
    this.m = null;
    this.mongo();
  }

  public void dropDB(String dbName)
  {
    log.info("---- Dropping DB "+dbName);
    mongo().dropDatabase(dbName);
    log.info("-------- DB "+dbName+" dropped!");

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
      boolean authenticate = "true".equals(PropertyManager.getProperty(PropertyManager.PROPERTY_DB_AUTHENTICATION));
      if (authenticate)
      {
        db.authenticate(PropertyManager.getProperty(PropertyManager.PROPERTY_DB_USER), PropertyManager.getProperty(PropertyManager.PROPERTY_DB_PASSWORD).toCharArray());
      }
      initCollection("notifications");
      initCollection("room_rooms");
      initCollection("tokens");
      initCollection("spaces");
      initCollection("users");

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
    if (getDB().collectionExists(name)) return;

    BasicDBObject doc = new BasicDBObject();
    doc.put("capped", isCapped);
    if (isCapped)
      doc.put("size", size);
    getDB().createCollection(name, doc);

  }

  public void ensureIndexes()
  {
    log.info("### ensureIndexes in "+getDB().getName());
    BasicDBObject unique = new BasicDBObject();
    unique.put("unique", true);
    unique.put("background", true);
    BasicDBObject notUnique = new BasicDBObject();
    notUnique.put("unique", false);
    notUnique.put("background", true);

    DBCollection notifications = getDB().getCollection("notifications");
    notifications.dropIndexes();
    notifications.createIndex(new BasicDBObject("user", 1), notUnique.append("name", "user_1").append("ns", "chat.notifications"));
    notifications.createIndex(new BasicDBObject("isRead", 1), notUnique.append("name", "isRead_1").append("ns", "chat.notifications"));
    BasicDBObject index = new BasicDBObject();
    index.put("user", 1);
    index.put("categoryId", 1);
    index.put("category", 1);
    index.put("type", 1);
//    index.put("isRead", 1);
    notifications.createIndex(index, notUnique.append("name", "user_1_type_1_category_1_categoryId_1").append("ns", "chat.notifications"));
    log.info("### notifications indexes in "+getDB().getName());

    DBCollection rooms = getDB().getCollection("room_rooms");
    rooms.dropIndexes();
    rooms.createIndex(new BasicDBObject("space", 1), notUnique.append("name", "space_1").append("ns", "chat.room_rooms"));
    rooms.createIndex(new BasicDBObject("users", 1), notUnique.append("name", "users_1").append("ns", "chat.room_rooms"));
    log.info("### rooms indexes in "+getDB().getName());

    DBCollection tokens = getDB().getCollection("tokens");
    tokens.dropIndexes();
    tokens.createIndex(new BasicDBObject("token", 1), unique.append("name", "token_1").append("ns", "chat.tokens"));
    tokens.createIndex(new BasicDBObject("validity", -1), notUnique.append("name", "validity_m1").append("ns", "chat.tokens"));
    index = new BasicDBObject();
    index.put("user", 1);
    index.put("token", 1);
    tokens.createIndex(index, unique.append("name", "user_1_token_1").append("ns", "chat.tokens"));
    index = new BasicDBObject();
    index.put("validity", -1);
    index.put("isDemoUser", 1);
    tokens.createIndex(index, notUnique.append("name", "validity_1_isDemoUser_m1").append("ns", "chat.tokens"));
    log.info("### tokens indexes in "+getDB().getName());

    DBCollection users = getDB().getCollection("users");
    users.dropIndexes();
    users.createIndex(new BasicDBObject("user", 1), unique.append("name", "user_1").append("ns", "chat.users"));
    users.createIndex(new BasicDBObject("spaces", 1), notUnique.append("name", "spaces_1").append("ns", "chat.users"));
    log.info("### users indexes in "+getDB().getName());

    log.info("### Indexes creation completed in "+getDB().getName());

  }
}
