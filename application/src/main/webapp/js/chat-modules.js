
/**
 ##################                           ##################
 ##################                           ##################
 ##################   CHAT ROOM               ##################
 ##################                           ##################
 ##################                           ##################
 */

/**
 * ChatRoom allows to store and retrieve data from a room in the localStorage
 * and update the room when new data arrives on the server side.
 * @constructor
 */
function ChatRoom(jzChatRead, jzChatSend, jzChatGetRoom, jzChatUpdateUnreadMessages, jzChatSendMeetingNotes, jzChatGetMeetingNotes, chatIntervalChat, isPublic) {
  this.id = "";
  this.messages = "";
  this.jzChatRead = jzChatRead;
  this.jzChatSend = jzChatSend;
  this.jzChatGetRoom = jzChatGetRoom;
  this.jzChatUpdateUnreadMessages = jzChatUpdateUnreadMessages;
  this.jzChatSendMeetingNotes = jzChatSendMeetingNotes;
  this.jzChatGetMeetingNotes = jzChatGetMeetingNotes;
  this.chatEventInt = -1;
  this.chatIntervalChat = chatIntervalChat;
  this.username = "";
  this.token = "";
  this.targetUser = "";
  this.targetFullname = "";
  this.isPublic = isPublic;
  this.miniChat = undefined;

  this.ANONIM_USER = "__anonim_";

  this.onRefreshCB;
  this.onShowMessagesCB;

  this.highlight = "";

  this.startMeetingTimestamp = "";
  this.startCallTimestamp = "";
}


ChatRoom.prototype.init = function(username, token, targetUser, targetFullname, isAdmin, callback) {
  this.username = username;
  this.token = token;
  this.targetUser = targetUser;
  this.targetFullname = targetFullname;

  var thiss = this;
  snack.request({
    url: thiss.jzChatGetRoom,
    data: {"targetUser": targetUser,
      "user": username,
      "token": token,
      "isAdmin": isAdmin
    }
  }, function (err, response){
    if (!err) {
      thiss.id = response;

      if (typeof callback === "function") {
        callback(thiss.id);
      }

      jzStoreParam("lastUsername"+thiss.username, thiss.targetUser, 60000);
      jzStoreParam("lastFullName"+thiss.username, thiss.targetFullname, 60000);
      jzStoreParam("lastTS"+thiss.username, "0");
      thiss.chatEventInt = window.clearInterval(thiss.chatEventInt);
      thiss.chatEventInt = setInterval(jqchat.proxy(thiss.refreshChat, thiss), thiss.chatIntervalChat);
      thiss.refreshChat(false);
    }
  });

};

ChatRoom.prototype.onRefresh = function(callback) {
  this.onRefreshCB = callback;
};

ChatRoom.prototype.setMiniChatDiv = function(elt) {
  this.miniChat = elt;
};

ChatRoom.prototype.clearInterval = function() {
  this.chatEventInt = window.clearInterval(this.chatEventInt);
};

ChatRoom.prototype.onShowMessages = function(callback) {
  this.onShowMessagesCB = callback;
};

ChatRoom.prototype.sendMessage = function(msg, options, isSystemMessage, callback) {
  this.sendFullMessage(this.username, this.token, this.targetUser, this.id, msg, options, isSystemMessage, callback);
};

/**
 * Send message to server
 * @param message : the message to send
 * @param callback : the method to execute on success
 */
