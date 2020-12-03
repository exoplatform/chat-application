package org.exoplatform.chat.server;

import juzu.Response;
import org.exoplatform.chat.AbstractChatTestCase;
import org.exoplatform.chat.bootstrap.ServiceBootstrap;
import org.exoplatform.chat.model.MessageBean;
import org.exoplatform.chat.model.SpaceBean;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.TokenService;
import org.exoplatform.chat.services.UserService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 *
 */
@Ignore
public class ChatServerTest extends AbstractChatTestCase {

  @Test
  public void testReadMessageWhenMemberOfTheUserRoom() throws Exception {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    List<String> users = new ArrayList<String>();
    users.add("mary");
    users.add("john");
    String roomId = chatService.getRoom(users);

    String token = tokenService.getToken("john");
    tokenService.addUser("john", token);

    // When
    Response.Content read = chatServer.read("john", token, roomId, "1", null,"false", "0");

    // Then
    assertNotNull(read);
    assertEquals(200, read.getCode());
  }

  @Test
  public void testReadMessageWhenNotMemberOfTheUserRoom() throws Exception {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    List<String> users = new ArrayList<String>();
    users.add("mary");
    users.add("john");
    String roomId = chatService.getRoom(users);

    String token = tokenService.getToken("james");
    tokenService.addUser("james", token);

    // When
    Response.Content read = chatServer.read("james", token, roomId, "1", null,"false", "0");

    // Then
    assertNotNull(read);
    assertEquals(403, read.getCode());
  }

  @Test
  public void testReadMessageWhenMemberOfTheTeamRoom() throws Exception {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    List<String> users = new ArrayList<String>();
    users.add("john");
    users.add("mary");
    String roomId = chatService.getTeamRoom("myteam", "john");

    userService.addTeamUsers(roomId, users);

    String token = tokenService.getToken("john");
    tokenService.addUser("john", token);

    // When
    Response.Content read = chatServer.read("john", token, roomId, "1", null, "false", "0");

    // Then
    assertNotNull(read);
    assertEquals(200, read.getCode());
  }

  @Test
  public void testReadMessageWhenNotMemberOfTheTeamRoom() throws Exception {
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    List<String> users = new ArrayList<String>();
    users.add("john");
    users.add("mary");
    String roomId = chatService.getTeamRoom("myteam", "john");

    userService.addTeamUsers(roomId, users);

    String token = tokenService.getToken("james");
    tokenService.addUser("james", token);

    Response.Content read = chatServer.read("james", token, roomId, "1", null,"false", "0");

    assertNotNull(read);
    assertEquals(403, read.getCode());
  }

  @Test
  public void testReadMessageWhenMemberOfTheSpaceRoom() throws Exception {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    TokenService tokenService = ServiceBootstrap.getTokenService();

    SpaceBean mySpace = new SpaceBean();
    mySpace.setId("myspace");
    mySpace.setGroupId("/spaces/myspace");
    mySpace.setDisplayName("My Space");
    mySpace.setRoom("myspace");
    userService.setSpaces("john", Collections.singletonList(mySpace));
    userService.setSpaces("mary", Collections.singletonList(mySpace));

    String roomId = chatService.getSpaceRoom("myspace");

    String token = tokenService.getToken("john");
    tokenService.addUser("john", token);

    // When
    Response.Content read = chatServer.read("john", token, roomId, "1", null,"false", "0");

    // Then
    assertNotNull(read);
    assertEquals(200, read.getCode());
  }

  @Test
  public void testReadMessageWhenNotMemberOfTheSpaceRoom() throws Exception {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    TokenService tokenService = ServiceBootstrap.getTokenService();

    SpaceBean mySpace = new SpaceBean();
    mySpace.setId("myspace");
    mySpace.setGroupId("/spaces/myspace");
    mySpace.setDisplayName("My Space");
    mySpace.setRoom("myspace");
    userService.setSpaces("john", Collections.singletonList(mySpace));
    userService.setSpaces("mary", Collections.singletonList(mySpace));

    String roomId = chatService.getSpaceRoom("myspace");

    String token = tokenService.getToken("james");
    tokenService.addUser("james", token);

    // When
    Response.Content read = chatServer.read("james", token, roomId, "1", null,"false", "0");

    // Then
    assertNotNull(read);
    assertEquals(403, read.getCode());
  }

  @Test
  public void testEditMessageWhenAuthorOfTheMessage() throws Exception {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    UserService userService = ServiceBootstrap.getUserService();
    List<String> users = new ArrayList<String>();
    users.add("mary");
    users.add("john");
    String roomId = chatService.getRoom(users);

    String token = tokenService.getToken("john");
    tokenService.addUser("john", token);

    chatService.write("my message", "john", roomId, "false");

    String jsonMessages = chatService.read("mary", roomId);
    JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonMessages);
    JSONArray messages = (JSONArray) jsonObject.get("messages");
    JSONObject message = (JSONObject) messages.get(0);
    String messageId = (String) message.get("id");

    // When
    Response.Content edit = chatServer.edit("john", token, roomId, messageId, "my new message");

