package org.benjp.services.jcr;

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
  static final String USER_NODETYPE = "chat:user";
  static final String ROOM_NODETYPE = "chat:room";
  static final String MESSAGE_NODETYPE = "chat:message";
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
  static final String EMAIL_PROPERTY = "chat:email";
  static final String FAVORITES_PROPERTY = "chat:favorites";
  static final String FULLNAME_PROPERTY = "chat:fullname";
  static final String SPACES_PROPERTY = "chat:spaces";
  static final String TEAMS_PROPERTY = "chat:teams";
  static final String TEAM_PROPERTY = "chat:team";
  static final String SPACE_PROPERTY = "chat:space";
  static final String USERS_PROPERTY = "chat:users";
  static final String STATUS_PROPERTY = "chat:status";
  static final String IS_SUPPORT_ADMIN_PROPERTY = "chat:issupport";
  static final String ID_PROPERTY = "chat:id";
  static final String DISPLAY_NAME_PROPERTY = "chat:displayname";
  static final String GROUP_ID_PROPERTY = "chat:groupid";
  static final String SHORT_NAME_PROPERTY = "chat:shortname";
  static final String MESSAGE_PROPERTY = "chat:message";
  static final String TIME_PROPERTY = "chat:time";
  static final String IS_SYSTEM_PROPERTY = "chat:issystem";
  static final String OPTIONS_PROPERTY = "chat:option";


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

      try
      {
        NodeType ntRoom = nodeTypeManager.getNodeType(ROOM_NODETYPE);

      }
      catch (NoSuchNodeTypeException nsne)
      {
        NodeTypeValue chatRoom = new NodeTypeValue();
        chatRoom.setName(ROOM_NODETYPE);
        chatRoom.setMixin(false);
        List<String> superTypes = new ArrayList<String>();
        superTypes.add("nt:base");
        chatRoom.setDeclaredSupertypeNames(superTypes);

        PropertyDefinitionValue idProperty = new PropertyDefinitionValue();
        idProperty.setMultiple(false);
        idProperty.setAutoCreate(false);
        idProperty.setName(ID_PROPERTY);
        idProperty.setReadOnly(false);
        idProperty.setRequiredType(PropertyType.STRING);
        idProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue displayNameProperty = new PropertyDefinitionValue();
        displayNameProperty.setMultiple(false);
        displayNameProperty.setAutoCreate(false);
        displayNameProperty.setName(DISPLAY_NAME_PROPERTY);
        displayNameProperty.setReadOnly(false);
        displayNameProperty.setRequiredType(PropertyType.STRING);
        displayNameProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue groupIdProperty = new PropertyDefinitionValue();
        groupIdProperty.setMultiple(false);
        groupIdProperty.setAutoCreate(false);
        groupIdProperty.setName(GROUP_ID_PROPERTY);
        groupIdProperty.setReadOnly(false);
        groupIdProperty.setRequiredType(PropertyType.STRING);
        groupIdProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue shortNameProperty = new PropertyDefinitionValue();
        shortNameProperty.setMultiple(false);
        shortNameProperty.setAutoCreate(false);
        shortNameProperty.setName(SHORT_NAME_PROPERTY);
        shortNameProperty.setReadOnly(false);
        shortNameProperty.setRequiredType(PropertyType.STRING);
        shortNameProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue spaceProperty = new PropertyDefinitionValue();
        spaceProperty.setMultiple(false);
        spaceProperty.setAutoCreate(false);
        spaceProperty.setName(SPACE_PROPERTY);
        spaceProperty.setReadOnly(false);
        spaceProperty.setRequiredType(PropertyType.STRING);
        spaceProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue teamProperty = new PropertyDefinitionValue();
        teamProperty.setMultiple(false);
        teamProperty.setAutoCreate(false);
        teamProperty.setName(TEAM_PROPERTY);
        teamProperty.setReadOnly(false);
        teamProperty.setRequiredType(PropertyType.STRING);
        teamProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue userProperty = new PropertyDefinitionValue();
        userProperty.setMultiple(false);
        userProperty.setAutoCreate(false);
        userProperty.setName(USER_PROPERTY);
        userProperty.setReadOnly(false);
        userProperty.setRequiredType(PropertyType.STRING);
        userProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue usersProperty = new PropertyDefinitionValue();
        usersProperty.setMultiple(true);
        usersProperty.setAutoCreate(false);
        usersProperty.setName(USERS_PROPERTY);
        usersProperty.setReadOnly(false);
        usersProperty.setRequiredType(PropertyType.STRING);
        usersProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue timestampProperty = new PropertyDefinitionValue();
        timestampProperty.setMultiple(false);
        timestampProperty.setAutoCreate(false);
        timestampProperty.setName(TIMESTAMP_PROPERTY);
        timestampProperty.setReadOnly(false);
        timestampProperty.setRequiredType(PropertyType.LONG);
        timestampProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue typeProperty = new PropertyDefinitionValue();
        typeProperty.setMultiple(false);
        typeProperty.setAutoCreate(false);
        typeProperty.setName(TYPE_PROPERTY);
        typeProperty.setReadOnly(false);
        typeProperty.setRequiredType(PropertyType.STRING);
        typeProperty.setOnVersion(OnParentVersionAction.IGNORE);


        List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();
        props.add(idProperty);
        props.add(displayNameProperty);
        props.add(groupIdProperty);
        props.add(shortNameProperty);
        props.add(spaceProperty);
        props.add(teamProperty);
        props.add(userProperty);
        props.add(usersProperty);
        props.add(timestampProperty);
        props.add(typeProperty);

        chatRoom.setDeclaredPropertyDefinitionValues(props);

        nodeTypeManager.registerNodeType(chatRoom, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
      }

      try
      {
        NodeType ntUser = nodeTypeManager.getNodeType(USER_NODETYPE);

      }
      catch (NoSuchNodeTypeException nsne)
      {
        NodeTypeValue chatUser = new NodeTypeValue();
        chatUser.setName(USER_NODETYPE);
        chatUser.setMixin(false);
        List<String> superTypes = new ArrayList<String>();
        superTypes.add("nt:base");
        chatUser.setDeclaredSupertypeNames(superTypes);

        PropertyDefinitionValue userProperty = new PropertyDefinitionValue();
        userProperty.setMultiple(false);
        userProperty.setAutoCreate(false);
        userProperty.setName(USER_PROPERTY);
        userProperty.setReadOnly(false);
        userProperty.setRequiredType(PropertyType.STRING);
        userProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue statusProperty = new PropertyDefinitionValue();
        statusProperty.setMultiple(false);
        statusProperty.setAutoCreate(false);
        statusProperty.setName(STATUS_PROPERTY);
        statusProperty.setReadOnly(false);
        statusProperty.setRequiredType(PropertyType.STRING);
        statusProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue spacesProperty = new PropertyDefinitionValue();
        spacesProperty.setMultiple(true);
        spacesProperty.setAutoCreate(false);
        spacesProperty.setName(SPACES_PROPERTY);
        spacesProperty.setReadOnly(false);
        spacesProperty.setRequiredType(PropertyType.STRING);
        spacesProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue teamsProperty = new PropertyDefinitionValue();
        teamsProperty.setMultiple(true);
        teamsProperty.setAutoCreate(false);
        teamsProperty.setName(TEAMS_PROPERTY);
        teamsProperty.setReadOnly(false);
        teamsProperty.setRequiredType(PropertyType.STRING);
        teamsProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue favoritesProperty = new PropertyDefinitionValue();
        favoritesProperty.setMultiple(true);
        favoritesProperty.setAutoCreate(false);
        favoritesProperty.setName(FAVORITES_PROPERTY);
        favoritesProperty.setReadOnly(false);
        favoritesProperty.setRequiredType(PropertyType.STRING);
        favoritesProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue emailProperty = new PropertyDefinitionValue();
        emailProperty.setMultiple(false);
        emailProperty.setAutoCreate(false);
        emailProperty.setName(EMAIL_PROPERTY);
        emailProperty.setReadOnly(false);
        emailProperty.setRequiredType(PropertyType.STRING);
        emailProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue fullnameProperty = new PropertyDefinitionValue();
        fullnameProperty.setMultiple(false);
        fullnameProperty.setAutoCreate(false);
        fullnameProperty.setName(FULLNAME_PROPERTY);
        fullnameProperty.setReadOnly(false);
        fullnameProperty.setRequiredType(PropertyType.STRING);
        fullnameProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue isSupport = new PropertyDefinitionValue();
        isSupport.setMultiple(false);
        isSupport.setAutoCreate(false);
        isSupport.setName(IS_SUPPORT_ADMIN_PROPERTY);
        isSupport.setReadOnly(false);
        isSupport.setRequiredType(PropertyType.BOOLEAN);
        isSupport.setOnVersion(OnParentVersionAction.IGNORE);

        List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();
        props.add(userProperty);
        props.add(statusProperty);
        props.add(spacesProperty);
        props.add(teamsProperty);
        props.add(favoritesProperty);
        props.add(emailProperty);
        props.add(fullnameProperty);
        props.add(isSupport);

        chatUser.setDeclaredPropertyDefinitionValues(props);

        nodeTypeManager.registerNodeType(chatUser, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
      }

      try
      {
        NodeType ntMsg = nodeTypeManager.getNodeType(MESSAGE_NODETYPE);

      }
      catch (NoSuchNodeTypeException nsne)
      {
        NodeTypeValue chatMessage = new NodeTypeValue();
        chatMessage.setName(MESSAGE_NODETYPE);
        chatMessage.setMixin(false);
        List<String> superTypes = new ArrayList<String>();
        superTypes.add("nt:base");
        chatMessage.setDeclaredSupertypeNames(superTypes);

        PropertyDefinitionValue userProperty = new PropertyDefinitionValue();
        userProperty.setMultiple(false);
        userProperty.setAutoCreate(false);
        userProperty.setName(USER_PROPERTY);
        userProperty.setReadOnly(false);
        userProperty.setRequiredType(PropertyType.STRING);
        userProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue messageProperty = new PropertyDefinitionValue();
        messageProperty.setMultiple(false);
        messageProperty.setAutoCreate(false);
        messageProperty.setName(MESSAGE_PROPERTY);
        messageProperty.setReadOnly(false);
        messageProperty.setRequiredType(PropertyType.STRING);
        messageProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue timeProperty = new PropertyDefinitionValue();
        timeProperty.setMultiple(false);
        timeProperty.setAutoCreate(false);
        timeProperty.setName(TIME_PROPERTY);
        timeProperty.setReadOnly(false);
        timeProperty.setRequiredType(PropertyType.DATE);
        timeProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue timestampProperty = new PropertyDefinitionValue();
        timestampProperty.setMultiple(false);
        timestampProperty.setAutoCreate(false);
        timestampProperty.setName(TIMESTAMP_PROPERTY);
        timestampProperty.setReadOnly(false);
        timestampProperty.setRequiredType(PropertyType.LONG);
        timestampProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue typeProperty = new PropertyDefinitionValue();
        typeProperty.setMultiple(false);
        typeProperty.setAutoCreate(false);
        typeProperty.setName(TYPE_PROPERTY);
        typeProperty.setReadOnly(false);
        typeProperty.setRequiredType(PropertyType.STRING);
        typeProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue optionsProperty = new PropertyDefinitionValue();
        optionsProperty.setMultiple(false);
        optionsProperty.setAutoCreate(false);
        optionsProperty.setName(OPTIONS_PROPERTY);
        optionsProperty.setReadOnly(false);
        optionsProperty.setRequiredType(PropertyType.STRING);
        optionsProperty.setOnVersion(OnParentVersionAction.IGNORE);

        PropertyDefinitionValue isSystem = new PropertyDefinitionValue();
        isSystem.setMultiple(false);
        isSystem.setAutoCreate(false);
        isSystem.setName(IS_SYSTEM_PROPERTY);
        isSystem.setReadOnly(false);
        isSystem.setRequiredType(PropertyType.BOOLEAN);
        isSystem.setOnVersion(OnParentVersionAction.IGNORE);

        List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();
        props.add(userProperty);
        props.add(messageProperty);
        props.add(timeProperty);
        props.add(timestampProperty);
        props.add(typeProperty);
        props.add(optionsProperty);
        props.add(isSystem);

        chatMessage.setDeclaredPropertyDefinitionValues(props);

        nodeTypeManager.registerNodeType(chatMessage, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
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
      if (!rootNode.hasNode("chat"))
      {
        rootNode.addNode("chat", "nt:unstructured");
        session.save();
      }
      Node chatNode = rootNode.getNode("chat");

      if (!chatNode.hasNode("tokens"))
      {
        chatNode.addNode("tokens", "nt:unstructured");
        session.save();
      }

      if (!chatNode.hasNode("notifications"))
      {
        chatNode.addNode("notifications", "nt:unstructured");
        session.save();
      }

      if (!chatNode.hasNode("users"))
      {
        chatNode.addNode("users", "nt:unstructured");
        session.save();
      }

      if (!chatNode.hasNode("room_rooms"))
      {
        chatNode.addNode("room_rooms", "nt:unstructured");
        session.save();
      }

      if (!chatNode.hasNode("rooms"))
      {
        chatNode.addNode("rooms", "nt:unstructured");
        session.save();
      }

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

  }

}
