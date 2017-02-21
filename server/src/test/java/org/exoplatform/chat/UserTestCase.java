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
    boolean hasToken = ServiceBootstrap.getTokenService().hasUserWithToken(username, token, null);
    assertFalse(hasToken);

    ServiceBootstrap.getTokenService().addUser(username, token, null);
    assertTrue(ServiceBootstrap.getTokenService().hasUserWithToken(username, token, null));
  }

  @Test
  public void testUserCreation() throws Exception
  {
    log.info("UserTestCase.testUserCreation");
    String fullname = ServiceBootstrap.getUserService().getUserFullName(username, null);
    assertNull(fullname);

    ServiceBootstrap.getUserService().addUserFullName(username, "Benjamin Paillereau", null);
    ServiceBootstrap.getUserService().addUserEmail(username, "bpaillereau@exoplatform.com", null);

    UserBean user = ServiceBootstrap.getUserService().getUser(username, null);

    assertEquals("Benjamin Paillereau", user.getFullname());
    assertEquals("bpaillereau@exoplatform.com", user.getEmail());

    ServiceBootstrap.getUserService().setAsAdmin(username, false, null);

    assertFalse(ServiceBootstrap.getUserService().isAdmin(username, null));

    assertEquals(1, ServiceBootstrap.getUserService().getNumberOfUsers(null));

    String token = ServiceBootstrap.getTokenService().getToken("john");
    ServiceBootstrap.getTokenService().addUser("john", token, null);
    ServiceBootstrap.getUserService().addUserFullName("john", "John Smith", null);

    token = ServiceBootstrap.getTokenService().getToken("mary");
    ServiceBootstrap.getTokenService().addUser("mary", token, null);
    ServiceBootstrap.getUserService().addUserFullName("mary", "Mary Williams", null);

    token = ServiceBootstrap.getTokenService().getToken("james");
    ServiceBootstrap.getTokenService().addUser("james", token, null);
    ServiceBootstrap.getUserService().addUserFullName("james", "James Potter", null);

    assertEquals(4, ServiceBootstrap.getUserService().getNumberOfUsers(null));
  }

  @Test
  public void testGetUsers() throws Exception
  {
    log.info("UserTestCase.testGetUsers");
    String fullname = ServiceBootstrap.getUserService().getUserFullName(username, null);
    assertNull(fullname);

    ServiceBootstrap.getUserService().addUserFullName(username, "Benjamin Paillereau", null);
    ServiceBootstrap.getUserService().addUserEmail(username, "bpaillereau@exoplatform.com", null);

    String token = ServiceBootstrap.getTokenService().getToken("john");
    ServiceBootstrap.getTokenService().addUser("john", token, null);
    ServiceBootstrap.getUserService().addUserFullName("john", "John Smith", null);

    token = ServiceBootstrap.getTokenService().getToken("mary");
    ServiceBootstrap.getTokenService().addUser("mary", token, null);
    ServiceBootstrap.getUserService().addUserFullName("mary", "Mary Williams", null);

    token = ServiceBootstrap.getTokenService().getToken("james");
    ServiceBootstrap.getTokenService().addUser("james", token, null);
    ServiceBootstrap.getUserService().addUserFullName("james", "James Potter", null);

    int nbUsers = ServiceBootstrap.getUserService().getUsers("", false, null).size();
    assertEquals(4, nbUsers);

    int nbJ = ServiceBootstrap.getUserService().getUsers("j", false, null).size();
    assertEquals(3, nbJ);

    int nbJame = ServiceBootstrap.getUserService().getUsers("jame", false, null).size();
    assertEquals(1, nbJame);

    int nbBePa = ServiceBootstrap.getUserService().getUsers("be pa", false, null).size();
    assertEquals(1, nbBePa);

    int nbBePaUC = ServiceBootstrap.getUserService().getUsers("BE PA", false, null).size();
    assertEquals(1, nbBePaUC);

  }

  @Test
  public void testDemoUser() throws Exception
  {
    log.info("UserTestCase.testDemoUser");
    ServiceBootstrap.getUserService().addUserFullName(username, "Benjamin Paillereau", null);

    assertFalse(ServiceBootstrap.getTokenService().isDemoUser(username));

  }

  @Test
  public void testFavorites() throws Exception
  {
    log.info("UserTestCase.testFavorites");
    ServiceBootstrap.getUserService().addUserFullName(username, "Benjamin Paillereau", null);
    ServiceBootstrap.getUserService().addUserFullName("john", "John Smith", null);
    ServiceBootstrap.getUserService().addUserFullName("mary", "Mary Williams", null);

    assertFalse(ServiceBootstrap.getUserService().isFavorite(username, "john", null));

    ServiceBootstrap.getUserService().toggleFavorite(username, "john", null);
    assertTrue(ServiceBootstrap.getUserService().isFavorite(username, "john", null));
    assertFalse(ServiceBootstrap.getUserService().isFavorite(username, "mary", null));

    ServiceBootstrap.getUserService().toggleFavorite(username, "john", null);
    assertFalse(ServiceBootstrap.getUserService().isFavorite(username, "john", null));
    assertFalse(ServiceBootstrap.getUserService().isFavorite(username, "mary", null));

    ServiceBootstrap.getUserService().toggleFavorite(username, "john", null);
    ServiceBootstrap.getUserService().toggleFavorite(username, "mary", null);

    UserBean user = ServiceBootstrap.getUserService().getUser(username, null);
    assertNull(user.getFavorites());

    user = ServiceBootstrap.getUserService().getUser(username, true, null);
    assertEquals(2, user.getFavorites().size());

  }

  @Test
  public void testStatus()
  {
    log.info("UserTestCase.testStatus");
    UserService userService = ServiceBootstrap.getUserService();
    userService.addUserFullName(username, "Benjamin Paillereau", null);
    userService.addUserFullName("john", "John Smith", null);

    assertEquals(UserService.STATUS_AVAILABLE, userService.getStatus(username, null));

    userService.setStatus(username, UserService.STATUS_DONOTDISTURB, null);
    assertEquals(UserService.STATUS_DONOTDISTURB, userService.getStatus(username, null));

    assertEquals(UserService.STATUS_AVAILABLE, userService.getStatus("john", null));

    userService.setStatus(username, UserService.STATUS_AWAY, null);
    assertEquals(UserService.STATUS_AWAY, userService.getStatus(username, null));

    userService.setStatus(username, UserService.STATUS_INVISIBLE, null);
    assertEquals(UserService.STATUS_INVISIBLE, userService.getStatus(username, null));

  }

  @Test
  public void testAdmin()
  {
    log.info("UserTestCase.testAdmin");
    UserService userService = ServiceBootstrap.getUserService();
    userService.addUserFullName(username, "Benjamin Paillereau", null);
    userService.addUserFullName("john", "John Smith", null);

    assertFalse(userService.isAdmin(username, null));

    userService.setAsAdmin(username, true, null);

    assertTrue(userService.isAdmin(username, null));

  }
}
