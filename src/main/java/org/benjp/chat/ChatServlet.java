package org.benjp.chat;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ChatServlet extends HttpServlet
{

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    String[] params = request.getRequestURI().split("/");
    try
    {
      if (params.length==6)
      {
        String uuid = params[4];

        Node node = getFile(uuid);
        response.setContentType(getMimeType(node));
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        InputStream in = getStream(node);
        OutputStream out = response.getOutputStream();


        byte[] buffer = new byte[1024];
        int len = in.read(buffer);
        while (len != -1) {
          out.write(buffer, 0, len);
          len = in.read(buffer);
        }

      }
    }
    catch (Exception e)
    {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
    return;
  }

  private Node getFile(String uuid)
  {
    RepositoryService repositoryService = (RepositoryService)PortalContainer.getInstance().getComponentInstanceOfType(RepositoryService.class);
    NodeHierarchyCreator nodeHierarchyCreator = (NodeHierarchyCreator)PortalContainer.getInstance().getComponentInstanceOfType(NodeHierarchyCreator.class);

    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try
    {
      //get info
      Session session = sessionProvider.getSession("collaboration", repositoryService.getCurrentRepository());

      Node node = session.getNodeByUUID(uuid);
      return node;
    }
    catch (Exception e)
    {
      System.out.println("JCR::" + e.getMessage());
    }
    finally
    {
      sessionProvider.close();
    }
    return null;
  }

  private InputStream getStream(Node node) throws Exception
  {
    if (node.hasNode("jcr:content")) {
      Node contentNode = node.getNode("jcr:content");
      if (contentNode.hasProperty("jcr:data")) {
        InputStream inputStream = contentNode.getProperty("jcr:data").getStream();
        return inputStream;
      }
    }
    return null;

  }

  private String getMimeType(Node node) throws Exception
  {
    if (node.hasNode("jcr:content")) {
      Node jcrContent = node.getNode("jcr:content");
      return jcrContent.getProperty("jcr:mimeType").getString();
    }
    return null;

  }

}