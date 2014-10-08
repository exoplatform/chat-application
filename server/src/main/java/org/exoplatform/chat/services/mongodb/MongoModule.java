package org.exoplatform.chat.services.mongodb;

import com.google.inject.AbstractModule;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.NotificationService;
import org.exoplatform.chat.services.TokenService;
import org.exoplatform.chat.services.UserService;

public class MongoModule extends AbstractModule
{

  @Override
  protected void configure() {
    bind(ChatService.class).to(ChatServiceImpl.class);
    bind(NotificationService.class).to(NotificationServiceImpl.class);
    bind(TokenService.class).to(TokenServiceImpl.class);
    bind(UserService.class).to(UserServiceImpl.class);
  }
}
