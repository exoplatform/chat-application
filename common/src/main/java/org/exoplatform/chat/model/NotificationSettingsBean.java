package org.exoplatform.chat.model;

import org.apache.commons.lang3.StringEscapeUtils;
import org.exoplatform.chat.services.UserService;
import org.json.JSONException;
import org.json.simple.JSONObject;

/**
 * Created by marwen on 8/26/16.
 */
/*
*  "notificationsSettings" : {
	"enabledTriggers": ["notify-even-not-disturb"],
	"enabledChannels": ["desktop", "on-site"],
	"rooms" : {
		"f223373a3614ab887e84b7be01713cb491dbbdda" : {
			"notificationMode" : "normal",
			"time" : NumberLong("1471937006368")
		}
	}
}

*
* */
public class NotificationSettingsBean
{
    private String  enabledTriggers;
    private String  enabledRoomTriggers;
    private String  enabledChannels;
    private String rooms;

    public NotificationSettingsBean() {
        this.enabledTriggers = null;
        this.enabledRoomTriggers = null;
        this.enabledChannels = null;
        this.rooms = null;
    }

    public String getEnabledRoomTriggers() {
        return enabledRoomTriggers;
    }

    public void setEnabledRoomTriggers(String enabledRoomTriggers) {
        this.enabledRoomTriggers = enabledRoomTriggers;
    }



    public String getEnabledChannels() {
        return enabledChannels;
    }

    public void setEnabledChannels(String enabledChannels) {
        this.enabledChannels = enabledChannels;
    }

    public String getRooms() {
        return rooms;
    }

    public void setRooms(String rooms) {
        this.rooms = rooms;
    }

    public String getEnabledTriggers() {
        return enabledTriggers;
    }

    public void setEnabledTriggers(String enabledTriggers) {
        this.enabledTriggers = enabledTriggers;
    }



    public JSONObject toJSON()
    {
      JSONObject obj = new JSONObject();
      if(this.getEnabledTriggers()!=null) {
        obj.put(UserService.PREFERRED_NOTIFICATION_TRIGGER, this.getEnabledTriggers());
      }
      if(this.getEnabledChannels()!=null) {
        obj.put(UserService.PREFERRED_NOTIFICATION, this.getEnabledChannels());
      }
      if(this.getEnabledRoomTriggers()!=null) {
        obj.put(UserService.PREFERRED_ROOM_NOTIFICATION_TRIGGER, this.getEnabledRoomTriggers());
      }

      return obj;
    }
}
