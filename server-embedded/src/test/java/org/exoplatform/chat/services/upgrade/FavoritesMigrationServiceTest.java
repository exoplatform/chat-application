package org.exoplatform.chat.services.upgrade;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;

import org.exoplatform.chat.AbstractChatTestCase;
import org.exoplatform.chat.bootstrap.ServiceBootstrap;
import org.exoplatform.chat.listener.ConnectionManager;
import org.exoplatform.chat.model.UserBean;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.chat.services.UserService;
import org.exoplatform.chat.services.mongodb.MongoBootstrap;
import org.exoplatform.chat.services.mongodb.SettingMongoDataStorage;
import org.exoplatform.chat.services.mongodb.UserMongoDataStorage;

public class FavoritesMigrationServiceTest extends AbstractChatTestCase {

  private DB db;

  private UserService userService;

  private ChatService chatService;

  private FavoritesMigrationService favoritesMigrationService;

  @Before
  public void setUp() {
    db = ConnectionManager.getInstance().getDB();
    db.getCollection(UserMongoDataStorage.M_USERS_COLLECTION).drop();
    db.getCollection(SettingMongoDataStorage.M_SETTINGS_COLLECTION).drop();

    userService = ServiceBootstrap.getUserService();
    chatService = ServiceBootstrap.getChatService();
    favoritesMigrationService = ServiceBootstrap.getFavoritesMigrationService();
  }

  @Test
  public void testUpgradeWithUsersTeamsAndSpacesRoomsInFavorites() {
    // Given
    userService.addUserFullName("thomas", "Thomas Delhoménie", null);
    userService.addUserFullName("john", "John Smith", null);
    userService.addUserFullName("mary", "Mary Williams", null);
    userService.addUserFullName("james", "James Doe", null);

    String roomThomasJohn = chatService.getRoom(Arrays.asList("thomas", "john"), null);
    String roomThomasMary = chatService.getRoom(Arrays.asList("thomas", "mary"), null);
    String roomThomasJames = chatService.getRoom(Arrays.asList("thomas", "james"), null);
    String roomJohnMary = chatService.getRoom(Arrays.asList("john", "mary"), null);
    String roomTeam1 = chatService.getTeamRoom("Team 1", "thomas", null);
    String roomTeam2 = chatService.getTeamRoom("Team 2", "thomas", null);
    String roomSpace1 = chatService.getSpaceRoom("Space 1", null);
    String roomSpace2 = chatService.getSpaceRoom("Space 2", null);
    String roomSpace3 = chatService.getSpaceRoom("Space 3", null);

    setOldFavoritesToUser("thomas",
                          Arrays.asList("john",
                                        "mary",
                                        ChatService.TEAM_PREFIX + roomTeam1,
                                        ChatService.SPACE_PREFIX + roomSpace2,
                                        ChatService.SPACE_PREFIX + roomSpace3));
    setOldFavoritesToUser("mary", Arrays.asList("john", ChatService.SPACE_PREFIX + roomSpace3));

    // When
    favoritesMigrationService.processMigration();

    // Then
    UserBean thomas = userService.getUser("thomas", true, null);
    assertNotNull(thomas);
    List<String> newThomasFavorites = thomas.getFavorites();
    assertNotNull(newThomasFavorites);
    assertEquals(5, newThomasFavorites.size());
    assertTrue(newThomasFavorites.contains(roomThomasJohn));
    assertTrue(newThomasFavorites.contains(roomThomasMary));
    assertTrue(newThomasFavorites.contains(roomTeam1));
    assertTrue(newThomasFavorites.contains(roomSpace2));
    assertTrue(newThomasFavorites.contains(roomSpace2));
    UserBean john = userService.getUser("john", true, null);
    assertNotNull(john);
    List<String> newJohnFavorites = john.getFavorites();
    assertNull(newJohnFavorites);
    UserBean mary = userService.getUser("mary", true, null);
    assertNotNull(mary);
    List<String> newMaryFavorites = mary.getFavorites();
    assertNotNull(newMaryFavorites);
    assertEquals(2, newMaryFavorites.size());
    assertTrue(newThomasFavorites.contains(roomThomasMary));
    assertTrue(newThomasFavorites.contains(roomSpace3));

    assertEquals(FavoritesMigrationService.FavoritesMigrationStatus.DONE,
                 favoritesMigrationService.getMigrationStatus(db.getName()));
  }

  @Test
  public void testDoNotUpgradeWhenMigrationAlreadyDone() {
    // Given
    ServiceBootstrap.getUserService().addUserFullName("thomas", "Thomas Delhoménie", null);

    String roomTeam1 = ServiceBootstrap.getChatService().getTeamRoom("Team 1", "thomas", null);
    String roomSpace1 = ServiceBootstrap.getChatService().getSpaceRoom("Space 1", null);

    setOldFavoritesToUser("thomas",
                          Arrays.asList(ChatService.TEAM_PREFIX + roomTeam1,
                                        ChatService.SPACE_PREFIX + roomSpace1));

    ServiceBootstrap.getFavoritesMigrationService().setMigrationStatus(FavoritesMigrationService.FavoritesMigrationStatus.DONE, db.getName());

    // When
    ServiceBootstrap.getFavoritesMigrationService().processMigration();

    // Then
    UserBean thomas = ServiceBootstrap.getUserService().getUser("thomas", true, null);
    assertNotNull(thomas);
    List<String> newThomasFavorites = thomas.getFavorites();
    assertNotNull(newThomasFavorites);
    assertEquals(2, newThomasFavorites.size());
    // Nothing has changed, no migration has been done since it is set as already done in the settings
    assertTrue(newThomasFavorites.contains(ChatService.TEAM_PREFIX + roomTeam1));
    assertTrue(newThomasFavorites.contains(ChatService.SPACE_PREFIX + roomSpace1));

    assertEquals(FavoritesMigrationService.FavoritesMigrationStatus.DONE,
            ServiceBootstrap.getFavoritesMigrationService().getMigrationStatus(db.getName()));
  }

  private void setOldFavoritesToUser(String username, List<String> oldFavorites) {
    MongoBootstrap mongoBootstrap = ConnectionManager.getInstance();
    DB db = mongoBootstrap.getDB();
    DBCollection usersCol = db.getCollection(UserMongoDataStorage.M_USERS_COLLECTION);

    BasicDBObject searchQuery = new BasicDBObject("user", username);

    BasicDBObject updateQuery = new BasicDBObject();
    updateQuery.append("$set", new BasicDBObject().append("favorites", oldFavorites));

    usersCol.update(searchQuery, updateQuery);
  }
}
