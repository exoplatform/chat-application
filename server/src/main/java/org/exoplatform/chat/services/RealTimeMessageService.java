package org.exoplatform.chat.services;

import org.cometd.bayeux.server.BayeuxServer;
import org.exoplatform.chat.model.RealTimeMessageBean;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;

import java.util.List;

/**
 * Interface for real time messaging service
 */
public interface RealTimeMessageService {

  void setBayeux(BayeuxServer bayeux);

  void sendMessage(RealTimeMessageBean realTimeMessageBean, String receiver);

  void sendMessage(RealTimeMessageBean realTimeMessageBean, List<String> receivers);

  void sendMessageToAll(RealTimeMessageBean realTimeMessageBean);

}
