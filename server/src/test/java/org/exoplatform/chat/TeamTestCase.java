package org.exoplatform.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.chat.bootstrap.ServiceBootstrap;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.model.RoomBean;
import org.exoplatform.chat.model.RoomsBean;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.UserService;
import org.junit.Before;
import org.junit.Test;

public class TeamTestCase extends AbstractChatTestCase
{
  @Before
  public void setUp()
  {
    ConnectionManager.getInstance().getDB().getCollection(UserService.M_USERS_COLLECTION).drop();
    ConnectionManager.getInstance().getDB().getCollection(ChatService.M_ROOMS_COLLECTION).drop();
    ServiceBootstrap.getUserService().addUserFullName("benjamin", "Benjamin Paillereau", null);
    ServiceBootstrap.getUserService().addUserEmail("benjamin", "bpaillereau@exoplatform.com", null);
    ServiceBootstrap.getUserService().addUserFullName("john", "John Smith", null);
    ServiceBootstrap.getUserService().addUserEmail("john", "john@exoplatform.com", null);
    ServiceBootstrap.getUserService().addUserFullName("mary", "Mary Williams", null);
    ServiceBootstrap.getUserService().addUserEmail("mary", "mary@exoplatform.com", null);
  }

  @Test
  public void testTeamCreation() throws Exception
  {
    log.info("TeamTestCase.testTeam");
    String user = "benjamin";
    String user2 = "john";

    String room1 = ServiceBootstrap.getChatService().getTeamRoom("My VIP Team", user, null);
    String room2 = ServiceBootstrap.getChatService().getTeamRoom("My VIP Team 2", user, null);
    String room3 = ServiceBootstrap.getChatService().getTeamRoom("My VIP Team", user2, null);

    assertNotEquals(room1, room2);
    assertNotEquals(room1, room3);

    assertEquals(user, ServiceBootstrap.getChatService().getTeamCreator(room1, null));
    assertEquals(user, ServiceBootstrap.getChatService().getTeamCreator(room2, null));
    assertEquals(user2, ServiceBootstrap.getChatService().getTeamCreator(room3, null));

  }

  @Test
  public void testTeamUserAdd() throws Exception
  {
    log.info("TeamTestCase.testTeamUserAdd");
    String user = "benjamin";
    String user2 = "john";

    String room1 = ServiceBootstrap.getChatService().getTeamRoom("My VIP Team", user, null);
    String room2 = ServiceBootstrap.getChatService().getTeamRoom("My Other Team", user, null);
    String room3 = ServiceBootstrap.getChatService().getTeamRoom("My Last Team", user, null);

    assertEquals(user, ServiceBootstrap.getChatService().getTeamCreator(room1, null));

    ServiceBootstrap.getUserService().addTeamRoom(user, room1, null);
    ServiceBootstrap.getUserService().addTeamRoom(user, room2, null);
    ServiceBootstrap.getUserService().addTeamRoom(user, room3, null);

    List<RoomBean> teams = ServiceBootstrap.getUserService().getTeams(user, null);
    assertEquals(3, teams.size());

    List<String> users = new ArrayList<String>();
    users.add("john");
    users.add("mary");
    List<String> usersRemove = new ArrayList<String>();
    usersRemove.add("john");

    List<RoomBean> teamsJohn = ServiceBootstrap.getUserService().getTeams("john", null);
    assertEquals(0, teamsJohn.size());

    ServiceBootstrap.getUserService().addTeamUsers(room1, users, null);
    ServiceBootstrap.getUserService().addTeamUsers(room2, users, null);
    teamsJohn = ServiceBootstrap.getUserService().getTeams("john", null);
    assertEquals(2, teamsJohn.size());

    ServiceBootstrap.getUserService().removeTeamUsers(room1, usersRemove, null);
    teamsJohn = ServiceBootstrap.getUserService().getTeams("john", null);
    assertEquals(1, teamsJohn.size());

    List<RoomBean> teamsMary = ServiceBootstrap.getUserService().getTeams("mary", null);
    assertEquals(2, teamsMary.size());


  }


  @Test
  public void testTeamUserFilter() throws Exception
  {
    log.info("TeamTestCase.testTeamUserFilter");
    String user = "benjamin";

    String room1 = ServiceBootstrap.getChatService().getTeamRoom("My VIP Team", user, null);
    String room2 = ServiceBootstrap.getChatService().getTeamRoom("My Other Team", user, null);
    String room3 = ServiceBootstrap.getChatService().getTeamRoom("My Last Team", user, null);

    assertEquals(user, ServiceBootstrap.getChatService().getTeamCreator(room1, null));

    ServiceBootstrap.getUserService().addTeamRoom(user, room1, null);
    ServiceBootstrap.getUserService().addTeamRoom(user, room2, null);

    List<String> users = new ArrayList<String>();
    users.add("john");
    ServiceBootstrap.getUserService().addTeamUsers(room1, users, null);

    List<String> users1 = ServiceBootstrap.getUserService().getUsersFilterBy(user, room1, ChatService.TYPE_ROOM_TEAM, null);
    assertEquals(1, users1.size());

    List<String> users2 = ServiceBootstrap.getUserService().getUsersFilterBy(user, room2, ChatService.TYPE_ROOM_TEAM, null);
    assertEquals(0, users2.size());
  }

  @Test
  public void testTeamRoomName() throws Exception
  {
    log.info("TeamTestCase.testTeamRoomName");
    String user = "benjamin";

    String room1 = ServiceBootstrap.getChatService().getTeamRoom("My VIP Team", user, null);
    ServiceBootstrap.getUserService().addTeamRoom(user, room1, null);

    RoomsBean rooms = ServiceBootstrap.getChatService().getRooms(user, "", true, true, true, true, true,
            ServiceBootstrap.getNotificationService(), ServiceBootstrap.getUserService(), ServiceBootstrap.getTokenService(), null);

    assertEquals(1, rooms.getRooms().size());

    RoomBean room = rooms.getRooms().get(0);
    assertEquals("My VIP Team", room.getFullname());

    ServiceBootstrap.getChatService().setRoomName(room1, "VIP Team", null);

    rooms = ServiceBootstrap.getChatService().getRooms(user, "", true, true, true, true, true,
            ServiceBootstrap.getNotificationService(), ServiceBootstrap.getUserService(), ServiceBootstrap.getTokenService(), null);

    assertEquals(1, rooms.getRooms().size());

    room = rooms.getRooms().get(0);
    assertEquals("VIP Team", room.getFullname());


  }

}