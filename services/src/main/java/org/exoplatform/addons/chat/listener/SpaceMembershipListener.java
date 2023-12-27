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
  public void spaceAccessEdited(SpaceLifeCycleEvent event) {
  }

  @Override
  public void spaceBannerEdited(SpaceLifeCycleEvent event) {
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
  public void applicationActivated(SpaceLifeCycleEvent event) {
  }

  @Override
  public void applicationAdded(SpaceLifeCycleEvent event) {
  }

  @Override
  public void applicationDeactivated(SpaceLifeCycleEvent event) {
  }

  @Override
  public void applicationRemoved(SpaceLifeCycleEvent event) {
  }

  @Override
  public void spaceRenamed(SpaceLifeCycleEvent event) {
    Space space = event.getSpace();
    refreshSpaceChatRoom(space);
  }

  @Override
  public void spaceDescriptionEdited(SpaceLifeCycleEvent event) {
  }

  @Override
  public void spaceAvatarEdited(SpaceLifeCycleEvent event) {
  }

  @Override
  public void addInvitedUser(SpaceLifeCycleEvent event) {
  }

  @Override
  public void addPendingUser(SpaceLifeCycleEvent event) {
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
