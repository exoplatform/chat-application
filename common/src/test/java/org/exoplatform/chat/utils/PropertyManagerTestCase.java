package org.exoplatform.chat.utils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PropertyManagerTestCase {

  @Before
  public void before() {
    PropertyManager.forceReload();
  }

  @After
  public void after() {
    PropertyManager.forceReload();
  }

  @Test
  public void testLoadSystemProperties() {
    //exo prefix
    System.setProperty(PropertyManager.EXO_PROPERTY_SYSTEM_PREFIX + PropertyManager.PROPERTY_SERVER_HOST, "localhost");
    //system prefix
    System.setProperty(PropertyManager.PROPERTY_SYSTEM_PREFIX + PropertyManager.PROPERTY_SERVER_PORT, "1234");

    String dbServerHost = PropertyManager.getProperty(PropertyManager.PROPERTY_SERVER_HOST);
    String dbServerPort = PropertyManager.getProperty(PropertyManager.PROPERTY_SERVER_PORT);

    Assert.assertEquals("localhost", dbServerHost);
    Assert.assertEquals("1234", dbServerPort);
  }

  @Test
  public void testoverrideProperty() {
    System.setProperty(PropertyManager.PROPERTY_DB_PASSWORD, "pass");
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_DB_PASSWORD, "pass2");
    String dbPassword = PropertyManager.getProperty(PropertyManager.PROPERTY_DB_PASSWORD);
    Assert.assertEquals("pass2", dbPassword);
    }

  @Test
  public void testDefaultProperties() {
    Assert.assertEquals(PropertyManager.PROPERTY_SERVICE_IMPL_MONGO, PropertyManager.getProperty(PropertyManager.PROPERTY_SERVICES_IMPLEMENTATION));
    Assert.assertEquals("mongo", PropertyManager.getProperty(PropertyManager.PROPERTY_SERVER_TYPE));
    Assert.assertEquals("", PropertyManager.getProperty(PropertyManager.PROPERTY_SERVER_HOST));
    Assert.assertEquals("", PropertyManager.getProperty(PropertyManager.PROPERTY_SERVERS_HOSTS));
    Assert.assertEquals("", PropertyManager.getProperty(PropertyManager.PROPERTY_SERVER_PORT));
    Assert.assertEquals("chat", PropertyManager.getProperty(PropertyManager.PROPERTY_DB_NAME));
    Assert.assertEquals("false", PropertyManager.getProperty(PropertyManager.PROPERTY_DB_AUTHENTICATION));
    Assert.assertEquals("", PropertyManager.getProperty(PropertyManager.PROPERTY_DB_USER));
    Assert.assertEquals("", PropertyManager.getProperty(PropertyManager.PROPERTY_DB_PASSWORD));
    Assert.assertEquals("", PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_SERVER_BASE));
    Assert.assertEquals("/chatServer", PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_SERVER_URL));
    Assert.assertEquals("/portal/intranet/chat", PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_PORTAL_PAGE));
    Assert.assertEquals("60000", PropertyManager.getProperty(PropertyManager.PROPERTY_INTERVAL_SESSION));
    Assert.assertEquals("chat", PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE));
    Assert.assertEquals("0 0/60 * * * ?", PropertyManager.getProperty(PropertyManager.PROPERTY_CRON_NOTIF_CLEANUP));
    Assert.assertEquals("/platform/administrators", PropertyManager.getProperty(PropertyManager.PROPERTY_PUBLIC_ADMIN_GROUP));
    Assert.assertEquals("/platform/users", PropertyManager.getProperty(PropertyManager.PROPERTY_TEAM_ADMIN_GROUP));
    Assert.assertEquals("60000", PropertyManager.getProperty(PropertyManager.PROPERTY_TOKEN_VALIDITY));
    Assert.assertEquals("200", PropertyManager.getProperty(PropertyManager.PROPERTY_READ_TOTAL_JSON));
    Assert.assertEquals("2000", PropertyManager.getProperty(PropertyManager.PROPERTY_READ_TOTAL_TXT));

    Assert.assertEquals("smtp", PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_PROTOCAL));
    Assert.assertEquals("localhost", PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_HOST));
    Assert.assertEquals("25", PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_PORT));
    Assert.assertEquals("", PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_USER));
    Assert.assertEquals("", PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_PASSWORD));
    Assert.assertEquals("eXo Platform", PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_FROM));
    Assert.assertEquals("false", PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_STARTTLS_ENABLE));
    Assert.assertEquals("true", PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_ENABLE_SSL_ENABLE));
    Assert.assertEquals("false", PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_AUTH));
    Assert.assertEquals("", PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_SOCKET_FACTORY_PORT));
    Assert.assertEquals("", PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_SOCKET_FACTORY_CLASS));
    Assert.assertEquals("false", PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_SOCKET_FACTORY_FALLBACK));
    Assert.assertEquals("eXo Platform", PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_SENDER));

    Assert.assertEquals("/rest/state/status/", PropertyManager.getProperty(PropertyManager.PROPERTY_PLF_USER_STATUS_UPDATE_URL));
    Assert.assertEquals("15000", PropertyManager.getProperty(PropertyManager.PROPERTY_REQUEST_TIMEOUT));
    Assert.assertNull(PropertyManager.getProperty("No key"));
  }

}
