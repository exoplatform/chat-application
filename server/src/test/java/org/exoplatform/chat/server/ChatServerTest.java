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
    String roomId = chatService.getRoom(users, null);

    String token = tokenService.getToken("john");
    tokenService.addUser("john", token, null);

    // When
    Response.Content read = chatServer.read("john", token, roomId, "1", "false", null);

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
    String roomId = chatService.getRoom(users, null);

    String token = tokenService.getToken("james");
    tokenService.addUser("james", token, null);

    // When
    Response.Content read = chatServer.read("james", token, roomId, "1", "false", null);

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
    String roomId = chatService.getTeamRoom("myteam", "john", null);

    userService.addTeamUsers(roomId, users, null);

    String token = tokenService.getToken("john");
    tokenService.addUser("john", token, null);

    // When
    Response.Content read = chatServer.read("john", token, roomId, "1", "false", null);

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
    String roomId = chatService.getTeamRoom("myteam", "john", null);

    userService.addTeamUsers(roomId, users, null);

    String token = tokenService.getToken("james");
    tokenService.addUser("james", token, null);

    Response.Content read = chatServer.read("james", token, roomId, "1", "false", null);

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
    userService.setSpaces("john", Collections.singletonList(mySpace), null);
    userService.setSpaces("mary", Collections.singletonList(mySpace), null);

    String roomId = chatService.getSpaceRoom("myspace", null);

    String token = tokenService.getToken("john");
    tokenService.addUser("john", token, null);

    // When
    Response.Content read = chatServer.read("john", token, roomId, "1", "false", null);

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
    userService.setSpaces("john", Collections.singletonList(mySpace), null);
    userService.setSpaces("mary", Collections.singletonList(mySpace), null);

    String roomId = chatService.getSpaceRoom("myspace", null);

    String token = tokenService.getToken("james");
    tokenService.addUser("james", token, null);

    // When
    Response.Content read = chatServer.read("james", token, roomId, "1", "false", null);

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
    String roomId = chatService.getRoom(users, null);

    String token = tokenService.getToken("john");
    tokenService.addUser("john", token, null);

    chatService.write("my message", "john", roomId, "false", null);

    String jsonMessages = chatService.read("mary", roomId, null);
    JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonMessages);
    JSONArray messages = (JSONArray) jsonObject.get("messages");
    JSONObject message = (JSONObject) messages.get(0);
    String messageId = (String) message.get("id");

    // When
    Response.Content edit = chatServer.edit("john", token, roomId, messageId, "my new message", null);

    // Then
    assertNotNull(edit);
    assertEquals(200, edit.getCode());
    MessageBean messageBean = chatService.getMessage(roomId, messageId, null);
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
    String roomId = chatService.getRoom(users, null);

    String tokenJohn = tokenService.getToken("john");
    tokenService.addUser("john", tokenJohn, null);
    String tokenJames = tokenService.getToken("james");
    tokenService.addUser("james", tokenJames, null);

    chatService.write("my message", "john", roomId, "false", null);

    String jsonMessages = chatService.read("mary", roomId, null);
    JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonMessages);
    JSONArray messages = (JSONArray) jsonObject.get("messages");
    JSONObject message = (JSONObject) messages.get(0);
    String messageId = (String) message.get("id");

    // When
    Response.Content edit = chatServer.edit("james", tokenJames, roomId, messageId, "my new message", null);

    // Then
    assertNotNull(edit);
    assertEquals(404, edit.getCode());
    MessageBean messageBean = chatService.getMessage(roomId, messageId, null);
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
    String roomId = chatService.getRoom(users, null);

    String token = tokenService.getToken("john");
    tokenService.addUser("john", token, null);

    chatService.write("my message", "john", roomId, "false", null);

    String jsonMessages = chatService.read("mary", roomId, null);
    JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonMessages);
    JSONArray messages = (JSONArray) jsonObject.get("messages");
    JSONObject message = (JSONObject) messages.get(0);
    String messageId = (String) message.get("id");

    // When
    Response.Content delete = chatServer.delete("john", token, roomId, messageId, null);

    // Then
    assertNotNull(delete);
    assertEquals(200, delete.getCode());
    MessageBean messageBean = chatService.getMessage(roomId, messageId, null);
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
    String roomId = chatService.getRoom(users, null);

    String tokenJohn = tokenService.getToken("john");
    tokenService.addUser("john", tokenJohn, null);
    String tokenJames = tokenService.getToken("james");
    tokenService.addUser("james", tokenJames, null);

    chatService.write("my message", "john", roomId, "false", null);

    String jsonMessages = chatService.read("mary", roomId, null);
    JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonMessages);
    JSONArray messages = (JSONArray) jsonObject.get("messages");
    JSONObject message = (JSONObject) messages.get(0);
    String messageId = (String) message.get("id");

    // When
    Response.Content delete = chatServer.delete("james", tokenJames, roomId, messageId, null);

    // Then
    assertNotNull(delete);
    assertEquals(404, delete.getCode());
    MessageBean messageBean = chatService.getMessage(roomId, messageId, null);
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
    
    userService.addUserFullName(benjamin, "Benjamin Paillereau", null);
    userService.addUserFullName(john, "John Smith", null);
    userService.toggleFavorite(benjamin, john, null);

    String tokenBen = tokenService.getToken(benjamin);
    tokenService.addUser(benjamin, tokenBen, null);
    
    String tokenJohn = tokenService.getToken(john);
    tokenService.addUser(john, tokenJohn, null);
  
    // When
    Response.Content isFavoriteBen = chatServer.isFavorite(benjamin, tokenBen, john, null);
    Response.Content isFavoriteJohn = chatServer.isFavorite(benjamin, tokenJohn, john, null);
    // Then
    assertNotNull(isFavoriteBen);
    assertEquals(200, isFavoriteBen.getCode());
    
    assertNotNull(isFavoriteJohn);
    assertEquals(404, isFavoriteJohn.getCode());
  }
  
}