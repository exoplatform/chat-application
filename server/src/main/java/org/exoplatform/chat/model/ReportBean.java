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

  ResourceBundle res;

  public ReportBean(ResourceBundle res) {
    attendees = new HashMap<String, UserBean>();
    questions = new ArrayList<String>();
    links = new ArrayList<String>();
    messages = new ArrayList<MessageBean>();
    files = new ArrayList<FileBean>();
    tasks = new ArrayList<TaskBean>();
    events = new ArrayList<EventBean>();

    this.res = res;
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
            msg = "[ "+ res.getString("exoplatform.chat.meetingnotes.link")+": "+ options.get("link").toString()+" ]";
          }
          else if ("type-question".equals(options.get("type").toString()))
          {
            addQuestion(msg);
            msg = "[ "+ res.getString("exoplatform.chat.meetingnotes.question")+": "+ msg +" ]";
          }
          else if ("type-file".equals(options.get("type").toString()))
          {
            FileBean file = new FileBean();
            file.setName(options.get("name").toString());
            file.setUrl(options.get("restPath").toString());
            file.setSize(options.get("sizeLabel").toString());
            addFile(file);
            msg = "[ "+ res.getString("exoplatform.chat.meetingnotes.file")+": "+ options.get("name").toString()+" ]";
          }
          else if ("type-hand".equals(options.get("type").toString()))
          {
            msg = "[ "+ res.getString("exoplatform.chat.meetingnotes.raiseHand")+": " + msg + "]";
          }
          else if ("type-task".equals(options.get("type").toString()))
          {
            TaskBean task = new TaskBean();
            task.setAssignee(options.get("fullname").toString());
            task.setTask(options.get("task").toString());
            task.setDueDate(options.get("dueDate").toString());
            task.setUsername(options.get("username").toString());
            addTask(task);
            msg = "[ "+ res.getString("exoplatform.chat.meetingnotes.task").
              replace("{0}", options.get("task").toString()).
              replace("{1}", options.get("fullname").toString()).
              replace("{2}", options.get("dueDate").toString())+
              " ]";
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
            msg = "[ "+res.getString("exoplatform.chat.meetingnotes.event").
              replace("{0}", options.get("summary").toString()).
              replace("{1}", options.get("startDate").toString()).
              replace("{2}", options.get("startTime").toString()).
              replace("{3}", options.get("endDate").toString()).
              replace("{4}", options.get("endTime").toString())+
              " ]";
          }
          else if ("call-join".equals(options.get("type").toString()))
          {
            msg = "[ "+ res.getString("exoplatform.chat.meetingnotes.joined")+" ]";
          }
          else if ("type-meeting-start".equals(options.get("type").toString()))
          {
            msg = "[ "+ res.getString("exoplatform.chat.meetingnotes.started")+" ]";
          }
          else if ("type-meeting-stop".equals(options.get("type").toString()))
          {
            msg = "[ "+ res.getString("exoplatform.chat.meetingnotes.finished")+" ]";
          }
        }

      }
      addAttendee(user);
      MessageBean messageBean = new MessageBean();
      messageBean.setTimestamp(timestamp);
      messageBean.setFullName(fullname);
      messageBean.setUser(user);
      messageBean.setMessage(msg);
      addMessage(messageBean);


      // timestamp, user, fullname, email, date, message, options, type, isSystem
    }
  }

  public String getAsXWiki(String serverBase, String portalURI)
  {
    StringBuilder xwiki = new StringBuilder();

    xwiki.append("\n{{section}}\n{{column}}\n");

    /**
     * Questions
     */
    if (questions.size()>0) {
      xwiki.append("\n=== "+ res.getString("exoplatform.chat.meetingnotes.questions")+" ===\n");
      for (String question:this.getQuestions())
      {
        xwiki.append(question).append("\n");
      }
    }

    /**
     * Links
     */
    if (links.size()>0) {
      xwiki.append("\n=== "+ res.getString("exoplatform.chat.meetingnotes.links")+" ===\n");
      for (String link:this.getLinks())
      {
        xwiki.append("[[").append(link).append("]]").append("\n");
      }
    }

    /**
     * Files
     */
    if (files.size()>0) {
      xwiki.append("\n=== "+ res.getString("exoplatform.chat.meetingnotes.files")+" ===\n");
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
      xwiki.append("\n=== "+ res.getString("exoplatform.chat.meetingnotes.tasks")+" ===\n");
      xwiki.append("|= "+ res.getString("exoplatform.chat.meetingnotes.tasks.task")+
        " |= "+ res.getString("exoplatform.chat.meetingnotes.tasks.assignee")+
        " |= "+ res.getString("exoplatform.chat.meetingnotes.tasks.due")+"\n");

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
      xwiki.append("\n=== "+ res.getString("exoplatform.chat.meetingnotes.events")+" ===\n");
      xwiki.append("|= "+ res.getString("exoplatform.chat.meetingnotes.events.event")+
        " |= "+ res.getString("exoplatform.chat.meetingnotes.events.start")+
        " |= "+ res.getString("exoplatform.chat.meetingnotes.events.end")+"\n");
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
    xwiki.append("\n{{column}}\n=== "+ res.getString("exoplatform.chat.meetingnotes.attendees")+" ===\n{{panel}}\n");
    for (UserBean userBean:this.getAttendees()) {
      if ("available".equals(userBean.getStatus()))
        xwiki.append("(/) ");
      else
        xwiki.append("(x)");
      xwiki.append(" [[").append(userBean.getFullname()).append(">>" + serverBase + portalURI + "/profile/").append(userBean.getName()).append("]]\n");
    }
    xwiki.append("\n{{/panel}}\n{{/column}}\n{{/section}}\n");

    /**
     * Discussions
     */
    xwiki.append("\n=== "+ res.getString("exoplatform.chat.meetingnotes.discussions")+" ===\n\n");
    String prevUser = "";
    for (MessageBean messageBean:this.getMessages())
    {
      xwiki.append("{{span style='padding: 4px; border-bottom: 1px dotted #DDD; width: 500px;display: block;'}}");
      if (!messageBean.getUser().equals(prevUser))
      {
        xwiki.append("{{div style='padding: 4px;color: #CCC;margin:0;'}}");
        xwiki.append("{{span style='float: left; display: inline-block;padding-right: 10px;'}} [[image:"+serverBase+"/rest/v1/social/users/"+messageBean.getUser()+"/avatar||width='30' height='30']] {{/span}}");
        xwiki.append("{{span style='width: 400px;display: inline-block;vertical-align: top;'}}").append(messageBean.getFullname()).append("{{/span}}");
        xwiki.append("{{span style='font-size: smaller;vertical-align: top;'}}").append(messageBean.getTimestamp()).append("{{/span}}");
        xwiki.append("{{/div}}");
      }
      prevUser = messageBean.getUser();
      xwiki.append("{{div style='padding: 0 4px; margin:0 0 0 40px;vertical-align: top;'}}").append(messageBean.getMessage()).append("{{/div}}");
      xwiki.append("{{/span}}");
    }


    return xwiki.toString();
  }

  public String getAsHtml(String title, String serverBase, Locale locale)
  {
    StringBuilder html = new StringBuilder();
    html.append("<div style='font-family: Lucida,arial,tahoma;'>");

    html.append("<style>" +
            ".type-file {" +
            "width: 16px;" +
            "height: 16px;}" +
            ".type-link {" +
            "width: 16px;" +
            "height: 16px;}" +
            ".type-question {" +
            "width: 16px;" +
            "height: 16px;}" +
            "</style>");
    /**
     * Attendees
     */
    html.append("<span style='float:right; border: 1px solid #CCC;'>");
    html.append("  <div style='font-weight: bold;border-bottom: 1px solid #CCC;padding: 4px;font-size: larger'>Attendees</div>");
    html.append("  <div style='font-weight: bold;border-bottom: 1px solid #CCC;padding: 4px;font-size: larger'>" + res.getString("exoplatform.chat.meetingnotes.attendees") + "</div>");
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
     html.append("    <span style='vertical-align: top;line-height: 18px;'>"+res.getString("exoplatform.chat.meetingnotes.questions")+"</span>");
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
     html.append("    <span style='vertical-align: top;line-height: 18px;'>"+res.getString("exoplatform.chat.meetingnotes.links")+"</span>");
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
    html.append("    <span style='vertical-align: top;line-height: 18px;'>"+res.getString("exoplatform.chat.meetingnotes.files")+"</span>");
    html.append("  </div>");
    for (FileBean file:this.getFiles())
    {
      html.append("<div style='padding: 4px'>");
      html.append("  <div><a href='"+serverBase+""+file.getUrl().replaceFirst("/rest", "/rest/private")+"'>").append(file.getName()).append("</a></div>");
      html.append("  <div style='color: #ccc;'>").append(file.getSize()).append("</div>");
      html.append("</div>");
    }
    html.append("</span><br><br>");
   }

    /**
     * Discussions
     */
    html.append("<h3>"+res.getString("exoplatform.chat.meetingnotes.discussions")+"</h3>");
    html.append("<span>");
    String prevUser = "";
    String keyAvatar = "";
    int index =0;
    for (MessageBean messageBean:this.getMessages())
    {
      html.append("<span style='padding: 4px; border-bottom: 1px dotted #DDD; width: 500px;display: block;'>");
      
      if (!messageBean.getUser().equals(prevUser))
      {
        keyAvatar = messageBean.getUser() + index;
        html.append("  <div style='padding: 4px;color: #CCC;'>");
        html.append("    <span style='float: left; display: inline-block;padding-right: 10px;'><img src=\"cid:"+keyAvatar+"\" width='30px' style='width:30px;'></span>");
        html.append("    <span style='width: 300px;display: inline-block;vertical-align: top;'>").append(messageBean.getFullName()).append("</span>");
        html.append("    <span style='font-size: smaller;vertical-align: top;'>").append(messageBean.getTimestamp()).append("</span>");
        html.append("  </div>");
        index ++;
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
