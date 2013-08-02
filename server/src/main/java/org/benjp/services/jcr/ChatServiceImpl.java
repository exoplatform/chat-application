package org.benjp.services.jcr;

import org.benjp.model.RoomBean;
import org.benjp.model.RoomsBean;
import org.benjp.services.ChatService;
import org.benjp.services.NotificationService;
import org.benjp.services.TokenService;
import org.benjp.services.UserService;

import java.util.List;

public class ChatServiceImpl extends AbstractJCRService implements ChatService
{
  public void write(String message, String user, String room, String isSystem) {
  }

  public void write(String message, String user, String room, String isSystem, String options) {
  }

  public void delete(String room, String user, String messageId) {
  }

  public void edit(String room, String user, String messageId, String message) {
  }

  public String read(String room, UserService userService) {
    return null;
  }

  public String read(String room, UserService userService, boolean isTextOnly, Long fromTimestamp) {
    return null;
  }

  public String getSpaceRoom(String space) {
    return null;
  }

  public String getRoom(List<String> users) {
    return null;
  }

  public List<RoomBean> getExistingRooms(String user, boolean withPublic, boolean isAdmin, NotificationService notificationService, TokenService tokenService) {
    return null;
  }

  public RoomsBean getRooms(String user, String filter, boolean withUsers, boolean withSpaces, boolean withPublic, boolean withOffline, boolean isAdmin, NotificationService notificationService, UserService userService, TokenService tokenService) {
    return null;
  }

  public int getNumberOfRooms() {
    return 0;
  }

  public int getNumberOfMessages() {
    return 0;
  }
}
