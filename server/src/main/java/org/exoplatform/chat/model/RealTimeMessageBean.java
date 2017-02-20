package org.exoplatform.chat.model;

import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Real Time Message
 */
public class RealTimeMessageBean {
  public enum EventType {
    MESSAGE_SENT("message-sent"),
    MESSAGE_UPDATED("message-updated"),
    MESSAGE_DELETED("message-deleted"),
    MESSAGE_READ("message-read"),
    USER_STATUS_CHANGED("user-status-changed"),
    ROOM_MEMBER_JOIN("room-member-join"),
    ROOM_MEMBER_LEFT("room-member-left"),
    ROOM_DELETED("room-deleted"),
    FAVOTITE_ADDED("favorite-added"),
    FAVORITE_REMOVED("favorite-removed");

    private final String eventType;

    // Reverse-lookup map for getting a EventType from its value
    private static Map<String, EventType> lookup = null;

    /**
     * @param eventType
     */
    EventType(final String eventType) {
      this.eventType = eventType;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
      return eventType;
    }

    public static EventType get(String value) {
      if(lookup == null) {
        lookup = Arrays.stream(EventType.values()).collect(Collectors.toMap(e -> e.toString(), e -> e));
      }
      return lookup.get(value);
    }
  }

  private EventType event;

  private String room;

  private String sender;

  private Date timestamp;

  private Map<String, Object> data;

  public RealTimeMessageBean() {
  }

  public RealTimeMessageBean(EventType event, String room, String sender, Date timestamp, Map<String, Object> data) {
    this.event = event;
    this.room = room;
    this.sender = sender;
    this.timestamp = timestamp;
    this.data = data;
  }

  public EventType getEvent() {
    return event;
  }

  public void setEvent(EventType event) {
    this.event = event;
  }

  public String getRoom() {
    return room;
  }

  public void setRoom(String room) {
    this.room = room;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
  }

  public String toJSON() {
    JSONObject message = new JSONObject();
    message.put("event", event.toString());
    message.put("room", room);
    message.put("sender", sender);
    Date ts = timestamp;
    if(ts == null) {
      ts = new Date();
    }
    message.put("ts", ts.getTime());
    if(data != null) {
      message.put("data", new JSONObject(data));
    }

    return message.toJSONString();
  }
}
