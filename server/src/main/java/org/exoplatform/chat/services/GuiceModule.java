package org.exoplatform.chat.services;

import com.google.inject.AbstractModule;
import org.exoplatform.chat.services.*;
import org.exoplatform.chat.services.mongodb.ChatMongoDataStorage;
import org.exoplatform.chat.services.mongodb.NotificationMongoDataStorage;
import org.exoplatform.chat.services.mongodb.TokenServiceImpl;
import org.exoplatform.chat.services.mongodb.UserMongoDataStorage;

public class GuiceModule extends AbstractModule
{

  @Override
  protected void configure() {
    bind(ChatDataStorage.class).to(ChatMongoDataStorage.class);
    bind(ChatService.class).to(ChatServiceImpl.class);
    bind(NotificationDataStorage.class).to(NotificationMongoDataStorage.class);
    bind(NotificationService.class).to(NotificationServiceImpl.class);
    bind(TokenService.class).to(TokenServiceImpl.class);
    bind(UserDataStorage.class).to(UserMongoDataStorage.class);
    bind(UserService.class).to(UserServiceImpl.class);
    bind(RealTimeMessageService.class).to(CometdMessageServiceImpl.class);
  }
}
