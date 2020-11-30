package org.exoplatform.chat.model;

import org.exoplatform.chat.utils.MessageDigester;

import java.util.List;

public class UsersBean {

  List<UserBean> users;
  long size;

  public List<UserBean> getUsers() {
    return users;
  }

  public void setUsers(List<UserBean> users) {
    this.users = users;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public String usersToJSON()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"users\": [");
    boolean first=true;
    for (UserBean userBean:this.getUsers()) {
      if (!first) {
        sb.append(",");
      } else {
        first=false;
      }

      sb.append(userBean.toJSON());

    }
    sb.append("],");
    sb.append("\"md5\": \"").append(MessageDigester.getHash(sb.toString())).append("\",");
    sb.append("\"timestamp\": \"").append(System.currentTimeMillis()).append("\",");
    sb.append("\"totalSize\": \"").append(size).append("\"");
    sb.append("}");


    return sb.toString();
  }

}
