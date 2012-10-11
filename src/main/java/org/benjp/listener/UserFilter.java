package org.benjp.listener;

import org.benjp.services.UserService;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class UserFilter implements Filter
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
    if (request.getRemoteUser()!=null)
    {
//      System.out.println("FILTER :: "+request.getRemoteUser());

      UserService.addUser(request.getRemoteUser(), request.getSession().getId());
    }

    filterChain.doFilter(request, response);

  }

  @Override
  public void destroy()
  {
  }
}
