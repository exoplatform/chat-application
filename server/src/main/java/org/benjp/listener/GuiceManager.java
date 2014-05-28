package org.benjp.listener;

import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.benjp.services.mongodb.MongoModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class GuiceManager implements ServletContextListener
{

  private static Logger log = Logger.getLogger("GuiceManager");

  private static Injector injector_;

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent)
  {
    log.info("INITIALIZING GUICE");
    GuiceManager.forceNew();

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
//      if (PropertyManager.PROPERTY_SERVICE_IMPL_MONGO.equals(PropertyManager.getProperty(PropertyManager.PROPERTY_SERVICES_IMPLEMENTATION)))
        injector_ = Guice.createInjector(new MongoModule());
    }
  }
}
