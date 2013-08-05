package org.benjp.services.jcr;

import com.google.inject.AbstractModule;
import org.benjp.services.ChatService;
import org.benjp.services.NotificationService;
import org.benjp.services.TokenService;
import org.benjp.services.UserService;

public class JCRModule extends AbstractModule
{

  @Override
  protected void configure() {
//    bind(ChatService.class).to(ChatServiceImpl.class);
    bind(ChatService.class).to(org.benjp.services.mongodb.ChatServiceImpl.class);

    bind(UserService.class).to(UserServiceImpl.class);
    bind(NotificationService.class).to(NotificationServiceImpl.class);
    bind(TokenService.class).to(TokenServiceImpl.class);
  }
}
