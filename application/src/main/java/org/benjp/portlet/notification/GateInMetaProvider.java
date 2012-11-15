package org.benjp.portlet.notification;

import javax.inject.Provider;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class GateInMetaProvider implements juzu.inject.ProviderFactory
{
  public <T> Provider<? extends T> getProvider(final Class<T> implementationType)
  {
    return new Provider<T>() {
      public T get() {
        RootContainer rootContainer = RootContainer.getInstance();
        T ret = (T)rootContainer.getComponentInstanceOfType(implementationType);
        if(ret == null)
        {
          PortalContainer portalContainer = PortalContainer.getInstance();
          ret = (T)portalContainer.getComponentInstanceOfType(implementationType);
        }
        return ret;
      }
    };
  }
}
