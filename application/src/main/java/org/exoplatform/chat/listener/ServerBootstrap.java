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

package org.exoplatform.chat.listener;

import org.apache.commons.io.IOUtils;
import org.exoplatform.chat.model.SpaceBeans;
import org.exoplatform.chat.utils.ChatUtils;
import org.exoplatform.chat.utils.MessageDigester;
import org.exoplatform.chat.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.organization.OrganizationService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.logging.Logger;

public class ServerBootstrap {

  static {
    // Register UpdateUserEventListener
    UpdateUserEventListener listener = new UpdateUserEventListener();
    PortalContainer pcontainer = PortalContainer.getInstance();
    OrganizationService oService =
            (OrganizationService) pcontainer.getComponentInstanceOfType(OrganizationService.class);
    oService.getUserHandler().addUserEventListener(listener);
  }

  private static final Logger LOG = Logger.getLogger(ServerBootstrap.class.getName());

  public static String getUserFullName(String username)
  {
    return callServer("getUserFullName", "username="+username);
  }

  public static void addUser(String username, String token)
  {
    postServer("addUser", "username="+username+"&token="+token);
  }

  public static void setAsAdmin(String username, boolean isAdmin)
  {
    postServer("setAsAdmin", "username="+username+"&isAdmin="+isAdmin);
  }

  public static void addUserFullNameAndEmail(String username, String fullname, String email)
  {
    try {
      postServer("addUserFullNameAndEmail", "username=" + username + "&fullname=" + ChatUtils.toString(fullname) + "&email=" + email);
    } catch (IOException e) {
      LOG.warning(e.getMessage());
    }
  }

  public static String getToken(String username)
  {
//    return callServer("getToken", "username="+username+"&tokenOnly=true");
    String passphrase = PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE);
    String in = username+passphrase;
    String token = MessageDigester.getHash(in);
    return token;
  }

  public static void setSpaces(String username, SpaceBeans beans)
  {
    String params = "username="+username;
    String serSpaces = "";
    try {
      serSpaces = ChatUtils.toString(beans);
      serSpaces = URLEncoder.encode(serSpaces);
    } catch (IOException e) {
      LOG.warning(e.getMessage());
    }
    params += "&spaces="+serSpaces;
    postServer("setSpaces", params);
  }

  private static String callServer(String serviceUri, String params)
  {

    String serviceUrl = getServerBase()
            + PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_SERVER_URL)
            +"/"+serviceUri+"?passphrase="+PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE)
            +"&"+params;
    String body = "";
    try {
      URL url = new URL(serviceUrl);
      URLConnection con = url.openConnection();
      InputStream in = con.getInputStream();
      String encoding = con.getContentEncoding();
      encoding = encoding == null ? "UTF-8" : encoding;
      body = IOUtils.toString(in, encoding);
      if ("null".equals(body)) body = null;
    } catch (MalformedURLException e) {
      LOG.warning(e.getMessage());
    } catch (IOException e) {
      LOG.warning(e.getMessage());
    }
    return body;
  }

  private static String postServer(String serviceUri, String params)
  {
    String serviceUrl = getServerBase()
            + PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_SERVER_URL)
            +"/"+serviceUri;
    String allParams = "passphrase="+PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE) + "&" + params;
    String body = "";
    OutputStreamWriter writer = null;
    try {
      URL url = new URL(serviceUrl);
      URLConnection con = url.openConnection();
      con.setDoOutput(true);

      //envoi de la requête
      writer = new OutputStreamWriter(con.getOutputStream());
      writer.write(allParams);
      writer.flush();

      InputStream in = con.getInputStream();
      String encoding = con.getContentEncoding();
      encoding = encoding == null ? "UTF-8" : encoding;
      body = IOUtils.toString(in, encoding);
      if ("null".equals(body)) body = null;

    } catch (MalformedURLException e) {
      LOG.warning(e.getMessage());
    } catch (IOException e) {
      LOG.warning(e.getMessage());
    } finally{
      try{writer.close();}catch(Exception e){}
    }
    return body;
  }

  public static String getServerBase()
  {
    String serverBase = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_SERVER_BASE);
    if ("".equals(serverBase)) {
      HttpServletRequest request = Util.getPortalRequestContext().getRequest();
      String scheme = request.getScheme();
      String serverName = request.getServerName();
      int serverPort= request.getServerPort();
      serverBase = scheme+"://"+serverName;
      if (serverPort!=80) serverBase += ":"+serverPort;
    }

    return serverBase;

  }


}
