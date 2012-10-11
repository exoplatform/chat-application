package org.benjp.portlet.notification;

import juzu.Controller;
import juzu.Path;
import juzu.View;
import juzu.template.Template;

import javax.inject.Inject;
import java.io.IOException;

public class NotificationApplication extends Controller
{

  @Inject
  @Path("index.gtmpl")
  Template index;

  @View
  public void index() throws IOException
  {
    index.render();
  }

}
