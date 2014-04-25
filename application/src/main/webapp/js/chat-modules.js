
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
function ChatRoom(jzChatRead, jzChatSend, jzChatGetRoom, jzChatSendMeetingNotes, jzChatGetMeetingNotes, chatIntervalChat, isPublic, labels) {
  this.id = "";
  this.messages = "";
  this.jzChatRead = jzChatRead;
  this.jzChatSend = jzChatSend;
  this.jzChatGetRoom = jzChatGetRoom;
  this.jzChatSendMeetingNotes = jzChatSendMeetingNotes;
  this.jzChatGetMeetingNotes = jzChatGetMeetingNotes;
  this.chatEventInt = -1;
  this.chatIntervalChat = chatIntervalChat;
  this.username = "";
  this.token = "";
  this.targetUser = "";
  this.targetFullname = "";
  this.isPublic = isPublic;
  this.labels = labels;

  this.ANONIM_USER = "__anonim_";

  this.onRefreshCB;
  this.onShowMessagesCB;

  this.highlight = "";
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

  var im = this.messages.length;
  this.messages[im] = {"user": this.username,
    "fullname": "You",
    "date": "pending",
    "message": msg,
    "options": options,
    "isSystem": isSystemMessage};
  this.showMessages();

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
        if (typeof thiss.onRefreshCB === "function") {
          thiss.onRefreshCB(1);
        }
        return;
      }


      res = res.split("\t").join(" ");
      // handle the response data
      var data = snack.parseJSON(res);
      var lastTS = jzGetParam("lastTS"+thiss.username);
