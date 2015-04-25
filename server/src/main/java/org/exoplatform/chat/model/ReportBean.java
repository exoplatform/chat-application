package org.exoplatform.chat.model;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.exoplatform.chat.services.UserService;

import java.util.*;

public class ReportBean {
  HashMap<String, UserBean> attendees;
  List<String> questions;
  List<String> links;
  List<MessageBean> messages;
  List<FileBean> files;
  List<TaskBean> tasks;
  List<EventBean> events;

  public ReportBean() {
    attendees = new HashMap<String, UserBean>();
    questions = new ArrayList<String>();
    links = new ArrayList<String>();
    messages = new ArrayList<MessageBean>();
    files = new ArrayList<FileBean>();
    tasks = new ArrayList<TaskBean>();
    events = new ArrayList<EventBean>();
  }

  public Collection<UserBean> getAttendees() {
    return attendees.values();
  }

  public void addUser(UserBean attendee) {
    if (!attendees.containsKey(attendee.getName()))
    {
      attendee.setStatus(UserService.STATUS_OFFLINE);
      this.attendees.put(attendee.getName(), attendee);
    }
  }

  public void addAttendee(String user) {
    if (attendees.containsKey(user))
      this.attendees.get(user).setStatus(UserService.STATUS_AVAILABLE);
  }

  public List<String> getQuestions() {
    return questions;
  }

  public void addQuestion(String question) {
    this.questions.add(question);
  }

  public List<String> getLinks() {
    return links;
  }

  public void addLink(String link) {
    this.links.add(link);
  }

  public List<MessageBean> getMessages() {
    return messages;
  }

  public void addMessage(MessageBean message) {
    this.messages.add(0, message);
  }

  public List<FileBean> getFiles() {
    return files;
  }

  public void addFile(FileBean file) {
    this.files.add(file);
  }

  public List<TaskBean> getTasks() {
    return tasks;
  }

  public void addTask(TaskBean task) {
    this.tasks.add(task);
  }

  public List<EventBean> getEvents() {
    return events;
  }

  public void addEvent(EventBean event) {
    this.events.add(event);
  }

  public void fill(BasicDBList messages, List<UserBean> users)
  {
    for (UserBean user:users)
    {
      addUser(user);
    }

    Iterator iterator = messages.iterator();
    while(iterator.hasNext())
    {
      BasicDBObject message = (BasicDBObject)iterator.next();
      String msg = message.get("message").toString();
      Long timestamp = (Long)message.get("timestamp");
      String user = message.get("user").toString();
      String fullname = message.get("fullname").toString();
      String email = message.get("email").toString();
      String date = message.get("date").toString();
      boolean isSystem = false;
      if (message.containsField("isSystem"))
        isSystem = "true".equals(message.get("isSystem").toString());
      BasicDBObject options = null;
      if (isSystem && message.containsField("options")) {
        options = (BasicDBObject)message.get("options");
        if (options.containsField("type")) {
          if ("type-link".equals(options.get("type").toString()))
          {
            addLink(options.get("link").toString());
            msg = "[ Link: "+ options.get("link").toString()+" ]";
          }
          else if ("type-question".equals(options.get("type").toString()))
          {
            addQuestion(msg);
            msg = "[ Question: "+ msg +" ]";
          }
          else if ("type-file".equals(options.get("type").toString()))
          {
            FileBean file = new FileBean();
            file.setName(options.get("name").toString());
            file.setUrl(options.get("restPath").toString());
            file.setSize(options.get("sizeLabel").toString());
            addFile(file);
            msg = "[ File: "+ options.get("name").toString()+" ]";
          }
          else if ("type-hand".equals(options.get("type").toString()))
          {
            msg = "[ Raised the hand: " + msg + "]";
          }
          else if ("type-task".equals(options.get("type").toString()))
          {
            TaskBean task = new TaskBean();
            task.setAssignee(options.get("fullname").toString());
            task.setTask(options.get("task").toString());
            task.setDueDate(options.get("dueDate").toString());
            task.setUsername(options.get("username").toString());
            addTask(task);
            msg = "[ Task \""+options.get("task").toString()+"\" assigned to "+options.get("fullname").toString()+" - due "+options.get("dueDate").toString()+" ]";
          }
          else if ("type-event".equals(options.get("type").toString()))
          {
            EventBean event = new EventBean();
            event.setSummary(options.get("summary").toString());
            event.setStartDate(options.get("startDate").toString());
            event.setStartTime(options.get("startTime").toString());
            event.setEndDate(options.get("endDate").toString());
            event.setEndTime(options.get("endTime").toString());
            addEvent(event);
            msg = "[ Event \""+options.get("summary").toString()+"\" from "+options.get("startDate").toString()+" "+options.get("startTime").toString()+" to "+options.get("endDate").toString()+" "+options.get("endTime").toString()+" ]";
          }
          else if ("call-join".equals(options.get("type").toString()))
          {
            msg = "[ Joined meeting ]";
          }
          else if ("call-on".equals(options.get("type").toString()))
          {
            msg = "[ Meeting started ]";
          }
          else if ("call-off".equals(options.get("type").toString()))
          {
            msg = "[ Meeting finished ]";
          }
        }

      }
      addAttendee(user);
      MessageBean messageBean = new MessageBean();
      messageBean.setDate(date);
      messageBean.setFullname(fullname);
      messageBean.setUser(user);
      messageBean.setMessage(msg);
      addMessage(messageBean);


      // timestamp, user, fullname, email, date, message, options, type, isSystem
    }
  }

