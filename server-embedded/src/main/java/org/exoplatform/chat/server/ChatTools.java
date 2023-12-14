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
package org.exoplatform.chat.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.listener.GuiceManager;
import org.exoplatform.chat.model.RealTimeMessageBean;
import org.exoplatform.chat.model.SpaceBeans;
import org.exoplatform.chat.model.UserBean;
import org.exoplatform.chat.services.ChatException;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.NotificationService;
import org.exoplatform.chat.services.RealTimeMessageService;
import org.exoplatform.chat.services.TokenService;
import org.exoplatform.chat.services.UserService;
import org.exoplatform.chat.utils.ChatUtils;
import org.exoplatform.chat.utils.PropertyManager;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

abstract class ChatTools extends HttpServlet {

  protected static final String UNIQUE_SESSION_PARAM             = "uniqueSession";

  protected static final String SESSION_ID_PARAM                 = "sessionId";

  protected static final String PASSPHRASE_DOESN_T_MATCH_MESSAGE = "{ \"message\": \"passphrase doesn't match\"}";

  protected static final String PASSPHRASE_PARAM                 = "passphrase";

  protected static final String USERNAME_PARAM                   = "username";

  protected static final String MIME_TYPE_JSON                   = "application/json";

  protected static final String MIME_TYPE_TEXT                   = "text/plain";

  protected static final String MESSAGES_PARAM                   = "messages";

  protected static final String UPDATED_MESSAGE                  = "Updated!";

  protected static final String ONLINE_ONLY_PARAM                = "onlineOnly";

  protected static final String STATUS_PARAM                     = "status";

  protected static final String DATA_PARAM                       = "data";

  protected static final String WITH_DETAILS_PARAM               = "withDetails";

  protected static final String EVENT_PARAM                      = "event";

  protected static final String ENABLED_PARAM                    = "enabled";

  protected static final String SPACE_ID_PARAM                   = "spaceId";

  protected static final String START_TIME_PARAM                 = "startTime";

  protected static final String START_PARAM                      = "start";

  protected static final String USERS_PARAM                      = "users";

  protected static final String TEAM_NAME_PARAM                  = "teamName";

  protected static final String TYPE_PARAM                       = "type";

  protected static final String WITH_DETAIL_PARAM                = "withDetail";

  protected static final String TIME_PARAM                       = "time";

  protected static final String NOTIF_CONDITION_TYPE_PARAM       = "notifConditionType";

  protected static final String NOTIF_CONDITION_PARAM            = "notifCondition";

  protected static final String NOTIF_MANNER_PARAM               = "notifManner";

  protected static final String FAVORITE_PARAM                   = "favorite";

  protected static final String MESSAGE_ID_PARAM                 = "messageId";

  protected static final String PORTAL_URI_PARAM                 = "portalURI";

  protected static final String IS_TEXT_ONLY_PARAM               = "isTextOnly";

  protected static final String TO_TIMESTAMP_PARAM               = "toTimestamp";

  protected static final String FROM_TIMESTAMP_PARAM             = "fromTimestamp";

  protected static final String OPTIONS_PARAM                    = "options";

  protected static final String IS_SYSTEM_PARAM                  = "isSystem";

  protected static final String ROOM_PARAM                       = "room";

  protected static final String MESSAGE_PARAM                    = "message";

  protected static final String SENDER_PARAM                     = "sender";

  protected static final String IS_EXTERNAL_PARAM                = "isExternal";

  protected static final String IS_ENABLED_PARAM                 = "isEnabled";

  protected static final String IS_DELETED_PARAM                 = "isDeleted";

  protected static final String TARGET_USER_PARAM                = "targetUser";

  protected static final String ROOM_TYPE_PARAM                  = "roomType";

  protected static final String LIMIT_PARAM                      = "limit";

  protected static final String OFFSET_PARAM                     = "offset";

  protected static final String FILTER_PARAM                     = "filter";

  protected static final String ONLINE_USERS_PARAM               = "onlineUsers";

  protected static final String USER_PARAM                       = "user";

  protected static final String ROOM_ID                          = "roomId";

  protected static final String TOKEN_PARAM                      = "token";

  private static final long     serialVersionUID                 = 3942640732294577324L;

  private static final Logger   LOG                              = Logger.getLogger("ChatTools");

  ChatService                   chatService;                                                                      // NOSONAR

  UserService                   userService;                                                                      // NOSONAR

