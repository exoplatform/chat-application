package org.benjp.services.jcr;

import org.benjp.model.SpaceBean;
import org.benjp.model.UserBean;
import org.benjp.services.UserService;

import java.util.List;

public class UserServiceImpl extends AbstractJCRService implements UserService
{
  public void toggleFavorite(String user, String targetUser) {
  }

  public boolean isFavorite(String user, String targetUser) {
    return false;
  }

  public void addUserFullName(String user, String fullname) {
  }

  public void addUserEmail(String user, String email) {
  }

  public void setSpaces(String user, List<SpaceBean> spaces) {
  }

  public List<SpaceBean> getSpaces(String user) {
    return null;
  }

  public List<UserBean> getUsers(String spaceId) {
    return null;
  }

  public String setStatus(String user, String status) {
    return null;
  }

  public void setAsAdmin(String user, boolean isAdmin) {
  }

  public boolean isAdmin(String user) {
    return false;
  }

  public String getStatus(String user) {
    return null;
  }

  public String getUserFullName(String user) {
    return null;
  }

  public UserBean getUser(String user) {
    return null;
  }

  public UserBean getUser(String user, boolean withFavorites) {
    return null;
  }

  public List<String> getUsersFilterBy(String user, String space) {
    return null;
  }

  public int getNumberOfUsers() {
    return 0;
  }
}
