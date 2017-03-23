package org.exoplatform.chat;

import org.exoplatform.chat.bootstrap.ServiceBootstrap;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.model.NotificationSettingsBean;
import org.exoplatform.chat.model.RoomBean;
import org.exoplatform.chat.model.RoomsBean;
import org.exoplatform.chat.model.SpaceBean;
import org.exoplatform.chat.services.*;
import org.exoplatform.chat.services.mongodb.ChatMongoDataStorage;
import org.exoplatform.chat.services.mongodb.UserMongoDataStorage;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONObject;

import static org.junit.Assert.*;

public class ChatTestCase extends AbstractChatTestCase
{
  @Before
  public void setUp()
  {
    List<String> users = new ArrayList<String>();
    users.add("benjamin");
    users.add("john");
    String roomId = ServiceBootstrap.getChatService().getRoom(users, null);
    String roomType = ServiceBootstrap.getChatService().getTypeRoomChat(roomId, null);
    ConnectionManager.getInstance().getDB().getCollection(ChatMongoDataStorage.M_ROOM_PREFIX+roomType).drop();
    users = new ArrayList<String>();
    users.add("benjamin");
    users.add("mary");
    roomId = ServiceBootstrap.getChatService().getRoom(users, null);
    roomType = ServiceBootstrap.getChatService().getTypeRoomChat(roomId, null);
    ConnectionManager.getInstance().getDB().getCollection(ChatMongoDataStorage.M_ROOM_PREFIX+roomType).drop();
    ConnectionManager.getInstance().getDB().getCollection(ChatMongoDataStorage.M_ROOMS_COLLECTION).drop();

    ConnectionManager.getInstance().getDB().getCollection(UserMongoDataStorage.M_USERS_COLLECTION).drop();

    UserService userService = ServiceBootstrap.getUserService();
    userService.addUserFullName("benjamin", "Benjamin Paillereau", null);
    userService.addUserEmail("benjamin", "bpaillereau@exoplatform.com", null);
    userService.addUserFullName("john", "John Smith", null);
    userService.addUserEmail("john", "john@exoplatform.com", null);
    userService.addUserFullName("mary", "Mary Williams", null);
    userService.addUserEmail("mary", "mary@exoplatform.com", null);
    userService.addUserFullName("james", "James Potter", null);
    userService.addUserEmail("james", "james@exoplatform.com", null);
  }

  @Test
  public void testGetRoom() throws Exception
  {
    ChatService chatService = ServiceBootstrap.getChatService();
    List<String> users = new ArrayList<String>();
    users.add("benjamin");
    users.add("john");
    String roomId = chatService.getRoom(users, null);

    users = new ArrayList<String>();
    users.add("john");
    users.add("benjamin");
    String roomId2 = chatService.getRoom(users, null);

    users = new ArrayList<String>();
    users.add("benjamin");
    users.add("mary");
    String roomId3 = chatService.getRoom(users, null);

    String roomId4 = chatService.getSpaceRoom("test-space", null);

    assertEquals(roomId, roomId2);
    assertNotEquals(roomId, roomId3);
    assertNotEquals(roomId, roomId4);

  }
  @Test
  public void testGetRoomTeamById() throws Exception {
    ChatService chatService = ServiceBootstrap.getChatService();
    String user = "Benjamin";
    String teamRoomName = "my team Room";
    String teamRoomId = chatService.getTeamRoom(teamRoomName, user, null);

    RoomBean teamRoom = chatService.getTeamRoomById(teamRoomId, null);
    assertEquals("The room id should be the same", teamRoomId, teamRoom.getRoom());
    assertEquals("The room name should be the same", teamRoomName, teamRoom.getFullName());
    assertEquals("The room owner should be the same", user, teamRoom.getUser());
    assertTrue("The room should be a team room", teamRoom.getType().equals(ChatService.TYPE_ROOM_TEAM));
    assertFalse("The room should not be a space room", teamRoom.getType().equals(ChatService.TYPE_ROOM_SPACE));

  }

  @Test
  public void testTextFormatRead() throws Exception
  {
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    List<String> users = new ArrayList<String>();
    users.add("benjamin");
    users.add("john");
    String roomId = chatService.getRoom(users, null);

    String resp = chatService.read("john", roomId, false, null, null);
    String json = "{\"room\": \"" + roomId + "\",\"messages\": []}";
    assertEquals(json, resp);

    resp = chatService.read("john", roomId, true, null, null);
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
    String roomId = chatService.getRoom(users, null);

    chatService.write("foo", "benjamin", roomId, "false", null);
    String resp = chatService.read("john", roomId, true, null, null);
    assertEquals(47, resp.length());
    assertTrue(resp.endsWith("] Benjamin Paillereau: foo\n"));

    chatService.write("bar", "john", roomId, "false", null);
    resp = chatService.read("benjamin", roomId, true, null, null);
    assertEquals(85, resp.length());
    assertTrue(resp.endsWith("] John Smith: bar\n"));
  }

