// GLOBAL VARIABLES
var chatNotification = new ChatNotification();
var weemoExtension = new WeemoExtension();

var jq171 = jQuery.noConflict(true);
(function($) {

  $(document).ready(function(){

    //GETTING DOM CONTEXT
    var $notificationApplication = $("#chat-status");
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
    }

    // CHAT NOTIFICATION : START WEEMO ON SUCCESS
    chatNotification.initUserProfile(startWeemo);


  });

})(jq171);





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

  this.oldNotifTotal = "";

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
  $(".uiCompanyNavigations > li")
    .children()
    .filter(function() {
      if ($(this).attr("href") == "/portal/intranet/chat") {
        $(this).css("width", "95%");
        var html = '<i class="uiChatIcon"></i>Chat';
        html += '<span id="chat-notification" style="float: right; display: none;"></span>';
        $(this).html(html);
      }
    });
};
ChatNotification.prototype.updateNotifEventURL = function() {
  this.notifEventURL = this.jzNotification+'?user='+this.username+'&token='+this.token;
}
/**
 * Init Chat User Profile
 * @param callback : allows you to call an async callback function(username, fullname) when the profile is initiated.
 */
ChatNotification.prototype.initUserProfile = function(callback) {

  $.ajax({
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
      this.notifEventInt = setInterval($.proxy(this.refreshNotif, this), this.chatIntervalNotif);
      this.refreshNotif();

      this.statusEventInt = window.clearInterval(this.statusEventInt);
      this.statusEventInt = setInterval($.proxy(this.refreshStatus, this), this.chatIntervalStatus);
      //this.refreshStatus();
    },
    error: function () {
      //retry in 3 sec
      setTimeout($.proxy(this.initUserProfile, this), 3000);
    }
  });

};


/**
 * Refresh Notifications
 */
ChatNotification.prototype.refreshNotif = function() {
//  if ( ! $("span.chat-status").hasClass("chat-status-offline-black") ) {
//  console.log("refreshNotif :: URL="+this.notifEventURL);
  this.updateNotifEventURL();
  $.ajax({
    url: this.notifEventURL,
    dataType: "json",
    context: this,
    success: function(data){
      if(this.oldNotifTotal!=data.total){
        var total = data.total;
        //console.log('refreshNotif :: '+total);
        var $chatNotification = $("#chat-notification");
        if (total>0) {
          $chatNotification.html('<span class="notif-total">'+total+'</span>');
          $chatNotification.css('display', 'block');
        } else {
          $chatNotification.html('<span></span>');
          $chatNotification.css('display', 'none');
        }
        this.oldNotifTotal = data.total;
      }
    },
    error: function(){
      var $chatNotification = $("#chat-notification");
      this.changeStatus("offline");
      $chatNotification.html('<span></span>');
      $chatNotification.css('display', 'none');
      this.oldNotifTotal = -1;
    }
  });

//  } else {
//    var $chatNotification = $("#chat-notification");
//    $chatNotification.html('<span></span>');
//    $chatNotification.css('display', 'none');
//    this.oldNotifTotal = -1;
//  }
};


/**
 * Refresh Status
 */
ChatNotification.prototype.refreshStatus = function() {
//  console.log("refreshStatus :: URL="+this.jzGetStatus);
  $.ajax({
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
}


/**
 * Change the current status
 * @param status : the new status : available, donotdisturb, invisible, away or offline
 */
ChatNotification.prototype.changeStatus = function(status) {
  var $spanStatus = $("span.chat-status");
  $spanStatus.removeClass("chat-status-available-black");
  $spanStatus.removeClass("chat-status-donotdisturb-black");
  $spanStatus.removeClass("chat-status-invisible-black");
  $spanStatus.removeClass("chat-status-away-black");
  $spanStatus.removeClass("chat-status-offline-black");
  $spanStatus.addClass("chat-status-"+status+"-black");
  var $spanStatusChat = $("span.chat-status-chat");
  $spanStatusChat.removeClass("chat-status-available");
  $spanStatusChat.removeClass("chat-status-donotdisturb");
  $spanStatusChat.removeClass("chat-status-invisible");
  $spanStatusChat.removeClass("chat-status-away");
  $spanStatusChat.removeClass("chat-status-offline");
  $spanStatusChat.addClass("chat-status-"+status);
}


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

  this.callOwner = false;
  this.callActive = false;
  this.callType = "";

  this.uidToCall = "";
  this.displaynameToCall = "";
}

WeemoExtension.prototype.setKey = function(weemoKey) {
  this.weemoKey = weemoKey;
};

/**
 * Init Weemo Call
 * @param $uid
 * @param $name
 */
WeemoExtension.prototype.initCall = function($uid, $name) {
  if (this.weemoKey!=="") {
    $(".btn-weemo-conf").css('display', 'none');
    //this.weemo = new Weemo(); // Creating a Weemo object instance
    //this.weemo.setMode("debug"); // Activate debugging in browser's log console
    this.weemo.setEnvironment("production"); // Set environment  (development, testing, staging, production)
    this.weemo.setPlatform("p1.weemo.com"); // Set connection platform (by default: "p1.weemo.com")
    this.weemo.setDomain("weemo-poc.com"); // Chose your domain, for POC all apikey are created for "weemo-poc.com" domain
    this.weemo.setApikey(this.weemoKey); // Configure your Api Key
    this.weemo.setUid("weemo"+$uid); // Configure your UID

    //weemo.setDisplayname($name); // Configure the display name
    this.weemo.connectToWeemoDriver(); // Launches the connection between WeemoDriver and Javascript

    this.weemo.onConnectionHandler = function(message, code) {
//      if(window.console)
//        console.log("Connection Handler : " + message + ' ' + code);
      switch(message) {
        case 'connectedWeemoDriver':
          this.connectToTheCloud();
          break;
        case 'sipOk':
          $(".btn-weemo").removeClass('disabled');
          var fn = $(".label-user").text();
          if (fn!=="") {
            this.setDisplayname(fn); // Configure the display name
          }
          break;
      }
    }

    this.weemo.onWeemoDriverNotStarted = function(downloadUrl) {
      var modal = new Modal('WeemoDriver download', 'Click <a href="'+downloadUrl+'">here</a> to download.');
      modal.show();
    };

  } else {
    $(".btn-weemo").css('display', 'none');
  }
}

/**
 *
 */
WeemoExtension.prototype.createWeemoCall = function(targetUser, fullname) {

  if (this.weemoKey!=="") {

    if (targetUser.indexOf("space-")===-1) {
      this.uidToCall = "weemo"+targetUser;
      this.displaynameToCall = fullname;
      this.callType = "internal";
    } else {
      this.uidToCall = this.weemo.getUid();
      this.displaynameToCall = this.weemo.getDisplayname();
      this.callType = "host";
    }
    this.callOwner = true;
    this.callActive = false;
    this.weemo.createCall(this.uidToCall, this.callType, this.displaynameToCall);

  }

}

/**
 *
 */
WeemoExtension.prototype.joinWeemoCall = function() {
  if (this.weemoKey!=="") {
    this.callType = "attendee";
    this.callOwner = false;
    this.weemo.createCall(this.uidToCall, this.callType, this.displaynameToCall);

  }

}




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

