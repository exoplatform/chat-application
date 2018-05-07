(function() {
  var preferredNotification = [];
  var preferredNotificationTrigger = [];
  var preferredRoomNotificationTrigger = {};

  return {
    ROOM_NOTIF_TRIGGER_NORMAL: "normal",
    ROOM_NOTIF_TRIGGER_SILENCE: "silence",
    ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD: "keywords",
    ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD_VALUE: "room-notif-trigger-when-key-word-value",
    ROOM_ON_SITE: "on-site",
    ROOM_DESKTOP: "desktop",
    ROOM_BIP : "bip",

    containKeyWord: function(message, keywords) {
      message = message.toLowerCase();
      var keyW = keywords.split(":")[1];
      if(keyW==="") {
        return false;
      }
      var keys = keyW.split(",");
      for (var i = 0; i < keys.length; i++) {
        if (message.includes(keys[i].trim().toLowerCase())) {
          return true;
        }
      }
      return false;
    },

    getPreferredNotification: function() {
      return preferredNotification;
    },

    setPreferredNotification: function(prefNotifs) {
      if (!(prefNotifs instanceof Array)) { // always force data to be wrapped into an array
        prefNotifs = [prefNotifs];
      }
      prefNotifs.forEach(function(prefNotif) {
        var index = preferredNotification.indexOf(prefNotif);
        if (index == -1) {
          preferredNotification.push(prefNotif);
        } else { //if a preferred notification is already set then remove it
          preferredNotification.splice(index, 1);
        }
      });
    },

    getPreferredNotificationTrigger: function() {
      return preferredNotificationTrigger;
    },

    setPreferredNotificationTrigger: function(prefNotifTrigger) {
      if (!(prefNotifTrigger instanceof Array)) { // always force data to be wrapped into an array
        prefNotifTrigger = [prefNotifTrigger];
      }
      prefNotifTrigger.forEach(function(prefNotif) {
        var index = preferredNotificationTrigger.indexOf(prefNotif);
        if (index == -1) {
          preferredNotificationTrigger.push(prefNotif);
        } else { //if a preferred notification is already set then remove it
          preferredNotificationTrigger.splice(index, 1);
        }
      });
    },

    getRoomPreferredNotificationTrigger: function() {
      return preferredRoomNotificationTrigger;
    },

    setRoomPreferredNotificationTrigger: function(roomId, value) {
      preferredRoomNotificationTrigger[roomId] = value;
    },

    setRoomPreferredNotificationTriggerSettings: function(settings) {
      for (var roomId in settings) {
        var notifData = settings[roomId];
        var notifCond = notifData["notifCond"];
        if(this.ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD === notifCond) {
          notifCond+=":"+notifData[notifCond];
        }
        this.setRoomPreferredNotificationTrigger(roomId, notifCond);
      }
    },

    setPreferredNotificationSettings: function(settings, overrideSettin) { //this is always called on the reload of the page
      if (settings.preferredNotification && overrideSettin)
        this.setPreferredNotification(JSON.parse(settings.preferredNotification));
      if (settings.preferredNotificationTrigger && overrideSettin)
        this.setPreferredNotificationTrigger(JSON.parse(settings.preferredNotificationTrigger));
      if (settings.preferredRoomNotificationTrigger)
        this.setRoomPreferredNotificationTriggerSettings(JSON.parse(settings.preferredRoomNotificationTrigger));
    },

    canBypassDonotDistrub: function() {
      return (preferredNotificationTrigger.indexOf("notify-even-not-distrub") == -1);
    },

    canPlaySound: function() {
      return (preferredNotification.indexOf(this.ROOM_BIP) !== -1);
    },

    canShowDesktopNotif: function() {
      return (preferredNotification.indexOf(this.ROOM_DESKTOP) !== -1);
    },

    canShowOnSiteNotif: function() {
      return (preferredNotification.indexOf(this.ROOM_ON_SITE) !== -1);
    },

    canBypassRoomNotif: function(msgObj) {
      var message = msgObj.content;
      var sourceRoom = msgObj.categoryId;
      return (!preferredRoomNotificationTrigger[sourceRoom]) ||   // Not specified yet
        (preferredRoomNotificationTrigger[sourceRoom].startsWith(this.ROOM_NOTIF_TRIGGER_NORMAL)) ||   // Normal condition
        (preferredRoomNotificationTrigger[sourceRoom].startsWith(this.ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD) &&
        this.containKeyWord(message, preferredRoomNotificationTrigger[sourceRoom]));   // Containing keywords
    },

    highlightMessage: function(msgObject) {
      var highlightedMsg = msgObject.content;

      if (msgObject.options) {
        switch (msgObject.options.type) {
          case "type-hand":
            highlightedMsg = "raises hand: " + msgObject.content;
            break;
          case "type-link":
            highlightedMsg = msgObject.options.link;
            break;
          case "type-meeting-start":
            highlightedMsg = "Start Meeting";
            break;
          case "type-meeting-stop":
            highlightedMsg = "End Meeting";
            break;
        }
      }

      return highlightedMsg;
    }
  };
})();
