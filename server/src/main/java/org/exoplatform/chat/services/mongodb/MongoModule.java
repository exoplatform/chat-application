package org.exoplatform.chat.services.mongodb;

import com.google.inject.AbstractModule;
import org.exoplatform.chat.services.*;

public class MongoModule extends AbstractModule
{

  @Override
  protected void configure() {
    bind(ChatDataStorage.class).to(ChatMongoDataStorage.class);
    bind(ChatService.class).to(ChatServiceImpl.class);
    bind(NotificationService.class).to(NotificationServiceImpl.class);
    bind(TokenService.class).to(TokenServiceImpl.class);
    bind(UserDataStorage.class).to(UserMongoDataStorage.class);
    bind(UserService.class).to(UserServiceImpl.class);
    bind(RealTimeMessageService.class).to(CometdMessageServiceImpl.class);
  }
}
