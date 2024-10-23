package org.exoplatform.addons.chat.listener;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.social.core.space.SpaceListenerPlugin;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleEvent;

public class SpaceMembershipListener extends SpaceListenerPlugin {

  @Override
  public void grantedLead(SpaceLifeCycleEvent event) {
    saveSpaces(event.getTarget());
  }

  @Override
  public void joined(SpaceLifeCycleEvent event) {
    saveSpaces(event.getTarget());
  }

  @Override
  public void left(SpaceLifeCycleEvent event) {
    saveSpaces(event.getTarget());
  }

  @Override
  public void revokedLead(SpaceLifeCycleEvent event) {
    saveSpaces(event.getTarget());
  }

  @Override
  public void spaceCreated(SpaceLifeCycleEvent event) {
    Space space = event.getSpace();
    refreshSpaceChatRoom(space);
  }

  @Override
  public void spaceRemoved(SpaceLifeCycleEvent event) {
    Space space = event.getSpace();
    refreshSpaceChatRoom(space);
  }

  @Override
  public void spaceRenamed(SpaceLifeCycleEvent event) {
    Space space = event.getSpace();
    refreshSpaceChatRoom(space);
  }

  private void refreshSpaceChatRoom(Space space) {
    String[] members = space.getMembers();
    if (members != null) {
      for (String username : members) {
        saveSpaces(username);
      }
    }
  }

  private void saveSpaces(String username) {
    if (StringUtils.isNotBlank(username)) {
      ServerBootstrap.saveSpaces(username);
    }
  }
}
