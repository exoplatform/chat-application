package org.exoplatform.chat.server;

import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;

import javax.inject.Inject;

/**
 * This service is used to intercept all Cometd messages and perform the required checks, filters, transformations, ...
 */
@Service
public class CometdService {

  @Inject
  private BayeuxServer bayeuxServer;

  @Listener("/eXo/Application/Chat")
  public boolean onMessageReceived(ServerSession remote, ServerMessage.Mutable message) {
    System.out.println(">>>>>>>>> message received on /eXo/Application/Chat : " + message.getJSON());

    //TODO read each message data and send it each room member. It requires to use 'deliver' instead of 'publish'
    //to avoid broadcasting the message to all connected clients (even the ones not members of the target room)

    //TODO must return false to not perform the processing of subsequent listeners
    // in order to not automatically publish the message
    // return false

    // Temporary return true to test messages delivery to connected clients
    return true;
  }

}
