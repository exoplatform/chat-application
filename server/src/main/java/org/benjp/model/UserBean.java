/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.benjp.model;

import java.util.List;

public class UserBean
{
  private String name, fullname="", email="", status;
  private List<String> favorites;

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getFullname()
  {
    return fullname;
  }

  public void setFullname(String fullname)
  {
    this.fullname = fullname;
  }

  public String getEmail()
  {
    return email;
  }

  public void setEmail(String email)
  {
    this.email = email;
  }

  public String getStatus()
  {
    return status;
  }

  public void setStatus(String status)
  {
    this.status = status;
  }

  public List<String> getFavorites() {
    return favorites;
  }

  public void setFavorites(List<String> favorites) {
    this.favorites = favorites;
  }

  public boolean isFavorite(String user)
  {
    if (favorites!=null)
    {
      return (favorites.contains(user));
    }
    return false;
  }

  public String toJSON()
  {
    StringBuffer sb = new StringBuffer();

    sb.append("{");

    sb.append("\"name\": \""+this.getName()+"\",");
    sb.append("\"email\": \""+this.getEmail()+"\",");
    sb.append("\"status\": \""+this.getStatus()+"\",");
    sb.append("\"fullname\": \""+this.getFullname()+"\"");

    sb.append("}");

    return sb.toString();
  }

}
