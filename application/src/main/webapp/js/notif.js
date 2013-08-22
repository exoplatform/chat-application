// GLOBAL VARIABLES
var chatNotification = new ChatNotification();
var weemoExtension = new WeemoExtension();

//var jq171 = jQuery.noConflict(true);
//(function($) {

  jQuery(document).ready(function(){

    //GETTING DOM CONTEXT
    var $notificationApplication = jQuery("#chat-status");
    // CHAT NOTIFICATION INIT
    chatNotification.initOptions({
      "token": $notificationApplication.attr("data-token"),
      "username": $notificationApplication.attr("data-username"),
      "urlInitUserProfile": $notificationApplication.jzURL("NotificationApplication.initUserProfile"),
      "urlNotification": $notificationApplication.attr("data-chat-server-url")+"/notification",
      "urlGetStatus": $notificationApplication.attr("data-chat-server-url")+"/getStatus",
      "notificationInterval": $notificationApplication.attr("data-chat-interval-notif"),
      "statusInterval": $notificationApplication.attr("data-chat-interval-status")
    });
    // CHAT NOTIFICATION USER INTERFACE PREPARATION
    chatNotification.initUserInterface();

    // WEEMO : GETTING AND SETTING KEY
    var weemoKey = $notificationApplication.attr("data-weemo-key");
    weemoExtension.setKey(weemoKey);

    // WEEMO : INIT CALL CALLBACK
    var startWeemo = function(username, fullname) {
      weemoExtension.initCall(username, fullname);
      weemoExtension.attachWeemoToPopups();
      weemoExtension.attachWeemoToConnections();
    }

    // CHAT NOTIFICATION : START WEEMO ON SUCCESS
    chatNotification.initUserProfile(startWeemo);





  });
//
//})(jq171);


/**
 ##################                           ##################
 ##################                           ##################
 ##################   CHAT NOTIFICATION       ##################
 ##################                           ##################
 ##################                           ##################
 */

/**
 * ChatNotification Class
 * @constructor
 */

function ChatNotification() {
  this.token = "";
  this.username = "";
  this.jzInitUserProfile = "";
  this.jzNotification = "";
  this.jzGetStatus = "";

  this.notifEventURL = "";
  this.notifEventInt = "";
  this.chatIntervalNotif = "";
  this.statusEventInt = "";
  this.chatIntervalStatus = "";

  this.oldNotifTotal = 0;
  this.profileStatus = "offline";
}

/**
 * Init Notifications variables
 * @param options
 */
ChatNotification.prototype.initOptions = function(options) {
  this.token = options.token;
  this.username = options.username;
  this.jzInitUserProfile = options.urlInitUserProfile;
  this.jzNotification = options.urlNotification;
  this.jzGetStatus = options.urlGetStatus;
  this.chatIntervalNotif = options.notificationInterval;
  this.chatIntervalStatus = options.statusInterval;
  this.notifEventURL = this.jzNotification+'?user='+this.username+'&token='+this.token;
};

/**
 * Create the User Interface in the Intranet DOM
 */
ChatNotification.prototype.initUserInterface = function() {
  jQuery(".uiCompanyNavigations > li")
    .children()
    .filter(function() {
      if (jQuery(this).attr("href") == "/portal/intranet/chat") {
        jQuery(this).css("width", "95%");
        var html = '<i class="uiChatIcon"></i>Chat';
        html += '<span id="chat-notification" style="float: right; display: none;"></span>';
        jQuery(this).html(html);
      }
    });
};

ChatNotification.prototype.updateNotifEventURL = function() {
  this.notifEventURL = this.jzNotification+'?user='+this.username+'&token='+this.token;
};

/**
 * Init Chat User Profile
 * @param callback : allows you to call an async callback function(username, fullname) when the profile is initiated.
 */
ChatNotification.prototype.initUserProfile = function(callback) {

  jQuery.ajax({
    url: this.jzInitUserProfile,
    dataType: "json",
    context: this,
    success: function(data){
//      console.log("Profile Update : "+data.msg);
      this.token = data.token;
//      console.log("Token : "+data.token);

      var fullname = this.username; //setting fullname with username from that point

      if (typeof callback === "function") {
        callback(this.username, fullname);
      }

      this.notifEventInt = window.clearInterval(this.notifEventInt);
      this.notifEventInt = setInterval(jQuery.proxy(this.refreshNotif, this), this.chatIntervalNotif);
      this.refreshNotif();

      this.statusEventInt = window.clearInterval(this.statusEventInt);
      this.statusEventInt = setInterval(jQuery.proxy(this.refreshStatus, this), this.chatIntervalStatus);
      this.refreshStatus();
    },
    error: function () {
      //retry in 3 sec
      setTimeout(jQuery.proxy(this.initUserProfile, this), 3000);
    }
  });

};


