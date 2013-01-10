package org.benjp.portlet.chat;

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

    Element metaViewport = response.createElement("meta");
    metaViewport.setAttribute("name", "viewport");
    metaViewport.setAttribute("content", "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0");
    response.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT, metaViewport);

    metaViewport = response.createElement("meta");
    metaViewport.setAttribute("name", "apple-mobile-web-app-capable");
    metaViewport.setAttribute("content", "yes");
    response.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT, metaViewport);

    metaViewport = response.createElement("link");
    metaViewport.setAttribute("rel", "apple-touch-icon");
    metaViewport.setAttribute("href", "/chat/img/chat-icon.png");
    response.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT, metaViewport);

    //
    chain.doFilter(request, response);
  }

  public void destroy()
  {
  }
}
