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
  this.spaceId = ""; // Id of current space being in
  this.jzInitUserProfile = "";
  this.jzNotification = "";
  this.jzGetStatus = "";

  this.wsEndpoint = "";
  this.cometdToken = "";

  this.dbName = "";

  this.oldNotifTotal = 0;
  this.profileStatus = "offline";

  this.chatPage = "";

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
  this.dbName = options.dbName;
  this.spaceId = options.spaceId;
  this.plfUserStatusUpdateUrl = options.plfUserStatusUpdateUrl;
  this.jzChatRead = options.jzChatRead;
  this.jzChatSend = options.jzChatSend;
  this.portalURI = options.portalURI;
  this.chatPage = this.portalURI + "/chat";
  this.wsEndpoint = options.wsEndpoint;
  this.cometdToken = options.cometdToken;
};

/**
 * Create the User Interface in the Intranet DOM
 */
ChatNotification.prototype.initUserInterface = function() {
  jqchat(".uiCompanyNavigations > li")
    .children()
    .filter(function() {
      if (jqchat(this).attr("href") == (chatNotification.portalURI + "chat")) {
        jqchat(this).css("width", "95%");
        var html = '<i class="uiChatIcon"></i>Chat';
        //html += '<span id="chat-notification" style="float: right; display: none;"></span>';
        jqchat(this).html(html);
      }
    });
};

ChatNotification.prototype.getNotifEventURL = function() {
  return (this.jzNotification+'?user='+this.username+'&dbName='+this.dbName);
};

/**
 * Init Chat User Profile
 * @param callback : allows you to call an async callback function(username, fullname) when the profile is initiated.
 */
