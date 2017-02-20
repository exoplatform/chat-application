package org.exoplatform.chat.services;

import org.exoplatform.chat.model.RealTimeMessageBean;

import java.util.List;

/**
 * Interface for real time messaging service
 */
public interface RealTimeMessageService {

  void sendMessage(RealTimeMessageBean realTimeMessageBean, String receiver);

  void sendMessage(RealTimeMessageBean realTimeMessageBean, List<String> receivers);

  void sendMessageToAll(RealTimeMessageBean realTimeMessageBean);

}
