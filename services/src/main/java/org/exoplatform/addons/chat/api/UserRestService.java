package org.exoplatform.addons.chat.api;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;

import org.exoplatform.addons.chat.listener.ServerBootstrap;
import org.exoplatform.addons.chat.utils.MessageDigester;
import org.exoplatform.chat.service.DocumentService;
import org.exoplatform.chat.utils.PropertyManager;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.user.UserStateService;
import org.exoplatform.ws.frameworks.cometd.ContinuationService;

@Path("/chat/api/1.0/user/")
public class UserRestService implements ResourceContainer {
  private static final String   CHAT_USER_INITIALIZATION_ATTR = "exo.chat.user.initialized";

  public static final String    ANONIM_USER                   = "__anonim_";

  /* The Constant LAST_MODIFIED_PROPERTY */
  protected static final String LAST_MODIFIED_PROPERTY        = "Last-Modified";

  /* The Constant IF_MODIFIED_SINCE_DATE_FORMAT */
  protected static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  private DocumentService       documentService;

  private OrganizationService   organizationService;

  private ContinuationService   continuationService;

  private UserStateService      userStateService;

  public UserRestService(UserStateService userStateService,
                         ContinuationService continuationService,
                         OrganizationService organizationService,
                         DocumentService documentService) {
    this.documentService = documentService;
    this.organizationService = organizationService;
    this.continuationService = continuationService;
    this.userStateService = userStateService;
  }

  @SuppressWarnings("unchecked")
  @GET
  @Path("/token/")
  public Response getToken(@Context HttpServletRequest request, @QueryParam("tokenOnly") String tokenOnly) throws Exception {
    init(request);

    ConversationState conversationState = ConversationState.getCurrent();
    String userId = conversationState.getIdentity().getUserId();
    String token;
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);

    boolean withTokenOnly = (tokenOnly != null && "true".equals(tokenOnly));
    if ("__anonim".equals(userId)) {
      userId = ANONIM_USER;
      token = "---";
    } else {
      String passphrase = PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE);
      String in = userId + passphrase;
      token = MessageDigester.getHash(in);
    }

    if (withTokenOnly) {
      return Response.ok(token, MediaType.TEXT_PLAIN)
                     .cacheControl(cacheControl)
                     .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                     .build();
    }

    JSONObject data = new JSONObject();
    data.put("username", userId);
    data.put("token", token);

    return Response.ok(data.toString(), MediaType.APPLICATION_JSON)
                   .cacheControl(cacheControl)
                   .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                   .build();
  }

  @GET
  @Path("/cometdToken/")
  @RolesAllowed("users")
  public Response getCometdToken(@Context HttpServletRequest request) throws Exception {
    init(request);

    ConversationState conversationState = ConversationState.getCurrent();
    String userId = conversationState.getIdentity().getUserId();

    Boolean standaloneChatServer = Boolean.valueOf(PropertyManager.getProperty("standaloneChatServer"));
    String token;
    if (standaloneChatServer) {
      String passphrase = PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE);
      String in = userId + passphrase;
      token = MessageDigester.getHash(in);
      ;
    } else {
      token = continuationService.getUserToken(userId);
    }

    return Response.ok(token, MediaType.TEXT_PLAIN).build();
  }

  @GET
  @Path("/onlineStatus/")
  @RolesAllowed("users")
  @SuppressWarnings("unchecked")
  public Response getOnlineStatus(@Context HttpServletRequest request, @QueryParam("users") String users) throws Exception {
    init(request);

    if (users != null) {
      String[] split = users.split(",");

      JSONObject data = new JSONObject();
      for (String u : split) {
        data.put(u, userStateService.isOnline(u));
      }
      return Response.ok(data.toString(), MediaType.APPLICATION_JSON).build();
    } else {
      return Response.serverError().status(400).build();
    }
  }

  @GET
  @Path("/onlineUsers/")
  @RolesAllowed("users")
  public Response getOnlineUsers(@Context HttpServletRequest request) throws Exception {
    init(request);

    List<String> list = userStateService.online().stream().map(u -> u.getUserId()).collect(Collectors.toList());
    String users = String.join(",", list);

    return Response.ok(users, MediaType.TEXT_PLAIN).build();
  }

  @SuppressWarnings("unchecked")
  @GET
  @Path("/settings")
  @RolesAllowed("users")
  public Response getUserSettings(@Context HttpServletRequest request, @Context SecurityContext sc) throws Exception {
    init(request);

    String currentUsername = sc.getUserPrincipal().getName();

    String token = ServerBootstrap.getToken(currentUsername);
    String dbName = ServerBootstrap.getDBName();
    String userFullName = ServerBootstrap.getUserFullName(currentUsername, dbName);

    Boolean isUserInitialized = (Boolean) request.getSession().getAttribute(CHAT_USER_INITIALIZATION_ATTR);
    if (isUserInitialized == null || !isUserInitialized) {
      // Add User in the DB
      ServerBootstrap.addUser(currentUsername, token, dbName);

      if (StringUtils.isBlank(userFullName)) {
        // Set user's Full Name in the DB
        User user = organizationService.getUserHandler().findUserByName(currentUsername);
        if (user != null) {
          userFullName = user.getDisplayName();
          ServerBootstrap.addUserFullNameAndEmail(currentUsername, userFullName, user.getEmail(), dbName);
        }
      }
      // Set user's Spaces in the DB
      ServerBootstrap.saveSpaces(currentUsername, dbName);
      request.getSession().setAttribute(CHAT_USER_INITIALIZATION_ATTR, true);
    }
    boolean online = userStateService.isOnline(currentUsername);

    String cometdToken = continuationService.getUserToken(currentUsername);

    String isStandaloneString = PropertyManager.getProperty("standaloneChatServer");
    boolean isStandalone = isStandaloneString != null && Boolean.valueOf(isStandaloneString);
    String chatServerURI = ServerBootstrap.getServerURI();
    String chatPage = PropertyManager.getProperty(PropertyManager.PROPERTY_CHAT_PORTAL_PAGE);
    String chatCometDServerUrl = null;
    if (isStandalone) {
      chatCometDServerUrl = chatServerURI + "/cometd";
    } else {
      chatCometDServerUrl = "/cometd/cometd";
    }
    String userStatus = ServerBootstrap.getStatus(currentUsername, token, currentUsername, dbName);

    JSONObject userSettings = new JSONObject();
    userSettings.put("username", currentUsername);
    userSettings.put("token", token);
    userSettings.put("fullName", userFullName);
    userSettings.put("status", userStatus);
    userSettings.put("isOnline", online);
    userSettings.put("cometdToken", cometdToken);
    userSettings.put("dbName", dbName);
    userSettings.put("sessionId", request.getSession().getId());
    userSettings.put("serverURL", ServerBootstrap.getServerURL());
    userSettings.put("standalone", isStandalone);
    userSettings.put("chatPage", chatPage);
    userSettings.put("offlineDelay", userStateService.getDelay());
    userSettings.put("wsEndpoint", chatCometDServerUrl);
    userSettings.put("maxUploadSize", documentService.getUploadLimitInMB());

    return Response.ok(userSettings, MediaType.APPLICATION_JSON).build();
  }

  private void init(HttpServletRequest request) {
    ServerBootstrap.init(request);
  }
}
