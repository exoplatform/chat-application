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

package org.exoplatform.chat.services;

import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.utils.PropertyManager;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.logging.Logger;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Named("schedulerService")
@ApplicationScoped
public class SchedulerService
{
  private static final Logger LOG = Logger.getLogger("SchedulerService");

  private static Scheduler sched;

  public SchedulerService()
  {
    LOG.info("Start Scheduler");
    startScheduler();
  }

  private void startScheduler()
  {
    SchedulerFactory sf = new StdSchedulerFactory();
    try {

      sched = sf.getScheduler();
      JobDetail notificationCleanupJob;

//      if (PropertyManager.PROPERTY_SERVICE_IMPL_MONGO.equals(PropertyManager.getProperty(PropertyManager.PROPERTY_SERVICES_IMPLEMENTATION)))
      notificationCleanupJob = newJob(org.exoplatform.chat.services.mongodb.NotificationCleanupJob.class)
              .withIdentity("notificationCleanupJobMongo", "chatServer")
              .build();

      CronTrigger notificationTrigger = newTrigger()
              .withIdentity("notificationTrigger", "chatServer")
              .withSchedule(cronSchedule(PropertyManager.getProperty(PropertyManager.PROPERTY_CRON_NOTIF_CLEANUP)))
              .build();

      sched.scheduleJob(notificationCleanupJob, notificationTrigger);

      sched.start();

      LOG.info("Scheduler Started");

      if (PropertyManager.PROPERTY_SERVICE_IMPL_MONGO.equals(PropertyManager.getProperty(PropertyManager.PROPERTY_SERVICES_IMPLEMENTATION)))
      {
        try {
          ConnectionManager.getInstance().ensureIndexes();
          LOG.info("MongoDB Indexes Up to Date");
        } catch (Exception e) {
          LOG.severe("MongoDB Indexes couldn't be created during startup. Chat Extension may be unstable!");
        }
      }

    } catch (SchedulerException e) {
      LOG.warning(e.getMessage());
    }
  }

  public void shutdown() throws SchedulerException {
    sched.shutdown();
  }
}