  public String getAsXWiki(String serverBase)
  {
    StringBuilder xwiki = new StringBuilder();

    xwiki.append("\n{{section}}\n{{column}}\n");

    /**
     * Questions
     */
    if (questions.size()>0) {
      xwiki.append("\n=== Questions ===\n");
      for (String question:this.getQuestions())
      {
        xwiki.append(question).append("\n");
      }
    }

    /**
     * Links
     */
    if (links.size()>0) {
      xwiki.append("\n=== Links ===\n");
      for (String link:this.getLinks())
      {
        xwiki.append("[[").append(link).append("]]").append("\n");
      }
    }

    /**
     * Files
     */
    if (files.size()>0) {
      xwiki.append("\n=== Files ===\n");
      for (FileBean file:this.getFiles())
      {
        xwiki.append("[[");
        xwiki.append(file.getName()).append(">>").append(file.getUrl().replaceFirst("/rest", serverBase+"/rest/private")).append("]]");
        xwiki.append(" (").append(file.getSize()).append(")\n");
      }
    }

    /**
     * Tasks
     */
    if (tasks.size()>0) {
      xwiki.append("\n=== Tasks ===\n");
      xwiki.append("|= Task |= Assignee |= Due\n");
      for (TaskBean task:this.getTasks())
      {
        xwiki.append("| ").append(task.getTask())
                .append(" | ").append(task.getAssignee())
                .append(" | ").append(task.getDueDate()).append(" \n");
      }
    }

    /**
     * Events
     */
    if (events.size()>0) {
      xwiki.append("\n=== Events ===\n");
      xwiki.append("|= Event |= Start |= End\n");
      for (EventBean event:this.getEvents())
      {
        xwiki.append("| ").append(event.getSummary())
                .append(" | ").append(event.getStartDate()).append(" ").append(event.getStartTime())
                .append(" | ").append(event.getEndDate()).append(" ").append(event.getEndTime()).append(" \n");
      }
    }

    xwiki.append("\n{{/column}}\n");
    /**
     * Attendees
     */
    xwiki.append("\n{{column}}\n=== Attendees ===\n{{panel}}\n");
    for (UserBean userBean:this.getAttendees()) {
      if ("available".equals(userBean.getStatus()))
        xwiki.append("(/) ");
      else
        xwiki.append("(x)");
      xwiki.append(" [[").append(userBean.getFullname()).append(">>"+serverBase+"/portal/intranet/profile/").append(userBean.getName()).append("]]\n");
    }
    xwiki.append("\n{{/panel}}\n{{/column}}\n{{/section}}\n");

    /**
     * Discussions
     */
    xwiki.append("\n=== Discussions ===\n\n");
    String prevUser = "";
    for (MessageBean messageBean:this.getMessages())
    {
      xwiki.append("{{span style='padding: 4px; border-bottom: 1px dotted #DDD; width: 500px;display: block;'}}");
      if (!messageBean.getUser().equals(prevUser))
      {
        xwiki.append("{{div style='padding: 4px;color: #CCC;margin:0;'}}");
        xwiki.append("{{span style='float: left; display: inline-block;padding-right: 10px;'}} [[image:"+serverBase+"/rest/chat/api/1.0/user/getAvatarURL/"+messageBean.getUser()+"||width='30' height='30']] {{/span}}");
        xwiki.append("{{span style='width: 400px;display: inline-block;vertical-align: top;'}}").append(messageBean.getFullname()).append("{{/span}}");
        xwiki.append("{{span style='font-size: smaller;vertical-align: top;'}}").append(messageBean.getDate()).append("{{/span}}");
        xwiki.append("{{/div}}");
      }
      prevUser = messageBean.getUser();
      xwiki.append("{{div style='padding: 0 4px; margin:0 0 0 40px;vertical-align: top;'}}").append(messageBean.getMessage()).append("{{/div}}");
      xwiki.append("{{/span}}");
    }


    return xwiki.toString();
  }