  @Test
  public void testWriteJson() throws Exception
  {
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    List<String> users = new ArrayList<String>();
    users.add("benjamin");
    users.add("john");
    String roomId = chatService.getRoom(users, null);

    chatService.write("foo", "benjamin", roomId, "false", null);
    String resp = chatService.read("john", roomId, null);

    JSONObject jsonObject = (JSONObject)JSONValue.parse(resp);
    String room = (String)jsonObject.get("room");
    assertEquals(roomId, room);

    String timestamp = (String)jsonObject.get("timestamp");
    assertNotNull(timestamp);

    JSONArray messages = (JSONArray)jsonObject.get("messages");
    assertEquals(1, messages.size());

    chatService.write("bar", "john", roomId, "false", null);
    resp = chatService.read("benjamin", roomId, null);
    jsonObject = (JSONObject)JSONValue.parse(resp);
    messages = (JSONArray)jsonObject.get("messages");
    assertEquals(2, messages.size());

    String message = (String)((JSONObject)messages.get(0)).get("msg");
    assertEquals("bar", message);
    message = (String)((JSONObject)messages.get(1)).get("msg");
    assertEquals("foo", message);

    JSONObject msgJson = (JSONObject)messages.get(0);

    assertNotNull(msgJson.get("msgId"));
    assertNotNull(msgJson.get("timestamp"));
    assertNotNull(msgJson.get("user"));
    assertNotNull(msgJson.get("fullname"));
    assertNull(msgJson.get("type"));
    assertNotNull(msgJson.get("isSystem"));


  }

  @Test
  public void testEdit() throws Exception
  {
    ChatDataStorage chatDataStorage = ServiceBootstrap.getChatDataStorage();
    UserService userService = ServiceBootstrap.getUserService();
    List<String> users = new ArrayList<String>();
    users.add("benjamin");
    users.add("john");
    String roomId = chatDataStorage.getRoom(users, null);

    chatDataStorage.write("foo", "benjamin", roomId, "false", null);
    String resp = chatDataStorage.read(roomId, null);
    JSONObject jsonObject = (JSONObject)JSONValue.parse(resp);
    JSONArray messages = (JSONArray)jsonObject.get("messages");
    assertEquals(1, messages.size());

    String message = (String)((JSONObject)messages.get(0)).get("msg");
    String id = (String)((JSONObject)messages.get(0)).get("msgId");

    assertEquals("foo", message);

    chatDataStorage.edit(roomId, "benjamin", id, "bar", null);

    resp = chatDataStorage.read(roomId, null);
    jsonObject = (JSONObject)JSONValue.parse(resp);
    messages = (JSONArray)jsonObject.get("messages");
    assertEquals(1, messages.size());

    JSONObject msgJson = (JSONObject)messages.get(0);
    assertEquals(id, msgJson.get("msgId"));
    assertEquals("bar", msgJson.get("msg"));
    assertEquals(ChatService.TYPE_EDITED, msgJson.get("type"));

  }

  @Test
  public void testDelete() throws Exception
  {
    ChatDataStorage chatDataStorage = ServiceBootstrap.getChatDataStorage();
    UserService userService = ServiceBootstrap.getUserService();
    List<String> users = new ArrayList<String>();
    users.add("benjamin");
    users.add("john");
    String roomId = chatDataStorage.getRoom(users, null);

    chatDataStorage.write("foo", "benjamin", roomId, "false", null);
    chatDataStorage.write("bar", "benjamin", roomId, "false", null);
    String resp = chatDataStorage.read(roomId, null);
    JSONObject jsonObject = (JSONObject)JSONValue.parse(resp);
    JSONArray messages = (JSONArray)jsonObject.get("messages");
    assertEquals(2, messages.size());

    String id = (String)((JSONObject)messages.get(0)).get("msgId");

    chatDataStorage.delete(roomId, "benjamin", id, null);

    resp = chatDataStorage.read(roomId, null);
    jsonObject = (JSONObject)JSONValue.parse(resp);
    messages = (JSONArray)jsonObject.get("messages");
    assertEquals(2, messages.size());

    String id3 = (String)((JSONObject)messages.get(0)).get("msgId");
    String type3 = (String)((JSONObject)messages.get(0)).get("type");

    assertEquals(id, id3);
    assertEquals(ChatService.TYPE_DELETED, type3);

  }

