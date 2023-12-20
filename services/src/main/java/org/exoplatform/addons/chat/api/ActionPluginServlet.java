package org.exoplatform.addons.chat.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.*;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.commons.api.ui.ActionContext;
import org.exoplatform.commons.api.ui.PlugableUIService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;

/**
 * A Servlet used to invoke Chat Plugins Actions
 */
public class ActionPluginServlet extends GenericServlet {

  private static final long  serialVersionUID     = 1234812387123921L;

  public static final String CHAT_EXTENSION_POPUP = "chat_extension_popup";

  public static final String EX_ACTION_NAME       = "extension_action";

  private PlugableUIService  uiService;

  public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) req;
    HttpServletResponse httpServletResponse = (HttpServletResponse) res;
    String remoteUser = httpServletRequest.getRemoteUser();
    if (StringUtils.isBlank(remoteUser)) {
      httpServletResponse.setStatus(HTTPStatus.NOT_FOUND);
      return;
    }

    Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();
    Map<String, List<String>> parameterListMap = new HashMap<>();
    parameterMap.entrySet()
                .forEach(parameterEntry -> parameterListMap.put(parameterEntry.getKey(),
                                                                parameterEntry.getValue() == null ? Collections.emptyList()
                                                                                                  : Arrays.asList(parameterEntry.getValue())));

    String actionName = httpServletRequest.getParameter(EX_ACTION_NAME);
    ActionContext actContext = new ActionContext(CHAT_EXTENSION_POPUP, actionName);
    actContext.setParams(parameterListMap);

    org.exoplatform.commons.api.ui.Response response = null;
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      response = getUiService().processAction(actContext);
    } finally {
      RequestLifeCycle.end();
    }

    if (response != null) {
      ServletOutputStream outputStream = httpServletResponse.getOutputStream();
      outputStream.write(response.getData());
      outputStream.flush();
      outputStream.close();
    } else {
      httpServletResponse.setStatus(HTTPStatus.NOT_FOUND);
    }
  }

  public PlugableUIService getUiService() {
    if (uiService == null) {
      uiService = CommonsUtils.getService(PlugableUIService.class);
    }
    return uiService;
  }
}
