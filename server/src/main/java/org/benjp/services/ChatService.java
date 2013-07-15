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
import org.apache.commons.lang3.StringUtils;
import org.benjp.listener.ConnectionManager;
import org.benjp.model.RoomBean;
import org.benjp.model.RoomsBean;
import org.benjp.model.SpaceBean;
import org.benjp.model.UserBean;
import org.benjp.utils.MessageDigester;
import org.bson.types.ObjectId;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

@Named("chatService")
@ApplicationScoped
public class ChatService
{
  public static final String M_ROOM_PREFIX = "room_";
  public static final String M_ROOMS_COLLECTION = "rooms";

  public static final String SPACE_PREFIX = "space-";

  public static final String TYPE_DELETED = "DELETED";
  public static final String TYPE_EDITED = "EDITED";

  private static Logger log = Logger.getLogger("ChatService");

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
    return read(room, userService, false);
  }

  public String read(String room, UserService userService, boolean isTextOnly)
  {
    StringBuffer sb = new StringBuffer();

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
    query.put("timestamp", new BasicDBObject("$gt", System.currentTimeMillis()-7*24*60*60*1000));

    BasicDBObject sort = new BasicDBObject();
    sort.put("timestamp", -1);

    int limit = (isTextOnly)?2000:200;
    DBCursor cursor = coll.find(query).sort(sort).limit(limit);
    String prevUser = "";
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

      List<DBObject> listdbo = new ArrayList<DBObject>();
      String mostRecentTimestamp = null;
      while (cursor.hasNext())
      {
        /** sorting and unsorting on cursor doesn't work, we need to reverse using a 2nd loop
         * not good in term of performance, we should find a better way for this to work with
         * sorting and limit in the query
         */
        DBObject dbo = cursor.next();
        if (mostRecentTimestamp==null)
        {
          mostRecentTimestamp = dbo.get("timestamp").toString();
        }
        listdbo.add(0, dbo);
      }
      if (!isTextOnly)
      {
        sb.append("{\"room\": \"").append(room).append("\",");
        sb.append("\"timestamp\": \"").append(mostRecentTimestamp).append("\",");
        sb.append("\"messages\": [");
      }
      boolean first = true;
      for(DBObject dbo:listdbo)
      {
        String user = dbo.get("user").toString();
        String fullname = "", email = "";
        String msgId = ((ObjectId)dbo.get("_id")).toString();
        UserBean userBean = users.get(user);
        if (userBean==null)
        {
          userBean = userService.getUser(user);
          users.put(user, userBean);
        }
        fullname = userBean.getFullname();
        email = userBean.getEmail();

        String date = "";
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
          sb.append("[").append(date).append("] ");
          String message = dbo.get("message").toString();
          if (TYPE_DELETED.equals(message)) message = TYPE_DELETED+"\n";
          if ("true".equals(dbo.get("isSystem")))
          {
            sb.append("System Message: ");
            if (message.endsWith("<br/>")) message = message.substring(0, message.length()-5);
            sb.append(message).append("\n");
          }
          else
          {
            sb.append(fullname).append(": ");
            message = message.replaceAll("<br/>", "\n");
            sb.append(message);
          }
        }
        else
        {
          if (!first)sb.append(",");
          sb.append("{\"id\": \"").append(msgId).append("\",");
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
      if (dbo.containsField("space"))
      {  //space room, we set timestamp in the collection spaces
        String spaceId = dbo.get("space").toString();
        //removing "space-" prefix
        if (spaceId.indexOf("space-")>-1)
        {
          spaceId = spaceId.substring(6);
        }
        DBCollection colls = db().getCollection(UserService.M_SPACES_COLLECTION);
        BasicDBObject query = new BasicDBObject();
        query.put("_id", spaceId);
        DBCursor cursors = colls.find(query);
        if (cursors.hasNext())
        {
          DBObject doc = cursors.next();
          doc.put("timestamp", System.currentTimeMillis());
          colls.save(doc, WriteConcern.NONE);
        }
      }
      else
      {  //users room, we set directly in rooms_room
        dbo.put("timestamp", System.currentTimeMillis());
        coll.save(dbo, WriteConcern.NONE);
      }
    }

  }

  private void ensureIndexInRoom(String room)
  {
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+room);
    BasicDBObject doc = new BasicDBObject();
    doc.put("timestamp", System.currentTimeMillis());
    coll.insert(doc);
    coll.ensureIndex("timestamp");
    coll.remove(doc);
  }

  private String getRoomId(String space)
  {
    ArrayList<String> spaces = new ArrayList<String>();
    spaces.add("1-space-room");
    spaces.add(space);
    return getRoomId(spaces);
  }

  private String getRoomId(List<String> users)
  {
    Collections.sort(users);
    StringBuilder sb = new StringBuilder();
    for (String user:users)
    {
      sb.append(user).append(";");
    }

    String roomId = MessageDigester.getHash(sb.toString());
    return roomId;
  }

  public String getSpaceRoom(String space)
  {
    String room = getRoomId(space);
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (!cursor.hasNext())
    {
      try {
        basicDBObject.put("space", space);
        coll.insert(basicDBObject);
        ensureIndexInRoom(room);
      } catch (MongoException me) {
        log.warning(me.getCode()+" : "+room+" : "+me.getMessage());
      }
    }

    return room;
  }


  public String getRoom(List<String> users)
  {
    Collections.sort(users);
    String room = getRoomId(users);
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("_id", room);

    DBCursor cursor = coll.find(basicDBObject);
    if (!cursor.hasNext())
    {
      try {
        basicDBObject.put("users", users);
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

  public boolean hasRoom(List<String> users)
  {
    Collections.sort(users);
    String room = null;
    DBCollection coll = db().getCollection(M_ROOM_PREFIX+M_ROOMS_COLLECTION);

    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("users", users);

    DBCursor cursor = coll.find(basicDBObject);
    return cursor.hasNext();
  }

  public RoomsBean getRooms(String user, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean isAdmin, NotificationService notificationService, UserService userService, TokenService tokenService)
  {
    return getRooms(user, filter, withUsers, withSpaces, withPublic, true, isAdmin, notificationService, userService, tokenService);
  }
  public RoomsBean getRooms(String user, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, NotificationService notificationService, UserService userService, TokenService tokenService)
  {
    List<RoomBean> rooms = new ArrayList<RoomBean>();
    List<RoomBean> roomsOffline = new ArrayList<RoomBean>();
    UserBean userBean = userService.getUser(user, true);
    int unreadOffline=0, unreadOnline=0, unreadSpaces=0;

    Collection<String> availableUsers = tokenService.getActiveUsersFilterBy(user, withUsers, withPublic, isAdmin);

    rooms = this.getExistingRooms(user, withPublic, isAdmin, notificationService, tokenService);
    if (isAdmin)
      rooms.addAll(this.getExistingRooms(UserService.SUPPORT_USER, withPublic, isAdmin, notificationService, tokenService));

    for (RoomBean roomBean:rooms)
    {
      String targetUser = roomBean.getUser();
      UserBean targetUserBean = userService.getUser(targetUser);
      roomBean.setFullname(targetUserBean.getFullname());
      roomBean.setFavorite(userBean.isFavorite(targetUser));

      if (availableUsers.contains(targetUser))
      {
        roomBean.setAvailableUser(true);
        roomBean.setStatus(targetUserBean.getStatus());
        availableUsers.remove(targetUser);
        if (roomBean.getUnreadTotal()>0)
          unreadOnline += roomBean.getUnreadTotal();
      }
      else
      {
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

      for (String availableUser: availableUsers)
      {
        RoomBean roomBean = new RoomBean();
        roomBean.setUser(availableUser);
        UserBean availableUserBean = userService.getUser(availableUser);
        roomBean.setFullname(availableUserBean.getFullname());
        roomBean.setStatus(availableUserBean.getStatus());
        roomBean.setAvailableUser(true);
        roomBean.setFavorite(userBean.isFavorite(roomBean.getUser()));
        String status = roomBean.getStatus();
        if (withOffline || (!withOffline && !UserService.STATUS_INVISIBLE.equals(roomBean.getStatus()) && !UserService.STATUS_OFFLINE.equals(roomBean.getStatus())))
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
      roomBeanS.setUser(SPACE_PREFIX+space.getId());
      roomBeanS.setFullname(space.getDisplayName());
      roomBeanS.setStatus(UserService.STATUS_SPACE);
      roomBeanS.setTimestamp(space.getTimestamp());
      roomBeanS.setAvailableUser(true);
      roomBeanS.setSpace(true);
      roomBeanS.setUnreadTotal(notificationService.getUnreadNotificationsTotal(user, "chat", "room", getSpaceRoom(SPACE_PREFIX + space.getId())));
      if (roomBeanS.getUnreadTotal()>0)
        unreadSpaces += roomBeanS.getUnreadTotal();
      roomBeanS.setFavorite(userBean.isFavorite(roomBeanS.getUser()));
      if (withSpaces)
      {
        rooms.add(roomBeanS);
      }

    }


    List<RoomBean> finalRooms = new ArrayList<RoomBean>();
    for (RoomBean roomBean:rooms) {
      String targetUser = roomBean.getFullname();
      if (filter(targetUser, filter))
        finalRooms.add(roomBean);
    }

    Collections.sort(finalRooms);

    RoomsBean roomsBean = new RoomsBean();
    roomsBean.setRooms(rooms);
    roomsBean.setUnreadOffline(unreadOffline);
    roomsBean.setUnreadOnline(unreadOnline);
    roomsBean.setUnreadSpaces(unreadSpaces);

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
      nb += cursorr.count();
    }

    return nb;
  }


}