  public String getAsHtml(String title)
  {
    StringBuilder html = new StringBuilder();
    html.append("<div style='font-family: Lucida,arial,tahoma;'>");

    html.append("<style>" +
            ".type-file {" +
            "background: url('http://demo.exoplatform.net/chat/img/general.png') no-repeat -16px -33px;" +
            "background-size: 48px 81px;" +
            "width: 16px;" +
            "height: 16px;}" +
            ".type-link {" +
            "background: url('http://demo.exoplatform.net/chat/img/general.png') no-repeat 0px -33px;" +
            "background-size: 48px 81px;" +
            "width: 16px;" +
            "height: 16px;}" +
            ".type-question {" +
            "background: url('http://demo.exoplatform.net/chat/img/general.png') no-repeat -32px -49px;" +
            "background-size: 48px 81px;" +
            "width: 16px;" +
            "height: 16px;}" +
            "</style>");
    /**
     * Attendees
     */
    html.append("<span style='float:right; border: 1px solid #CCC;'>");
    html.append("  <div style='font-weight: bold;border-bottom: 1px solid #CCC;padding: 4px;font-size: larger'>Attendees</div>");
    for (UserBean userBean:this.getAttendees())
    {
      html.append("<div style='padding: 4px;'>");
      if ("available".equals(userBean.getStatus()))
        html.append("<span style='background-color: #3C3;color: white;padding: 1px 5px;'>o</span>");
      else
        html.append("<span style='background-color: #CCC;color: white;padding: 1px 5px;'>x</span>");
      html.append("  <span style='font-weight:bold;padding: 5px;'>").append(userBean.getFullname()).append("</span>");
      html.append("</div>");

    }
    html.append("</span>");

    /**
     * Meeting Notes Title
     */
    html.append("<h2>").append(title).append("</h2>");
    /**
     * Questions
     */
   if(questions.size() > 0) {
    html.append("<span style='border: 1px solid #CCC;width: 300px; display: inline-block;'>");
    html.append("  <div style='font-weight: bold;border-bottom: 1px solid #CCC;padding: 4px;font-size: larger'>");
    html.append("    <img class='type-question' src='http://demo.exoplatform.net/chat/img/empty.png' width='16px' style='width:16px;'>");
    html.append("    <span style='vertical-align: top;line-height: 18px;'>Questions</span>");
    html.append("  </div>");
    for (String question:this.getQuestions())
    {
      html.append("<div style='padding: 4px'>").append(question).append("</div>");
    }
    html.append("</span><br><br>");
   }

    /**
     * Links
     */
   if(links.size()>0) {
    html.append("<span style='border: 1px solid #CCC;width: 300px; display: inline-block;'>");
    html.append("  <div style='font-weight: bold;border-bottom: 1px solid #CCC;padding: 4px;font-size: larger'>");
    html.append("    <img class='type-link' src='http://demo.exoplatform.net/chat/img/empty.png' width='16px' style='width:16px;'>");
    html.append("    <span style='vertical-align: top;line-height: 18px;'>Links</span>");
    html.append("  </div>");
    for (String link:this.getLinks())
    {
      html.append("<div style='padding: 4px'>");
      html.append("<span><a href='"+link+"'>").append(link).append("</a></span>");
      html.append("</div>");
    }
    html.append("</span><br><br>");
   }

    /**
     * Files
     */
   if(files.size()>0) {
    html.append("<span style='border: 1px solid #CCC;width: 300px; display: inline-block;'>");
    html.append("  <div style='font-weight: bold;border-bottom: 1px solid #CCC;padding: 4px;font-size: larger'>");
    html.append("    <img class='type-file' src='http://demo.exoplatform.net/chat/img/empty.png' width='16px' style='width:16px;'>");
    html.append("    <span style='vertical-align: top;line-height: 18px;'>Files</span>");
    html.append("  </div>");
    for (FileBean file:this.getFiles())
    {
      html.append("<div style='padding: 4px'>");
      html.append("  <div><a href='http://demo.exoplatform.net"+file.getUrl().replaceFirst("/rest", "/rest/private")+"'>").append(file.getName()).append("</a></div>");
      html.append("  <div style='color: #ccc;'>").append(file.getSize()).append("</div>");
      html.append("</div>");
    }
    html.append("</span><br><br>");
   }

    /**
     * Discussions
     */
    html.append("<h3>Discussions</h3>");
    html.append("<span>");
    String prevUser = "";
    for (MessageBean messageBean:this.getMessages())
    {
      html.append("<span style='padding: 4px; border-bottom: 1px dotted #DDD; width: 500px;display: block;'>");
      if (!messageBean.getUser().equals(prevUser))
      {
        html.append("  <div style='padding: 4px;color: #CCC;'>");
        html.append("    <span style='float: left; display: inline-block;padding-right: 10px;'><img src='http://demo.exoplatform.net:8080/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:"+messageBean.getUser()+"/soc:profile/soc:avatar' width='30px' style='width:30px;'></span>");
        html.append("    <span style='width: 300px;display: inline-block;vertical-align: top;'>").append(messageBean.getFullname()).append("</span>");
        html.append("    <span style='font-size: smaller;vertical-align: top;'>").append(messageBean.getDate()).append("</span>");
        html.append("  </div>");
      }
      prevUser = messageBean.getUser();
      html.append("  <div style='padding: 0 4px; margin-left: 40px;vertical-align: top;'>").append(messageBean.getMessage()).append("</div>");
      html.append("</span>");
    }
    html.append("</span>");

    /**
     * closing
     */
    html.append("</div>");

    return html.toString();
  }

}
