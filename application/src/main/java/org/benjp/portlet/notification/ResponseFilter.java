package org.benjp.portlet.notification;

import org.benjp.utils.PropertyManager;
import org.w3c.dom.Element;

import javax.portlet.MimeResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.filter.FilterChain;
import javax.portlet.filter.FilterConfig;
import javax.portlet.filter.RenderFilter;
import java.io.IOException;

/** @author <a href="mailto:bpaillereau@exoplatform.com">Benjamin Paillereau</a> */
public class ResponseFilter implements RenderFilter
{

  public void init(FilterConfig filterConfig) throws PortletException
  {
  }

  public void doFilter(RenderRequest request, RenderResponse response, FilterChain chain) throws IOException, PortletException
  {

    String chatWeemoKey = PropertyManager.getProperty(PropertyManager.PROPERTY_WEEMO_KEY);

    if (chatWeemoKey!=null && !"".equals(chatWeemoKey)) {
      Element jQuery1 = response.createElement("script");
      jQuery1.setAttribute("type", "text/javascript");
      jQuery1.setAttribute("src", "https://download.weemo.com/js/webappid/"+chatWeemoKey);
      response.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT, jQuery1);
    }

    //
    chain.doFilter(request, response);
  }

  public void destroy()
  {
  }
}
