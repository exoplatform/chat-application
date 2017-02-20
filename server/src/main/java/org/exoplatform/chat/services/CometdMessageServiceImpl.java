package org.exoplatform.chat.services;

import org.exoplatform.chat.model.RealTimeMessageBean;
import org.exoplatform.container.PortalContainer;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.List;

/**
 * Real time messageing service using Cometd
 */
@Named("realTimeMessageService")
@ApplicationScoped
public class CometdMessageServiceImpl implements RealTimeMessageService {

  public static final String COMETD_CHANNEL_NAME = "/service/chat";

  private EXoContinuationBayeux bayeux;

  public CometdMessageServiceImpl() {
    bayeux = PortalContainer.getInstance().getComponentInstanceOfType(EXoContinuationBayeux.class);
  }

  @Override
  public void sendMessage(RealTimeMessageBean realTimeMessageBean, String receiver) {
    if(bayeux.isPresent(receiver)) {
      bayeux.sendMessage(receiver, COMETD_CHANNEL_NAME, realTimeMessageBean.toJSON(), null);
    }
  }

  @Override
  public void sendMessage(RealTimeMessageBean realTimeMessageBean, List<String> receivers) {
    receivers.stream()
            .filter(u -> bayeux.isPresent(u))
            .forEach(u -> bayeux.sendMessage(u, COMETD_CHANNEL_NAME, realTimeMessageBean.toJSON(), null));
  }

  @Override
  public void sendMessageToAll(RealTimeMessageBean realTimeMessageBean) {
    bayeux.getSessions().stream().forEach(s -> s.deliver(s, COMETD_CHANNEL_NAME, realTimeMessageBean.toJSON()));
  }
}
