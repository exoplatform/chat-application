package org.benjp.chat;

import org.benjp.chat.bootstrap.ServiceBootstrap;
import org.benjp.model.SpaceBean;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class SpaceTestCase extends AbstractChatTestCase
{

  @Test
  public void testSpaces() throws Exception
  {
    log.info("SpaceTestCase.testSpaces");
    String user = "benjamin";
    List<SpaceBean> spaces = ServiceBootstrap.getUserService().getSpaces(user);
    assertTrue(spaces.size()==0);
    SpaceBean space = new SpaceBean();
    space.setDisplayName("Test Space");
    space.setGroupId("test_space");
    space.setId("test_space");
    space.setShortName("Test Space");
    space.setTimestamp(System.currentTimeMillis());
    spaces.add(space);
    ServiceBootstrap.getUserService().setSpaces(user, spaces);

    spaces = ServiceBootstrap.getUserService().getSpaces(user);
    assertTrue(spaces.size()==1);
  }

}
