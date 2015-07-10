package org.exoplatform.chat.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.exoplatform.chat.services.MigrateService;
 
public class MigrateServiceListener implements ServletContextListener{

  private MigrateService migrateService;

  @Override
  public void contextDestroyed(ServletContextEvent arg0) {
  }
 
  //Run this before web application is started
  @Override
  public void contextInitialized(ServletContextEvent arg0) {
    migrateService = new MigrateService();
    migrateService.migrate();
  }
}