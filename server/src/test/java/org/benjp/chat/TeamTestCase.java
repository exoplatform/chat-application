package org.benjp.chat;

import org.benjp.chat.bootstrap.ServiceBootstrap;
import org.benjp.listener.ConnectionManager;
import org.benjp.model.RoomBean;
import org.benjp.model.RoomsBean;
import org.benjp.model.SpaceBean;
import org.benjp.services.ChatService;
import org.benjp.services.UserService;
import org.benjp.utils.ChatUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TeamTestCase extends AbstractChatTestCase
{
  @Before
  public void setUp()
  {
    ConnectionManager.getInstance().getDB().getCollection(UserService.M_USERS_COLLECTION).drop();
    ConnectionManager.getInstance().getDB().getCollection(ChatService.M_ROOMS_COLLECTION).drop();
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
    log.info("TeamTestCase.testTeam");
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
            ServiceBootstrap.getNotificationService(), ServiceBootstrap.getUserService(), ServiceBootstrap.getTokenService());

    assertEquals(1, rooms.getRooms().size());

    RoomBean room = rooms.getRooms().get(0);
    assertEquals("My VIP Team", room.getFullname());

    ServiceBootstrap.getChatService().setRoomName(room1, "VIP Team");

    rooms = ServiceBootstrap.getChatService().getRooms(user, "", true, true, true, true, true,
            ServiceBootstrap.getNotificationService(), ServiceBootstrap.getUserService(), ServiceBootstrap.getTokenService());

    assertEquals(1, rooms.getRooms().size());

    room = rooms.getRooms().get(0);
    assertEquals("VIP Team", room.getFullname());


  }

}