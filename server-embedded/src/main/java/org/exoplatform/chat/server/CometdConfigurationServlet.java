package org.exoplatform.chat.server;

import org.cometd.annotation.server.AnnotationCometDServlet;
import org.cometd.annotation.server.ServerAnnotationProcessor;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.client.http.jetty.JettyHttpClientTransport;
import org.cometd.oort.*;
import org.cometd.server.BayeuxServerImpl;
import org.cometd.client.websocket.javax.WebSocketTransport;
import org.eclipse.jetty.client.HttpClient;
import org.exoplatform.chat.listener.GuiceManager;
import org.exoplatform.chat.services.RealTimeMessageService;
import org.exoplatform.chat.services.TokenService;
import org.exoplatform.chat.utils.PropertyManager;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.ws.frameworks.cometd.ServletContextWrapper;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Cometd configuration servlet. It allows to instantiate Cometd custom services, or even configure the Bayeux object
 * by adding extensions or specifying a SecurityPolicy
 */
public class CometdConfigurationServlet extends AnnotationCometDServlet {
  private static final Log LOG = ExoLogger.getExoLogger(CometdConfigurationServlet.class);

  private OortConfigServlet     oConfig;

  private SetiServlet           setiConfig;

  private boolean standalone;

  private boolean clusterEnabled = false;

  public static final String    PREFIX             = "exo.cometd.";

  protected static final String CLOUD_ID_SEPARATOR = PREFIX + "cloudIDSeparator";

  public static String OORT_CONFIG_TYPE = PREFIX + "oort.configType";
  public static String OORT_STATIC = "static";

  public static final Pattern URL_REGEX;
  static {
    String ip_regex = "(((((25[0-5])|(2[0-4][0-9])|([01]?[0-9]?[0-9]))\\.){3}((25[0-4])|(2[0-4][0-9])|((1?[1-9]?[1-9])|([1-9]0))))|(0\\.){3}0)";
    URL_REGEX = Pattern.compile("^((ht|f)tp(s?)://)" // protocol
        + "(\\w+(:\\w+)?@)?" // username:password@
        + "(" + ip_regex // ip
        + "|([0-9a-z_!~*'()-]+\\.)*([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\.[a-z]{2,6}" // domain like www.exoplatform.org
        + "|([a-zA-Z][-a-zA-Z0-9]+))" // domain like localhost
        + "(:[0-9]{1,5})?" // port number :8080
        + "((/?)|(/[0-9a-zA-Z_!~*'().;?:@&=+$,%#-]+)+/?)$"); // uri
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(new ServletConfigWrapper(config));
  }

  public void init() throws ServletException {
    standalone = Boolean.valueOf(PropertyManager.getProperty("standaloneChatServer"));
    if (standalone) {
      LOG.debug("Chat mode detected : 2 servers");

      lazyInit();
    } else {
      LOG.debug("Chat mode detected : 1 server");
      // Add a portal container init task to initialize Cometd stuff
      RootContainer.getInstance().addInitTask(getServletContext(), new RootContainer.PortalContainerPostInitTask() {
        @Override
        public void execute(ServletContext servletContext, PortalContainer portalContainer) {
          // Grab the BayeuxServer object
          lazyInit();
        }
      }, PortalContainer.DEFAULT_PORTAL_CONTAINER_NAME);
    }
  }

  private void lazyInit() {
    try {
      super.init();

      if (standalone) {
        String profiles = org.exoplatform.commons.utils.PropertyManager.getProperty("exo.profiles");
        if (profiles != null) {
          clusterEnabled = profiles.contains("cluster");
          if (clusterEnabled) {
            warnInvalidUrl(getInitParameter(OortConfigServlet.OORT_URL_PARAM));
          }
        }

        String configType = getInitParameter(OORT_CONFIG_TYPE);
        if (OORT_STATIC.equals(configType)) {
          oConfig = new OortStaticConfig();
        } else {
          oConfig = new OortMulticastConfig();
        }
        ServletConfig servletConfig = getServletConfig();
        oConfig.init(servletConfig);

        setiConfig = new SetiServlet();
        setiConfig.init(servletConfig);

        ServletContext cometdContext = servletConfig.getServletContext();
        Seti seti = (Seti) cometdContext.getAttribute(Seti.SETI_ATTRIBUTE);
        Oort oort = (Oort) cometdContext.getAttribute(Oort.OORT_ATTRIBUTE);

        EXoContinuationBayeux bayeux = (EXoContinuationBayeux) getBayeux();
        bayeux.setSeti(seti);
        bayeux.setOort(oort);

        String separator = getInitParameter(CLOUD_ID_SEPARATOR);
        if (separator != null) {
          bayeux.setCloudIDSeparator(separator);
        }
      }

      RealTimeMessageService realTimeMessageService = GuiceManager.getInstance().getInstance(RealTimeMessageService.class);
      realTimeMessageService.setBayeux(getBayeux());
    } catch (ServletException e) {
      LOG.error("Cannot initialize Bayeux", e);
    }
  }

