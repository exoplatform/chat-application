package org.exoplatform.chat.portlet;

import javax.portlet.*;
import java.io.IOException;

public class ChatApplication extends GenericPortlet {

    @Override
    protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        PortletRequestDispatcher prd = getPortletContext().getRequestDispatcher("/index.html");
        prd.include(request, response);
    }
}