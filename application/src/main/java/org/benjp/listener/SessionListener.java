package org.benjp.listener;

import org.benjp.services.UserService;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.net.UnknownHostException;

public class SessionListener implements HttpSessionListener
{

  @Override
  public void sessionCreated(HttpSessionEvent httpSessionEvent) {
//    System.out.println("SESSION CREATED :: "+httpSessionEvent.getSession().getId());
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
    HttpSession session = httpSessionEvent.getSession();
    try {
      new UserService().removeSession(session.getId());
    } catch (UnknownHostException e) {

    }
//    System.out.println("SESSION REMOVED :: "+httpSessionEvent.getSession().getId());
  }
}
