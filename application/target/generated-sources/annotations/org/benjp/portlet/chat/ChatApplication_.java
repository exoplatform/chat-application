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
public class ChatApplication_ {
private static final MethodDescriptor method_0 = new MethodDescriptor("ChatApplication.index",Phase.VIEW,org.benjp.portlet.chat.ChatApplication.class,Tools.safeGetMethod(org.benjp.portlet.chat.ChatApplication.class,"index"), Arrays.<ParameterDescriptor>asList());
public static Update index() { return ((ActionContext)Request.getCurrent().getContext()).createResponse(method_0); }
public static URLBuilder indexURL() { return ((MimeContext)Request.getCurrent().getContext()).createURLBuilder(method_0); }
public static final ControllerDescriptor DESCRIPTOR = new ControllerDescriptor(ChatApplication.class,Arrays.<MethodDescriptor>asList(method_0));
}
