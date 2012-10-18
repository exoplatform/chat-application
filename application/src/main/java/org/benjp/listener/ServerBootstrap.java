package org.benjp.listener;

import org.benjp.services.UserService;

public class ServerBootstrap {
  private static final UserService userService = new UserService();

  public static UserService getUserService() {
    return userService;
  }
}
