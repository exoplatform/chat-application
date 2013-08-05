package org.benjp.services.jcr;

import org.benjp.model.SpaceBean;
import org.benjp.model.UserBean;
import org.benjp.services.UserService;
import org.benjp.utils.ChatUtils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.ArrayList;
import java.util.List;

public class UserServiceImpl extends AbstractJCRService implements UserService
{
  public void toggleFavorite(String user, String targetUser) {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node usersNode = session.getRootNode().getNode("chat/"+M_USERS_COLLECTION);
      if (usersNode.hasNode(user))
      {
        Node userNode = usersNode.getNode(user);
        ArrayList<String> favorites = new ArrayList<String>();
        boolean hasAlready = false;
        if (userNode.hasProperty(FAVORITES_PROPERTY))
        {
          Value[] values = userNode.getProperty(FAVORITES_PROPERTY).getValues();
          for (Value value:values)
          {
            String fav = value.getString();
            if (!fav.equals(targetUser)) {
              favorites.add(fav);
            }
            else
            {
              hasAlready = true;
            }
          }

        }

        if (!hasAlready)
          favorites.add(targetUser);

        userNode.setProperty(FAVORITES_PROPERTY, (String[])favorites.toArray());
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public boolean isFavorite(String user, String targetUser) {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node usersNode = session.getRootNode().getNode("chat/"+M_USERS_COLLECTION);
      if (usersNode.hasNode(user))
      {
        Node userNode = usersNode.getNode(user);
        if (userNode.hasProperty(FAVORITES_PROPERTY))
        {
          Value[] values = userNode.getProperty(FAVORITES_PROPERTY).getValues();
          for (Value value:values)
          {
            String fav = value.getString();
            if (fav.equals(targetUser))
              return true;
          }
        }
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return false;
  }

  public void addUserFullName(String user, String fullname) {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node usersNode = session.getRootNode().getNode("chat/"+M_USERS_COLLECTION);
      if (usersNode.hasNode(user))
      {
        Node userNode = usersNode.getNode(user);
        userNode.setProperty(FULLNAME_PROPERTY, fullname);
        userNode.save();
        session.save();
      } else {
        Node userNode = usersNode.addNode(user, USER_NODETYPE);
        userNode.setProperty(USER_PROPERTY, user);
        userNode.setProperty(FULLNAME_PROPERTY, fullname);
        session.save();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public void addUserEmail(String user, String email) {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node usersNode = session.getRootNode().getNode("chat/"+M_USERS_COLLECTION);
      if (usersNode.hasNode(user))
      {
        Node userNode = usersNode.getNode(user);
        userNode.setProperty(EMAIL_PROPERTY, email);
        userNode.save();
        session.save();
      } else {
        Node userNode = usersNode.addNode(user, USER_NODETYPE);
        userNode.setProperty(USER_PROPERTY, user);
        userNode.setProperty(EMAIL_PROPERTY, email);
        session.save();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public void setSpaces(String user, List<SpaceBean> spaces) {
    List<String> spaceIds = new ArrayList<String>();
    try
    {
        //get info
      Session session = JCRBootstrap.getSession();
      Node roomsNode = session.getRootNode().getNode("chat/"+M_ROOMS_COLLECTION);
      for (SpaceBean bean:spaces)
      {
        String room = ChatUtils.getRoomId(bean.getDisplayName());
        spaceIds.add(room);

        if (!roomsNode.hasNode(room))
        {
          Node roomNode = roomsNode.addNode(room);
          roomNode.setProperty(ID_PROPERTY, bean.getId());
          roomNode.setProperty(DISPLAY_NAME_PROPERTY, bean.getDisplayName());
          roomNode.setProperty(GROUP_ID_PROPERTY, bean.getGroupId());
          roomNode.setProperty(SHORT_NAME_PROPERTY, bean.getShortName());
          session.save();
        }
        else
        {
          Node roomNode = roomsNode.getNode(room);
          String displayName = roomNode.getProperty(DISPLAY_NAME_PROPERTY).getString();
          if (!bean.getDisplayName().equals(displayName))
          {
            roomNode.setProperty(ID_PROPERTY, bean.getId());
            roomNode.setProperty(DISPLAY_NAME_PROPERTY, bean.getDisplayName());
            roomNode.setProperty(GROUP_ID_PROPERTY, bean.getGroupId());
            roomNode.setProperty(SHORT_NAME_PROPERTY, bean.getShortName());
            roomNode.save();
            session.save();
          }
        }


      }
      String[] sids = new String[spaceIds.size()];
      int i = 0;
      for (String id:spaceIds) {
        sids[i++] = id ;
      }
      Node usersNode = session.getRootNode().getNode("chat/"+M_USERS_COLLECTION);
      if (usersNode.hasNode(user))
      {
        Node userNode = usersNode.getNode(user);
        userNode.setProperty(SPACES_PROPERTY, sids);
        userNode.save();
        session.save();
      }
      else
      {
        Node userNode = usersNode.addNode(user, USER_NODETYPE);
        userNode.setProperty(USER_PROPERTY, user);
        userNode.setProperty(SPACES_PROPERTY, sids);
        session.save();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  private SpaceBean getSpace(String spaceId)
  {
    SpaceBean spaceBean = null;
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();
      Node roomsNode = session.getRootNode().getNode("chat/" + M_ROOMS_COLLECTION);

      if (roomsNode.hasNode(spaceId))
      {
        Node roomNode = roomsNode.getNode(spaceId);
        spaceBean = new SpaceBean();
        spaceBean.setId(spaceId);
        spaceBean.setDisplayName(roomNode.getProperty(DISPLAY_NAME_PROPERTY).getString());
        spaceBean.setGroupId(roomNode.getProperty(GROUP_ID_PROPERTY).getString());
        spaceBean.setShortName(roomNode.getProperty(SHORT_NAME_PROPERTY).getString());
        if (roomNode.hasProperty(TIMESTAMP_PROPERTY))
        {
          spaceBean.setTimestamp(roomNode.getProperty(TIMESTAMP_PROPERTY).getLong());
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return spaceBean;
  }

  public List<SpaceBean> getSpaces(String user) {
    List<SpaceBean> spaces = new ArrayList<SpaceBean>();
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node usersNode = session.getRootNode().getNode("chat/"+M_USERS_COLLECTION);
      if (usersNode.hasNode(user))
      {
        Node userNode = usersNode.getNode(user);

        if (userNode.hasProperty(SPACES_PROPERTY))
        {
          Value[] values = userNode.getProperty(SPACES_PROPERTY).getValues();
          for (Value val:values)
          {
            spaces.add(getSpace(val.getString()));
          }
        }
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return spaces;
  }

  public List<UserBean> getUsers(String spaceId) {
    if (spaceId.indexOf("space-")>-1)
    {
      spaceId = spaceId.substring(6);
    }
    List<UserBean> users = new ArrayList<UserBean>();
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();
      QueryManager manager = session.getWorkspace().getQueryManager();

      StringBuilder statement = new StringBuilder();
      statement.append("SELECT * FROM ").append(USER_NODETYPE).append(" WHERE ");
      statement.append(SPACES_PROPERTY).append(" = '").append(spaceId).append("'");
      Query query = manager.createQuery(statement.toString(), Query.SQL);
      NodeIterator nodeIterator = query.execute().getNodes();

//      System.out.println(statement.toString()+" : "+nodeIterator.getSize());
      while (nodeIterator.hasNext())
      {
        UserBean userBean = new UserBean();
        Node userNode = nodeIterator.nextNode();
        userBean.setName(userNode.getName());
        userBean.setFullname(userNode.hasProperty(FULLNAME_PROPERTY) ? userNode.getProperty(FULLNAME_PROPERTY).getString() : "");
        userBean.setEmail(userNode.hasProperty(EMAIL_PROPERTY) ? userNode.getProperty(EMAIL_PROPERTY).getString() : "");
        userBean.setStatus(userNode.hasProperty(STATUS_PROPERTY) ? userNode.getProperty(STATUS_PROPERTY).getString() : "");
        users.add(userBean);
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return users;
  }

  public String setStatus(String user, String status) {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node usersNode = session.getRootNode().getNode("chat/"+M_USERS_COLLECTION);
      if (usersNode.hasNode(user))
      {
        Node userNode = usersNode.getNode(user);
        userNode.setProperty(STATUS_PROPERTY, status);
        userNode.save();
        session.save();
      } else {
        Node userNode = usersNode.addNode(user, USER_NODETYPE);
        userNode.setProperty(USER_PROPERTY, user);
        userNode.setProperty(STATUS_PROPERTY, status);
        session.save();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return status;
  }

  public void setAsAdmin(String user, boolean isAdmin) {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node usersNode = session.getRootNode().getNode("chat/"+M_USERS_COLLECTION);
      if (usersNode.hasNode(user))
      {
        Node userNode = usersNode.getNode(user);
        userNode.setProperty(IS_SUPPORT_ADMIN_PROPERTY, isAdmin);
        userNode.save();
        session.save();
      } else {
        Node userNode = usersNode.addNode(user, USER_NODETYPE);
        userNode.setProperty(USER_PROPERTY, user);
        userNode.setProperty(IS_SUPPORT_ADMIN_PROPERTY, isAdmin);
        session.save();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public boolean isAdmin(String user) {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node usersNode = session.getRootNode().getNode("chat/"+M_USERS_COLLECTION);
      if (usersNode.hasNode(user))
      {
        Node userNode = usersNode.getNode(user);
        if (userNode.hasProperty(IS_SUPPORT_ADMIN_PROPERTY))
        {
          return  (userNode.getProperty(IS_SUPPORT_ADMIN_PROPERTY).getBoolean());
        }
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return false;
  }

  public String getStatus(String user) {
    String status = STATUS_NONE;
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node usersNode = session.getRootNode().getNode("chat/"+M_USERS_COLLECTION);
      if (usersNode.hasNode(user))
      {
        Node userNode = usersNode.getNode(user);
        if (userNode.hasProperty(STATUS_PROPERTY))
        {
          status = userNode.getProperty(STATUS_PROPERTY).getString();
        }
        else
        {
          status = setStatus(user, STATUS_AVAILABLE);
        }
      }
      else
      {
        status = setStatus(user, STATUS_AVAILABLE);
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return status;
  }

  public String getUserFullName(String user) {
    String fullname = null;
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node usersNode = session.getRootNode().getNode("chat/"+M_USERS_COLLECTION);
      if (usersNode.hasNode(user))
      {
        Node userNode = usersNode.getNode(user);
        if (userNode.hasProperty(FULLNAME_PROPERTY))
        {
          fullname = userNode.getProperty(FULLNAME_PROPERTY).getString();
        }
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return fullname;
  }

  public UserBean getUser(String user) {
    return getUser(user, false);
  }

  public UserBean getUser(String user, boolean withFavorites) {
    UserBean userBean = new UserBean();
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node usersNode = session.getRootNode().getNode("chat/"+M_USERS_COLLECTION);
      if (usersNode.hasNode(user))
      {
        Node userNode = usersNode.getNode(user);
        userBean.setName(user);
        if (userNode.hasProperty(FULLNAME_PROPERTY))
        {
          userBean.setFullname(userNode.getProperty(FULLNAME_PROPERTY).getString());
        }
        if (userNode.hasProperty(EMAIL_PROPERTY))
        {
          userBean.setEmail(userNode.getProperty(EMAIL_PROPERTY).getString());
        }
        if (userNode.hasProperty(STATUS_PROPERTY))
        {
          userBean.setStatus(userNode.getProperty(STATUS_PROPERTY).getString());
        }
        if (withFavorites)
        {
          if (userNode.hasProperty(FAVORITES_PROPERTY))
          {
            List<String> favorites = new ArrayList<String>();
            Value[] values = userNode.getProperty(FAVORITES_PROPERTY).getValues();
            for (Value val:values)
            {
              favorites.add(val.getString());
            }
            userBean.setFavorites(favorites);
          }
        }
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return userBean;
  }

  public List<String> getUsersFilterBy(String user, String space) {
    ArrayList<String> users = new ArrayList<String>();
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();
      QueryManager manager = session.getWorkspace().getQueryManager();

      StringBuilder statement = new StringBuilder();
      statement.append("SELECT * FROM ").append(USER_NODETYPE).append(" WHERE ");
      statement.append(SPACES_PROPERTY).append(" = '").append(space).append("'");
      Query query = manager.createQuery(statement.toString(), Query.SQL);
      NodeIterator nodeIterator = query.execute().getNodes();

//      System.out.println(statement.toString()+" : "+nodeIterator.getSize());
      while (nodeIterator.hasNext())
      {
        Node userNode = nodeIterator.nextNode();
        if (!user.equals(userNode.getName()))
        {
          users.add(userNode.getName());
        }
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return users;
  }

  public int getNumberOfUsers() {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node usersNode = session.getRootNode().getNode("chat/"+M_USERS_COLLECTION);

      return new Long(usersNode.getNodes().getSize()).intValue();

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return 0;
  }
}
