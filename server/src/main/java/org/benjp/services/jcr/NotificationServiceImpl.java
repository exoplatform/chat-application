package org.benjp.services.jcr;

import org.benjp.services.NotificationService;

public class NotificationServiceImpl  extends AbstractJCRService implements NotificationService
{
  public void addNotification(String user, String type, String category, String categoryId, String content, String link) {
  }

  public void setNotificationsAsRead(String user, String type, String category, String categoryId) {
  }

  public int getUnreadNotificationsTotal(String user) {
    return 0;
  }

  public int getUnreadNotificationsTotal(String user, String type, String category, String categoryId) {
    return 0;
  }

  public int getNumberOfNotifications() {
    return 0;
  }

  public int getNumberOfUnreadNotifications() {
    return 0;
  }
}