/**
 * Refresh Notifications
 */
ChatNotification.prototype.refreshNotif = function() {
//  if ( ! jQuery("span.chat-status").hasClass("chat-status-offline-black") ) {
//  console.log("refreshNotif :: URL="+this.notifEventURL);
  this.updateNotifEventURL();
  jQuery.ajax({
    url: this.notifEventURL,
    dataType: "json",
    context: this,
    success: function(data){
      if(this.oldNotifTotal!=data.total){
        var total = Math.abs(data.total);
        //console.log('refreshNotif :: '+total);
        var $chatNotification = jQuery("#chat-notification");
        if (total>0) {
          $chatNotification.html('<span class="notif-total">'+total+'</span>');
          $chatNotification.css('display', 'block');
        } else {
          $chatNotification.html('<span></span>');
          $chatNotification.css('display', 'none');
        }
        if (total>this.oldNotifTotal && this.profileStatus !== "donotdisturb" && this.profileStatus !== "offline") {
          this.playNotifSound();
        }

        this.oldNotifTotal = total;
      }
    },
    error: function(){
      var $chatNotification = jQuery("#chat-notification");
      this.changeStatus("offline");
      $chatNotification.html('<span></span>');
      $chatNotification.css('display', 'none');
      this.oldNotifTotal = -1;
    }
  });

};

/**
 * Play Notif Sound
 */
ChatNotification.prototype.playNotifSound = function() {
  var notifSound=document.getElementById("chat-audio-notif");
  notifSound.play();
};


/**
 * Refresh Status
 */
ChatNotification.prototype.refreshStatus = function() {
//  console.log("refreshStatus :: URL="+this.jzGetStatus);
  jQuery.ajax({
    url: this.jzGetStatus,
    data: {
      "user": this.username,
      "token": this.token
    },
    context: this,
    success: function(response){
      this.changeStatus(response);
    },
    error: function(response){
      this.changeStatus("offline");
    }
  });
};

/**
 * Gets target user status
 * @param targetUser
 */
ChatNotification.prototype.getStatus = function(targetUser, callback) {
//  console.log("refreshStatus :: URL="+this.jzGetStatus);
  jQuery.ajax({
    url: this.jzGetStatus,
    data: {
      "user": this.username,
      "token": this.token,
      "targetUser": targetUser
    },
    context: this,
    success: function(response){
      if (typeof callback === "function") {
        callback(targetUser, response);
      }
    },
    error: function(response){
      if (typeof callback === "function") {
        callback(targetUser, "offline");
      }
    }
  });
};


/**
 * Change the current status
 * @param status : the new status : available, donotdisturb, invisible, away or offline
 */
ChatNotification.prototype.changeStatus = function(status) {
  this.profileStatus = status;
  var $spanStatusChat = jQuery("span.chat-status-chat");
  $spanStatusChat.removeClass("chat-status-available");
  $spanStatusChat.removeClass("chat-status-donotdisturb");
  $spanStatusChat.removeClass("chat-status-invisible");
  $spanStatusChat.removeClass("chat-status-away");
  $spanStatusChat.removeClass("chat-status-offline");
  $spanStatusChat.addClass("chat-status-"+status);
};


/**
 ##################                           ##################
 ##################                           ##################
 ##################   WEEMO EXTENSION         ##################
 ##################                           ##################
 ##################                           ##################
 */


/**
 * WeemoExtension Class
 * @constructor
 */
function WeemoExtension() {
  this.weemoKey = "";
  this.weemo = new Weemo();

  this.callOwner = jzGetParam("callOwner", false);
  this.callActive = jzGetParam("callActive", false);
  this.callType = jzGetParam("callType", "");

  this.uidToCall = jzGetParam("uidToCall", "");
  this.displaynameToCall = jzGetParam("displaynameToCall", "");

  this.chatMessage = JSON.parse( jzGetParam("chatMessage", '{}') );

  this.isConnected = false;
}

WeemoExtension.prototype.log = function() {
  console.log("callOwner         :: "+this.callOwner);
  console.log("callActive        :: "+this.callActive);
  console.log("callType          :: "+this.callType);
  console.log("uidToCall         :: "+this.uidToCall);
  console.log("displayNameToCall :: "+this.displaynameToCall);
  console.log("chatMessage       :: "+this.chatMessage);
}

