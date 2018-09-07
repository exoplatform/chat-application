package org.exoplatform.chat.services.mongodb.utils;

import static org.junit.Assert.assertEquals;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.exoplatform.chat.utils.PropertyManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.ServerAddress;

/**
 * Created by eXo Platform SAS.
 *
 * @author Ahmed Zaoui <azaoui@exoplatform.com>
 */
public class ConnectionHelperTest {

  @Before
  public void before() {
    PropertyManager.forceReload();
  }

  @After
  public void after() {
    PropertyManager.forceReload();

    System.clearProperty(PropertyManager.EXO_PROPERTY_SYSTEM_PREFIX + PropertyManager.PROPERTY_SERVER_HOST);
    System.clearProperty(PropertyManager.PROPERTY_SYSTEM_PREFIX + PropertyManager.PROPERTY_SERVER_PORT);
    System.clearProperty(PropertyManager.EXO_PROPERTY_SYSTEM_PREFIX + PropertyManager.PROPERTY_SERVERS_HOSTS);
    System.clearProperty(PropertyManager.PROPERTY_SYSTEM_PREFIX + PropertyManager.PROPERTY_SERVERS_HOSTS);
  }

  @Test
  public void testDefaultProperties() throws Exception {
    //No system properties nor config
    List<ServerAddress> actual = ConnectionHelper.getMongoServerAdresses();
    assertEquals(1, actual.size());
    assertEquals(27017, actual.get(0).getPort());
    assertEquals("localhost", actual.get(0).getHost());
  }

  @Test
  public void testServerHostSystemProperties() throws Exception {
    //no configuration but system properties
    System.setProperty(PropertyManager.EXO_PROPERTY_SYSTEM_PREFIX + PropertyManager.PROPERTY_SERVER_HOST, "127.0.0.1");
    System.setProperty(PropertyManager.PROPERTY_SYSTEM_PREFIX + PropertyManager.PROPERTY_SERVER_PORT, "27776");

    List<ServerAddress> actual = ConnectionHelper.getMongoServerAdresses();
    assertEquals(1, actual.size());
    assertEquals(27776, actual.get(0).getPort());
    assertEquals("127.0.0.1", actual.get(0).getHost());
  }

  @Test
  public void testServerHostsExoSystemProperties() throws Exception {
    //no configuration but system properties with exo prefix
    System.setProperty(PropertyManager.EXO_PROPERTY_SYSTEM_PREFIX + PropertyManager.PROPERTY_SERVERS_HOSTS, "localhost:4445,localhost:4449");

    List<ServerAddress> expectedReplicatSet = new ArrayList<ServerAddress>();
    expectedReplicatSet.add(new ServerAddress("localhost", 4445));
    expectedReplicatSet.add(new ServerAddress("localhost", 4449));
    List<ServerAddress> actualReplicatSet = ConnectionHelper.getMongoServerAdresses();
    assertEquals(expectedReplicatSet.size(), actualReplicatSet.size());
    assertEquals(expectedReplicatSet, actualReplicatSet);
  }

  @Test
  public void testServerHostsSystemProperties() throws Exception {
    System.setProperty(PropertyManager.PROPERTY_SYSTEM_PREFIX + PropertyManager.PROPERTY_SERVERS_HOSTS, "127.0.0.1:4442,127.0.0.1:4443");

    List<ServerAddress> expectedReplicatSet = new ArrayList<ServerAddress>();
    expectedReplicatSet.add(new ServerAddress("127.0.0.1", 4442));
    expectedReplicatSet.add(new ServerAddress("127.0.0.1", 4443));
    List<ServerAddress> actualReplicatSet = ConnectionHelper.getMongoServerAdresses();
    assertEquals(expectedReplicatSet.size(), actualReplicatSet.size());
    assertEquals(expectedReplicatSet, actualReplicatSet);
  }

  @Test
  public void dbServerHostTest() throws Exception {
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVER_PORT, "27775");
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVER_HOST, "localhost");
    PropertyManager.overrideProperty(PropertyManager.PROPERTY_SERVERS_HOSTS, "");
    List<ServerAddress> actual = ConnectionHelper.getMongoServerAdresses();
    assertEquals(1, actual.size());
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
