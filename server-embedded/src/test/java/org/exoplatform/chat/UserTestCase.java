package org.exoplatform.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.exoplatform.chat.bootstrap.ServiceBootstrap;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.model.UserBean;
import org.exoplatform.chat.services.UserService;
import org.exoplatform.chat.services.mongodb.UserMongoDataStorage;
import org.exoplatform.chat.utils.PropertyManager;
import org.junit.Before;
import org.junit.Test;

public class UserTestCase extends AbstractChatTestCase
{
  String username = "benjamin";

  @Before
  public void setUp()
  {
    ConnectionManager.getInstance().getDB().getCollection(UserMongoDataStorage.M_USERS_COLLECTION).drop();
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
  }

  @Test
  public void testGetUsers() throws Exception
  {
    log.info("UserTestCase.testGetUsers");
    String fullname = ServiceBootstrap.getUserService().getUserFullName(username);
    assertNull(fullname);

    ServiceBootstrap.getUserService().addUserFullName(username, "Benjamin Paillereau");
    ServiceBootstrap.getUserService().addUserEmail(username, "bpaillereau@exoplatform.com");

    String token = ServiceBootstrap.getTokenService().getToken("john");
    ServiceBootstrap.getTokenService().addUser("john", token);
    ServiceBootstrap.getUserService().addUserFullName("john", "John Smith");

    token = ServiceBootstrap.getTokenService().getToken("mary");
    ServiceBootstrap.getTokenService().addUser("mary", token);
    ServiceBootstrap.getUserService().addUserFullName("mary", "Mary Williams");

    token = ServiceBootstrap.getTokenService().getToken("james");
    ServiceBootstrap.getTokenService().addUser("james", token);
    ServiceBootstrap.getUserService().addUserFullName("james", "James Potter");

    int nbUsers = ServiceBootstrap.getUserService().getUsers("", false).size();
    assertEquals(4, nbUsers);

    int nbJ = ServiceBootstrap.getUserService().getUsers("j", false).size();
    assertEquals(3, nbJ);

    int nbJame = ServiceBootstrap.getUserService().getUsers("jame", false).size();
    assertEquals(1, nbJame);

    int nbBePa = ServiceBootstrap.getUserService().getUsers("be pa", false).size();
    assertEquals(1, nbBePa);

    int nbBePaUC = ServiceBootstrap.getUserService().getUsers("BE PA", false).size();
    assertEquals(1, nbBePaUC);

  }

  @Test
  public void testDemoUser() throws Exception
  {
    log.info("UserTestCase.testDemoUser");
    ServiceBootstrap.getUserService().addUserFullName(username, "Benjamin Paillereau");

    assertFalse(ServiceBootstrap.getTokenService().isDemoUser(username));

  }

  @Test
  public void testFavorites() throws Exception
  {
    log.info("UserTestCase.testFavorites");
    ServiceBootstrap.getUserService().addUserFullName(username, "Benjamin Paillereau");
    ServiceBootstrap.getUserService().addUserFullName("john", "John Smith");
    ServiceBootstrap.getUserService().addUserFullName("mary", "Mary Williams");

    assertFalse(ServiceBootstrap.getUserService().isFavorite(username, "john"));

    ServiceBootstrap.getUserService().toggleFavorite(username, "john");
    assertTrue(ServiceBootstrap.getUserService().isFavorite(username, "john"));
    assertFalse(ServiceBootstrap.getUserService().isFavorite(username, "mary"));

    ServiceBootstrap.getUserService().toggleFavorite(username, "john");
    assertFalse(ServiceBootstrap.getUserService().isFavorite(username, "john"));
    assertFalse(ServiceBootstrap.getUserService().isFavorite(username, "mary"));

    ServiceBootstrap.getUserService().toggleFavorite(username, "john");
    ServiceBootstrap.getUserService().toggleFavorite(username, "mary");

    UserBean user = ServiceBootstrap.getUserService().getUser(username);
    assertNull(user.getFavorites());

    user = ServiceBootstrap.getUserService().getUser(username, true);
    assertEquals(2, user.getFavorites().size());

  }

  @Test
  public void testStatus()
  {
    log.info("UserTestCase.testStatus");
    UserService userService = ServiceBootstrap.getUserService();
    userService.addUserFullName(username, "Benjamin Paillereau");
    userService.addUserFullName("john", "John Smith");

    assertEquals(UserService.STATUS_AVAILABLE, userService.getStatus(username));

    userService.setStatus(username, UserService.STATUS_DONOTDISTURB);
    assertEquals(UserService.STATUS_DONOTDISTURB, userService.getStatus(username));

    assertEquals(UserService.STATUS_AVAILABLE, userService.getStatus("john"));

    userService.setStatus(username, UserService.STATUS_AWAY);
    assertEquals(UserService.STATUS_AWAY, userService.getStatus(username));

    userService.setStatus(username, UserService.STATUS_INVISIBLE);
    assertEquals(UserService.STATUS_INVISIBLE, userService.getStatus(username));

  }

  @Test
  public void testAdmin()
  {
    log.info("UserTestCase.testAdmin");
    UserService userService = ServiceBootstrap.getUserService();
    userService.addUserFullName(username, "Benjamin Paillereau");
    userService.addUserFullName("john", "John Smith");

    assertFalse(userService.isAdmin(username));

    userService.setAsAdmin(username, true);

    assertTrue(userService.isAdmin(username));

  }
}
