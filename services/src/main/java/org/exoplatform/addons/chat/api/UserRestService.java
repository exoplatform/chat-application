package org.exoplatform.addons.chat.api;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.addons.chat.model.MentionModel;
import org.exoplatform.addons.chat.model.MessageReceivedModel;
import org.exoplatform.chat.model.UserBean;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.json.simple.JSONObject;

import org.exoplatform.addons.chat.listener.ServerBootstrap;
import org.exoplatform.addons.chat.utils.MessageDigester;
import org.exoplatform.chat.service.*;
import org.exoplatform.chat.utils.PropertyManager;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.user.UserStateService;
import org.exoplatform.ws.frameworks.cometd.ContinuationService;

import static org.exoplatform.addons.chat.utils.NotificationUtils.*;

@Path("/chat/api/1.0/user/")
public class UserRestService implements ResourceContainer {
  private static final String   CHAT_USER_INITIALIZATION_ATTR = "exo.chat.user.initialized";

  public static final String    ANONIM_USER                   = "__anonim_";

  /* The Constant LAST_MODIFIED_PROPERTY */
  protected static final String LAST_MODIFIED_PROPERTY        = "Last-Modified";

  /* The Constant IF_MODIFIED_SINCE_DATE_FORMAT */
  protected static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  protected static final String EXTERNAL_PROPERTY             = "external";

  private DocumentService       documentService;

  private OrganizationService   organizationService;

  private RelationshipManager   relationshipManager;

  private IdentityManager   identityManager;

  private ContinuationService   continuationService;

  private UserStateService      userStateService;

