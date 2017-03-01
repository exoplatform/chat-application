var desktopNotification = (function() {
  ROOM_NOTIF_TRIGGER_NORMAL = "normal";
  ROOM_NOTIF_TRIGGER_SILENCE = "silence";
  ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD = "keywords";
  ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD_VALUE = "room-notif-trigger-when-key-word-value";
  NOTIFY_EVEN_NOT_DISTRUB = "notify-even-not-distrub";
  NOTIFY_WHEN_MENTION = "notify-when-mention";
  ROOM_ON_SITE = "on-site";
  ROOM_DESKTOP = "desktop";
  ROOM_BIP = "bip";
  ROOM_TYPE_MEETING_STOP = "type-meeting-stop";
  ROOM_TYPE_MEETING_START = "type-meeting-start";
  ROOM_TYPE_LINK = "type-link";
  ROOM_TYPE_HAND = "type-hand";

  var preferredNotification = [];
  var preferredNotificationTrigger = [];
  var preferredRoomNotificationTrigger = {};

  var setPreferredNotification = function(prefNotifs) {
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
  }

  var setPreferredNotificationTrigger = function(prefNotifTrigger) {
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
  }

  var setRoomPreferredNotificationTrigger = function(roomId, value) {
    preferredRoomNotificationTrigger[roomId] = value;
  }

  var setRoomPreferredNotificationTriggerSettings = function(settings) {
    for (var roomId in settings) {
      var notifData = settings[roomId];
      var notifCond = notifData["notifCond"];
      if(ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD === notifCond) {
        notifCond+=":"+notifData[notifCond];
      }
      setRoomPreferredNotificationTrigger(roomId, notifCond);

    }
  }

  var getPreferredNotification = function() {
    return preferredNotification;
  }

  var getPreferredNotificationTrigger = function() {
    return preferredNotificationTrigger;
  }

  var getRoomPreferredNotificationTrigger = function() {
    return preferredRoomNotificationTrigger;
  }

  var setPreferredNotificationSettings = function(settings,overrideSettin) { //this is always called on the reload of the page
    if (!(settings.preferredNotification === null || typeof settings.preferredNotification === 'undefined') && overrideSettin)
      setPreferredNotification(JSON.parse(settings.preferredNotification));
    if (!(settings.preferredNotificationTrigger === null || typeof settings.preferredNotificationTrigger === 'undefined') && overrideSettin)
      setPreferredNotificationTrigger(JSON.parse(settings.preferredNotificationTrigger));
    if (!(settings.preferredRoomNotificationTrigger === null || typeof settings.preferredRoomNotificationTrigger === 'undefined'))
      setRoomPreferredNotificationTriggerSettings(JSON.parse(settings.preferredRoomNotificationTrigger));
  }
  var canBypassDonotDistrub = function() {
    return (preferredNotificationTrigger.indexOf(NOTIFY_EVEN_NOT_DISTRUB) !== -1);
  }


  var canPlaySound = function() {
    return (preferredNotification.indexOf(ROOM_BIP) !== -1);
  }

  var canShowDesktopNotif = function() {
    return (preferredNotification.indexOf(ROOM_DESKTOP) !== -1);
  }

  var canShowOnSiteNotif = function() {
    return (preferredNotification.indexOf(ROOM_ON_SITE) !== -1);
  }

  var canBypassRoomNotif = function(msgObj) {
    var message = msgObj.content;
    var sourceRoom = msgObj.categoryId;
    return (!preferredRoomNotificationTrigger[sourceRoom]) || // 1-1 chat, but room
      (preferredRoomNotificationTrigger[sourceRoom] == ROOM_NOTIF_TRIGGER_NORMAL) ||
      (preferredRoomNotificationTrigger[sourceRoom].startsWith(ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD) &&
        containKeyWord(message, preferredRoomNotificationTrigger[sourceRoom]));
  }

  var containKeyWord = function(message, keywords) {
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
  }

  var highlightMessage = function(msgObject) {
    var highlightedMsg = "";
    switch (msgObject.options.type) {
      case ROOM_TYPE_HAND:
        highlightedMsg = "raises hand: " + msgObject.content;
        break;
      case ROOM_TYPE_LINK:
        highlightedMsg = msgObject.options.link;
        break;
      case ROOM_TYPE_MEETING_START:
        highlightedMsg = "Start Meeting";
        break;
      case ROOM_TYPE_MEETING_STOP:
        highlightedMsg = "End Meeting";
        break;
      default:
        highlightedMsg = msgObject.content;
    }
    return highlightedMsg;
  }

  return {
    getPreferredNotification: getPreferredNotification,
    setPreferredNotification: setPreferredNotification,
    getPreferredNotificationTrigger: getPreferredNotificationTrigger,
    setPreferredNotificationTrigger: setPreferredNotificationTrigger,
    getRoomPreferredNotificationTrigger: getRoomPreferredNotificationTrigger,
    setRoomPreferredNotificationTrigger: setRoomPreferredNotificationTrigger,
    setPreferredNotificationSettings: setPreferredNotificationSettings,
    canBypassDonotDistrub: canBypassDonotDistrub,
    canPlaySound: canPlaySound,
    canShowDesktopNotif: canShowDesktopNotif,
    canShowOnSiteNotif: canShowOnSiteNotif,
    canBypassRoomNotif: canBypassRoomNotif,
    highlightMessage: highlightMessage,
    ROOM_NOTIF_TRIGGER_NORMAL: ROOM_NOTIF_TRIGGER_NORMAL,
    ROOM_NOTIF_TRIGGER_SILENCE: ROOM_NOTIF_TRIGGER_SILENCE,
    ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD: ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD,
    ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD_VALUE: ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD_VALUE,
    ROOM_ON_SITE: ROOM_ON_SITE,
    ROOM_DESKTOP: ROOM_DESKTOP,
    ROOM_BIP : ROOM_BIP
  };

})()
