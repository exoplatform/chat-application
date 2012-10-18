package org.benjp.listener;

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
    ServerBootstrap.getUserService().removeSession(session.getId());
//    System.out.println("SESSION REMOVED :: "+httpSessionEvent.getSession().getId());
  }
}
