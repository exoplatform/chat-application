package org.exoplatform.chat;

import org.exoplatform.chat.bootstrap.ServiceBootstrap;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.services.NotificationService;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class NotificationTestCase extends AbstractChatTestCase
{
  String user1 = "benjamin";
  String user2 = "john";
  NotificationService notificationService_;

  @Before
  public void setUp()
  {
    ConnectionManager.getInstance().getDB().getCollection(NotificationService.M_NOTIFICATIONS).drop();
    notificationService_ = ServiceBootstrap.getNotificationService();
  }

  @Test
  public void testAddNotif() throws Exception
  {
    int tot1 = notificationService_.getUnreadNotificationsTotal(user1, null);
    int tot2 = notificationService_.getUnreadNotificationsTotal(user2, null);

    assertEquals(0, tot1);
    assertEquals(0, tot2);

    notificationService_.addNotification(user1, user2, "type", "cat", "catid", "content", "link", null);
    notificationService_.addNotification(user1, user2, "type", "cat", "catid", "content2", "link", null);

    tot1 = notificationService_.getUnreadNotificationsTotal(user1, null);
    tot2 = notificationService_.getUnreadNotificationsTotal(user2, null);

    assertEquals(2, tot1);
    assertEquals(0, tot2);

  }

  @Test
  public void testSetAsRead() throws Exception
  {
    notificationService_.addNotification(user1, user2, "type", "cat", "catid", "content", "link", null);
    notificationService_.addNotification(user1, user2, "type", "cat", "catid", "content2", "link", null);
    notificationService_.addNotification(user2, user1, "type", "cat", "catid", "content", "link", null);
    notificationService_.addNotification(user2, user1, "type", "cat", "catid", "content2", "link", null);

    int tot1 = notificationService_.getUnreadNotificationsTotal(user1, null);
    int tot2 = notificationService_.getUnreadNotificationsTotal(user2, null);

    assertEquals(2, tot1);
    assertEquals(2, tot2);

    notificationService_.setNotificationsAsRead(user1, null, null, null, null);

    tot1 = notificationService_.getUnreadNotificationsTotal(user1, null);
    tot2 = notificationService_.getUnreadNotificationsTotal(user2, null);

    assertEquals(0, tot1);
    assertEquals(2, tot2);
  }

  @Test
  public void testSetAsReadByCategory() throws Exception
  {
    notificationService_.addNotification(user1, user2, "type", "cat", "catid", "content", "link", null);
    notificationService_.addNotification(user1, user2, "type", "cat", "catid", "content2", "link", null);

    notificationService_.addNotification(user1, user2, "type", "othercat", "catid", "content", "link", null);
    notificationService_.addNotification(user1, user2, "type", "othercat", "catid", "content2", "link", null);
    notificationService_.addNotification(user1, user2, "type", "othercat", "catid", "content2", "link", null);

    int tot1 = notificationService_.getUnreadNotificationsTotal(user1, null);
    assertEquals(5, tot1);

    notificationService_.setNotificationsAsRead(user1, "type", "cat", "catid", null);
    tot1 = notificationService_.getUnreadNotificationsTotal(user1, null);
    assertEquals(3, tot1);

    notificationService_.setNotificationsAsRead(user1, "type", "othercat", "othercatid", null);
    tot1 = notificationService_.getUnreadNotificationsTotal(user1, null);
    assertEquals(3, tot1);

    notificationService_.setNotificationsAsRead(user1, "type", "othercat", "catid", null);
    tot1 = notificationService_.getUnreadNotificationsTotal(user1, null);
    assertEquals(0, tot1);

  }

  @Test
  public void testTotalByCategory() throws Exception
  {
    notificationService_.addNotification(user1, user2, "type", "cat", "catid", "content", "link", null);
    notificationService_.addNotification(user1, user2, "type", "cat", "catid", "content2", "link", null);

    notificationService_.addNotification(user1, user2, "type", "othercat", "catid", "content", "link", null);
    notificationService_.addNotification(user1, user2, "type", "othercat", "catid", "content2", "link", null);
    notificationService_.addNotification(user1, user2, "type", "othercat", "catid", "content2", "link", null);

    int total = notificationService_.getUnreadNotificationsTotal(user1, null);
    int totcat = notificationService_.getUnreadNotificationsTotal(user1, "type", "cat", "catid", null);
    int totothercat = notificationService_.getUnreadNotificationsTotal(user1, "type", "othercat", "catid", null);
    int totnone = notificationService_.getUnreadNotificationsTotal(user1, "type", "othercat", "othercatid", null);

    assertEquals(5, total);
    assertEquals(2, totcat);
    assertEquals(3, totothercat);
    assertEquals(0, totnone);

  }

}
