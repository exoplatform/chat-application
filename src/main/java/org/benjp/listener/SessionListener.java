package org.benjp.listener;

import org.benjp.services.UserService;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SessionListener implements HttpSessionListener
{

  @Override
  public void sessionCreated(HttpSessionEvent httpSessionEvent) {
//    System.out.println("SESSION CREATED :: "+httpSessionEvent.getSession().getId());
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
    HttpSession session = httpSessionEvent.getSession();
    UserService.removeSession(session.getId());
//    System.out.println("SESSION REMOVED :: "+httpSessionEvent.getSession().getId());
  }
}
