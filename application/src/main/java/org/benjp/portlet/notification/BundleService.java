package org.benjp.portlet.notification;

import juzu.Resource;
import juzu.Response;
import juzu.plugin.ajax.Ajax;
import juzu.request.ResourceContext;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

@ApplicationScoped
public class BundleService {

  Map<String, String> resourceBundles = new HashMap<String, String>();

  @Ajax
  @Resource
  public Response.Content getBundle(String target, ResourceContext resourceContext)
  {
    Locale locale = resourceContext.getUserContext().getLocale();
    String loc = locale.getCountry()+"_"+locale.getLanguage();
    String out = "";
    if (resourceBundles.containsKey(loc))
    {
      out = resourceBundles.get(loc);
    }
    else {
      ResourceBundle bundle= resourceContext.getApplicationContext().resolveBundle(locale) ;

      StringBuffer sb = new StringBuffer();
      sb.append("var "+target+" = {\"version\":\"0.9.1\"");
      for (String key:bundle.keySet())
      {
        String value = bundle.getString(key).replaceAll("\"", "\\\\\"");
        String tkey = key.replaceAll("\\.", "_");
        if (tkey.indexOf("benjp_")==0)
          sb.append(", \""+tkey+"\":\""+value+"\"");
      }
      sb.append("};");

      out = sb.toString();
      resourceBundles.put(loc, out);
    }

    return Response.ok(out).withMimeType("text/javascript; charset=UTF-8").withHeader("Cache-Control", "no-cache");
  }


}
