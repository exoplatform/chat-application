package org.benjp.services.jcr;

import org.benjp.model.UserBean;
import org.benjp.services.TokenService;
import org.benjp.utils.MessageDigester;
import org.benjp.utils.PropertyManager;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TokenServiceImpl extends AbstractJCRService implements TokenService
{

  private int validity_ = -1;

  public TokenServiceImpl()
  {
    this.initNodetypes();
    this.initMandatoryNodes();
  }

  public String getToken(String user)
  {
    String passphrase = PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE);
    String in = user+passphrase;
    String token = MessageDigester.getHash(in);
    //System.out.println("getToken :: user="+user+" ; token="+token);
    return token;
  }

  public boolean hasUserWithToken(String user, String token)
  {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node tokensNode = session.getRootNode().getNode("chat/"+M_TOKENS_COLLECTION);
      if (tokensNode.hasNode(user))
      {
        Node tokenNode = tokensNode.getNode(user);
        String tokenUser = tokenNode.getProperty(TOKEN_PROPERTY).getString();
        return token.equals(tokenUser);
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return false;
  }

  public void addUser(String user, String token)
  {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node tokensNode = session.getRootNode().getNode("chat/"+M_TOKENS_COLLECTION);
      if (!tokensNode.hasNode(user))
      {
        Node tokenNode = tokensNode.addNode(user, TOKEN_NODETYPE);
        session.save();
        tokenNode.setProperty(USER_PROPERTY, user);
        tokenNode.setProperty(TOKEN_PROPERTY, token);
        tokenNode.setProperty(VALIDITY_PROPERTY, System.currentTimeMillis());
        tokenNode.setProperty(IS_DEMO_USER_PROPERTY, user.startsWith(ANONIM_USER));
        tokenNode.save();
        session.save();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

  }


  public void updateValidity(String user, String token) {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node tokensNode = session.getRootNode().getNode("chat/"+M_TOKENS_COLLECTION);
      if (tokensNode.hasNode(user))
      {
        Node tokenNode = tokensNode.getNode(user);
        String tokenUser = tokenNode.getProperty(TOKEN_PROPERTY).getString();

        if (token.equals(tokenUser))
        {
          tokenNode.setProperty(VALIDITY_PROPERTY, System.currentTimeMillis());
          tokenNode.save();

        }
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

  }

  public HashMap<String, UserBean> getActiveUsersFilterBy(String user, boolean withUsers, boolean withPublic, boolean isAdmin)
  {
    return getActiveUsersFilterBy(user, withUsers, withPublic, isAdmin, 0);
  }

  public HashMap<String, UserBean> getActiveUsersFilterBy(String user, boolean withUsers, boolean withPublic, boolean isAdmin, int limit)
  {
    HashMap<String, UserBean> users = new HashMap<String, UserBean>();
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();
      QueryManager manager = session.getWorkspace().getQueryManager();

      StringBuilder statement = new StringBuilder();

      statement.append("SELECT * FROM ").append(TOKEN_NODETYPE).append(" WHERE ");
      statement.append(VALIDITY_PROPERTY).append(" > ").append(System.currentTimeMillis()-getValidity());
      if (isAdmin)
      {
        if (withPublic && !withUsers)
        {
          statement.append(" AND ").append(IS_DEMO_USER_PROPERTY).append(" = 'true' ");
        }
        else if (!withPublic && withUsers)
        {
          statement.append(" AND ").append(IS_DEMO_USER_PROPERTY).append(" = 'false' ");
        }
      }
      else
      {
        statement.append(" AND ").append(IS_DEMO_USER_PROPERTY).append(" = '").append(user.startsWith(ANONIM_USER)).append("'");
      }
      Query query = manager.createQuery(statement.toString(), Query.SQL);

      NodeIterator nodeIterator = query.execute().getNodes();

//      System.out.println(statement.toString()+" : "+nodeIterator.getSize());
      while (nodeIterator.hasNext())
      {
        Node node = nodeIterator.nextNode();
        String target = node.getProperty(USER_PROPERTY).getString();
        if (!user.equals(target)) {
          UserBean userBean = new UserBean();
          userBean.setName(target);
          users.put(target, userBean);
        }

      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return users;
  }

  public boolean isUserOnline(String user)
  {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node tokensNode = session.getRootNode().getNode("chat/"+M_TOKENS_COLLECTION);
      if (tokensNode.hasNode(user))
      {
        Node tokenNode = tokensNode.getNode(user);
        if (tokenNode.hasProperty(VALIDITY_PROPERTY))
        {
          long validity = tokenNode.getProperty(VALIDITY_PROPERTY).getLong();
          return (validity > System.currentTimeMillis()-getValidity());
        }
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return false;
  }

  public boolean isDemoUser(String user)
  {
    return user.startsWith(ANONIM_USER);
  }

  private int getValidity() {
    if (validity_==-1)
    {
      validity_ = 25000;
      try
      {
        validity_ = new Integer(PropertyManager.getProperty(PropertyManager.PROPERTY_TOKEN_VALIDITY));
      }
      catch (Exception e)
      {
        //do nothing if exception happens, keep 15000 value (=> statusInterval should set)
      }

    }
    return validity_;
  }


}