ChatRoom.prototype.sendFullMessage = function(user, token, targetUser, room, msg, options, isSystemMessage, callback) {

  // Update temporary message for smooth view
  if (room !== "" && room === this.id) {
    var im = this.messages.length;
    var tmpMessage = msg.replace(/&/g, "&#38");
    tmpMessage = tmpMessage.replace(/</g, "&lt;");
    tmpMessage = tmpMessage.replace(/>/g, "&gt;");
    tmpMessage = tmpMessage.replace(/\"/g, "&quot;");
    tmpMessage = tmpMessage.replace(/\n/g, "<br/>");
    tmpMessage = tmpMessage.replace(/\\\\/g, "&#92");
    tmpMessage = tmpMessage.replace(/\t/g, "  ");
    var tmpOptions = JSON.stringify(options);
    tmpOptions = tmpOptions.replace(/</g, "&lt;");
    tmpOptions = tmpOptions.replace(/>/g, "&gt;");
    tmpOptions = snack.parseJSON(tmpOptions);
    this.messages[im] = {"user": this.username,
      "fullname": chatBundleData.exoplatform_chat_you,
      "date": "pending",
      "message": tmpMessage,
      "options": tmpOptions,
      "isSystem": isSystemMessage};
    this.showMessages();
  }
  // Send message to server
  var thiss = this;
  snack.request({
    url: thiss.jzChatSend,
    data: {"user": user,
      "targetUser": targetUser,
      "room": room,
      "message": msg,
      "options": JSON.stringify(options),
      "token": token,
      "timestamp": new Date().getTime(),
      "isSystem": isSystemMessage
    }
  }, function (err, response){
    if (!err) {
      thiss.refreshChat();
      if (typeof callback === "function") {
        callback();
      }
    }
  });
};

/**
 * Refresh Chat : refresh messages and panels
 */
ChatRoom.prototype.refreshChat = function(forceRefresh, callback) {
  if (this.id === "") return;

  //var thiss = chatApplication;
  if (this.username !== this.ANONIM_USER) {
    var lastTS = jzGetParam("lastTS"+this.username);

    var thiss = this;
    snack.request({
      url: this.jzChatRead,
      data: {
        room: this.id,
        user: this.username,
        token: this.token
      }
    }, function (err, res){
      // check for an error
      if (err) {
        if (err === 403 &&  ( thiss.messages.length === 0 || "type-kicked" !== thiss.messages[0].options.type)) {

          // Show message user has been kicked
          var options = {
            "type" : "type-kicked"
          };
          var messages = [{
            "message": "",
            "options": options,
            "isSystem": "true"
          }];
          thiss.messages = messages;
          thiss.showMessages();

          // Disable action buttons
          var $msg = jqchat('#msg');
          var $msButtonRecord = jqchat(".msButtonRecord");
          var $msgEmoticons = jqchat(".msg-emoticons");
          var $meetingActionToggle = jqchat(".meeting-action-toggle");
          $msg.attr("disabled", "disabled");
          $msButtonRecord.attr("disabled", "disabled");
          $msButtonRecord.tooltip("disable");
          $msgEmoticons.parent().addClass("disabled");
          $msgEmoticons.parent().tooltip("disable");
          $meetingActionToggle.addClass("disabled");
          $meetingActionToggle.children("span").tooltip("disable");
        }
        if (typeof thiss.onRefreshCB === "function") {
          thiss.onRefreshCB(1);
        }
        return;
      } else if (thiss.miniChat === undefined) {
        chatApplication.activateRoomButtons();
      }


      res = res.split("\t").join(" ");
      // handle the response data
      var data = snack.parseJSON(res);
      var lastTS = jzGetParam("lastTS"+thiss.username);
      var lastUpdatedTS = jzGetParam("lastUpdatedTS"+thiss.username);
//      console.log("chatEvent :: lastTS="+lastTS+" :: serverTS="+data.timestamp);
      var im, message, out="", prevUser="";
      if (data.messages.length===0) {
        thiss.showMessages(data);
      } else {
        var ts = data.timestamp;
        var updatedTS = Math.max.apply(Math,TAFFY(data.messages)().select("lastUpdatedTimestamp").filter(Boolean));
        if (updatedTS < 0) updatedTS = 0;
        if (ts != lastTS || (forceRefresh === true) || (Number(lastUpdatedTS) != updatedTS)) {
          jzStoreParam("lastTS"+thiss.username, ts, 600);
          jzStoreParam("lastUpdatedTS"+thiss.username, updatedTS, 600);

          //console.log("new data to show");
          thiss.showMessages(data);
        }
      }

      if (typeof thiss.onRefreshCB === "function") {
        thiss.onRefreshCB(0);
      }

      if (typeof callback === "function") {
        callback(data.messages);
      }


    })

  }
};

ChatRoom.prototype.getChatMessages = function(room, callback) {
  if (room === "") return;

  if (this.username !== this.ANONIM_USER) {
    snack.request({
      url: this.jzChatRead,
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

ChatRoom.prototype.showAsText = function(callback) {

  var thiss = this;
  snack.request({
    url: thiss.jzChatRead,
    data: {
      room: thiss.id,
      user: thiss.username,
      token: thiss.token,
      isTextOnly: "true"
    }
  }, function (err, response){
    if (!err) {
      if (typeof callback === "function") {
        callback(response);
      }
    }
  });

};

ChatRoom.prototype.sendMeetingNotes = function(room, fromTimestamp, toTimestamp, callback) {
  var thiss = this;
  snack.request({
    url: thiss.jzChatSendMeetingNotes,
    data: {
      room: room,
      user: thiss.username,
      token: thiss.token,
      fromTimestamp: fromTimestamp,
      toTimestamp: toTimestamp
    }
  }, function (err, response){
    if (!err) {
      if (typeof callback === "function") {
        callback(response);
      }
    }
  });

};

ChatRoom.prototype.getMeetingNotes = function(room, fromTimestamp, toTimestamp, callback) {
  var serverBase = window.location.href.substr(0, 9+window.location.href.substr(9).indexOf("/"));

  var thiss = this;
  snack.request({
    url: thiss.jzChatGetMeetingNotes,
    data: {
      room: room,
      user: thiss.username,
      token: thiss.token,
      serverBase: serverBase,
      fromTimestamp: fromTimestamp,
      toTimestamp: toTimestamp
    }
  }, function (err, response){
    if (!err) {
      if (typeof callback === "function") {
        callback(response);
      }
    }
  });

};


/**
 * Show Messages (json to html)
 * @param msgs : json messages data to show
 */
ChatRoom.prototype.showMessages = function(msgs) {
  var im, message, out="", prevUser="", prevFullName, prevOptions, msRightInfo ="", msUserMes="";
  if (msgs!==undefined) {
    this.messages = msgs.messages;
  }

  if (this.messages.length===0) {
    if (this.isPublic) {
      out = "<div class='msRow' style='padding:22px 20px;'>";
      out += "<b><center>"+chatBundleData.exoplatform_chat_public_welcome+"</center></b>";
      out += "</div>";
    }
    else
      out += "<div class='noMessage'><span class='text'>" + chatBundleData.exoplatform_chat_no_messages + "</span></div>";

    // Set recorder button to start status
    if (this.miniChat === undefined) {
      chatApplication.updateMeetingButtonStatus('stopped');
    }
  } else {

    var messages = TAFFY(this.messages);
    var thiss = this;
    messages().order("timestamp asec").each(function (message, i) {

      if (message.isSystem!=="true")
      {
        if (prevUser != message.user)
        {
          if (prevUser !== "") {
            out += "        </div>";
            out += "      </div>";
            if (prevUser !== "__system")
              out += "    <div class='msUserAvatar'>";
          }
          if (message.user != thiss.username) {
            if (prevUser !== "") {
              if (prevUser !== "__system") {
                if (thiss.isPublic) {
                  out += "  <a class='msAvatarLink' href='#'><img src='/chat/img/support-avatar.png'></a>";
                } else {
                  out += "  <a class='msAvatarLink' href='/portal/intranet/profile/" + prevUser + "'><img onerror=\"this.src='/chat/img/user-default.jpg'\" src='/rest/chat/api/1.0/user/getAvatarURL/" + prevUser + "' alt='" + prevFullName + "'></a>";
                }
                out += "  </div>";
              } else {
                out += thiss.getActionMeetingStyleClasses(prevOptions);
              }
              out += "  </div>";
              out += "</div>";
            }
            out += "  <div class='msRow'>";
            out += "    <div class='msMessagesGroup clearfix'>";
            out += "      <div class='msContBox'>";
            out += "        <div class='inner'>";
            out += "          <div class='msTiltleLn clearfix'>";
            if (thiss.isPublic) {
              out += "          <a class='msNameUser muted' href='#'>" + chatBundleData.exoplatform_chat_support_fullname + "</a>";
            }
            else {
              out += "          <a class='msNameUser muted' href='/portal/intranet/profile/"+message.user+"'>" +message.fullname  + "</a>";
            }
            out += "          </div>";
          } else {
            if (prevUser !== "") {
              if (prevUser !== "__system") {
                out += "    <a class='msAvatarLink' href='/portal/intranet/profile/" + prevUser + "'><img onerror=\"this.src='/chat/img/user-default.jpg'\" src='/rest/chat/api/1.0/user/getAvatarURL/" + prevUser + "' alt='" + prevFullName + "'></a>";
                out += "  </div>";
              } else {
                out += thiss.getActionMeetingStyleClasses(prevOptions);
              }
              out += "  </div>";
              out += "</div>";
            }
            out += "  <div class='msRow rowOdd odd'>";
            out += "    <div class='msMessagesGroup clearfix'>";
            out += "      <div class='msContBox'>";
            out += "        <div class='inner'>";
            out += "          <div class='msTiltleLn clearfix'>";
            out += "            <a class='msNameUser muted' href='/portal/intranet/profile/"+message.user+"'>" +message.fullname  + "</a>";
            out += "          </div>";
          }
        }
        else
        {
//          out += "<hr style='margin:0px;'>";
        }
        var msgtemp = message.message;
        var noEditCssClass = "";
        if (message.type === "DELETED") {
          msgtemp = "<span class='contentDeleted empty'>"+chatBundleData.exoplatform_chat_deleted+"</span>";
          noEditCssClass = "noEdit";
        } else {
          msgtemp = thiss.messageBeautifier(message);
        }
        out += "            <div class='msUserCont msg-text clearfix " + noEditCssClass + "'>";

        msRightInfo = "";
        msRightInfo += "      <div class='msRightInfo pull-right'>";
        msRightInfo += "        <div class='msTimePost'>";
        if (message.type === "DELETED" || message.type === "EDITED") {
          msRightInfo += "        <span href='#' class='msEditMes'><i class='uiIconChatEdited uiIconChatLightGray'></i></span>";
        }
        msRightInfo += "          <span class='msg-date time'>" + thiss.getDate(message.timestamp) + "</span>";
        msRightInfo += "        </div>";
        if (message.type !== "DELETED") {
          msRightInfo += "      <div class='msAction msg-actions' style='visibility:hidden;'><span style='display: none;' class='msg-data' data-id='"+message.id+"' data-fn='"+message.fullname+"' data-timestamp='" + message.timestamp + "'>"+message.message+"</span>";
          msRightInfo += "        <a href='#' class='msg-action-savenotes'>" + chatBundleData.exoplatform_chat_notes + "</a> |";
          if (message.user === thiss.username) {
            msRightInfo += "      <a href='#' class='msg-action-edit'>" + chatBundleData.exoplatform_chat_edit + "</a> |";
            msRightInfo += "      <a href='#' class='msg-action-delete'>" + chatBundleData.exoplatform_chat_delete + "</a> |";
          }
          msRightInfo += "        <a href='#' class='msg-action-quote'>" + chatBundleData.exoplatform_chat_quote + "</a>";
          msRightInfo += "       </div>";
        }
        msRightInfo += "       </div>";
        msUserMes  = "         <div class='msUserMes'><span>" + msgtemp + "</span></div>";
        if (thiss.miniChat === undefined) {
          out += msRightInfo;
          out += msUserMes;
        } else {
          out += msUserMes;
          out += msRightInfo;
        }

        out += "            </div>";
        prevUser = message.user;
        prevFullName = message.fullname;
        prevOptions = message.options;

        if (i === (thiss.messages.length -1)) {
          out += "          </div>";
          out += "        </div>";
          out += "        <div class='msUserAvatar'>";
          if (thiss.isPublic) {
            out += "        <a class='msAvatarLink' href='#'><img src='/chat/img/support-avatar.png'></a>";
          } else {
            out += "        <a class='msAvatarLink' href='/portal/intranet/profile/" + prevUser + "'><img onerror=\"this.src='/chat/img/user-default.jpg'\" src='/rest/chat/api/1.0/user/getAvatarURL/" + prevUser + "' alt='" + prevFullName + "'></a>";
          }
          out += "        </div>";
          out += "      </div>";
          out += "    </div>";
        }
      }
      else
      {
        if (prevUser !== "") {
          out += "          </div>";
          out += "        </div>";
          if (prevUser !== "__system") {
            out += "      <div class='msUserAvatar'>";
            if (thiss.isPublic)
              out += "      <a class='msAvatarLink' href='#'><img src='/chat/img/support-avatar.png'></a>";
            else
              out += "      <a class='msAvatarLink' href='/portal/intranet/profile/" + prevUser + "'><img onerror=\"this.src='/chat/img/user-default.jpg'\" src='/rest/chat/api/1.0/user/getAvatarURL/" + prevUser + "' alt='" + prevFullName + "'></a>";
            out += "      </div>";
          } else {
            out += thiss.getActionMeetingStyleClasses(prevOptions);
          }
          out += "      </div>";
          out += "    </div>";
        }
        if (message.options !== undefined && message.options.type !== 'type-add-team-user' && message.options.type !=='type-remove-team-user'  && message.options.type !=='type-kicked' )
          out += "    <div class='msRow'>";
        else out += " <div class='msRow rowOdd odd'>";
        out += "        <div class='msMessagesGroup clearfix'>";
        out += "          <div class='msContBox'>";
        out += "            <div class='inner'>";
        if (message.options !== undefined && message.options.type !== 'type-add-team-user' && message.options.type !=='type-remove-team-user' && message.options.type !=='type-kicked'  ) {
          out += "            <div class='msTiltleLn clearfix'>";
          out += "              <a class='msNameUser muted' href='/portal/intranet/profile/"+message.user+"'>" +message.fullname  + "</a>";
          out += "            </div>";
        }
        out += "              <div class='msUserCont noEdit msg-text clearfix'>";
        msRightInfo = "";
        msRightInfo += "         <div class='msRightInfo pull-right'>";
        msRightInfo += "           <div class='msTimePost'>";
        msRightInfo += "             <span class='msg-date time'>" + thiss.getDate(message.timestamp) + "</span>";
        msRightInfo += "           </div>";
        msRightInfo += "         </div>";
        var options = {};

        if (typeof message.options == "object")
          options = message.options;
        var nbOptions = thiss.getObjectSize(options);

        if (options.type==="call-on") {
          if (options.timestamp!==undefined) {
            jzStoreParam("weemoCallHandlerFrom", message.timestamp, 600000);
            jzStoreParam("weemoCallHandlerOwner", message.user, 600000);
          }
        } else if (options.type==="call-off") {
          if (options.timestamp!==undefined) {
            jzStoreParam("weemoCallHandlerTo", message.timestamp, 600000);
          }
        }

        msUserMes = "            <div class='msUserMes'>" + thiss.messageBeautifier(message, options) + "</div>";
        if (thiss.miniChat === undefined) {
          out += msRightInfo;
          out += msUserMes;
        } else {
          out += msUserMes;
          out += msRightInfo;
        }

        if (options.type !== "call-join" && (options.type.indexOf('call-') !== -1)) {
          if (options.uidToCall!==undefined && options.displaynameToCall!==undefined) {
            if (typeof weemoExtension!=="undefined") {
              weemoExtension.setUidToCall(options.uidToCall);
              weemoExtension.setDisplaynameToCall(options.displaynameToCall);
            }
            jqchat(".btn-weemo").css("display", "none");
            jqchat(".btn-weemo-conf").css("display", "block");
            if (typeof weemoExtension!=="undefined") {
              if (options.uidToCall!=="weemo"+thiss.username && weemoExtension.isConnected)
                jqchat(".btn-weemo-conf").removeClass("disabled");
              else
                jqchat(".btn-weemo-conf").addClass("disabled");
            }
            else
              jqchat(".btn-weemo-conf").addClass("disabled");
          } else {
            jqchat(".btn-weemo").css("display", "block");
            jqchat(".btn-weemo-conf").css("display", "none");
          }
        }

//        if (options.type === "type-me") {
//          out += "<span class=\"system-event\">"+thiss.messageBeautifier(message, options)+"</span>";
//          out += "<div style='margin-left:50px;'>";
//        } else {
//// TODO: check to new BD
////          if (message.user != thiss.username) {
////            if (thiss.isPublic)
////              out += "<span class='invisible-text'>- </span><a href='#'>"+chatBundleData.exoplatform_chat_support_fullname+"</a><span class='invisible-text'> : </span><br/>";
////            else
////              out += "<a href='/portal/intranet/profile/"+message.user+"' class='user-link' target='_new'>"+message.fullname+"</a>";
////          } else {
////            out += "<span class='invisible-text'>- </span><a href='/portal/intranet/profile/"+message.user+"' class='user-link' target='_new'>"+message.fullname+"</a><span class='invisible-text'> : </span><br/>";
////          }
//          //out += "<div style='margin-left:50px;' class='msg-text'><span style='float:left' class=\"system-event\">"+thiss.messageBeautifier(message.message, options)+"</span>";
//
//        }

//        out +=  "<span class='invisible-text'> [</span>"+
//          "<span style='float:right;color:#CCC;font-size:10px'>"+thiss.getDate(message.timestamp)+"</span>" +
//          "<span class='invisible-text'>]</span></div>"+
//          "<div style='clear:both;'></div>";
//        out += "</span></div>";
//        out += "<hr style='margin: 0'>";
//        out += "<div><span>";
        out += "            </div>";
        prevUser = "__system";
        prevOptions = message.options;

        if (i === (thiss.messages.length -1)) {
          out += "          </div>";
          out += "        </div>";
          out += thiss.getActionMeetingStyleClasses(message.options);
          out += "      </div>";
          out += "    </div>";
        }
      }
    });

  }


  if (typeof this.onShowMessagesCB === "function") {
    this.onShowMessagesCB(out);
  }


};

ChatRoom.prototype.getActionMeetingStyleClasses = function(options) {
  var actionType = options.type;
  var out = "";

  if (actionType.indexOf("type-") !== -1 || actionType.indexOf("call-") !== -1) {
    out += "                <div class='msUserAvatar'>";
    if ("type-question" === actionType) {
      out += "                <i class='uiIconChat32x32Question uiIconChat32x32LightGray'></i>";
    } else if ("type-hand" === actionType) {
      out += "                <i class='uiIconChat32x32RaiseHand uiIconChat32x32LightGray'></i>";
    } else if ("type-file" === actionType) {
      out += "                <i class='uiIconChat32x32ShareFile uiIconChat32x32LightGray'></i>";
    } else if ("type-link" === actionType) {
      out += "                <i class='uiIconChat32x32HyperLink uiIconChat32x32LightGray'></i>";
    } else if ("type-task" === actionType) {
      out += "                <i class='uiIconChat32x32Task uiIconChat32x32LightGray'></i>";
    } else if ("type-event" === actionType) {
      out += "                <i class='uiIconChat32x32Event uiIconChat32x32LightGray'><span class='dayOnCalendar time'>" + options.startDate.substr(3, 2) + "</span></i>";
    } else if ("type-notes" === actionType || "type-meeting-start" === actionType || "type-meeting-stop" === actionType) {
      out += "                <i class='uiIconChat32x32Metting uiIconChat32x32LightGray'></i>";
    } else if ("call-on" === actionType) {
      out += "                <i class='uiIconChat32x32StartCall uiIconChat32x32LightGray'></i>";
    } else if ("call-off" === actionType) {
      out += "                <i class='uiIconChat32x32FinishCall uiIconChat32x32LightGray'></i>";
    } else if ("call-proceed" === actionType) {
      out += "                <i class='uiIconChat32x32AddCall uiIconChat32x32LightGray'></i>";
    }
    out += "                </div>";
  }

  return out;
};

ChatRoom.prototype.getDate = function(timestampServer) {
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


ChatRoom.prototype.getObjectSize = function(obj) {
  var size = 0;
  if (this.IsIE8Browser()) {
    for (prop in obj) {
      if (obj.hasOwnProperty(prop))
        size++;
    }
  } else {
    size = Object.keys(obj).length
  }
  return size;
}



/**
 * HTML Message Beautifier
 *
 * @param message
 * @returns {string} : the html markup
 */
ChatRoom.prototype.messageBeautifier = function(objMessage, options) {
  var message = objMessage.message;
  var msg = "";
  var thiss = this;
  if (options!==undefined) {
    var out = "";

    if (options.type ==="type-me") {
      var urlProfile = "<a href='/portal/intranet/profile/"+options.username+"' target='_new'>"+options.fullname+"</a>";
      var text = message.replace("/me", urlProfile);
      out += "<center>"+text+"</center>";
    } else if (options.type ==="type-file") {
      var urlFile = "<a class='msLinkInMes' href='"+options.restPath+"' target='_new'>"+options.title+"</a> ";
      var size = "<span class=\"fileSize\">("+options.sizeLabel+")</span>";
      out += urlFile + size;
      var link = options.restPath;
      if (link.endsWith(".png") || link.endsWith(".jpg") || link.endsWith(".gif")) {
        out += "<div class='msAttachmentBox'><div class='msAttachFile'><img src=\""+options.restPath+"\"/></div><div class='msActionAttach'><div class='inner'><div><a href='" + options.restPath + "' target='_blank'><i class='uiIconSearch uiIconWhite'></i> " + chatBundleData.exoplatform_chat_view + "</a></div><div><a href='"+options.downloadLink+"' target='_blank'><i class='uiIconDownload uiIconWhite'></i> " + chatBundleData.exoplatform_chat_download + "</a></div></div></div></div>";
      }

    } else if (options.type==="type-link") {
      var link = options.link.toLowerCase();
      var url = "<a href='"+options.link+"' target='_new'>"+options.link+"</a>";
      out += url;
      if (link.endsWith(".png") || link.endsWith(".jpg") || link.endsWith(".gif")) {
        out += "<div><img src=\""+options.link+"\" style=\"max-width: 200px;max-height: 140px;border: 1px solid #CCC;padding: 5px;margin: 5px 0;\"/></div>";
      }
    } else if (options.type==="type-task") {
      out += "<b>" + options.task + "</b>";
      out += "<div class='msTimeEvent'>";
      out += "  <div>";
      out += "    <i class='uiIconChatAssign uiIconChatLightGray mgR10'></i><span class='muted'>" + chatBundleData.exoplatform_chat_assign_to + ": </span>" + options.fullname;
      out += "  </div>";
      out += "  <div>";
      out += "    <i class='uiIconChatClock uiIconChatLightGray mgR10'></i><span class='muted'>" + chatBundleData.exoplatform_chat_due_date +  ":</span> <b>" + options.dueDate + "</b>";
      out += "  </div>";
      out += "</div>";
    } else if (options.type==="type-event") {
      out += "<b>" + options.summary + "</b>";
      out += "<div class='msTimeEvent'>";
      out += "  <div>";
      out += "    <i class='uiIconChatClock uiIconChatLightGray mgR20'></i><span class='muted'>" + chatBundleData.exoplatform_chat_from + ": </span><b class='mgR5'>" + options.startDate + " " + options.startTime + "</b><span class='muted'>" + chatBundleData.exoplatform_chat_to + ": </span><b>" + options.endDate + " " + options.endTime + "</b>";
      out += "  </div>";
      out += "  <div>";
      out += "    <i class='uiIconChatCheckin uiIconChatLightGray mgR20'></i>" + options.location;
      out += "  </div>";
      out += "</div>";
    } else if (options.type==="type-add-team-user") {
      var users = "<b>" + options.users.replace("; ","</b>; <b>") + "</b>";
      out += chatBundleData.exoplatform_chat_team_msg_adduser.replace("{0}", "<b>" + options.fullname + "</b>").replace("{1}", users);
    } else if (options.type==="type-remove-team-user") {
      var users = "<b>" + options.users.replace("; ","</b>; <b>") + "</b>";
      out += chatBundleData.exoplatform_chat_team_msg_removeuser.replace("{0}", "<b>" + options.fullname + "</b>").replace("{1}", users);
    } else if (options.type==="type-kicked") {
      out += "<b>" + chatBundleData.exoplatform_chat_team_msg_kicked + "</b>";
    } else if (options.type==="type-question" || options.type==="type-hand") {
      out += "<b>" + message + "</b>";
// TODO review
//=======
//      var url = options.task+
//        "<br><div style='font-weight: normal;color:#AAA;margin-top: 6px;'>"+chatBundleData.exoplatform_chat_assigned+" <a href='/portal/intranet/profile/"+options.username+"' style='color:#AAA' target='_new'>"+options.fullname+"</a> - "+chatBundleData.exoplatform_chat_ldue+" <span style='color:#ac724f'>"+options.dueDate+"</span></div>";
//      out += url;
//    } else if (options.type==="type-event") {
//      var url = options.summary+
//        "<br><div style='font-weight: normal;color:#AAA;margin-top: 6px;'>"+chatBundleData.exoplatform_chat_from+" "+options.startDate+" "+options.startTime+" "+chatBundleData.exoplatform_chat_to+" "+options.endDate+" "+options.endTime+"</div>";
//      out += url;
//>>>>>>> origin/develop
    } else if (options.type==="type-notes") {
      out += "<b>" + chatBundleData.exoplatform_chat_notes_saved + "</b>";
      out += "<div class='msMeetingNotes'>";
      out += "  <div>";
      out += "    <i class='uiIconChatSendEmail uiIconChatLightGray mgR10'></i>";
      out += "    <a class='send-meeting-notes' href='#' data-from='" + options.fromTimestamp + "' data-to='" + objMessage.timestamp + "' data-room='" + this.id + "' data-owner='" + this.username +"' data-id='" + objMessage.timestamp + "'>" + chatBundleData.exoplatform_chat_send_notes + "</a>";
      out += "  </div>";
      out += "  <div>";
      out += "    <i class='uiIconChatWiki uiIconChatLightGray mgR10'></i>";
      out += "    <a class='save-meeting-notes' href='#' data-from='" + options.fromTimestamp + "' data-to='" + objMessage.timestamp + "' data-room='" + this.id + "' data-owner='" + this.username +"' data-id='" + objMessage.timestamp + "2'>" + chatBundleData.exoplatform_chat_save_wiki + "</a>";
      out += "  </div>";
      out += "  <div class='alert alert-success' id='"+objMessage.timestamp+"' style='display:none;'><button type='button' class='close' onclick='jqchat(\"#"+objMessage.timestamp+"\").hide();' style='right: 0;'>×</button><strong>"+chatBundleData.exoplatform_chat_sent+"</strong> "+chatBundleData.exoplatform_chat_check_mailbox+"</div>";
      out += "  <div class='alert alert-success' id='"+objMessage.timestamp+"2' style='display:none;'><button type='button' class='close' onclick='jqchat(\"#"+objMessage.timestamp+"2\").hide();' style='right: 0;'>×</button><strong>"+chatBundleData.exoplatform_chat_saved+"</strong> <a href=\"/portal/intranet/wiki\">"+chatBundleData.exoplatform_chat_open_wiki+"</a>.</div>";
      out += "</div>";
    } else if (options.type==="type-meeting-start") {
      out += "<b>" + chatBundleData.exoplatform_chat_meeting_started + "</b>";
      out += "<p><i class='muted'>" + chatBundleData.exoplatform_chat_meeting_started_message + "</i></p>";

      thiss.startMeetingTimestamp = objMessage.timestamp;
      if (thiss.miniChat === undefined) {
        chatApplication.updateMeetingButtonStatus('started');
      }
    } else if (options.type==="type-meeting-stop") {
      out += "<b>" + chatBundleData.exoplatform_chat_meeting_finished + "</b>";
      var isStopedByCurrentUser = (thiss.username === options.fromUser);
      if (isStopedByCurrentUser) {
        out += "<div class='msMeetingNotes'>";
        out += "  <div>";
        out += "    <i class='uiIconChatSendEmail uiIconChatLightGray mgR10'></i>";
        out += "    <a class='" + (isStopedByCurrentUser ? "send-meeting-notes" : "") + "' href='" + (isStopedByCurrentUser ? "javascript:void(0);" : "javascript:alert(\"Only the participants who stopped the session can send or save meeting notes!\");") + "' data-from='" + thiss.startMeetingTimestamp + "' data-to='" + objMessage.timestamp + "' data-room='" + this.id + "' data-owner='" + this.username + "' data-id='" + objMessage.timestamp + "'>" + chatBundleData.exoplatform_chat_send_notes + "</a>";
        out += "  </div>";
        out += "  <div>";
        out += "    <i class='uiIconChatWiki uiIconChatLightGray mgR10'></i>";
        out += "    <a class='" + (isStopedByCurrentUser ? "save-meeting-notes" : "") + "' href='" + (isStopedByCurrentUser ? "javascript:void(0);" : "javascript:alert(\"Only the participants who stopped the session can send or save meeting notes!\");") + "' data-from='" + thiss.startMeetingTimestamp + "' data-to='" + objMessage.timestamp + "' data-room='" + this.id + "' data-owner='" + this.username + "' data-id='" + objMessage.timestamp + "2'>" + chatBundleData.exoplatform_chat_save_wiki + "</a>";
        out += "  </div>";
        out += "  <div class='alert alert-success' id='" + objMessage.timestamp + "' style='display:none;'><button type='button' class='close' onclick='jqchat(\"#" + objMessage.timestamp + "\").hide();' style='right: 0;'>×</button><strong>" + chatBundleData.exoplatform_chat_sent + "</strong> " + chatBundleData.exoplatform_chat_check_mailbox + "</div>";
        out += "  <div class='alert alert-success' id='" + objMessage.timestamp + "2' style='display:none;'><button type='button' class='close' onclick='jqchat(\"#" + objMessage.timestamp + "2\").hide();' style='right: 0;'>×</button><strong>" + chatBundleData.exoplatform_chat_saved + "</strong> <a href=\"/portal/intranet/wiki\">" + chatBundleData.exoplatform_chat_open_wiki + "</a>.</div>";
        out += "</div>";
      }
      if (thiss.miniChat === undefined) {
        chatApplication.updateMeetingButtonStatus('stopped');
      }
    } else if (options.type==="call-on") {
      this.startCallTimestamp = objMessage.timestamp;
      out += "<b>" + chatBundleData.exoplatform_chat_meeting_started + "</b>";

    } else if (options.type==="call-off") {
      var callDuration = (objMessage.timestamp - this.startCallTimestamp)/1000;
      var hours = Math.floor(callDuration / 3600);
      callDuration -= hours * 3600;
      var minutes = Math.floor(callDuration / 60);
      callDuration -= minutes * 60;
      var seconds = parseInt(callDuration % 60, 10);
      var stime = "<span class='msTextGray'>";
      if (hours>0) {
        if (hours===1)
          stime += hours+ " "+chatBundleData.exoplatform_chat_hour+" ";
        else
          stime += hours+ " "+chatBundleData.exoplatform_chat_hours+" ";
      }
      if (minutes>0) {
        if (minutes===1)
          stime += minutes+ " "+chatBundleData.exoplatform_chat_minute+" ";
        else
          stime += minutes+ " "+chatBundleData.exoplatform_chat_minutes+" ";
      }
      if (seconds>0) {
        if (seconds===1)
          stime += seconds+ " "+chatBundleData.exoplatform_chat_second;
        else
          stime += seconds+ " "+chatBundleData.exoplatform_chat_seconds;
      }
      stime += "</span>";
      out += "<b>" + chatBundleData.exoplatform_chat_meeting_finished + "</b> " + stime;

      var callOwner = jzGetParam("weemoCallHandlerOwner");
      if (thiss.username === callOwner) {
        out += "<br>";
        out += "<div style='display: block;margin: 10px 0;'>" +
          "<span class='msMeetingNotes'>" +
          "<a href='#' class='send-meeting-notes' " +
          "data-from='"+jzGetParam("weemoCallHandlerFrom")+"' " +
          "data-to='"+jzGetParam("weemoCallHandlerTo")+"' " +
          "data-room='"+this.id+"' " +
          "data-owner='"+this.username +"' " +
          "data-id='"+options.timestamp+"' " +
          ">"+chatBundleData.exoplatform_chat_send_notes+"</a>" +
          " - " +
          "<a href='#' class='save-meeting-notes' " +
          "data-from='"+jzGetParam("weemoCallHandlerFrom")+"' " +
          "data-to='"+jzGetParam("weemoCallHandlerTo")+"' " +
          "data-room='"+this.id+"' " +
          "data-owner='"+this.username +"' " +
          "data-id='"+options.timestamp+"2' " +
          ">"+chatBundleData.exoplatform_chat_save_wiki+"</a>" +
          "</span>" +
          "<div class='alert alert-success' id='"+options.timestamp+"' style='display:none;'><button type='button' class='close' onclick='jqchat(\"#"+options.timestamp+"\").hide();' style='right: 0;'>×</button><strong>"+chatBundleData.exoplatform_chat_sent+"</strong> "+chatBundleData.exoplatform_chat_check_mailbox+"</div>" +
          "<div class='alert alert-success' id='"+options.timestamp+"2' style='display:none;'><button type='button' class='close' onclick='jqchat(\"#"+options.timestamp+"2\").hide();' style='right: 0;'>×</button><strong>"+chatBundleData.exoplatform_chat_saved+"</strong> <a href=\"/portal/intranet/wiki\">"+chatBundleData.exoplatform_chat_open_wiki+"</a>.</div>" +
          "</div>";
      }
    } else if (options.type==="call-proceed") {
      out += "<b>" + chatBundleData.exoplatform_chat_call_comming  + "...</b>";
    } else {
      out += message;
    }

    return out;
  }


  if (message.indexOf("java:")===0) {
    msg = "<div class='sh_container '><pre class='sh_java'>"+message.substr(5, message.length-6)+"</pre></div>";
    return msg;
  } else if (message.indexOf("html:")===0) {
    msg = "<div class='sh_container '><pre class='sh_html'>"+message.substr(5, message.length-6)+"</pre></div>";
    return msg;
  } else if (message.indexOf("js:")===0) {
    msg = "<div class='sh_container '><pre class='sh_javascript'>"+message.substr(3, message.length-4)+"</pre></div>";
    return msg;
  } else if (message.indexOf("css:")===0) {
    msg = "<div class='sh_container '><pre class='sh_css'>"+message.substr(4, message.length-5)+"</pre></div>";
    return msg;
  }



  var lines = message.split("<br/>");
  var il,l;
  for (il=0 ; il<lines.length ; il++) {
    l = lines[il];
    if (l.indexOf("google:")===0) {
      // console.log("*"+l+"* "+l.length);
      msg += "google:<a href='http://www.google.com/search?q="+l.substr(7, l.length-7)+"' target='_new'>"+l.substr(7, l.length-7)+"</a> ";
    } else if (l.indexOf("wolfram:")===0) {
      // console.log("*"+l+"* "+l.length);
      msg += "wolfram:<a href='http://www.wolframalpha.com/input/?i="+l.substr(8, l.length-8)+"' target='_new'>"+l.substr(8, l.length-8)+"</a> ";
    } else {
      var tab = l.split(" ");
      var it,w;
      for (it=0 ; it<tab.length ; it++) {
        w = tab[it];
        if (w.indexOf("google:")===0) {
          w = "google:<a href='http://www.google.com/search?q="+w.substr(7, w.length-7)+"' target='_new'>"+w.substr(7, w.length-7)+"</a>";
        } else if (w.indexOf("wolfram:")===0) {
          w = "wolfram:<a href='http://www.wolframalpha.com/input/?i="+w.substr(8, w.length-8)+"' target='_new'>"+w.substr(8, w.length-8)+"</a>";
        } else if (w.indexOf("/")>-1 && w.indexOf("&lt;/")===-1 && w.indexOf("/&gt;")===-1) {
          var link = w;
          if (w.endsWith(".jpg") || w.endsWith(".png") || w.endsWith(".gif") || w.endsWith(".JPG") || w.endsWith(".PNG") || w.endsWith(".GIF")) {
            w = "<a href='"+w+"' target='_new'><img src='"+w+"' width='100%' /></a>";
            w += "<span class='invisible-text'>"+link+"</span>";
          } else if (w.indexOf("http://www.youtube.com/watch?v=")===0 && !this.IsIE8Browser() ) {
            var id = w.substr(31);
            w = "<iframe width='100%' src='http://www.youtube.com/embed/"+id+"' frameborder='0' allowfullscreen></iframe>";
            w += "<span class='invisible-text'>"+link+"</span>";
          } else if (w.indexOf("[/quote]")===-1 && (w.indexOf("http:")===0 || w.indexOf("https:")===0 || w.indexOf("ftp:")===0) ) {
            w = "<a href='"+w+"' target='_new'>"+w+"</a>";
          }
        } else if (w == ":-)" || w==":)") {
          w = "<span class='uiIconChatGifsmile'><span class='emoticon-text'>:)</span></span>";
        } else if (w == ":-p" || w==":p" || w==":-P" || w==":P") {
          w = "<span class='uiIconChatGifsmile-with-tongue'><span class='emoticon-text'>:p</span></span>";
        } else if (w == ":-D" || w==":D" || w==":-d" || w==":d") {
          w = "<span class='uiIconChatGiflaugh'><span class='emoticon-text'>:D</span></span>";
        } else if (w == ":-|" || w==":|") {
          w = "<span class='uiIconChatGifspeechless'><span class='emoticon-text'>:|</span></span>";
        } else if (w == ":-(" || w==":(") {
          w = "<span class='uiIconChatGifsad'><span class='emoticon-text'>:(</span></span>";
        } else if (w == ";-)" || w==";)") {
          w = "<span class='uiIconChatGifwink'><span class='emoticon-text'>;)</span></span>";
        } else if (w == ":-O" || w==":O") {
          w = "<span class='uiIconChatGifsurprise'><span class='emoticon-text'>:O</span></span>";
        } else if (w == "(beer)") {
          w = "<span class='uiIconChatGifbeer'><span class='emoticon-text'>(beer)</span></span>";
        } else if (w == "(bow)") {
          w = "<span class='uiIconChatGifbow'><span class='emoticon-text'>(bow)</span></span>";
        } else if (w == "(bug)") {
          w = "<span class='uiIconChatGifbug'><span class='emoticon-text'>(bug)</span></span>";
        } else if (w == "(cake)" || w == "(^)") {
          w = "<span class='uiIconChatGifcake'><span class='emoticon-text'>(^)</span></span>";
        } else if (w == "(cash)") {
          w = "<span class='uiIconChatGifcash'><span class='emoticon-text'>(cash)</span></span>";
        } else if (w == "(coffee)") {
          w = "<span class='uiIconChatGifcoffee'><span class='emoticon-text'>(coffee)</span></span>";
        } else if (w == "(n)" || w == "(no)") {
          w = "<span class='uiIconChatGifraise-down'><span class='emoticon-text'>(no)</span></span>";
        } else if (w == "(y)" || w == "(yes)") {
          w = "<span class='uiIconChatGifraise-up'><span class='emoticon-text'>(yes)</span></span>";
        } else if (w == "(star)") {
          w = "<span class='uiIconChatGifstar'><span class='emoticon-text'>(star)</span></span>";
        } else if (this.highlight.length >1) {
          w = w.replace(eval("/"+this.highlight+"/g"), "<span style='background-color:#FF0;font-weight:bold;'>"+this.highlight+"</span>");
        }
        msg += w+" ";
      }
    }
    // console.log(il + "::" + lines.length);
    if (il < lines.length-1) {
      msg += "<br/>";
    }
  }

  var quote = "";
  if (msg.indexOf("[quote=")===0) {
    msg = this.getQuote(msg, msg);
  }
  return msg;
};

/**
 Generate html markup from quote, eg: [quote=xxx] [quote=yyy]information [/quote]comment1[/quote]comment2
 */
ChatRoom.prototype.getQuote = function(message, originMessage) {
  var numQuotes = message.split('[quote=').length - 1;
  var numOriginQuotes = originMessage.split('[quote=').length - 1;
  var outermostName = message.substring(message.indexOf('[quote=') + 7, message.indexOf(']'));
  var outtermostContent = message.substring(message.indexOf(']') + 1, message.lastIndexOf('[/quote]'));
  var outermostComment = message.substring(message.lastIndexOf('[/quote]') + 8);
  if (numOriginQuotes === 1) {
    return "<div class='postContent'><div class='msUserQuote contentQuote quoteDefault'><b class='msNameUser'>" + outermostName + ":</b><div>" + outtermostContent + "</div></div>" + outermostComment + "</div>";
  }
  if (numQuotes > 1) {
    if (numQuotes === numOriginQuotes) {
      return "<div class='postContent'><div class='msUserQuote contentQuote quoteDefault'><div class='msTiltleLn clearfix'><b class='msNameUser'>" + outermostName + "</b></div><div class='msQuoteCont'>" + this.getQuote(outtermostContent, originMessage) + "</div></div>" + outermostComment + "</div>";
    } else {
      return "<quote><div class='msTiltleLn clearfix'><b class='msNameUser'>" + outermostName + "</b></div><div class='msQuoteCont '>" + this.getQuote(outtermostContent, originMessage) + "</div></quote>" + outermostComment;
    }
  } else {
    return "<quote><b class='msNameUser'>" + outermostName + ":</b><div>" + outtermostContent + "</div></quote>" + outermostComment;
  }
};

/**
 * Test if IE8
 * @returns {boolean}
 * @constructor
 */
ChatRoom.prototype.IsIE8Browser = function() {
  var rv = -1;
  var ua = navigator.userAgent;
  var re = new RegExp("Trident\/([0-9]{1,}[\.0-9]{0,})");
  if (re.exec(ua) != null) {
    rv = parseFloat(RegExp.$1);
  }
  return (rv == 4);
};

/**
 * Update Unread Messages
 *
 * @param callback
 */
ChatRoom.prototype.updateUnreadMessages = function() {
  jqchat.ajax({
    url: this.jzChatUpdateUnreadMessages,
    data: {"room": this.id,
      "user": this.username,
      "token": this.token,
      "timestamp": new Date().getTime()
    },

    success:function(response){
      //console.log("success");
    },

    error:function (xhr, status, error){
      //console.log("error");
    }
  });
};

/**
 ##################                           ##################
 ##################                           ##################
 ##################   MINI CHATROOM           ##################
 ##################                           ##################
 ##################                           ##################
 */
String.prototype.endsWith = function(suffix) {
  return this.indexOf(suffix, this.length - suffix.length) !== -1;
};


(function($) {

  $(document).ready(function() {
    //GETTING DOM CONTEXT
    var $miniChat = $(".mini-chat");
    $miniChat.each( function(index) {
      var initialized = $(this).attr("data-init");
      if (initialized === undefined) {
        $(this).attr("data-init", "auto");
        $(this).attr("data-index", index);
        var $obj = $(this);
        var urlToken = "/rest/chat/api/1.0/user/token";
        snack.request({
          url: urlToken
        }, function (err, response){
          if (!err) {
            var data = snack.parseJSON(response);

            var username = data.username;
            var token = data.token;
            $obj.attr("data-username", username);
            $obj.attr("data-token", token);
            var innerMiniChatHtml = "";
            innerMiniChatHtml += "<div class='title clearfix'>";
            innerMiniChatHtml +=    "<div class='title-right'>";
            innerMiniChatHtml +=      " <a class='uiActionWithLabel btn-mini' href='javaScript:void(0);' data-placement='top' data-toggle='tooltip' title='" + chatBundleData.exoplatform_chat_minimize + "' ><i class='uiIconMinimize uiIconWhite'></i></a>";
            innerMiniChatHtml +=      " <a class='uiActionWithLabel btn-maxi' style='display:none;' href='javaScript:void(0);' data-placement='top' data-toggle='tooltip' title='" + chatBundleData.exoplatform_chat_maximize + "' ><i class='uiIconMaximize uiIconWhite'></i></a>";
            innerMiniChatHtml +=      " <a class='uiActionWithLabel btn-open-chat' href='" + chatNotification.chatPage + "' data-placement='top' data-toggle='tooltip' title='" + chatBundleData.exoplatform_chat_open_chat + "' target='_chat'><i class='uiIconChatPopOut uiIconChatWhite'></i></a>";
            innerMiniChatHtml +=      " <a class='uiActionWithLabel btn-close' href='javaScript:void(0);' data-placement='top' data-toggle='tooltip' title='" + chatBundleData.exoplatform_chat_close + "' ><i class='uiIconClose uiIconWhite'></i></a>";
            innerMiniChatHtml +=    "</div>";
            innerMiniChatHtml +=    "<div class='title-left'>";
            innerMiniChatHtml +=      "<span class='notify-info'></span>";
            innerMiniChatHtml +=      " <span class='fullname'></span>";
            innerMiniChatHtml +=    "</div>";
            innerMiniChatHtml += "</div>";
            innerMiniChatHtml += "<div class='history uiContentBox'>";
            innerMiniChatHtml += "</div>";
            innerMiniChatHtml += "<div class='message'><input type='text' class='message-input' autocomplete='off' name='text'></div>"
            $obj.html(innerMiniChatHtml);

            // If this is "refresh page" case, show minichat (if it shown before)
            var miniChatRoom = jzGetParam(chatNotification.sessionId + "miniChatRoom");
            if (miniChatRoom && miniChatRoom !== "") {
              var miniChatType = jzGetParam(chatNotification.sessionId + "miniChatType");
              var miniChatMode = jzGetParam(chatNotification.sessionId + "miniChatMode");
              showMiniChatPopup(miniChatRoom, miniChatType);
            }
          }
        });


      }
    });
  });

})(jqchat);

var miniChats = {};

function maximizeMiniChat() {
  // Keep infor for "refresh page" case
  jzStoreParam(chatNotification.sessionId + "miniChatMode", "maxi");

  var $miniChat = jqchat(".mini-chat").first();
  var $history = $miniChat.find(".history");

  $miniChat.find(".btn-mini").show();
  $miniChat.find(".btn-maxi").hide();
  $miniChat.find(".notify-info").hide();
  $history.show("fast");
  $miniChat.find(".message").show("fast");
  $history.animate({ scrollTop: 20000 }, 'fast');

  $miniChat.show("fast", function() {
    $miniChat.find(".message-input").focus();
  });
};

function minimizeMiniChat() {
  // Keep infor for "refresh page" case
  jzStoreParam(chatNotification.sessionId + "miniChatMode", "mini");

  var $miniChat = jqchat(".mini-chat").first();

  $miniChat.find(".btn-mini").hide();
  $miniChat.find(".btn-maxi").show();
  $miniChat.find(".history").hide();
  $miniChat.find(".message").hide();
  $miniChat.css("height","auto");
  $miniChat.css("display", "block");
};

function showMiniChatPopup(room, type) {
  // Keep infor for "refresh page" case
  jzStoreParam(chatNotification.sessionId + "miniChatRoom", room);
  jzStoreParam(chatNotification.sessionId + "miniChatType", type);

  var chatServerUrl = jqchat("#chat-status").attr("data-chat-server-url");
  var $miniChat = jqchat(".mini-chat").first();
  var username = $miniChat.attr("data-username");
  var token = $miniChat.attr("data-token");
  var index = $miniChat.attr("data-index");

  if (miniChats[index] !== undefined)
    miniChats[index].id = "";

  // Display chat
  var miniChatMode = jzGetParam(chatNotification.sessionId + "miniChatMode","");
  if (miniChatMode === "mini") {
    minimizeMiniChat();
  } else {
    maximizeMiniChat();
  }
  jqchat("[data-toggle='tooltip']").tooltip();

  // Show chat messages
  var urlRoom = chatServerUrl+"/getRoom";
  snack.request({
    url: urlRoom,
    data: {
      targetUser: room,
      user: username,
      token: token,
      withDetail: true,
      type: type
    }

  }, function (err, response){
    if (!err) {
      var cRoom = snack.parseJSON(response);
      var targetUser = cRoom.user;
      var targetFullname = cRoom.escapedFullname;
      $miniChat.find(".fullname").html(targetFullname);
      var jzChatRead = chatServerUrl+"/read";
      var jzChatSend = chatServerUrl+"/send";
      var jzChatGetRoom = chatServerUrl+"/getRoom";
      var jzChatUpdateUnreadMessages = chatServerUrl+"/updateUnreadMessages";
      if (miniChats[index] === undefined) {
        miniChats[index] = new ChatRoom(jzChatRead, jzChatSend, jzChatGetRoom, jzChatUpdateUnreadMessages,  "", "", 3000, false);

        $miniChat.find(".message-input").keyup(function(event) {
          var msg = jqchat(this).val();
//        console.log("keyup : "+event.which + ";"+msg.length);
          var isSystemMessage = (msg.indexOf("/")===0 && msg.length>1) ;

          if ( event.which === 13 && msg.length>=1) {
            //console.log("sendMsg=>"+username + " : " + room + " : "+msg);
            if(!msg)
            {
              return;
            }
            //      console.log("*"+msg+"*");
            jqchat(this).val("");
            miniChats[index].sendMessage(msg, {}, false, function() {
//        console.log("message sent : "+msg);
              $miniChat.find(".message-input").val("");
            });

          }
          if ( event.which === 27 && msg.length === 0) {
            $miniChat.find(".message-input").val("");
            miniChats[index].clearInterval();
            $miniChat.slideUp(200);
          }

        });
      }
      miniChats[index].id = cRoom.room;
      miniChats[index].updateUnreadMessages();

      miniChats[index].setMiniChatDiv($miniChat);
      miniChats[index].onRefresh(function() {

      });
      miniChats[index].onShowMessages(function(out) {
        var $history = this.miniChat.find(".history");
        $history.html('<span>'+out+'</span>');
        var totalMsgs = jqchat(".msUserCont", $history).length;
        $history.animate({ scrollTop: 20000 }, 'fast');

        if ($history.is(":hidden")) {
          var unreadTotal = totalMsgs - $miniChat.attr("readTotal");
          if (unreadTotal > 0) {
            $miniChat.find(".notify-info").show();
            $miniChat.find(".notify-info").html(unreadTotal);
          }
          else
            $miniChat.find(".notify-info").hide();
        } else {
          $miniChat.attr("readTotal", totalMsgs);
          $miniChat.find(".notify-info").hide();
        }
      });
      miniChats[index].init(username, token, targetUser, targetFullname, false, function(){
      });
    }
  });

  /************ MINI CHAT event handlers *************/
  $miniChat.find(".btn-close").on("click", function(){
    // Keep infor for "refresh page" case
    jzStoreParam(chatNotification.sessionId + "miniChatRoom", "");
    jzStoreParam(chatNotification.sessionId + "miniChatType", "");
    jzStoreParam(chatNotification.sessionId + "miniChatMode", "");


    $miniChat.find(".message-input").val("");
    miniChats[index].clearInterval();
    $miniChat.hide();
  });

  $miniChat.find(".btn-mini").on("click", function(){
    var $history = $miniChat.find(".history");
    var totalMsgs = jqchat(".msUserCont", $history).length;
    $miniChat.attr("readTotal", totalMsgs);
    minimizeMiniChat();
  });

  $miniChat.find(".btn-maxi").on("click", function(){
    maximizeMiniChat();
  });

  $miniChat.find(".message-input").on("focus", function(){
    if (miniChats[index] && miniChats[index].id !== "")
      miniChats[index].updateUnreadMessages();
  });
};
