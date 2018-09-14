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

import org.apache.commons.lang3.StringUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.services.SettingDataStorage;

@Named("settingStorage")
@ApplicationScoped
@Singleton
public class SettingMongoDataStorage implements SettingDataStorage {

  private static final Logger LOG = Logger.getLogger(SettingMongoDataStorage.class.getName());

  public static final String M_SETTINGS_COLLECTION = "settings";

  private DB db(String dbName)
  {
    if (StringUtils.isEmpty(dbName)) {
      return ConnectionManager.getInstance().getDB();
    } else {
      return ConnectionManager.getInstance().getDB(dbName);
    }
  }

  @Override
  public String getSetting(String name, String dbName) {
    DBCollection coll = db(dbName).getCollection(M_SETTINGS_COLLECTION);

    BasicDBObject query = new BasicDBObject();
    query.put("name", name);

    DBCursor dbCursor = coll.find(query);
    if(dbCursor.hasNext()) {
      return (String) dbCursor.next().get("value");
    }
    return null;
  }

  @Override
  public void setSetting(String name, String value, String dbName) {
    DBCollection settingsCol = db(dbName).getCollection("settings");

    BasicDBObject query = new BasicDBObject();
    query.put("name", name);

    BasicDBObject newStatusDBObject = new BasicDBObject();
    newStatusDBObject.put("name", name);
    newStatusDBObject.put("value", value);

    settingsCol.update(query, newStatusDBObject, true, false);
  }
}
