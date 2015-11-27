package org.exoplatform.addons.chat.api;


import org.exoplatform.addons.chat.utils.MessageDigester;
import org.exoplatform.addons.chat.utils.PropertyManager;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    StringBuilder sb = new StringBuilder();
    sb.append("{\"username\":\"").append(userId).append("\",");
    sb.append("\"token\":\"").append(token).append("\"}");

    return Response.ok(sb.toString(), MediaType.APPLICATION_JSON)
            .cacheControl(cacheControl)
            .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
            .build();
  }
  
  @GET
  @Path("/getAvatarURL/{userId}/")
  @RolesAllowed("users")
  public Response getAvatarURL(@PathParam("userId") String userId, @Context UriInfo uri) {
    return getAvartar(false, userId, uri);
  }

  @GET
  @Path("/getSpaceAvartar/{spaceName}/")
  @RolesAllowed("users")
  public Response getSpaceAvartar(@PathParam("spaceName") String spaceName, @Context UriInfo uri) {
    return getAvartar(true, spaceName, uri);
  }

  private Response getAvartar(boolean isSpace, String spaceOrUserId, UriInfo uri) {
    CacheControl cacheControl = new CacheControl();
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);

    // Get server base
    String scheme = uri.getBaseUri().getScheme();
    String serverName = uri.getBaseUri().getHost();
    int serverPort = uri.getBaseUri().getPort();
    String serverBase = scheme + "://" + serverName;
    if (serverPort != 80) serverBase += ":" + serverPort;

    // Get avatar
    InputStream in = null;
    URL url = null;
    String avartarURL = "/rest/jcr/repository/social/production/soc:providers/soc:";
    avartarURL = (isSpace) ? avartarURL.concat("space") : avartarURL.concat("organization");
    avartarURL = avartarURL.concat("/soc:").concat(spaceOrUserId).concat("/soc:profile/soc:avatar");
    try {
      url = new URL(serverBase.concat(avartarURL));
      URLConnection con = url.openConnection();
      con.setDoOutput(true);
      in = con.getInputStream();
    } catch (Exception e) {
      try {
        String defaultAvartarURL = "/eXoSkin/skin/images/themes/default/social/skin/ShareImages/UserAvtDefault.png";
        if (isSpace) defaultAvartarURL = "/chat/img/SpaceChatAvatar.png";
        url = new URL(serverBase + defaultAvartarURL);
        URLConnection con = url.openConnection();
        con.setDoOutput(true);
        in = con.getInputStream();
      } catch (Exception e1) {
        return Response.status(Status.NOT_FOUND).build();
      }
    }

    return Response.ok(in, "Image").cacheControl(cacheControl)
            .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
            .build();
  }
}
