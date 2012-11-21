package org.benjp.utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class PropertyManager {
  private static Properties properties;

  private static final String PROPERTIES_PATH = System.getProperty("catalina.base")+"/conf/chat.properties";

  public static final String PROPERTY_SERVER_HOST = "serverHost";
  public static final String PROPERTY_SERVER_PORT = "serverPort";
  public static final String PROPERTY_DB_NAME = "dbName";
  public static final String PROPERTY_DB_AUTHENTICATION = "dbAuthentication";
  public static final String PROPERTY_DB_USER = "dbUser";
  public static final String PROPERTY_DB_PASSWORD = "dbPassword";

  public static String getProperty(String key)
  {
    String value = (String)properties().get(key);
    System.out.println("PROP:"+key+"="+value);
    return value;
  }

  private static Properties properties()
  {
    if (properties==null)
    {
      properties = new Properties();
      InputStream stream = null;
      try
      {
        stream = new FileInputStream(PROPERTIES_PATH);
        properties.load(stream);
        stream.close();
      }
      catch (Exception e)
      {
        properties.setProperty(PROPERTY_SERVER_HOST, "localhost");
        properties.setProperty(PROPERTY_SERVER_PORT, "27017");
        properties.setProperty(PROPERTY_DB_NAME, "chat");
        properties.setProperty(PROPERTY_DB_AUTHENTICATION, "false");
        properties.setProperty(PROPERTY_DB_USER, "");
        properties.setProperty(PROPERTY_DB_PASSWORD, "");
      }
    }
    return properties;
  }
}
