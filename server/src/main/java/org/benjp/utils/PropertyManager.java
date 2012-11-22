/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.benjp.utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class PropertyManager {
  private static Properties properties;

  private static final String PROPERTIES_PATH = System.getProperty("catalina.base")+"/conf/chat.properties";

  public static final String PROPERTY_SERVER_HOST = "dbServerHost";
  public static final String PROPERTY_SERVER_PORT = "dbServerPort";
  public static final String PROPERTY_DB_NAME = "dbName";
  public static final String PROPERTY_DB_AUTHENTICATION = "dbAuthentication";
  public static final String PROPERTY_DB_USER = "dbUser";
  public static final String PROPERTY_DB_PASSWORD = "dbPassword";
  public static final String PROPERTY_CHAT_SERVER_URL = "chatServerUrl";
  public static final String PROPERTY_CHAT_PORTAL_PAGE = "chatPortalPage";

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
        properties.setProperty(PROPERTY_CHAT_SERVER_URL, "/chatServer");
        properties.setProperty(PROPERTY_CHAT_PORTAL_PAGE, "/portal/intranet/chat");
      }
    }
    return properties;
  }
}
