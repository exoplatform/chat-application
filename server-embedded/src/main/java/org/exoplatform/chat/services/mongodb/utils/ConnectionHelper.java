package org.exoplatform.chat.services.mongodb.utils;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.chat.utils.PropertyManager;

import com.mongodb.ServerAddress;

/**
 * This class provide a simple helper method to parse mongodb server addresses
 */

public class ConnectionHelper {

  public static List<ServerAddress> getMongoServerAdresses() throws Exception {
    String replicaSetHosts = PropertyManager.getProperty(PropertyManager.PROPERTY_SERVERS_HOSTS);
    List<ServerAddress> serverList = new ArrayList<>();
    if (StringUtils.isNotBlank(replicaSetHosts)) {
      for (String serverHost : replicaSetHosts.split(",")) {
        if (serverHost.contains(":")) {
          String[] server = serverHost.split(":");
          try {
            ServerAddress address = new ServerAddress(server[0], Integer.parseInt(server[1]));
            serverList.add(address);
          } catch (NumberFormatException e) {
            throw new UnknownHostException(server[1] + " is not a valid mongodb port");
          }
        } else {
          throw new UnknownHostException(serverHost + " is not a valid mongodb host");
        }
      }
    } else {
      String host = PropertyManager.getProperty(PropertyManager.PROPERTY_SERVER_HOST);
      String port = PropertyManager.getProperty(PropertyManager.PROPERTY_SERVER_PORT);
      try {
        ServerAddress address = new ServerAddress(host, Integer.parseInt(port));
        serverList.add(address);
      } catch (NumberFormatException e) {
        throw new UnknownHostException(host + ":" + port + " is not a valid mongodb host");
      }
    }
    return serverList;
  }
}
