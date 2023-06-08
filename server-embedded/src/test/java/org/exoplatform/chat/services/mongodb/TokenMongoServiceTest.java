package org.exoplatform.chat.services.mongodb;

import com.mongodb.client.MongoDatabase;
import org.exoplatform.chat.AbstractChatTestCase;
import org.exoplatform.chat.bootstrap.ServiceBootstrap;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.model.UserBean;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.TokenService;
import org.exoplatform.chat.services.UserService;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TokenMongoServiceTest extends AbstractChatTestCase {

  private MongoDatabase db;

  private UserService userService;

  private ChatService chatService;

  private TokenService tokenService;

  @Before
  public void setUp() {
    db = ConnectionManager.getInstance().getDB();
    db.getCollection(UserMongoDataStorage.M_USERS_COLLECTION).drop();
    db.getCollection(SettingMongoDataStorage.M_SETTINGS_COLLECTION).drop();
  }

  @Test
  public void testGetActiveUsersFilterBy() {
    // Given
    userService = ServiceBootstrap.getUserService();
    chatService = ServiceBootstrap.getChatService();
    tokenService = ServiceBootstrap.getTokenService();

    // When
    userService.addUserFullName("john", "jhon");

    List<String> onlineUsers = new ArrayList<String>();

    for (int i = 1; i <= 35; i++) {
      String userId = "john" + i;
      userService.addUserFullName(userId, userId);
      userService.setStatus(userId, UserService.STATUS_AVAILABLE);

      String tokenId = tokenService.getToken(userId);
      tokenService.addUser(userId, tokenId);

      onlineUsers.add(userId);

      List<String> users = new ArrayList<String>();
      users.add("john");
      users.add(userId);
      String roomId = chatService.getRoom(users);
      chatService.write("foo", userId, roomId, "false");
    }

    // Then
    Map<String, UserBean> availableUsers = tokenService.getActiveUsersFilterBy("john", onlineUsers, true, true, false, 30);
    assertEquals("should be 30",30, availableUsers.size());
  }
}
