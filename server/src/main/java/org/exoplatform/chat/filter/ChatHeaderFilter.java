/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.chat.filter;

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
