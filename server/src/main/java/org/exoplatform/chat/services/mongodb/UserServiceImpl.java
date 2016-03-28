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

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.listener.GuiceManager;
import org.exoplatform.chat.model.RoomBean;
import org.exoplatform.chat.model.SpaceBean;
import org.exoplatform.chat.model.UserBean;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.UserService;
import org.exoplatform.chat.utils.ChatUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@Named("userService")
@ApplicationScoped
public class UserServiceImpl implements org.exoplatform.chat.services.UserService
{

  private static final Logger LOG = Logger.getLogger("UserService");

  private DB db(String dbName)
  {
    if (StringUtils.isEmpty(dbName)) {
      return ConnectionManager.getInstance().getDB();
    } else {
      return ConnectionManager.getInstance().getDB(dbName);
    }
  }

  public void toggleFavorite(String user, String targetUser, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      List<String> favorites = new ArrayList<String>();
      if (doc.containsField("favorites")) {
        favorites = (List<String>)doc.get("favorites");
      }
      if (favorites.contains(targetUser))
        favorites.remove(targetUser);
      else
        favorites.add(targetUser);

      doc.put("favorites", favorites);
      coll.save(doc, WriteConcern.SAFE);
    }
  }

  public boolean isFavorite(String user, String targetUser, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      if (doc.containsField("favorites")) {
        List<String> favorites = (List<String>)doc.get("favorites");
        if (favorites.contains(targetUser))
          return true;
      }
    }
    return false;
  }

  public void addUserFullName(String user, String fullname, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (!cursor.hasNext())
    {
      BasicDBObject doc = new BasicDBObject();
      doc.put("_id", user);
      doc.put("user", user);
      doc.put("fullname", fullname);
      coll.insert(doc);
    }
    else
    {
      DBObject doc = cursor.next();
      doc.put("fullname", fullname);
      coll.save(doc);

    }
  }

  public void addUserEmail(String user, String email, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (!cursor.hasNext())
    {
      BasicDBObject doc = new BasicDBObject();
      doc.put("_id", user);
      doc.put("user", user);
      doc.put("email", email);
      coll.insert(doc);
    }
    else
    {
      DBObject doc = cursor.next();
      doc.put("email", email);
      coll.save(doc);

    }
  }

  public void setSpaces(String user, List<SpaceBean> spaces, String dbName)
  {
    List<String> spaceIds = new ArrayList<String>();
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);
    for (SpaceBean bean:spaces)
    {
      String room = ChatUtils.getRoomId(bean.getId());
      spaceIds.add(room);


      BasicDBObject query = new BasicDBObject();
      query.put("_id", room);
      DBCursor cursor = coll.find(query);
      if (!cursor.hasNext())
      {
        BasicDBObject doc = new BasicDBObject();
        doc.put("_id", room);
        doc.put("space_id", bean.getId());
        doc.put("displayName", bean.getDisplayName());
        doc.put("groupId", bean.getGroupId());
        doc.put("shortName", bean.getShortName());
        doc.put("type", ChatService.TYPE_ROOM_SPACE);
        coll.insert(doc);
      }
      else
      {
        DBObject doc = cursor.next();
        String displayName = doc.get("displayName").toString();
        if (!bean.getDisplayName().equals(displayName))
        {
          doc.put("_id", room);
          doc.put("displayName", bean.getDisplayName());
          doc.put("groupId", bean.getGroupId());
          doc.put("shortName", bean.getShortName());
          coll.save(doc);
        }
      }


    }
    coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      doc.put("spaces", spaceIds);
      coll.save(doc, WriteConcern.SAFE);
    }
    else
    {
      BasicDBObject doc = new BasicDBObject();
      doc.put("_id", user);
      doc.put("user", user);
      doc.put("spaces", spaceIds);
      coll.insert(doc);
    }
  }

  public void addTeamRoom(String user, String teamRoomId, String dbName) {
    List<String> teamIds = new ArrayList<String>();
    teamIds.add(teamRoomId);
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      if (doc.containsField("teams"))
      {
        List<String> existingTeams = ((List<String>)doc.get("teams"));
        if (!existingTeams.contains(teamRoomId))
          existingTeams.add(teamRoomId);
        doc.put("teams", existingTeams);
      }
      else
      {
        doc.put("teams", teamIds);
      }
      coll.save(doc, WriteConcern.SAFE);
    }
    else
    {
      BasicDBObject doc = new BasicDBObject();
      doc.put("_id", user);
      doc.put("user", user);
      doc.put("teams", teamIds);
      coll.insert(doc);
    }
  }

  public void addTeamUsers(String teamRoomId, List<String> users, String dbName) {
    for (String user:users)
    {
      LOG.info("Team Add : " + user);
      this.addTeamRoom(user, teamRoomId, dbName);
    }
  }

  public void removeTeamUsers(String teamRoomId, List<String> users, String dbName) {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    for (String user:users)
    {
      LOG.info("Team Remove : " + user);
      BasicDBObject query = new BasicDBObject();
      query.put("user", user);
      DBCursor cursor = coll.find(query);
      if (cursor.hasNext())
      {
        DBObject doc = cursor.next();
        if (doc.containsField("teams"))
        {
          List<String> teams = (List<String>)doc.get("teams");
          if (teams.contains(teamRoomId))
          {
            teams.remove(teamRoomId);
            doc.put("teams", teams);
            coll.save(doc, WriteConcern.SAFE);
          }
        }
      }

    }
  }

  private RoomBean getTeam(String teamId, String dbName)
  {
    RoomBean roomBean = null;
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("_id", teamId);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      roomBean = new RoomBean();
      roomBean.setRoom(teamId);
      roomBean.setUser(doc.get("user").toString());
      roomBean.setFullname(doc.get("team").toString());
      if (doc.containsField("timestamp"))
      {
        roomBean.setTimestamp(((Long) doc.get("timestamp")).longValue());
      }
    }

    return roomBean;
  }

  public List<RoomBean> getTeams(String user, String dbName) {
    List<RoomBean> rooms = new ArrayList<RoomBean>();
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();

      List<String> listrooms = ((List<String>)doc.get("teams"));
      if (listrooms!=null)
      {
        for (String room:listrooms)
        {
          rooms.add(getTeam(room, dbName));
        }
      }

    }
    return rooms;
  }

  public RoomBean getRoom(String user, String roomId, String dbName) {
    RoomBean roomBean = new RoomBean();
    roomBean.setRoom(roomId);
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("_id", roomId);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      if (doc.containsField("timestamp"))
      {
        roomBean.setTimestamp(((Long) doc.get("timestamp")).longValue());
      }
      String type = doc.get("type").toString();
      if ("s".equals(type))
      {
        roomBean.setUser(ChatService.SPACE_PREFIX+roomId);
        roomBean.setFullname(doc.get("displayName").toString());
        roomBean.setSpace(true);
      }
      else if ("t".equals(type))
      {
        roomBean.setUser(ChatService.TEAM_PREFIX+roomId);
        roomBean.setFullname(doc.get("team").toString());
        roomBean.setTeam(true);
      }
      else if ("u".equals(type))
      {
        List<String> users = ((List<String>)doc.get("users"));
        users.remove(user);
        String targetUser = users.get(0);
        roomBean.setUser(targetUser);
        roomBean.setFullname(this.getUserFullName(targetUser, dbName));
      }
      else if ("e".equals(type))
      {
        roomBean.setUser(ChatService.EXTERNAL_PREFIX+roomId);
        roomBean.setFullname(doc.get("identifier").toString());
      }
    }

    return roomBean;
  }

  private SpaceBean getSpace(String roomId, String dbName)
  {
    SpaceBean spaceBean = null;
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("_id", roomId);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      spaceBean = new SpaceBean();
      spaceBean.setRoom(roomId);
      spaceBean.setId(doc.get("space_id").toString());
      spaceBean.setDisplayName(doc.get("displayName").toString());
      spaceBean.setGroupId(doc.get("groupId").toString());
      spaceBean.setShortName(doc.get("shortName").toString());
      if (doc.containsField("timestamp"))
      {
        spaceBean.setTimestamp(((Long)doc.get("timestamp")).longValue());
      }
    }

    return spaceBean;
  }

  public List<SpaceBean> getSpaces(String user, String dbName)
  {
    List<SpaceBean> spaces = new ArrayList<SpaceBean>();
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();

      List<String> listspaces = ((List<String>)doc.get("spaces"));
      if (listspaces!=null)
      {
        for (String space:listspaces)
        {
          spaces.add(getSpace(space, dbName));
        }
      }

    }
    return spaces;
  }

  public List<UserBean> getUsers(String roomId, String dbName)
  {
    //removing "space-" prefix
    if (roomId.indexOf(ChatService.SPACE_PREFIX)==0)
    {
      roomId = roomId.substring(ChatService.SPACE_PREFIX.length());
    }
    //removing "team-" prefix
    if (roomId.indexOf(ChatService.TEAM_PREFIX)==0)
    {
      roomId = roomId.substring(ChatService.TEAM_PREFIX.length());
    }
    List<UserBean> users = new ArrayList<UserBean>();
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);

    BasicDBObject spaces = new BasicDBObject("spaces", roomId);
    BasicDBObject teams = new BasicDBObject("teams", roomId);
    ArrayList<BasicDBObject> orList = new ArrayList<BasicDBObject>();
    orList.add(spaces);
    orList.add(teams);
    BasicDBObject query = new BasicDBObject("$or", orList);


    DBCursor cursor = coll.find(query);
    while (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      UserBean userBean = new UserBean();
      userBean.setName(doc.get("user").toString());
      Object prop = doc.get("fullname");
      userBean.setFullname((prop!=null)?prop.toString():"");
      prop = doc.get("email");
      userBean.setEmail((prop!=null)?prop.toString():"");
      prop = doc.get("status");
      userBean.setStatus((prop!=null)?prop.toString():"");
      users.add(userBean);
    }
    return users;
  }
  
  public List<UserBean> getUsersInRoomChatOneToOne(String roomId, String dbName) {
    List<UserBean> users = new ArrayList<UserBean>();
    DBCollection coll = db(dbName).getCollection(M_ROOMS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("_id", roomId);
    DBCursor cursor = coll.find(query);
    while (cursor.hasNext()) {
      DBObject doc = cursor.next();
      Object objectUsers = doc.get("users");
      ArrayList myArrayList = (ArrayList) objectUsers;
      for (int i = 0; i < myArrayList.size(); i++) {
        users.add(getUser(myArrayList.get(i).toString(), dbName));
      }
    }
    return users;
  }
  
  public List<UserBean> getUsers(String filter, boolean fullBean, String dbName) {
    filter = filter.replaceAll(" ", ".*");
    List<UserBean> users = new ArrayList<UserBean>();
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    Pattern regex = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);

    BasicDBObject un = new BasicDBObject("user", regex);
    BasicDBObject fn = new BasicDBObject("fullname", regex);
    ArrayList<BasicDBObject> orList = new ArrayList<BasicDBObject>();
    orList.add(un);
    orList.add(fn);
    BasicDBObject query = new BasicDBObject("$or", orList);

    DBCursor cursor = coll.find(query);
    while (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      UserBean userBean = new UserBean();
      userBean.setName(doc.get("user").toString());
      Object prop = doc.get("fullname");
      userBean.setFullname((prop!=null)?prop.toString():"");
      prop = doc.get("email");
      userBean.setEmail((prop!=null)?prop.toString():"");
      prop = doc.get("status");
      userBean.setStatus((prop!=null)?prop.toString():"");
      users.add(userBean);
    }
    return users;
  }

  @Override
  public List<UserBean> getSuggestionUsers(String currentUserName, String roomId, String filter, String dbName, int limit) {
    Map<String, UserBean> users = new LinkedHashMap<>();

    String originalFilter = filter;
    filter = filter.replaceAll(" ", ".*");
    Pattern regex = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);

    BasicDBObject un = new BasicDBObject("user", regex);
    BasicDBObject fn = new BasicDBObject("fullname", regex);
    List<BasicDBObject> orList = new ArrayList<BasicDBObject>();
    orList.add(un);
    orList.add(fn);
    BasicDBObject filterQuery = new BasicDBObject("$or", orList);

    // Search in team members first
    if (roomId != null && !"".equals(roomId)) {
      // Search in room first
      //removing "space-" prefix
      if (roomId.indexOf(ChatService.SPACE_PREFIX)==0) {
        roomId = roomId.substring(ChatService.SPACE_PREFIX.length());
      }
      //removing "team-" prefix
      if (roomId.indexOf(ChatService.TEAM_PREFIX)==0) {
        roomId = roomId.substring(ChatService.TEAM_PREFIX.length());
      }

      BasicDBObject spaces = new BasicDBObject("spaces", roomId);
      BasicDBObject teams = new BasicDBObject("teams", roomId);
      orList = new ArrayList<BasicDBObject>();
      orList.add(spaces);
      orList.add(teams);
      BasicDBObject teamQuery = new BasicDBObject("$or", orList);

      BasicDBObject query = new BasicDBObject("$and", Arrays.asList(teamQuery, filterQuery));
      searchByQuery(users, dbName, query, limit);
    }

    int remain = limit - users.size();

    // Then search in connection
    if (remain > 0 && currentUserName != null && !"".equals(currentUserName)) {
      // We can not inject IdentityManager to this service here because it threw NPE,
      // maybe Guice container only return this service instance when portal container started
      IdentityManager identityManager = GuiceManager.getInstance().getInstance(IdentityManager.class);
      if (identityManager != null) {
        Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, currentUserName, false);
        if (identity != null && !identity.isDeleted()) {
          try {
            ListAccess<Identity> connections = identityManager.getConnectionsWithListAccess(identity);
            if (connections != null && connections.getSize() > 0) {
              int start = 0;
              int size = connections.getSize();
              while (start < size && users.size() < limit) {
                Identity[] ids = connections.load(0, limit);
                for (Identity id : ids) {
                  Profile profile = identityManager.getProfile(id);
                  if (!users.containsKey(id.getRemoteId()) && isMatch(originalFilter, profile)) {
                    UserBean userBean = new UserBean();
                    userBean.setName(id.getRemoteId());
                    userBean.setEmail(profile.getEmail());
                    userBean.setFullname(profile.getFullName());
                    userBean.setStatus(UserService.STATUS_AVAILABLE);

                    users.put(id.getRemoteId(), userBean);
                  }
                  if (users.size() >= limit) break;
                }
                start += limit;
              }
            }
          } catch (Exception ex) {
            LOG.info(ex.getMessage());
          }
          //
          remain = limit - users.size();
        }
      }
    }

    if (remain > 0) {
      searchByQuery(users, dbName, filterQuery, limit);
    }

    return new LinkedList<>(users.values());
  }

  private void searchByQuery(Map<String, UserBean> users, String dbName, BasicDBObject query, int limit) {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    DBCursor cursor = coll.find(query).sort(new BasicDBObject("fullname", 1));
    while (cursor.hasNext() && users.size() < limit) {
      DBObject doc = cursor.next();
      UserBean userBean = new UserBean();
      userBean.setName(doc.get("user").toString());
      Object prop = doc.get("fullname");
      userBean.setFullname((prop!=null)?prop.toString():"");
      prop = doc.get("email");
      userBean.setEmail((prop!=null)?prop.toString():"");
      prop = doc.get("status");
      userBean.setStatus((prop!=null)?prop.toString():"");
      if (!users.containsKey(userBean.getName())) {
        users.put(userBean.getName(), userBean);
      }
    }
  }

  private boolean isMatch(String keyword, Profile profile) {
    String fullName = profile.getFullName();
    String firstName = (String)profile.getProperty(Profile.FIRST_NAME);
    String lastName = (String)profile.getProperty(Profile.FIRST_NAME);
    return ((fullName != null && fullName.toLowerCase().contains(keyword))
            || (firstName != null && firstName.toLowerCase().contains(keyword))
            || (lastName != null && lastName.toLowerCase().contains(keyword)));
  }

  public String setStatus(String user, String status, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      doc.put("status", status);
      coll.save(doc, WriteConcern.SAFE);
    }
    else
    {
      BasicDBObject doc = new BasicDBObject();
      doc.put("_id", user);
      doc.put("user", user);
      doc.put("status", status);
      coll.insert(doc);
    }
    return status;
  }

  public void setAsAdmin(String user, boolean isAdmin, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      doc.put("isSupportAdmin", isAdmin);
      coll.save(doc, WriteConcern.SAFE);
    }
    else
    {
      BasicDBObject doc = new BasicDBObject();
      doc.put("_id", user);
      doc.put("user", user);
      doc.put("isSupportAdmin", isAdmin);
      coll.insert(doc);
    }
  }

  public boolean isAdmin(String user, String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      Object isAdmin = doc.get("isSupportAdmin");
      return (isAdmin!=null && "true".equals(isAdmin.toString()));
    }
    return false;
  }

  public String getStatus(String user, String dbName)
  {
    String status = STATUS_NONE;
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      if (doc.containsField("status"))
        status = doc.get("status").toString();
      else
        status = setStatus(user, STATUS_AVAILABLE, dbName);
    }
    else
    {
      status = setStatus(user, STATUS_AVAILABLE, dbName);
    }

    return status;
  }

  public String getUserFullName(String user, String dbName)
  {
    String fullname = null;
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      if (doc.get("fullname")!=null)
        fullname = doc.get("fullname").toString();
    }

    return fullname;
  }

  public UserBean getUser(String user, String dbName)
  {
    return getUser(user, false, dbName);
  }

  public UserBean getUser(String user, boolean withFavorites, String dbName)
  {
    UserBean userBean = new UserBean();
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    query.put("user", user);
    DBCursor cursor = coll.find(query);
    if (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      userBean.setName(user);
      if (doc.get("fullname")!=null)
        userBean.setFullname( doc.get("fullname").toString() );
      if (doc.get("email")!=null)
        userBean.setEmail(doc.get("email").toString());
      if (doc.get("status")!=null)
        userBean.setStatus(doc.get("status").toString());
      if (withFavorites)
      {
        if (doc.containsField("favorites")) {
          userBean.setFavorites ((List<String>) doc.get("favorites"));
        }
      }
    }

    return userBean;
  }

  public List<String> getUsersFilterBy(String user, String room, String type, String dbName)
  {
    ArrayList<String> users = new ArrayList<String>();
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    if (ChatService.TYPE_ROOM_SPACE.equals(type))
      query.put("spaces", room);
    else
      query.put("teams", room);
    DBCursor cursor = coll.find(query);
    while (cursor.hasNext())
    {
      DBObject doc = cursor.next();
      String target = doc.get("user").toString();
      if (user==null || !user.equals(target))
        users.add(target);
    }

    return users;
  }

  public int getNumberOfUsers(String dbName)
  {
    DBCollection coll = db(dbName).getCollection(M_USERS_COLLECTION);
    BasicDBObject query = new BasicDBObject();
    DBCursor cursor = coll.find(query);
    return cursor.count();
  }


}
