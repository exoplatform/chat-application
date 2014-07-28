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
import com.mongodb.util.JSON;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.model.RoomBean;
import org.exoplatform.chat.model.RoomsBean;
import org.exoplatform.chat.model.SpaceBean;
import org.exoplatform.chat.model.UserBean;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.NotificationService;
import org.exoplatform.chat.services.TokenService;
import org.exoplatform.chat.services.UserService;
import org.exoplatform.chat.utils.ChatUtils;
import org.exoplatform.chat.utils.PropertyManager;
import org.bson.types.ObjectId;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

@Named("chatService")
@ApplicationScoped
public class ChatServiceImpl implements org.exoplatform.chat.services.ChatService
{

  private static Logger log = Logger.getLogger("ChatService");

  private long readMillis;
  private int readTotalJson, readTotalTxt;

  public ChatServiceImpl()
  {
    long readDays = Long.parseLong(PropertyManager.getProperty(PropertyManager.PROPERTY_READ_DAYS));
    readMillis = readDays*24*60*60*1000;
    readTotalJson = Integer.parseInt(PropertyManager.getProperty(PropertyManager.PROPERTY_READ_TOTAL_JSON));
    readTotalTxt = Integer.parseInt(PropertyManager.getProperty(PropertyManager.PROPERTY_READ_TOTAL_TXT));
  }

  private DB db()
  {
    return ConnectionManager.getInstance().getDB();
  }

  public void write(String message, String user, String room, String isSystem)
  {
    write(message, user, room, isSystem, null);
  }

  public void write(String message, String user, String room, String isSystem, String options)
  {
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+room);

    message = StringUtils.chomp(message);
    message = message.replaceAll("&", "&#38");
    message = message.replaceAll("<", "&lt;");
    message = message.replaceAll(">", "&gt;");
    message = message.replaceAll("\"", "&quot;");
    message = message.replaceAll("\n", "<br/>");
    message = message.replaceAll("\\\\", "&#92");
    message = message.replaceAll("\t", "  ");

    BasicDBObject doc = new BasicDBObject();
    doc.put("user", user);
    doc.put("message", message);
    doc.put("time", new Date());
    doc.put("timestamp", System.currentTimeMillis());
    doc.put("isSystem", isSystem);
    if (options!=null)
    {
      options = options.replaceAll("<", "&lt;");
      options = options.replaceAll(">", "&gt;");
      options = options.replaceAll("'", "\"");
//      options = options.replaceAll("\"", "&quot;");
//      options = options.replaceAll("\\\\", "&#92");
      doc.put("options", options);
    }

    coll.insert(doc);

