package org.benjp.services.jcr;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;

import javax.jcr.*;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.OnParentVersionAction;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractJCRService
{

  static final String TOKEN_NODETYPE = "chat:token";
  static final String NOTIF_NODETYPE = "chat:notification";
  static final String USER_PROPERTY = "chat:user";
  static final String TIMESTAMP_PROPERTY = "chat:timestamp";
  static final String TOKEN_PROPERTY = "chat:token";
  static final String VALIDITY_PROPERTY = "chat:validity";
  static final String IS_DEMO_USER_PROPERTY = "chat:isdemouser";
  static final String TYPE_PROPERTY = "chat:type";
  static final String CATEGORY_PROPERTY = "chat:category";
  static final String CATEGORY_ID_PROPERTY = "chat:categoryid";
  static final String CONTENT_PROPERTY = "chat:content";
  static final String LINK_PROPERTY = "chat:link";
  static final String IS_READ_PROPERTY = "chat:isread";


  protected void initNodetypes()
  {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

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
      try
      {
        NodeType ntToken = nodeTypeManager.getNodeType(TOKEN_NODETYPE);

      }
      catch (NoSuchNodeTypeException nsne)
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
      try
      {
        NodeType ntToken = nodeTypeManager.getNodeType(NOTIF_NODETYPE);

      }
      catch (NoSuchNodeTypeException nsne)
      {
        NodeTypeValue chatNotif = new NodeTypeValue();
        chatNotif.setName(NOTIF_NODETYPE);
        chatNotif.setMixin(false);
        List<String> superTypes = new ArrayList<String>();
        superTypes.add("nt:base");
        chatNotif.setDeclaredSupertypeNames(superTypes);

        PropertyDefinitionValue userProperty = new PropertyDefinitionValue();
        userProperty.setMultiple(false);
        userProperty.setAutoCreate(false);
        userProperty.setName(USER_PROPERTY);
        userProperty.setReadOnly(false);
        userProperty.setRequiredType(PropertyType.STRING);
        userProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue typeProperty = new PropertyDefinitionValue();
        typeProperty.setMultiple(false);
        typeProperty.setAutoCreate(false);
        typeProperty.setName(TYPE_PROPERTY);
        typeProperty.setReadOnly(false);
        typeProperty.setRequiredType(PropertyType.STRING);
        typeProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue categoryProperty = new PropertyDefinitionValue();
        categoryProperty.setMultiple(false);
        categoryProperty.setAutoCreate(false);
        categoryProperty.setName(CATEGORY_PROPERTY);
        categoryProperty.setReadOnly(false);
        categoryProperty.setRequiredType(PropertyType.STRING);
        categoryProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue categoryIdProperty = new PropertyDefinitionValue();
        categoryIdProperty.setMultiple(false);
        categoryIdProperty.setAutoCreate(false);
        categoryIdProperty.setName(CATEGORY_ID_PROPERTY);
        categoryIdProperty.setReadOnly(false);
        categoryIdProperty.setRequiredType(PropertyType.STRING);
        categoryIdProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue linkProperty = new PropertyDefinitionValue();
        linkProperty.setMultiple(false);
        linkProperty.setAutoCreate(false);
        linkProperty.setName(LINK_PROPERTY);
        linkProperty.setReadOnly(false);
        linkProperty.setRequiredType(PropertyType.STRING);
        linkProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue contentProperty = new PropertyDefinitionValue();
        contentProperty.setMultiple(false);
        contentProperty.setAutoCreate(false);
        contentProperty.setName(CONTENT_PROPERTY);
        contentProperty.setReadOnly(false);
        contentProperty.setRequiredType(PropertyType.STRING);
        contentProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue timestampProperty = new PropertyDefinitionValue();
        timestampProperty.setMultiple(false);
        timestampProperty.setAutoCreate(false);
        timestampProperty.setName(TIMESTAMP_PROPERTY);
        timestampProperty.setReadOnly(false);
        timestampProperty.setRequiredType(PropertyType.LONG);
        timestampProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue isReadProperty = new PropertyDefinitionValue();
        isReadProperty.setMultiple(false);
        isReadProperty.setAutoCreate(false);
        isReadProperty.setName(IS_READ_PROPERTY);
        isReadProperty.setReadOnly(false);
        isReadProperty.setRequiredType(PropertyType.BOOLEAN);
        isReadProperty.setOnVersion(OnParentVersionAction.IGNORE);

        List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();
        props.add(userProperty);
        props.add(typeProperty);
        props.add(categoryProperty);
        props.add(categoryIdProperty);
        props.add(contentProperty);
        props.add(linkProperty);
        props.add(timestampProperty);
        props.add(isReadProperty);

        chatNotif.setDeclaredPropertyDefinitionValues(props);

        nodeTypeManager.registerNodeType(chatNotif, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
      }

    }
    catch (RepositoryException e)
    {
      e.printStackTrace();
    }

  }

  protected void initMandatoryNodes()
  {
    try
    {
      //get info
      Session session = JCRBootstrap.getSession();

      Node rootNode = session.getRootNode();
      if (!rootNode.hasNode("tokens"))
      {
        rootNode.addNode("tokens", "nt:unstructured");
        session.save();
      }

      if (!rootNode.hasNode("notifications"))
      {
        rootNode.addNode("notifications", "nt:unstructured");
        session.save();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

  }

}
