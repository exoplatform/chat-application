package org.exoplatform.chat;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.chat.bootstrap.ServiceBootstrap;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.model.RoomBean;
import org.exoplatform.chat.model.RoomsBean;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.mongodb.ChatMongoDataStorage;
import org.exoplatform.chat.services.mongodb.UserMongoDataStorage;
import org.junit.Before;
import org.junit.Test;

public class TeamTestCase extends AbstractChatTestCase
{
  @Before
  public void setUp()
  {
    ConnectionManager.getInstance().getDB().getCollection(UserMongoDataStorage.M_USERS_COLLECTION).drop();
    ConnectionManager.getInstance().getDB().getCollection(ChatMongoDataStorage.M_ROOMS_COLLECTION).drop();
    ServiceBootstrap.getUserService().addUserFullName("benjamin", "Benjamin Paillereau");
    ServiceBootstrap.getUserService().addUserEmail("benjamin", "bpaillereau@exoplatform.com");
    ServiceBootstrap.getUserService().addUserFullName("john", "John Smith");
    ServiceBootstrap.getUserService().addUserEmail("john", "john@exoplatform.com");
    ServiceBootstrap.getUserService().addUserFullName("mary", "Mary Williams");
    ServiceBootstrap.getUserService().addUserEmail("mary", "mary@exoplatform.com");
  }

  @Test
  public void testTeamCreation() throws Exception
  {
    log.info("TeamTestCase.testTeamCreation");
    String user = "benjamin";
    String user2 = "john";

    String room1 = ServiceBootstrap.getChatService().getTeamRoom("My VIP Team", user);
    String room2 = ServiceBootstrap.getChatService().getTeamRoom("My VIP Team 2", user);
    String room3 = ServiceBootstrap.getChatService().getTeamRoom("My VIP Team", user2);

    assertNotEquals(room1, room2);
    assertNotEquals(room1, room3);

    assertEquals(user, ServiceBootstrap.getChatService().getTeamCreator(room1));
    assertEquals(user, ServiceBootstrap.getChatService().getTeamCreator(room2));
    assertEquals(user2, ServiceBootstrap.getChatService().getTeamCreator(room3));

  }

  @Test
  public void testTeamRoomsByName() throws Exception
  {
    log.info("TeamTestCase.testTeamCreation");
    String user = "benjamin";
    String user2 = "john";

    String room1 = ServiceBootstrap.getChatService().getTeamRoom("My VIP Team", user);
    String room2 = ServiceBootstrap.getChatService().getTeamRoom("My VIP Team 2", user);
    String room3 = ServiceBootstrap.getChatService().getTeamRoom("My VIP Team", user2);

    assertNotEquals(room1, room2);
    assertNotEquals(room1, room3);

    assertEquals(2, ServiceBootstrap.getChatService().getTeamRoomsByName("My VIP Team").size());
    assertEquals(1, ServiceBootstrap.getChatService().getTeamRoomsByName("My VIP Team 2").size());
    assertEquals(0, ServiceBootstrap.getChatService().getTeamRoomsByName("Not existing room").size());

  }

  @Test
  public void testTeamRoomDeletion() throws Exception
  {
    log.info("TeamTestCase.testTeamDeletion");
    String user = "benjamin";
    String teamName = "My Team to Delete";

    // create a team room
    String roomId = ServiceBootstrap.getChatService().getTeamRoom(teamName, user);
    RoomBean room = ServiceBootstrap.getChatService().getTeamRoomById(roomId);
    assertNotNull("The room should exist", room);

    // delete the Team Room
    ServiceBootstrap.getChatService().deleteTeamRoom(roomId, user);

    List<String> members = ServiceBootstrap.getUserService().getUsersFilterBy(null, roomId, ChatService.TYPE_ROOM_TEAM);
    assertTrue("No user should be member of the deleted Team Room", members.isEmpty());

    RoomBean roomDelete = ServiceBootstrap.getChatService().getTeamRoomById(roomId);
    assertNull("The room should have been deleted", roomDelete);

  }

  @Test
  public void testTeamRoomLoadById() throws Exception
  {
    log.info("TeamTestCase.testTeamRoomLoadById");
    String user = "benjamin";
    String roomSpaceName = "RoomSpace";
    String roomTeamName = "RoomTeam";

    // create a team room
    String roomTeamId = ServiceBootstrap.getChatService().getTeamRoom(roomTeamName, user);
    RoomBean roomTeam = ServiceBootstrap.getChatService().getTeamRoomById(roomTeamId);
    assertNotNull("The room should exists", roomTeam);
    assertEquals("The room name is not good", roomTeamName, roomTeam.getFullName());
    assertEquals("The room owner is not good", user, roomTeam.getUser());

    // create a space room
    String roomId = ServiceBootstrap.getChatService().getSpaceRoom(roomSpaceName);
    RoomBean room = ServiceBootstrap.getChatService().getTeamRoomById(roomId); // nothing should be loaded
    assertNull("The team room should not exist", room);
  }

