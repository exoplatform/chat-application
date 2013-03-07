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

package org.benjp.server;

import juzu.Resource;
import juzu.Response;
import juzu.Route;
import org.benjp.listener.ConnectionManager;
import org.benjp.services.MongoBootstrap;
import org.benjp.services.TokenService;
import org.benjp.services.UserService;
import org.benjp.utils.PropertyManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ChatTools
{

  @Inject
  UserService userService;

  @Inject
  TokenService tokenService;


  @Resource
  @Route("/createDemoUser")
  public Response.Content createDemoUser(String username, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    if (username == null)
    {
      return Response.notFound("{ \"message\": \"username is null\"}");
    }

    String token = tokenService.getToken(username);
    tokenService.addUser(username, token);
    userService.addUserFullName(username, username);
    userService.setAsAdmin(username, false);

    StringBuffer data = new StringBuffer();
    data.append("{");
    data.append(" \"message\": \"OK\", ");
    data.append(" \"token\": \""+token+"\", ");
    data.append(" \"user\": \""+ username+"\" ");
    data.append("}");

    return Response.ok(data.toString()).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/getToken")
  public Response.Content getToken(String username, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    if (username == null)
    {
      return Response.notFound("{ \"message\": \"username is null\"}");
    }

    String token = tokenService.getToken(username);

    StringBuffer data = new StringBuffer();
    data.append("{");
    data.append(" \"message\": \"OK\", ");
    data.append(" \"token\": \""+token+"\", ");
    data.append(" \"user\": \""+ username+"\" ");
    data.append("}");

    return Response.ok(data.toString()).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/initDB")
  public Response.Content initDB(String db, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    if (db == null)
    {
      return Response.notFound("{ \"message\": \"db is null\"}");
    }

    ConnectionManager.getInstance().getDB(db);

    StringBuffer data = new StringBuffer();
    data.append("{");
    data.append(" \"message\": \"using db="+db+"\"");
    data.append("}");

    return Response.ok(data.toString()).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/dropDB")
  public Response.Content dropDB(String db, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    if (db == null)
    {
      return Response.notFound("{ \"message\": \"db is null\"}");
    }

    ConnectionManager.getInstance().dropDB(db);

    StringBuffer data = new StringBuffer();
    data.append("{");
    data.append(" \"message\": \"deleting db="+db+"\"");
    data.append("}");

    return Response.ok(data.toString()).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/ensureIndexes")
  public Response.Content ensureIndexes(String db, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    if (db == null)
    {
      return Response.notFound("{ \"message\": \"db is null\"}");
    }

    if (!db.equals(ConnectionManager.getInstance().getDB().getName()))
    {
      return Response.notFound("{ \"message\": \"db name doesn't match\"}");
    }

    ConnectionManager.getInstance().ensureIndexes();

    StringBuffer data = new StringBuffer();
    data.append("{");
    data.append(" \"message\": \"indexes created or updated on db="+db+"\"");
    data.append("}");

    return Response.ok(data.toString()).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

}
