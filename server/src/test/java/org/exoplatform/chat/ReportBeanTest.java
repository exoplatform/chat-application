package org.exoplatform.chat;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.exoplatform.chat.model.MessageBean;
import org.exoplatform.chat.model.ReportBean;
import org.exoplatform.chat.model.UserBean;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ReportBeanTest {

  public static ResourceBundle getResourceBundle() {
    return new ListResourceBundle() {

      @Override
      protected Object[][] getContents() {
        return contents;
      }

      private Object[][] contents = {
        { "exoplatform.chat.meetingnotes.task", "Task {0} assigned to {1} - Due date : {2}" }
      };
    };

  }

  @Test
  public void shouldReportTaskWithAllFields() {
    // Given
    ReportBean reportBean = new ReportBean(getResourceBundle());

    BasicDBList messages = new BasicDBList();
    long now = Instant.now().getEpochSecond();
    String tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).toString();
    BasicDBObject options = new BasicDBObject()
            .append("type", "type-task")
            .append("task", "Task 1")
            .append("username", "john")
            .append("dueDate", tomorrow);
    BasicDBObject message = new BasicDBObject()
            .append("msg", "Message test")
            .append("timestamp", now)
            .append("user", "john")
            .append("fullname", "John Smith")
            .append("options", options)
            .append("isSystem", "true");
    messages.add(message);

    // When
    reportBean.fill(messages, Arrays.asList(new UserBean()));

    // Then
    List<MessageBean> reportMessages = reportBean.getMessages();
    assertNotNull(reportMessages);
    assertEquals(1, reportMessages.size());
    MessageBean messageBean = reportMessages.get(0);
    assertEquals("john", messageBean.getUser());
    assertEquals(now, messageBean.getTimestamp());
    assertEquals("[ Task Task 1 assigned to john - Due date : " + tomorrow + " ]", messageBean.getMessage());
  }

  @Test
  public void shouldReportTaskWhenDoesNotContainAllFields() {
    // Given
    ReportBean reportBean = new ReportBean(getResourceBundle());

    BasicDBList messages = new BasicDBList();
    long now = Instant.now().getEpochSecond();
    BasicDBObject options = new BasicDBObject()
            .append("type", "type-task")
            .append("task", "Task 1");
    BasicDBObject message = new BasicDBObject()
            .append("msg", "Message test")
            .append("timestamp", now)
            .append("user", "john")
            .append("fullname", "John Smith")
            .append("options", options)
            .append("isSystem", "true");
    messages.add(message);

    // When
    reportBean.fill(messages, Arrays.asList(new UserBean()));

    // Then
    List<MessageBean> reportMessages = reportBean.getMessages();
    assertNotNull(reportMessages);
    assertEquals(1, reportMessages.size());
    MessageBean messageBean = reportMessages.get(0);
    assertEquals("john", messageBean.getUser());
    assertEquals(now, messageBean.getTimestamp());
    assertEquals("[ Task Task 1 assigned to  - Due date :  ]", messageBean.getMessage());
  }
}
