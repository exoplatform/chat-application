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
  this.jzChatRead = "";
  this.jzChatSend = "";
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
  this.dbName = "";

  this.oldNotifTotal = 0;
  this.profileStatus = "offline";

  this.chatPage = "/portal/intranet/chat";

  this.plfUserStatusUpdateUrl = "";

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
  this.dbName = options.dbName;
  this.notifEventURL = this.jzNotification+'?user='+this.username+'&token='+this.token+'&dbName='+this.dbName;
  this.shortSpaceName = options.shortSpaceName;
  this.plfUserStatusUpdateUrl = options.plfUserStatusUpdateUrl;
  this.jzChatRead = options.jzChatRead;
  this.jzChatSend = options.jzChatSend;
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
  this.notifEventURL = this.jzNotification+'?user='+this.username+'&token='+this.token+'&dbName='+this.dbName;
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


              html += '<div class="chat-notification-detail block-item ' + evenClass + '" data-link="' + notif.link + '" data-id="' + notif.categoryId + '" >';
              html +=   '<span class="avatarXSmall">';
              html +=     '<img onerror="this.src=\'/chat/img/user-default.jpg\'" src=\'/rest/chat/api/1.0/user/getAvatarURL/'+notif.from+'\' class="avatar-image">';
              html +=   '</span>';
              html += '  <div class="chat-label-status">';
              html += '    <div class="content">';
              html += '      <span class="name text-link" href="#">' + notif.fromFullName + '</span>';
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
                  content = chatBundleData["exoplatform.chat.notes.saved"];
                } else if ("type-meeting-start" === messageType) {
                  html += "       <i class='uiIconChatMeeting uiIconChatLightGray'></i>";
                  content = chatBundleData["exoplatform.chat.meeting.started"];
                } else if ("type-meeting-stop" === messageType) {
                  html += "       <i class='uiIconChatMeeting uiIconChatLightGray'></i>";
                  content = chatBundleData["exoplatform.chat.meeting.finished"];
                } else if ("type-add-team-user" === messageType) {
                  content = chatBundleData["exoplatform.chat.team.msg.adduser"].replace("{0}", notif.options.fullname).replace("{1}", notif.options.users);
                } else if ("type-remove-team-user" === messageType) {
                  content = chatBundleData["exoplatform.chat.team.msg.removeuser"].replace("{0}", notif.options.fullname).replace("{1}", notif.options.users);
                } else if ("call-join" === messageType) {
                  html += "       <i class='uiIconChatAddPeopleToMeeting uiIconChatLightGray'></i>";
                  content = chatBundleData["exoplatform.chat.meeting.joined"];
                } else if ("call-on" === messageType) {
                  html += "       <i class='uiIconChatStartCall uiIconChatLightGray'></i>";
                  content = chatBundleData["exoplatform.chat.meeting.started"];
                } else if ("call-off" === messageType) {
                  html += "       <i class='uiIconChatFinishCall uiIconChatLightGray'></i>";
                  content = chatBundleData["exoplatform.chat.meeting.finished"];
                }
                content = "<a href='#'>" + content + "</a>";
              }

              html +=          content;
              html += '      </div>';
              html += '    </div>';
              html += '    <div class="gray-box">';
              html += '      <div class="timestamp time">' + thiss.getDate(notif.timestamp) + '</div>';
              if (notif.roomDisplayName.trim()) {
                html += '    <div class="team muted">' + notif.roomDisplayName + '</div>';
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
    url: this.notifEventURL+"&withDetails=true",
    dataType: "json",
    context: this,
    success: function(data){
      if(this.oldNotifTotal!=data.notifications.length){
        var notifyMe = false;
        data.notifications.sort(function(el1, el2){
          return el2.timestamp - el1.timestamp;
        });
        var lastMsg = data.notifications[0]; // the last one is at 0 index
        var total = Math.abs(data.notifications.length);
        if (total>this.oldNotifTotal && ( this.profileStatus !== "donotdisturb" || desktop.canIbypassDonotDistrub() ||  desktop.checkMention(this.username,lastMsg.content) ) && 
            this.profileStatus !== "offline" && desktop.canIbypassRoomNotif(lastMsg.content)) {
          notifyMe = true;
        }

        //console.log('refreshNotif :: '+total);
        var $chatNotification = jqchat("#chat-notification");
        if (total>0) {
          if(desktop.canIShowOnSiteNotif() && notifyMe) {
             $chatNotification.html('<span class="notif-total  badgeDefault badgePrimary mini">'+total+'</span>');
             $chatNotification.css('display', 'block');
          }
        } else {
          $chatNotification.html('<span></span>');
          $chatNotification.css('display', 'none');
          var $chatNotificationsDetails = jqchat("#chat-notifications-details");
          $chatNotificationsDetails.css("display", "none");
          $chatNotificationsDetails.html('<span class="chat-notification-loading no-user-selection">'+chatBundleData["exoplatform.chat.loading"]+'</span>');
          $chatNotificationsDetails.parent().removeClass("full-width");
          $chatNotificationsDetails.next().hide();
        }

        if(notifyMe) {
          if(desktop.canIPlaySound()){
            this.playNotifSound();
          }
          if(desktop.canIShowDesktopNotif()){
            this.showDesktopNotif(this.chatPage,total,lastMsg);
          }
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
 * Show desktop Notif
 */
ChatNotification.prototype.showDesktopNotif = function(path, nbrNotif, msg) {
  var displayMsg = desktop.highlightMessage(msg);
  if(Notification.permission !== "granted")
    Notification.requestPermission();

  if (!Notification) {
    alert('Desktop notifications not available in your browser. Please update your browser.');
    return;
  }

  if(Notification.permission !== "granted")
    Notification.requestPermission();
  else {
    var notification = new Notification('You have new notifications', {
      icon: '/rest/chat/api/1.0/user/getAvatarURL/'+msg.from,
      body: displayMsg ,
    });

    notification.onclick = function () {
      window.open(path);
    };
  }
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
      "timestamp": new Date().getTime(),
      "dbName": thiss.dbName 
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
      "targetUser": targetUser,
      "dbName": this.dbName 
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
    // Update mongo chat status

    jqchat.ajax({
      url: this.jzSetStatus,
      data: { "user": this.username,
        "token": this.token,
        "status": status,
        "timestamp": new Date().getTime(),
        "dbName": this.dbName
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

    // Update platform user status
    var url = this.plfUserStatusUpdateUrl + this.username  + "?status=" + status;
    jqchat.ajax({
      url: url,
      type: 'PUT',
      context: this,

      success: function(response){
      },
      error: function(response){
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
    var strChatLink = "<a style='margin-left:5px;' data-username='" + toUserName + "' data-fullname='" + toFullName + "' title='Chat' class='btn chatPopupOverlay chatPopup-" + toUserName.replace('.', '-') + "' type='button'><i class='uiIconForum uiIconLightGray'></i> Chat</a>";
    var strWeemoLink = '<a type="button" class="btn weemoCallOverlay weemoCall-'+toUserName.replace('.', '-')+' pull-right disabled" id="weemoCall-'+toUserName.replace('.', '-')+'" title="'+chatBundleData["exoplatform.videocall.makeCall"]+ '" data-username="'+toUserName+'" data-fullname="'+toFullName+'" style="margin-left:5px; display:none;"><i class="uiIconWeemoVideoCalls uiIconLightGray"></i> '+chatBundleData["exoplatform.videocall.Call"]+'</a>';

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
        var targetFullname = jqchat(this).attr("data-fullname");
        if(jqchat("#chat-application").length) {
          // we are in the chat application, load the one-to-one room with this user
          chatApplication.targetUser = targetUser;
          chatApplication.targetFullname = targetFullname;
          chatApplication.loadRoom();
        } else {
          // we are not in the chat application, open the mini-chat popup
          showMiniChatPopup(targetUser, 'username');
        }
        var popup = jqchat(this).closest('#tiptip_holder');
        popup.hide();
      }
    });
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

ChatNotification.prototype.attachChatToProfile = function() {
    if (window.location.href.indexOf("/portal/intranet/profile") == -1) return;

    var $UIStatusProfilePortlet = jqchat("#UIStatusProfilePortlet");
    if ($UIStatusProfilePortlet.html() === undefined) {
        setTimeout(jqchat.proxy(this.attachChatToProfile, this), 250);
        return;
    }

    var userName = jqchat(".user-status", $UIStatusProfilePortlet).attr('data-userid');
    var fullName = jqchat(".user-status span", $UIStatusProfilePortlet).text();
    var $userActions = jqchat("#UIActionProfilePortlet .user-actions");

    if (userName != chatNotification.username && userName !== "" && $userActions.has(".chatPopupOverlay").length === 0 && $userActions.has("button").length) {
        var strChatLink = "<a style='margin-top:0px !important;margin-right:-3px' data-username='" + userName + "' title='Chat' class='btn chatPopupOverlay chatPopup-" + userName.replace('.', '-') + "' type='button'><i class='uiIconChat uiIconForum uiIconLightGray'></i> Chat</a>";

        if ($userActions.has(".weemoCallOverlay").length === 0) {
            $userActions.prepend(strChatLink);
        } else {
            jqchat("a:first-child", $userActions).after(strChatLink);
        }

        jqchat(".chatPopupOverlay").on("click", function() {
            if (!jqchat(this).hasClass("disabled")) {
                var targetUser = jqchat(this).attr("data-username");
                showMiniChatPopup(targetUser, 'username');
            }
        });

        // Fix PLF-6493: Only let hover happens on connection buttons instead of all in .user-actions
        var $btnConnections = jqchat(".show-default, .hide-default", $userActions);
        var $btnShowConnection = jqchat(".show-default", $userActions);
        var $btnHideConnection = jqchat(".hide-default", $userActions);
        $btnShowConnection.show();
        $btnConnections.css('font-style', 'italic');
        $btnHideConnection.hide();
        $btnConnections.removeClass('show-default hide-default');
        $btnConnections.hover(function(e) {
          $btnConnections.toggle();
        });
    }

    setTimeout(function() {
        chatNotification.attachChatToProfile()
    }, 250);
};
ChatNotification.prototype.sendFullMessage = function(user, token, targetUser, room, msg, options, isSystemMessage, callback) {

// Send message to server
  var thiss = this;
  snack.request({
    url: thiss.jzChatSend,
    data: {
      "user": user,
      "targetUser": targetUser,
      "room": room,
      "message": encodeURIComponent(msg),
      "options": encodeURIComponent(JSON.stringify(options)),
      "token": token,
      "timestamp": new Date().getTime(),
      "isSystem": isSystemMessage
    }
  }, function (err, response) {
    if (!err) {
      if (typeof callback === "function") {
        callback();
      }
    }
  });
};



/**
 * return a status if a meeting is started or not :
 * -1 : no meeting in chat history
 * 0 : meeting terminated
 * 1 : obgoing meeting
 *
 * @param callback (callStatus)
 */
ChatNotification.prototype.checkIfMeetingStarted = function (room, callback) {
  chatNotification.getChatMessages(room, function (msgs) {
    var callStatus = -1; // -1:no call ; 0:terminated call ; 1:ongoing call
    var recordStatus = -1;
    for (var i = 0; i < msgs.length && callStatus === -1; i++) {
      var msg = msgs[i];
      var type = msg.options.type;
      if (type === "call-off") {
        callStatus = 0;
      } else if (type === "call-on") {
        callStatus = 1;
      }
    }
    for (var i = 0; i < msgs.length && recordStatus === -1; i++) {
      var msg = msgs[i];
      var type = msg.options.type;
      if (type === "type-meeting-stop") {
        recordStatus = 0;
      } else if (type === "type-meeting-start") {
        recordStatus = 1;
      }
    }
    if (callback !== undefined) {
      callback(callStatus, recordStatus);
    }
  });
};

ChatNotification.prototype.getChatMessages = function(room, callback) {
  if (room === "") return;

  if (this.username !== this.ANONIM_USER) {
    snack.request({
      url: this.jzChatRead,
            async: false,

      data: {
        room: room,
        user: this.username,
        token: this.token
      }
    }, function (err, res){
      if (err) {
        return;
      }

      res = res.split("\t").join(" ");
      var data = snack.parseJSON(res);

      if (typeof callback === "function") {
        callback(data.messages);
      }
    })
  }
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
      "shortSpaceName": $notificationApplication.attr("data-short-space-name"),
      "plfUserStatusUpdateUrl": $notificationApplication.attr("data-plf-user-status-update-url"),
      "dbName": $notificationApplication.attr("data-db-name"),
      "jzChatRead": $notificationApplication.attr("data-chat-server-url")+"/read",
      "jzChatSend": $notificationApplication.attr("data-chat-server-url")+"/send"
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

    // Attach chat to profile
    chatNotification.attachChatToProfile();

  });

})(jqchat);


var offX;
var offY;