WeemoExtension.prototype.setKey = function(weemoKey) {
  this.weemoKey = weemoKey;
  jzStoreParam("weemoKey", weemoKey, 14400); // timeout = 60 sec * 60 min * 4 hours = 14400 sec
};

WeemoExtension.prototype.setCallOwner = function(callOwner) {
  this.callOwner = callOwner;
  jzStoreParam("callOwner", callOwner, 14400);
};

WeemoExtension.prototype.setCallType = function(callType) {
  this.callType = callType;
  jzStoreParam("callType", callType, 14400);
};

WeemoExtension.prototype.setCallActive = function(callActive) {
  this.callActive = callActive;
  jzStoreParam("callActive", callActive, 14400);
};

WeemoExtension.prototype.setUidToCall = function(uidToCall) {
  this.uidToCall = uidToCall;
  jzStoreParam("uidToCall", uidToCall, 14400);
};

WeemoExtension.prototype.setDisplaynameToCall = function(displaynameToCall) {
  this.displaynameToCall = displaynameToCall;
  jzStoreParam("displaynameToCall", displaynameToCall, 14400);
};
/**
 * A JSON Object like :
 * { "url" : url,
 *   "user" : user,
 *   "targetUser" : targetUser,
 *   "room" : room,
 *   "token" : token
 * }
 * @param chatMessage
 */
WeemoExtension.prototype.setChatMessage = function(chatMessage) {
  this.chatMessage = chatMessage;
  jzStoreParam("chatMessage", JSON.stringify(chatMessage), 14400);
};

WeemoExtension.prototype.hasChatMessage = function() {
  return (this.chatMessage.url !== undefined);
};

WeemoExtension.prototype.initChatMessage = function() {
  this.setChatMessage({});
};

/**
 * Init Weemo Call
 * @param $uid
 * @param $name
 */
WeemoExtension.prototype.initCall = function($uid, $name) {
  if (this.weemoKey!=="") {
    jQuery(".btn-weemo-conf").css('display', 'none');
    //this.weemo = new Weemo(); // Creating a Weemo object instance
    this.weemo.setMode("production"); // Activate debugging in browser's log console
    this.weemo.setEnvironment("production"); // Set environment  (development, testing, staging, production)
    this.weemo.setPlatform("p1.weemo.com"); // Set connection platform (by default: "p1.weemo.com")
    this.weemo.setDomain("weemo-poc.com"); // Chose your domain, for POC all apikey are created for "weemo-poc.com" domain
    this.weemo.setApikey(this.weemoKey); // Configure your Api Key
    this.weemo.setUid("weemo"+$uid); // Configure your UID

    //weemo.setDisplayname($name); // Configure the display name
    this.weemo.connectToWeemoDriver(); // Launches the connection between WeemoDriver and Javascript


    /**
     * Weemo Driver On Connection Javascript Handler
     *
     * @param message
     * @param code
     */
    this.weemo.onConnectionHandler = function(message, code) {
//      if(window.console)
//        console.log("Connection Handler : " + message + ' ' + code);
      switch(message) {
        case 'connectedWeemoDriver':
          this.connectToTheCloud();
          break;
        case 'sipOk':
          weemoExtension.isConnected = true;
          jQuery(".btn-weemo").removeClass('disabled');
          var fn = jQuery(".label-user").text();
          var fullname = jQuery("#UIUserPlatformToolBarPortlet > a:first").text().trim();
          if (fullname!=="") {
            this.setDisplayname(fullname); // Configure the display name
          } else if (fn!=="") {
            this.setDisplayname(fn); // Configure the display name
          }
          break;
      }
    }

    /**
     * Weemo Driver On Driver Started Javascript Handler
     *
     * @param downloadUrl
     */
    this.weemo.onWeemoDriverNotStarted = function(downloadUrl) {
      var $btnDownload = jQuery(".btn-weemo-download");
      $btnDownload.css("display", "inline-block");
      if (navigator.platform === "Linux") {
        $btnDownload.addClass("disabled");
        $btnDownload.attr("title", "Weemo is not yet compatible with Linux OS.");
      } else {
        $btnDownload.attr("href", downloadUrl);
      }
/*
      var modal = new Modal('WeemoDriver download', 'Click <a href="'+downloadUrl+'">here</a> to download.');
      modal.show();
      jQuery(".weemo_modal_box").css("top", "42px");
      var $weemo_inner_modal_box = jQuery(".weemo_inner_modal_box");
      $weemo_inner_modal_box.css("text-align", "center");
      $weemo_inner_modal_box.css("padding", "25px");
      $weemo_inner_modal_box.css("font-size", "18px");
      $weemo_inner_modal_box.children("h2").css("font-size", "24px");
*/
    };


    /**
     * Weemo Driver On Call Javascript Handler
     *
     * @param type
     * @param status
     */
    this.weemo.onCallHandler = function(type, status)
    {
      console.log("WEEMO:onCallHandler::"+type+":"+status);
      var messageWeemo = "";
      var optionsWeemo = {};
      if(weemoExtension.callOwner && type==="call" && ( status==="active" || status==="terminated" ))
      {
        console.log("Call Handler : " + type + ": " + status);
        ts = Math.round(new Date().getTime() / 1000);

        if (status === "terminated") weemoExtension.setCallOwner(false);

        if (weemoExtension.callType==="internal" || status==="terminated") {
          messageWeemo = "Call "+status;
          optionsWeemo.timestamp = ts;
        } else if (weemoExtension.callType==="host") {
          messageWeemo = "Call "+status;
          optionsWeemo.timestamp = ts;
          optionsWeemo.uidToCall = weemoExtension.uidToCall;
          optionsWeemo.displaynameToCall = weemoExtension.displaynameToCall;
        }

        if (status==="active" && weemoExtension.callActive) return; //Call already active, no need to push a new message
        if (status==="terminated" && !weemoExtension.callActive) return; //Terminate a non started call, no message needed


        if (status==="active") {
          weemoExtension.setCallActive(true);
          optionsWeemo.type = "call-on";
        }
        else if (status==="terminated") {
          weemoExtension.setCallActive(false);
          optionsWeemo.type = "call-off";
        }

        if (weemoExtension.callType!=="attendee") {
          if (weemoExtension.hasChatMessage()) {
            console.log("WEEMO:hasChatMessage::"+weemoExtension.chatMessage.user+":"+weemoExtension.chatMessage.targetUser);
            if (chatApplication !== undefined) {
              chatApplication.chatRoom.sendFullMessage(
                weemoExtension.chatMessage.user,
                weemoExtension.chatMessage.token,
                weemoExtension.chatMessage.targetUser,
                weemoExtension.chatMessage.room,
                messageWeemo,
                optionsWeemo,
                "true"
              )
            }

            if (status==="terminated") {
              weemoExtension.initChatMessage();
            }
          }
        }
      }
    }


  } else {
    jQuery(".btn-weemo").css('display', 'none');
  }
};

