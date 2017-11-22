package org.exoplatform.chat;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.chat.bootstrap.ServiceBootstrap;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.model.SpaceBean;
import org.exoplatform.chat.services.UserService;
import org.exoplatform.chat.services.mongodb.UserMongoDataStorage;
import org.exoplatform.chat.utils.ChatUtils;
import org.junit.Before;
import org.junit.Test;

public class SpaceTestCase extends AbstractChatTestCase
{
  @Before
  public void setUp()
  {
    ConnectionManager.getInstance().getDB().getCollection(UserMongoDataStorage.M_USERS_COLLECTION).drop();
    ServiceBootstrap.getUserService().addUserFullName("benjamin", "Benjamin Paillereau", null);
    ServiceBootstrap.getUserService().addUserEmail("benjamin", "bpaillereau@exoplatform.com", null);
    ServiceBootstrap.getUserService().addUserFullName("john", "John Smith", null);
    ServiceBootstrap.getUserService().addUserEmail("john", "john@exoplatform.com", null);
    ServiceBootstrap.getUserService().addUserFullName("mary", "Mary Williams", null);
    ServiceBootstrap.getUserService().addUserEmail("mary", "mary@exoplatform.com", null);
  }

  @Test
  public void testSpaces() throws Exception
  {
    log.info("SpaceTestCase.testSpaces");
    String user = "benjamin";

    List<SpaceBean> spaces = ServiceBootstrap.getUserService().getSpaces(user, null);
    assertEquals(0, spaces.size());
    SpaceBean space = new SpaceBean();
    space.setDisplayName("Test Space");
    space.setGroupId("test_space");
    space.setId("test_space");
    space.setShortName("Test Space");
    space.setTimestamp(System.currentTimeMillis());
    spaces.add(space);
    ServiceBootstrap.getUserService().setSpaces(user, spaces, null);

    spaces = ServiceBootstrap.getUserService().getSpaces(user, null);
    assertEquals(1, spaces.size());

    SpaceBean space2 = new SpaceBean();
    space2.setDisplayName("Test Space 2");
    space2.setGroupId("test_space_2");
    space2.setId("test_space_2");
    space2.setShortName("Test Space 2");
    space2.setTimestamp(System.currentTimeMillis());
    spaces.add(space2);
    ServiceBootstrap.getUserService().setSpaces(user, spaces, null);

    spaces = ServiceBootstrap.getUserService().getSpaces(user, null);
    assertEquals(2, spaces.size());

    spaces = new ArrayList<SpaceBean>();
    spaces.add(space);
    ServiceBootstrap.getUserService().setSpaces(user, spaces, null);
    spaces = ServiceBootstrap.getUserService().getSpaces(user, null);
    assertEquals(1, spaces.size());


    spaces = ServiceBootstrap.getUserService().getSpaces("john", null);
    assertEquals(0, spaces.size());

  }

  @Test
  public void testSpace() throws Exception
  {
    String user = "benjamin";
    List<SpaceBean> spaces = ServiceBootstrap.getUserService().getSpaces(user, null);
    SpaceBean space = new SpaceBean();
    String displayName = "Test Space";
    String spaceId = "123456789";
    String room = ChatUtils.getRoomId(spaceId);
    space.setDisplayName(displayName);
    space.setGroupId("test_space");
    space.setId(spaceId);
    space.setRoom(room);
    space.setShortName("Test Space");
    space.setTimestamp(System.currentTimeMillis());
    spaces.add(space);
    ServiceBootstrap.getUserService().setSpaces(user, spaces, null);

    SpaceBean target = ServiceBootstrap.getUserService().getSpaces(user, null).get(0);

    assertEquals(space, target);

  }

  @Test
  public void testSpaceUsers() throws Exception
  {
    String user = "benjamin";
    List<SpaceBean> spaces = ServiceBootstrap.getUserService().getSpaces(user, null);
    SpaceBean space = new SpaceBean();
    String displayName = "Test Space";
    String spaceId = "123456789";
    String room = ChatUtils.getRoomId(spaceId);
    space.setDisplayName(displayName);
    space.setGroupId("test_space");
    space.setId(spaceId);
    space.setRoom(room);
    space.setShortName("Test Space");
    space.setTimestamp(System.currentTimeMillis());
    spaces.add(space);

    ServiceBootstrap.getUserService().setSpaces(user, spaces, null);
    ServiceBootstrap.getUserService().setSpaces("john", spaces, null);

    assertEquals(2, ServiceBootstrap.getUserService().getUsers(space.getRoom(), null).size());

    ServiceBootstrap.getUserService().setSpaces("mary", spaces, null);
    assertEquals(3, ServiceBootstrap.getUserService().getUsers(space.getRoom(), null).size());

    SpaceBean space2 = new SpaceBean();
    String displayName2 = "Test Space 2";
    String spaceId2 = "129623459876";
    String room2 = ChatUtils.getRoomId(spaceId2);
    space2.setDisplayName(displayName2);
    space2.setGroupId("test_space_2");
    space2.setId(spaceId2);
    space2.setRoom(room2);
    space2.setShortName("Test Space 2");
    space2.setTimestamp(System.currentTimeMillis());
    spaces.add(space2);

    ServiceBootstrap.getUserService().setSpaces("mary", spaces, null);
    assertEquals(1, ServiceBootstrap.getUserService().getUsers(space2.getRoom(), null).size());
    assertEquals(3, ServiceBootstrap.getUserService().getUsers(space.getRoom(), null).size());

  }


}
