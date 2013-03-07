package org.benjp.listener;

import org.benjp.services.MongoBootstrap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.logging.Logger;

public class ConnectionManager implements ServletContextListener {

  private static MongoBootstrap mongoBootstrap;
  private static Logger log = Logger.getLogger("ConnectionManager");

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    log.info("INITIALIZING MONGODB");
    mongoBootstrap = new MongoBootstrap();
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    log.info("CLOSING MONGODB");
    mongoBootstrap.close();
  }

  public static MongoBootstrap getInstance()
  {
    return mongoBootstrap;
  }
}
