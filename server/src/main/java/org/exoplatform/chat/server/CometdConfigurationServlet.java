package org.exoplatform.chat.server;

import org.cometd.annotation.ServerAnnotationProcessor;
import org.cometd.bayeux.server.BayeuxServer;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;

import javax.servlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cometd configuration servlet. It allows to instantiate Cometd custom services, or even configure the Bayeux object
 * by adding extensions or specifying a SecurityPolicy
 */
public class CometdConfigurationServlet extends GenericServlet {
  private static final Log LOG = ExoLogger.getExoLogger(CometdConfigurationServlet.class);

  private final List<Object> services = new ArrayList<>();
  private ServerAnnotationProcessor processor;

  public void init() throws ServletException {
    ExoContainer exoContainer = ExoContainerContext.getCurrentContainerIfPresent();
    LOG.debug("CometdConfigurationServlet init");
    if(true) {
      LOG.debug("Chat mode detected : 1 server");
      // Add a portal container init task to initialize Cometd stuff
      PortalContainer.addInitTask(getServletContext(), new RootContainer.PortalContainerPostInitTask() {
        @Override
        public void execute(ServletContext servletContext, PortalContainer portalContainer) {
          // Grab the BayeuxServer object
          BayeuxServer bayeux = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(EXoContinuationBayeux.class);

          // Create the annotation processor
          processor = new ServerAnnotationProcessor(bayeux);

          // Create the Cometd annotated service instance and process it
          Object service = new CometdService();
          processor.process(service);
          services.add(service);
        }
      });
    } else {
      LOG.debug("Chat mode detected : 2 servers");

      // Grab the BayeuxServer object
      BayeuxServer bayeux = (BayeuxServer)getServletContext().getAttribute(BayeuxServer.ATTRIBUTE);

      // Create the annotation processor
      processor = new ServerAnnotationProcessor(bayeux);

      // Create the Cometd annotated service instance and process it
      Object service = new CometdService();
      processor.process(service);
      services.add(service);
    }
  }

  public void destroy() {
    // Deprocess the services that have been created
    for (Object service : services) {
      processor.deprocess(service);
    }
  }

  public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
    throw new ServletException();
  }
}
