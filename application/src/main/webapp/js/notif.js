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
  this.jzSetStatus = "";

  this.notifEventURL = "";
  this.notifEventInt = "";
  this.chatIntervalNotif = "";
  this.statusEventInt = "";
  this.chatIntervalStatus = "";

  this.oldNotifTotal = 0;
  this.profileStatus = "offline";

  this.chatPage = "/portal/intranet/chat";
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
  this.jzSetStatus = options.urlSetStatus;
  this.chatIntervalNotif = options.notificationInterval;
  this.chatIntervalStatus = options.statusInterval;
  this.notifEventURL = this.jzNotification+'?user='+this.username+'&token='+this.token;
};

/**
 * Create the User Interface in the Intranet DOM
 */
ChatNotification.prototype.initUserInterface = function() {
  jqchat(".uiCompanyNavigations > li")
    .children()
    .filter(function() {
      if (jqchat(this).attr("href") == "/portal/intranet/chat") {
        jqchat(this).css("width", "95%");
        var html = '<i class="uiChatIcon"></i>Chat';
        //html += '<span id="chat-notification" style="float: right; display: none;"></span>';
        jqchat(this).html(html);
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

  jqchat.ajax({
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
      this.notifEventInt = setInterval(jqchat.proxy(this.refreshNotif, this), this.chatIntervalNotif);
      this.refreshNotif();

      this.statusEventInt = window.clearInterval(this.statusEventInt);
      this.statusEventInt = setInterval(jqchat.proxy(this.refreshStatusChat, this), this.chatIntervalStatus);
      this.refreshStatusChat();
    },
    error: function () {
      //retry in 3 sec
      setTimeout(jqchat.proxy(this.initUserProfile, this), 3000);
    }
  });

};


/**
 * Refresh Notifications
 */
ChatNotification.prototype.refreshNotifDetails = function() {
  if (this.oldNotifTotal>0) {
    jqchat("#chat-notifications-details").css("display", "initial");

    this.updateNotifEventURL();
    jqchat.ajax({
      url: this.notifEventURL+"&withDetails=true",
      dataType: "json",
      context: this,
      success: function(data){
        var $chatNotificationsDetails = jqchat("#chat-notifications-details");
        var html = '';
        if (data.notifications.length>0) {
          for (var inot = 0 ; inot<data.notifications.length ; inot++) {
            var notif = data.notifications[inot];
            html += '<li>';
            html +=   '<a href="#" data-link="'+notif.link+'" data-id="'+notif.categoryId+'" class="chat-notification-detail" >';
            html +=     '<img class="avatar-image" onerror="this.src=\'/chat/img/Avatar.gif;\'" src=\'/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:'+notif.from+'/soc:profile/soc:avatar\' width=\'30px\' height=\'30px\'  style="width:30px; height:30px;">';
            html +=     '<div class="chat-label-status">';
            html +=      '<div class="content">'+notif.content+'</div>';
            html +=      '<div class="timestamp">'+this.getDate(notif.timestamp)+'</div>';
            html +=     '</div>';
            html +=   '</a>';
            html += '</li>';
            if (fromChromeApp) {
              if (this.profileStatus !== "donotdisturb" && this.profileStatus !== "offline") {
                doSendMessage(notif);
              }
            }

          }
          html += '<li class="divider">&nbsp;</li>';
        }
        $chatNotificationsDetails.html(html);
        $chatNotificationsDetails.css("display", "block");
        jqchat(".chat-notification-detail").on("click", function(){
          var id = jqchat(this).attr("data-id");
          showMiniChatPopup(id, "room-id");
        });
      },
      error: function(){
        var $chatNotificationsDetails = jqchat("#chat-notifications-details");
        $chatNotificationsDetails.html("");
        $chatNotificationsDetails.css("display", "none");
      }
    });


  }
};

ChatNotification.prototype.getDate = function(timestampServer) {
  var date = new Date();
  if (timestampServer !== undefined)
    date = new Date(timestampServer);

  var now = new Date();
  var sNowDate = now.toLocaleDateString();
  var sDate = date.toLocaleDateString();

  var sTime = "";
  var sHours = date.getHours();
  var sMinutes = date.getMinutes();
  var timezone = date.getTimezoneOffset();

  var ampm = "";
  if (timezone>60) {// 12 Hours AM/PM model
    ampm = "AM";
    if (sHours>11) {
      ampm = "PM";
      sHours -= 12;
    }
    if (sHours===0) sHours = 12;
  }
  if (sHours<10) sTime = "0";
  sTime += sHours+":";
  if (sMinutes<10) sTime += "0";
  sTime += sMinutes;
  if (ampm !== "") sTime += " "+ampm;
  if (sNowDate !== sDate) {
    sTime = sDate + " " + sTime;
  }
  return sTime;

}


/**
 * Refresh Notifications
 */
ChatNotification.prototype.refreshNotif = function() {
//  if ( ! jqchat("span.chat-status").hasClass("chat-status-offline-black") ) {
//  console.log("refreshNotif :: URL="+this.notifEventURL);
  this.updateNotifEventURL();
  jqchat.ajax({
    url: this.notifEventURL,
    dataType: "json",
    context: this,
    success: function(data){
      if(this.oldNotifTotal!=data.total){
        var total = Math.abs(data.total);
        //console.log('refreshNotif :: '+total);
        var $chatNotification = jqchat("#chat-notification");
        if (total>0) {
          $chatNotification.html('<span class="notif-total">'+total+'</span>');
          $chatNotification.css('display', 'block');
        } else {
          $chatNotification.html('<span></span>');
          $chatNotification.css('display', 'none');
          var $chatNotificationsDetails = jqchat("#chat-notifications-details");
          $chatNotificationsDetails.css("display", "none");
          $chatNotificationsDetails.html('<span class="chat-notification-loading no-user-selection">Loading...</span><li class="divider">&nbsp;</li>');
        }
        if (total>this.oldNotifTotal && this.profileStatus !== "donotdisturb" && this.profileStatus !== "offline") {
          this.playNotifSound();
        }

        this.oldNotifTotal = total;
      }
    },
    error: function(){
      var $chatNotification = jqchat("#chat-notification");
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
ChatNotification.prototype.refreshStatusChat = function() {
  var thiss = this;
  snack.request({
    url: thiss.jzGetStatus,
    data: {
      "user": thiss.username,
      "token": thiss.token,
      "timestamp": new Date().getTime()
    }
  }, function (err, response){
    if (err) {
      thiss.changeStatusChat("offline");
    } else {
      thiss.changeStatusChat(response);
    }
  });

};

/**
 * Gets target user status
 * @param targetUser
 */
ChatNotification.prototype.getStatus = function(targetUser, callback) {
//  console.log("refreshStatus :: URL="+this.jzGetStatus);
  jqchat.ajax({
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
 * Set Current Status
 * @param status
 * @param callback
 */
ChatNotification.prototype.setStatus = function(status, callback) {

  if (status !== undefined) {
    //console.log("setStatus :: "+status);

    jqchat.ajax({
      url: this.jzSetStatus,
      data: { "user": this.username,
        "token": this.token,
        "status": status,
        "timestamp": new Date().getTime()
      },
      context: this,

      success: function(response){
        //console.log("SUCCESS:setStatus::"+response);
        chatNotification.changeStatusChat(response);
        if (typeof callback === "function") {
          callback(response);
        }

      },
      error: function(response){
        chatNotification.changeStatusChat("offline");
        if (typeof callback === "function") {
          callback("offline");
        }
      }

    });
  }

};


/**
 * Change the current status
 * @param status : the new status : available, donotdisturb, invisible, away or offline
 */
ChatNotification.prototype.changeStatusChat = function(status) {
  this.profileStatus = status;
  if (typeof chatApplication === "object") {
    chatApplication.profileStatus = status;
  }
  var $chatStatusChat = jqchat(".chat-status-chat");
  $chatStatusChat.removeClass("chat-status-available");
  $chatStatusChat.removeClass("chat-status-donotdisturb");
  $chatStatusChat.removeClass("chat-status-invisible");
  $chatStatusChat.removeClass("chat-status-away");
  $chatStatusChat.removeClass("chat-status-offline");
  $chatStatusChat.addClass("chat-status-"+status);
};

ChatNotification.prototype.openChatPopup = function() {
  window.open(this.chatPage+"?noadminbar=true","chat-popup","menubar=no, status=no, scrollbars=no, titlebar=no, resizable=no, location=no, width=700, height=600");
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
  try {
    this.weemo = new Weemo('', '', 'internal', '', '1');
  } catch (err) {
    console.log("WEEMO NOT AVAILABLE YET");
    this.weemo = undefined;
    jqchat(".btn-weemo-conf").css('display', 'none');
    jqchat(".btn-weemo").addClass('disabled');

  }
  this.callObj;

  this.callOwner = jzGetParam("callOwner", false);
  this.callActive = jzGetParam("callActive", false);
  this.callType = jzGetParam("callType", "");

  this.uidToCall = jzGetParam("uidToCall", "");
  this.displaynameToCall = jzGetParam("displaynameToCall", "");

  this.chatMessage = JSON.parse( jzGetParam("chatMessage", '{}') );

  this.isConnected = false;
  this.timeoutWeemo = -1;
  this.changeStatus("not-connected");
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

WeemoExtension.prototype.hangup = function() {
  if (this.callObj !== undefined) {
    this.callObj.hangup();
  }
};

WeemoExtension.prototype.changeStatus = function(status) {
  var $weemoStatus = jqchat(".weemo-status");
  if (typeof status === "undefined") {
    $weemoStatus.removeClass("weemo-status-connected");
    return;
  }
  $weemoStatus.removeClass("weemo-status-not-connected");
  $weemoStatus.removeClass("weemo-status-connecting");
  $weemoStatus.removeClass("weemo-status-error");
  $weemoStatus.removeClass("weemo-status-connected");
  $weemoStatus.addClass("weemo-status-"+status);

}

/**
 * Init Weemo Call
 * @param $uid
 * @param $name
 */
WeemoExtension.prototype.initCall = function($uid, $name) {
  if (this.weemoKey!=="" && this.weemo !== undefined) {
    jqchat(".btn-weemo-conf").css('display', 'none');

    this.weemo.setDebugLevel(4); // Activate debug in JavaScript console
    this.weemo.setWebAppId(this.weemoKey); // Configure your Web App Identifier (For POC use your Web Application Identifier provided by Weeemo)
    this.weemo.setToken("weemo"+$uid); // Set user unique identifier
    this.weemo.initialize(); // Launches the connection between WeemoDriver and Javascript
    var fn = jqchat(".label-user").text();
    var fullname = jqchat("#UIUserPlatformToolBarPortlet > a:first").text().trim();
    if (fullname!=="") {
      this.weemo.setDisplayName(fullname); // Configure the display name
    } else if (fn!=="") {
      this.weemo.setDisplayName(fn); // Configure the display name
    }

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
        case 'connectedWebRTC':
        case 'connectedWeemoDriver':
          weemoExtension.changeStatus("connecting");
//          this.authenticate();
          break;
        case 'sipOk':
          weemoExtension.isConnected = true;
          jqchat(".btn-weemo").removeClass('disabled');
          weemoExtension.changeStatus("connected");
          clearTimeout(weemoExtension.timeoutWeemo);
          weemoExtension.timeoutWeemo = setTimeout(weemoExtension.changeStatus, 3000);
          break;
        case 'loggedasotheruser':
          // force weemo to kick previous user and replace it with current one
          this.authenticate(1);
        case 'sipNok':
        case 'error':
        case 'kicked':
          weemoExtension.changeStatus("error");
          break;
      }
    };

    /**
     * Weemo Driver On Driver Started Javascript Handler
     *
     * @param downloadUrl
     */
    this.weemo.onWeemoDriverNotStarted = function(downloadUrl) {
      var $btnDownload = jqchat(".btn-weemo-download");
      $btnDownload.css("display", "inline-block");
      if (navigator.platform === "Linux") {
        $btnDownload.addClass("disabled");
        $btnDownload.attr("title", "Weemo is not yet compatible with Linux OS.");
      } else {
        $btnDownload.attr("href", downloadUrl);
      }
    };


    /**
     * Weemo Driver On Call Javascript Handler
     *
     * @param type
     * @param status
     */
    this.weemo.onCallHandler = function(callObj, args)
    {
      weemoExtension.callObj = callObj;
      var type = args.type;
      var status = args.status;
      console.log("WEEMO:onCallHandler  ::"+type+":"+status+":"+weemoExtension.callType+":"+weemoExtension.callOwner+":"+weemoExtension.hasChatMessage());
      var messageWeemo = "";
      var optionsWeemo = {};
      if((type==="call" || type==="webRTCcall") && ( status==="active" || status==="terminated" ))
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

        if(type==="webRTCcall" && status==="active") {
          addWeemoDragListeners();
        }

        if (status==="active" && weemoExtension.callActive) return; //Call already active, no need to push a new message
        if (status==="terminated" && (!weemoExtension.callActive || weemoExtension.callType==="attendee")) return; //Terminate a non started call or a joined call, no message needed


        if (weemoExtension.callType==="attendee" && status==="active") {
          weemoExtension.setCallActive(true);
          optionsWeemo.type = "call-join";
          optionsWeemo.username = weemoExtension.chatMessage.user;
          optionsWeemo.fullname = weemoExtension.chatMessage.fullname;

        }
        else if (status==="active") {
          weemoExtension.setCallActive(true);
          optionsWeemo.type = "call-on";
        }
        else if (status==="terminated") {
          weemoExtension.setCallActive(false);
          optionsWeemo.type = "call-off";
        }

        if (weemoExtension.hasChatMessage()) {

          console.log("WEEMO:hasChatMessage::"+weemoExtension.chatMessage.user+":"+weemoExtension.chatMessage.targetUser);
          if (chatApplication !== undefined) {
            chatApplication.checkIfMeetingStarted(function(callStatus){
              if (callStatus === 1 && optionsWeemo.type==="call-on") {
                // Call is already created, not allowed.
                weemoExtension.initChatMessage();
                callObj.hangup();
                return;
              }
              if (callStatus === 0 && optionsWeemo.type==="call-off") {
                // Call is already terminated, no need to terminate again
                return;
              }

              chatApplication.chatRoom.sendFullMessage(
                weemoExtension.chatMessage.user,
                weemoExtension.chatMessage.token,
                weemoExtension.chatMessage.targetUser,
                weemoExtension.chatMessage.room,
                messageWeemo,
                optionsWeemo,
                "true"
              )

              if (status==="terminated") {
                weemoExtension.initChatMessage();
              }

            });
          }
        }
      }
      else if(type==="webRTCcall")
      {
        if(status == 'proceeding')
        {
          console.log('WebRTC call proceeding');
        }
        else if(status == 'incoming')
        {
          console.log('WebRTC Call incoming');
          console.log(callObj);
          var confirmStr;
          if(callObj.dn !== "undefined" && callObj.dn !== undefined)
          {
            confirmStr = callObj.dn + ' invites you to video chat';
          }
          else
          {
            confirmStr = 'A person invites you to video chat';
          }
          if (confirm(confirmStr))
          {
            callObj.accept();
          }
          else
          {
            callObj.hangup();
          }
        }
      }
    }


  } else {
    jqchat(".btn-weemo").css('display', 'none');
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

    if (targetUser.indexOf("space-")===-1 && targetUser.indexOf("team-")===-1) {
      this.setUidToCall("weemo"+targetUser);
      this.setDisplaynameToCall(targetFullname);
      this.setCallType("internal");
    } else {
      this.setUidToCall(this.weemo.getToken());
      this.setDisplaynameToCall(this.weemo.getDisplayName());
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
WeemoExtension.prototype.joinWeemoCall = function(chatMessage) {
  if (this.weemoKey!=="") {
    if (chatMessage !== undefined) {
      this.setChatMessage(chatMessage);
    }
    this.setCallType("attendee");
    this.setCallOwner(false);
    this.setCallActive(false);
    this.weemo.createCall(this.uidToCall, this.callType, this.displaynameToCall);

  }

};

WeemoExtension.prototype.attachWeemoToPopups = function() {
  var checkTiptip = jqchat('#tiptip_content').html();
  if (checkTiptip === undefined) {
    setTimeout(jqchat.proxy(this.attachWeemoToPopups, this), 250);
    return;
  }
  jqchat('#tiptip_content').bind('DOMNodeInserted', function() {
    var username = "";
    var fullname = "";
    var addStyle = "";
    var $uiElement;

    var $uiAction = jqchat(".uiAction", this).first();
    if ($uiAction !== undefined && $uiAction.html() !== undefined) {
      //console.log("uiAction bind on weemoCallOverlay");
      var $uiFullname = jqchat('#tiptip_content').children('#tipName').children("tbody").children("tr").children("td").children("a");
      $uiFullname.each(function() {
        var html = jqchat(this).html();
        if (html.indexOf("/rest/")==-1) {
          fullname = html;
        }
        var href = jqchat(this).attr("href");
        if (href.indexOf("/portal/intranet/activities/")>-1) {
          username = href.substr(28);
        }
      });
      $uiElement = $uiAction;
    }

    if (username !== "" && $uiElement.has(".weemoCallOverlay").size()===0) {
      var out = '<a type="button" class="btn weemoCallOverlay weemoCall-'+username.replace('.', '-')+' disabled" title="Make a Video Call"';
          out += ' data-fullname="'+fullname+'"';
          out += ' data-username="'+username+'" style="margin-left:5px;'+addStyle+'">';
          out += '<i class="icon-facetime-video"></i> Call</a>';
          out += '<a type="button" class="btn chatPopupOverlay chatPopup-'+username.replace('.', '-')+' disabled" title="Chat"';
          out += ' data-username="'+username+'" style="margin-left:5px;'+addStyle+'">';
          out += '<i class="uiIconForum uiIconLightGray"></i> Chat</a>';
      //$uiElement.append("<div class='btn weemoCallOverlay' data-username='"+username+"' style='margin-left:5px;"+addStyle+"'>Call</div>");
      $uiElement.append(out);
      jqchat(".weemoCallOverlay").on("click", function() {
        if (!jqchat(this).hasClass("disabled")) {
          //console.log("weemo button clicked");
          var targetUser = jqchat(this).attr("data-username");
          var targetFullname = jqchat(this).attr("data-fullname");
          weemoExtension.createWeemoCall(targetUser, targetFullname);
        }
      });

      jqchat(".chatPopupOverlay").on("click", function() {
        if (!jqchat(this).hasClass("disabled")) {
          //console.log("weemo button clicked");
          var targetUser = jqchat(this).attr("data-username");
          showMiniChatPopup(targetUser,'username');
        }
      });

      function cbGetStatus(targetUser, status) {
        //console.log("Status :: target="+targetUser+" : status="+status);
        if (status !== "offline") {
          jqchat(".weemoCall-"+targetUser.replace('.', '-')).removeClass("disabled");
          jqchat(".chatPopup-"+targetUser.replace('.', '-')).removeClass("disabled");
        }
      }
      chatNotification.getStatus(username, cbGetStatus);


    }

  });

};

WeemoExtension.prototype.attachWeemoToConnections = function() {
  if (window.location.href.indexOf("/portal/intranet/connexions")==-1) return;

  var $uiPeople = jqchat('.uiTabInPage').first();
  if ($uiPeople.html() === undefined) {
    setTimeout(jqchat.proxy(this.attachWeemoToConnections, this), 250);
    return;
  }

  function cbGetConnectionStatus(targetUser, status) {
    //console.log("Status :: target="+targetUser+" : status="+status);
    if (status !== "offline") {
      jqchat(".weemoCall-"+targetUser.replace('.', '-')).removeClass("disabled");
    }
  }

  jqchat(".contentBox", ".uiTabInPage").each(function() {
    var $uiUsername = jqchat(this).children(".spaceTitle").children("a").first();
    var username = $uiUsername.attr("href");
    username = username.substring(username.lastIndexOf("/")+1);
    var fullname = $uiUsername.html();

    var $uiActionWeemo = jqchat(".weemoCallOverlay", this).first();
    if ($uiActionWeemo !== undefined && $uiActionWeemo.html() == undefined) {
      var html = jqchat(this).html();
      html += '<a type="button" class="btn weemoCallOverlay weemoCall-'+username.replace('.', '-')+' pull-right disabled" id="weemoCall-'+username.replace('.', '-')+'" title="Make a Video Call"';
      html += ' data-username="'+username+'" data-fullname="'+fullname+'"';
      html += ' style="margin-left:5px;"><i class="icon-facetime-video"></i> Call</a>';
      jqchat(this).html(html);

      chatNotification.getStatus(username, cbGetConnectionStatus);
    }

  });


  jqchat(".weemoCallOverlay").on("click", function() {
    if (!jqchat(this).hasClass("disabled")) {
      //console.log("weemo button clicked");
      var targetUser = jqchat(this).attr("data-username");
      var targetFullname = jqchat(this).attr("data-fullname");
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



/**
 ##################                           ##################
 ##################                           ##################
 ##################   GLOBAL                  ##################
 ##################                           ##################
 ##################                           ##################
 */

// GLOBAL VARIABLES
var chatNotification = new ChatNotification();
var weemoExtension = new WeemoExtension();


(function($) {

  $(document).ready(function() {
    //GETTING DOM CONTEXT
    var $notificationApplication = $("#chat-status");
    // CHAT NOTIFICATION INIT
    chatNotification.initOptions({
      "token": $notificationApplication.attr("data-token"),
      "username": $notificationApplication.attr("data-username"),
      "urlInitUserProfile": $notificationApplication.jzURL("NotificationApplication.initUserProfile"),
      "urlNotification": $notificationApplication.attr("data-chat-server-url")+"/notification",
      "urlGetStatus": $notificationApplication.attr("data-chat-server-url")+"/getStatus",
      "urlSetStatus": $notificationApplication.attr("data-chat-server-url")+"/setStatus",
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

    $(".chat-status").on("click", function() {
      var status = $(this).attr("data-status");
      chatNotification.setStatus(status);
    });

    $(".uiNotifChatIcon").on("click", function() {
      console.log("NEED TO REFRESH NOTIFICATIONS");
      chatNotification.refreshNotifDetails();
    });

  });

})(jqchat);


/**
 * Weemo WebRTC Video Div : support for draggable div video
 * Call method addWeemoDragListeners() after div #weemo-videobox is created
 */
var offX;
var offY;

function addWeemoDragListeners() {
  document.getElementById('weemo-videobox').addEventListener('mousedown', mouseDown, false);
  window.addEventListener('mouseup', mouseUp, false);
}

function mouseUp() {
  window.removeEventListener('mousemove', divMove, true);
}

function mouseDown(e) {
  var div = document.getElementById('weemo-videobox');
  offY= e.clientY-parseInt(div.offsetTop);
  offX= e.clientX-parseInt(div.offsetLeft);
  window.addEventListener('mousemove', divMove, true);
}

function divMove(e) {
  var div = document.getElementById('weemo-videobox');
  div.style.position = 'absolute';
  div.style.top = (e.clientY-offY) + 'px';
  div.style.left = (e.clientX-offX) + 'px';
}
