package org.benjp.services;

/**
 * Created with IntelliJ IDEA.
 * User: benjamin
 * Date: 11/10/12
 * Time: 16:48
 * To change this template use File | Settings | File Templates.
 */
public class NotificationBean {
  private String user, type, info;
  private Long timestamp;

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getInfo() {
    return info;
  }

  public void setInfo(String info) {
    this.info = info;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }
}
