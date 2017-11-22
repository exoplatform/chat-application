package org.exoplatform.chat.filter;

import juzu.impl.bridge.spi.servlet.ServletRequestContext;
import juzu.impl.common.Name;
import juzu.impl.common.RunMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.nio.charset.Charset;

import static org.junit.Assert.*;

/**
 * Test class for ChatAuthenticationFilter
 */
@RunWith(MockitoJUnitRunner.class)
public class ChatAuthenticationFilterTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @InjectMocks
  private ChatAuthenticationFilter chatAuthenticationFilter;

  @Test
  public void doFilterWithoutAuthenticationHeader() throws Exception {
    // When
    chatAuthenticationFilter.doFilter(request, response, filterChain);

    // Then
    ArgumentCaptor<ServletRequest> argumentRequest = ArgumentCaptor.forClass(ServletRequest.class);
    Mockito.verify(filterChain).doFilter(argumentRequest.capture(), Mockito.any(HttpServletResponse.class));
    assertTrue(argumentRequest.getValue() instanceof HttpServletRequest);
  }

  @Test
  public void doFilterWithAuthenticationHeader() throws Exception {
    // Given
    Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer t0k3n");

    // When
    chatAuthenticationFilter.doFilter(request, response, filterChain);

    // Then
    ArgumentCaptor<ServletRequest> argumentRequest = ArgumentCaptor.forClass(ServletRequest.class);
    Mockito.verify(filterChain).doFilter(argumentRequest.capture(), Mockito.any(HttpServletResponse.class));
    assertTrue(argumentRequest.getValue() instanceof ChatAuthenticationFilter.ChatHttpServletRequestWrapper);

    ChatAuthenticationFilter.ChatHttpServletRequestWrapper chatHttpServletRequestWrapper = (ChatAuthenticationFilter.ChatHttpServletRequestWrapper) argumentRequest.getValue();

    assertEquals("t0k3n", chatHttpServletRequestWrapper.getParameter("token"));

    assertNotNull(chatHttpServletRequestWrapper.getParameterMap());
    assertEquals(1, chatHttpServletRequestWrapper.getParameterMap().size());
    assertTrue(chatHttpServletRequestWrapper.getParameterMap().containsKey("token"));
    assertEquals("t0k3n", chatHttpServletRequestWrapper.getParameterMap().get("token")[0]);

    assertNotNull(chatHttpServletRequestWrapper.getParameterNames());
    assertEquals("token", chatHttpServletRequestWrapper.getParameterNames().nextElement());

    assertNotNull(chatHttpServletRequestWrapper.getQueryString());
    assertEquals("token=t0k3n", chatHttpServletRequestWrapper.getQueryString());
  }

  @Test
  public void doFilterWithJuzu() throws Exception {
    // Given
    Mockito.when(request.getRequestURI()).thenReturn("/");
    Mockito.when(request.getContextPath()).thenReturn("/");
    Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer t0k3n");

    // When
    chatAuthenticationFilter.doFilter(request, response, filterChain);

    ArgumentCaptor<ServletRequest> argumentRequest = ArgumentCaptor.forClass(ServletRequest.class);
    Mockito.verify(filterChain).doFilter(argumentRequest.capture(), Mockito.any(HttpServletResponse.class));
    assertTrue(argumentRequest.getValue() instanceof ChatAuthenticationFilter.ChatHttpServletRequestWrapper);

    ServletRequestContext servletRequestContext = new ServletRequestContext(Name.parse("TestApp"),
            Charset.forName("UTF-8"),
            (ChatAuthenticationFilter.ChatHttpServletRequestWrapper) argumentRequest.getValue(),
            response,
            "/",
            RunMode.PROD);

    // Then
    assertNotNull(servletRequestContext.getParameters());
    assertEquals(1, servletRequestContext.getParameters().size());
    assertTrue(servletRequestContext.getParameters().containsKey("token"));
    assertEquals("t0k3n", servletRequestContext.getParameters().get("token").getValue());
  }
}