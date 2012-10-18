package org.benjp.services;

import com.mongodb.Mongo;
import com.mongodb.MongoOptions;
import com.mongodb.WriteConcern;

import java.net.UnknownHostException;

public class MongoBootstrap {
  private static Mongo m;

  public static Mongo mongo()
  {
    if (m==null)
    {
      try {
        MongoOptions options = new MongoOptions();
        options.connectionsPerHost = 50;
        options.connectTimeout = 60000;
        options.threadsAllowedToBlockForConnectionMultiplier = 10;
        m = new Mongo("localhost", options);
        m.setWriteConcern(WriteConcern.SAFE);
      } catch (UnknownHostException e) {
      }
    }
    return m;
  }
}
