/*
 * Copyright (C) 2016 eXo Platform SAS.
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

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.chat.listener.GuiceManager;
import org.exoplatform.chat.services.TokenService;

import javax.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Filter which handles the authentication token. If available in the header, it exposes it in the request parameters.
 * It allows to pass the authentication token in the header instead of making it visible in the URL (more secure).
 * It relies on Juzu internals which parses the query string to build its Request object and its parameters.
 */
public class ChatAuthenticationFilter implements Filter {
  private static final Logger LOG = Logger.getLogger(ChatAuthenticationFilter.class.getName());

  @Inject
  private TokenService tokenService;

  public ChatAuthenticationFilter() {
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
    doFilter((HttpServletRequest)req, (HttpServletResponse)resp, chain);
  }

  private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
    // Wrap the request in a ChatHttpServletRequestWrapper object if an Authorization header is available
    String authorizationHeader = request.getHeader("Authorization");
    if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      HttpServletRequest wrappedRequest = new ChatHttpServletRequestWrapper(request, authorizationHeader.substring("Bearer ".length()));
      filterChain.doFilter(wrappedRequest, response);
    } else {
      filterChain.doFilter(request, response);
    }
  }

  @Override
  public void destroy() {
  }

  /**
   * HttpServletRequest wrapper which add the token in the parameters and the query string
   */
  public class ChatHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private String PARAM_NAME_TOKEN = "token";

    private String token;

    public ChatHttpServletRequestWrapper(HttpServletRequest request, String token) {
      super(request);
      this.token = token;
    }

    @Override
    public String getParameter(String name) {
      if("token".equals(name)) {
        return token;
      }
      return super.getParameter(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
      Map<String, String[]> parameterMap = new HashMap<>(super.getParameterMap());
      parameterMap.put(PARAM_NAME_TOKEN, new String[]{ token });
      return parameterMap;
    }

    @Override
    public Enumeration<String> getParameterNames() {
      return Collections.enumeration(getParameterMap().keySet());
    }

    @Override
    public String getQueryString() {
      String queryString = super.getQueryString();
      if(StringUtils.isNotEmpty(queryString)) {
        queryString += "&" + PARAM_NAME_TOKEN + "=" + token;
      } else {
        queryString = PARAM_NAME_TOKEN + "=" + token;
      }
      return queryString;
    }
  }
}
