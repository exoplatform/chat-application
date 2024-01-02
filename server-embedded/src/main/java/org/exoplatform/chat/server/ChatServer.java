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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

import org.exoplatform.chat.model.MessageBean;
import org.exoplatform.chat.model.NotificationBean;
import org.exoplatform.chat.model.RealTimeMessageBean;
import org.exoplatform.chat.model.ReportBean;
import org.exoplatform.chat.model.RoomBean;
import org.exoplatform.chat.model.RoomsBean;
import org.exoplatform.chat.model.SpaceBean;
import org.exoplatform.chat.model.UserBean;
import org.exoplatform.chat.model.UsersBean;
import org.exoplatform.chat.services.ChatException;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.UserService;
import org.exoplatform.chat.utils.ChatUtils;
import org.exoplatform.chat.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.LocalePolicy;
import org.exoplatform.services.resources.ResourceBundleService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("unchecked")
public class ChatServer extends ChatTools {

  private static final long   serialVersionUID           = 5214457021130598730L;

  private static final Logger LOG                        = Logger.getLogger("ChatServer");

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String requestUri = req.getRequestURI().replace(req.getContextPath(), "");
    switch (requestUri) {
    case "", "/": {
      index(resp);
      break;
    }
    case "/filterOutSilentUsers": {
      getFilteredList(req, resp);
      break;
    }
    case "/userRooms": {
      getUserRooms(req, resp);
      break;
    }
    case "/whoIsOnline": {
      whoIsOnline(req, resp);
      break;
    }
    case "/updateUser": {
      updateUser(req, resp);
      break;
    }
    case "/setExternal": {
      setExternal(req, resp);
      break;
    }
    case "/read": {
      read(req, resp);
      break;
    }
    case "/setRoomNotificationTrigger": {
      setRoomNotificationTrigger(req, resp);
      break;
    }
    case "/getMeetingNotes": {
      getMeetingNotes(req, resp);
      break;
    }
    case "/toggleFavorite": {
      toggleFavorite(req, resp);
      break;
    }
    case "/isFavorite": {
      isFavorite(req, resp);
      break;
    }
    case "/getUserDesktopNotificationSettings": {
      getUserDesktopNotificationSettings(req, resp);
      break;
    }
    case "/setPreferredNotification": {
      setPreferredNotification(req, resp);
      break;
    }
    case "/setNotificationSettings": {
      setNotificationSettings(req, resp);
      break;
    }
    case "/setNotificationTrigger": {
      setNotificationTrigger(req, resp);
      break;
    }
    case "/getRoom": {
      getRoom(req, resp);
      break;
    }
    case "/isRoomEnabled": {
      isRoomEnabled(req, resp);
      break;
    }
    case "/updateRoomEnabled": {
      setRoomEnabled(req, resp);
      break;
    }
    case "/updateUnreadMessages": {
      updateUnreadMessages(req, resp);
      break;
    }
    case "/notification": {
      notification(req, resp);
      break;
    }
    case "/setStatus": {
      setStatus(req, resp);
      break;
    }
    case "/getCreator": {
      getCreator(req, resp);
      break;
    }
    case "/users": {
      users(req, resp);
      break;
    }
    case "/getUserFullName": {
      getUserFullName(req, resp);
      break;
    }
    case "/shouldUpdate": {
      shouldUpdate(req, resp);
      break;
    }
    case "/usersCount": {
      usersCount(req, resp);
      break;
    }
    case "/statistics": {
      statistics(resp);
      break;
    }
    case "/getStatus": {
      getStatus(req, resp);
      break;
    }
    default:
      writeTextResponse(resp, null, HttpStatus.SC_NOT_FOUND);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String requestUri = req.getRequestURI().replace(req.getContextPath(), "");
    switch (requestUri) {
    case "/edit": {
      edit(req, resp);
      break;
    }
    case "/send": {
      send(req, resp);
      break;
    }
    case "/sendMeetingNotes": {
      sendMeetingNotes(req, resp);
      break;
    }
    case "/saveTeamRoom": {
      saveTeamRoom(req, resp);
      break;
    }
    case "/updateRoomMeetingStatus": {
      updateRoomMeetingStatus(req, resp);
      break;
    }
    case "/addUser": {
      addUser(req, resp);
      break;
    }
    case "/logout": {
      logout(req, resp);
      break;
    }
    case "/setAsAdmin": {
      setAsAdmin(req, resp);
      break;
    }
    case "/addUserFullNameAndEmail": {
      addUserFullNameAndEmail(req, resp);
      break;
    }
    case "/deleteUser": {
      deleteUser(req, resp);
      break;
    }
    case "/setEnabledUser": {
      setEnabledUser(req, resp);
      break;
    }
    case "/setExternalUser": {
      setExternalUser(req, resp);
      break;
    }
    case "/setSpaces": {
      setSpaces(req, resp);
      break;
    }
    case "/delete": {
      delete(req, resp);
      break;
    }
    case "/deleteTeamRoom": {
      deleteTeamRoom(req, resp);
      break;
    }
    default:
      writeTextResponse(resp, null, HttpStatus.SC_NOT_FOUND);
    }
  }

