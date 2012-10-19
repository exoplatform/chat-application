package org.benjp.server;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ChatHeaderFilter implements Filter
{
  @Override
  public void init(FilterConfig filterConfig) throws ServletException
  {
  }

  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException
  {
    doFilter((HttpServletRequest)req, (HttpServletResponse)resp, chain);
  }

  private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
  {

    response.setHeader("Access-Control-Allow-Origin", "*");

    filterChain.doFilter(request, response);

  }

  @Override
  public void destroy()
  {
  }
}
