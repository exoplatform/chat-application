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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.model.UserBean;
import org.exoplatform.chat.services.TokenService;
import org.exoplatform.chat.utils.MessageDigester;
import org.exoplatform.chat.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.user.UserStateService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named("tokenService")
@ApplicationScoped
public class TokenServiceImpl implements TokenService
{
  //TODO this service will not be available in 2 mode servers, we will need to find another solution (REST API /state ?)
  private UserStateService userStateService;

  public TokenServiceImpl() {
    userStateService = PortalContainer.getInstance().getComponentInstanceOfType(UserStateService.class);
  }

  public TokenServiceImpl(UserStateService userStateService) {
    this.userStateService = userStateService;
  }

  private DB db(String dbName)
  {
    if (StringUtils.isEmpty(dbName)) {
      return ConnectionManager.getInstance().getDB();
    } else {
      return ConnectionManager.getInstance().getDB(dbName);
    }
  }

  public String getToken(String user)
  {
    String passphrase = PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE);
    String in = user+passphrase;
    String token = MessageDigester.getHash(in);
    return token;
  }

  public boolean hasUserWithToken(String user, String token, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    query.put("token", token);
    DBCursor cursor = coll.find(query);
    return (cursor.hasNext());
  }

  public void addUser(String user, String token, String dbName)
  {
    if (!hasUserWithToken(user, token, dbName))
    {
      DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);

      BasicDBObject query = new BasicDBObject();
      query.put("user", user);
      DBCursor cursor = coll.find(query);
      if (cursor.hasNext())
      {
        DBObject doc = cursor.next();
        doc.put("token", token);
        doc.put("isDemoUser", user.startsWith(ANONIM_USER));
        coll.save(doc, WriteConcern.SAFE);
      }
      else
      {
        BasicDBObject doc = new BasicDBObject();
        doc.put("_id", user);
        doc.put("user", user);
        doc.put("token", token);
        doc.put("isDemoUser", user.startsWith(ANONIM_USER));
        coll.insert(doc);
      }
    }
  }

  public Map<String, UserBean> getActiveUsersFilterBy(String user, String dbName, boolean withUsers, boolean withPublic, boolean isAdmin)
  {
    return getActiveUsersFilterBy(user, dbName, withUsers, withPublic, isAdmin, 0);
  }

  public Map<String, UserBean> getActiveUsersFilterBy(String user, String dbName, boolean withUsers, boolean withPublic, boolean isAdmin, int limit)
  {
    List<String> onlineUsers = userStateService.online().stream().map(u -> u.getUserId()).collect(Collectors.toList());

    HashMap<String, UserBean> users = new HashMap<>();
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    if (isAdmin)
    {
      if (withPublic && !withUsers)
      {
        query.put("isDemoUser", true);
      }
      else if (!withPublic && withUsers)
      {
        query.put("isDemoUser", false);
      }
    }
    else
    {
      query.put("isDemoUser", user.startsWith(ANONIM_USER));
    }
    if (limit<0) limit=0;
    DBCursor cursor = coll.find(query).limit(limit);
    while (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      String target = doc.get("user").toString();
      // Exclude current user and not online users
      if (!user.equals(target) && onlineUsers.contains(target)) {
        UserBean userBean = new UserBean();
        userBean.setName(target);
        if (doc.get("fullname")!=null)
          userBean.setFullname( doc.get("fullname").toString() );
        if (doc.get("status")!=null)
          userBean.setStatus(doc.get("status").toString());
        users.put(target, userBean);
      }
    }

    return users;
  }

  public boolean isUserOnline(String user, String dbName)
  {
    return userStateService != null ? userStateService.isOnline(user) : false;
  }

  public boolean isDemoUser(String user)
  {
    return user.startsWith(ANONIM_USER);
  }
}