  protected void index(HttpServletResponse resp) {
    resp.setStatus(HttpStatus.SC_OK);
  }

  protected void getFilteredList(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String roomId = request.getParameter(ROOM_ID);
    String token = request.getParameter(TOKEN_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }
    List<String> receivers = new ArrayList<>();
    List<UserBean> roomParticipants = userService.getUsers(roomId);
    if (roomParticipants.isEmpty())
      roomParticipants = userService.getUsersInRoomChatOneToOne(roomId);
    for (UserBean roomUser : roomParticipants) {
      if (!roomUser.getName().equals(user) && !notificationService.isRoomSilentForUser(roomUser.getName(), roomId)) {
        receivers.add(roomUser.getName());
      }
    }
    writeJsonResponse(response, JSONArray.toJSONString(receivers));
  }

  protected void getUserRooms(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String onlineUsers = request.getParameter(ONLINE_USERS_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String filter = request.getParameter(FILTER_PARAM);
    String offset = request.getParameter(OFFSET_PARAM);
    String limit = request.getParameter(LIMIT_PARAM);
    String roomType = request.getParameter(ROOM_TYPE_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }
    int limitValue = StringUtils.isNumeric(limit) ? Integer.parseInt(limit) : 20;
    int offsetValue = StringUtils.isNumeric(offset) ? Integer.parseInt(offset) : 0;
    List<String> limitUsers = Arrays.asList(onlineUsers.split(","));
    RoomsBean roomsBean = chatService.getUserRooms(user,
                                                   limitUsers,
                                                   filter,
                                                   offsetValue,
                                                   limitValue,
                                                   notificationService,
                                                   tokenService,
                                                   roomType);

    writeJsonResponse(response, roomsBean.roomsToJSON());
  }

  protected void whoIsOnline(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String onlineUsers = request.getParameter(ONLINE_USERS_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String filter = request.getParameter(FILTER_PARAM);
    String limit = request.getParameter(LIMIT_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }
    int ilimit = StringUtils.isNumeric(limit) ? Integer.parseInt(limit) : 20;

    List<String> limitUsers = Arrays.asList(onlineUsers.split(","));
    RoomsBean roomsBean = chatService.getRooms(user,
                                               limitUsers,
                                               filter,
                                               true,
                                               true,
                                               true,
                                               true,
                                               false,
                                               ilimit,
                                               notificationService,
                                               tokenService);
    writeJsonResponse(response, roomsBean.roomsToJSON());
  }

  protected void updateUser(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String targetUser = request.getParameter(TARGET_USER_PARAM);
    String isDeleted = request.getParameter(IS_DELETED_PARAM);
    String isEnabled = request.getParameter(IS_ENABLED_PARAM);
    String isExternal = request.getParameter(IS_EXTERNAL_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }
    // FIXME: Why a user update the sate of another user ???!!!
    userService.setExternalUser(targetUser, isExternal);
    if (StringUtils.equalsIgnoreCase(isDeleted, "true")) {
      userService.deleteUser(targetUser);
    } else {
      userService.setEnabledUser(targetUser, StringUtils.equalsIgnoreCase(isEnabled, "true"));
    }

    writeTextResponse(response, UPDATED_MESSAGE);
  }

  protected void setExternal(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String targetUser = request.getParameter(TARGET_USER_PARAM);
    String isExternal = request.getParameter(IS_EXTERNAL_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    // FIXME: Why a user update the sate of another user ???!!!
    userService.setExternalUser(targetUser, isExternal);
    writeTextResponse(response, UPDATED_MESSAGE);
  }

  protected void send(HttpServletRequest request, HttpServletResponse response) {
    String sender = request.getParameter(SENDER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String message = request.getParameter(MESSAGE_PARAM);
    String room = request.getParameter(ROOM_PARAM);
    String isSystem = request.getParameter(IS_SYSTEM_PARAM);
    String options = request.getParameter(OPTIONS_PARAM);
    send(response, sender, token, message, room, isSystem, options);
  }

  protected void read(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String room = request.getParameter(ROOM_PARAM);
    String fromTimestamp = request.getParameter(FROM_TIMESTAMP_PARAM);
    String toTimestamp = request.getParameter(TO_TIMESTAMP_PARAM);
    String isTextOnly = request.getParameter(IS_TEXT_ONLY_PARAM);
    String limit = request.getParameter(LIMIT_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    Long from = StringUtils.isNumeric(fromTimestamp) ? Long.parseLong(fromTimestamp) : null;
    Long to = StringUtils.isNumeric(toTimestamp) ? Long.parseLong(toTimestamp) : null;
    int ilimit = StringUtils.isNumeric(limit) ? Integer.parseInt(limit) : 20;
    try {
      String data = chatService.read(user, room, "true".equals(isTextOnly), from, to, ilimit);
      notificationService.setNotificationsAsRead(user, "chat", ROOM_PARAM, room);
      writeJsonResponse(response, data);
    } catch (ChatException e) {
      writeErrorResponse(response, e);
    } catch (Exception e) {
      writeErrorResponse(response, e);
    }
  }

  protected void sendMeetingNotes(HttpServletRequest request, HttpServletResponse response) { // NOSONAR
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String room = request.getParameter(ROOM_PARAM);
    String fromTimestamp = request.getParameter(FROM_TIMESTAMP_PARAM);
    String toTimestamp = request.getParameter(TO_TIMESTAMP_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    Long from = StringUtils.isNumeric(fromTimestamp) ? Long.parseLong(fromTimestamp) : null;
    Long to = StringUtils.isNumeric(toTimestamp) ? Long.parseLong(toTimestamp) : null;
    String data = chatService.read(user, room, false, from, to, 0);
    BasicDBObject datao = BasicDBObject.parse(data);
    String roomType = chatService.getTypeRoomChat(room);
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    String date = formatter.format(new GregorianCalendar().getTime());
    String title = "";
    String roomName = "";

    List<UserBean> users;
    Locale locale = request.getLocale();
    ResourceBundle res = resolveBundle(request, locale);

    if (datao.containsField(MESSAGES_PARAM)) {
      if (ChatService.TYPE_ROOM_USER.equalsIgnoreCase(roomType)) {
        users = userService.getUsersInRoomChatOneToOne(room)
                           .stream()
                           .filter(UserBean::isEnabledUser)
                           .toList();
        title = res.getString("exoplatform.chat.meetingnotes") + " [" + date + "]";
      } else {
        users = userService.getUsers(room).stream().filter(UserBean::isEnabledUser).toList();
        List<SpaceBean> spaces = userService.getSpaces(user);
        for (SpaceBean spaceBean : spaces) {
          if (room.equals(spaceBean.getRoom())) {
            roomName = spaceBean.getDisplayName();
          }
        }
        List<RoomBean> roomBeans = userService.getTeams(user);
        for (RoomBean roomBean : roomBeans) {
          if (room.equals(roomBean.getRoom())) {
            roomName = roomBean.getFullName();
          }
        }
        title = roomName + " : " + res.getString("exoplatform.chat.meetingnotes") + " [" + date + "]";
      }
      ReportBean reportBean = new ReportBean(res);
      reportBean.fill((BasicDBList) datao.get(MESSAGES_PARAM), users);

      ArrayList<String> tos = new ArrayList<>();
      String senderName = user;
      String senderMail = "";
      for (UserBean userBean : users) {
        if (!"".equals(userBean.getEmail())) {
          tos.add(userBean.getEmail());
        }
        if (user.equals(userBean.getName())) {
          senderName = userBean.getFullname();
          senderMail = userBean.getEmail();
        }
      }
      String serverBase = ChatUtils.getServerBase();
      String html = reportBean.getAsHtml(title, serverBase, locale);

      // inline images
      String prevUser = "";
      int index = 0;
      Map<String, String> inlineImages = new HashMap<>();
      for (MessageBean messageBean : reportBean.getMessages()) {
        if (!messageBean.getUser().equals(prevUser)) {
          String keyAvatar = messageBean.getUser() + index;
          String valueAvatar = serverBase + ChatService.USER_AVATAR_URL.replace("{}", messageBean.getUser());
          inlineImages.put(keyAvatar, valueAvatar);
          index++;
        }
        prevUser = messageBean.getUser();
      }

      try {
        sendMailWithAuth(senderName, senderMail, tos, html, title, inlineImages);
      } catch (Exception e) {
        LOG.info(e.getMessage());
      }
    }
    writeTextResponse(response, "sent");
  }

  protected void getMeetingNotes(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String room = request.getParameter(ROOM_PARAM);
    String fromTimestamp = request.getParameter(FROM_TIMESTAMP_PARAM);
    String toTimestamp = request.getParameter(TO_TIMESTAMP_PARAM);
    String portalURI = request.getParameter(PORTAL_URI_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    Long from = StringUtils.isNumeric(fromTimestamp) ? Long.parseLong(fromTimestamp) : null;
    Long to = StringUtils.isNumeric(toTimestamp) ? Long.parseLong(toTimestamp) : null;
    String wikiPageContent = "";
    List<UserBean> users = new ArrayList<>();
    JSONObject jsonObject = new JSONObject();
    String data = chatService.read(user, room, false, from, to, 0);
    String typeRoom = chatService.getTypeRoomChat(room);
    BasicDBObject datao = BasicDBObject.parse(data);
    if (datao.containsField(MESSAGES_PARAM)) {
      if (ChatService.TYPE_ROOM_USER.equalsIgnoreCase(typeRoom)) {
        users = userService.getUsersInRoomChatOneToOne(room)
                           .stream()
                           .filter(UserBean::isEnabledUser)
                           .toList();
      }
      Locale locale = request.getLocale();
      ResourceBundle res = resolveBundle(request, locale);

      ReportBean reportBean = new ReportBean(res);

      reportBean.fill((BasicDBList) datao.get(MESSAGES_PARAM), users);
      ArrayList<String> usersInGroup = new ArrayList<>();
      String serverBase = ChatUtils.getServerBase();
      wikiPageContent = reportBean.getWikiPageContent(serverBase, portalURI);
      try {
        for (UserBean userBean : users) {
          if (!"".equals(userBean.getName())) {
            usersInGroup.add(userBean.getName());
          }
        }
        jsonObject.put(USERS_PARAM, usersInGroup);
        jsonObject.put("wikiPageContent", wikiPageContent);
        jsonObject.put("typeRoom", typeRoom);
        writeJsonResponse(response, jsonObject.toString());
      } catch (Exception e) {
        writeTextResponse(response, "No Room yet", HttpStatus.SC_NOT_FOUND);
      }
    }
  }

  protected void delete(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String room = request.getParameter(ROOM_PARAM);
    String messageId = request.getParameter(MESSAGE_ID_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    // Only author of the message can delete it
    MessageBean message = chatService.getMessage(room, messageId);
    if (message == null || !message.getUser().equals(user)) {
      writeTextResponse(response, "", HttpStatus.SC_NOT_FOUND);
      return;
    }

    try {
      chatService.delete(room, user, messageId);
      writeTextResponse(response, UPDATED_MESSAGE);
    } catch (Exception e) {
      writeErrorResponse(response, e);
    }
  }

  protected void deleteTeamRoom(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String room = request.getParameter(ROOM_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }
    try {
      String creator = chatService.getTeamCreator(room);
      if (!creator.equals(user)) {
        writeTextResponse(response, "", HttpStatus.SC_NOT_FOUND);
        return;
      } else {
        chatService.deleteTeamRoom(room, user);
      }
      writeTextResponse(response, UPDATED_MESSAGE);
    } catch (Exception e) {
      writeErrorResponse(response, e);
    }
  }

  protected void edit(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String room = request.getParameter(ROOM_PARAM);
    String messageId = request.getParameter(MESSAGE_ID_PARAM);
    String message = request.getParameter(MESSAGE_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    // Only author of the message can edit it
    MessageBean currentMessage = chatService.getMessage(room, messageId);
    if (currentMessage == null || !currentMessage.getUser().equals(user)) {
      writeTextResponse(response, "", HttpStatus.SC_NOT_FOUND);
      return;
    }

    try {
      message = URLDecoder.decode(message, StandardCharsets.UTF_8);
      chatService.edit(room, user, messageId, message);
      writeTextResponse(response, UPDATED_MESSAGE);
    } catch (Exception e) {
      writeErrorResponse(response, e);
    }
  }

  protected void toggleFavorite(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String targetUser = request.getParameter(TARGET_USER_PARAM);
    String favorite = request.getParameter(FAVORITE_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }
    if (StringUtils.equalsIgnoreCase(favorite, "true")) {
      userService.addFavorite(user, targetUser);
    } else {
      userService.removeFavorite(user, targetUser);
    }
    writeTextResponse(response, UPDATED_MESSAGE);
  }

  protected void isFavorite(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String targetUser = request.getParameter(TARGET_USER_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }
    boolean isFavorite = false;
    try {
      isFavorite = userService.isFavorite(user, targetUser);
      writeTextResponse(response, String.valueOf(isFavorite));
    } catch (Exception e) {
      writeErrorResponse(response, e);
    }
  }

  protected void getUserDesktopNotificationSettings(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    JSONObject content = getUserDesktopNotificationSettings(user);
    writeJsonResponse(response, content.toJSONString());
  }

  protected void setPreferredNotification(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String notifManner = request.getParameter(NOTIF_MANNER_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    JSONObject content = new JSONObject();
    try {
      userService.setPreferredNotification(user, notifManner);
      content.put("done", true);
    } catch (Exception e) {
      content.put("done", false);
    }
    writeJsonResponse(response, content.toJSONString());
  }

  protected void setRoomNotificationTrigger(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String room = request.getParameter(ROOM_PARAM);
    String notifCondition = request.getParameter(NOTIF_CONDITION_PARAM);
    String notifConditionType = request.getParameter(NOTIF_CONDITION_TYPE_PARAM);
    String time = request.getParameter(TIME_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    try {
      Long timeMs = StringUtils.isNumeric(time) ? Long.parseLong(time) : null;
      userService.setRoomNotificationTrigger(user, room, notifCondition, notifConditionType, timeMs);
      Map<String, Object> data = new HashMap<>();

      data.put("notificationTrigger", notifConditionType);
      data.put("targetRoom", room);

      // Deliver the saved message to sender's subscribed channel itself.
      RealTimeMessageBean messageBean = new RealTimeMessageBean(
                                                                RealTimeMessageBean.EventType.ROOM_NOTIFICATION_SETTINGS_UPDATED,
                                                                null,
                                                                user,
                                                                null,
                                                                data);
      realTimeMessageService.sendMessage(messageBean, user);
    } catch (Exception e) {
      writeErrorResponse(response, e);
    }

    JSONObject content = getUserDesktopNotificationSettings(user);
    writeJsonResponse(response, content.toJSONString());
  }

  protected void setNotificationSettings(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String[] notifConditions = request.getParameterValues("notifConditions");
    String[] notifManners = request.getParameterValues("notifManners");
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    if (notifConditions != null) {
      for (String notifCondition : notifConditions) {
        try {
          userService.setPreferredNotification(user, notifCondition);
        } catch (Exception e) {
          LOG.severe("Error while updating the preferences of the user notifications: " + e.getMessage());
        }
      }
    }
    if (notifManners != null) {
      for (String notifManner : notifManners) {
        try {
          userService.setNotificationTrigger(user, notifManner);
        } catch (Exception e) {
          writeErrorResponse(response, e);
        }
      }
    }

    JSONObject content = getUserDesktopNotificationSettings(user);
    writeJsonResponse(response, content.toJSONString());
  }

  protected void setNotificationTrigger(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String notifCondition = request.getParameter(NOTIF_CONDITION_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    try {
      userService.setNotificationTrigger(user, notifCondition);
      JSONObject content = getUserDesktopNotificationSettings(user);
      writeJsonResponse(response, content.toJSONString());
    } catch (Exception e) {
      writeErrorResponse(response, e);
    }
  }

  protected void getRoom(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String targetUser = request.getParameter(TARGET_USER_PARAM);
    String withDetail = request.getParameter(WITH_DETAIL_PARAM);
    String type = request.getParameter(TYPE_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }
    String room = targetUser;
    RoomBean roomBean = null;
    try {
      if (type != null) {
        if ("room-id".equals(type)) {
          room = targetUser; // NOSONAR
        } else if ("space-name".equals(type)) {
          room = chatService.getSpaceRoomByName(targetUser);
        } else if ("space-id".equals(type)) {
          room = ChatUtils.getRoomId(targetUser);
        } else if ("username".equals(type)) {
          List<String> users = new ArrayList<>();
          users.add(user);
          users.add(targetUser);
          room = chatService.getRoom(users);
        } else if ("external".equals(type)) {
          room = chatService.getExternalRoom(targetUser);
        }
      } else if (targetUser.startsWith(ChatService.SPACE_PREFIX)) {
        room = chatService.getSpaceRoom(targetUser);

      } else if (targetUser.startsWith(ChatService.TEAM_PREFIX)) {
        room = chatService.getTeamRoom(targetUser, user);

      } else if (targetUser.startsWith(ChatService.EXTERNAL_PREFIX)) {
        room = chatService.getExternalRoom(targetUser);
      } else {
        ArrayList<String> users = new ArrayList<>(2);
        users.add(user);
        users.add(targetUser);
        room = chatService.getRoom(users);
      }

      if ("true".equals(withDetail)) {
        roomBean = userService.getRoom(user, room);
      }
      String out = room;
      if (roomBean != null) {
        out = roomBean.toJSON();
      }
      writeJsonResponse(response, out);
    } catch (Exception e) {
      writeErrorResponse(response, e);
    }
  }

  protected void saveTeamRoom(HttpServletRequest request, HttpServletResponse response) { // NOSONAR
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String teamName = request.getParameter(TEAM_NAME_PARAM);
    String room = request.getParameter(ROOM_PARAM);
    String users = request.getParameter(USERS_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    teamName = URLDecoder.decode(teamName, StandardCharsets.UTF_8);
    teamName = teamName.trim();
    if (StringUtils.isBlank(teamName)) {
      writeTextResponse(response, "Data is invalid!", HttpStatus.SC_BAD_REQUEST);
      return;
    }

    JSONObject jsonObject = new JSONObject();
    try {
      // Room creation
      if (room == null || "".equals(room) || "---".equals(room)) {
        List<RoomBean> roomBeans = chatService.getTeamRoomsByName(teamName);
        if (roomBeans != null && !roomBeans.isEmpty()) {
          for (RoomBean roomBean : roomBeans) {
            if (user.equals(roomBean.getUser())) {
              writeTextResponse(response, "roomAlreadyExists.creator", HttpStatus.SC_BAD_REQUEST);
              return;
            } else {
              List<String> existingUsers = userService.getUsersFilterBy(null, roomBean.getRoom(), ChatService.TYPE_ROOM_TEAM);
              if (existingUsers != null && existingUsers.contains(user)) {
                writeTextResponse(response, "roomAlreadyExists.notCreator", HttpStatus.SC_BAD_REQUEST);
                return;
              }
            }
          }
        }
        room = chatService.getTeamRoom(teamName, user);
      }
      // Only creator can edit members or set team name
      String creator = chatService.getTeamCreator(room);
      if (!user.equals(creator)) {
        response.setStatus(HttpStatus.SC_NOT_FOUND);
        return;
      }

      List<String> usersToNotifyForAdd = new JSONArray();
      List<String> usersToAdd = new JSONArray();

      if (users != null && !users.isEmpty()) {
        List<String> existingUsers = userService.getUsersFilterBy(null, room, ChatService.TYPE_ROOM_TEAM);

        List<String> usersNew = Arrays.asList(users.split(","));

        usersToAdd.addAll(usersNew);
        List<String> usersToRemove = new JSONArray();

        for (String u : existingUsers) {
          if (usersNew.contains(u)) {
            usersToNotifyForAdd.add(u);
            usersToAdd.remove(u);
          } else {
            usersToRemove.add(u);
          }
        }

        if (!usersToRemove.isEmpty()) {
          userService.removeTeamUsers(room, usersToRemove);

          StringBuilder sbUsers = new StringBuilder();
          boolean first = true;
          for (String u : usersToRemove) {
            if (!first)
              sbUsers.append("; ");
            UserBean userBean = userService.getUser(u);
            String fullName = userBean.isExternal()
                != null && userBean.isExternal().equals("true") ? userService.getUserFullName(u) + " (" +
                    getResourceBundleLabel(new Locale(getCurrentUserLanguage(user)), "external.label.tag") + ")" :
                                                                userService.getUserFullName(u);
            sbUsers.append(fullName);
            first = false;
            notificationService.setNotificationsAsRead(u, "chat", ROOM_PARAM, room);
          }

          // Send members removal message in the room
          String removeTeamUserOptions = "{\"type\":\"type-remove-team-user\",\"users\":\"" + sbUsers + "\", " +
              "\"fullname\":\"" + userService.getUserFullName(user) + "\"}";
          send(response, user, token, StringUtils.EMPTY, room, "true", removeTeamUserOptions);
        }

        chatService.setRoomName(room, teamName);

        if (!usersToAdd.isEmpty()) {
          userService.addTeamUsers(room, usersToAdd);

          StringBuilder sbUsers = new StringBuilder();
          boolean first = true;
          for (String usert : usersToAdd) {
            if (usert.equals(creator)) {
              continue;
            }
            if (!first)
              sbUsers.append("; ");
            UserBean userBean = userService.getUser(usert);
            String fullName = userBean.isExternal()
                != null && userBean.isExternal().equals("true") ? userService.getUserFullName(usert) + " (" +
                    getResourceBundleLabel(new Locale(getCurrentUserLanguage(user)), "external.label.tag") + ")" :
                                                                userService.getUserFullName(usert);
            sbUsers.append(fullName);
            first = false;
          }
          String addTeamUserOptions = "{\"type\":\"type-add-team-user\",\"users\":\"" + sbUsers + "\", " +
              "\"fullname\":\"" + userService.getUserFullName(user) + "\"}";
          send(response, user, token, StringUtils.EMPTY, room, "true", addTeamUserOptions);
        }
      }

      RoomBean roomBean = userService.getRoom(user, room);
      JSONObject data = roomBean.toJSONObject();
      data.put("title", teamName);
      data.put("participants", usersToAdd);

      RealTimeMessageBean updatedRoomMessage = new RealTimeMessageBean(
                                                                       RealTimeMessageBean.EventType.ROOM_UPDATED,
                                                                       room,
                                                                       user,
                                                                       null,
                                                                       data);

      if (!usersToNotifyForAdd.contains(creator)) {
        usersToNotifyForAdd.add(creator);
      }
      realTimeMessageService.sendMessage(updatedRoomMessage, usersToNotifyForAdd);
      jsonObject.putAll(data);
      jsonObject.put("name", teamName);
      jsonObject.put(ROOM_PARAM, room);
      writeJsonResponse(response, jsonObject.toJSONString());
    } catch (Exception e) {
      writeErrorResponse(response, "No Room yet");
    }
  }

  protected void updateRoomMeetingStatus(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String start = request.getParameter(START_PARAM);
    String room = request.getParameter(ROOM_PARAM);
    String startTime = request.getParameter(START_TIME_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    // only member of a room can updateRoomMeetingStatus
    if (chatService.isMemberOfRoom(user, room)) {
      chatService.setRoomMeetingStatus(room, Boolean.parseBoolean(start), startTime);
      writeTextResponse(response, UPDATED_MESSAGE);
    } else {
      writeTextResponse(response, "", HttpStatus.SC_NOT_FOUND);
    }
  }

  protected void isRoomEnabled(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String spaceId = request.getParameter(SPACE_ID_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    String room = ChatUtils.getRoomId(spaceId);
    Boolean isEnabled = chatService.isRoomEnabled(room);
    writeTextResponse(response, isEnabled.toString());
  }

  protected void setRoomEnabled(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String spaceId = request.getParameter(SPACE_ID_PARAM);
    String enabled = request.getParameter(ENABLED_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    String room = ChatUtils.getRoomId(spaceId);
    chatService.setRoomEnabled(room, StringUtils.equalsIgnoreCase(enabled, "true"));

    writeTextResponse(response, UPDATED_MESSAGE);
  }

  protected void updateUnreadMessages(HttpServletRequest request, HttpServletResponse response) {
    String room = request.getParameter(ROOM_PARAM);
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }
    try {
      notificationService.setNotificationsAsRead(user, "chat", ROOM_PARAM, room);
      if (userService.isAdmin(user)) {
        notificationService.setNotificationsAsRead(UserService.SUPPORT_USER, "chat", ROOM_PARAM, room);
      }
      writeTextResponse(response, UPDATED_MESSAGE);
    } catch (Exception e) {
      writeErrorResponse(response, e);
    }
  }

  protected void notification(HttpServletRequest request, HttpServletResponse response) { // NOSONAR
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String event = request.getParameter(EVENT_PARAM);
    String withDetails = request.getParameter(WITH_DETAILS_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    boolean detailed = StringUtils.equalsIgnoreCase(withDetails, "true");
    int totalUnread = 0;
    List<NotificationBean> notifications = null;
    if (!detailed) {
      // GETTING TOTAL NOTIFICATION WITHOUT DETAILS
      List<NotificationBean> notificationBeans = notificationService.getUnreadNotifications(user, userService);
      totalUnread = notificationBeans.size();
      if (userService.isAdmin(user)) {
        totalUnread += notificationService.getUnreadNotificationsTotal(UserService.SUPPORT_USER);
      }
    } else {
      // GETTING ALL NOTIFICATION DETAILS
      notifications = notificationService.getUnreadNotifications(user, userService);
      totalUnread = notifications.size();
    }

    if (event != null && event.equals("1")) {
      String data = "id: " + totalUnread + "\n";
      data += "data: {\"total\": " + totalUnread + "}\n\n";
      writeJsonResponse(response, data);
    } else {
      JSONObject json = new JSONObject();
      json.put("total", totalUnread);
      if (detailed) {
        JSONArray notifies = new JSONArray();
        if (CollectionUtils.isNotEmpty(notifications)) {
          for (NotificationBean o : notifications) {
            notifies.add(o.toJSONObject());
          }
        }
        json.put("notifications", notifies);
      }
      writeJsonResponse(response, json.toJSONString());
    }
  }

  protected void getStatus(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String targetUser = request.getParameter(TARGET_USER_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }
    String status = UserService.STATUS_INVISIBLE;
    try {
      if (targetUser != null) {
        status = userService.getStatus(targetUser);
      }
      writeTextResponse(response, status);
    } catch (Exception e) {
      writeErrorResponse(response, e);
    }
  }

  protected void setStatus(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String status = request.getParameter(STATUS_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }
    try {
      userService.setStatus(user, status);
      writeTextResponse(response, status);
    } catch (Exception e) {
      writeErrorResponse(response, "No Status for this User");
    }
  }

  protected void getCreator(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String room = request.getParameter(ROOM_PARAM);
    String creator = "";
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }
    try {
      creator = chatService.getTeamCreator(room);
      writeTextResponse(response, creator);
    } catch (Exception e) {
      writeErrorResponse(response, "No Status for this User");
    }
  }

  protected void users(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String onlineUsers = request.getParameter(ONLINE_USERS_PARAM);
    String room = request.getParameter(ROOM_PARAM);
    String filter = request.getParameter(FILTER_PARAM);
    String limitString = request.getParameter(LIMIT_PARAM);
    String onlineOnly = request.getParameter(ONLINE_ONLY_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    boolean showOnlyOnlineUsers = StringUtils.isNotBlank(onlineOnly) && "true".equals(onlineOnly);
    int limit = StringUtils.isNumeric(limitString) ? Integer.parseInt(limitString) : 20;
    List<String> onlineUserList = StringUtils.isNotBlank(onlineUsers) ? Arrays.asList(onlineUsers.split(",")) : null;
    List<UserBean> users = userService.getUsers(room, onlineUserList, filter, limit, showOnlyOnlineUsers);
    if (StringUtils.isNotBlank(user)) {
      UserBean currentUser = userService.getUser(user);
      users.remove(currentUser);
    }
    UsersBean usersBean = new UsersBean();
    usersBean.setUsers(users);
    writeJsonResponse(response, usersBean.usersToJSON());
  }

  protected void usersCount(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getParameter(USER_PARAM);
    String token = request.getParameter(TOKEN_PARAM);
    String room = request.getParameter(ROOM_PARAM);
    String filter = request.getParameter(FILTER_PARAM);
    if (!tokenService.hasUserWithToken(user, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    StringBuilder data = new StringBuilder();
    data.append("{");
    data.append(" \"usersCount\": ").append(userService.getUsersCount(room, filter)).append(",");
    data.append(" \"activeUsersCount\": ").append(userService.getActiveUsersCount(room, filter));
    data.append("}");

    writeJsonResponse(response, data.toString());
  }

  protected void statistics(HttpServletResponse response) {
    StringBuilder data = new StringBuilder();
    data.append("{");
    data.append(" \"users\": " + userService.getNumberOfUsers() + ", ");
    data.append(" \"rooms\": " + chatService.getNumberOfRooms() + ", ");
    data.append(" \"messages\": " + chatService.getNumberOfMessages() + ", ");
    data.append(" \"notifications\": " + notificationService.getNumberOfNotifications() + ", ");
    data.append(" \"notificationsUnread\": " + notificationService.getNumberOfUnreadNotifications());
    data.append("}");
    writeJsonResponse(response, data.toString());
  }

  private Session getMailSession() {
    String protocal = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_PROTOCAL);
    String host = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_HOST);
    final String user = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_USER);
    final String password = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_PASSWORD);
    String port = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_PORT);
    String auth = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_AUTH);
    String starttlsEnable = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_STARTTLS_ENABLE);
    String enableSSL = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_ENABLE_SSL_ENABLE);
    String smtpAuth = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_AUTH);
    String socketFactoryPort = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_SOCKET_FACTORY_PORT);
    String socketFactoryClass = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_SOCKET_FACTORY_CLASS);
    String socketFactoryFallback = PropertyManager.getProperty(PropertyManager.PROPERTY_MAIL_SOCKET_FACTORY_FALLBACK);

    Properties props = new Properties();

    // SMTP protocol properties
    props.put("mail.transport.protocol", protocal);
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", port);
    props.put("mail.smtp.auth", auth);

    if (Boolean.parseBoolean(smtpAuth)) {
      props.put("mail.smtp.socketFactory.port", socketFactoryPort);
      props.put("mail.smtp.socketFactory.class", socketFactoryClass);
      props.put("mail.smtp.socketFactory.fallback", socketFactoryFallback);
      props.put("mail.smtp.starttls.enable", starttlsEnable);
      props.put("mail.smtp.ssl.enable", enableSSL);
      return Session.getInstance(props, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(user, password);
        }
      });
    } else {
      return Session.getInstance(props);
    }
  }

  private void sendMailWithAuth(String senderFullname,
                                String senderMail,
                                List<String> toList,
                                String htmlBody,
                                String subject,
                                Map<String, String> inlineImages) throws Exception { // NOSONAR

    Session session = getMailSession();

    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(senderMail, senderFullname, "UTF-8"));

    // To get the array of addresses
    for (String to : toList) {
      message.addRecipients(Message.RecipientType.BCC, InternetAddress.parse(to));
    }

    message.setSubject(subject, "UTF-8");
    // use a MimeMultipart as we need to handle the file attachments
    Multipart multipart = new MimeMultipart();
    // Create a message part to represent the body text
    MimeBodyPart messageBodyPart = new MimeBodyPart();
    messageBodyPart.setContent(htmlBody, "text/html; charset=UTF-8");
    // add the message body to the mime message
    multipart.addBodyPart(messageBodyPart);

    // Part2: get user's avatar

    if (inlineImages != null && inlineImages.size() > 0) {
      Set<String> setImageID = inlineImages.keySet();
      for (String contentId : setImageID) {
        messageBodyPart = new MimeBodyPart();
        String imageFilePath = inlineImages.get(contentId);
        URL url = new URL(imageFilePath);
        URLConnection con = url.openConnection();
        con.setDoOutput(true);
        InputStream is = con.getInputStream();
        ByteArrayDataSource byteArrayDataSource = new ByteArrayDataSource(is, con.getContentType());
        messageBodyPart.setDataHandler(new DataHandler(byteArrayDataSource));
        messageBodyPart.setContentID("<" + contentId + ">");
        messageBodyPart.setDisposition(Part.INLINE);
        multipart.addBodyPart(messageBodyPart);
      }
    }
    // Put all message parts in the message
    message.setContent(multipart);

    try {
      Transport.send(message);
    } catch (Exception e) {
      LOG.info(e.getMessage());
    }
  }

  /**
   * Get the ressource bundle label.
   *
   * @return the ressource bundle label
   */
  public static String getResourceBundleLabel(Locale locale, String label) {
    ResourceBundleService resourceBundleService = ExoContainerContext.getService(ResourceBundleService.class);
    return resourceBundleService.getResourceBundle(resourceBundleService.getSharedResourceBundleNames(), locale).getString(label);
  }

  /**
   * Gets platform language of current user. In case of any errors return null.
   *
   * @return the platform language
   */
  public static String getCurrentUserLanguage(String userId) {
    LocaleContextInfo localeCtx = LocaleContextInfoUtils.buildLocaleContextInfo(userId);
    LocalePolicy localePolicy = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(LocalePolicy.class);
    String lang = Locale.getDefault().getLanguage();
    if (localePolicy != null) {
      Locale locale = localePolicy.determineLocale(localeCtx);
      lang = locale.toString();
    }
    return lang;
  }

  private void send(HttpServletResponse response,
                    String sender,
                    String token,
                    String message,
                    String room,
                    String isSystem,
                    String options) {
    if (!tokenService.hasUserWithToken(sender, token)) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }

    if (message != null) {
      message = URLDecoder.decode(message, StandardCharsets.UTF_8);
      options = URLDecoder.decode(options, StandardCharsets.UTF_8);
      try {
        chatService.write(null, message, sender, room, isSystem, options);
      } catch (ChatException e) {
        writeErrorResponse(response, e);
      }
    }
    writeTextResponse(response, "ok");
  }

  private JSONObject getUserDesktopNotificationSettings(String user) {
    JSONObject response = new JSONObject();
    JSONObject res = userService.getUserDesktopNotificationSettings(user).toJSON();
    response.put("done", res != null && !res.isEmpty());
    response.put("userDesktopNotificationSettings", res);
    return response;
  }

  private ResourceBundle resolveBundle(HttpServletRequest request, Locale locale) {
    try {
      return ResourceBundle.getBundle("locale.chat.server.Resource", locale, request.getServletContext().getClassLoader());
    } catch (Exception e) {
      return ResourceBundle.getBundle("locale.chat.server.Resource", Locale.ENGLISH, request.getServletContext().getClassLoader());
    }
  }

}
