package org.benjp.portlet.chat;

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.filter.FilterChain;
import javax.portlet.filter.FilterConfig;
import javax.portlet.filter.RenderFilter;
import java.io.IOException;

public class ResponseFilter implements RenderFilter
{
  @Override
  public void doFilter(RenderRequest renderRequest, RenderResponse renderResponse, FilterChain filterChain) throws IOException, PortletException {

  }

  @Override
  public void init(FilterConfig filterConfig) throws PortletException {
  }

  @Override
  public void destroy() {
  }
}