  protected BayeuxServerImpl newBayeuxServer()
  {
    if (standalone) {
      return new EXoContinuationBayeux(GuiceManager.getInstance().getInstance(TokenService.class));
    } else {
      return ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(org.mortbay.cometd.continuation.EXoContinuationBayeux.class);
    }
  }

  protected ServerAnnotationProcessor newServerAnnotationProcessor(BayeuxServer bayeuxServer)
  {
    return new ServerAnnotationProcessor(bayeuxServer);
  }

  private void warnInvalidUrl(String url) {
    if (url == null || url.isEmpty()) {
      LOG.warn("You didn’t set exo.cometd.oort.url, cometd cannot work in cluster mode without this property, please set it.");
    } else if (!URL_REGEX.matcher(url).matches()) {
      LOG.warn("exo.cometd.oort.url is invalid {}, cometd cannot work in cluster mode without this property, please set it.", url);
    }
  }

  private Oort configTransports(Oort oort) {
    ServletConfig config = getServletConfig();
    String transport = config.getInitParameter("transports");
    if (transport == null || !transport.contains(WebSocketTransport.class.getName())) {
      oort.getClientTransportFactories().add(new JettyHttpClientTransport.Factory(new HttpClient()));
    }
    return oort;
  }

  /**
   * This class help to workaround issue with eap 6.2 that has not support
   * Websocket transport yet
   */
  public class OortStaticConfig extends OortStaticConfigServlet {
    private static final long serialVersionUID = 1054209695244836363L;

    @Override
    protected void configureCloud(ServletConfig config, Oort oort) throws Exception {
      if (clusterEnabled) {
        super.configureCloud(config, oort);
      }
    }

    @Override
    protected Oort newOort(BayeuxServer bayeux, String url) {
      Oort oort = super.newOort(bayeux, url);
      return configTransports(oort);
    }
  }

  public class OortMulticastConfig extends OortMulticastConfigServlet {
    private static final long serialVersionUID = 6836833932474627776L;

    @Override
    protected void configureCloud(ServletConfig config, Oort oort) throws Exception {
      if (clusterEnabled) {
        super.configureCloud(config, oort);
      }
    }

    @Override
    protected Oort newOort(BayeuxServer bayeux, String url) {
      Oort oort = super.newOort(bayeux, url);
      return configTransports(oort);
    }
  }


  private class ServletConfigWrapper implements ServletConfig {
    private ServletConfig delegate;
    private ServletContext contextWrapper;
    private String[] configs;

    public ServletConfigWrapper(ServletConfig config) {
      this.delegate = config;
      contextWrapper = new ServletContextWrapper(delegate.getServletContext());
    }

    @Override
    public String getInitParameter(String name) {
      String value = org.exoplatform.commons.utils.PropertyManager.getProperty(PREFIX + name);
      if (value == null) {
        value = delegate.getInitParameter(name);
      }
      return value;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
      if (configs == null) {
        List<String> keys = new LinkedList<String>();
        Properties props = PrivilegedSystemHelper.getProperties();
        int len = PREFIX.length();

        for (Object key : props.keySet()) {
          String k = key.toString().trim();
          if (k.startsWith(PREFIX) && k.length() > len) {
            keys.add(k.substring(len));
          }
        }

        configs = keys.toArray(new String[keys.size()]);
      }
      Set<String> names = new HashSet<String>();
      names.addAll(Collections.list(delegate.getInitParameterNames()));
      names.addAll(Arrays.asList(configs));

      return Collections.enumeration(names);
    }

    @Override
    public ServletContext getServletContext() {
      return contextWrapper;
    }

    @Override
    public String getServletName() {
      return delegate.getServletName();
    }
  }
}
