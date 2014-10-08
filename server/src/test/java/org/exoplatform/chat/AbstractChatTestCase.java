package org.exoplatform.chat;

import org.exoplatform.chat.bootstrap.ServiceBootstrap;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.listener.GuiceManager;
import org.exoplatform.chat.utils.PropertyManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.logging.Logger;

public class AbstractChatTestCase
{


  static Logger log = Logger.getLogger("ChatTestCase");

  @BeforeClass
  public static void before() throws IOException
  {
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVER_TYPE, "embed");
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVER_PORT, "27777");
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_TOKEN_VALIDITY, "100");

    ConnectionManager.forceNew();
    ConnectionManager.getInstance().getDB("unittest");

    GuiceManager.forceNew();
    ServiceBootstrap.forceNew();
  }

  @AfterClass
  public static void teardown() throws Exception {
    ConnectionManager.getInstance().close();
  }




}