  @Test
  public void testGetExistingRooms() throws Exception
  {
    ChatService chatService = ServiceBootstrap.getChatService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    NotificationService notificationService = ServiceBootstrap.getNotificationService();
    List<String> users = new ArrayList<String>();
    users.add("benjamin");
    users.add("john");
    String roomId1 = chatService.getRoom(users, null);

    users = new ArrayList<String>();
    users.add("benjamin");
    users.add("mary");
    chatService.getRoom(users, null);


    chatService.write("foo", "benjamin", roomId1, "false", null);

    List<RoomBean> rooms = chatService.getExistingRooms("benjamin", false, false, notificationService, tokenService, null);
    assertEquals(2, rooms.size());

    rooms = chatService.getExistingRooms("john", false, false, notificationService, tokenService, null);
    assertEquals(1, rooms.size());

    rooms = chatService.getExistingRooms("mary", false, false, notificationService, tokenService, null);
    assertEquals(1, rooms.size());

    rooms = chatService.getExistingRooms("james", false, false, notificationService, tokenService, null);
    assertEquals(0, rooms.size());



  }

  @Test
  public void testGetRooms() throws Exception
  {
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    NotificationService notificationService = ServiceBootstrap.getNotificationService();
    List<String> users = new ArrayList<String>();
    users.add("benjamin");
    users.add("john");
    String roomId1 = chatService.getRoom(users, null);

    users = new ArrayList<String>();
    users.add("benjamin");
    users.add("mary");
    String roomId2 = chatService.getRoom(users, null);

    users = new ArrayList<String>();
    users.add("benjamin");
    users.add("james");
    String roomId3 = chatService.getRoom(users, null);

    chatService.write("foo", "benjamin", roomId1, "false", null);
    chatService.write("bar", "john", roomId1, "false", null);

    chatService.write("foo", "benjamin", roomId2, "false", null);
    chatService.write("bar", "mary", roomId2, "false", null);

    chatService.write("foo", "benjamin", roomId3, "false", null);
    chatService.write("bar", "james", roomId3, "false", null);


    List<SpaceBean> spaces = ServiceBootstrap.getUserService().getSpaces("benjamin", null);
    SpaceBean space = new SpaceBean();
    space.setDisplayName("Test Space");
    space.setGroupId("test_space");
    space.setId("test_space");
    space.setShortName("Test Space");
    space.setTimestamp(System.currentTimeMillis());
    spaces.add(space);

    ServiceBootstrap.getUserService().setSpaces("john", spaces, null);

    SpaceBean space2 = new SpaceBean();
    space2.setDisplayName("Test Space 2");
    space2.setGroupId("test_space_2");
    space2.setId("test_space_2");
    space2.setShortName("Test Space 2");
    space2.setTimestamp(System.currentTimeMillis());
    spaces.add(space2);

    ServiceBootstrap.getUserService().setSpaces("benjamin", spaces, null);


    String spaceId1 = chatService.getSpaceRoom("test_space", null);
    chatService.write("foo", "benjamin", spaceId1, "false", null);

    String spaceId2 = chatService.getSpaceRoom("test_space_2", null);
    chatService.write("foo", "benjamin", spaceId2, "false", null);
    chatService.write("foo", "john", spaceId2, "false", null);


    //public RoomsBean getRooms(String user, String filter,
    //      boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin,
    //      NotificationService notificationService, UserService userService, TokenService tokenService)

    RoomsBean roomsBenAll = chatService.getRooms("benjamin", null,
            true, true, false, true, false,
            notificationService, tokenService, null);
    RoomsBean roomsMaryAll = chatService.getRooms("mary", null,
            true, true, false, true, false,
            notificationService, tokenService, null);

    RoomsBean roomsBenUsers = chatService.getRooms("benjamin", null,
            true, false, false, true, false,
            notificationService, tokenService, null);
    RoomsBean roomsMaryUsers = chatService.getRooms("mary", null,
            true, false, false, true, false,
            notificationService, tokenService, null);
    RoomsBean roomsMarySpaces = chatService.getRooms("mary", null,
            false, true, false, false, false,
            notificationService, tokenService, null);

    Thread.sleep(110);

    RoomsBean roomsBenOffline = chatService.getRooms("benjamin", null,
            true, false, false, true, false,
            notificationService, tokenService, null);

    RoomsBean roomsBenOnline = chatService.getRooms("benjamin", null,
            true, false, false, false, false,
            notificationService, tokenService, null);

    assertEquals(5, roomsBenAll.getRooms().size());
    assertEquals(3, roomsBenUsers.getRooms().size());
    assertEquals(3, roomsBenOffline.getRooms().size());
    assertEquals(0, roomsBenOnline.getRooms().size());
    assertEquals(1, roomsMaryAll.getRooms().size());
    assertEquals(0, roomsMarySpaces.getRooms().size());
    assertEquals(1, roomsMaryUsers.getRooms().size());

  }

