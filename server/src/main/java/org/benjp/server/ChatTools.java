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

import juzu.Action;
import juzu.Resource;
import juzu.Response;
import juzu.Route;
import org.benjp.listener.ConnectionManager;
import org.benjp.model.SpaceBean;
import org.benjp.model.SpaceBeans;
import org.benjp.services.ChatService;
import org.benjp.services.NotificationService;
import org.benjp.services.TokenService;
import org.benjp.services.UserService;
import org.benjp.utils.ChatUtils;
import org.benjp.utils.PropertyManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ApplicationScoped
public class ChatTools
{

  @Inject
  UserService userService;

  @Inject
  TokenService tokenService;

  @Inject
  NotificationService notificationService;

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
  @Route("/createDemoSpaces")
  public Response.Content createDemoSpaces(String username, String nbSpaces, String nbSpacesMax, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    if (username == null)
    {
      return Response.notFound("{ \"message\": \"username is null\"}");
    }

    int nbSpacesInt = new Integer(nbSpaces);
    int nbSpacesMaxInt = new Integer(nbSpacesMax);
    if (nbSpacesInt<1)
    {
      return Response.notFound("{ \"message\": \"you must create at least one space\"}");
    }

    if (nbSpacesMaxInt<nbSpacesInt)
    {
      return Response.notFound("{ \"message\": \"max spaces must be greater than spaces per user\"}");
    }

    String spacePrefix = "demo-";
    Random random = new Random(System.currentTimeMillis());
    List<SpaceBean> beans = new ArrayList<SpaceBean>();
    for (int i=0 ; i<nbSpacesInt ; i++)
    {
      int spaceNum = random.nextInt(nbSpacesMaxInt);
      SpaceBean spaceBean = new SpaceBean();
      spaceBean.setDisplayName("Demo Space "+spaceNum);
      spaceBean.setGroupId("/spaces/"+spacePrefix+spaceNum);
      spaceBean.setId(spacePrefix+spaceNum);
      spaceBean.setShortName("Demo Space "+spaceNum);
      beans.add(spaceBean);
    }
    userService.setSpaces(username, beans);



    StringBuffer data = new StringBuffer();
    data.append("{");
    data.append(" \"message\": \"OK\", ");
    data.append(" \"user\": \""+ username+"\" ");
    data.append("}");

    return Response.ok(data.toString()).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/getToken")
  public Response.Content getToken(String username, String passphrase, String tokenOnly)
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
    if (tokenOnly!=null && "true".equals(tokenOnly))
    {
      data.append(token);
    }
    else
    {
      data.append("{");
      data.append(" \"message\": \"OK\", ");
      data.append(" \"token\": \""+token+"\", ");
      data.append(" \"user\": \""+ username+"\" ");
      data.append("}");
    }

    return Response.ok(data.toString()).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/addUser")
  public Response.Content addUser(String username, String token, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    tokenService.addUser(username, token);

    return Response.ok("OK").withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/setAsAdmin")
  public Response.Content setAsAdmin(String username, String isAdmin, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    userService.setAsAdmin(username, "true".equals(isAdmin));

    return Response.ok("OK").withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/addUserFullNameAndEmail")
  public Response.Content addUserFullNameAndEmail(String username, String fullname, String email, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    userService.addUserEmail(username, email);
    userService.addUserFullName(username, fullname);

    return Response.ok("OK").withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/setSpaces")
  public Response.Content setSpaces(String username, String spaces, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    try {
      SpaceBeans spaceBeans = (SpaceBeans)ChatUtils.fromString(spaces);
      userService.setSpaces(username, spaceBeans.getSpaces());
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (ClassNotFoundException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }

    return Response.ok("OK").withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/getUserFullName")
  public Response.Content getUserFullName(String username, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    String fullname = userService.getUserFullName(username);

    return Response.ok(fullname).withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }

  @Resource
  @Route("/updateUnreadTestMessages")
  public Response.Content updateUnreadTestMessages(String username, String room, String passphrase)
  {
    if (!PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase))
    {
      return Response.notFound("{ \"message\": \"passphrase doesn't match\"}");
    }

    if (username == null)
    {
      return Response.notFound("{ \"message\": \"username is null\"}");
    }

    if (room == null)
    {
      return Response.notFound("{ \"message\": \"room is null\"}");
    }

    if (username.startsWith(ChatService.SPACE_PREFIX))
      return Response.ok("OK").withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");

    if (!room.equals("ALL"))
      notificationService.setNotificationsAsRead(username, "chat", "room", room);
    else
      notificationService.setNotificationsAsRead(username, null, null, null);

    return Response.ok("OK").withMimeType("text/event-stream; charset=UTF-8").withHeader("Cache-Control", "no-cache");
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
