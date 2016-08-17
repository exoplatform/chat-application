var desktop = (function () {
    ROOM_NOTIF_TRIGGER_NORMAL = "normal";
    ROOM_NOTIF_TRIGGER_SILENCE = "silence";
    ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD = "when-key-word";
    ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD_VALUE = "room-notif-trigger-when-key-word-value";
    
	var preferredNotification = [];
	var preferredNotificationTrigger = [];
	var preferredRoomNotificationTrigger = {};

	var setPreferredNotification = function(prefNotifs) {
	if(!(prefNotifs instanceof Array)){ // always force data to be wrapped into an array
		prefNotifs =[prefNotifs];
	}
		prefNotifs.forEach(function(prefNotif) {
			var index = preferredNotification.indexOf(prefNotif);
			if(index ==-1) {
				preferredNotification.push(prefNotif);
			} else {//if a preferred notification is already set then remove it
				preferredNotification.splice(index, 1);
			}
		});
	}

	var setPreferredNotificationTrigger = function(prefNotifTrigger) {
		if(!(prefNotifTrigger instanceof Array)){ // always force data to be wrapped into an array
    		prefNotifTrigger =[prefNotifTrigger];
    	}
    		prefNotifTrigger.forEach(function(prefNotif){
    		  var index = preferredNotificationTrigger.indexOf(prefNotif);
			  if(index ==-1) {
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
	      setRoomPreferredNotificationTrigger(roomId, notifData["notifCond"]);
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

	var setPreferredNotificationSettings = function(settings) {//this is always called on the reload of the page
		if(!(settings.preferredNotification === null || typeof settings.preferredNotification === 'undefined'))
			setPreferredNotification(JSON.parse(settings.preferredNotification));
		if(!(settings.preferredNotificationTrigger === null || typeof settings.preferredNotificationTrigger === 'undefined'))
			setPreferredNotificationTrigger(JSON.parse(settings.preferredNotificationTrigger));
		if(!(settings.preferredRoomNotificationTrigger === null || typeof settings.preferredRoomNotificationTrigger === 'undefined'))
            setRoomPreferredNotificationTriggerSettings(JSON.parse(settings.preferredRoomNotificationTrigger));
    }
	var canIbypassDonotDistrub = function(){
		return (preferredNotificationTrigger.indexOf('notify-even-not-distrub')!== -1) ;
	}

	var canIPlaySound = function(){
		return (preferredNotification.indexOf('bip')!== -1) ;
	}

	var canIShowDesktopNotif = function(){
		return (preferredNotification.indexOf('desktop')!== -1) ;
	}

	var canIShowOnSiteNotif = function(){
		return (preferredNotification.indexOf('on-site')!== -1) ;
	}
	
	var canIbypassRoomNotif = function(msgObj) {
	  var message =  msgObj.content;
	  var sourceRoom = msgObj.categoryId;
	  return (!preferredRoomNotificationTrigger[sourceRoom]) || // 1-1 chat, but room
	         (preferredRoomNotificationTrigger[sourceRoom] == ROOM_NOTIF_TRIGGER_NORMAL) ||
	         (preferredRoomNotificationTrigger[sourceRoom].startsWith(ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD) &&
              containKeyWord(message, preferredRoomNotificationTrigger[sourceRoom]));
	}
	
	var containKeyWord = function(message, keywords) {
      message = message.toLowerCase();
	  var keys = keywords.split(":")[1].split(",");
	  for (var i = 0; i < keys.length; i++) {
	    if (message.includes(keys[i].trim().toLowerCase())) {
	      return true;
	    }
	  }
	  return false;
	}

	var checkMention = function(username,msg) {
	  return (msg.indexOf('@'+username) > -1) && (preferredNotificationTrigger.indexOf('notify-when-mention') !== -1);
	};

	var highlightMessage = function(msgObject){
	  var highlightedMsg = "";
	  switch(msgObject.options.type) {
		  case "type-hand" : highlightedMsg = "raises hand: "+msgObject.content; break;
		  case "type-link" : highlightedMsg = msgObject.options.link; break;
		  case "type-meeting-start" : highlightedMsg = "Start Meeting" ; break;
		  case "type-meeting-stop" : highlightedMsg = "End Meeting"; break;
		  default : highlightedMsg = msgObject.content;
	  }
	  return highlightedMsg;
	}

	return {
		getPreferredNotification : getPreferredNotification,
		setPreferredNotification : setPreferredNotification,
		getPreferredNotificationTrigger : getPreferredNotificationTrigger,
		setPreferredNotificationTrigger : setPreferredNotificationTrigger,
        getRoomPreferredNotificationTrigger : getRoomPreferredNotificationTrigger,
        setRoomPreferredNotificationTrigger : setRoomPreferredNotificationTrigger,
		setPreferredNotificationSettings : setPreferredNotificationSettings,
		canIbypassDonotDistrub : canIbypassDonotDistrub,
		canIPlaySound :  canIPlaySound,
		canIShowDesktopNotif : canIShowDesktopNotif,
		canIShowOnSiteNotif : canIShowOnSiteNotif,
		canIbypassRoomNotif : canIbypassRoomNotif,
		checkMention : checkMention,
		highlightMessage : highlightMessage,
		ROOM_NOTIF_TRIGGER_NORMAL : ROOM_NOTIF_TRIGGER_NORMAL,
        ROOM_NOTIF_TRIGGER_SILENCE : ROOM_NOTIF_TRIGGER_SILENCE,
        ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD : ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD,
        ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD_VALUE : ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD_VALUE
	};

})()