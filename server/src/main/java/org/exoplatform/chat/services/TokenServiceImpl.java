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

package org.exoplatform.chat.services;

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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named("tokenService")
@ApplicationScoped
@Singleton
public class TokenServiceImpl implements TokenService
{
  //TODO this service will not be available in 2 mode servers, we will need to find another solution (REST API /state ?)
  private UserStateService userStateService;

  @Inject
  private TokenStorage storage;

  public TokenServiceImpl() {
    userStateService = PortalContainer.getInstance().getComponentInstanceOfType(UserStateService.class);
  }

  public TokenServiceImpl(UserStateService userStateService) {
    this.userStateService = userStateService;
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
    return storage.hasUserWithToken(user, token, dbName);
  }

  public void addUser(String user, String token, String dbName)
  {
    storage.addUser(user, token, dbName);
  }

  public Map<String, UserBean> getActiveUsersFilterBy(String user, String dbName, boolean withUsers, boolean withPublic, boolean isAdmin)
  {
    return getActiveUsersFilterBy(user, dbName, withUsers, withPublic, isAdmin, 0);
  }

  public Map<String, UserBean> getActiveUsersFilterBy(String user, String dbName, boolean withUsers, boolean withPublic, boolean isAdmin, int limit)
  {
    List<String> onlineUsers = userStateService.online().stream().map(u -> u.getUserId()).collect(Collectors.toList());

    return storage.getActiveUsersFilterBy(user, onlineUsers, dbName, withUsers, withPublic, isAdmin, limit);
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