//      console.log("chatEvent :: lastTS="+lastTS+" :: serverTS="+data.timestamp);
      var im, message, out="", prevUser="";
      if (data.messages.length===0) {
        thiss.showMessages(data);
      } else {
        var ts = data.timestamp;
        if (ts != lastTS || (forceRefresh === true)) {
          jzStoreParam("lastTS"+thiss.username, ts, 600);
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
  var im, message, out="", prevUser="", prevFullName, prevOptions;
  if (msgs!==undefined) {
    this.messages = msgs.messages;
  }

  if (this.messages.length===0) {

    if (this.isPublic) {
      out = "<div class='msRow' style='padding:22px 20px;'>";
      out += "<b><center>"+this.labels.get("label-public-welcome")+"</center></b>";
      out += "</div>";
    }
    else
      out += "<div class='noMessage'></div>";
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
                  out += "  <a class='msAvatarLink' href='#'><img onerror=\"this.src='/chat/img/Avatar.gif;'\" src='/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:" + prevUser + "/soc:profile/soc:avatar' alt='" + prevFullName + "'></a>";
                }
                out += "  </div>";
              } else {
                if (prevOptions.type === "type-question") {
                  out += "  <div class='msUserAvatar'>";
                  out += "    <i class='uiIconChat32x32Question uiIconChat32x32LightGray'></i>";
                  out += "  </div>";
                }
              }
              out += "  </div>";
              out += "</div>";
            }
            out += "  <div class='msRow odd'>";
            out += "    <div class='msMessagesGroup clearfix'>";
            out += "      <div class='msContBox'>";
            out += "        <div class='inner'>";
            out += "          <div class='msTiltleLn clearfix'>";
            if (thiss.isPublic) {
              out += "          <a class='msNameUser' href='#'>" + thiss.labels.get("label-support-fullname") + "</a>";
            }
            else {
              out += "          <a class='msNameUser' href='/portal/intranet/profile/"+message.user+"'>" +message.fullname  + "</a>";
            }
            out += "          </div>";
          } else {
            if (prevUser !== "") {
              if (prevUser !== "__system") {
                out += "    <a class='msAvatarLink' href='#'><img onerror=\"this.src='/chat/img/Avatar.gif;'\" src='/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:" + prevUser + "/soc:profile/soc:avatar' alt='" + prevFullName + "'></a>";
                out += "  </div>";
              } else {
                if (prevOptions.type === "type-question") {
                  out += "  <div class='msUserAvatar'>";
                  out += "    <i class='uiIconChat32x32Question uiIconChat32x32LightGray'></i>";
                  out += "  </div>";
                }
              }
              out += "  </div>";
              out += "</div>";
            }
            out += "  <div class='msRow'>";
            out += "    <div class='msMessagesGroup clearfix'>";
            out += "      <div class='msContBox'>";
            out += "        <div class='inner'>";
            out += "          <div class='msTiltleLn clearfix'>";
            out += "            <a class='msNameUser' href='/portal/intranet/profile/"+message.user+"'>" +message.fullname  + "</a>";
            out += "          </div>";
          }
        }
        else
        {
//          out += "<hr style='margin:0px;'>";
        }
        var msgtemp = message.message;
        if (message.type === "DELETED") {
          msgtemp = "<span class='contentDeleted'>"+thiss.labels.get("label-deleted")+"</span>";
        } else {
          msgtemp = thiss.messageBeautifier(message.message);
        }
        out += "            <div class='msUserCont msg-text clearfix'>";
        out += "              <div class='msRightInfo pull-right'>";
        out += "                <div class='msTimePost'>";
        if (message.type === "DELETED" || message.type === "EDITED") {
          out += "                <span href='#' class='msEditMes'><i class='uiIconChatEdited uiIconChatLightGray'></i></span>";
        }
        out += "                  <span class='msg-date'>" + thiss.getDate(message.timestamp) + "</span>";
        out += "                </div>";
        if (message.type !== "DELETED") {
          out += "              <div class='msAction msg-actions' style='display:none;'><span style='display: none;' class='msg-data' data-id='"+message.id+"' data-fn='"+message.fullname+"'>"+message.message+"</span>";
          if (message.user === thiss.username) {
            out += "              <a href='#' class='msg-action-edit'>" + thiss.labels.get("label-edit") + "</a>";
            out += "              <a href='#' class='msg-action-delete'>" + thiss.labels.get("label-delete") + "</a>";
          }
          out += "                <a href='#' class='msg-action-quote'>" + thiss.labels.get("label-quote") + "</a>";
          out += "              </div>";
        }
        out += "              </div>";
        out += "              <div class='msUserMes'><span>" + msgtemp + "</span></div>";
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
            out += "        <a class='msAvatarLink' href='#'><img onerror=\"this.src='/chat/img/Avatar.gif;'\" src='/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:" + prevUser + "/soc:profile/soc:avatar' alt='" + prevFullName + "'></a>";
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
              out += "      <a class='msAvatarLink' href='#'><img onerror=\"this.src='/chat/img/Avatar.gif;'\" src='/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:" + prevUser + "/soc:profile/soc:avatar' alt='" + prevFullName + "'></a>";
            out += "      </div>";
          } else {
            if (prevOptions.type === "type-question") {
              out += "      <div class='msUserAvatar'>";
              out += "        <i class='uiIconChat32x32Question uiIconChat32x32LightGray'></i>";
              out += "      </div>";
            }
          }
          out += "      </div>";
          out += "    </div>";
        }
        out += "      <div class='msRow odd'>";
        out += "        <div class='msMessagesGroup clearfix'>";
        out += "          <div class='msContBox'>";
        out += "            <div class='inner'>";
        out += "              <div class='msUserCont clearfix'>";
        out += "                <div class='msRightInfo pull-right'>";
        out += "                  <div class='msTimePost'>";
        out += "                    <span class='msg-date'>" + thiss.getDate(message.timestamp) + "</span>";
        out += "                  </div>";
        out += "                </div>";
        var options = {};
        // Legacy test
        if (message.message.indexOf("&")>0) {
          message.message = message.message.substring(0, message.message.indexOf("&"));
          options.timestamp = new Date().getTime();
        }
        // end of legacy test
        if (typeof message.options == "object")
          options = message.options;
        var nbOptions = thiss.getObjectSize(options);

        if (options.type==="call-on") {
          if (options.timestamp!==undefined) {
            jzStoreParam("weemoCallHandlerFrom", message.timestamp, 600000);
            jzStoreParam("weemoCallHandlerOwner", message.user, 600000);
          }
          jqchat(".btn-weemo").addClass('disabled');
        } else if (options.type==="call-off") {
          if (chatApplication.weemoExtension.isConnected)
            jqchat(".btn-weemo").removeClass('disabled');
          if (options.timestamp!==undefined) {
            jzStoreParam("weemoCallHandlerTo", message.timestamp, 600000);
          }
        }
