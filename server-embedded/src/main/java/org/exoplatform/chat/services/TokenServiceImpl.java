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

import org.exoplatform.chat.model.UserBean;
import org.exoplatform.chat.utils.MessageDigester;
import org.exoplatform.chat.utils.PropertyManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Named("tokenService")
@ApplicationScoped
@Singleton
public class TokenServiceImpl implements TokenService
{
  @Inject
  private TokenStorage storage;

  public TokenServiceImpl() {
  }

  public String getToken(String user)
  {
    String passphrase = PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE);
    String in = user+passphrase;
    String token = MessageDigester.getHash(in);
    return token;
  }

  public boolean hasUserWithToken(String user, String token)
  {
    return hasUserWithToken(user, token, null);
  }

  public boolean hasUserWithToken(String user, String token, String dbName)
  {
    return storage.hasUserWithToken(user, token, dbName);
  }

  public void addUser(String user, String token, String dbName)
  {
    storage.addUser(user, token, dbName);
  }

  public Map<String, UserBean> getActiveUsersFilterBy(String user, List<String> limitUsers, String dbName, boolean withUsers, boolean withPublic, boolean isAdmin, int limit)
  {
    return storage.getActiveUsersFilterBy(user, limitUsers, dbName, withUsers, withPublic, isAdmin, limit);
  }

  public boolean isDemoUser(String user)
  {
    return user.startsWith(ANONIM_USER);
  }
}
