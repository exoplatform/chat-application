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
  this.sessionId = "";
  this.shortSpaceName = ""; // short Name of current space being in
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

  this.tiptipContentDOMNodeInsertedHandler = function() {
    chatNotification.attachChatButtonToUserPopup();
  };
}

/**
 * Init Notifications variables
 * @param options
 */
ChatNotification.prototype.initOptions = function(options) {
  this.token = options.token;
  this.username = options.username;
  this.sessionId = options.sessionId;
  this.jzInitUserProfile = options.urlInitUserProfile;
  this.jzNotification = options.urlNotification;
  this.jzGetStatus = options.urlGetStatus;
  this.jzSetStatus = options.urlSetStatus;
  this.chatIntervalNotif = options.notificationInterval;
  this.chatIntervalStatus = options.statusInterval;
  this.notifEventURL = this.jzNotification+'?user='+this.username+'&token='+this.token;
  this.shortSpaceName = options.shortSpaceName;
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
ChatNotification.prototype.refreshNotifDetails = function(callback) {
  var $chatNotificationsDetails = jqchat("#chat-notifications-details");

  if (this.oldNotifTotal>0) {
    $chatNotificationsDetails.css("display", "initial");
    if (jqchat(".chat-notification-loading", $chatNotificationsDetails).length > 0) {
      $chatNotificationsDetails.next().show();
    }

    this.updateNotifEventURL();
    jqchat.ajax({
      url: this.notifEventURL+"&withDetails=true",
      dataType: "json",
      context: this,
      success: function(data){
        var html = '';
        var categoryIdList = new Array(); // Only display last unread messages from different conversations
        if (data.notifications.length>0) {
          var notifs = TAFFY(data.notifications);
          var notifs = notifs();
          var thiss = this;
          var froms = [];
          notifs.order("timestamp desc").each(function (notif, number) {
            if (jqchat.inArray(notif.categoryId, categoryIdList) === -1) {
              var content = notif.content;
              var messageType = notif.options.type;
              var evenClass = (categoryIdList.length % 2) ? "even": "";


              html += '<div class="chat-notification-detail ' + evenClass + '" data-link="' + notif.link + '" data-id="' + notif.categoryId + '" >';
              html +=   '<span class="avatarXSmall">';
              html +=     '<img onerror="this.src=\'/chat/img/user-default.jpg\'" src=\'/rest/chat/api/1.0/user/getAvatarURL/'+notif.from+'\' class="avatar-image">';
              html +=   '</span>';
              html += '  <div class="chat-label-status">';
              html += '    <div class="content">';
              html += '      <span class="name" href="#">' + notif.fromFullName + '</span>';
              html += '      <div class="text">';

              // Icon for system message
              if (messageType == undefined) {
                if (content.indexOf("http:")===0 || content.indexOf("https:")===0 || content.indexOf("ftp:")===0) {
                  content = "<a href='#'>" + content + "</a>";
                }
              } else {
                if ("type-question" === messageType) {
                  html += "       <i class='uiIconChatQuestion uiIconChatLightGray'></i>";
                } else if ("type-hand" === messageType) {
                  html += "       <i class='uiIconChatRaiseHand uiIconChatLightGray'></i>";
                } else if ("type-file" === messageType) {
                  html += "       <i class='uiIconChatUpload uiIconChatLightGray'></i>";
                } else if ("type-link" === messageType) {
                  html += "       <i class='uiIconChatLink uiIconChatLightGray'></i>";
                  content = notif.options.link;
                } else if ("type-task" === messageType) {
                  html += "       <i class='uiIconChatCreateTask uiIconChatLightGray'></i>";
                } else if ("type-event" === messageType) {
                  html += "       <i class='uiIconChatCreateEvent uiIconChatLightGray'></i>";
                } else if ("type-notes" === messageType) {
                  html += "       <i class='uiIconChatMeeting uiIconChatLightGray'></i>";
                  content = chatBundleData.exoplatform_chat_notes_saved;
                } else if ("type-meeting-start" === messageType) {
                  html += "       <i class='uiIconChatMeeting uiIconChatLightGray'></i>";
                  content = chatBundleData.exoplatform_chat_meeting_started;
                } else if ("type-meeting-stop" === messageType) {
                  html += "       <i class='uiIconChatMeeting uiIconChatLightGray'></i>";
                  content = chatBundleData.exoplatform_chat_meeting_finished;
                } else if ("type-add-team-user" === messageType) {
                  content = chatBundleData.exoplatform_chat_team_msg_adduser.replace("{0}", notif.options.fullname).replace("{1}", notif.options.users);
                } else if ("type-remove-team-user" === messageType) {
                  content = chatBundleData.exoplatform_chat_team_msg_removeuser.replace("{0}", notif.options.fullname).replace("{1}", notif.options.users);
                } else if ("call-join" === messageType) {
                  html += "       <i class='uiIconChatAddPeopleToMeeting uiIconChatLightGray'></i>";
                  content = chatBundleData.exoplatform_chat_meeting_joined;
                } else if ("call-on" === messageType) {
                  html += "       <i class='uiIconChatStartCall uiIconChatLightGray'></i>";
                  content = chatBundleData.exoplatform_chat_meeting_started;
                } else if ("call-off" === messageType) {
                  html += "       <i class='uiIconChatFinishCall uiIconChatLightGray'></i>";
                  content = chatBundleData.exoplatform_chat_meeting_finished;
                }
                content = "<a href='#'>" + content + "</a>";
              }

              html +=          content;
              html += '      </div>';
              html += '    </div>';
              html += '    <div class="gray-box">';
              html += '      <div class="timestamp">' + thiss.getDate(notif.timestamp) + '</div>';
              if (notif.roomDisplayName.trim()) {
                html += '    <div class="team">' + notif.roomDisplayName + '</div>';
              }
              html += '    </div>';
              html += '  </div>';
              html += '</div>';

              if (fromChromeApp) {
                if (thiss.profileStatus !== "donotdisturb" && thiss.profileStatus !== "offline") {
                  doSendMessage(notif);
                }
              }

              categoryIdList.push(notif.categoryId);
            }
          });
        }
        $chatNotificationsDetails.html(html);
        if (categoryIdList.length > 0) {
          $chatNotificationsDetails.parent().addClass("full-width");
          $chatNotificationsDetails.next().show();
        } else {
          $chatNotificationsDetails.parent().removeClass("full-width");
          $chatNotificationsDetails.next().hide();
        }
        $chatNotificationsDetails.css("display", "block");
        jqchat(".chat-notification-detail").on("click", function(){
          var id = jqchat(this).attr("data-id");
          showMiniChatPopup(id, "room-id");
        });

        if (typeof callback === "function") {
          callback();
        }
      },
      error: function(){
        $chatNotificationsDetails.html("");
        $chatNotificationsDetails.css("display", "none");

        if (typeof callback === "function") {
          callback();
        }
      }
    });
  }
  else {
    $chatNotificationsDetails.parent().removeClass("full-width");
    $chatNotificationsDetails.next().hide();

    if (typeof callback === "function") {
      callback();
    }
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
          $chatNotificationsDetails.html('<span class="chat-notification-loading no-user-selection">'+chatBundleData.exoplatform_chat_loading+'</span>');
          $chatNotificationsDetails.parent().removeClass("full-width");
          $chatNotificationsDetails.next().hide();
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

  // Update chat status on chatApplication
  var $chatStatusChat = jqchat(".chat-status-chat");
  $chatStatusChat.removeClass("chat-status-available");
  $chatStatusChat.removeClass("chat-status-donotdisturb");
  $chatStatusChat.removeClass("chat-status-invisible");
  $chatStatusChat.removeClass("chat-status-away");
  $chatStatusChat.removeClass("chat-status-offline");
  $chatStatusChat.addClass("chat-status-"+status);

  jqchat(".chat-status-selected").each(function () {
    var labelStatus = jqchat(this).parent(".chat-status").attr("data-status");
    if (labelStatus === status) {
      jqchat(this).html("&#10003;");
    }
    else
    {
      jqchat(this).html("");
    }
  });

  // Update chat status on top navigation
  var $uiNotifChatIcon = jqchat(".uiNotifChatIcon");
  $uiNotifChatIcon.removeClass("toggle-status-available");
  $uiNotifChatIcon.removeClass("toggle-status-away");
  $uiNotifChatIcon.removeClass("toggle-status-donotdisturb");
  $uiNotifChatIcon.removeClass("toggle-status-invisible");
  $uiNotifChatIcon.addClass("toggle-status-" + status);
};

ChatNotification.prototype.openChatPopup = function() {
  window.open(this.chatPage+"?noadminbar=true","chat-popup","menubar=no, status=no, scrollbars=no, titlebar=no, resizable=no, location=no, width=700, height=600");
};

ChatNotification.prototype.attachChatButtonToUserPopup = function() {
  var $tiptip_content = jqchat("#tiptip_content");
  if ($tiptip_content.length == 0 || $tiptip_content.hasClass("DisabledEvent")) {
    //setTimeout(chatNotification.attachChatButtonToUserPopup(), 250);
    setTimeout(jqchat.proxy(this.attachChatButtonToUserPopup, this), 250);
    return;
  }

  $tiptip_content.addClass("DisabledEvent");
  var $uiAction = jqchat(".uiAction", $tiptip_content);
  var $btnChat = jqchat(".chatPopupOverlay", $uiAction);
  if ($uiAction.length > 0 && $btnChat.length === 0) {
    var toUserName = jqchat("[href^='/portal/intranet/activities/']", $tiptip_content).first().attr("href").substr(28);
    var toFullName = jqchat("[href^='/portal/intranet/activities/']", $tiptip_content).last().html();
    var strChatLink = "<a style='margin-left:5px;' data-username='" + toUserName + "' title='Chat' class='btn chatPopupOverlay chatPopup-" + toUserName.replace('.', '-') + " disabled' type='button'><i class='uiIconForum uiIconLightGray'></i> Chat</a>";
    var strWeemoLink = '<a type="button" class="btn weemoCallOverlay weemoCall-'+toUserName.replace('.', '-')+' pull-right disabled" id="weemoCall-'+toUserName.replace('.', '-')+'" title="'+chatBundleData.exoplatform_videocall_makeCall+ '" data-username="'+toUserName+'" data-fullname="'+toFullName+'" style="margin-left:5px; display:none;"><i class="uiIconWeemoVideoCalls uiIconLightGray"></i> '+chatBundleData.exoplatform_videocall_Call+'</a>';

    // Position of chat button depend on weemo installation
    var $btnWeemoCall = jqchat(".weemoCallOverlay", $uiAction);
    if ($btnWeemoCall.length > 0) {
      var $btnConnect = jqchat(".connect", $uiAction);
      $btnConnect.wrap("<div></div>");
      $uiAction.addClass("twice-line");
    }
    $uiAction.append(strChatLink);

    jqchat(".chatPopupOverlay").on("click", function() {
      if (!jqchat(this).hasClass("disabled")) {
        var targetUser = jqchat(this).attr("data-username");
        showMiniChatPopup(targetUser,'username');
      }
    });

    function cbGetStatus(targetUser, status) {
      if (status !== "offline") {
        jqchat(".chatPopup-"+targetUser.replace('.', '-')).removeClass("disabled");
      }
    }
    chatNotification.getStatus(toUserName, cbGetStatus);
  }

  $tiptip_content.removeClass("DisabledEvent");
  $tiptip_content.unbind("DOMNodeInserted", this.tiptipContentDOMNodeInsertedHandler);
  $tiptip_content.bind('DOMNodeInserted', this.tiptipContentDOMNodeInsertedHandler);
};

ChatNotification.prototype.attachChatButtonBelowLeftNavigationSpaceName = function() {
  var $uiBreadcumbsNavigationPortlet = jqchat("#UIBreadCrumbsNavigationPortlet");
  if ($uiBreadcumbsNavigationPortlet.length == 0) {
    setTimeout(chatNotification.attachChatButtonBelowLeftNavigationSpaceName, 250);
    return;
  }

  var $breadcumbEntry = jqchat(".breadcumbEntry", $uiBreadcumbsNavigationPortlet);
  var $btnChat = jqchat(".chat-button", $breadcumbEntry);
  var spaceName = this.shortSpaceName;
  if ($breadcumbEntry.length > 0 && $btnChat.length === 0 && spaceName !== "") {
    var strChatLink = "<a onclick='javascript:showMiniChatPopup(\"" + spaceName + "\", \"space-name\");' class='chat-button actionIcon' href='javascript:void();'><span class='uiIconChatChat uiIconChatLightGray'></span><span class='chat-label-status'>&nbsp;Chat</span></a>";
    $breadcumbEntry.append(strChatLink);
  }

  $uiBreadcumbsNavigationPortlet.one('DOMNodeInserted', function() {
    chatNotification.attachChatButtonBelowLeftNavigationSpaceName();
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

(function($) {

  $(document).ready(function() {
    //GETTING DOM CONTEXT
    var $notificationApplication = $("#chat-status");
    // CHAT NOTIFICATION INIT
    chatNotification.initOptions({
      "token": $notificationApplication.attr("data-token"),
      "username": $notificationApplication.attr("data-username"),
      "sessionId":$notificationApplication.attr("data-session-id"),
      "urlInitUserProfile": $notificationApplication.jzURL("NotificationApplication.initUserProfile"),
      "urlNotification": $notificationApplication.attr("data-chat-server-url")+"/notification",
      "urlGetStatus": $notificationApplication.attr("data-chat-server-url")+"/getStatus",
      "urlSetStatus": $notificationApplication.attr("data-chat-server-url")+"/setStatus",
      "notificationInterval": $notificationApplication.attr("data-chat-interval-notif"),
      "statusInterval": $notificationApplication.attr("data-chat-interval-status"),
      "shortSpaceName": $notificationApplication.attr("data-short-space-name")
    });
    // CHAT NOTIFICATION USER INTERFACE PREPARATION
    chatNotification.initUserInterface();

    chatNotification.initUserProfile();

    $(".chat-status").on("click", function() {
      var status = $(this).attr("data-status");

      chatNotification.setStatus(status);
    });

    $(".uiNotifChatIcon").click( function(e) {
      console.log("NEED TO REFRESH NOTIFICATIONS");
      if (!$(this).hasClass("disabled")) {
        $(this).addClass("disabled");
        chatNotification.refreshNotifDetails(function () {
          $(".uiNotifChatIcon").removeClass("disabled");
        });
        chatNotification.changeStatusChat(chatNotification.profileStatus);
      }
    });

    // Attach chat to user popup
    chatNotification.attachChatButtonToUserPopup();

    // Attach chat below left navigation space name
    chatNotification.attachChatButtonBelowLeftNavigationSpaceName();

  });

})(jqchat);


var offX;
var offY;

