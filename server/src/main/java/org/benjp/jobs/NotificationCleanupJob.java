package org.benjp.jobs;

import org.benjp.services.NotificationService;
import org.benjp.services.mongodb.NotificationServiceImpl;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.logging.Logger;

public class NotificationCleanupJob implements Job
{
  Logger log = Logger.getLogger("NotificationCleanupJob");
  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    log.info("Job started");
    NotificationServiceImpl.cleanupNotifications();
    log.info("Job finished");
  }
}
