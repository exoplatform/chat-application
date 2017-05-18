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

package org.exoplatform.chat.model;


import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class NotificationBean {
  private String user;
  private String from;
  private String fromFullName = StringUtils.EMPTY;
  private String type;
  private String content;
  private String link;
  private String category;
  private String categoryId;
  private String options = StringUtils.EMPTY;
  private String roomDisplayName = StringUtils.EMPTY;
  private String roomType;
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

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }

  public String getOptions() {
    return options;
  }

  public void setOptions(String options) {
    this.options = options;
  }

  public String getRoomDisplayName() {
    return roomDisplayName;
  }

  public void setRoomType(String roomType) { this.roomType = roomType; }

  public String getRoomType() {
    return roomType;
  }

  public void setRoomDisplayName(String roomDisplayName) {
    this.roomDisplayName = roomDisplayName;
  }


  public String getFromFullName() {
    return fromFullName;
  }

  public void setFromFullName(String fromFullName) {
    this.fromFullName = fromFullName;
  }

  public JSONObject toJSONObject() {
    JSONObject obj = new JSONObject();
    obj.put("user", this.getUser());
    obj.put("type", this.getType());
    obj.put("from", this.getFrom());
    obj.put("fromFullName", this.getFromFullName());
    obj.put("category", this.getCategory());
    obj.put("categoryId", this.getCategoryId());
    obj.put("content", this.getContent().replaceAll("\n", "<br/>"));
    obj.put("link", this.getLink());

    String options = this.getOptions();
    if (StringUtils.isNotEmpty(this.getOptions())) {
      if (options.startsWith("{")) {
        JSONObject optionsJson = null;
        try {
          optionsJson = (JSONObject) new JSONParser().parse(options);
        } catch (ParseException e) {
        }
        obj.put("options", optionsJson);
      } else {
        obj.put("options", options);
      }
    } else {
      obj.put("options", StringUtils.EMPTY);
    }
    obj.put("roomDisplayName", StringEscapeUtils.escapeHtml4(this.getRoomDisplayName()));
    obj.put("roomType", this.getRoomType());
    obj.put("timestamp", this.getTimestamp());
    return obj;
  }
}
