package org.exoplatform.chat.model;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MessageBean {
  private String id;
  private String user;
  private String fullName;
  private String message;
  private long timestamp;
  private long lastUpdatedTimestamp;
  private String options;
  private String type;
  private boolean isSystem;

  public String getId() {
    return id;
  }

  public long getLastUpdatedTimestamp() {
    return lastUpdatedTimestamp;
  }

  public String getOptions() {
    return options;
  }

  public String getType() {
    return type;
  }

  public boolean isSystem() {
    return isSystem;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setLastUpdatedTimestamp(long lastUpdatedTimestamp) {
    this.lastUpdatedTimestamp = lastUpdatedTimestamp;
  }

  public void setOptions(String options) {
    this.options = options;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setSystem(boolean system) {
    isSystem = system;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullname) {
    this.fullName = fullname;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String toJSONString() {
    JSONObject msg = new JSONObject();
    msg.put("id", id);
    msg.put("timestamp", timestamp);
    if (lastUpdatedTimestamp > 0) {
      msg.put("lastUpdatedTimestamp", lastUpdatedTimestamp);
    }
    msg.put("user", user);
    msg.put("fullname", fullName);
    msg.put("message", message);
    if (options != null)
    {
      JSONParser parser = new JSONParser();
      try {
        msg.put("options", (JSONObject) parser.parse(options));
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    else
    {
      msg.put("options", "");
    }
    msg.put("type", type);
    msg.put("isSystem", isSystem);

    return msg.toJSONString();
  }
}
