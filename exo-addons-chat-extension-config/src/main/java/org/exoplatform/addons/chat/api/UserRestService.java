package org.exoplatform.addons.chat.api;


import org.exoplatform.addons.chat.utils.MessageDigester;
import org.exoplatform.addons.chat.utils.PropertyManager;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Path("/chat/api/1.0/user/")
public class UserRestService implements ResourceContainer
{
  /** The Constant LAST_MODIFIED_PROPERTY. */
  protected static final String LAST_MODIFIED_PROPERTY = "Last-Modified";

  /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
  protected static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  public static final String ANONIM_USER = "__anonim_";

  public UserRestService()
  {

  }

  @GET
  @Path("/token/")
  public Response getToken(@QueryParam("tokenOnly") String tokenOnly) throws Exception
  {
    ConversationState conversationState = ConversationState.getCurrent();
    String userId = conversationState.getIdentity().getUserId();
    String token;
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);

    boolean withTokenOnly = (tokenOnly!=null && "true".equals(tokenOnly));
    if ("__anonim".equals(userId))
    {
      userId = ANONIM_USER;
      token = "---";
    } else {
      String passphrase = PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE);
      String in = userId+passphrase;
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

}
