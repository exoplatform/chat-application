package org.benjp.chat;

import org.benjp.chat.bootstrap.ServiceBootstrap;
import org.benjp.listener.ConnectionManager;
import org.benjp.model.SpaceBean;
import org.benjp.services.ChatService;
import org.benjp.services.UserService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class ChatTestCase extends AbstractChatTestCase
{
  @Before
  public void setUp()
  {
    List<String> users = new ArrayList<String>();
    users.add("benjamin");
    users.add("john");
    String roomId = ServiceBootstrap.getChatService().getRoom(users);
    ConnectionManager.getInstance().getDB().getCollection(ChatService.M_ROOM_PREFIX+roomId).drop();
    ConnectionManager.getInstance().getDB().getCollection(UserService.M_USERS_COLLECTION).drop();
    UserService userService = ServiceBootstrap.getUserService();
    userService.addUserFullName("benjamin", "Benjamin Paillereau");
    userService.addUserEmail("benjamin", "bpaillereau@exoplatform.com");
    userService.addUserFullName("john", "John Smith");
    userService.addUserEmail("john", "john@exoplatform.com");
    userService.addUserFullName("mary", "Mary Williams");
    userService.addUserEmail("mary", "mary@exoplatform.com");
  }

  @Test
  public void testGetRoom() throws Exception
  {
    ChatService chatService = ServiceBootstrap.getChatService();
    List<String> users = new ArrayList<String>();
    users.add("benjamin");
    users.add("john");
    String roomId = chatService.getRoom(users);

    users = new ArrayList<String>();
    users.add("john");
    users.add("benjamin");
    String roomId2 = chatService.getRoom(users);

    users = new ArrayList<String>();
    users.add("benjamin");
    users.add("mary");
    String roomId3 = chatService.getRoom(users);

    assertEquals(roomId, roomId2);
    assertNotEquals(roomId, roomId3);

  }

  @Test
  public void testTextFormatRead() throws Exception
  {
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    List<String> users = new ArrayList<String>();
    users.add("benjamin");
    users.add("john");
    String roomId = chatService.getRoom(users);

    String resp = chatService.read(roomId, userService, false);
    String json = "{\"messages\": []}";
    assertEquals(json, resp);

    resp = chatService.read(roomId, userService, true);
    String text = "no messages";
    assertEquals(text, resp);
  }

  @Test
  public void testWriteText() throws Exception
  {
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    List<String> users = new ArrayList<String>();
    users.add("benjamin");
    users.add("john");
    String roomId = chatService.getRoom(users);

    chatService.write("foo", "benjamin", roomId, "false");
    String resp = chatService.read(roomId, userService, true);
    assertEquals(46, resp.length());
    assertTrue(resp.endsWith("] Benjamin Paillereau: foo"));

    chatService.write("bar", "john", roomId, "false");
    resp = chatService.read(roomId, userService, true);
    assertEquals(83, resp.length());
    assertTrue(resp.endsWith("] John Smith: bar"));
  }

  @Test
  public void testWriteJson() throws Exception
  {
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    List<String> users = new ArrayList<String>();
    users.add("benjamin");
    users.add("john");
    String roomId = chatService.getRoom(users);

    chatService.write("foo", "benjamin", roomId, "false");
    String resp = chatService.read(roomId, userService);
    JSONObject jsonObject = (JSONObject)JSONValue.parse(resp);
    String room = (String)jsonObject.get("room");
    assertEquals(roomId, room);

    JSONArray messages = (JSONArray)jsonObject.get("messages");
    assertEquals(1, messages.size());

    chatService.write("bar", "john", roomId, "false");
    resp = chatService.read(roomId, userService);
    jsonObject = (JSONObject)JSONValue.parse(resp);
    messages = (JSONArray)jsonObject.get("messages");
    assertEquals(2, messages.size());

    String message = (String)((JSONObject)messages.get(0)).get("message");
    assertEquals("foo", message);
    message = (String)((JSONObject)messages.get(1)).get("message");
    assertEquals("bar", message);

  }


}