    this.updateRoomTimestamp(room);
  }

  public void delete(String room, String user, String messageId)
  {
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+room);
    BasicDBObject query = new BasicDBObject();
    query.put("_id", new ObjectId(messageId));
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject dbo = cursor.next();
      dbo.put("message", TYPE_DELETED);
      dbo.put("type", TYPE_DELETED);
      coll.save(dbo, WriteConcern.NONE);
    }
  }

  public void edit(String room, String user, String messageId, String message)
  {
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+room);

    message = StringUtils.chomp(message);
    message = message.replaceAll("&", "&#38");
    message = message.replaceAll("<", "&lt;");
    message = message.replaceAll(">", "&gt;");
    message = message.replaceAll("\"", "&quot;");
    message = message.replaceAll("\n", "<br/>");
    message = message.replaceAll("\\\\", "&#92");

    BasicDBObject query = new BasicDBObject();
    query.put("_id", new ObjectId(messageId));
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject dbo = cursor.next();
      dbo.put("message", message);
      dbo.put("type", TYPE_EDITED);
      coll.save(dbo, WriteConcern.NONE);
    }
  }


  public String read(String room, UserService userService)
  {
    return read(room, userService, false, null, null);
  }

  public String read(String room, UserService userService, boolean isTextOnly, Long fromTimestamp)
  {
    return read(room, userService, isTextOnly, fromTimestamp, null);
  }

  public String read(String room, UserService userService, boolean isTextOnly, Long fromTimestamp, Long toTimestamp) {
    StringBuilder sb = new StringBuilder();

    SimpleDateFormat formatter = new SimpleDateFormat("hh:mm aaa");
    SimpleDateFormat formatterDate = new SimpleDateFormat("dd/MM/yyyy hh:mm aaa");
    // formatter.format();
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    Date today = calendar.getTime();

    DBCollection coll = db().getCollection(M_ROOM_PREFIX+room);

    BasicDBObject query = new BasicDBObject();
    long from = (fromTimestamp!=null) ? fromTimestamp : System.currentTimeMillis() - readMillis;
    BasicDBObject tsobj = new BasicDBObject("$gt", from);
    if (toTimestamp!=null)
    {
      tsobj.append("$lt", toTimestamp);
    }
    query.put("timestamp", tsobj);

    BasicDBObject sort = new BasicDBObject();
    sort.put("timestamp", -1);
    int limit = (isTextOnly)?readTotalTxt:readTotalJson;
    DBCursor cursor = coll.find(query).sort(sort).limit(limit);
    if (!cursor.hasNext())
    {
      if (isTextOnly)
        sb.append("no messages");
      else
        sb.append("{\"messages\": []}");
    }
    else
    {
      Map<String, UserBean> users = new HashMap<String, UserBean>();

      String timestamp, user, fullname, email, msgId, date;
      boolean first = true;

      while (cursor.hasNext())
      {
        DBObject dbo = cursor.next();
        timestamp = dbo.get("timestamp").toString();
        if (first) //first element (most recent one)
        {
          if (!isTextOnly)
          {
            sb.append("{\"room\": \"").append(room).append("\",");
            sb.append("\"timestamp\": \"").append(timestamp).append("\",");
            sb.append("\"messages\": [");
          }
        }

        user = dbo.get("user").toString();
        msgId = dbo.get("_id").toString();
        UserBean userBean = users.get(user);
        if (userBean==null)
        {
          userBean = userService.getUser(user);
          users.put(user, userBean);
        }
        fullname = userBean.getFullname();
        email = userBean.getEmail();

        date = "";
        try
        {
          if (dbo.containsField("time"))
          {
            Date date1 = (Date)dbo.get("time");
            if (date1.before(today) || isTextOnly)
              date = formatterDate.format(date1);
            else
              date = formatter.format(date1);

          }
        }
        catch (Exception e)
        {
          log.info("Message Date Format Error : "+e.getMessage());
        }

        if (isTextOnly)
        {
          StringBuilder line = new StringBuilder();
          line.append("[").append(date).append("] ");
          String message = dbo.get("message").toString();
          if (TYPE_DELETED.equals(message)) message = TYPE_DELETED;
          if ("true".equals(dbo.get("isSystem")))
          {
            line.append("System Message: ");
            if (message.endsWith("<br/>")) message = message.substring(0, message.length()-5);
            line.append(message).append("\n");
          }
          else
          {
            line.append(fullname).append(": ");
            message = message.replaceAll("<br/>", "\n");
            line.append(message).append("\n");
          }
          sb.insert(0, line);
        }
        else
        {
          if (!first)sb.append(",");
          sb.append("{\"id\": \"").append(msgId).append("\",");
          sb.append("\"timestamp\": ").append(timestamp).append(",");
          sb.append("\"user\": \"").append(user).append("\",");
          sb.append("\"fullname\": \"").append(fullname).append("\",");
          sb.append("\"email\": \"").append(email).append("\",");
          sb.append("\"date\": \"").append(date).append("\",");
          sb.append("\"message\": \"").append(dbo.get("message")).append("\",");
          if (dbo.containsField("options"))
          {
            String options = dbo.get("options").toString();
            if (options.startsWith("{"))
              sb.append("\"options\": ").append(options).append(",");
            else
              sb.append("\"options\": \"").append(options).append("\",");
          }
          else
          {
            sb.append("\"options\": \"\",");
          }
          sb.append("\"type\": \"").append(dbo.get("type")).append("\",");
          sb.append("\"isSystem\": \"").append(dbo.get("isSystem")).append("\"}");
        }

        first = false;
      }

      if (!isTextOnly)
      {
        sb.append("]}");
      }
    }

    return sb.toString();

  }

  private void updateRoomTimestamp(String room)
  {
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext())
    {
      DBObject dbo = cursor.next();
      dbo.put("timestamp", System.currentTimeMillis());
      coll.save(dbo, WriteConcern.NONE);
    }

  }

  private void ensureIndexInRoom(String room)
  {
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+room);
    BasicDBObject doc = new BasicDBObject();
    doc.put("timestamp", System.currentTimeMillis());
    coll.insert(doc);
    ConnectionManager.getInstance().ensureIndexesInRoom(room);
    coll.remove(doc);
  }

  public String getSpaceRoom(String space)
  {
    String room = ChatUtils.getRoomId(space);
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (!cursor.hasNext())
    {
      try {
        basicDBObject.put("space", space);
        basicDBObject.put("type", TYPE_ROOM_SPACE);
        coll.insert(basicDBObject);
        ensureIndexInRoom(room);
      } catch (MongoException me) {
        log.warning(me.getCode()+" : "+room+" : "+me.getMessage());
      }
    }

    return room;
  }

  public String getSpaceRoomByName(String name) {
    String room = null;
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("shortName", name);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      room = doc.get("_id").toString();
    }

    return room;
  }

  public String getTeamRoom(String team, String user) {
    String room = ChatUtils.getRoomId(team, user);
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (!cursor.hasNext())
    {
      try {
        basicDBObject.put("team", team);
        basicDBObject.put("user", user);
        basicDBObject.put("type", TYPE_ROOM_TEAM);
        coll.insert(basicDBObject);
        ensureIndexInRoom(room);
      } catch (MongoException me) {
        log.warning(me.getCode()+" : "+room+" : "+me.getMessage());
      }
    }

    return room;
  }

  public String getExternalRoom(String identifier) {
    String room = ChatUtils.getExternalRoomId(identifier);
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (!cursor.hasNext())
    {
      try {
        basicDBObject.put("identifier", identifier);
        basicDBObject.put("type", TYPE_ROOM_EXTERNAL);
        coll.insert(basicDBObject);
        ensureIndexInRoom(room);
      } catch (MongoException me) {
        log.warning(me.getCode()+" : "+room+" : "+me.getMessage());
      }
    }

    return room;
  }

  public String getTeamCreator(String room) {
    if (room.indexOf(ChatService.TEAM_PREFIX)==0)
    {
      room = room.substring(ChatService.TEAM_PREFIX.length());
    }
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);
    String creator = "";
    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext())
    {
      try {
        DBObject dbo = cursor.next();
        creator = dbo.get("user").toString();
      } catch (MongoException me) {
        log.warning(me.getCode()+" : "+room+" : "+me.getMessage());
      }
    }

    return creator;
  }

  public void setRoomName(String room, String name) {
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (cursor.hasNext())
    {
      DBObject dbo = cursor.next();
      dbo.put("team", name);
      coll.save(dbo, WriteConcern.NONE);
    }
  }


  public String getRoom(List<String> users)
  {
    Collections.sort(users);
    String room = ChatUtils.getRoomId(users);
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (!cursor.hasNext())
    {
      try {
        basicDBObject.put("users", users);
        basicDBObject.put("type", TYPE_ROOM_USER);
        coll.insert(basicDBObject);
        ensureIndexInRoom(room);
      } catch (MongoException me) {
        log.warning(me.getCode()+" : "+room+" : "+me.getMessage());
      }
    }

    return room;
  }

  public List<RoomBean> getExistingRooms(String user, boolean withPublic, boolean isAdmin, NotificationService notificationService, TokenService tokenService)
  {
    List<RoomBean> rooms = new ArrayList<RoomBean>();
    String roomId = null;
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("users", user);

    DBCursor cursor = coll.find(basicDBObject);
    while (cursor.hasNext())
    {
      DBObject dbo = cursor.next();
      roomId = dbo.get("_id").toString();
      long timestamp = -1;
      if (dbo.containsField("timestamp")) {
        timestamp = ((Long)dbo.get("timestamp")).longValue();
      }
      List<String> users = ((List<String>)dbo.get("users"));
      users.remove(user);
      if (users.size()>0 && !user.equals(users.get(0)))
      {
        String targetUser = users.get(0);
        boolean isDemoUser = tokenService.isDemoUser(targetUser);
        if (!isAdmin || (isAdmin && ((!withPublic && !isDemoUser) || (withPublic && isDemoUser))))
        {
          RoomBean roomBean = new RoomBean();
          roomBean.setRoom(roomId);
          roomBean.setUnreadTotal(notificationService.getUnreadNotificationsTotal(user, "chat", "room", roomId));
          roomBean.setUser(users.get(0));
          roomBean.setTimestamp(timestamp);
          rooms.add(roomBean);
        }
      }
    }

    return rooms;
  }

  public RoomsBean getRooms(String user, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, NotificationService notificationService, UserService userService, TokenService tokenService) {
    return getRooms(user, filter, withUsers, withSpaces, withPublic, withOffline, isAdmin, 0, notificationService, userService, tokenService);
  }

  public RoomsBean getRooms(String user, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, int limit, NotificationService notificationService, UserService userService, TokenService tokenService)
  {
    List<RoomBean> rooms = new ArrayList<RoomBean>();
    List<RoomBean> roomsOffline = new ArrayList<RoomBean>();
    UserBean userBean = userService.getUser(user, true);
    int unreadOffline=0, unreadOnline=0, unreadSpaces=0, unreadTeams=0;

    HashMap<String, UserBean> availableUsers = tokenService.getActiveUsersFilterBy(user, withUsers, withPublic, isAdmin, limit);

    rooms = this.getExistingRooms(user, withPublic, isAdmin, notificationService, tokenService);
    if (isAdmin)
      rooms.addAll(this.getExistingRooms(UserServiceImpl.SUPPORT_USER, withPublic, isAdmin, notificationService, tokenService));

    for (RoomBean roomBean:rooms)
    {
      String targetUser = roomBean.getUser();
      roomBean.setFavorite(userBean.isFavorite(targetUser));

      if (availableUsers.keySet().contains(targetUser))
      {
        UserBean targetUserBean = availableUsers.get(targetUser);
        roomBean.setFullname(targetUserBean.getFullname());
        roomBean.setStatus(targetUserBean.getStatus());
        roomBean.setAvailableUser(true);
        availableUsers.remove(targetUser);
        if (roomBean.getUnreadTotal()>0)
          unreadOnline += roomBean.getUnreadTotal();
      }
      else
      {
        UserBean targetUserBean = userService.getUser(targetUser);
        roomBean.setFullname(targetUserBean.getFullname());
        roomBean.setAvailableUser(false);
        if (!withOffline)
          roomsOffline.add(roomBean);
        if (roomBean.getUnreadTotal()>0)
          unreadOffline += roomBean.getUnreadTotal();

      }
    }

    if (withUsers)
    {
      if (!withOffline)
      {
        for (RoomBean roomBean:roomsOffline)
        {
          rooms.remove(roomBean);
        }
      }

      for (UserBean availableUser: availableUsers.values())
      {
        RoomBean roomBean = new RoomBean();
        roomBean.setUser(availableUser.getName());
        roomBean.setFullname(availableUser.getFullname());
        roomBean.setStatus(availableUser.getStatus());
        roomBean.setAvailableUser(true);
        roomBean.setFavorite(userBean.isFavorite(roomBean.getUser()));
        String status = roomBean.getStatus();
        if (withOffline || (!withOffline && !UserServiceImpl.STATUS_INVISIBLE.equals(roomBean.getStatus()) && !UserServiceImpl.STATUS_OFFLINE.equals(roomBean.getStatus())))
        {
          rooms.add(roomBean);
        }
      }
    }
    else
    {
      rooms = new ArrayList<RoomBean>();
    }

    List<SpaceBean> spaces = userService.getSpaces(user);
    for (SpaceBean space:spaces)
    {
      RoomBean roomBeanS = new RoomBean();
      roomBeanS.setUser(SPACE_PREFIX+space.getRoom());
      roomBeanS.setRoom(space.getRoom());
      roomBeanS.setFullname(space.getDisplayName());
      roomBeanS.setStatus(UserService.STATUS_SPACE);
      roomBeanS.setTimestamp(space.getTimestamp());
      roomBeanS.setAvailableUser(true);
      roomBeanS.setSpace(true);
      roomBeanS.setUnreadTotal(notificationService.getUnreadNotificationsTotal(user, "chat", "room", getSpaceRoom(SPACE_PREFIX + space.getRoom())));
      if (roomBeanS.getUnreadTotal()>0)
        unreadSpaces += roomBeanS.getUnreadTotal();
      roomBeanS.setFavorite(userBean.isFavorite(roomBeanS.getUser()));
      if (withSpaces)
      {
        rooms.add(roomBeanS);
      }

    }

    List<RoomBean> teams = userService.getTeams(user);
    for (RoomBean team:teams)
    {
      RoomBean roomBeanS = new RoomBean();
      roomBeanS.setUser(TEAM_PREFIX + team.getRoom());
      roomBeanS.setRoom(team.getRoom());
      roomBeanS.setFullname(team.getFullname());
      roomBeanS.setStatus(UserService.STATUS_TEAM);
      roomBeanS.setTimestamp(team.getTimestamp());
      roomBeanS.setAvailableUser(true);
      roomBeanS.setSpace(false);
      roomBeanS.setTeam(true);
      roomBeanS.setUnreadTotal(notificationService.getUnreadNotificationsTotal(user, "chat", "room", team.getRoom()));
      if (roomBeanS.getUnreadTotal()>0)
        unreadTeams += roomBeanS.getUnreadTotal();
      roomBeanS.setFavorite(userBean.isFavorite(roomBeanS.getUser()));
      if (withSpaces)
      {
        rooms.add(roomBeanS);
      }

    }

    List<RoomBean> finalRooms = new ArrayList<RoomBean>();
    if (filter!=null)
    {
      for (RoomBean roomBean:rooms) {
        String targetUser = roomBean.getFullname();
        if (filter(targetUser, filter))
          finalRooms.add(roomBean);
      }
    }
    else
    {
      finalRooms = rooms;
    }

    RoomsBean roomsBean = new RoomsBean();
    roomsBean.setRooms(finalRooms);
    roomsBean.setUnreadOffline(unreadOffline);
    roomsBean.setUnreadOnline(unreadOnline);
    roomsBean.setUnreadSpaces(unreadSpaces);
    roomsBean.setUnreadTeams(unreadTeams);

    return roomsBean;

  }

  private boolean filter(String user, String filter)
  {
    if (user==null || filter==null || "".equals(filter)) return true;

    String[] args = filter.toLowerCase().split(" ");
    String s = user.toLowerCase();
    int ind;
    for (String arg:args)
    {
      ind = s.indexOf(arg);
      if (ind == -1)
        return false;
      else
        s = s.substring(ind);
    }
    return true;
  }

  public int getNumberOfRooms()
  {
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    DBCursor cursor = coll.find(query);
    return cursor.count();
  }

  public int getNumberOfMessages()
  {
    int nb = 0;
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    DBCursor cursor = coll.find(query);
    while (cursor.hasNext())
    {
      DBObject dbo = cursor.next();
      String roomId = dbo.get("_id").toString();
      DBCollection collr = db().getCollection(M_ROOM_PREFIX+roomId);
      BasicDBObject queryr = new BasicDBObject();
      DBCursor cursorr = collr.find(queryr);
//      log.info(roomId+" = "+cursorr.count());
      nb += cursorr.count();
    }

    return nb;
  }


}
