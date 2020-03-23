package org.exoplatform.chat.bean;

import org.apache.commons.lang3.StringEscapeUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class File implements Comparable<File> {
  String title;
  String name;
  String owner;
  Calendar createdDate;
  String sizeLabel;
  Long size;
  String path;
  String uuid="";
  String publicUrl;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getCreatedDateAsDate() {
    return createdDate.getTime();
  }

  public String getCreatedDate() {
    SimpleDateFormat formatter = new SimpleDateFormat("d/M/yy hh:mm aaa");
    return formatter.format(createdDate.getTime());
  }

  public void setCreatedDate(Calendar createdDate) {
    this.createdDate = createdDate;
  }

  public String getSizeLabel() {
    return sizeLabel;
  }

  public void setSizeLabel(String sizeLabel) {
    this.sizeLabel = sizeLabel;
  }

  public String getRestPath() {
    return "/portal/rest/jcr/repository/collaboration" + path;
  }

  public String getDownloadLink() {
    return "/portal/rest/private/contents/download/collaboration" + path;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getPublicUrl() {
    return publicUrl;
  }

  public void setPublicUrl(String publicUrl) {
    this.publicUrl = publicUrl;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public String getOwner() {
    if (owner==null || "".equals(owner)) return "Someone";
    return owner.substring(0, 1).toUpperCase() + owner.substring(1);
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public int compareTo(File file) {
    return this.getName().compareToIgnoreCase(file.getName());
  }

  public String toJSON()
  {
    StringBuffer sb = new StringBuffer();

    sb.append("{");

      sb.append("\"status\": \"ok\",");
      sb.append("\"name\": \""+this.getName()+"\",");
      sb.append("\"title\": \""+ StringEscapeUtils.escapeJson(StringEscapeUtils.escapeHtml4(this.getTitle()))+"\",");
      sb.append("\"size\": "+this.getSize()+",");
      sb.append("\"owner\": \""+this.getOwner()+"\",");
      sb.append("\"createdDate\": \""+this.getCreatedDate()+"\",");
      sb.append("\"restPath\": \""+this.getRestPath()+"\",");
      sb.append("\"downloadLink\": \""+this.getDownloadLink()+"\",");
      sb.append("\"uuid\": \""+this.getUuid()+"\",");
      sb.append("\"path\": \""+this.getPath()+"\",");
      sb.append("\"publicUrl\": \""+this.getPublicUrl()+"\",");
      sb.append("\"sizeLabel\": \""+this.getSizeLabel()+"\"");

    sb.append("}");

    return sb.toString();
  }

}