//        out += "<img class='"+options.type+"' src='/chat/img/empty.png' width='32px' style='width:32px;'>";
        if (options.type==="type-event") {
          var day = options.startDate.substr(3, 2);
          out += "<span style='position: absolute;top: 2px;left: 9px;font-weight: bold;font-size: 20px;color: #848484;'>"+day+"</span>";
        }
        //out += "</span>";

        if (options.type !== "call-join") {
          if (options.uidToCall!==undefined && options.displaynameToCall!==undefined) {
            chatApplication.weemoExtension.setUidToCall(options.uidToCall);
            chatApplication.weemoExtension.setDisplaynameToCall(options.displaynameToCall);
            jqchat(".btn-weemo").css("display", "none");
            jqchat(".btn-weemo-conf").css("display", "block");
            if (options.uidToCall!=="weemo"+thiss.username && chatApplication.weemoExtension.isConnected)
              jqchat(".btn-weemo-conf").removeClass("disabled");
            else
              jqchat(".btn-weemo-conf").addClass("disabled");
          } else {
            jqchat(".btn-weemo").css("display", "block");
            jqchat(".btn-weemo-conf").css("display", "none");
          }
        }

//        out += "<span>";

        if (options.type === "type-me") {
          out += "<span class=\"system-event\">"+thiss.messageBeautifier(message.message, options)+"</span>";
          out += "<div style='margin-left:50px;'>";
        } else {
//          if (message.user != thiss.username) {
//            if (thiss.isPublic)
//              out += "<span class='invisible-text'>- </span><a href='#'>"+thiss.labels.get("label-support-fullname")+"</a><span class='invisible-text'> : </span><br/>";
//            else
//              out += "<a href='/portal/intranet/profile/"+message.user+"' class='user-link' target='_new'>"+message.fullname+"</a>";
//          } else {
//            out += "<span class='invisible-text'>- </span><a href='/portal/intranet/profile/"+message.user+"' class='user-link' target='_new'>"+message.fullname+"</a><span class='invisible-text'> : </span><br/>";
//          }
          out += "              <div class='msUserMes'>" + thiss.messageBeautifier(message.message, options) + "</div>";
          //out += "<div style='margin-left:50px;' class='msg-text'><span style='float:left' class=\"system-event\">"+thiss.messageBeautifier(message.message, options)+"</span>";
        }

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
          if (options.type === "type-question") {
            out += "      <div class='msUserAvatar'>";
            out += "        <i class='uiIconChat32x32Question uiIconChat32x32LightGray'></i>";
            out += "      </div>";
          }
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
ChatRoom.prototype.messageBeautifier = function(message, options) {
  var msg = "";
  var thiss = this;
  if (options!==undefined) {
    var out = "";

    if (options.type ==="type-me") {
      var urlProfile = "<a href='/portal/intranet/profile/"+options.username+"' target='_new'>"+options.fullname+"</a>";
      var text = message.replace("/me", urlProfile);
      out += "<center>"+text+"</center>";
    } else if (options.type ==="type-file") {
      var urlFile = "<a href='"+options.restPath+"' target='_new'>"+options.name+"</a>";
      var size = "<span class=\"msg-time\" style='font-weight: normal;'>("+options.sizeLabel+")</span>";
      out += urlFile+size;
      var link = options.restPath;
      if (link.endsWith(".png") || link.endsWith(".jpg") || link.endsWith(".gif")) {
        out += "<div><img src=\""+options.restPath+"\" style=\"max-width: 200px;max-height: 140px;border: 1px solid #CCC;padding: 5px;margin: 5px 0;\"/></div>";
      }

    } else if (options.type ==="call-join") {
      out += "";
    } else if (options.type ==="call-on") {
      out += "Meeting started";
    } else if (options.type==="call-off") {
      out += "Meeting finished";
      var tsold = Math.round(jzGetParam("weemoCallHandlerFrom"));
      var time = Math.round((options.timestamp*1000-tsold)/1000);
      var hours = Math.floor(time / 3600);
      time -= hours * 3600;
      var minutes = Math.floor(time / 60);
      time -= minutes * 60;
      var seconds = parseInt(time % 60, 10);
      var stime = "<span class=\"msg-time\" style='font-weight: normal;'>";
      if (hours>0) {
        if (hours===1)
          stime += hours+ " hour ";
        else
          stime += hours+ " hours ";
      }
      if (minutes>0) {
        if (minutes===1)
          stime += minutes+ " minute ";
        else
          stime += minutes+ " minutes ";
      }
      if (seconds>0) {
        if (seconds===1)
          stime += seconds+ " second";
        else
          stime += seconds+ " seconds";
      }
      stime += "</span>";
      out += stime;
      var callOwner = jzGetParam("weemoCallHandlerOwner");
      if (this.username === callOwner) {
        out += "<br>";
        out += "<div style='display: block;margin: 10px 0;'>" +
          "<span class='meeting-notes'>" +
          "<a href='#' class='send-meeting-notes' " +
          "data-from='"+jzGetParam("weemoCallHandlerFrom")+"' " +
          "data-to='"+jzGetParam("weemoCallHandlerTo")+"' " +
          "data-room='"+this.id+"' " +
          "data-owner='"+this.username +"' " +
          "data-id='"+options.timestamp+"' " +
          ">Send meeting notes</a>" +
          " - " +
          "<a href='#' class='save-meeting-notes' " +
          "data-from='"+jzGetParam("weemoCallHandlerFrom")+"' " +
          "data-to='"+jzGetParam("weemoCallHandlerTo")+"' " +
          "data-room='"+this.id+"' " +
          "data-owner='"+this.username +"' " +
          "data-id='"+options.timestamp+"2' " +
          ">Save as Wiki</a>" +
          "</span>" +
          "<div class='alert alert-success' id='"+options.timestamp+"' style='display:none;'><button type='button' class='close' onclick='jqchat(\"#"+options.timestamp+"\").hide();' style='right: 0;'>�</button><strong>Sent!</strong> Check your mailbox.</div>" +
          "<div class='alert alert-success' id='"+options.timestamp+"2' style='display:none;'><button type='button' class='close' onclick='jqchat(\"#"+options.timestamp+"2\").hide();' style='right: 0;'>�</button><strong>Saved!</strong> <a href=\"/portal/intranet/wiki\">Open Wiki application</a>.</div>" +
          "</div>";
      }

    } else if (options.type==="type-link") {
      var url = "<a href='"+options.link+"' target='_new'>"+options.link+"</a>";
      out += url;
      var link = options.link.toLowerCase();
      if (link.endsWith(".png") || link.endsWith(".jpg") || link.endsWith(".gif")) {
        out += "<div><img src=\""+options.link+"\" style=\"max-width: 200px;max-height: 140px;border: 1px solid #CCC;padding: 5px;margin: 5px 0;\"/></div>";
      }
    } else if (options.type==="type-task") {
      var url = options.task+
        "<br><div style='font-weight: normal;color:#AAA;margin-top: 6px;'>assigned to <a href='/portal/intranet/profile/"+options.username+"' style='color:#AAA' target='_new'>"+options.fullname+"</a> - due <span style='color:#ac724f'>"+options.dueDate+"</span></div>";
      out += url;
    } else if (options.type==="type-event") {
      var url = options.summary+
        "<br><div style='font-weight: normal;color:#AAA;margin-top: 6px;'>from "+options.startDate+" "+options.startTime+" to "+options.endDate+" "+options.endTime+"</div>";
      out += url;
    } else if (options.type==="type-add-team-user") {
      var users = "<b>" + options.users.replace("; ","</b>; <b>") + "</b>";
      out += thiss.labels.get("label-msg-add-team-user").replace("{0}", options.fullname).replace("{1}", users);
    } else if (options.type==="type-remove-team-user") {
      var users = "<b>" + options.users.replace("; ","</b>; <b>") + "</b>";
      out += thiss.labels.get("label-msg-remove-team-user").replace("{0}", options.fullname).replace("{1}", users);
    } else if (options.type==="type-question") {
      out += "<b>" + message + "</b>";
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
          w = "<span class='emoticon emoticon-smile'><span class='emoticon-text'>:)</span></span>";
        } else if (w == ":-p" || w==":p" || w==":-P" || w==":P") {
          w = "<span class='emoticon emoticon-tongue'><span class='emoticon-text'>:p</span></span>";
        } else if (w == ":-D" || w==":D" || w==":-d" || w==":d") {
          w = "<span class='emoticon emoticon-big-smile'><span class='emoticon-text'>:D</span></span>";
        } else if (w == ":-|" || w==":|") {
          w = "<span class='emoticon emoticon-no-voice'><span class='emoticon-text'>:|</span></span>";
        } else if (w == ":-(" || w==":(") {
          w = "<span class='emoticon emoticon-sad'><span class='emoticon-text'>:(</span></span>";
        } else if (w == ";-)" || w==";)") {
          w = "<span class='emoticon emoticon-eye-blink'><span class='emoticon-text'>;)</span></span>";
        } else if (w == ":-O" || w==":O") {
          w = "<span class='emoticon emoticon-surprise'><span class='emoticon-text'>:O</span></span>";
        } else if (w == "(beer)") {
          w = "<span class='emoticon emoticon-beer'><span class='emoticon-text'>(beer)</span></span>";
        } else if (w == "(bow)") {
          w = "<span class='emoticon emoticon-bow'><span class='emoticon-text'>(bow)</span></span>";
        } else if (w == "(bug)") {
          w = "<span class='emoticon emoticon-bug'><span class='emoticon-text'>(bug)</span></span>";
        } else if (w == "(cake)" || w == "(^)") {
          w = "<span class='emoticon emoticon-cake'><span class='emoticon-text'>(^)</span></span>";
        } else if (w == "(cash)") {
          w = "<span class='emoticon emoticon-cash'><span class='emoticon-text'>(cash)</span></span>";
        } else if (w == "(coffee)") {
          w = "<span class='emoticon emoticon-coffee'><span class='emoticon-text'>(coffee)</span></span>";
        } else if (w == "(n)" || w == "(no)") {
          w = "<span class='emoticon emoticon-no'><span class='emoticon-text'>(no)</span></span>";
        } else if (w == "(y)" || w == "(yes)") {
          w = "<span class='emoticon emoticon-yes'><span class='emoticon-text'>(yes)</span></span>";
        } else if (w == "(star)") {
          w = "<span class='emoticon emoticon-star'><span class='emoticon-text'>(star)</span></span>";
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
    return "<div class='msUserQuote'><b class='msNameUser'>" + outermostName + ":</b><div>" + outtermostContent + "</div></div>" + outermostComment;
  }
  if (numQuotes > 1) {
    if (numQuotes === numOriginQuotes) {
      return "<div class='msUserQuote'><div class='msTiltleLn clearfix'><b class='msNameUser'>" + outermostName + "</b></div><div class='msQuoteCont'>" + this.getQuote(outtermostContent, originMessage) + "</div></div>" + outermostComment;
    } else {
      return "<quote><div class='msTiltleLn clearfix'><b class='msNameUser'>" + outermostName + "</b></div><div class='msQuoteCont'>" + this.getQuote(outtermostContent, originMessage) + "</div></quote>" + outermostComment;
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
 ##################                           ##################
 ##################                           ##################
 ##################   JUZU LABELS             ##################
 ##################                           ##################
 ##################                           ##################
 */

/**
 * JuzuLabels allows to store and retrieve html5 data- value from dom elements
 * @constructor
 */
function JuzuLabels() {
  this.element = "";
}

/**
 * Sets the target DOM element
 * @param element
 */
JuzuLabels.prototype.setElement = function(element) {
  this.element = element;
};

/**
 * Get the value
 * @param key
 * @returns {*}
 */
JuzuLabels.prototype.get = function(key) {
  var val;
  if (this.element!=="") {
    val = jqchat.data(this.element, key);
    if (val === undefined) {
      val = this.element.attr("data-"+key);
      return val;
    }
  }
  return "";
};

/**
 * Set a value in the DOM using jQuery.data
 * @param key
 * @param value
 */
JuzuLabels.prototype.set = function(key, value) {
  if (this.element!=="")
    jqchat.data(this.element, key, value);
};

/**
 * Logs what are the available values (only those created by jQuery)
 */
JuzuLabels.prototype.log = function() {
  if (this.element!=="") {
    console.log(jqchat.data( this.element ));
  }
};

