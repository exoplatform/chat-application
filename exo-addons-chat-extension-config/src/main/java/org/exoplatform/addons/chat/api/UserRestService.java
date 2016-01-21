package org.exoplatform.addons.chat.api;


import org.exoplatform.addons.chat.utils.MessageDigester;
import org.exoplatform.addons.chat.utils.PropertyManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.servlet.ServletContext;
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
  public Response getAvatarURL(@PathParam("userId") String userId) {
    return getAvartar(false, userId);
  }

  @GET
  @Path("/getSpaceAvartar/{spaceName}/")
  @RolesAllowed("users")
  public Response getSpaceAvartar(@PathParam("spaceName") String spaceName) {
    return getAvartar(true, spaceName);
  }

  private Response getAvartar(boolean isSpace, String spaceOrUserId) {
    CacheControl cacheControl = new CacheControl();
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);

    // Get avatar
    StringBuilder avartaNodePathBuilder = new StringBuilder();
    avartaNodePathBuilder.append("/production/soc:providers/soc:");
    avartaNodePathBuilder.append(isSpace ? "space" : "organization");
    avartaNodePathBuilder.append("/soc:").append(spaceOrUserId).append("/soc:profile/soc:avatar");
    InputStream avartaInputStream = null;
    try {
      Node avartaNode = (Node) WCMCoreUtils.getService(NodeFinder.class).getItem("social", avartaNodePathBuilder
              .toString(), true);
      avartaInputStream = avartaNode.getNode("jcr:content").getProperty("jcr:data").getStream();
    } catch (Exception e) {
      try {
        String defaultAvartarURL = "img/UserAvtDefault.png";
        if (isSpace) defaultAvartarURL = "img/SpaceChatAvatar.png";
        avartaInputStream = getClass().getClassLoader().getResourceAsStream(defaultAvartarURL);
      } catch (Exception e1) {
        return Response.status(Status.NOT_FOUND).build();
      }
    }

    return Response.ok(avartaInputStream, "Image").cacheControl(cacheControl)
            .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
            .build();
  }
}
