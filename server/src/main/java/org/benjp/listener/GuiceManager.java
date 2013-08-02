package org.benjp.listener;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.benjp.services.mongodb.MongoModule;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.logging.Logger;

public class GuiceManager implements ServletContextListener
{

  private static Logger log = Logger.getLogger("GuiceManager");

  private static Injector injector_;

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent)
  {
    log.info("INITIALIZING GUICE");
    injector_ = Guice.createInjector(new MongoModule());

  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent)
  {
    log.info("CLOSING GUICE");
  }

  public static Injector getInstance()
  {
    return injector_;
  }

  public static void forceNew()
  {
    if (injector_==null)
    {
      injector_ = Guice.createInjector(new MongoModule());
    }
  }
}
