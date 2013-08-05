package org.benjp.services.jcr;

import org.benjp.services.NotificationService;
import org.exoplatform.services.jcr.util.IdGenerator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

public class NotificationServiceImpl  extends AbstractJCRService implements NotificationService
{
  public void addNotification(String user, String type, String category, String categoryId, String content, String link)
  {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node notifsNode = session.getRootNode().getNode(M_NOTIFICATIONS);
      String id = IdGenerator.generate();
      Node notifNode = notifsNode.addNode(id, NOTIF_NODETYPE);
      notifNode.setProperty(TIMESTAMP_PROPERTY, System.currentTimeMillis());
      notifNode.setProperty(USER_PROPERTY, user);
      notifNode.setProperty(TYPE_PROPERTY, type);
      notifNode.setProperty(CATEGORY_PROPERTY, category);
      notifNode.setProperty(CATEGORY_ID_PROPERTY, categoryId);
      notifNode.setProperty(CONTENT_PROPERTY, category);
      notifNode.setProperty(LINK_PROPERTY, link);
      notifNode.setProperty(IS_READ_PROPERTY, false);
      session.save();

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

  }

  public void setNotificationsAsRead(String user, String type, String category, String categoryId)
  {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();
      QueryManager manager = session.getWorkspace().getQueryManager();

      StringBuilder statement = new StringBuilder();

      statement.append("SELECT * FROM ").append(NOTIF_NODETYPE).append(" WHERE ");
      statement.append(USER_PROPERTY).append(" = '").append(user).append("' ");
      if (categoryId!=null)
        statement.append(" AND ").append(CATEGORY_ID_PROPERTY).append(" = '").append(categoryId).append("' ");
      if (category!=null)
        statement.append(" AND ").append(CATEGORY_PROPERTY).append(" = '").append(category).append("' ");
      if (type!=null)
        statement.append(" AND ").append(TYPE_PROPERTY).append(" = '").append(type).append("' ");

      Query query = manager.createQuery(statement.toString(), Query.SQL);

      NodeIterator nodeIterator = query.execute().getNodes();

//      System.out.println(statement.toString()+" : "+nodeIterator.getSize());
      while (nodeIterator.hasNext())
      {
        Node node = nodeIterator.nextNode();
        node.setProperty(IS_READ_PROPERTY, true);
        node.save();
        session.save();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

  }

  public int getUnreadNotificationsTotal(String user)
  {
    return getUnreadNotificationsTotal(user, null, null, null);
  }

  public int getUnreadNotificationsTotal(String user, String type, String category, String categoryId)
  {
    int total = -1;
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();
      QueryManager manager = session.getWorkspace().getQueryManager();

      StringBuilder statement = new StringBuilder();

      statement.append("SELECT * FROM ").append(NOTIF_NODETYPE).append(" WHERE ");
      statement.append(USER_PROPERTY).append(" = '").append(user).append("' ");
      statement.append(" AND ").append(IS_READ_PROPERTY).append(" = 'false' ");
      if (categoryId!=null)
        statement.append(" AND ").append(CATEGORY_ID_PROPERTY).append(" = '").append(categoryId).append("' ");
      if (category!=null)
        statement.append(" AND ").append(CATEGORY_PROPERTY).append(" = '").append(category).append("' ");
      if (type!=null)
        statement.append(" AND ").append(TYPE_PROPERTY).append(" = '").append(type).append("' ");

      Query query = manager.createQuery(statement.toString(), Query.SQL);

      NodeIterator nodeIterator = query.execute().getNodes();

      total = Integer.parseInt(""+nodeIterator.getSize());
//      System.out.println(statement.toString()+" : "+nodeIterator.getSize());

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return total;
  }

  public int getNumberOfNotifications()
  {
    int total = -1;
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();
      QueryManager manager = session.getWorkspace().getQueryManager();

      StringBuilder statement = new StringBuilder();

      statement.append("SELECT * FROM ").append(NOTIF_NODETYPE);

      Query query = manager.createQuery(statement.toString(), Query.SQL);

      NodeIterator nodeIterator = query.execute().getNodes();

      total = Integer.parseInt(""+nodeIterator.getSize());
//      System.out.println(statement.toString()+" : "+nodeIterator.getSize());

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return total;
  }

  public int getNumberOfUnreadNotifications()
  {
    int total = -1;
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();
      QueryManager manager = session.getWorkspace().getQueryManager();

      StringBuilder statement = new StringBuilder();

      statement.append("SELECT * FROM ").append(NOTIF_NODETYPE);
      //statement.append(" WHERE ").append(IS_READ_PROPERTY).append(" = 'false'");

      Query query = manager.createQuery(statement.toString(), Query.SQL);

      NodeIterator nodeIterator = query.execute().getNodes();

      total = Integer.parseInt(""+nodeIterator.getSize());
//      System.out.println(statement.toString()+" : "+nodeIterator.getSize());

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return total;
  }
}
