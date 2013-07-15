package org.benjp.chat;

import org.benjp.chat.bootstrap.ServiceBootstrap;
import org.benjp.listener.ConnectionManager;
import org.benjp.model.UserBean;
import org.benjp.services.TokenService;
import org.benjp.services.UserService;
import org.benjp.utils.PropertyManager;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class UserTestCase extends AbstractChatTestCase
{
  String username = "benjamin";

  @Before
  public void setUp()
  {
    ConnectionManager.getInstance().getDB().getCollection(TokenService.M_TOKENS_COLLECTION).drop();
    ConnectionManager.getInstance().getDB().getCollection(UserService.M_USERS_COLLECTION).drop();

  }

  @Test
  public void testIsEmbed() throws Exception
  {
    log.info("UserTestCase.testIsEmbed");
    String connexion = PropertyManager.PROPERTY_SERVER_TYPE_EMBED;
    String serverType = PropertyManager.getProperty(PropertyManager.PROPERTY_SERVER_TYPE);
    assertTrue(serverType.equals(connexion));
  }

  @Test
  public void testToken() throws Exception
  {
    log.info("UserTestCase.testToken");
    String token = ServiceBootstrap.getTokenService().getToken(username);
    boolean hasToken = ServiceBootstrap.getTokenService().hasUserWithToken(username, token);
    assertFalse(hasToken);

    ServiceBootstrap.getTokenService().addUser(username, token);
    assertTrue(ServiceBootstrap.getTokenService().hasUserWithToken(username, token));
  }

  @Test
  public void testTokenValidity() throws Exception
  {
    log.info("UserTestCase.testTokenValidity");
    String token = ServiceBootstrap.getTokenService().getToken(username);
    ServiceBootstrap.getTokenService().addUser(username, token);

    assertTrue(ServiceBootstrap.getTokenService().hasUserWithToken(username, token));
    assertTrue(ServiceBootstrap.getTokenService().isUserOnline(username));

    Thread.sleep(110);

    assertFalse(ServiceBootstrap.getTokenService().isUserOnline(username));

    ServiceBootstrap.getTokenService().updateValidity(username, token);

    assertTrue(ServiceBootstrap.getTokenService().isUserOnline(username));

  }

  @Test
  public void testUserCreation() throws Exception
  {
    log.info("UserTestCase.testUserCreation");
    String fullname = ServiceBootstrap.getUserService().getUserFullName(username);
    assertNull(fullname);

    ServiceBootstrap.getUserService().addUserFullName(username, "Benjamin Paillereau");
    ServiceBootstrap.getUserService().addUserEmail(username, "bpaillereau@exoplatform.com");

    UserBean user = ServiceBootstrap.getUserService().getUser(username);

    assertEquals("Benjamin Paillereau", user.getFullname());
    assertEquals("bpaillereau@exoplatform.com", user.getEmail());

    ServiceBootstrap.getUserService().setAsAdmin(username, false);

    assertFalse(ServiceBootstrap.getUserService().isAdmin(username));

    assertEquals(1, ServiceBootstrap.getUserService().getNumberOfUsers());

    String token = ServiceBootstrap.getTokenService().getToken("john");
    ServiceBootstrap.getTokenService().addUser("john", token);
    ServiceBootstrap.getUserService().addUserFullName("john", "John Smith");

    token = ServiceBootstrap.getTokenService().getToken("mary");
    ServiceBootstrap.getTokenService().addUser("mary", token);
    ServiceBootstrap.getUserService().addUserFullName("mary", "Mary Williams");

    token = ServiceBootstrap.getTokenService().getToken("james");
    ServiceBootstrap.getTokenService().addUser("james", token);
    ServiceBootstrap.getUserService().addUserFullName("james", "James Potter");

    assertEquals(4, ServiceBootstrap.getUserService().getNumberOfUsers());

    int size = ServiceBootstrap.getTokenService().getActiveUsersFilterBy(username, true, true, false).size();
    assertEquals(3, size);

  }

  @Test
  public void testDemoUser() throws Exception
  {
    log.info("UserTestCase.testDemoUser");
    ServiceBootstrap.getUserService().addUserFullName(username, "Benjamin Paillereau");

    assertFalse(ServiceBootstrap.getTokenService().isDemoUser(username));

  }
}