    // Then
    assertNotNull(edit);
    assertEquals(200, edit.getCode());
    MessageBean messageBean = chatService.getMessage(roomId, messageId);
    assertNotNull(messageBean);
    assertEquals("my new message", messageBean.getMessage());
  }

  @Test
  public void testEditMessageWhenNotAuthorOfTheMessage() throws Exception {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    UserService userService = ServiceBootstrap.getUserService();
    List<String> users = new ArrayList<String>();
    users.add("mary");
    users.add("john");
    String roomId = chatService.getRoom(users);

    String tokenJohn = tokenService.getToken("john");
    tokenService.addUser("john", tokenJohn);
    String tokenJames = tokenService.getToken("james");
    tokenService.addUser("james", tokenJames);

    chatService.write("my message", "john", roomId, "false");

    String jsonMessages = chatService.read("mary", roomId);
    JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonMessages);
    JSONArray messages = (JSONArray) jsonObject.get("messages");
    JSONObject message = (JSONObject) messages.get(0);
    String messageId = (String) message.get("id");

    // When
    Response.Content edit = chatServer.edit("james", tokenJames, roomId, messageId, "my new message");

    // Then
    assertNotNull(edit);
    assertEquals(404, edit.getCode());
    MessageBean messageBean = chatService.getMessage(roomId, messageId);
    assertNotNull(messageBean);
    assertEquals("my message", messageBean.getMessage());
  }

  @Test
  public void testDeleteMessageWhenAuthorOfTheMessage() throws Exception {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    UserService userService = ServiceBootstrap.getUserService();
    List<String> users = new ArrayList<String>();
    users.add("mary");
    users.add("john");
    String roomId = chatService.getRoom(users);

    String token = tokenService.getToken("john");
    tokenService.addUser("john", token);

    chatService.write("my message", "john", roomId, "false");

    String jsonMessages = chatService.read("mary", roomId);
    JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonMessages);
    JSONArray messages = (JSONArray) jsonObject.get("messages");
    JSONObject message = (JSONObject) messages.get(0);
    String messageId = (String) message.get("id");

    // When
    Response.Content delete = chatServer.delete("john", token, roomId, messageId);

    // Then
    assertNotNull(delete);
    assertEquals(200, delete.getCode());
    MessageBean messageBean = chatService.getMessage(roomId, messageId);
    assertNotNull(messageBean);
    assertEquals(ChatService.TYPE_DELETED, messageBean.getMessage());
  }

  @Test
  public void testDeleteMessageWhenNotAuthorOfTheMessage() throws Exception {
    // Given
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    UserService userService = ServiceBootstrap.getUserService();
    List<String> users = new ArrayList<String>();
    users.add("mary");
    users.add("john");
    String roomId = chatService.getRoom(users);

    String tokenJohn = tokenService.getToken("john");
    tokenService.addUser("john", tokenJohn);
    String tokenJames = tokenService.getToken("james");
    tokenService.addUser("james", tokenJames);

    chatService.write("my message", "john", roomId, "false");

    String jsonMessages = chatService.read("mary", roomId);
    JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonMessages);
    JSONArray messages = (JSONArray) jsonObject.get("messages");
    JSONObject message = (JSONObject) messages.get(0);
    String messageId = (String) message.get("id");

    // When
    Response.Content delete = chatServer.delete("james", tokenJames, roomId, messageId);

    // Then
    assertNotNull(delete);
    assertEquals(404, delete.getCode());
    MessageBean messageBean = chatService.getMessage(roomId, messageId);
    assertNotNull(messageBean);
    assertEquals("my message", messageBean.getMessage());
  }
  
  @Test
  public void testIsFavorite() throws Exception {
    // Given
    ChatServer chatServer = new ChatServer();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    UserService userService = ServiceBootstrap.getUserService();
    
    String benjamin = "benjamin";
    String john = "john";
    
    userService.addUserFullName(benjamin, "Benjamin Paillereau");
    userService.addUserFullName(john, "John Smith");
    userService.toggleFavorite(benjamin, john);

    String tokenBen = tokenService.getToken(benjamin);
    tokenService.addUser(benjamin, tokenBen);
    
    String tokenJohn = tokenService.getToken(john);
    tokenService.addUser(john, tokenJohn);
  
    // When
    Response.Content isFavoriteBen = chatServer.isFavorite(benjamin, tokenBen, john);
    Response.Content isFavoriteJohn = chatServer.isFavorite(benjamin, tokenJohn, john);
    // Then
    assertNotNull(isFavoriteBen);
    assertEquals(200, isFavoriteBen.getCode());
    
    assertNotNull(isFavoriteJohn);
    assertEquals(404, isFavoriteJohn.getCode());
  }

  @Test
  public void testGetUsersCount() {
    ChatServer chatServer = new ChatServer();
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    TokenService tokenService = ServiceBootstrap.getTokenService();

    SpaceBean mySpace = new SpaceBean();
    mySpace.setId("myspace");
    mySpace.setGroupId("/spaces/myspace");
    mySpace.setDisplayName("My Space");
    mySpace.setRoom("myspace");
    userService.setSpaces("john", Collections.singletonList(mySpace));
    userService.setSpaces("mary", Collections.singletonList(mySpace));
    userService.setSpaces("ali", Collections.singletonList(mySpace));
    userService.setSpaces("shahin", Collections.singletonList(mySpace));

    String roomId = chatService.getSpaceRoom("myspace");

    String token = tokenService.getToken("john");
    tokenService.addUser("john", token);
    Response.Content usersCount = chatServer.getUsersCount("john", token, roomId, "");
    assertNotNull(usersCount);
    assertEquals(200, usersCount.getCode());
    assertTrue("Users count should be 4",  usersCount.toString().contains("4"));
  }
}