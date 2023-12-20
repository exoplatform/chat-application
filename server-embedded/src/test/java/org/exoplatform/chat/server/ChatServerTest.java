package org.exoplatform.chat.server;

import static org.exoplatform.chat.server.ChatTools.FILTER_PARAM;
import static org.exoplatform.chat.server.ChatTools.FROM_TIMESTAMP_PARAM;
import static org.exoplatform.chat.server.ChatTools.MESSAGES_PARAM;
import static org.exoplatform.chat.server.ChatTools.MESSAGE_ID_PARAM;
import static org.exoplatform.chat.server.ChatTools.MESSAGE_PARAM;
import static org.exoplatform.chat.server.ChatTools.ROOM_ID;
import static org.exoplatform.chat.server.ChatTools.ROOM_PARAM;
import static org.exoplatform.chat.server.ChatTools.START_PARAM;
import static org.exoplatform.chat.server.ChatTools.START_TIME_PARAM;
import static org.exoplatform.chat.server.ChatTools.TARGET_USER_PARAM;
import static org.exoplatform.chat.server.ChatTools.TOKEN_PARAM;
import static org.exoplatform.chat.server.ChatTools.TO_TIMESTAMP_PARAM;
import static org.exoplatform.chat.server.ChatTools.USER_PARAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.chat.AbstractChatTestCase;
import org.exoplatform.chat.bootstrap.ServiceBootstrap;
import org.exoplatform.chat.model.MessageBean;
import org.exoplatform.chat.model.SpaceBean;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.TokenService;
import org.exoplatform.chat.services.UserService;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class ChatServerTest extends AbstractChatTestCase {

  private static final String SPACE_GROUP_ID   = "/spaces/myspace";

  private static final String SPACE_NAME       = "My Space";

  private static final String SPACE_ROOM_ID    = "myspace";

  private static final String JOHN_SMITH       = "John Smith";

  private static final String MESSAGE          = "my message";

  private static final String MODIFIED_MESSAGE = "my new message";

  private static final String MY_TEAM_ROOM     = "myteam";

  private static final String MARY             = "mary";

  private static final String JOHN             = "john";

  private static final String JAMES            = "james";

  private static final String FALSE_STRING     = "false";

  @Mock
  HttpServletRequest          request;

  @Mock
  HttpServletResponse         response;

  @Mock
  ServletOutputStream         responseOutputStream;

  String                      content;

  @Before
  @SneakyThrows
  public void setup() {
    this.content = null;
    when(response.getOutputStream()).thenReturn(responseOutputStream);
    doAnswer(invocation -> {
      byte[] bytes = invocation.getArgument(0);
      this.content = new String(bytes);
      return null;
    }).when(responseOutputStream).write(any());
  }

  @Test
  public void testReadMessageWhenMemberOfTheUserRoom() {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    List<String> users = new ArrayList<>();
    users.add(MARY);
    users.add(JOHN);
    String roomId = chatService.getRoom(users);

    String token = tokenService.getToken(JOHN);
    tokenService.addUser(JOHN, token);

    // When
    when(request.getParameter(USER_PARAM)).thenReturn(JOHN);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(token);
    when(request.getParameter(ROOM_PARAM)).thenReturn(roomId);
    when(request.getParameter(FROM_TIMESTAMP_PARAM)).thenReturn("1");
    when(request.getParameter(TO_TIMESTAMP_PARAM)).thenReturn(null);
    chatServer.read(request, response);

    // Then
    verify(response, times(1)).setStatus(HttpStatus.SC_OK);
  }

  @Test
  public void testReadMessageWhenNotMemberOfTheUserRoom() {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    List<String> users = new ArrayList<>();
    users.add(MARY);
    users.add(JOHN);
    String roomId = chatService.getRoom(users);

    String token = tokenService.getToken(JAMES);
    tokenService.addUser(JAMES, token);

    // When
    when(request.getParameter(USER_PARAM)).thenReturn(JAMES);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(token);
    when(request.getParameter(ROOM_PARAM)).thenReturn(roomId);
    when(request.getParameter(FROM_TIMESTAMP_PARAM)).thenReturn("1");
    when(request.getParameter(TO_TIMESTAMP_PARAM)).thenReturn(null);
    chatServer.read(request, response);

    // Then
    verify(response, times(1)).setStatus(HttpStatus.SC_UNAUTHORIZED);
  }

  @Test
  public void testReadMessageWhenMemberOfTheTeamRoom() {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    List<String> users = new ArrayList<>();
    users.add(JOHN);
    users.add(MARY);
    String roomId = chatService.getTeamRoom(MY_TEAM_ROOM, JOHN);

    userService.addTeamUsers(roomId, users);

    String token = tokenService.getToken(JOHN);
    tokenService.addUser(JOHN, token);

    // When
    when(request.getParameter(USER_PARAM)).thenReturn(JOHN);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(token);
    when(request.getParameter(ROOM_PARAM)).thenReturn(roomId);
    when(request.getParameter(FROM_TIMESTAMP_PARAM)).thenReturn("1");
    when(request.getParameter(TO_TIMESTAMP_PARAM)).thenReturn(null);
    chatServer.read(request, response);

    // Then
    verify(response, times(1)).setStatus(HttpStatus.SC_OK);
  }

  @Test
  public void testReadMessageWhenNotMemberOfTheTeamRoom() {
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    List<String> users = new ArrayList<>();
    users.add(JOHN);
    users.add(MARY);
    String roomId = chatService.getTeamRoom(MY_TEAM_ROOM, JOHN);

    userService.addTeamUsers(roomId, users);

    String token = tokenService.getToken(JAMES);
    tokenService.addUser(JAMES, token);

    when(request.getParameter(USER_PARAM)).thenReturn(JAMES);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(token);
    when(request.getParameter(ROOM_PARAM)).thenReturn(roomId);
    when(request.getParameter(FROM_TIMESTAMP_PARAM)).thenReturn("1");
    when(request.getParameter(TO_TIMESTAMP_PARAM)).thenReturn(null);
    chatServer.read(request, response);

    verify(response, times(1)).setStatus(HttpStatus.SC_UNAUTHORIZED);
  }

  @Test
  public void testReadMessageWhenMemberOfTheSpaceRoom() {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    TokenService tokenService = ServiceBootstrap.getTokenService();

    SpaceBean mySpace = new SpaceBean();
    mySpace.setId(SPACE_ROOM_ID);
    mySpace.setGroupId(SPACE_GROUP_ID);
    mySpace.setDisplayName(SPACE_NAME);
    mySpace.setRoom(SPACE_ROOM_ID);
    userService.setSpaces(JOHN, Collections.singletonList(mySpace));
    userService.setSpaces(MARY, Collections.singletonList(mySpace));

    String roomId = chatService.getSpaceRoom(SPACE_ROOM_ID);

    String token = tokenService.getToken(JOHN);
    tokenService.addUser(JOHN, token);

    // When
    when(request.getParameter(USER_PARAM)).thenReturn(JOHN);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(token);
    when(request.getParameter(ROOM_PARAM)).thenReturn(roomId);
    when(request.getParameter(FROM_TIMESTAMP_PARAM)).thenReturn("1");
    when(request.getParameter(TO_TIMESTAMP_PARAM)).thenReturn(null);
    chatServer.read(request, response);

    // Then
    verify(response, times(1)).setStatus(HttpStatus.SC_OK);
  }

  @Test
  public void testReadMessageWhenNotMemberOfTheSpaceRoom() {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    TokenService tokenService = ServiceBootstrap.getTokenService();

    SpaceBean mySpace = new SpaceBean();
    mySpace.setId(SPACE_ROOM_ID);
    mySpace.setGroupId(SPACE_GROUP_ID);
    mySpace.setDisplayName(SPACE_NAME);
    mySpace.setRoom(SPACE_ROOM_ID);
    userService.setSpaces(JOHN, Collections.singletonList(mySpace));
    userService.setSpaces(MARY, Collections.singletonList(mySpace));

    String roomId = chatService.getSpaceRoom(SPACE_ROOM_ID);

    String token = tokenService.getToken(JAMES);
    tokenService.addUser(JAMES, token);

    // When
    when(request.getParameter(USER_PARAM)).thenReturn(JAMES);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(token);
    when(request.getParameter(ROOM_PARAM)).thenReturn(roomId);
    when(request.getParameter(FROM_TIMESTAMP_PARAM)).thenReturn("1");
    when(request.getParameter(TO_TIMESTAMP_PARAM)).thenReturn(null);
    chatServer.read(request, response);

    // Then
    verify(response, times(1)).setStatus(HttpStatus.SC_UNAUTHORIZED);
  }

  @Test
  public void testEditMessageWhenAuthorOfTheMessage() {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    List<String> users = new ArrayList<>();
    users.add(MARY);
    users.add(JOHN);
    String roomId = chatService.getRoom(users);

    String token = tokenService.getToken(JOHN);
    tokenService.addUser(JOHN, token);

    chatService.write(MESSAGE, JOHN, roomId, FALSE_STRING);

    String jsonMessages = chatService.read(MARY, roomId);
    JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonMessages);
    JSONArray messages = (JSONArray) jsonObject.get(MESSAGE_PARAM);
    JSONObject message = (JSONObject) messages.get(0);
    String messageId = (String) message.get("id");
    when(request.getParameter(USER_PARAM)).thenReturn(JOHN);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(token);
    when(request.getParameter(ROOM_PARAM)).thenReturn(roomId);
    when(request.getParameter(MESSAGE_ID_PARAM)).thenReturn(messageId);
    when(request.getParameter(MESSAGE_PARAM)).thenReturn(MODIFIED_MESSAGE);
    chatServer.edit(request, response);

    // Then
    verify(response, times(1)).setStatus(HttpStatus.SC_OK);

    MessageBean messageBean = chatService.getMessage(roomId, messageId);
    assertNotNull(messageBean);
    assertEquals(MODIFIED_MESSAGE, messageBean.getMessage());
  }

  @Test
  public void testEditMessageWhenNotAuthorOfTheMessage() {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    List<String> users = new ArrayList<>();
    users.add(MARY);
    users.add(JOHN);
    String roomId = chatService.getRoom(users);

    String tokenJohn = tokenService.getToken(JOHN);
    tokenService.addUser(JOHN, tokenJohn);
    String tokenJames = tokenService.getToken(JAMES);
    tokenService.addUser(JAMES, tokenJames);

    chatService.write(MESSAGE, JOHN, roomId, FALSE_STRING);

    String jsonMessages = chatService.read(MARY, roomId);
    JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonMessages);
    JSONArray messages = (JSONArray) jsonObject.get(MESSAGES_PARAM);
    JSONObject message = (JSONObject) messages.get(0);
    String messageId = (String) message.get("id");

    // When
    when(request.getParameter(USER_PARAM)).thenReturn(JAMES);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(tokenJames);
    when(request.getParameter(ROOM_PARAM)).thenReturn(roomId);
    when(request.getParameter(MESSAGE_ID_PARAM)).thenReturn(messageId);
    when(request.getParameter(MESSAGE_PARAM)).thenReturn(MODIFIED_MESSAGE);
    chatServer.edit(request, response);

    // Then
    verify(response, times(1)).setStatus(HttpStatus.SC_NOT_FOUND);

    MessageBean messageBean = chatService.getMessage(roomId, messageId);
    assertNotNull(messageBean);
    assertEquals(MESSAGE, messageBean.getMessage());
  }

  @Test
  public void testDeleteMessageWhenAuthorOfTheMessage() {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    List<String> users = new ArrayList<>();
    users.add(MARY);
    users.add(JOHN);
    String roomId = chatService.getRoom(users);

    String token = tokenService.getToken(JOHN);
    tokenService.addUser(JOHN, token);

    chatService.write(MESSAGE, JOHN, roomId, FALSE_STRING);

    String jsonMessages = chatService.read(MARY, roomId);
    JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonMessages);
    JSONArray messages = (JSONArray) jsonObject.get(MESSAGES_PARAM);
    JSONObject message = (JSONObject) messages.get(0);
    String messageId = (String) message.get("id");

    // When
    when(request.getParameter(USER_PARAM)).thenReturn(JOHN);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(token);
    when(request.getParameter(ROOM_PARAM)).thenReturn(roomId);
    when(request.getParameter(MESSAGE_ID_PARAM)).thenReturn(messageId);
    chatServer.delete(request, response);

    // Then
    verify(response, times(1)).setStatus(HttpStatus.SC_OK);

    MessageBean messageBean = chatService.getMessage(roomId, messageId);
    assertNotNull(messageBean);
    assertEquals(ChatService.TYPE_DELETED, messageBean.getMessage());
  }

  @Test
  public void testDeleteMessageWhenNotAuthorOfTheMessage() {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    List<String> users = new ArrayList<>();
    users.add(MARY);
    users.add(JOHN);
    String roomId = chatService.getRoom(users);

    String tokenJohn = tokenService.getToken(JOHN);
    tokenService.addUser(JOHN, tokenJohn);
    String tokenJames = tokenService.getToken(JAMES);
    tokenService.addUser(JAMES, tokenJames);

    chatService.write(MESSAGE, JOHN, roomId, FALSE_STRING);

    String jsonMessages = chatService.read(MARY, roomId);
    JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonMessages);
    JSONArray messages = (JSONArray) jsonObject.get(MESSAGES_PARAM);
    JSONObject message = (JSONObject) messages.get(0);
    String messageId = (String) message.get("id");

    // When
    when(request.getParameter(USER_PARAM)).thenReturn(JAMES);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(tokenJames);
    when(request.getParameter(ROOM_PARAM)).thenReturn(roomId);
    when(request.getParameter(MESSAGE_ID_PARAM)).thenReturn(messageId);
    chatServer.delete(request, response);

    // Then
    verify(response, times(1)).setStatus(HttpStatus.SC_NOT_FOUND);

    MessageBean messageBean = chatService.getMessage(roomId, messageId);
    assertNotNull(messageBean);
    assertEquals(MESSAGE, messageBean.getMessage());
  }

  @Test
  public void testIsFavorite() {
    // Given
    ChatServer chatServer = new ChatServer();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    UserService userService = ServiceBootstrap.getUserService();

    String benjamin = "benjamin";
    String john = JOHN;

    userService.addUserFullName(benjamin, "Benjamin Paillereau");
    userService.addUserFullName(john, JOHN_SMITH);
    userService.addFavorite(benjamin, john);

    String tokenBen = tokenService.getToken(benjamin);
    tokenService.addUser(benjamin, tokenBen);

    String tokenJohn = tokenService.getToken(john);
    tokenService.addUser(john, tokenJohn);

    // When
    when(request.getParameter(USER_PARAM)).thenReturn(benjamin);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(tokenBen);
    when(request.getParameter(TARGET_USER_PARAM)).thenReturn(john);
    chatServer.isFavorite(request, response);

    // Then
    verify(response, times(1)).setStatus(HttpStatus.SC_OK);
    assertEquals(FALSE_STRING, this.content);

    // When
    this.content = null;
    when(request.getParameter(USER_PARAM)).thenReturn(benjamin);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(tokenJohn);
    when(request.getParameter(TARGET_USER_PARAM)).thenReturn(john);
    chatServer.isFavorite(request, response);

    // Then
    verify(response, times(1)).setStatus(HttpStatus.SC_NOT_FOUND);
    assertNull(this.content);
  }

  @Test
  public void testGetUsersCount() {
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    TokenService tokenService = ServiceBootstrap.getTokenService();

    SpaceBean mySpace = new SpaceBean();
    mySpace.setId(SPACE_ROOM_ID);
    mySpace.setGroupId(SPACE_GROUP_ID);
    mySpace.setDisplayName(SPACE_NAME);
    mySpace.setRoom(SPACE_ROOM_ID);
    userService.setSpaces(JOHN, Collections.singletonList(mySpace));
    userService.setSpaces(MARY, Collections.singletonList(mySpace));
    userService.setSpaces("ali", Collections.singletonList(mySpace));
    userService.setSpaces("shahin", Collections.singletonList(mySpace));

    String roomId = chatService.getSpaceRoom(SPACE_ROOM_ID);

    String token = tokenService.getToken(JOHN);
    tokenService.addUser(JOHN, token);

    when(request.getParameter(USER_PARAM)).thenReturn(JOHN);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(token);
    when(request.getParameter(ROOM_ID)).thenReturn(roomId);
    when(request.getParameter(FILTER_PARAM)).thenReturn("");
    chatServer.usersCount(request, response);

    verify(response, times(1)).setStatus(HttpStatus.SC_OK);
    assertTrue("Users count should be 4", this.content.contains("4"));
  }

  @Test
  public void testDeleteRoomWhenCreator() {
    // Given
    ChatServer chatServer = new ChatServer();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();

    userService.addUserFullName(JOHN, JOHN_SMITH);

    String token = tokenService.getToken(JOHN);
    tokenService.addUser(JOHN, token);

    String roomId = chatService.getTeamRoom(MY_TEAM_ROOM, JOHN);

    // When
    when(request.getParameter(USER_PARAM)).thenReturn(JOHN);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(token);
    when(request.getParameter(ROOM_ID)).thenReturn(roomId);
    chatServer.deleteTeamRoom(request, response);

    // Then
    verify(response, times(1)).setStatus(HttpStatus.SC_OK);
  }

  @Test
  public void testDeleteRoomWhenNotCreator() {
    // Given
    ChatServer chatServer = new ChatServer();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();

    userService.addUserFullName(JOHN, JOHN_SMITH);

    String token = tokenService.getToken(JOHN);
    tokenService.addUser(JOHN, token);

    String roomId = chatService.getTeamRoom(MY_TEAM_ROOM, MARY);

    // When
    when(request.getParameter(USER_PARAM)).thenReturn(JOHN);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(token);
    when(request.getParameter(ROOM_ID)).thenReturn(roomId);
    chatServer.deleteTeamRoom(request, response);

    // Then
    verify(response, times(1)).setStatus(HttpStatus.SC_NOT_FOUND);
  }

  @Test
  public void testUpdateRoomMeetingWhenMember() {
    // Given
    ChatServer chatServer = new ChatServer();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();

    String john = JOHN;

    userService.addUserFullName(john, JOHN_SMITH);

    String token = tokenService.getToken(john);
    tokenService.addUser(john, token);

    String roomId = chatService.getTeamRoom(MY_TEAM_ROOM, JOHN);

    // When
    when(request.getParameter(USER_PARAM)).thenReturn(JOHN);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(token);
    when(request.getParameter(ROOM_ID)).thenReturn(roomId);
    when(request.getParameter(START_PARAM)).thenReturn("true");
    when(request.getParameter(START_TIME_PARAM)).thenReturn("123456789");
    chatServer.updateRoomMeetingStatus(request, response);

    // Then
    verify(response, times(1)).setStatus(HttpStatus.SC_OK);
  }

  @Test
  public void testUpdateRoomMeetingWhenNotMember() {
    // Given
    ChatServer chatServer = new ChatServer();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();

    userService.addUserFullName(JOHN, JOHN_SMITH);

    String token = tokenService.getToken(JOHN);
    tokenService.addUser(JOHN, token);

    String roomId = chatService.getTeamRoom(MY_TEAM_ROOM, MARY);

    // When
    when(request.getParameter(USER_PARAM)).thenReturn(JOHN);
    when(request.getParameter(TOKEN_PARAM)).thenReturn(token);
    when(request.getParameter(ROOM_ID)).thenReturn(roomId);
    when(request.getParameter(START_PARAM)).thenReturn("true");
    when(request.getParameter(START_TIME_PARAM)).thenReturn("123456789");
    chatServer.updateRoomMeetingStatus(request, response);

    // Then
    verify(response, times(1)).setStatus(HttpStatus.SC_OK);
  }
}
