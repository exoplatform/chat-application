package org.exoplatform.chat.server;

import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.annotation.Subscription;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;

import javax.inject.Inject;

/**
 * This service is used to receive all Cometd messages and then publish the messages to the right clients.
 * A Cometd service channel is used so the messages sent by clients on this channel are only sent to the server (so
 * by this service) and not to all the subscribed clients. It is up to the server to forward the messages to
 * the right clients.
 */
@Service
public class CometdService {

  @Inject
  private BayeuxServer bayeuxServer;

  @Listener("/service/chat")
  public void onMessageReceived(final ServerSession remoteSession, final ServerMessage message) {
    System.out.println(">>>>>>>>> message received on /service/chat : " + message.getJSON());

    //TODO read each message data and send it each room member. It requires to use 'deliver' instead of 'publish'
    //to avoid broadcasting the message to all connected clients (even the ones not members of the target room)
  }

}
