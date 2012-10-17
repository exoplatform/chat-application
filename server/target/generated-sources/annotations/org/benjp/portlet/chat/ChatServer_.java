package org.benjp.portlet.chat;
import juzu.impl.plugin.controller.descriptor.MethodDescriptor;
import juzu.impl.plugin.controller.descriptor.ParameterDescriptor;
import juzu.impl.common.Tools;
import java.util.Arrays;
import juzu.request.Phase;
import juzu.URLBuilder;
import juzu.impl.plugin.application.ApplicationContext;
import juzu.request.MimeContext;
import juzu.request.ActionContext;
import juzu.Response.Update;
import juzu.impl.plugin.controller.descriptor.ControllerDescriptor;
import javax.annotation.Generated;
import juzu.impl.common.Cardinality;
import juzu.impl.request.Request;
@Generated(value={})
public class ChatServer_ {
private static final MethodDescriptor method_0 = new MethodDescriptor("ChatServer.notification",Phase.RESOURCE,org.benjp.portlet.chat.ChatServer.class,Tools.safeGetMethod(org.benjp.portlet.chat.ChatServer.class,"notification",java.lang.String.class), Arrays.<ParameterDescriptor>asList(new ParameterDescriptor("user",Cardinality.SINGLE,null,java.lang.String.class)));
public static URLBuilder notificationURL(java.lang.String user) { return ((MimeContext)Request.getCurrent().getContext()).createURLBuilder(method_0,(Object)user); }
private static final MethodDescriptor method_1 = new MethodDescriptor("ChatServer.readNotification",Phase.RESOURCE,org.benjp.portlet.chat.ChatServer.class,Tools.safeGetMethod(org.benjp.portlet.chat.ChatServer.class,"readNotification",java.lang.String.class), Arrays.<ParameterDescriptor>asList(new ParameterDescriptor("user",Cardinality.SINGLE,null,java.lang.String.class)));
public static URLBuilder readNotificationURL(java.lang.String user) { return ((MimeContext)Request.getCurrent().getContext()).createURLBuilder(method_1,(Object)user); }
private static final MethodDescriptor method_2 = new MethodDescriptor("ChatServer.whoIsOnline",Phase.RESOURCE,org.benjp.portlet.chat.ChatServer.class,Tools.safeGetMethod(org.benjp.portlet.chat.ChatServer.class,"whoIsOnline",java.lang.String.class,java.lang.String.class), Arrays.<ParameterDescriptor>asList(new ParameterDescriptor("user",Cardinality.SINGLE,null,java.lang.String.class),new ParameterDescriptor("sessionId",Cardinality.SINGLE,null,java.lang.String.class)));
public static URLBuilder whoIsOnlineURL(java.lang.String user,java.lang.String sessionId) { return ((MimeContext)Request.getCurrent().getContext()).createURLBuilder(method_2,new Object[]{user,sessionId}); }
private static final MethodDescriptor method_3 = new MethodDescriptor("ChatServer.send",Phase.RESOURCE,org.benjp.portlet.chat.ChatServer.class,Tools.safeGetMethod(org.benjp.portlet.chat.ChatServer.class,"send",java.lang.String.class,java.lang.String.class,java.lang.String.class,java.lang.String.class,java.lang.String.class), Arrays.<ParameterDescriptor>asList(new ParameterDescriptor("user",Cardinality.SINGLE,null,java.lang.String.class),new ParameterDescriptor("sessionId",Cardinality.SINGLE,null,java.lang.String.class),new ParameterDescriptor("targetUser",Cardinality.SINGLE,null,java.lang.String.class),new ParameterDescriptor("message",Cardinality.SINGLE,null,java.lang.String.class),new ParameterDescriptor("room",Cardinality.SINGLE,null,java.lang.String.class)));
public static URLBuilder sendURL(java.lang.String user,java.lang.String sessionId,java.lang.String targetUser,java.lang.String message,java.lang.String room) { return ((MimeContext)Request.getCurrent().getContext()).createURLBuilder(method_3,new Object[]{user,sessionId,targetUser,message,room}); }
private static final MethodDescriptor method_4 = new MethodDescriptor("ChatServer.updateUnreadMessages",Phase.RESOURCE,org.benjp.portlet.chat.ChatServer.class,Tools.safeGetMethod(org.benjp.portlet.chat.ChatServer.class,"updateUnreadMessages",java.lang.String.class,java.lang.String.class,java.lang.String.class), Arrays.<ParameterDescriptor>asList(new ParameterDescriptor("room",Cardinality.SINGLE,null,java.lang.String.class),new ParameterDescriptor("user",Cardinality.SINGLE,null,java.lang.String.class),new ParameterDescriptor("sessionId",Cardinality.SINGLE,null,java.lang.String.class)));
public static URLBuilder updateUnreadMessagesURL(java.lang.String room,java.lang.String user,java.lang.String sessionId) { return ((MimeContext)Request.getCurrent().getContext()).createURLBuilder(method_4,new Object[]{room,user,sessionId}); }
private static final MethodDescriptor method_5 = new MethodDescriptor("ChatServer.index",Phase.VIEW,org.benjp.portlet.chat.ChatServer.class,Tools.safeGetMethod(org.benjp.portlet.chat.ChatServer.class,"index"), Arrays.<ParameterDescriptor>asList());
public static Update index() { return ((ActionContext)Request.getCurrent().getContext()).createResponse(method_5); }
public static URLBuilder indexURL() { return ((MimeContext)Request.getCurrent().getContext()).createURLBuilder(method_5); }
private static final MethodDescriptor method_6 = new MethodDescriptor("ChatServer.getRoom",Phase.RESOURCE,org.benjp.portlet.chat.ChatServer.class,Tools.safeGetMethod(org.benjp.portlet.chat.ChatServer.class,"getRoom",java.lang.String.class,java.lang.String.class,java.lang.String.class), Arrays.<ParameterDescriptor>asList(new ParameterDescriptor("user",Cardinality.SINGLE,null,java.lang.String.class),new ParameterDescriptor("sessionId",Cardinality.SINGLE,null,java.lang.String.class),new ParameterDescriptor("targetUser",Cardinality.SINGLE,null,java.lang.String.class)));
public static URLBuilder getRoomURL(java.lang.String user,java.lang.String sessionId,java.lang.String targetUser) { return ((MimeContext)Request.getCurrent().getContext()).createURLBuilder(method_6,new Object[]{user,sessionId,targetUser}); }
public static final ControllerDescriptor DESCRIPTOR = new ControllerDescriptor(ChatServer.class,Arrays.<MethodDescriptor>asList(method_0,method_1,method_2,method_3,method_4,method_5,method_6));
}
