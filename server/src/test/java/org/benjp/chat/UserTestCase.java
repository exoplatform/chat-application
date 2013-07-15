package org.benjp.chat;

import org.benjp.chat.bootstrap.ServiceBootstrap;
import org.benjp.utils.PropertyManager;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserTestCase extends AbstractChatTestCase
{

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
    String user = "benjamin";
    String token = ServiceBootstrap.getTokenService().getToken(user);
    boolean hasToken = ServiceBootstrap.getTokenService().hasUserWithToken(user, token);
    assertFalse(hasToken);

    ServiceBootstrap.getTokenService().addUser(user, token);
    assertTrue(ServiceBootstrap.getTokenService().hasUserWithToken(user, token));
  }

}
