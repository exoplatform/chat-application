package org.exoplatform.chat.listener;

import java.util.logging.Logger;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import com.google.inject.Module;
import org.exoplatform.chat.services.GuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class GuiceManager implements ServletContextListener {

  private static final Logger LOG = Logger.getLogger("GuiceManager");

  private static Injector     injector_;

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    LOG.info("INITIALIZING GUICE");
    GuiceManager.forceNew();
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    LOG.info("CLOSING GUICE");
  }

  public static Injector getInstance() {
    return injector_;
  }

  public static void forceNew() {
    forceNew(null);
  }

  public static void forceNew(Module module) {
    if (injector_ == null) {
      if (module == null) {
        injector_ = Guice.createInjector(new GuiceModule());
      } else {
        injector_ = Guice.createInjector(module);
      }
    }
  }
}
