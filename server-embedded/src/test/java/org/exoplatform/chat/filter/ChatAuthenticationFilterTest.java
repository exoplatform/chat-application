package org.exoplatform.chat.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test class for ChatAuthenticationFilter
 */
@RunWith(MockitoJUnitRunner.Silent.class)
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
  public void doFilter() throws Exception {
    // Given
    Mockito.when(request.getRequestURI()).thenReturn("/");
    Mockito.when(request.getContextPath()).thenReturn("/");
    Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer t0k3n");

    // When
    chatAuthenticationFilter.doFilter(request, response, filterChain);

    ArgumentCaptor<ServletRequest> argumentRequest = ArgumentCaptor.forClass(ServletRequest.class);
    Mockito.verify(filterChain).doFilter(argumentRequest.capture(), Mockito.any(HttpServletResponse.class));
    assertTrue(argumentRequest.getValue() instanceof ChatAuthenticationFilter.ChatHttpServletRequestWrapper);
  }

}