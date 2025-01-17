package org.exoplatform.addons.chat.upgrade;

import org.exoplatform.addons.chat.listener.ServerBootstrap;
import org.exoplatform.commons.upgrade.UpgradePluginExecutionContext;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.SpaceFilter;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.ws.frameworks.cometd.ContinuationService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

public class ChatSpacesAdministratorsUpgradePlugin extends UpgradeProductPlugin {

  private static final Log log = ExoLogger.getExoLogger(ChatSpacesAdministratorsUpgradePlugin.class);

  private SpaceService        spaceService;
  private   ContinuationService continuationService;
  private String superUser;
  private String superUserChatToken;
  private int                 LIMIT = 100;

  public ChatSpacesAdministratorsUpgradePlugin(InitParams initParams, SpaceService spaceService,UserACL userACL, ContinuationService continuationService) {
    super(initParams);
    this.spaceService=spaceService;
    this.continuationService=continuationService;

    this.superUser = userACL.getSuperUser();
    this.superUserChatToken = ServerBootstrap.getToken(this.superUser);
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    long startupTime = System.currentTimeMillis();
    log.info("Start Remove Space administrator from all space chat room");

    boolean chatServerStarted = false;
    int nbTest = 0;
    while (!chatServerStarted && nbTest<100) {
      try {
        Thread.sleep(1000);
      } catch (Exception e2) {
        //do nothing
      }
      nbTest++;
      log.debug("Test if chatServer is running ({} times)", nbTest);
      ServerBootstrap.addUser(this.superUser, this.superUserChatToken);
      String result = ServerBootstrap.getStatus(this.superUser, this.superUserChatToken, this.superUser);
      if (result!=null) {
        chatServerStarted=true;
      }
    }

    if (!chatServerStarted) {
      throw new RuntimeException("Unable to join chatServer");
    }

    try {
      ListAccess<Space> allSpaces = spaceService.getAllSpacesByFilter(new SpaceFilter());
      int current = 0;

      do {
          Space[] spaces = allSpaces.load(current, LIMIT);
          Arrays.stream(spaces).forEach(space -> checkSpace(space));
          current+=spaces.length;
          log.info("Run upgrade on {}/{} spaces",current,allSpaces.getSize());
      } while (current<allSpaces.getSize());
    } catch (Exception e) {
      log.error("Unable to treat all spaces",e);
      throw new RuntimeException(e);
    }
    log.info("End Remove Space administrator from all space chat room, execution time={}ms",
             System.currentTimeMillis() - startupTime);

  }

  private void checkSpace(Space space) {
    String spaceString = ServerBootstrap.getSpaceRoom(this.superUser, this.superUserChatToken, space.getId());
    log.debug("Check space {}, result={}",space.getPrettyName(), spaceString);
    if (spaceString != null) {
      JSONObject jsonSpace = new JSONObject(spaceString);
      if (jsonSpace.has("user")) {
        String spaceRoomId = jsonSpace.getString("user");
        String userCountInSpace = ServerBootstrap.getUserCount(this.superUser,this.superUserChatToken,spaceRoomId);
        if (userCountInSpace != null) {
          JSONObject jsonCountUsers = new JSONObject(userCountInSpace);
          if (jsonCountUsers.has("usersCount")) {
            int spaceUserCount = jsonCountUsers.getInt("usersCount");
            log.debug("Space Room with name {} and id {} has {} users", spaceRoomId, space.getPrettyName(), spaceUserCount);
            String usersInSpaceRoom = ServerBootstrap.getUsersWithLimit(this.superUser,this.superUserChatToken, spaceRoomId,spaceUserCount);
            JSONObject users = new JSONObject(usersInSpaceRoom);
            if (users.has("users")) {
              checkUsers(users.getJSONArray("users"),space);
              if (spaceUserCount != users.getJSONArray("users").length()) {
                checkUser(space,this.superUser);
              }
            }
          }

        }
      }

    }
  }

  private void checkUsers(JSONArray users, Space space) {
    for (int i=0; i<users.length();i++) {
      JSONObject user = users.getJSONObject(i);
      String username = user.getString("name");
      checkUser(space, username);
    }

  }

  private void checkUser(Space space, String username) {
    if (!spaceService.isMember(space, username)) {
      log.debug("User {} is member of space room, but not member of space {}, remove it from the space room", username, space.getPrettyName());
      ServerBootstrap.removeUserFromSpace(username,space.getId());
    }
  }

  public boolean shouldProceedToUpgrade(String newVersion, String previousGroupVersion, UpgradePluginExecutionContext previousUpgradePluginExecution) {
    int executionCount = previousUpgradePluginExecution == null ? 0 : previousUpgradePluginExecution.getExecutionCount();
    return !isExecuteOnlyOnce() || executionCount == 0;
  }

}
