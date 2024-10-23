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

package org.exoplatform.addons.chat.listener;

import java.io.*;
import java.net.*;
import java.util.*;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.chat.model.SpaceBean;
import org.exoplatform.chat.model.SpaceBeans;
import org.exoplatform.chat.utils.*;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

public class ServerBootstrap {

  private static final Log LOG = ExoLogger.getLogger(ServerBootstrap.class.getName());

  private static String    serverURL = null;

  private static String    serviceURL = null;

  private static String    serverURI = null;

  public static String getStatus(String username, String token, String targetUser) {
    return callServer("getStatus", "user=" + username + "&targetUser=" + targetUser + "&token=" + token);
  }

  public static String getUsers(String username, String token, String room) {
    return callServer("users", "user=" + username + "&room=" + room + "&token=" + token);
  }

  public static String getRoom(String username, String token, String room) {
    return callServer("getRoom", "user=" + username + "&room=" + room + "&targetUser=" + room + "&token=" + token + "&withDetail=true");
  }

  public static String getUserFullName(String username) {
    return callServer("getUserFullName", "username=" + username);
  }

  public static String shouldUpdate(String user) {
    return callServer("shouldUpdate", "user=" + user);
  }

  public static void addUser(String username, String token) {
    postServer("addUser", "username=" + username + "&token=" + token);
  }

  public static void logout(String username, String token, String sessionId, boolean uniqueSession) {
    postServer("logout", "username=" + username + "&token=" + token + "&sessionId=" + sessionId + "&uniqueSession=" + uniqueSession);
  }

  public static void setAsAdmin(String username, boolean isAdmin) {
    postServer("setAsAdmin", "username=" + username + "&isAdmin=" + isAdmin);
  }

  public static void addUserFullNameAndEmail(String username, String fullname, String email) {
    try {
      postServer("addUserFullNameAndEmail",
              "username=" + username + "&fullname=" + URLEncoder.encode(ChatUtils.toString(fullname), "UTF-8") + "&email=" + email);
    } catch (IOException e) {
      LOG.error("Error while updating user information for user {} [ {} ]", username, email, e);
    }
  }

  public static void deleteUser(String username) {
    postServer("deleteUser",
            "username=" + username);
  }

  public static void setEnabledUser(String username, Boolean enabled) {
    postServer("setEnabledUser",
            "username=" + username + "&enabled=" + enabled);
  }

  public static void setExternalUser(String username, String external) {
    postServer("setExternalUser",
               "username=" + username + "&external=" + external);
  }

  public static String getToken(String username) {
    String passphrase = PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE);
    String in = username + passphrase;
    String token = MessageDigester.getHash(in);
    return token;
  }

  public static void saveSpaces(String username) {
    try {
      SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
      ListAccess<Space> spacesListAccess = spaceService.getAccessibleSpacesWithListAccess(username);
      List<Space> spaces = Arrays.asList(spacesListAccess.load(0, spacesListAccess.getSize()));
      ArrayList<SpaceBean> beans = new ArrayList<>();
      for (Space space : spaces) {
        SpaceBean spaceBean = new SpaceBean();
        spaceBean.setDisplayName(space.getDisplayName());
        spaceBean.setGroupId(space.getGroupId());
        spaceBean.setId(space.getId());
        spaceBean.setShortName(space.getShortName());
        spaceBean.setPrettyName(space.getPrettyName());
        beans.add(spaceBean);
      }
      setSpaces(username, new SpaceBeans(beans));
    } catch (Exception e) {
      LOG.warn("Error while initializing spaces of User '" + username + "'", e);
    }
  }

  public static void setSpaces(String username, SpaceBeans beans) {
    String params = "username=" + username;
    String serSpaces = "";
    try {
      serSpaces = ChatUtils.toString(beans);
      serSpaces = URLEncoder.encode(serSpaces, "UTF-8");
    } catch (IOException e) {
      LOG.error("Error encoding spaces", e);
    }
    params += "&spaces=" + serSpaces;
    postServer("setSpaces", params);
  }

  private static String callServer(String serviceUri, String params) {
    String serverURLBase = getServiceURL() + "/" + serviceUri;
    String serviceUrl = serverURLBase + "?passphrase=" + PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE) + "&"
        + params;
    String body = null;
    try {
      URL url = new URL(serviceUrl);
      URLConnection con = url.openConnection();
      InputStream in = con.getInputStream();
      String encoding = con.getContentEncoding();
      encoding = encoding == null ? "UTF-8" : encoding;
      body = IOUtils.toString(in, encoding);
      if ("null".equals(body))
        body = null;
    } catch (MalformedURLException e) {
      LOG.error("Malformed URI " + serverURLBase, e);
    } catch (IOException e) {
      LOG.error("Could not establish connection to URL " + serverURLBase, e);
    } catch (Exception e) {
      LOG.error("Error occurred while sending request to " + serverURLBase, e);
    }
    return body;
  }

  private static String postServer(String serviceUri, String params) {
    String serviceUrl = getServiceURL() + "/" + serviceUri;
    String allParams = "passphrase=" + PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE) + "&" + params;
    String body = null;
    OutputStreamWriter writer = null;
    try {
      URL url = new URL(serviceUrl);
      URLConnection con = url.openConnection();
      int timeout = Integer.parseInt(PropertyManager.getProperty(PropertyManager.PROPERTY_REQUEST_TIMEOUT));
      con.setConnectTimeout(timeout);
      con.setReadTimeout(timeout);
      con.setDoOutput(true);

      // envoi de la requête
      writer = new OutputStreamWriter(con.getOutputStream());
      writer.write(allParams);
      writer.flush();

      InputStream in = con.getInputStream();
      String encoding = con.getContentEncoding();
      encoding = encoding == null ? "UTF-8" : encoding;
      body = IOUtils.toString(in, encoding);
      if ("null".equals(body))
        body = null;

    } catch (MalformedURLException e) {
      LOG.error("Malformed URL " + serviceUrl, e);
    } catch (IOException e) {
      LOG.error("Error converting input stream", e);
    } catch (Exception e) {
      LOG.error("Error occurred while sending request to " + serviceUrl, e);
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (Exception e) {
          LOG.error("Error when closing writer", e);
        }
      }
    }
    return body;
  }

  public static String getServerURL() {
    if (StringUtils.isBlank(serverURL)) {
      serverURL = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_FRONT_END_SERVER_URL);
    }
    if (StringUtils.isNotBlank(serverURL) && serverURL.startsWith("http")) {
      return serverURL;
    } else {
      serverURL = ChatUtils.getServerBase() + getServerURI();
      return serverURL;
    }
  }

  public static String getServiceURL() {
    if (serviceURL != null) {
      return serviceURL;
    }
    serviceURL = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_BACKEND_END_SERVER_URL);
    if (StringUtils.isBlank(serviceURL)) {
      serviceURL = getServerURL();
    }
    return serviceURL;
  }

  public static String getServerURI() {
    if (StringUtils.isBlank(serverURI)) {
      serverURI = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_FRONT_END_SERVER_URL);
    }
    return serverURI;
  }

  public static void init(HttpServletRequest request) {
    if (request == null) {
      throw new IllegalArgumentException();
    }
    if (StringUtils.isBlank(ChatUtils.getServerBase())) {
      String scheme = request.getScheme();
      String serverName = request.getServerName();
      int serverPort = request.getServerPort();
      String serverBase = scheme + "://" + serverName;
      if (serverPort != 80) {
        serverBase += ":" + serverPort;
      }
      ChatUtils.initServerBase(serverBase);
    }
  }
}
