package org.exoplatform.chat;

import com.google.inject.AbstractModule;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.transitions.Start;
import org.cometd.bayeux.server.BayeuxServer;
import org.exoplatform.chat.bootstrap.ServiceBootstrap;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.listener.GuiceManager;
import org.exoplatform.chat.model.RealTimeMessageBean;
import org.exoplatform.chat.services.*;
import org.exoplatform.chat.services.mongodb.*;
import org.exoplatform.chat.utils.PropertyManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class AbstractChatTestCase
{
  static Logger log = Logger.getLogger("ChatTestCase");
  private static RunningMongodProcess runningProcess;

  @BeforeClass
  public static void before() throws IOException
  {
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVER_TYPE, "embed");
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVERS_HOSTS, "localhost:27777");
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_TOKEN_VALIDITY, "100");

    ConnectionManager.forceNew();

    GuiceManager.forceNew(new TestModule());
    ServiceBootstrap.forceNew();

    Mongod mongod = Mongod.builder()
            .net(Start.to(Net.class).initializedWith(Net.defaults()
                    .withPort(27777)))
            .build();
    runningProcess = mongod.start(Version.Main.V4_0).current();
  }

  @AfterClass
  public static void after() throws Exception {
    runningProcess.stop();
  }
  /**
   * Guice module allowing to mock services for tests
   */
  private static class TestModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(ChatDataStorage.class).to(ChatMongoDataStorage.class);
      bind(ChatService.class).to(ChatServiceImpl.class);
      bind(NotificationDataStorage.class).to(NotificationMongoDataStorage.class);
      bind(NotificationService.class).to(NotificationServiceImpl.class);
      bind(TokenService.class).to(TokenServiceImpl.class);
      bind(TokenStorage.class).to(TokenMongoService.class);
      bind(UserDataStorage.class).to(UserMongoDataStorage.class);
      bind(UserService.class).to(UserServiceImpl.class);
      bind(SettingDataStorage.class).to(SettingMongoDataStorage.class);
      // mock for RealTimeMessageService
      bind(RealTimeMessageService.class).toInstance(new RealTimeMessageService() {
        @Override
        public void setBayeux(BayeuxServer bayeux) {

        }

        @Override
        public void sendMessage(RealTimeMessageBean realTimeMessageBean, String receiver) {
        }

        @Override
        public void sendMessage(RealTimeMessageBean realTimeMessageBean, List<String> receivers) {
        }

        @Override
        public void sendMessageToAll(RealTimeMessageBean realTimeMessageBean) {
        }
      });
    }
  }
}
