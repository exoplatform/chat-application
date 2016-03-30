package org.exoplatform.chat.services.mongodb;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.NotificationService;
import org.exoplatform.chat.services.TokenService;
import org.exoplatform.chat.services.UserService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.social.core.manager.IdentityManager;

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
