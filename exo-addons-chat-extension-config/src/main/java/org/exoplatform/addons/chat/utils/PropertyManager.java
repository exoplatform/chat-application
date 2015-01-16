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

package org.exoplatform.addons.chat.utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class PropertyManager {
  private static final Logger LOG = Logger.getLogger(PropertyManager.class.getName());

  private static Properties properties;

    private static final String PROPERTIES_PATH;

    static {
        if ((System.getProperty("catalina.base") == null) || System.getProperty("catalina.base").isEmpty()) {
            PROPERTIES_PATH =  System.getProperty("jboss.server.config.dir") + "/gatein/chat.properties";
        }    else {

            PROPERTIES_PATH = System.getProperty("catalina.base") + "/conf/chat.properties";
        }
    }

    public static final String PROPERTY_SYSTEM_PREFIX = "chat.";
  public static final String PROPERTY_PASSPHRASE = "chatPassPhrase";

  public static String getProperty(String key)
  {
    String value = (String)properties().get(key);
    //System.out.println("PROP:"+key+"="+value);
    return value;
  }

  private synchronized static Properties properties()
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
        LOG.warning(e.getMessage());
      }

      overridePropertyIfNotSet(PROPERTY_PASSPHRASE, "chat");
    }
    return properties;
  }

  private static void overridePropertyIfNotSet(String key, String value) {
    if (properties().getProperty(key)==null)
    {
      properties().setProperty(key, value);
    }
    if (System.getProperty(PROPERTY_SYSTEM_PREFIX+key)!=null) {
      properties().setProperty(key, System.getProperty(PROPERTY_SYSTEM_PREFIX+key));
    }

  }

  public static void overrideProperty(String key, String value) {
    properties().setProperty(key, value);
  }
}
