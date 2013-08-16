package org.benjp.services.jcr;

import org.apache.commons.lang3.StringUtils;
import org.benjp.model.RoomBean;
import org.benjp.model.RoomsBean;
import org.benjp.model.SpaceBean;
import org.benjp.model.UserBean;
import org.benjp.services.ChatService;
import org.benjp.services.NotificationService;
import org.benjp.services.TokenService;
import org.benjp.services.UserService;
import org.benjp.utils.ChatUtils;
import org.benjp.utils.PropertyManager;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.jcr.util.IdGenerator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class ChatServiceImpl extends AbstractJCRService implements ChatService
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

  public void write(String message, String user, String room, String isSystem) {
    write(message, user, room, isSystem, null);
  }

  public void write(String message, String user, String room, String isSystem, String options) {
    message = StringUtils.chomp(message);
    message = message.replaceAll("&", "&#38");
    message = message.replaceAll("<", "&lt;");
    message = message.replaceAll(">", "&gt;");
    message = message.replaceAll("\"", "&quot;");
    message = message.replaceAll("\n", "<br/>");
    message = message.replaceAll("\\\\", "&#92");
    if (options!=null)
    {
      options = options.replaceAll("<", "&lt;");
      options = options.replaceAll(">", "&gt;");
    }

    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node roomNode = getRoom(room, session);

      String id = IdGenerator.generate();
      Node messageNode = roomNode.addNode(id, MESSAGE_NODETYPE);
      messageNode.setProperty(USER_PROPERTY, user);
      messageNode.setProperty(MESSAGE_PROPERTY, message);
      messageNode.setProperty(TIME_PROPERTY, Calendar.getInstance());
      messageNode.setProperty(TIMESTAMP_PROPERTY, System.currentTimeMillis());
      messageNode.setProperty(IS_SYSTEM_PROPERTY, isSystem);
      if (options!=null) messageNode.setProperty(OPTIONS_PROPERTY, options);
      session.save();

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    this.updateRoomTimestamp(room);

  }

  public void delete(String room, String user, String messageId) {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node roomNode = getRoom(room, session);

      if (roomNode.hasNode(messageId))
      {
        Node messageNode = roomNode.getNode(messageId);
        if (messageNode.hasProperty(USER_PROPERTY))
        {
          String msgUser = messageNode.getProperty(USER_PROPERTY).getString();
          if (msgUser.equals(user))
          {
            messageNode.setProperty(MESSAGE_PROPERTY, TYPE_DELETED);
            messageNode.setProperty(TYPE_PROPERTY, TYPE_DELETED);
            messageNode.save();
            session.save();
          }
        }
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public void edit(String room, String user, String messageId, String message) {
    message = StringUtils.chomp(message);
    message = message.replaceAll("&", "&#38");
    message = message.replaceAll("<", "&lt;");
    message = message.replaceAll(">", "&gt;");
    message = message.replaceAll("\"", "&quot;");
    message = message.replaceAll("\n", "<br/>");
    message = message.replaceAll("\\\\", "&#92");

    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node roomNode = getRoom(room, session);

      if (roomNode.hasNode(messageId))
      {
        Node messageNode = roomNode.getNode(messageId);
        if (messageNode.hasProperty(USER_PROPERTY))
        {
          String msgUser = messageNode.getProperty(USER_PROPERTY).getString();
          if (msgUser.equals(user))
          {
            messageNode.setProperty(MESSAGE_PROPERTY, message);
            messageNode.setProperty(TYPE_PROPERTY, TYPE_EDITED);
            messageNode.save();
            session.save();
          }
        }
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

  }

  public String read(String room, UserService userService) {
    return read(room, userService, false, null);
  }

  public String read(String room, UserService userService, boolean isTextOnly, Long fromTimestamp) {
    StringBuilder sb = new StringBuilder();

    SimpleDateFormat formatter = new SimpleDateFormat("hh:mm aaa");
    SimpleDateFormat formatterDate = new SimpleDateFormat("dd/MM/yyyy hh:mm aaa");
    // formatter.format();
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    Date today = calendar.getTime();

    try {
      Session session = JCRBootstrap.getSession();
      QueryManager manager = session.getWorkspace().getQueryManager();

      long from = (fromTimestamp!=null) ? fromTimestamp : System.currentTimeMillis() - readMillis;
      StringBuilder statement = new StringBuilder();
      statement.append("SELECT * FROM ").append(MESSAGE_NODETYPE).append(" WHERE ");
      statement.append(" jcr:path like '/chat/rooms/").append(room).append("/%' ");
      statement.append(" AND NOT jcr:path like '/chat/rooms/").append(room).append("/%/%' ");
      statement.append(" AND ").append(TIMESTAMP_PROPERTY).append(" > ").append(from);
      statement.append(" ORDER BY chat:timestamp DESC");
      Query query = manager.createQuery(statement.toString(), Query.SQL);
      int limit = (isTextOnly)?readTotalTxt:readTotalJson;
      ((QueryImpl)query).setLimit(limit);
      NodeIterator nodeIterator = query.execute().getNodes();

      if (nodeIterator.getSize()==0)
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
        while (nodeIterator.hasNext())
        {
          Node msg = nodeIterator.nextNode();
          timestamp = msg.getProperty(TIMESTAMP_PROPERTY).getString();

          if (first) //first element (most recent one)
          {
            if (!isTextOnly)
            {
              sb.append("{\"room\": \"").append(room).append("\",");
              sb.append("\"timestamp\": \"").append(timestamp).append("\",");
              sb.append("\"messages\": [");
            }
          }

          user = msg.getProperty(USER_PROPERTY).getString();
          msgId = msg.getName();
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
            if (msg.hasProperty(TIME_PROPERTY))
            {
              Date date1 = msg.getProperty(TIME_PROPERTY).getDate().getTime();
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
            String message = msg.getProperty(MESSAGE_PROPERTY).getString();
            if (TYPE_DELETED.equals(message)) message = TYPE_DELETED;
            if (msg.hasProperty(IS_SYSTEM_PROPERTY) && msg.getProperty(IS_SYSTEM_PROPERTY).getBoolean())
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
            if (msg.hasProperty(OPTIONS_PROPERTY))
            {
              String options = msg.getProperty(OPTIONS_PROPERTY).getString();
              if (options.startsWith("{"))
                sb.append("\"options\": ").append(options).append(",");
              else
                sb.append("\"options\": \"").append(options).append("\",");
            }
            else
            {
              sb.append("\"options\": \"\",");
            }
            if (msg.hasProperty(TYPE_PROPERTY))
              sb.append("\"type\": \"").append(msg.getProperty(TYPE_PROPERTY).getString()).append("\",");

            sb.append("\"isSystem\": \"").append(msg.getProperty(IS_SYSTEM_PROPERTY).getBoolean()).append("\",");
            sb.append("\"message\": \"").append(msg.getProperty(MESSAGE_PROPERTY).getString()).append("\"}");
          }

          first = false;
        }

        if (!isTextOnly)
        {
          sb.append("]}");
        }

      }


    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return sb.toString();
  }

  private void updateRoomTimestamp(String room)
  {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node roomsNode = session.getRootNode().getNode("chat/"+M_ROOM_PREFIX+M_ROOMS_COLLECTION);

      if (roomsNode.hasNode(room))
      {
        Node roomNode = roomsNode.getNode(room);
        roomNode.setProperty(TIMESTAMP_PROPERTY, System.currentTimeMillis());
        roomNode.save();
        session.save();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }


  public String getSpaceRoom(String space) {
    String room = ChatUtils.getRoomId(space);
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node roomNode = getRoom(room, session, M_ROOM_PREFIX+M_ROOMS_COLLECTION);
      if (!roomNode.hasProperty(SPACE_PROPERTY))
      {
        roomNode.setProperty(SPACE_PROPERTY, space);
        roomNode.setProperty(TYPE_PROPERTY, TYPE_ROOM_SPACE);
        session.save();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return room;
  }

  public String getTeamRoom(String team, String user) {
    String room = ChatUtils.getRoomId(team, user);
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node roomNode = getRoom(room, session, M_ROOM_PREFIX+M_ROOMS_COLLECTION);
      if (!roomNode.hasProperty(TEAM_PROPERTY))
      {
        roomNode.setProperty(TEAM_PROPERTY, team);
        roomNode.setProperty(USER_PROPERTY, user);
        roomNode.setProperty(TYPE_PROPERTY, TYPE_ROOM_TEAM);
        session.save();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return room;
  }

  public String getTeamCreator(String room) {
    if (room.indexOf(ChatService.TEAM_PREFIX)==0)
    {
      room = room.substring(ChatService.TEAM_PREFIX.length());
    }
    String creator = "";
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node roomNode = getRoom(room, session, M_ROOM_PREFIX+M_ROOMS_COLLECTION);
      if (roomNode.hasProperty(USER_PROPERTY))
      {
        creator = roomNode.getProperty(USER_PROPERTY).getString();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return creator;
  }

  public void setRoomName(String room, String name) {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node roomNode = getRoom(room, session, M_ROOM_PREFIX+M_ROOMS_COLLECTION);
      if (roomNode.hasProperty(TEAM_PROPERTY))
      {
        roomNode.setProperty(TEAM_PROPERTY, name);
        session.save();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }


  public String getRoom(List<String> users) {
    Collections.sort(users);
    String room = ChatUtils.getRoomId(users);
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();
      Node roomNode = getRoom(room, session, M_ROOM_PREFIX+M_ROOMS_COLLECTION);
      if (!roomNode.hasProperty(USERS_PROPERTY))
      {
        String[] tabu = new String[users.size()];
        int i=0;
        for (String user:users)
        {
          tabu[i++] = user;
        }
        roomNode.setProperty(USERS_PROPERTY, tabu);
        roomNode.setProperty(TYPE_PROPERTY, TYPE_ROOM_USER);
        session.save();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return room;
  }

  public List<RoomBean> getExistingRooms(String user, boolean withPublic, boolean isAdmin, NotificationService notificationService, TokenService tokenService) {
    List<RoomBean> rooms = new ArrayList<RoomBean>();
    String roomId = null;

    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      QueryManager manager = session.getWorkspace().getQueryManager();

      StringBuilder statement = new StringBuilder();
      statement.append("SELECT * FROM ").append(ROOM_NODETYPE).append(" WHERE ");
      statement.append(" jcr:path like '/chat/room_rooms/%'");
      statement.append(" AND NOT jcr:path like '/chat/room_rooms/%/%'");
      statement.append(" AND ").append(USERS_PROPERTY).append(" = '").append(user).append("'");
      Query query = manager.createQuery(statement.toString(), Query.SQL);
      NodeIterator nodeIterator = query.execute().getNodes();
      while (nodeIterator.hasNext())
      {
        Node roomNode = nodeIterator.nextNode();

        roomId = roomNode.getName();
        long timestamp = -1;
        if (roomNode.hasProperty(TIMESTAMP_PROPERTY)) {
          timestamp = roomNode.getProperty(TIMESTAMP_PROPERTY).getLong();
        }
        Value[] values = roomNode.getProperty(USERS_PROPERTY).getValues();
        List<String> users = new ArrayList<String>();
        for (Value val:values) {
          if (!user.equals(val.getString()))
            users.add(val.getString());
        }
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
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }


    return rooms;
  }

  public RoomsBean getRooms(String user, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, NotificationService notificationService, UserService userService, TokenService tokenService) {
    List<RoomBean> rooms = new ArrayList<RoomBean>();
    List<RoomBean> roomsOffline = new ArrayList<RoomBean>();
    UserBean userBean = userService.getUser(user, true);
    int unreadOffline=0, unreadOnline=0, unreadSpaces=0, unreadTeams=0;

    Collection<String> availableUsers = tokenService.getActiveUsersFilterBy(user, withUsers, withPublic, isAdmin);

    rooms = this.getExistingRooms(user, withPublic, isAdmin, notificationService, tokenService);
    if (isAdmin)
      rooms.addAll(this.getExistingRooms(org.benjp.services.mongodb.UserServiceImpl.SUPPORT_USER, withPublic, isAdmin, notificationService, tokenService));

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
        if (withOffline || (!withOffline && !org.benjp.services.mongodb.UserServiceImpl.STATUS_INVISIBLE.equals(roomBean.getStatus()) && !org.benjp.services.mongodb.UserServiceImpl.STATUS_OFFLINE.equals(roomBean.getStatus())))
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

  public int getNumberOfRooms() {
    int nb = 0;
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node roomsNode = session.getRootNode().getNode("chat/"+M_ROOMS_COLLECTION);

      nb = new Long(roomsNode.getNodes().getSize()).intValue();

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return nb;
  }

  public int getNumberOfMessages() {
    int nb = 0;
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      QueryManager manager = session.getWorkspace().getQueryManager();

      StringBuilder statement = new StringBuilder();
      statement.append("SELECT * FROM ").append(MESSAGE_NODETYPE).append(" WHERE ");
      statement.append(" jcr:path like '/chat/rooms/%'");
      Query query = manager.createQuery(statement.toString(), Query.SQL);
      NodeIterator nodeIterator = query.execute().getNodes();
      nb = new Long(nodeIterator.getSize()).intValue();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return nb;
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


  private Node getRoom(String room, Session session)
  {
    return getRoom(room, session, M_ROOMS_COLLECTION);
  }

  private Node getRoom(String room, Session session, String path)
  {
    Node node = null;
    try
    {
      Node roomsNode = session.getRootNode().getNode("chat/"+path);
      if (roomsNode.hasNode(room))
      {
        node = roomsNode.getNode(room);
      }
      else
      {
        String nt = ROOM_NODETYPE;
        if (path.equals(M_ROOMS_COLLECTION)) nt = "nt:unstructured";
        node = roomsNode.addNode(room, nt);
        session.save();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return node;

  }
}