  @Test
  public void testTeamRoomLoadByIdNonExisting() throws Exception
  {
    log.info("TeamTestCase.testTeamRoomLoadByIdNonExisting");
    String roomId = "non-existing-id";

    RoomBean nullTeamRoom = ServiceBootstrap.getChatService().getTeamRoomById(roomId);
    assertNull("The returned room should be null", nullTeamRoom);
  }

  @Test
  public void testTeamUserAdd() throws Exception
  {
    log.info("TeamTestCase.testTeamUserAdd");
    String user = "benjamin";
    String user2 = "john";

    String room1 = ServiceBootstrap.getChatService().getTeamRoom("My VIP Team", user);
    String room2 = ServiceBootstrap.getChatService().getTeamRoom("My Other Team", user);
    String room3 = ServiceBootstrap.getChatService().getTeamRoom("My Last Team", user);

    assertEquals(user, ServiceBootstrap.getChatService().getTeamCreator(room1));

    ServiceBootstrap.getUserService().addTeamRoom(user, room1);
    ServiceBootstrap.getUserService().addTeamRoom(user, room2);
    ServiceBootstrap.getUserService().addTeamRoom(user, room3);

    List<RoomBean> teams = ServiceBootstrap.getUserService().getTeams(user);
    assertEquals(3, teams.size());

    List<String> users = new ArrayList<String>();
    users.add("john");
    users.add("mary");
    List<String> usersRemove = new ArrayList<String>();
    usersRemove.add("john");

    List<RoomBean> teamsJohn = ServiceBootstrap.getUserService().getTeams("john");
    assertEquals(0, teamsJohn.size());

    ServiceBootstrap.getUserService().addTeamUsers(room1, users);
    ServiceBootstrap.getUserService().addTeamUsers(room2, users);
    teamsJohn = ServiceBootstrap.getUserService().getTeams("john");
    assertEquals(2, teamsJohn.size());

    ServiceBootstrap.getUserService().removeTeamUsers(room1, usersRemove);
    teamsJohn = ServiceBootstrap.getUserService().getTeams("john");
    assertEquals(1, teamsJohn.size());

    List<RoomBean> teamsMary = ServiceBootstrap.getUserService().getTeams("mary");
    assertEquals(2, teamsMary.size());


  }


  @Test
  public void testTeamUserFilter() throws Exception
  {
    log.info("TeamTestCase.testTeamUserFilter");
    String user = "benjamin";

    String room1 = ServiceBootstrap.getChatService().getTeamRoom("My VIP Team", user);
    String room2 = ServiceBootstrap.getChatService().getTeamRoom("My Other Team", user);
    String room3 = ServiceBootstrap.getChatService().getTeamRoom("My Last Team", user);

    assertEquals(user, ServiceBootstrap.getChatService().getTeamCreator(room1));

    ServiceBootstrap.getUserService().addTeamRoom(user, room1);
    ServiceBootstrap.getUserService().addTeamRoom(user, room2);

    List<String> users = new ArrayList<String>();
    users.add("john");
    ServiceBootstrap.getUserService().addTeamUsers(room1, users);

    List<String> users1 = ServiceBootstrap.getUserService().getUsersFilterBy(user, room1, ChatService.TYPE_ROOM_TEAM);
    assertEquals(1, users1.size());

    List<String> users2 = ServiceBootstrap.getUserService().getUsersFilterBy(user, room2, ChatService.TYPE_ROOM_TEAM);
    assertEquals(0, users2.size());
  }

  @Test
  public void testTeamRoomName() throws Exception
  {
    log.info("TeamTestCase.testTeamRoomName");
    String user = "benjamin";

    String room1 = ServiceBootstrap.getChatService().getTeamRoom("My VIP Team", user);
    ServiceBootstrap.getUserService().addTeamRoom(user, room1);

    RoomsBean rooms = ServiceBootstrap.getChatService().getRooms(user, "", true, true, true, true, true,
            ServiceBootstrap.getNotificationService(), ServiceBootstrap.getTokenService());

    assertEquals(1, rooms.getRooms().size());

    RoomBean room = rooms.getRooms().get(0);
    assertEquals("My VIP Team", room.getFullName());

    ServiceBootstrap.getChatService().setRoomName(room1, "VIP Team");

    rooms = ServiceBootstrap.getChatService().getRooms(user, "", true, true, true, true, true,
            ServiceBootstrap.getNotificationService(), ServiceBootstrap.getTokenService());

    assertEquals(1, rooms.getRooms().size());

    room = rooms.getRooms().get(0);
    assertEquals("VIP Team", room.getFullName());


  }

}
