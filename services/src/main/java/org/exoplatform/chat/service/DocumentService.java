package org.exoplatform.chat.service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.StringUtils;
import org.gatein.common.text.EntityEncoder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;

import org.exoplatform.addons.chat.listener.ServerBootstrap;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.rest.api.RestUtils;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

import jakarta.servlet.http.HttpServletRequest;

@Path("/chat/api/1.0/file/")
public class DocumentService implements ResourceContainer {

  public static final String     FILE_CREATED_ACTIVITY  = "ActivityNotify.event.FileCreated";

  public static final String     UPLOAD_LIMIT_PARAMETER = "upload.limit";

  public static final int        MB_IN_BYTES            = 1048576;

  private static final Log       LOG                    = ExoLogger.getLogger(DocumentService.class.getName());

  private RepositoryService      repositoryService_;

  private NodeHierarchyCreator   nodeHierarchyCreator_;

  private SessionProviderService sessionProviderService_;

  private SpaceService           spaceService_;

  private ListenerService        listenerService_;

  private UploadService          uploadService_;

  private ActivityCommonService  activityService_;

  private int                    uploadLimit            = 100;

  public DocumentService(RepositoryService repositoryService,
                         SessionProviderService sessionProviderService,
                         NodeHierarchyCreator nodeHierarchyCreator,
                         UploadService uploadService,
                         SpaceService spaceService,
                         ActivityCommonService activityService,
                         ListenerService listenerService,
                         InitParams params) {
    repositoryService_ = repositoryService;
    nodeHierarchyCreator_ = nodeHierarchyCreator;
    sessionProviderService_ = sessionProviderService;
    spaceService_ = spaceService;
    listenerService_ = listenerService;
    uploadService_ = uploadService;
    activityService_ = activityService;
    if (params.containsKey(UPLOAD_LIMIT_PARAMETER)) {
      String uploadLimitString = params.getValueParam(UPLOAD_LIMIT_PARAMETER).getValue();
      if (StringUtils.isNotBlank(uploadLimitString)) {
        try {
          uploadLimit = Integer.parseInt(uploadLimitString);
        } catch (Exception e) {
          LOG.warn("upload.limit parameter should be a number");
        }
      }
    }
  }

  @POST
  @Path("upload")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response uploadFile(@Context SecurityContext securityContext,
                             @Context HttpServletRequest httpServletRequest,
                             @QueryParam("uploadId") String uploadId,
                             @QueryParam("action") String action) throws Exception {
    // The upload process duplicates (partially) the one from Gatein (org.exoplatform.web.handler.UploadHandler) since
    // the upload limit can only be set this way in UploadService
    if(action == null || action.equals("upload")) {
      uploadService_.createUploadResource(httpServletRequest);
      uploadService_.addUploadLimit(uploadId, getUploadLimitInMB());
    } else if(action.equals("progress")) {
      if (uploadId == null) {
        return Response.serverError().build();
      }
      UploadResource upResource = uploadService_.getUploadResource(uploadId);
      if (upResource == null) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return Response.ok(getProgress(upResource)).header("Cache-Control", "no-cache").build();
    }
    return Response.ok().header("Cache-Control", "no-cache").build();
  }

  /**
   * Return JSON representation of the upload progress for the given upload resource.
   * @param upResource The upload resource
   * @return The JSON representation of the upload progress
   */
  private String getProgress(UploadResource upResource) {
    StringBuilder value = new StringBuilder();
    value.append("{\n  upload : {");
    if (upResource.getStatus() == UploadResource.FAILED_STATUS) {
      UploadService.UploadLimit limit = uploadService_.getUploadLimits().get(upResource.getUploadId());
      value.append("\n    \"").append(upResource.getUploadId()).append("\": {");
      value.append("\n      \"status\":").append('\"').append("failed").append("\",");
      value.append("\n      \"size\":").append('\"').append(limit.getLimit()).append("\",");
      value.append("\n      \"unit\":").append('\"').append(limit.getUnit()).append("\"");
      value.append("\n    }");
    } else {
      double percent = 100;
      if (upResource.getStatus() == UploadResource.UPLOADING_STATUS) {
        percent = (upResource.getUploadedSize() * 100) / upResource.getEstimatedSize();
      }
      value.append("\n    \"").append(upResource.getUploadId()).append("\": {");
      value.append("\n      \"percent\":").append('\"').append((int) percent).append("\",");
      String fileName = EntityEncoder.FULL.encode(upResource.getFileName());
      value.append("\n      \"fileName\":").append('\"').append(fileName).append("\"");
      value.append("\n    }");
      value.append("\n  }\n}");
    }
    return value.toString();
  }

