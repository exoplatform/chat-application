package org.benjp.services.jcr;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class JCRBootstrap {
  private static RepositoryService repositoryService_;
  private static SessionProviderService sessionProviderService_;

  public static void init()
  {
    if (repositoryService_ == null)
    {
      PortalContainer portalContainer = PortalContainer.getInstance();
      repositoryService_ = (RepositoryService)portalContainer.getComponentInstanceOfType(RepositoryService.class);
      sessionProviderService_ = (SessionProviderService)portalContainer.getComponentInstanceOfType(SessionProviderService.class);
    }

    sessionProviderService_.getSystemSessionProvider(null);
  }

  public static Session getSession()
  {
    if (sessionProviderService_ == null) JCRBootstrap.init();

    Session session = null;
    try {
      session = sessionProviderService_.getSystemSessionProvider(null).getSession("collaboration", repositoryService_.getCurrentRepository());
    } catch (RepositoryException e) {
      e.printStackTrace();
    }

    return session;

  }

  public static void close() {
    repositoryService_ = null;
    sessionProviderService_ = null;
  }
}
