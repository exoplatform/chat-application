/*
 * Copyright (C) 2018 eXo Platform SAS.
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

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.inject.Singleton;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;

import com.mongodb.BasicDBObject;

import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.services.SettingDataStorage;

@Named("settingStorage")
@ApplicationScoped
@Singleton
public class SettingMongoDataStorage implements SettingDataStorage {

  private static final Logger LOG = Logger.getLogger(SettingMongoDataStorage.class.getName());

  public static final String M_SETTINGS_COLLECTION = "settings";
  public static final String NAME = "name";
  public static final String VALUE = "value";

  private MongoDatabase db()
  {
    return ConnectionManager.getInstance().getDB();
  }

  @Override
  public String getSetting(String name) {
    MongoCollection<Document> coll = db().getCollection(M_SETTINGS_COLLECTION);

    BasicDBObject query = new BasicDBObject();
    query.put(NAME, name);

    try(MongoCursor<Document> dbCursor = coll.find(query).cursor()) {
      if (dbCursor.hasNext()) {
        return (String) dbCursor.next().get(VALUE);
      }
    }
    return null;
  }

  @Override
  public void setSetting(String name, String value) {
    MongoCollection<Document> settingsCol = db().getCollection(M_SETTINGS_COLLECTION);

    Bson query = Filters.eq(NAME, name);

    Bson newStatusDBObject = Updates.combine(Updates.set(NAME, name), Updates.set(VALUE, value));

    UpdateOptions options = new UpdateOptions().upsert(true);
    settingsCol.updateOne(query, newStatusDBObject, options);
  }
}
