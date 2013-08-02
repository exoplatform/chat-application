package org.benjp.services.jcr;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;

import javax.jcr.*;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.OnParentVersionAction;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractJCRService
{
  RepositoryService repositoryService_;

  NodeHierarchyCreator nodeHierarchyCreator_;

  SessionProviderService sessionProviderService_;

  static final String TOKEN_NODETYPE = "chat:token";
  static final String USER_PROPERTY = "chat:user";
  static final String TIMESTAMP_PROPERTY = "chat:timestamp";
  static final String TOKEN_PROPERTY = "chat:token";
  static final String VALIDITY_PROPERTY = "chat:validity";
  static final String IS_DEMO_USER_PROPERTY = "chat:isdemouser";

  public AbstractJCRService()
  {
    PortalContainer portalContainer = PortalContainer.getInstance();
    repositoryService_ = (RepositoryService)portalContainer.getComponentInstanceOfType(RepositoryService.class);
    nodeHierarchyCreator_ = (NodeHierarchyCreator)portalContainer.getComponentInstanceOfType(NodeHierarchyCreator.class);
    sessionProviderService_ = (SessionProviderService)portalContainer.getComponentInstanceOfType(SessionProviderService.class);
  }

  public SessionProvider getUserSessionProvider() {
    SessionProvider sessionProvider = sessionProviderService_.getSessionProvider(null);
    return sessionProvider;
  }

  protected void initNodetypes()
  {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try
    {
      //get info
      Session session = sessionProvider.getSession("collaboration", repositoryService_.getCurrentRepository());

      NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
      try
      {
        String uri = namespaceRegistry.getURI("chat");
      }
      catch (NamespaceException ne)
      {
        namespaceRegistry.registerNamespace("chat", "http://www.exoplatform.com/jcr/chat/1.0");
      }

      ExtendedNodeTypeManager nodeTypeManager = (ExtendedNodeTypeManager) session.getWorkspace().getNodeTypeManager();
      try {
        NodeType ntToken = nodeTypeManager.getNodeType(TOKEN_NODETYPE);

      } catch (NoSuchNodeTypeException nsne)
      {
        NodeTypeValue chatToken = new NodeTypeValue();
        chatToken.setName(TOKEN_NODETYPE);
        chatToken.setMixin(false);
        List<String> superTypes = new ArrayList<String>();
        superTypes.add("nt:base");
        chatToken.setDeclaredSupertypeNames(superTypes);

        PropertyDefinitionValue userProperty = new PropertyDefinitionValue();
        userProperty.setMultiple(false);
        userProperty.setAutoCreate(false);
        userProperty.setName(USER_PROPERTY);
        userProperty.setReadOnly(false);
        userProperty.setRequiredType(PropertyType.STRING);
        userProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue validityProperty = new PropertyDefinitionValue();
        validityProperty.setMultiple(false);
        validityProperty.setAutoCreate(false);
        validityProperty.setName(VALIDITY_PROPERTY);
        validityProperty.setReadOnly(false);
        validityProperty.setRequiredType(PropertyType.LONG);
        validityProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue tokenProperty = new PropertyDefinitionValue();
        tokenProperty.setMultiple(false);
        tokenProperty.setAutoCreate(false);
        tokenProperty.setName(TOKEN_PROPERTY);
        tokenProperty.setReadOnly(false);
        tokenProperty.setRequiredType(PropertyType.STRING);
        tokenProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue demoUserProperty = new PropertyDefinitionValue();
        demoUserProperty.setMultiple(false);
        demoUserProperty.setAutoCreate(false);
        demoUserProperty.setName(IS_DEMO_USER_PROPERTY);
        demoUserProperty.setReadOnly(false);
        demoUserProperty.setRequiredType(PropertyType.BOOLEAN);
        demoUserProperty.setOnVersion(OnParentVersionAction.IGNORE);

        List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();
        props.add(userProperty);
        props.add(validityProperty);
        props.add(tokenProperty);
        props.add(demoUserProperty);

        chatToken.setDeclaredPropertyDefinitionValues(props);

        nodeTypeManager.registerNodeType(chatToken, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      sessionProvider.close();
    }


  }

  protected void initMandatoryNodes()
  {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try
    {
      //get info
      Session session = sessionProvider.getSession("collaboration", repositoryService_.getCurrentRepository());

      Node rootNode = session.getRootNode();
      if (!rootNode.hasNode("tokens"))
      {
        rootNode.addNode("tokens", "nt:unstructured");
        session.save();
      }

    }
    catch (Exception e)
    {
      System.out.println("JCR::\n" + e.getMessage());
    }
    finally
    {
      sessionProvider.close();
    }

  }

}
