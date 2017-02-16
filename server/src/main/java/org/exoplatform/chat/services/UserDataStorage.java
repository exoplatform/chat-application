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
  public static final String STATUS_SPACE = "space";
  public static final String STATUS_TEAM = "team";

  public static final String ANONIM_USER = "__anonim_";
  public static final String SUPPORT_USER = "__support_";

  public static final String PREFERRED_ROOM_NOTIFICATION_TRIGGER = "preferredRoomNotificationTrigger";
  public static final String PREFERRED_NOTIFICATION = "preferredNotification";
  public static final String PREFERRED_NOTIFICATION_TRIGGER = "preferredNotificationTrigger";
  public static final String ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD = "keywords";

  void addFavorite(String user, String targetUser, String dbName);

  void removeFavorite(String user, String targetUser, String dbName);

  /*
    * This methode is responsible for setting a notification channel for a specific user
    * available channels :
    *  -on-site
    *  -desktop
    *  -bip
    */
  void setPreferredNotification(String user, String notifManner, String dbName) throws Exception;

  /*
    * This methode is responsible for setting a notification triggers for a specific user
    * available triggers :
    *  -mention
    *  -even-on-do-not-distrub
    *
    */
  void setNotificationTrigger(String user, String notifCond, String dbName) throws Exception;

  /*
    * This methode is responsible for setting a notification triggers for a specific user in a specific room
    * available triggers :
    *  -mention
    *  -key-words
    *
    */
  void setRoomNotificationTrigger(String user, String room, String notifCond, String notifConditionType, String dbName, long time) throws Exception;

  /*
    * This methode is responsible for getting all desktop settings in a single object
    */
  NotificationSettingsBean getUserDesktopNotificationSettings(String user, String dbName) throws JSONException;

  boolean isFavorite(String user, String targetUser, String dbName);

  void addUserFullName(String user, String fullname, String dbName);

  void addUserEmail(String user, String email, String dbName);

  void setSpaces(String user, List<SpaceBean> spaces, String dbName);

  void addTeamRoom(String user, String teamRoomId, String dbName);

  void addTeamUsers(String teamRoomId, List<String> users, String dbName);

  void removeTeamUsers(String teamRoomId, List<String> users, String dbName);

  List<RoomBean> getTeams(String user, String dbName);

  RoomBean getRoom(String user, String roomId, String dbName);

  List<SpaceBean> getSpaces(String user, String dbName);

  List<UserBean> getUsers(String roomId, String dbName);

  List<UserBean> getUsersInRoomChatOneToOne(String roomId, String dbName);

  List<UserBean> getUsers(String filter, boolean fullBean, String dbName);

  String setStatus(String user, String status, String dbName);

  void setAsAdmin(String user, boolean isAdmin, String dbName);

  boolean isAdmin(String user, String dbName);

  String getStatus(String user, String dbName);

  String getUserFullName(String user, String dbName);

  UserBean getUser(String user, String dbName);

  UserBean getUser(String user, boolean withFavorites, String dbName);

  List<String> getUsersFilterBy(String user, String room, String type, String dbName);

  int getNumberOfUsers(String dbName);
}
