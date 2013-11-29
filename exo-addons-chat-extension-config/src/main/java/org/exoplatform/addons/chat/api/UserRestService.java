package org.exoplatform.addons.chat.api;


import org.benjp.services.UserService;
import org.benjp.utils.MessageDigester;
import org.benjp.utils.PropertyManager;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
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

  public UserRestService()
  {

  }

  @GET
  @Path("/token/")
  public Response getToken() throws Exception
  {
    ConversationState conversationState = ConversationState.getCurrent();
    String userId = conversationState.getIdentity().getUserId();
    String token;
    if ("__anonim".equals(userId))
    {
      userId = UserService.ANONIM_USER;
      token = "---";
    } else {
      String passphrase = PropertyManager.getProperty(PropertyManager.PROPERTY_PASSPHRASE);
      String in = userId+passphrase;
      token = MessageDigester.getHash(in);
    }

    StringBuilder sb = new StringBuilder();
    sb.append("{\"username\":\"").append(userId).append("\",");
    sb.append("\"token\":\"").append(token).append("\"}");

    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok(sb.toString(), MediaType.APPLICATION_JSON)
            .cacheControl(cacheControl)
            .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
            .build();
  }

}
