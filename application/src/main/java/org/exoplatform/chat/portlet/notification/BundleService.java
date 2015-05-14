package org.exoplatform.chat.portlet.notification;

import juzu.Resource;
import juzu.Response;
import juzu.impl.common.Tools;
import juzu.plugin.ajax.Ajax;
import juzu.request.ApplicationContext;
import juzu.request.UserContext;

import javax.enterprise.context.ApplicationScoped;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@ApplicationScoped
public class BundleService {

  Map<String, String> resourceBundles = new HashMap<String, String>();

  @Ajax
  @Resource
  public Response.Content getBundle(String target, ApplicationContext applicationContext, UserContext userContext)
  {
    Locale locale = userContext.getLocale();
    ResourceBundle bundle= applicationContext.resolveBundle(locale) ;
    return Response.ok(getBundle(target, bundle, locale)).withMimeType("text/javascript; charset=UTF-8").withHeader("Cache-Control", "no-cache")
                   .withCharset(Tools.UTF_8);
  }

  public String getBundle(String target, ResourceBundle bundle, Locale locale) {
    String loc = locale.getCountry()+"_"+locale.getLanguage();
    String out = "";
    if (resourceBundles.containsKey(loc))
    {
      out = resourceBundles.get(loc);
    }
    else {
      StringBuffer sb = new StringBuffer();
      sb.append("var "+target+" = {\"version\":\""+getVersion()+"\"");
      for (String key:bundle.keySet())
      {
        String value = bundle.getString(key).replaceAll("\"", "\\\\\"");
        String tkey = key.replaceAll("\\.", "_");
        if (tkey.indexOf("exoplatform_")==0)
          sb.append(", \""+tkey+"\":\""+value+"\"");
      }
      sb.append("};");

      out = sb.toString();
      resourceBundles.put(loc, out);
    }

    return out;
  }

  private static String getVersion()
  {
    InputStream inputStream = BundleService.class.getClassLoader().getResourceAsStream("/META-INF/MANIFEST.MF");

    String version = "N/A";
    try {
      Manifest manifest = new Manifest(inputStream);
      Attributes attributes = manifest.getMainAttributes();
      version = attributes.getValue("Implementation-Version");
    }
    catch(Exception e) {
      version = "N/A";
    }

    return version;
  }

}