/**
 *
 */
WeemoExtension.prototype.createWeemoCall = function(targetUser, targetFullname, chatMessage) {
  console.log(targetUser+" : "+targetFullname);
  if (this.weemoKey!=="") {

    if (chatMessage !== undefined) {
      this.setChatMessage(chatMessage);
    }

    if (targetUser.indexOf("space-")===-1) {
      this.setUidToCall("weemo"+targetUser);
      this.setDisplaynameToCall(targetFullname);
      this.setCallType("internal");
    } else {
      this.setUidToCall(this.weemo.getUid());
      this.setDisplaynameToCall(this.weemo.getDisplayname());
      this.setCallType("host");
    }
    this.setCallOwner(true);
    this.setCallActive(false);
    this.weemo.createCall(this.uidToCall, this.callType, this.displaynameToCall);

  }

};

/**
 *
 */
WeemoExtension.prototype.joinWeemoCall = function() {
  if (this.weemoKey!=="") {
    this.setCallType("attendee");
    this.setCallOwner(false);
    this.weemo.createCall(this.uidToCall, this.callType, this.displaynameToCall);

  }

};

WeemoExtension.prototype.attachWeemoToPopups = function() {
  var checkTiptip = jQuery('#tiptip_content').html();
  if (checkTiptip === undefined) {
    setTimeout(jQuery.proxy(this.attachWeemoToPopups, this), 250);
    return;
  }
  jQuery('#tiptip_content').bind('DOMNodeInserted', function() {
    var username = "";
    var fullname = "";
    var addStyle = "";
    var $uiElement;
    var $uiDetail = jQuery('#tiptip_content').children('#tipName').children(".detail").children(".name").children("a");
    if ($uiDetail !== undefined) {
      var href = $uiDetail.attr("href");
      if (href !== undefined) {
        fullname = $uiDetail.html();
        username = href.substr(href.indexOf("/activities/")+12);
      }
    }

    var $uiMessage = jQuery('#connectMessge', this);
    if ($uiMessage !== undefined) {
      $uiElement = $uiMessage;
    }

    var $uiAction = jQuery(".uiAction", this).first();
    if ($uiAction !== undefined && $uiAction.html() !== undefined) {
      //console.log("uiAction bind on weemoCallOverlay");
      var attr = $uiAction.children(".connect:first").attr("data-action");
      if (attr !== undefined) {
        var $uiFullname = jQuery('#tiptip_content').children('#tipName').children("tbody").children("tr").children("td").children("a");
        fullname = $uiFullname.html();

        if (attr.indexOf(":")>0) {
          if (attr.indexOf("Disconnect:")>-1)
            addStyle = "margin-top:8px;";
          username = attr.substr(attr.indexOf(":")+1, attr.length-attr.indexOf(":"));
        }
      }
      $uiElement = $uiAction;
    }

    if (username !== "" && $uiElement.has(".weemoCallOverlay").size()===0) {
      var out = '<a type="button" class="btn weemoCallOverlay disabled" title="Make a Video Call"';
          out += ' data-fullname="'+fullname+'"';
          out += ' data-username="'+username+'" style="margin-left:5px;'+addStyle+'">';
          out += '<i class="icon-facetime-video"></i> Call</a>';
      //$uiElement.append("<div class='btn weemoCallOverlay' data-username='"+username+"' style='margin-left:5px;"+addStyle+"'>Call</div>");
      $uiElement.append(out);
      jQuery(".weemoCallOverlay").on("click", function() {
        if (!jQuery(this).hasClass("disabled")) {
          //console.log("weemo button clicked");
          var targetUser = jQuery(this).attr("data-username");
          var targetFullname = jQuery(this).attr("data-fullname");
          weemoExtension.createWeemoCall(targetUser, targetFullname);
        }
      });

      function cbGetStatus(targetUser, status) {
        //console.log("Status :: target="+targetUser+" : status="+status);
        if (status !== "offline") {
          var $weemoBtn = jQuery(".weemoCallOverlay");
          if ($weemoBtn.attr("data-username") == targetUser) {
            $weemoBtn.removeClass("disabled");
          }
        }
      }
      chatNotification.getStatus(username, cbGetStatus);


    }

  });

};