  TokenService                  tokenService;                                                                     // NOSONAR

  NotificationService           notificationService;                                                              // NOSONAR

  RealTimeMessageService        realTimeMessageService;                                                           // NOSONAR

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    chatService = GuiceManager.getInstance().getInstance(ChatService.class);
    userService = GuiceManager.getInstance().getInstance(UserService.class);
    tokenService = GuiceManager.getInstance().getInstance(TokenService.class);
    notificationService = GuiceManager.getInstance().getInstance(NotificationService.class);
    realTimeMessageService = GuiceManager.getInstance().getInstance(RealTimeMessageService.class);
  }

  protected void addUser(HttpServletRequest request, HttpServletResponse response) {
    String username = request.getParameter(USERNAME_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String passphrase = request.getParameter(PASSPHRASE_PARAM);
    if (!checkPassphrase(passphrase)) {
      writeJsonResponse(response, PASSPHRASE_DOESN_T_MATCH_MESSAGE, HttpStatus.SC_NOT_FOUND);
      return;
    }

    tokenService.addUser(username, token);
    writeTextResponse(response, "OK");
  }

  protected void logout(HttpServletRequest request, HttpServletResponse response) {
    String username = request.getParameter(USERNAME_PARAM);
    String sessionId = request.getParameter(SESSION_ID_PARAM);
    String passphrase = request.getParameter(PASSPHRASE_PARAM);
    String uniqueSession = request.getParameter(UNIQUE_SESSION_PARAM);
    if (!checkPassphrase(passphrase)) {
      writeJsonResponse(response, PASSPHRASE_DOESN_T_MATCH_MESSAGE, HttpStatus.SC_NOT_FOUND);
      return;
    }

    Map<String, Object> options = new HashMap<>();
    options.put(SESSION_ID_PARAM, sessionId);

    // send logout message to all sessions of the given user to check if their
    // session is closed
    RealTimeMessageBean realTimeMessageBean = new RealTimeMessageBean(RealTimeMessageBean.EventType.LOGOUT_SENT,
                                                                      null,
                                                                      username,
                                                                      new Date(),
                                                                      options);
    realTimeMessageService.sendMessage(realTimeMessageBean, username);

    if (StringUtils.equals(uniqueSession, "true")) {
      // Notify other users about the session logout of user
      Map<String, Object> data = new HashMap<>();
      data.put(STATUS_PARAM, UserService.STATUS_OFFLINE);
      realTimeMessageBean = new RealTimeMessageBean(RealTimeMessageBean.EventType.USER_STATUS_CHANGED,
                                                    username,
                                                    username,
                                                    new Date(),
                                                    data);
      realTimeMessageService.sendMessageToAll(realTimeMessageBean);
    }
    writeTextResponse(response, "OK");
  }

  protected void setAsAdmin(HttpServletRequest request, HttpServletResponse response) {
    String username = request.getParameter(USERNAME_PARAM);
    String isAdmin = request.getParameter("isAdmin");
    String passphrase = request.getParameter(PASSPHRASE_PARAM);
    if (!checkPassphrase(passphrase)) {
      writeJsonResponse(response, PASSPHRASE_DOESN_T_MATCH_MESSAGE, HttpStatus.SC_NOT_FOUND);
      return;
    }

    userService.setAsAdmin(username, "true".equals(isAdmin));

    writeTextResponse(response, "OK");
  }

  protected void addUserFullNameAndEmail(HttpServletRequest request, HttpServletResponse response) {
    String username = request.getParameter(USERNAME_PARAM);
    String fullname = request.getParameter("fullname");
    String email = request.getParameter("email");
    String passphrase = request.getParameter(PASSPHRASE_PARAM);
    if (!checkPassphrase(passphrase)) {
      writeJsonResponse(response, PASSPHRASE_DOESN_T_MATCH_MESSAGE, HttpStatus.SC_NOT_FOUND);
      return;
    }
    try {
      userService.addUserEmail(username, email);
      fullname = (String) ChatUtils.fromString(fullname);
      userService.addUserFullName(username, fullname);
      writeTextResponse(response, "OK");
    } catch (Exception e) {
      LOG.log(Level.SEVERE,
              String.format("The fullName with value %s of the user %s couldn't be serialized : ", fullname, username),
              e);
      writeTextResponse(response, e.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }
  }

  protected void deleteUser(HttpServletRequest request, HttpServletResponse response) {
    String username = request.getParameter(USERNAME_PARAM);
    String passphrase = request.getParameter(PASSPHRASE_PARAM);
    if (!checkPassphrase(passphrase)) {
      writeJsonResponse(response, PASSPHRASE_DOESN_T_MATCH_MESSAGE, HttpStatus.SC_NOT_FOUND);
      return;
    }
    userService.deleteUser(username);
    writeTextResponse(response, "OK");
  }

  protected void setEnabledUser(HttpServletRequest request, HttpServletResponse response) {
    String username = request.getParameter(USERNAME_PARAM);
    String enabled = request.getParameter(ENABLED_PARAM);
    String passphrase = request.getParameter(PASSPHRASE_PARAM);
    if (!checkPassphrase(passphrase)) {
      writeJsonResponse(response, PASSPHRASE_DOESN_T_MATCH_MESSAGE, HttpStatus.SC_NOT_FOUND);
      return;
    }

    Boolean isEnabled = StringUtils.equals(enabled, "true");
    userService.setEnabledUser(username, isEnabled);
    writeTextResponse(response, "OK");
  }

  protected void setExternalUser(HttpServletRequest request, HttpServletResponse response) {
    String username = request.getParameter(USERNAME_PARAM);
    String external = request.getParameter("external");
    String passphrase = request.getParameter(PASSPHRASE_PARAM);
    if (!checkPassphrase(passphrase)) {
      writeJsonResponse(response, PASSPHRASE_DOESN_T_MATCH_MESSAGE, HttpStatus.SC_NOT_FOUND);
      return;
    }

    userService.setExternalUser(username, external);

    writeTextResponse(response, "OK");
  }

  protected void setSpaces(HttpServletRequest request, HttpServletResponse response) {
    String username = request.getParameter(USERNAME_PARAM);
    String spaces = request.getParameter("spaces");
    String passphrase = request.getParameter(PASSPHRASE_PARAM);
    if (!checkPassphrase(passphrase)) {
      writeJsonResponse(response, PASSPHRASE_DOESN_T_MATCH_MESSAGE, HttpStatus.SC_NOT_FOUND);
      return;
    }

    try {
      SpaceBeans spaceBeans = (SpaceBeans) ChatUtils.fromString(spaces);
      userService.setSpaces(username, spaceBeans.getSpaces());
    } catch (IOException | ClassNotFoundException e) {
      LOG.warning(e.getMessage());
    }

    writeTextResponse(response, "OK");
  }

  protected void getUserFullName(HttpServletRequest request, HttpServletResponse response) {
    String username = request.getParameter(USERNAME_PARAM);
    String passphrase = request.getParameter(PASSPHRASE_PARAM);
    if (!checkPassphrase(passphrase)) {
      writeJsonResponse(response, PASSPHRASE_DOESN_T_MATCH_MESSAGE, HttpStatus.SC_NOT_FOUND);
      return;
    }

    String fullname = userService.getUserFullName(username);
    writeTextResponse(response, fullname);
  }

  protected void shouldUpdate(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter("user");
    String passphrase = request.getParameter(PASSPHRASE_PARAM);
    if (!checkPassphrase(passphrase)) {
      writeJsonResponse(response, PASSPHRASE_DOESN_T_MATCH_MESSAGE, HttpStatus.SC_NOT_FOUND);
      return;
    }

    UserBean userBean = userService.getUser(user);
    boolean shouldUpdate = userBean.isEnabled() == null || userBean.isDeleted() == null;
    writeTextResponse(response, String.valueOf(shouldUpdate));
  }

  protected void updateUnreadTestMessages(HttpServletRequest request, HttpServletResponse response) {
    String username = request.getParameter(USERNAME_PARAM);
    String room = request.getParameter("room");
    String passphrase = request.getParameter(PASSPHRASE_PARAM);
    if (!checkPassphrase(passphrase)) {
      writeJsonResponse(response, PASSPHRASE_DOESN_T_MATCH_MESSAGE, HttpStatus.SC_NOT_FOUND);
      return;
    }

    if (username == null) {
      writeJsonResponse(response, "{ \"message\": \"username is null\"}", HttpStatus.SC_NOT_FOUND);
      return;
    }
    if (room == null) {
      writeJsonResponse(response, "{ \"message\": \"room is null\"}", HttpStatus.SC_NOT_FOUND);
      return;
    }
    if (username.startsWith(ChatService.SPACE_PREFIX)) {
      writeTextResponse(response, "OK");
      return;
    }
    if (!room.equals("ALL")) {
      notificationService.setNotificationsAsRead(username, "chat", "room", room);
    } else {
      notificationService.setNotificationsAsRead(username, null, null, null);
    }
    writeTextResponse(response, "OK");
  }

  protected void initDB(HttpServletRequest request, HttpServletResponse response) {
    String db = request.getParameter("db");
    String passphrase = request.getParameter(PASSPHRASE_PARAM);
    if (!checkPassphrase(passphrase)) {
      writeJsonResponse(response, PASSPHRASE_DOESN_T_MATCH_MESSAGE, HttpStatus.SC_NOT_FOUND);
      return;
    }

    if (db == null) {
      writeJsonResponse(response, "{ \"message\": \"db is null\"}", HttpStatus.SC_NOT_FOUND);
      return;
    }

    ConnectionManager.getInstance().getDB(db);

    StringBuilder data = new StringBuilder();
    data.append("{");
    data.append(" \"message\": \"using db=" + db + "\"");
    data.append("}");
    writeJsonResponse(response, data.toString());
  }

  protected void ensureIndexes(HttpServletRequest request, HttpServletResponse response) {
    String db = request.getParameter("db");
    String passphrase = request.getParameter(PASSPHRASE_PARAM);
    if (!checkPassphrase(passphrase)) {
      writeJsonResponse(response, PASSPHRASE_DOESN_T_MATCH_MESSAGE, HttpStatus.SC_NOT_FOUND);
      return;
    }

    if (db == null) {
      writeJsonResponse(response, "{ \"message\": \"db is null\"}", HttpStatus.SC_NOT_FOUND);
      return;
    }

    if (!db.equals(ConnectionManager.getInstance().getDB().getName())) {
      writeJsonResponse(response, "{ \"message\": \"db name doesn't match\"}", HttpStatus.SC_NOT_FOUND);
      return;
    }

    ConnectionManager.getInstance().ensureIndexes();

    StringBuilder data = new StringBuilder();
    data.append("{");
    data.append(" \"message\": \"indexes created or updated on db=" + db + "\"");
    data.append("}");
    writeJsonResponse(response, data.toString());
  }

  protected void writeJsonResponse(HttpServletResponse response, String content) {
    writeJsonResponse(response, content, HttpStatus.SC_OK);
  }

  protected void writeJsonResponse(HttpServletResponse response, String content, int status) {
    writeResponse(response, content, MIME_TYPE_JSON, status);
  }

  protected void writeTextResponse(HttpServletResponse response, String content) {
    writeTextResponse(response, content, HttpStatus.SC_OK);
  }

  protected void writeTextResponse(HttpServletResponse response, String content, int status) {
    writeResponse(response, content, MIME_TYPE_TEXT, status);
  }

  protected void writeErrorResponse(HttpServletResponse response, Exception e) {
    LOG.log(Level.WARNING,
            "Error processing request",
            e);
    writeTextResponse(response, e.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  protected void writeErrorResponse(HttpServletResponse response, ChatException e) {
    LOG.log(Level.WARNING,
            "Error processing request",
            e);
    writeTextResponse(response, e.getMessage(), e.getStatus());
  }

  protected void writeErrorResponse(HttpServletResponse response, String message) {
    LOG.log(Level.FINE, "Error processing request: {}", message);
    writeTextResponse(response, message, HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  protected void writeResponse(HttpServletResponse response, String content, String contentType, int status) {
    try {
      response.setDateHeader("Last-Modified", System.currentTimeMillis());
      response.setHeader("Cache-Control", "no-cache");
      response.setStatus(status);
      if (StringUtils.isNotBlank(contentType) && StringUtils.isNotBlank(content)) {
        response.setContentType(contentType);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        OutputStream out = response.getOutputStream();
        out.write(content.getBytes());
        out.close();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error writing response", e);
    }
  }

  private boolean checkPassphrase(String passphrase) {
    boolean checkPP = false;

    if (PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE).equals(passphrase)) {
      checkPP = true;
    }
    if ("".equals(passphrase) || "chat".equals(passphrase)) {
      LOG.warning("ChatServer is not secured! Please change 'chatPassPhrase' property in " + PropertyManager.PROPERTIES_PATH);
    }

    return checkPP;
  }
}
