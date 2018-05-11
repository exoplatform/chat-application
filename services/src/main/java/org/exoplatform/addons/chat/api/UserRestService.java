package org.exoplatform.addons.chat.api;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;

import org.json.simple.JSONObject;

import org.exoplatform.addons.chat.listener.ServerBootstrap;
import org.exoplatform.addons.chat.utils.MessageDigester;
import org.exoplatform.chat.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.user.UserStateService;
import org.exoplatform.ws.frameworks.cometd.ContinuationService;

@Path("/chat/api/1.0/user/")
public class UserRestService implements ResourceContainer {
  public static final String ANONIM_USER = "__anonim_";

  /* The Constant LAST_MODIFIED_PROPERTY */
  protected static final String LAST_MODIFIED_PROPERTY = "Last-Modified";

  /* The Constant IF_MODIFIED_SINCE_DATE_FORMAT */
  protected static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  @GET
  @Path("/token/")
  public Response getToken(@QueryParam("tokenOnly") String tokenOnly) throws Exception {
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
  public Response getCometdToken() throws Exception {
    ConversationState conversationState = ConversationState.getCurrent();
    String userId = conversationState.getIdentity().getUserId();

    Boolean standaloneChatServer = Boolean.valueOf(PropertyManager.getProperty("standaloneChatServer"));
    String token;
    if (standaloneChatServer) {
      String passphrase = PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE);
      String in = userId + passphrase;
      token = MessageDigester.getHash(in);;
    } else {
      ContinuationService continuation = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ContinuationService.class);
      token = continuation.getUserToken(userId);
    }

    return Response.ok(token, MediaType.TEXT_PLAIN).build();
  }

  @GET
  @Path("/onlineStatus/")
  @RolesAllowed("users")
  public Response getOnlineStatus(@QueryParam("users") String users) throws Exception {
    UserStateService userState = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(UserStateService.class);

    if (users != null) {
      String[] split = users.split(",");

      JSONObject data = new JSONObject();
      for (String u : split) {
        data.put(u, userState.isOnline(u));
      }
      return Response.ok(data.toString(), MediaType.APPLICATION_JSON).build();
    } else {
      return Response.serverError().status(400).build();
    }
  }

  @GET
  @Path("/onlineUsers/")
  @RolesAllowed("users")
  public Response getOnlineUsers() throws Exception {
    UserStateService userState = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(UserStateService.class);
    List<String> list = userState.online().stream().map(u -> u.getUserId()).collect(Collectors.toList());
    String users = String.join(",", list);

    return Response.ok(users, MediaType.TEXT_PLAIN).build();
  }

  @GET
  @Path("/settings")
  @RolesAllowed("users")
  public Response getUserSettings(@Context HttpServletRequest request, @Context SecurityContext sc) throws Exception {
    String currentUsername = sc.getUserPrincipal().getName();

    JSONObject userSettings = new JSONObject();
    userSettings.put("username", currentUsername);
    userSettings.put("token", ServerBootstrap.getToken(currentUsername));
    String dbName = ServerBootstrap.getDBName();
    userSettings.put("dbName", dbName);
    userSettings.put("serverURL", ServerBootstrap.getServerURL(request));
    userSettings.put("fullName", ServerBootstrap.getUserFullName(currentUsername, dbName));

    return Response.ok(userSettings, MediaType.APPLICATION_JSON).build();
  }
}
