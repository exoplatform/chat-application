package org.benjp.chat;

import org.benjp.chat.bootstrap.ServiceBootstrap;
import org.benjp.listener.ConnectionManager;
import org.benjp.model.SpaceBean;
import org.benjp.services.TokenService;
import org.benjp.services.UserService;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpaceTestCase extends AbstractChatTestCase
{
  @Before
  public void setUp()
  {
    ConnectionManager.getInstance().getDB().getCollection(UserService.M_USERS_COLLECTION).drop();
    ServiceBootstrap.getUserService().addUserFullName("benjamin", "Benjamin Paillereau");
    ServiceBootstrap.getUserService().addUserEmail("benjamin", "bpaillereau@exoplatform.com");
    ServiceBootstrap.getUserService().addUserFullName("john", "John Smith");
    ServiceBootstrap.getUserService().addUserEmail("john", "john@exoplatform.com");
    ServiceBootstrap.getUserService().addUserFullName("mary", "Mary Williams");
    ServiceBootstrap.getUserService().addUserEmail("mary", "mary@exoplatform.com");
  }

  @Test
  public void testSpaces() throws Exception
  {
    log.info("SpaceTestCase.testSpaces");
    String user = "benjamin";

    List<SpaceBean> spaces = ServiceBootstrap.getUserService().getSpaces(user);
    assertEquals(0, spaces.size());
    SpaceBean space = new SpaceBean();
    space.setDisplayName("Test Space");
    space.setGroupId("test_space");
    space.setId("test_space");
    space.setShortName("Test Space");
    space.setTimestamp(System.currentTimeMillis());
    spaces.add(space);
    ServiceBootstrap.getUserService().setSpaces(user, spaces);

    spaces = ServiceBootstrap.getUserService().getSpaces(user);
    assertEquals(1, spaces.size());

    SpaceBean space2 = new SpaceBean();
    space2.setDisplayName("Test Space 2");
    space2.setGroupId("test_space_2");
    space2.setId("test_space_2");
    space2.setShortName("Test Space 2");
    space2.setTimestamp(System.currentTimeMillis());
    spaces.add(space2);
    ServiceBootstrap.getUserService().setSpaces(user, spaces);

    spaces = ServiceBootstrap.getUserService().getSpaces(user);
    assertEquals(2, spaces.size());

    spaces = new ArrayList<SpaceBean>();
    spaces.add(space);
    ServiceBootstrap.getUserService().setSpaces(user, spaces);
    spaces = ServiceBootstrap.getUserService().getSpaces(user);
    assertEquals(1, spaces.size());


    spaces = ServiceBootstrap.getUserService().getSpaces("john");
    assertEquals(0, spaces.size());

  }

  @Test
  public void testSpace() throws Exception
  {
    String user = "benjamin";
    List<SpaceBean> spaces = ServiceBootstrap.getUserService().getSpaces(user);
    SpaceBean space = new SpaceBean();
    space.setDisplayName("Test Space");
    space.setGroupId("test_space");
    space.setId("test_space");
    space.setShortName("Test Space");
    space.setTimestamp(System.currentTimeMillis());
    spaces.add(space);
    ServiceBootstrap.getUserService().setSpaces(user, spaces);

    SpaceBean target = ServiceBootstrap.getUserService().getSpaces(user).get(0);

    assertEquals(space, target);

  }

  @Test
  public void testSpaceUsers() throws Exception
  {
    String user = "benjamin";
    List<SpaceBean> spaces = ServiceBootstrap.getUserService().getSpaces(user);
    SpaceBean space = new SpaceBean();
    space.setDisplayName("Test Space");
    space.setGroupId("test_space");
    space.setId("test_space");
    space.setShortName("Test Space");
    space.setTimestamp(System.currentTimeMillis());
    spaces.add(space);

    ServiceBootstrap.getUserService().setSpaces(user, spaces);
    ServiceBootstrap.getUserService().setSpaces("john", spaces);

    assertEquals(2, ServiceBootstrap.getUserService().getUsers(space.getId()).size());

    ServiceBootstrap.getUserService().setSpaces("mary", spaces);
    assertEquals(3, ServiceBootstrap.getUserService().getUsers(space.getId()).size());

    SpaceBean space2 = new SpaceBean();
    space2.setDisplayName("Test Space 2");
    space2.setGroupId("test_space_2");
    space2.setId("test_space_2");
    space2.setShortName("Test Space 2");
    space2.setTimestamp(System.currentTimeMillis());
    spaces.add(space2);

    ServiceBootstrap.getUserService().setSpaces("mary", spaces);
    assertEquals(1, ServiceBootstrap.getUserService().getUsers(space2.getId()).size());
    assertEquals(3, ServiceBootstrap.getUserService().getUsers(space.getId()).size());

  }


}