ChatNotification.prototype.initUserProfile = function() {

  jqchat.ajax({
    url: this.jzInitUserProfile,
    dataType: "json",
    context: this,
    success: function(data){
      this.token = data.token;

      if(typeof chatApplication === "undefined") {
        jqchat.ajax({
          url: this.getNotifEventURL() + "&withDetails=true",
          headers: {
            'Authorization': 'Bearer ' + this.token
          },
          dataType: "json",
          context: this,
          success: function(data){
            if (data.notifications.length > 0) {
              var total = Math.abs(data.notifications.length);

              var $chatNotification = jqchat("#chat-notification");
              if (total > 0) {
                if(desktopNotification.canShowOnSiteNotif()) {
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
      }

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
ChatNotification.prototype.showDetail = function(callback) {
  var $chatNotificationsDetails = jqchat("#chat-notifications-details");

  if (this.oldNotifTotal > 0) {
    $chatNotificationsDetails.css("display", "initial");
    if (jqchat(".chat-notification-loading", $chatNotificationsDetails).length > 0) {
      $chatNotificationsDetails.next().show();
    }

    jqchat.ajax({
      url: this.getNotifEventURL() + "&withDetails=true",
      headers: {
        'Authorization': 'Bearer ' + this.token
      },
      dataType: "json",
      context: this,
      success: function(data){
        var html = '';
        var categoryIdList = new Array(); // Only display last unread messages from different conversations
        if (data.notifications.length > 0) {
          var notifs = TAFFY(data.notifications);
          notifs = notifs();
          var thiss = this;
          var froms = [];
          notifs.order("timestamp desc").each(function (notif, number) {
            if (jqchat.inArray(notif.categoryId, categoryIdList) === -1) {
              var content = notif.content;
              var messageType = notif.options.type;

              html += '<div class="chat-notification-detail block-item ' + (categoryIdList.length % 2 ? 'even': '') + '" data-link="' + notif.link + '" data-id="' + notif.categoryId + '" >';
              html +=   '<span class="avatarXSmall">';
              html +=     '<img onerror="this.src=\'/chat/img/user-default.jpg\'" src=\'/rest/v1/social/users/'+notif.from+'/avatar\' class="avatar-image">';
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
 * Show desktop Notif
 */
ChatNotification.prototype.showDesktopNotif = function(path, msg) {
  var displayMsg = desktopNotification.highlightMessage(msg);
  if(Notification.permission !== "granted")
    Notification.requestPermission();

  if (!Notification) {
    alert('Desktop notifications not available in your browser. Please update your browser.');
    return;
  }

  if(Notification.permission !== "granted")
    Notification.requestPermission();
    else {
    var isFirefox = typeof InstallTrigger !== 'undefined';
    var isLinux = ( navigator.platform.indexOf('Linux') != -1 );

    var clickHandler = function(notif) {
      notif.onclick = function() {
        window.focus();
        notif.close();
        var displayTitle = msg.roomDisplayName;

        if(typeof chatApplication === "undefined") {
          localStorage.setItem('notification.room', msg.categoryId);
          window.open(path, "_chat");
        } else {
          // TODO Need to handle the case in the full chat app.
          chatApplication.loadRoom(msg.categoryId);

          if (chatApplication.isMobileView()) {
            jqchat(".right-chat").css("display", "block");
            jqchat(".left-chat").css("display", "none");
            jqchat(".room-name").html(displayTitle);
          }
        }
      }
    };

    var notification =null;
    // check if we're running Firefox on Linux then disable the Icons
    // bug firefox on Linux : https://bugzilla.mozilla.org/show_bug.cgi?id=1295974
    if (isLinux && isFirefox) {
      notification = new Notification(msg.roomDisplayName, {
        body: displayMsg
      });
      clickHandler(notification);

    } else {
      var avatarUrl = null;

      if (msg.roomType === 'u') {
        avatarUrl = '/rest/v1/social/users/' + msg.from + '/avatar';
      } else {
        avatarUrl = '/rest/v1/social/spaces/' + msg.roomDisplayName + '/avatar';
      }

      jqchat.ajax({
        url: avatarUrl,
        type: 'GET',
        complete: function (xhr) {
          if (xhr.status === 404) {
            if (msg.roomType == "u") {  // default user avatar
              avatarUrl = '/chat/img/user-default.jpg';
            } else {  // default group avatar
              avatarUrl = '/eXoSkin/skin/images/themes/default/social/skin/ShareImages/SpaceAvtDefault.png';
            }
          }

          notification = new Notification(msg.roomDisplayName, {
            icon: avatarUrl,
            body: displayMsg,
          });
          clickHandler(notification);
        }
      });
    }
  }
};

/**
 * Refresh Current User Status
 */
ChatNotification.prototype.refreshStatusChat = function() {
  this.getStatus(this.username, this.changeStatusChat)
};

/**
 * Gets target user status
 * @param targetUser
 */
ChatNotification.prototype.getStatus = function(targetUser, callback) {
  if(this.statusRequest) {
    return;
  }
  var thiss = this;
  this.statusRequest = jqchat.ajax({
    url: this.jzGetStatus,
    data: {
      "user": this.username,
      "targetUser": targetUser,
      "dbName": this.dbName 
    },
    headers: {
      'Authorization': 'Bearer ' + this.token
    },
    context: this,
    success: function(response){
      thiss.statusRequest = null;
      if (typeof callback === "function") {
        callback(response);
      }
    },
    error: function(response){
      thiss.statusRequest = null;
      if (typeof callback === "function") {
        callback("offline");
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
    chatNotification.changeStatusChat(status);

    // Send update status message (forward event to others client and update mongodb chat status)
    var thiss = this;
    require(['SHARED/commons-cometd3'], function(cCometD) {
      cCometD.publish('/service/chat', JSON.stringify({
        "event": "user-status-changed",
        "sender": thiss.username,
        "room": thiss.username,
        "dbName": thiss.dbName,
        "token": thiss.token,
        "data": {
          "status": status
        }
      }));
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
  chatNotification.profileStatus = status;
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
    // Position of chat button depend on weemo installation
    var $btnWeemoCall = jqchat(".weemoCallOverlay", $uiAction);
    if ($btnWeemoCall.length > 0) {
      var $btnConnect = jqchat(".connect", $uiAction);
      $btnConnect.wrap("<div></div>");
      $uiAction.addClass("twice-line");
    }
    var href = jqchat("#tipName a", $tiptip_content).first().attr("href");
    var toUserName = href.substr(href.lastIndexOf('/') + 1);
    var toFullName = jqchat("#tipName a", $tiptip_content).last().html();
    var strChatLink = "<a style='margin-left:5px;' data-username='" + toUserName + "' data-fullname='" + toFullName + "' title='Chat' class='btn chatPopupOverlay chatPopup-" + toUserName.replace('.', '-') + "' type='button'><i class='uiIconForum uiIconLightGray'></i> Chat</a>";
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
  var spaceId = this.spaceId;
  if ($breadcumbEntry.length > 0 && $btnChat.length === 0 && spaceId !== "") {
    var strChatLink = "<a onclick='javascript:showMiniChatPopup(\"" + spaceId + "\", \"space-id\");' class='chat-button actionIcon' href='javascript:void();'><span class='uiIconChatChat uiIconChatLightGray'></span><span class='chat-label-status'>&nbsp;Chat</span></a>";
    $breadcumbEntry.append(strChatLink);
  }

  $uiBreadcumbsNavigationPortlet.one('DOMNodeInserted', function() {
    chatNotification.attachChatButtonBelowLeftNavigationSpaceName();
  });
};

ChatNotification.prototype.attachChatToProfile = function() {
    if (window.location.href.indexOf(chatNotification.portalURI + "profile") == -1) return;

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
  jqchat.ajax({
    url: thiss.jzChatSend,
    data: {
      "user": user,
      "targetUser": targetUser,
      "room": room,
      "message": encodeURIComponent(msg),
      "options": encodeURIComponent(JSON.stringify(options)),
      "timestamp": new Date().getTime(),
      "isSystem": isSystemMessage
    },
    headers: {
      'Authorization': 'Bearer ' + token
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
    jqchat.ajax({
      url: this.jzChatRead,
      async: false,
      data: {
        room: room,
        user: this.username
      },
      headers: {
        'Authorization': 'Bearer ' + this.token
      },
      success: function(data) {
        data = data.split("\t").join(" ");
        data = JSON.parse(data);

        if (typeof callback === "function") {
          callback(data.messages);
        }
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
      "statusInterval": $notificationApplication.attr("data-chat-interval-status"),
      "spaceId": $notificationApplication.attr("data-space-id"),
      "plfUserStatusUpdateUrl": $notificationApplication.attr("data-plf-user-status-update-url"),
      "dbName": $notificationApplication.attr("data-db-name"),
      "jzChatRead": $notificationApplication.attr("data-chat-server-url")+"/read",
      "jzChatSend": $notificationApplication.attr("data-chat-server-url")+"/send",
      "portalURI": $notificationApplication.attr("data-portal-uri"),
      //TODO ws endpoint must be dynamic, depending on the chat mode (1 or 2 servers)
      "wsEndpoint": window.location.protocol + "//" + window.location.hostname + (window.location.port ? ":" + window.location.port : "")  + "/cometd/cometd",
      "cometdToken": $notificationApplication.attr("data-cometd-token")
    });

    // init cometd connection and subscriptions
    require(['SHARED/commons-cometd3'], function(cCometD) {
      cCometD.configure({
        url: chatNotification.wsEndpoint,
        'exoId': chatNotification.username, // current username
        'exoToken': chatNotification.cometdToken // unique token for the current user, got by calling ContinuationService.getUserToken(currentUsername) on server side
      });

      cCometD.subscribe('/service/chat', null, function (event) {
        var message = event.data;
        if (typeof message != 'object') {
          message = JSON.parse(message);
        }

        // Do what you want with the message...
        if(message.event == 'user-status-changed') {
          if(message.room == chatNotification.username) {
            // update current user status
            chatNotification.changeStatusChat(message.data.status);
          }
        } else if (message.event == "message-sent") {
          if ((typeof chatApplication === "undefined" || chatApplication.chatRoom.id !== message.room) && chatNotification.username !== message.sender) {

            var msg = message.data;

            // A tip that helps making a tiny delay in execution of block code in the function,
            // to avoid concurrency issue in condition checking.
            setTimeout(function() {
              // Check if the message has been notified by other tab
              if (localStorage.getItem('lastNotify-' + message.room) === msg.msgId) {
                return;
              }
              localStorage.setItem('lastNotify-' + message.room, msg.msgId);

              var notify = {
                roomType: msg.roomType,
                options: msg.options,
                roomDisplayName: msg.roomDisplayName,
                content: msg.msg,
                categoryId: message.room,
                from: message.sender
              };

              if (( chatNotification.profileStatus !== "donotdisturb" || desktopNotification.canBypassDonotDistrub()) &&
                chatNotification.profileStatus !== "offline" && desktopNotification.canBypassRoomNotif(notify)) {

                if(desktopNotification.canPlaySound()){
                  document.getElementById("chat-audio-notif").play();
                }
                if(desktopNotification.canShowDesktopNotif()){
                  chatNotification.showDesktopNotif(chatNotification.chatPage, notify);
                }
              }
            });
          }
        } else if (message.event == "notification-count-updated") {
          var total = message.data.totalUnreadMsg;

          // Check if the current page is the full Chat application page
          if (typeof chatApplication !== "undefined") {
            if (total > 0) {
              document.title = "Chat (" + total + ")";
            } else {
              document.title = "Chat";
            }
          } else {
            chatNotification.oldNotifTotal = total;
            var $chatNotification = jqchat("#chat-notification");
            if (total > 0) {
              if(desktopNotification.canShowOnSiteNotif()) {
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
          }
        }
      });
    });

    // CHAT NOTIFICATION USER INTERFACE PREPARATION
    chatNotification.initUserInterface();

    chatNotification.initUserProfile();

    window.onload = function() {
        if (typeof chatApplication !== "undefined") {
            var nbrOfFullScreenChat = localStorage.getItem('nbrOfFullScreenChat');
            nbrOfFullScreenChat = JSON.parse(nbrOfFullScreenChat);

            if (nbrOfFullScreenChat == null) {
                localStorage.setItem('nbrOfFullScreenChat', "0");
            } else {
                nbrOfFullScreenChat++;
                localStorage.setItem('nbrOfFullScreenChat', JSON.stringify(nbrOfFullScreenChat));
            }

        } else {
            var nbrOfFullSocial = localStorage.getItem('nbrOfFullSocial');
            nbrOfFullSocial = JSON.parse(nbrOfFullSocial);

            if (nbrOfFullSocial == null) {
                localStorage.setItem('nbrOfFullSocial', "0");
            } else {
                nbrOfFullSocial++;
                localStorage.setItem('nbrOfFullSocial', JSON.stringify(nbrOfFullSocial));
            }

        }
    }


    window.onbeforeunload = function(e) {
        if (typeof chatApplication !== "undefined") {
            var nbrOfFullScreenChat = localStorage.getItem('nbrOfFullScreenChat');
            nbrOfFullScreenChat = JSON.parse(nbrOfFullScreenChat);

            if (nbrOfFullScreenChat != null && nbrOfFullScreenChat > 0) {
                nbrOfFullScreenChat--;
                localStorage.setItem('nbrOfFullScreenChat', JSON.stringify(nbrOfFullScreenChat));
            }

        } else {
            var nbrOfFullSocial = localStorage.getItem('nbrOfFullSocial');
            nbrOfFullSocial = JSON.parse(nbrOfFullSocial);

            if (nbrOfFullSocial != null && nbrOfFullSocial > 0) {
                nbrOfFullSocial--;
                localStorage.setItem('nbrOfFullSocial', JSON.stringify(nbrOfFullSocial));
            }

        }
    }


    $(".chat-status").on("click", function() {
      var status = $(this).attr("data-status");

      chatNotification.setStatus(status);
    });

    $(".uiNotifChatIcon").click( function(e) {
      console.log("NEED TO REFRESH NOTIFICATIONS");
      if (!$(this).hasClass("disabled")) {
        $(this).addClass("disabled");
        chatNotification.showDetail(function () {
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

