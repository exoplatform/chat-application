package org.exoplatform.chat.services.mongodb.utils;

import static org.junit.Assert.assertEquals;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.exoplatform.chat.utils.PropertyManager;
import org.junit.Test;

import com.mongodb.ServerAddress;

/**
 * Created by eXo Platform SAS.
 *
 * @author Ahmed Zaoui <azaoui@exoplatform.com>
 */
public class ConnectionHelperTest {

  @Test
  public void dbServerHostTest() throws Exception {
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVER_PORT, "27775");
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVER_HOST, "localhost");
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVERS_HOSTS, "");
    List<ServerAddress> actual = ConnectionHelper.getMongoServerAdresses();
    assertEquals(actual.size(), 1);
    assertEquals(27775, actual.get(0).getPort());
    assertEquals("localhost", actual.get(0).getHost());
  }

  @Test
  public void dbServerHostsTest() throws Exception {
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVERS_HOSTS, "localhost:4445,localhost:4449");
    List<ServerAddress> expectedReplicatSet = new ArrayList<ServerAddress>();
    expectedReplicatSet.add(new ServerAddress("localhost", 4445));
    expectedReplicatSet.add(new ServerAddress("localhost", 4449));
    List<ServerAddress> actualReplicatSet = ConnectionHelper.getMongoServerAdresses();
    assertEquals(expectedReplicatSet.size(), actualReplicatSet.size());
    assertEquals(expectedReplicatSet, actualReplicatSet);
  }

  public void dbServerHostsWithIP() throws Exception {
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVER_PORT, "2700");
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVER_HOST, "localhost");
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVERS_HOSTS, "localhost:4445,localhost:4449,192.172.168.10:27010");
    List<ServerAddress> expectedReplicatSet = new ArrayList<ServerAddress>();
    expectedReplicatSet.add(new ServerAddress("localhost", 4445));
    expectedReplicatSet.add(new ServerAddress("localhost", 4449));
    expectedReplicatSet.add(new ServerAddress("192.172.168.10", 27010));
    List<ServerAddress> actualReplicatSet = ConnectionHelper.getMongoServerAdresses();
    assertEquals(expectedReplicatSet.size(), actualReplicatSet.size());
    assertEquals(expectedReplicatSet, actualReplicatSet);
  }

  @Test(expected = UnknownHostException.class)
  public void dbServerHostsWithIPFail() throws Exception {
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVERS_HOSTS, "localhost:4445,localhost:4449192.172.168.10:27010");
    List<ServerAddress> expectedReplicatSet = new ArrayList<ServerAddress>();
    expectedReplicatSet.add(new ServerAddress("localhost", 4445));
    expectedReplicatSet.add(new ServerAddress("localhost", 4449));
    expectedReplicatSet.add(new ServerAddress("192.172.168.10", 27010));
    List<ServerAddress> actualReplicatSet = ConnectionHelper.getMongoServerAdresses();
    assertEquals(expectedReplicatSet.size(), actualReplicatSet.size());
  }

  @Test(expected = UnknownHostException.class)
  public void dbServerHostTestFail() throws Exception {
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVER_PORT, "27xxx");
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVER_HOST, "localhost");
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVERS_HOSTS, "");
    ConnectionHelper.getMongoServerAdresses();
  }
}
