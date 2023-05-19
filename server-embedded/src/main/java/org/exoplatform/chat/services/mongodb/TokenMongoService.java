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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.model.UserBean;
import org.exoplatform.chat.services.TokenStorage;
import org.exoplatform.chat.utils.MessageDigester;
import org.exoplatform.chat.utils.PropertyManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.exoplatform.chat.services.TokenService.ANONIM_USER;

@Named("tokenStorage")
@ApplicationScoped
@Singleton
public class TokenMongoService implements TokenStorage
{
  public static final String M_USERS_COLLECTION = "users";
  public static final String IS_DEMO_USER = "isDemoUser";
  public static final String TOKEN = "token";
  public static final String USER = "user";
  public static final String ID = "_id";
  public static final String FULLNAME = "fullname";
  public static final String STATUS = "status";
  public static final String IS_ENABLED = "isEnabled";
  public static final String IS_DELETED = "isDeleted";
  public static final String IS_EXTERNAL = "isExternal";
  public static final String TRUE = "true";

  private MongoDatabase db()
  {
    return ConnectionManager.getInstance().getDB();
  }

  public String getToken(String user)
  {
    String passphrase = PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE);
    String in = user+passphrase;
    return MessageDigester.getHash(in);
  }

  public boolean hasUserWithToken(String user, String token)
  {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    Bson query = Filters.and(Filters.eq(USER, user), Filters.eq(TOKEN, token));
    return coll.find(query).cursor().hasNext();
  }

  public void addUser(String user, String token)
  {
    if (!hasUserWithToken(user, token)) {
      MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);

      BasicDBObject query = new BasicDBObject();
      query.put(USER, user);
      try (MongoCursor<Document> cursor = coll.find(query).cursor()) {
        if (cursor.hasNext()) {
          Document updateDocument = new Document();
          updateDocument.put(TOKEN, token);
          updateDocument.put(IS_DEMO_USER, user.startsWith(ANONIM_USER));
          coll.updateOne(query, updateDocument);
        } else {
          Document doc = new Document();
          doc.put(ID, user);
          doc.put(USER, user);
          doc.put(TOKEN, token);
          doc.put(IS_DEMO_USER, user.startsWith(ANONIM_USER));
          doc.put(IS_ENABLED, Boolean.TRUE.toString());
          doc.put(IS_DELETED, Boolean.FALSE.toString());
          coll.insertOne(doc);
        }
      }
    }
  }

  public void removeUserToken(String user, String token) {
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    Bson query = Filters.and(Filters.eq(USER, user), Filters.eq(TOKEN, token));
    Document tokenUpdate = new Document().append(TOKEN, "");
    coll.updateOne(query, tokenUpdate);
  }

  public Map<String, UserBean> getActiveUsersFilterBy(String user, List<String> limitedFilter, boolean withUsers, boolean withPublic, boolean isAdmin, int limit)
  {
    Bson query = new Document();
    if (isAdmin) {
      if (withPublic && !withUsers) {
        query = Filters.eq(IS_DEMO_USER, true);
      } else if (!withPublic && withUsers) {
        query = Filters.eq(IS_DEMO_USER, false);
      }
    } else {
      query = Filters.eq(IS_DEMO_USER, user.startsWith(ANONIM_USER));
    }
    query = Filters.and(query, Filters.in(USER, new BasicDBObject("$in", limitedFilter)));
    if (limit < 0) limit = 0;

    HashMap<String, UserBean> users = new HashMap<>();
    MongoCollection<Document> coll = db().getCollection(M_USERS_COLLECTION);
    try (MongoCursor<Document> cursor = coll.find(query).limit(limit).cursor()) {
      while (cursor.hasNext()) {
        Document doc = cursor.next();
        String target = doc.get(USER).toString();
        // Exclude current user and offline users
        if (!user.equals(target)) {
          UserBean userBean = new UserBean();
          userBean.setName(target);
          if (doc.get(FULLNAME) != null) {
            userBean.setFullname(doc.get(FULLNAME).toString());
          }
          if (doc.get(STATUS) != null) {
            userBean.setStatus(doc.get(STATUS).toString());
          }
          if (doc.get(IS_ENABLED) != null) {
            userBean.setEnabled(StringUtils.equals(doc.get(IS_ENABLED).toString(), TRUE));
          }
          if (doc.get(IS_DELETED) != null) {
            userBean.setDeleted(StringUtils.equals(doc.get(IS_DELETED).toString(), TRUE));
          }
          if (doc.get(IS_EXTERNAL) != null) {
            userBean.setExternal(doc.get(IS_EXTERNAL).toString());
          }
          if (Boolean.TRUE.equals(userBean.isEnabled())) {
            users.put(target, userBean);
          }
        }
      }
    }

    return users;
  }
}
