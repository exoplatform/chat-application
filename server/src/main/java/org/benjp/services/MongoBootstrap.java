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

public class MongoBootstrap
{
  private static Mongo m;
  private static DB db;

  private static Mongo mongo()
  {
    if (m==null)
    {
      try
      {
        MongoOptions options = new MongoOptions();
        options.connectionsPerHost = 50;
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

  public static DB getDB()
  {
    if (db==null)
    {
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

      ensureIndexes();

    }
    return db;
  }

  public static void initCappedCollection(String name, int size)
  {
    initCollection(name, true, size);
  }

  public static void initCollection(String name)
  {
    initCollection(name, false, 0);
  }

  private static void initCollection(String name, boolean isCapped, int size)
  {
    if (getDB().collectionExists(name)) return;

    BasicDBObject doc = new BasicDBObject();
    doc.put("capped", isCapped);
    if (isCapped)
      doc.put("size", size);
    getDB().createCollection(name, doc);

  }

  private static void ensureIndexes()
  {
    DBCollection notifications = getDB().getCollection("notifications");
    notifications.ensureIndex("user");
    notifications.ensureIndex("type");
    notifications.ensureIndex("category");
    notifications.ensureIndex("categoryId");
    notifications.ensureIndex("isRead");

    DBCollection rooms = getDB().getCollection("room_rooms");
    rooms.ensureIndex("space");
    rooms.ensureIndex("users");

    DBCollection tokens = getDB().getCollection("tokens");
    tokens.ensureIndex("user");
    tokens.ensureIndex("token");
    tokens.ensureIndex("validity");

    DBCollection users = getDB().getCollection("users");
    users.ensureIndex("user");
    users.ensureIndex("spaces");

    DBCollection spaces = getDB().getCollection("spaces");
    spaces.ensureIndex("user");

  }
}
