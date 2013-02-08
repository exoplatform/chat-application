/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.benjp.services;

import org.benjp.jobs.NotificationCleanupJob;
import org.benjp.utils.PropertyManager;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.Date;
import java.util.logging.Logger;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.CronScheduleBuilder.cronSchedule;

@Named("schedulerService")
@ApplicationScoped
public class SchedulerService
{
  Logger log = Logger.getLogger("SchedulerService");


  public SchedulerService()
  {
    log.info("Start Scheduler");
    startScheduler();
  }

  private void startScheduler()
  {
    SchedulerFactory sf = new StdSchedulerFactory();
    try {
      Scheduler sched = sf.getScheduler();

      JobDetail notificationCleanupJob = newJob(NotificationCleanupJob.class)
              .withIdentity("notificationCleanupJob", "chatServer")
              .build();


      CronTrigger notificationTrigger = newTrigger()
              .withIdentity("notificationTrigger", "chatServer")
              .withSchedule(cronSchedule(PropertyManager.getProperty(PropertyManager.PROPERTY_CRON_NOTIF_CLEANUP)))
              .build();

      sched.scheduleJob(notificationCleanupJob, notificationTrigger);

      sched.start();

      log.info("Scheduler Started");


    } catch (SchedulerException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }

  }
}