  @SuppressWarnings("unchecked")
  @POST
  @Path("persist")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  public Response persistFile(@Context SecurityContext securityContext,
                              @Context HttpServletRequest httpServletRequest,
                              @FormParam("uploadId") String uploadId,
                              @FormParam("targetRoom") String targetRoom,
                              @FormParam("targetFullname") String targetFullname,
                              @FormParam("token") String token) throws Exception {
    String remoteUser = securityContext.getUserPrincipal().getName();
    String room = targetRoom.replace(ChatService.TEAM_PREFIX, "").replace(ChatService.SPACE_PREFIX, "");
    String users = targetRoom.startsWith(ChatService.TEAM_PREFIX) ? ServerBootstrap.getUsers(remoteUser, token, room)
                                                                  : null;
    List<String> usernames =
                           targetRoom.startsWith(ChatService.TEAM_PREFIX) ? getUsernamesFromJSON(users)
                                                                          : targetRoom.startsWith(ChatService.SPACE_PREFIX) ? Collections.emptyList()
                                                                                                                            : Collections.singletonList(targetRoom);

    UploadResource uploadResource = uploadService_.getUploadResource(uploadId);

    if (uploadLimit > 0 && uploadLimit < (uploadResource.getUploadedSize() / MB_IN_BYTES)) {
      return Response.status(Status.NOT_ACCEPTABLE).build();
    }

    Node node = storeFile(uploadResource, remoteUser, targetRoom, targetFullname, usernames);

    String workspace = node.getSession().getWorkspace().getName();
    String repository = ((ManageableRepository) node.getSession().getRepository()).getConfiguration().getName();
    String nodePathWithWorkspace = workspace + node.getPath();
    String nodeName = node.getName();
    String encodedNodeName = URLEncoder.encode(nodeName, "UTF-8").replace("%", "%25");
    nodePathWithWorkspace = nodePathWithWorkspace.replace(nodeName, encodedNodeName);
    String baseDavPath = "/jcr/" + repository + "/" + nodePathWithWorkspace;
    String publicURL = RestUtils.getBaseRestUrl() + baseDavPath;
    String thumbnailURL = "/" + PortalContainer.getCurrentPortalContainerName() + "/" + CommonsUtils.getRestContextName() + "/thumbnailImage/large/" + repository + "/"
        + nodePathWithWorkspace;
    String restPath = "/" + PortalContainer.getCurrentPortalContainerName() + "/" + CommonsUtils.getRestContextName() + baseDavPath;
    String downloadLink = "/" + PortalContainer.getCurrentPortalContainerName() + "/" + CommonsUtils.getRestContextName() + "/contents/download/" + nodePathWithWorkspace;

    String filename = getFileName(uploadResource);

    JSONObject response = new JSONObject();
    response.put("status", "ok");
    response.put("name", node.getName());
    response.put("title", filename);
    response.put("size", uploadResource.getUploadedSize());
    response.put("owner", remoteUser);
    response.put("uuid", node.getUUID());
    response.put("path", node.getPath());
    response.put("createdDate",
                 node.getNode(NodetypeConstant.JCR_CONTENT).getProperty(NodetypeConstant.JCR_LAST_MODIFIED).getString());
    response.put("publicUrl", publicURL);
    response.put("thumbnailUrl", restPath);
    response.put("restPath", restPath);
    response.put("downloadLink", downloadLink);
    response.put("thumbnailURL", thumbnailURL);
    response.put("sizeLabel", calculateFileSize((long) uploadResource.getUploadedSize()));

    return Response.ok(response.toJSONString(), MediaType.APPLICATION_JSON).build();
  }

  public int getUploadLimitInMB() {
    return uploadLimit;
  }

