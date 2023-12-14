package org.exoplatform.chat.listener;

import org.exoplatform.chat.services.SchedulerService;
import org.quartz.SchedulerException;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.util.logging.Logger;

public class SchedulerManager implements ServletContextListener {

  private static SchedulerService schedulerService;
  private static final Logger LOG = Logger.getLogger("SchedulerManager");

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    LOG.info("INITIALIZING SCHEDULER");
    schedulerService = new SchedulerService();
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    LOG.info("CLOSING SCHEDULER");

    try {
      schedulerService.shutdown();
    } catch (SchedulerException e) {
      LOG.warning("for some reasons, Scheduler didn't want to stop");
    }
  }
}