  @Test
  public void testGetRoomsFiltered() throws Exception
  {
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    NotificationService notificationService = ServiceBootstrap.getNotificationService();
    List<String> users = new ArrayList<String>();
    users.add("benjamin");
    users.add("john");
    String roomId1 = chatService.getRoom(users, null);

    users = new ArrayList<String>();
    users.add("benjamin");
    users.add("mary");
    String roomId2 = chatService.getRoom(users, null);

    users = new ArrayList<String>();
    users.add("benjamin");
    users.add("james");
    String roomId3 = chatService.getRoom(users, null);

    chatService.write("foo", "benjamin", roomId1, "false", null);
    chatService.write("bar", "john", roomId1, "false", null);

    chatService.write("foo", "benjamin", roomId2, "false", null);
    chatService.write("bar", "mary", roomId2, "false", null);

    chatService.write("foo", "benjamin", roomId3, "false", null);
    chatService.write("bar", "james", roomId3, "false", null);


    List<SpaceBean> spaces = ServiceBootstrap.getUserService().getSpaces("benjamin", null);
    SpaceBean space = new SpaceBean();
    space.setDisplayName("Test Space");
    space.setGroupId("test_space");
    space.setId("test_space");
    space.setShortName("Test Space");
    space.setTimestamp(System.currentTimeMillis());
    spaces.add(space);

    ServiceBootstrap.getUserService().setSpaces("john", spaces, null);

    SpaceBean space2 = new SpaceBean();
    space2.setDisplayName("Test Space 2");
    space2.setGroupId("test_space_2");
    space2.setId("test_space_2");
    space2.setShortName("Test Space 2");
    space2.setTimestamp(System.currentTimeMillis());
    spaces.add(space2);

    ServiceBootstrap.getUserService().setSpaces("benjamin", spaces, null);


    String spaceId1 = chatService.getSpaceRoom("test_space", null);
    chatService.write("foo", "benjamin", spaceId1, "false", null);

    String spaceId2 = chatService.getSpaceRoom("test_space_2", null);
    chatService.write("foo", "benjamin", spaceId2, "false", null);
    chatService.write("foo", "john", spaceId2, "false", null);

    RoomsBean roomsBenAll = chatService.getRooms("benjamin", null,
            true, true, false, true, false,
            notificationService, tokenService, null);

    RoomsBean roomsBenTest = chatService.getRooms("benjamin", "Test",
            true, true, false, true, false,
            notificationService, tokenService, null);
    RoomsBean roomsBenTeSp = chatService.getRooms("benjamin", "Te Sp",
            true, true, false, true, false,
            notificationService, tokenService, null);
    RoomsBean roomsBenTeSpe = chatService.getRooms("benjamin", "Te Spe",
            true, true, false, true, false,
            notificationService, tokenService, null);
    RoomsBean roomsBenJohn = chatService.getRooms("benjamin", "john",
            true, true, false, true, false,
            notificationService, tokenService, null);
    RoomsBean roomsBenJo = chatService.getRooms("benjamin", "Jo",
            true, true, false, true, false,
            notificationService, tokenService, null);
    RoomsBean roomsBenMaWi = chatService.getRooms("benjamin", "Ma Wi",
            true, true, false, true, false,
            notificationService, tokenService, null);

    Thread.sleep(110);

    assertEquals(5, roomsBenAll.getRooms().size());
    assertEquals(2, roomsBenTest.getRooms().size());
    assertEquals(2, roomsBenTeSp.getRooms().size());
    assertEquals(0, roomsBenTeSpe.getRooms().size());
    assertEquals(1, roomsBenJohn.getRooms().size());
    assertEquals(1, roomsBenJo.getRooms().size());
    assertEquals(1, roomsBenMaWi.getRooms().size());

  }
  
  @Test
  public void testSetRoomNotificationTrigger() throws Exception
  {
    ChatService chatService = ServiceBootstrap.getChatService();
    UserService userService = ServiceBootstrap.getUserService();
    TokenService tokenService = ServiceBootstrap.getTokenService();
    NotificationService notificationService = ServiceBootstrap.getNotificationService();
    List<String> users = new ArrayList<String>();
    
    String benjamin = "benjamin";
    String john = "john";
    String normal = ChatService.NOTIFY_ME_ON_ROOM_NORMAL;
    users.add(benjamin);
    users.add(john);
    String roomId1 = chatService.getRoom(users, null);
    
    userService.setRoomNotificationTrigger(benjamin, roomId1,"", normal, null, 1000);
    userService.setRoomNotificationTrigger(benjamin, roomId1,"", "silence", null, 500);
    
    NotificationSettingsBean userDNS = userService.getUserDesktopNotificationSettings(benjamin, null);
    assertNotNull(userDNS);
    assertTrue(userDNS instanceof NotificationSettingsBean);
  }
}