WeemoExtension.prototype.attachWeemoToConnections = function() {
  if (window.location.href.indexOf("/portal/intranet/connexions")==-1) return;

  var $uiPeople = jQuery('.uiTabInPage').first();
  if ($uiPeople.html() === undefined) {
    setTimeout(jQuery.proxy(this.attachWeemoToConnections, this), 250);
    return;
  }

  function cbGetConnectionStatus(targetUser, status) {
    //console.log("Status :: target="+targetUser+" : status="+status);
    if (status !== "offline") {
      var $weemoBtn = jQuery("#weemoCall-"+targetUser.replace(".", "-"));
      if ($weemoBtn.attr("data-username") == targetUser) {
        $weemoBtn.removeClass("disabled");
      }
    }
  }

  jQuery(".contentBox", ".uiTabInPage").each(function() {
    var $uiUsername = jQuery(this).children(".spaceTitle").children("a").first();
    var username = $uiUsername.attr("href");
    username = username.substring(username.lastIndexOf("/")+1);
    var fullname = $uiUsername.html();

    var $uiActionWeemo = jQuery(".weemoCallOverlay", this).first();
    if ($uiActionWeemo !== undefined && $uiActionWeemo.html() == undefined) {
      var html = jQuery(this).html();
      html += '<a type="button" class="btn weemoCallOverlay pull-right disabled" id="weemoCall-'+username.replace('.', '-')+'" title="Make a Video Call"';
      html += ' data-username="'+username+'" data-fullname="'+fullname+'"';
      html += ' style="margin-left:5px;"><i class="icon-facetime-video"></i> Call</a>';
      jQuery(this).html(html);

      chatNotification.getStatus(username, cbGetConnectionStatus);
    }

  });


  jQuery(".weemoCallOverlay").on("click", function() {
    if (!jQuery(this).hasClass("disabled")) {
      //console.log("weemo button clicked");
      var targetUser = jQuery(this).attr("data-username");
      var targetFullname = jQuery(this).attr("data-fullname");
      weemoExtension.createWeemoCall(targetUser, targetFullname);
    }
  });


};

/**
 ##################                           ##################
 ##################                           ##################
 ##################   HACK                    ##################
 ##################                           ##################
 ##################                           ##################
 */



/**
 * Hack to ignore console on for Internet Explorer (without testing its existence
 * @type {*|{log: Function, warn: Function, error: Function}}
 */
var console = console || {
  log:function(){},
  warn:function(){},
  error:function(){}
};