  private Node storeFile(UploadResource uploadResource,
                         String remoteUser,
                         String room,
                         String roomFullName,
                         List<String> usernames) {
    String filename = getFileName(uploadResource);
    String title = filename;
    filename = Text.escapeIllegalJcrChars(Utils.cleanName(Utils.cleanNameWithAccents(filename)));

    boolean isPrivateContext = !room.startsWith(ChatService.SPACE_PREFIX);

    SessionProvider sessionProvider = sessionProviderService_.getSystemSessionProvider(null);

    Node node = null;
    try {
      Node homeNode;
      if (isPrivateContext) {
        Node userNode = nodeHierarchyCreator_.getUserNode(sessionProvider, remoteUser);
        homeNode = userNode.getNode("Private");
      } else {
        ManageableRepository currentRepository = repositoryService_.getCurrentRepository();
        String workspaceName = currentRepository.getConfiguration().getDefaultWorkspaceName();
        Session session = sessionProvider.getSession(workspaceName, currentRepository);

        Space space = spaceService_.getSpaceByDisplayName(roomFullName);
        String groupPath = nodeHierarchyCreator_.getJcrPath(BasePath.CMS_GROUPS_PATH);
        String spaceParentPath = groupPath + space.getGroupId();
        if (!session.itemExists(spaceParentPath)) {
          throw new IllegalStateException("Root node of space '" + spaceParentPath + "' doesn't exist");
        }
        homeNode = (Node) session.getItem(spaceParentPath);
      }

      Node docNode = homeNode.getNode("Documents");

      int suffix = 1;
      while (docNode.hasNode(filename)) {
        filename = filename.contains(".") ? filename.replace(".", "-" + suffix + ".") : filename + "-" + suffix;
        suffix++;
      }

      node = docNode.addNode(filename, NodetypeConstant.NT_FILE);
      node.setProperty(NodetypeConstant.EXO_TITLE, title);
      activityService_.setCreating(node, true);
      Node resourceNode = node.addNode(NodetypeConstant.JCR_CONTENT, NodetypeConstant.NT_RESOURCE);
      resourceNode.setProperty(NodetypeConstant.JCR_MIMETYPE, uploadResource.getMimeType());
      resourceNode.setProperty(NodetypeConstant.JCR_LAST_MODIFIED, Calendar.getInstance());
      String fileDiskLocation = uploadResource.getStoreLocation();
      InputStream inputStream = null;
      try {
        inputStream = new FileInputStream(fileDiskLocation);
        resourceNode.setProperty(NodetypeConstant.JCR_DATA, inputStream);
        docNode.save();

        node = docNode.getSession().getNodeByUUID(node.getUUID());
      } finally {
        if (inputStream != null) {
          inputStream.close();
        }
      }
      if (!usernames.isEmpty()) {
        if (node.canAddMixin("exo:privilegeable")) {
          node.addMixin("exo:privilegeable");
        }
        // Add permission
        Map<String, String[]> permissionsMap = new HashMap<String, String[]>();
        permissionsMap.put(remoteUser, PermissionType.ALL);
        ((NodeImpl) node).setPermissions(permissionsMap);

        for (String user : usernames) {
          ((NodeImpl) node).setPermission(user, new String[] { PermissionType.READ });
        }

        node.save();
      }
      activityService_.setCreating(node, false);

      // Broadcast an activity when uploading file in a space conversation
      if (!isPrivateContext) {
        listenerService_.broadcast(FILE_CREATED_ACTIVITY, null, node);
      }

    } catch (Exception e) {
      LOG.warn("An error occurred while persisting file in JCR", e);
    }
    return node;
  }

  private String getFileName(UploadResource uploadResource) {
    String filename = uploadResource.getFileName();
    try {
      filename = URLDecoder.decode(filename, "UTF-8");
    } catch (UnsupportedEncodingException e1) {
      LOG.warn("An error occurred while decoding file " + filename, e1);
    }
    return filename;
  }

  private String calculateFileSize(long fileLengthLong) {
    int fileLengthDigitCount = Long.toString(fileLengthLong).length();
    double fileSizeKB = 0.0;
    String howBig = "";
    if (fileLengthDigitCount < 4) {
      fileSizeKB = fileLengthLong;
      howBig = "Byte(s)";
    } else if (fileLengthDigitCount >= 4 && fileLengthDigitCount <= 6) {
      fileSizeKB = (new Double(fileLengthLong) / 1024.0);
      howBig = "KB";
    } else if (fileLengthDigitCount >= 7 && fileLengthDigitCount <= 9) {
      fileSizeKB = (new Double(fileLengthLong) / (1024.0 * 1024.0));
      howBig = "MB";
    } else if (fileLengthDigitCount > 9) {
      fileSizeKB = (new Double(fileLengthLong) / (1024.0 * 1024.0 * 1024.0));
      howBig = "GB";
    }
    String finalResult = roundTwoDecimals(fileSizeKB);
    return finalResult + " " + howBig;
  }

  private String roundTwoDecimals(double d) {
    DecimalFormat twoDForm = new DecimalFormat("#.##");
    return twoDForm.format(d);
  }

  private List<String> getUsernamesFromJSON(String users) throws JSONException {
    List<String> usernames = new ArrayList<>();
    if (StringUtils.isNotBlank(users)) {
      org.json.JSONObject usersObject = new org.json.JSONObject(users);
      if (usersObject.has("users")) {
        JSONArray usersArray = ((JSONArray) usersObject.get("users"));
        if (usersArray != null && usersArray.length() > 0) {
          for (int i = 0; i < usersArray.length(); i++) {
            org.json.JSONObject user = usersArray.getJSONObject(i);
            if (user != null && StringUtils.isNotBlank(user.getString("name"))) {
              usernames.add(user.getString("name"));
            }
          }
        }
      }
    }
    return usernames;
  }

}
