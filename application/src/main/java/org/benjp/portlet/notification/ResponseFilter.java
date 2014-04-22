package org.benjp.portlet.notification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.portlet.MimeResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.filter.FilterChain;
import javax.portlet.filter.FilterConfig;
import javax.portlet.filter.RenderFilter;

import org.benjp.utils.PropertyManager;
import org.w3c.dom.Element;

/** @author <a href="mailto:bpaillereau@exoplatform.com">Benjamin Paillereau</a> */
public class ResponseFilter implements RenderFilter
{

  public static final String INVALID_WEEMO_JS_MESSAGE = "{\"error\":503,\"error_description\":\"this webapp_identifier is not valid !\"}";
  
  public void init(FilterConfig filterConfig) throws PortletException
  {
  }

  public void doFilter(RenderRequest request, RenderResponse response, FilterChain chain) throws IOException, PortletException
  {

    String chatWeemoKey = PropertyManager.getProperty(PropertyManager.PROPERTY_WEEMO_KEY);

    if (chatWeemoKey!=null && !"".equals(chatWeemoKey)) {
      Element jQuery1 = response.createElement("script");
      jQuery1.setAttribute("type", "text/javascript");
      if(checkWeemoJavaScript()){
        jQuery1.setAttribute("src", "https://download.weemo.com/js/webappid/" + chatWeemoKey);
      }else{
        jQuery1.setTextContent("console.log('Can not load https://download.weemo.com/js/webappid/" + chatWeemoKey + "');");
      }
      response.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT, jQuery1);
    }

    //
    chain.doFilter(request, response);
  }
  
  private boolean checkWeemoJavaScript(){
    String chatWeemoKey = PropertyManager.getProperty(PropertyManager.PROPERTY_WEEMO_KEY);
    URL url;
    
    try {
      url = new URL("https://download.weemo.com/js/webappid/" + chatWeemoKey);
      URLConnection conn = url.openConnection();
      
      BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      
      StringBuilder stringBuilder = new StringBuilder();
      String inputLine;
      while ((inputLine = br.readLine()) != null) {
        stringBuilder.append(inputLine);
      }
      
      br.close();
      
      return ! (stringBuilder.toString().toUpperCase().equals(INVALID_WEEMO_JS_MESSAGE.toUpperCase()));
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public void destroy()
  {
  }
}
