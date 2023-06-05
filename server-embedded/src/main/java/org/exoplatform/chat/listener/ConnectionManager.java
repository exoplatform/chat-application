package org.exoplatform.chat.listener;

import org.exoplatform.chat.services.mongodb.MongoBootstrap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.logging.Logger;

public class ConnectionManager implements ServletContextListener {

  private static MongoBootstrap mongoBootstrap;
  private static final Logger LOG = Logger.getLogger("ConnectionManager");

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    LOG.info("INITIALIZING MONGODB");
    mongoBootstrap = new MongoBootstrap();
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    LOG.info("CLOSING MONGODB");
  }

  public static MongoBootstrap getInstance()
  {
    return mongoBootstrap;
  }

  public static MongoBootstrap forceNew()
  {
    LOG.warning("ConnectionManager.forceNew has been used : this should never happen in Production!");
    mongoBootstrap = new MongoBootstrap();
    return mongoBootstrap;
  }
}
