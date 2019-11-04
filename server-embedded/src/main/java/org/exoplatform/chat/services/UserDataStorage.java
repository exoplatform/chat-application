package org.exoplatform.chat.services;

import org.exoplatform.chat.model.NotificationSettingsBean;
import org.exoplatform.chat.model.RoomBean;
import org.exoplatform.chat.model.SpaceBean;
import org.exoplatform.chat.model.UserBean;
import org.json.JSONException;

import java.util.List;

/**
 * User data storage
 */
public interface UserDataStorage {

  public static final String STATUS_AVAILABLE = "available";
  public static final String STATUS_DONOTDISTURB = "donotdisturb";
  public static final String STATUS_AWAY = "away";
  public static final String STATUS_INVISIBLE = "invisible";
  public static final String STATUS_OFFLINE = "offline";
  public static final String STATUS_NONE = "none";

  public static final String PREFERRED_ROOM_NOTIFICATION_TRIGGER = "preferredRoomNotificationTrigger";
  public static final String PREFERRED_NOTIFICATION = "preferredNotification";
  public static final String PREFERRED_NOTIFICATION_TRIGGER = "preferredNotificationTrigger";
  public static final String ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD = "keywords";
  public static final String NOTIFICATIONS_SETTINGS = "notificationsSettings";

  void addFavorite(String user, String targetUser);

  void removeFavorite(String user, String targetUser);

  /*
    * This methode is responsible for setting a notification channel for a specific user
    * available channels :
    *  -on-site
    *  -desktop
    *  -bip
    */
  void setPreferredNotification(String user, String notifManner) throws Exception;

  /*
    * This methode is responsible for setting a notification triggers for a specific user
    * available triggers :
    *  -mention
    *  -even-on-do-not-disturb
    *
    */
  void setNotificationTrigger(String user, String notifCond) throws Exception;

  /*
    * This methode is responsible for setting a notification triggers for a specific user in a specific room
    * available triggers :
    *  -mention
    *  -key-words
    *
    */
  void setRoomNotificationTrigger(String user, String room, String notifCond, String notifConditionType, long time) throws Exception;

  /*
    * This methode is responsible for getting all desktop settings in a single object
    */
  NotificationSettingsBean getUserDesktopNotificationSettings(String user) throws JSONException;

  boolean isFavorite(String user, String targetUser);

  void addUserFullName(String user, String fullname);

  void addUserEmail(String user, String email);

  void setSpaces(String user, List<SpaceBean> spaces);

  void addTeamRoom(String user, String teamRoomId);

  void removeTeamUsers(String teamRoomId, List<String> users);

  List<RoomBean> getTeams(String user);

  RoomBean getRoom(String user, String roomId);

  List<SpaceBean> getSpaces(String user);

  List<UserBean> getUsers(String roomId, String filter, int limit);

  List<UserBean> getUsersInRoomChatOneToOne(String roomId);

  String setStatus(String user, String status);

  void setAsAdmin(String user, boolean isAdmin);

  boolean isAdmin(String user);

  String getStatus(String user);

  String getUserFullName(String user);

  UserBean getUser(String user);

  UserBean getUser(String user, boolean withFavorites);

  List<String> getUsersFilterBy(String user, String room, String type);

  int getNumberOfUsers();
}