  public UserRestService(UserStateService userStateService,
                         RelationshipManager relationshipManager,
                         IdentityManager identityManager,
                         ContinuationService continuationService,
                         OrganizationService organizationService) {
    this.organizationService = organizationService;
    this.identityManager = identityManager;
    this.relationshipManager = relationshipManager;
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

    String token = getCometdToken();

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

  @POST
  @Path("/mentionNotifications")
  @RolesAllowed("users")
  @SuppressWarnings("unchecked")
  public Response sendNotificationToMentionUsers(@Context HttpServletRequest request, @RequestBody(description = "MentionModel", required = true) MentionModel mentionModel  ) throws Exception {
    init(request);

    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(MENTION_MODEL, mentionModel);
    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(CHAT_MENTION_NOTIFICATION_PLUGIN))).execute(ctx);
    JSONObject mentionedUsers = new JSONObject();
    mentionedUsers.put("mentionedUsers", String.join(",", mentionModel.getMentionedUsers()));
    return Response.ok(mentionedUsers, MediaType.APPLICATION_JSON).build();
  }

  @POST
  @Path("/messageReceivedNotification")
  @RolesAllowed("users")
  @SuppressWarnings("unchecked")
  public Response sendNotificationToUsers(@Context HttpServletRequest request, @RequestBody(description = "MessageReceivedModel", required = true) MessageReceivedModel messageReceivedModel  ) throws Exception {
    init(request);

    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(MESSAGE_RECEIVED_MODEL, messageReceivedModel);
    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(CHAT_MESSAGE_RECEIVED_NOTIFICATION_PLUGIN))).execute(ctx);
    
    return Response.ok(messageReceivedModel.toString(), MediaType.TEXT_PLAIN).build();
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

  @POST
  @Path("getRoomParticipantsToSuggest")
  @RolesAllowed("users")
  @Operation(
          summary = "Get room participants to suggest",
          method = "POST",
          description = "This returns the list of room participants (external and internal)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "404", description = "Resource not found")})
  public Response getRoomParticipantsToSuggest(@Context UriInfo uriInfo,
                                               @Context HttpServletRequest request,
                                               @Parameter(description = "List of users.") List<UserBean> userList) throws Exception {
    List<UserBean> roomParticipantsToSuggest = new ArrayList<>();
    for (UserBean userBean : userList) {
      Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userBean.getName());
      if (userIdentity.getProfile() != null && userIdentity.getProfile().getProperty(EXTERNAL_PROPERTY) != null && userIdentity.getProfile().getProperty(EXTERNAL_PROPERTY).equals("true")) {
        userBean.setFullname(userBean.getFullname() + " " + "(" + getResourceBundleLabel(request.getLocale(), "exoplatform.chat.external") + ")");
        roomParticipantsToSuggest.add(userBean);
      } else if (userIdentity.getProfile() != null && (userIdentity.getProfile().getProperty(EXTERNAL_PROPERTY) == null || userIdentity.getProfile().getProperty(EXTERNAL_PROPERTY) != null && userIdentity.getProfile().getProperty(EXTERNAL_PROPERTY).equals("false"))) {
        roomParticipantsToSuggest.add(userBean);
      }
    }
    return Response.ok(roomParticipantsToSuggest, MediaType.APPLICATION_JSON).build();
  }

  @POST
  @Path("getModalParticipantsToSuggest")
  @RolesAllowed("users")
  @Operation(
          summary = "Get room participants to suggest in room modal",
          method = "POST",
          description = "This returns the list of room participants as non externals or current user connections")
  @ApiResponses(value = {
          @ApiResponse (responseCode = "200", description = "Request fulfilled"),
          @ApiResponse (responseCode = "404", description = "Resource not found")})
  public Response getModalParticipantsToSuggest(@Context UriInfo uriInfo,
                                               @Context HttpServletRequest request,
                                               @Parameter(description = "List of users.") List<UserBean> userList) throws Exception {
    String authenticatedUser;
    try {
      authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    } catch (Exception e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    Identity authenticatedUserIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, authenticatedUser);
    List<Identity> currentUserConnections = Arrays.asList(relationshipManager.getConnections(authenticatedUserIdentity).load(0, 0));

    List<UserBean> roomParticipantsToSuggest = new ArrayList<>();
    for (UserBean userBean : userList) {
      Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userBean.getName());
      if (currentUserConnections.contains(userIdentity) && userIdentity.getProfile() != null && (userIdentity.getProfile().getProperty(EXTERNAL_PROPERTY) == null || (userIdentity.getProfile().getProperty(EXTERNAL_PROPERTY) != null && userIdentity.getProfile().getProperty(EXTERNAL_PROPERTY).equals("false")))) {
        roomParticipantsToSuggest.add(userBean);
      }
    }
    return Response.ok(roomParticipantsToSuggest, MediaType.APPLICATION_JSON).build();
  }

  @GET
  @Path("/getUserState/")
  @RolesAllowed("users")
  public Response getUserState(@Context HttpServletRequest request, @QueryParam("user") String user) throws Exception {
    init(request);

    Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, user);
    JSONObject userStatus = new JSONObject();
    userStatus.put("isDeleted", userIdentity == null || userIdentity.isDeleted());
    userStatus.put("isEnabled", userIdentity != null && userIdentity.isEnable());
    if(userIdentity != null ) {
      userStatus.put("isExternal", userIdentity.getProfile() != null && userIdentity.getProfile().getProperty("external").equals("true") ? "true" : "false");
    }
    return Response.ok(userStatus, MediaType.APPLICATION_JSON).build();
  }

  @SuppressWarnings("unchecked")
  @GET
  @Path("/settings")
  @RolesAllowed("users")
  public Response getUserSettings(@Context HttpServletRequest request, @Context SecurityContext sc) throws Exception {
    init(request);

    String currentUsername = sc.getUserPrincipal().getName();

    String token = ServerBootstrap.getToken(currentUsername);
    String userFullName = ServerBootstrap.getUserFullName(currentUsername);
    Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, currentUsername);
    boolean isExternal = userIdentity.getProfile() != null && userIdentity.getProfile().getProperty("external") != null && userIdentity.getProfile().getProperty("external").equals("true");

    Boolean isUserInitialized = (Boolean) request.getSession().getAttribute(CHAT_USER_INITIALIZATION_ATTR);
    if (isUserInitialized == null || !isUserInitialized) {
      // Add User in the DB
      ServerBootstrap.addUser(currentUsername, token);

      if (StringUtils.isBlank(userFullName)) {
        // Set user's Full Name in the DB
        User user = organizationService.getUserHandler().findUserByName(currentUsername);
        if (user != null) {
          userFullName = user.getDisplayName();
          ServerBootstrap.addUserFullNameAndEmail(currentUsername, userFullName, user.getEmail());
        }
      }
      request.getSession().setAttribute(CHAT_USER_INITIALIZATION_ATTR, true);
    }
    if(isExternal){
      userFullName += " " + "(" + getResourceBundleLabel(request.getLocale(), "exoplatform.chat.external") + ")";
    }
    boolean online = userStateService.isOnline(currentUsername);

    String cometdToken = getCometdToken();

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
    String userStatus = ServerBootstrap.getStatus(currentUsername, token, currentUsername);

    JSONObject userSettings = new JSONObject();
    userSettings.put("username", currentUsername);
    userSettings.put("token", token);
    userSettings.put("fullName", userFullName);
    userSettings.put("status", userStatus);
    userSettings.put("isOnline", online);
    userSettings.put("cometdToken", cometdToken);
    userSettings.put("sessionId", request.getSession().getId());
    userSettings.put("serverURL", ServerBootstrap.getServerURL());
    userSettings.put("standalone", isStandalone);
    userSettings.put("chatPage", chatPage);
    userSettings.put("offlineDelay", userStateService.getDelay());
    userSettings.put("wsEndpoint", chatCometDServerUrl);

    int uploadLimitInMB = 0;
    boolean canUploadFiles = getDocumentService() != null;
    if (canUploadFiles) {
      uploadLimitInMB = getDocumentService().getUploadLimitInMB();
      userSettings.put("maxUploadSize", uploadLimitInMB);
    }
    userSettings.put("canUploadFiles", canUploadFiles);

    return Response.ok(userSettings, MediaType.APPLICATION_JSON).build();
  }

  private void init(HttpServletRequest request) {
    ServerBootstrap.init(request);
  }

  private String getCometdToken() {
    ConversationState conversationState = ConversationState.getCurrent();
    String userId = conversationState.getIdentity().getUserId();

    Boolean standaloneChatServer = Boolean.valueOf(PropertyManager.getProperty("standaloneChatServer"));
    String token;
    if (standaloneChatServer) {
      String passphrase = PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE);
      String in = userId + passphrase;
      token = MessageDigester.getHash(in);
    } else {
      token = continuationService.getUserToken(userId);
    }
    return token;
  }

  public DocumentService getDocumentService() {
    if (documentService == null) {
      documentService = CommonsUtils.getService(DocumentService.class);
    }
    return documentService;
  }

  private String getResourceBundleLabel(Locale locale, String label) {
    ResourceBundleService resourceBundleService =  CommonsUtils.getService(ResourceBundleService.class);
    return resourceBundleService.getResourceBundle(resourceBundleService.getSharedResourceBundleNames(), locale).getString(label);
  }
}
