package org.exoplatform.chat.service;

import java.io.*;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.json.simple.JSONObject;

import org.exoplatform.addons.chat.listener.ServerBootstrap;
import org.exoplatform.chat.services.ChatService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.rest.api.RestUtils;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

@Path("/chat/api/1.0/file/")
public class DocumentsData implements ResourceContainer {

  private static final Logger    LOG                   = Logger.getLogger(DocumentsData.class.getName());

  private RepositoryService      repositoryService_;

  private NodeHierarchyCreator   nodeHierarchyCreator_;

  private SessionProviderService sessionProviderService_;

  private SpaceService           spaceService_;

  private ListenerService        listenerService_;

  private UploadService          uploadService_;

  private ActivityCommonService  activityService_;

  public static String           FILE_CREATED_ACTIVITY = "ActivityNotify.event.FileCreated";

  public DocumentsData(RepositoryService repositoryService,
                       SessionProviderService sessionProviderService,
                       NodeHierarchyCreator nodeHierarchyCreator,
                       UploadService uploadService,
                       SpaceService spaceService,
                       ActivityCommonService activityService,
                       ListenerService listenerService) {
    repositoryService_ = repositoryService;
    nodeHierarchyCreator_ = nodeHierarchyCreator;
    sessionProviderService_ = sessionProviderService;
    spaceService_ = spaceService;
    listenerService_ = listenerService;
    uploadService_ = uploadService;
    activityService_ = activityService;
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
                              @FormParam("token") String token, 
                              @FormParam("dbName") String dbName) throws Exception {
    String remoteUser = securityContext.getUserPrincipal().getName();
    String room = targetRoom.replace(ChatService.TEAM_PREFIX, "").replace(ChatService.SPACE_PREFIX, "");
    String users = targetRoom.startsWith(ChatService.TEAM_PREFIX) ? ServerBootstrap.getUsers(remoteUser, token, room, dbName) : null;
    UploadResource uploadResource = uploadService_.getUploadResource(uploadId);
    Node node = storeFile(uploadResource, remoteUser, targetRoom, targetFullname, users);

    String workspace = node.getSession().getWorkspace().getName();
    String repository = ((ManageableRepository) node.getSession().getRepository()).getConfiguration().getName();
    String basePath = "/jcr/" + repository + "/" + workspace + node.getPath();
    String publicURL = RestUtils.getBaseRestUrl() + basePath;
    String restPath = "/" + CommonsUtils.getRestContextName() + basePath;
    String downloadLink = "/" + CommonsUtils.getRestContextName() + basePath;

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
    response.put("restPath", restPath);
    response.put("downloadLink", downloadLink);
    response.put("sizeLabel", calculateFileSize((long) uploadResource.getUploadedSize()));

    return Response.ok(response.toJSONString(), MediaType.APPLICATION_JSON).build();
  }

  protected Node storeFile(UploadResource uploadResource, String remoteUser, String room, String roomFullName, String users) {
    String filename = getFileName(uploadResource);
    String title = filename;
    filename = Text.escapeIllegalJcrChars(filename);

    boolean isPrivateContext = !room.startsWith(ChatService.SPACE_PREFIX);

    SessionProvider sessionProvider = sessionProviderService_.getSessionProvider(null);

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
      if (StringUtils.isNoneBlank(users)) {
        String[] usernames = users.split(",");
        if (node.canAddMixin("exo:privilegeable")) {
          node.addMixin("exo:privilegeable");
        }
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
      LOG.warning("JCR::" + e.getMessage());
    }
    return node;
  }

  private String getFileName(UploadResource uploadResource) {
    String filename = uploadResource.getFileName();
    try {
      filename = URLDecoder.decode(filename, "UTF-8");
    } catch (UnsupportedEncodingException e1) {
      LOG.warning("can't decode " + filename);
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
}
