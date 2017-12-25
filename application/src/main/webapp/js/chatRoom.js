(function(jqchat, TAFFY) {
  // Declare global functions (under window object)
  jzGetParam = function(key, defaultValue) {
    var ts  = localStorage.getItem(key+"TS");
    var val = localStorage.getItem(key);
    if (!ts) ts=-1;

    var now = Math.round(new Date()/1000);

    if (val !== undefined && val !== null && (now<ts || ts===-1 )) {
      return val;
    }

    return defaultValue;
  };

  jzStoreParam = function(key, value, expire) {
    expire = typeof expire !== 'undefined' ? expire : 300;
    localStorage.setItem(key+"TS", Math.round(new Date()/1000) + expire);
    localStorage.setItem(key, value);
  };

  function ChatRoom(username, token, dbName, jzChatRead, jzChatSendMeetingNotes, jzChatGetMeetingNotes, messagesContainer, portalURI) {
    this.jzChatRead = jzChatRead;
    this.jzChatSendMeetingNotes = jzChatSendMeetingNotes;
    this.jzChatGetMeetingNotes = jzChatGetMeetingNotes;
    this.messagesContainer = messagesContainer;
    this.portalURI = portalURI;
    this.dbName = dbName;
    this.isFocus = true;

    this.id = "";
    this.username = username;
    this.token = token;
    this.owner = "";
    this.targetUser = "";
    this.targetFullname = "";
    this.miniChat = undefined;
    this.users = [];
    this.messages = [];

    this.onRefreshCB;
    this.onShowMessagesCB;

    this.highlight = "";

    this.startMeetingTimestamp = "";
    this.startCallTimestamp = "";

    this.plugins = {};

    // If chatPlugins object is configured before initializing Chat Application
    if (typeof chatPlugins === 'object') {
      for (var i = 0; i < chatPlugins.length; i++) {
        this.registerPlugin(chatPlugins[i]);
      }
    }
  }

  ChatRoom.prototype.setUserPref = function(key, value, expire) {
    jzStoreParam(key + this.username, value, expire);
  }

  ChatRoom.prototype.getUserPref = function(key) {
    return jzGetParam(key + this.username);
  }

  ChatRoom.prototype.registerPlugin = function(plugin) {
    if (plugin.getType) {
      this.plugins[plugin.getType()] = plugin;
    }
  }

  ChatRoom.prototype.init = function(username, fullname, token, targetUser, targetFullname, dbName, callback) {
    this.fullname = fullname;
    this.targetUser = targetUser;
    this.targetFullname = targetFullname;
    this.owner = "";

    var thiss = this;
    var chatStatus = jqchat("#chat-status");
    var chatServerUrl = chatStatus.attr("data-chat-server-url");
    var urlRoom = chatServerUrl+"/getRoom";

    jqchat.ajax({
      url: urlRoom,
      data: {
        "targetUser": targetUser,
        "user": username,
        "dbName": thiss.dbName
      },
      headers: {
        'Authorization': 'Bearer ' + token
      },
      dataType: 'text',
      success: function (data) {
        if (thiss.id == null || thiss.id == "") {
          thiss.id = data;
        }

        thiss.callingOwner = thiss.id;

        if (typeof callback === "function") {
          callback(thiss.id);
        }

        thiss.setUserPref("lastRoom", thiss.id, 60000);
        thiss.setUserPref("lastUsername", thiss.targetUser, 60000);
        thiss.setUserPref("lastFullName", thiss.targetFullname, 60000);

        thiss.refreshChat(function () {
          var $chats = thiss.messagesContainer;

          // always scroll to the last message when loading a chat room
          if (thiss.messages.length > 0) {
            $chats.scrollTop($chats.prop('scrollHeight') - $chats.innerHeight());

            $chats.scroll(function() {
              if (jqchat(this).scrollTop() === 0) {
                // In the case of deleting a team room.
                if (!thiss.id || thiss.id == "") return;

                thiss.messagesContainer.prepend('<div class="loadMore text-center"><img src="/chat/img/sync.gif" width="64px"></div>');
                var messages = TAFFY(thiss.messages);
                var toTimestamp = messages().order("timestamp asec").first().timestamp;
                jqchat.ajax({
                  url: thiss.jzChatRead,
                  data: {
                    room: thiss.id,
                    user: thiss.username,
                    toTimestamp: toTimestamp,
                    dbName: thiss.dbName
                  },
                  headers: {
                    'Authorization': 'Bearer ' + thiss.token
                  },
                  context: this,
                  success: function (data) {
                    var loadMore = thiss.messagesContainer.find('.loadMore');
                    if (data.messages && data.messages.length > 0) {
                      var last = thiss.messagesContainer.prop('scrollHeight');

                      var $div = jqchat('<div></div>');
                      thiss.showMessages(data.messages, $div);

                      loadMore.after($div.html());
                      thiss.messagesContainer.scrollTop(thiss.messagesContainer.prop('scrollHeight') - last);
                    } else {
                      jqchat(this).off("scroll");
                    }
                    loadMore.remove();
                  }
                });
              }
            });
          } else {
            thiss.messagesContainer.prepend('<div class="noMessage"><span class="text">' + chatBundleData["exoplatform.chat.no.messages"] + '</span></div>');
            thiss.isEmpty = true;
          }

          $chats.off("click.quote");
          $chats.on("click.quote", ".msg-action-quote", function () {
            var $uimsg = jqchat(this).closest(".msg-text");
            var msgId = $uimsg.attr('id');
            var messages = TAFFY(thiss.messages);
            var tempMsg = messages({
              msgId: msgId
            });
            if (tempMsg.count() > 0) {
              var msg = tempMsg.first().msg;

              var msgHtml = msg.replace(/<br\/>/g, "\n");
              msgHtml = $('<div />').html(msgHtml).text();

              var msgFullname = $uimsg.attr("data-fn");
              jqchat("#msg").focus().val('').val("[quote=" + msgFullname + "]" + msgHtml + " [/quote] ");
            }
          });

          $chats.off("click.delete");
          $chats.on("click.delete", ".msg-action-delete", function () {
            var $uimsg = jqchat(this).closest(".msg-text");
            var msgId = $uimsg.attr("id");
            chatApplication.deleteMessage(msgId);
          });

          $chats.off("click.edit");
          $chats.on("click.edit", ".msg-action-edit", function () {
            var $uimsg = jqchat(this).closest(".msg-text");
            chatApplication.openEditMessagePopup($uimsg.attr("id"));
          });

          $chats.off("click.savenotes");
          $chats.on("click.savenotes", ".msg-action-savenotes", function () {
            var $uimsg = jqchat(this).closest(".msg-text");
            var msgTimestamp = $uimsg.attr("data-timestamp");

            var options = {
              type: "type-notes",
              fromTimestamp: msgTimestamp,
              fromUser: chatApplication.username,
              fromFullname: chatApplication.fullname
            };

            var msg = "";

            chatApplication.chatRoom.sendMessage("", options, "true");
          });

          $chats.off("click.send-meeting-notes");
          $chats.on("click.send-meeting-notes", ".send-meeting-notes", function () {
            var $this = jqchat(this);
            var $meetingNotes = $this.closest(".msMeetingNotes");
            $meetingNotes.animate({
              opacity: "toggle"
            }, 200, function () {
              var room = $this.attr("data-room");
              var from = $this.attr("data-from");
              var to = $this.attr("data-to");
              var id = $this.attr("data-id");

              from = Math.round(from) - 1;
              to = Math.round(to) + 1;

              $meetingNotes.find(".alert-success").hide();
              chatApplication.chatRoom.sendMeetingNotes(room, from, to, function (response) {
                if (response === "sent") {
                  jqchat("#" + id).animate({
                    opacity: "toggle"
                  }, 200, function () {
                    $meetingNotes.animate({
                      opacity: "toggle"
                    }, 1000);
                    jqchat("#" + id).show();
                  });
                }
              });
            });
          });

          $chats.off("click.save-meeting-notes");
          $chats.on("click.save-meeting-notes", ".save-meeting-notes", function () {
            var $this = jqchat(this);
            var $meetingNotes = $this.closest(".msMeetingNotes");
            $meetingNotes.animate({
              opacity: "toggle"
            }, 200, function () {
              var room = $this.attr("data-room");
              var from = $this.attr("data-from");
              var to = $this.attr("data-to");
              var id = $this.attr("data-id");

              from = Math.round(from) - 1;
              to = Math.round(to) + 1;

              $meetingNotes.find(".alert-success").hide();
              chatApplication.chatRoom.getMeetingNotes(room, from, to, function (response) {
                if (response !== "ko") {
                  jqchat.ajax({
                    type: "POST",
                    url: chatApplication.jzSaveWiki,
                    data: {
                      "targetFullname": chatApplication.targetFullname,
                      "content": JSON.stringify(response)
                    },
                    context: this,
                    dataType: "json",
                    success: function (data) {
                      if (data.path !== "") {
                        var baseUrl = location.protocol + "//" + location.hostname;
                        if (location.port) {
                          baseUrl += ":" + location.port;
                        }
                        var options = {
                          type: "type-link",
                          link: baseUrl + data.path,
                          from: chatApplication.username,
                          fullname: chatApplication.fullname
                        };
                        var msg = chatBundleData["exoplatform.chat.meeting.notes"];

                        chatApplication.chatRoom.sendMessage(msg, options, "true");
                      }

                      jqchat("#" + id).animate({
                        opacity: "toggle"
                      }, 200, function () {
                        $meetingNotes.animate({
                          opacity: "toggle"
                        }, 1000);
                        jqchat("#" + id).show();
                      });
                    },
                    error: function (xhr, status, error) {
                    }
                  });
                }
              });
            });
          });
        });
      }
    });
  };

  ChatRoom.prototype.onRefresh = function(callback) {
    this.onRefreshCB = callback;
  };

  ChatRoom.prototype.setNewRoom = function(bln) {
    this.loadingNewRoom = bln;
  }

  ChatRoom.prototype.setMiniChatDiv = function(elt) {
    this.miniChat = elt;
  };

  ChatRoom.prototype.onShowMessages = function(callback) {
    this.onShowMessagesCB = callback;
  };

  ChatRoom.prototype.sendMessage = function(msg, options, isSystemMessage, callback) {
    if(msg.trim().length != 0 || options.type) {
      if (jqchat.isEmptyObject(options)) {
        options = null;
      }

      var data = {
        "room": this.id,
        "clientId": new Date().getTime().toString(),
        "msg": msg,
        "user": this.username,
        "fullname": this.fullname,
        "options": options,
        "isSystem": isSystemMessage
      };

      this.addMessage(data, true);
      this.pushMessage(data, callback);

      this.messagesContainer.trigger("chat:sendMessage", data);
    }
  };

  /**
   * Empty and disable the Chat zone
   */
  ChatRoom.prototype.emptyChatZone = function(showLoading) {
    var $msg = jqchat('#msg');
    $msg.attr("disabled", "disabled");
    $msg.val('');
    var $msButtonRecord = jqchat("#chat-record-button");
    $msButtonRecord.attr("disabled", "disabled");
    $msButtonRecord.tooltip("disable");
    var $msgEmoticons = jqchat("#chat-msg-smiley-button");
    $msgEmoticons.addClass("disabled");
    $msgEmoticons.tooltip("disable");
    var $meetingActionToggle = jqchat("#chat-msg-meeting-actions");
    $meetingActionToggle.addClass("disabled");
    $meetingActionToggle.children("span").tooltip("disable");
    jqchat("#chat-room-detail-fullname").text('');
    jqchat("#chat-room-detail-avatar").hide();
    jqchat("#chat-team-button").hide();
    jqchat("#chat-video-button").hide();
    jqchat("#chat-record-button .btn").attr("disabled","");
    var $chats = jqchat("#chats");
    $chats.empty();

    // Show loading image if we are loading the messages of the room, else display "No messages"
    if(showLoading) {
      $chats.append("<div class='center'><img src='/chat/img/sync.gif' width='64px' class='chatLoading'></div>");
    } else {
      $chats.append("<div class=\"noContent\"><span class=\"text\">" + chatBundleData["exoplatform.chat.no.conversation"] + "</span></div>");
    }
  };

  /**
   * Refresh Chat : refresh messages and panels
   */
  ChatRoom.prototype.refreshChat = function(callback) {
    if(this.currentRequest) {
      this.currentRequest.abort();
      this.currentRequest = null;
    }

    if (this.id === "") return;

    if (typeof chatApplication != "undefined" && chatApplication.configMode) {
      return;//do nothing when we're on the config page
    }

    var thiss = this;
    this.currentRequest = jqchat.ajax({
      url: this.jzChatRead,
      data: {
        room: this.id,
        user: this.username,
        dbName: this.dbName
      },
      headers: {
        'Authorization': 'Bearer ' + this.token
      },
      success: function(data) {
        // data is null in case the XHR is cancelled, thus nothing to do here
        if(!data) {
          return;
        }

        // allow to execute periodic queries
        thiss.currentRequest = null;

        if (thiss.miniChat === undefined) {
          chatApplication.activateRoomButtons();
        }

        // If the requested room (returned from HTTP response) is not the same as the currently requested room
        if(thiss.loadingNewRoom && thiss.callingOwner && (!data.room || thiss.callingOwner.indexOf(data.room) < 0)) {
          return;
        }

        thiss.messagesContainer.html(''); // Clear the room
        if (data.messages && data.messages.length > 0) {
          thiss.showMessages(data.messages);
        } else if(thiss.loadingNewRoom) {
          // If room has changed but no messages was added there yet
          thiss.showMessages([]);
        }

        // set last succeeded request chat owner
        // to be able to detect when user switches from a room to another
        thiss.lastCallOwner = thiss.targetUser;
        if(thiss.loadingNewRoom) {
          // Enable composer if the new room loading has finished
          chatApplication.enableMessageComposer(true);
          thiss.setNewRoom(false);
        }

        if (typeof thiss.onRefreshCB === "function") {
          thiss.onRefreshCB(0);
        }

        if (typeof callback === "function") {
          callback(data.messages);
        }
      }
    })
  };

  ChatRoom.prototype.showAsText = function(callback) {
    var thiss = this;
    jqchat.ajax({
      url: thiss.jzChatRead,
      data: {
        room: thiss.id,
        user: thiss.username,
        isTextOnly: "true",
        dbName: thiss.dbName
      },
      headers: {
        'Authorization': 'Bearer ' + thiss.token
      },
      success: function(data) {
        if (typeof callback === "function") {
          callback(data);
        }
      }
    });
  };

  ChatRoom.prototype.sendMeetingNotes = function(room, fromTimestamp, toTimestamp, callback) {
    var serverBase = window.location.href.substr(0, 9+window.location.href.substr(9).indexOf("/"));

    var thiss = this;
    jqchat.ajax({
      url: thiss.jzChatSendMeetingNotes,
      data: {
        room: room,
        user: thiss.username,
        serverBase: serverBase,
        fromTimestamp: fromTimestamp,
        toTimestamp: toTimestamp,
        dbName: thiss.dbName
      },
      headers: {
        'Authorization': 'Bearer ' + thiss.token
      },
      success: function(data) {
        if (typeof callback === "function") {
          callback(data);
        }
      }
    });

  };

  ChatRoom.prototype.getMeetingNotes = function(room, fromTimestamp, toTimestamp, callback) {
    var serverBase = window.location.href.substr(0, 9+window.location.href.substr(9).indexOf("/"));

    var thiss = this;
    jqchat.ajax({
      url: thiss.jzChatGetMeetingNotes,
      data: {
        room: room,
        user: thiss.username,
        serverBase: serverBase,
        portalURI: thiss.portalURI,
        fromTimestamp: fromTimestamp,
        toTimestamp: toTimestamp,
        dbName: thiss.dbName
      },
      headers: {
        'Authorization': 'Bearer ' + thiss.token
      },
      success: function(data) {
        if (typeof callback === "function") {
          callback(data);
        }
      }
    });
  };

  /**
   * Return the storage key of pending messages for this room.
   */
  ChatRoom.prototype.getPendingMessagesKey = function () {
    return "exo.chat.pending.msgs." + this.username;
  }

  /**
   * Return an array of pending messages from stack for this room.
   */
  ChatRoom.prototype.getPendingMessages = function () {
    var msgs = localStorage.getItem(this.getPendingMessagesKey());

    if (msgs) {
      msgs = JSON.parse(msgs);
    } else {
      msgs = [];
    }

    return msgs;
  }

  /**
   * Add a message into pending stack of this room for checking to push to server later.
   */
  ChatRoom.prototype.addPendingMessage = function(msg) {
    var msgs = this.getPendingMessages();
    msgs.push(msg);

    localStorage.setItem(this.getPendingMessagesKey(), JSON.stringify(msgs));
  }

  /**
   * Remove a pending message from stack for this room.
   */
  ChatRoom.prototype.removePendingMessage = function(msg) {
    var msgs = this.getPendingMessages();
    var db = TAFFY(msgs);

    db({clientId: msg.clientId}).remove();
    localStorage.setItem(this.getPendingMessagesKey(), JSON.stringify(db().get()));
  }

  /**
   * Push pending messages from stack to server for this room.
   */
  ChatRoom.prototype.pushPendingMessages = function() {
    var localMsg = this.getPendingMessages();

    if (localMsg.length > 0) {
      localMsg.forEach(function(msg) {
        this.pushMessage(msg);
      }, this);
    } else {
      localStorage.removeItem(this.getPendingMessagesKey());
    }
  }

  /**
   * Push message to server
   * @param msg : the message object to send
   * @param callback : the method to execute on success
   */
  ChatRoom.prototype.pushMessage = function(data, callback) {
    var content = JSON.stringify({
      "event": "message-sent",
      "sender": this.username,
      "token": this.token,
      "dbName": this.dbName,
      "data": data
    });

    requireChatCometd(function(cCometD) {
      cCometD.publish('/service/chat', content, function(publishAck) {
        if (publishAck.successful) {
          if (typeof callback === "function") {
            callback();
          }
        }
      });
    });
  };

  /**
   * Convert local messages list in HTML output to display the list of messages
   */
  ChatRoom.prototype.showMessages = function(msgs, $container) {
    if (msgs) {
      this.messages = msgs;
    } else {
      msgs = this.messages || [];
    }

    if (msgs.length > 0) {
      var thiss = this;
      var messages = TAFFY(msgs);

      messages().order("timestamp asec").each(function (message, i) {
        thiss.showMessage(message, false, $container);
      });
    }

    if (typeof this.onShowMessagesCB === "function") {
      this.onShowMessagesCB();
    }
  };

  ChatRoom.prototype.updateMessage = function(message, withClientMsg) {
    var $msg;
    if (withClientMsg) {
      $msg = jqchat("#" + message.clientId);
    } else {
      $msg = jqchat("#" + message.msgId);
    }
    var out = this.generateMessageHTML(message);
    $msg.replaceWith(out);
  }

  /*
   * A message object:
   * {
   *    "id": "589affe904e5fe0b2efc1ac0",
   *    "user": "trongtt",
   *    "fullname": "Trong Changed Tran",
   *    "email": "trongtt@gmail.com",
   *    "date": "06:24 PM",
   *    "message": "sdg",
   *    "options": "",
   *    "type": "null",
   *    "isSystem": "false"
   * }
   */
  ChatRoom.prototype.addMessage = function(msg, checkToScroll) {
    // A server message
    if (msg.msgId) {
      var messages = TAFFY(this.messages);
      var tempMsg = messages({
        clientId: msg.clientId
      });

      // Update local message with server message.
      if (tempMsg.count() > 0) {
        tempMsg.update(msg);
        this.messages = messages().get();
        this.updateMessage(msg, true);
        return;
      }
    } else {
      this.addPendingMessage(msg);
    }

    this.messages.push(msg);
    this.showMessage(msg, checkToScroll);
  }

  ChatRoom.prototype.showMessage = function(message, checkToScroll, $container) {
    if ($container) {
      var $chats = $container;
    } else {
      var $chats = this.messagesContainer;
    }

    // Remove the empty icon
    if (this.isEmpty) {
      $chats.find('.noMessage').remove();
      this.isEmpty = false;
    }

    if (checkToScroll) {
      // check if scroll was at max before the new message
      var scrollTopMax = $chats.prop('scrollHeight') - $chats.innerHeight();
      var scrollAtMax = ($chats.scrollTop() == scrollTopMax);
    }

    var $lastMessage = $chats.children().last();
    var prevUser = $lastMessage.data("user");

    var out = '';
    // Check if it is a system message
    if (message.isSystem === "true" || message.isSystem === true) {
      var hideWemmoMessage = "";
      if (message.options !== undefined && (message.options.type === 'call-on' || message.options.type === 'call-off' || message.options.type === 'call-proceed' )) {
        hideWemmoMessage = "style='display:none;'";
      }

      var $msgDiv = jqchat('<div class="msRow" ' + hideWemmoMessage + ' data-user="__system">');
      $chats.append($msgDiv);

      var options = {};
      if (typeof message.options == "object")
        options = message.options;

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

      out += '    <div class="msMessagesGroup clearfix">';
      out += this.getActionMeetingStyleClasses(options);

      out += "          <div class='msContBox'>";
      out += "            <div class='inner'>";
      if (message.options !== undefined && message.options.type !== 'type-add-team-user' && message.options.type !=='type-remove-team-user' && message.options.type !=='type-kicked'  ) {
        out += "            <div class='msTiltleLn'>";
        out += "              <a class='msNameUser muted' href='/portal/intranet/profile/"+message.user+"'>" +message.fullname  + "</a>";
        out += "            </div>";
      }
      if (message.msgId) {
        out += "              <div id='" + message.msgId + "' class='msUserCont noEdit msg-text'>";
      } else {
        out += "              <div id='" + message.clientId + "' class='msUserCont noEdit msg-text pending'>";
      }
      var msRightInfo = "     <div class='msRightInfo pull-right'>";
      msRightInfo += "          <div class='msTimePost'>";
      msRightInfo +=              this.getMessageInfo(message);
      msRightInfo += "          </div>";
      msRightInfo += "        </div>";
      var msUserMes = "       <div class='msUserMes'>" + this.messageBeautifier(message, options) + "</div>";
      if (this.miniChat === undefined) {
        out += msRightInfo;
        out += msUserMes;
      } else {
        out += msUserMes;
        out += msRightInfo;
      }
      out += "              </div>";
      out += "            </div>";
      out += "          </div>";
      // End msMessageGroup div
      out += '    </div>';
      $msgDiv.append(out);

      if (options.type !== "call-join" && (options.type.indexOf('call-') !== -1)) {
        if (options.uidToCall!==undefined && options.displaynameToCall!==undefined) {
          if (typeof weemoExtension!=="undefined") {
            weemoExtension.setUidToCall(options.uidToCall);
            weemoExtension.setDisplaynameToCall(options.displaynameToCall);
            if (options.meetingPointId!==undefined) {
              weemoExtension.setMeetingPointId(options.meetingPointId);
            }
          }
          jqchat(".btn-weemo").css("display", "none");
          jqchat(".btn-weemo-conf").css("display", "block");
          if (typeof weemoExtension!=="undefined") {
            if (options.uidToCall!=="weemo"+thiss.username)
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
    } else {  // An user message
      if (message.user != prevUser) {
        $msgDiv = jqchat('<div class="msRow" data-user="' + message.user + '">');
        $chats.append($msgDiv);

        if (message.user == this.username) {
          $msgDiv.addClass("rowOdd odd msMy");
        }

        out += "<div class='msMessagesGroup clearfix'>" +
          "<div class='msUserAvatar'>";
        out +=    "<a class='msAvatarLink avatarCircle' href='" + this.portalURI + "profile/" + message.user + "'><img onerror=\"this.src='/chat/img/user-default.jpg'\" src='/rest/v1/social/users/" + message.user + "/avatar' alt='" + message.fullname + "'></a>";
        out +=    "</div>" +
          "<div class='msContBox'>" +
          "<div class='inner'>" +
          "<div class='msTiltleLn'>";
        out +=        "<a class='msNameUser muted' href='/portal/intranet/profile/"+message.user+"'>" +message.fullname  + "</a>";
        out +=        "</div>";
      } else {
        $msgDiv = $lastMessage.find(".inner");
      }

      out += this.generateMessageHTML(message);

      if (message.user != prevUser) {
        out +=      '</div>' +
          '</div>' +
          '</div>';
      }

      $msgDiv.append(out);
    }

    // if scroll was at max, scroll to the new max to display the new message. Otherwise don't move the scroll.
    if (checkToScroll && scrollAtMax) {
      $chats.scrollTop($chats.prop('scrollHeight') - $chats.innerHeight());
    }
  }

  ChatRoom.prototype.generateMessageHTML = function(message) {
    var out = '';
    var tempMsg;
    var noEditCssClass = "";
    if (message.type === "DELETED") {
      tempMsg = "<span class='contentDeleted empty'>"+chatBundleData["exoplatform.chat.deleted"]+"</span>";
      noEditCssClass = "noEdit";
    } else {
      tempMsg = this.messageBeautifier(message, message.options);
      if (message.isSystem === true) {
        noEditCssClass = "noEdit";
      }
    }
    if (message.msgId) {
      out += '          <div id="' + message.msgId + '" class="msUserCont msg-text ' + noEditCssClass + '" data-fn="' + message.fullname + '" data-timestamp="' + message.timestamp + '">';
    } else {
      out += '          <div id="' + message.clientId + '" class="msUserCont msg-text noEdit pending" data-fn="' + message.fullname + '" data-timestamp="' + message.timestamp + '">';
    }

    var msRightInfo = "";
    msRightInfo += "      <div class='msRightInfo pull-right'>";
    msRightInfo += "        <div class='msTimePost'>";
    if (message.type === "DELETED" || message.type === "EDITED") {
      msRightInfo += "        <span href='#' class='msEditMes'><i class='uiIconChatEdited uiIconChatLightGray'></i></span>";
    }
    msRightInfo +=            this.getMessageInfo(message);
    msRightInfo += "        </div>";
    if (message.type !== "DELETED" && message.isSystem !== true) {
      msRightInfo += "      <div class='msAction msg-actions'>";
      msRightInfo += "        <a href='#' class='msg-action-savenotes'>" + chatBundleData["exoplatform.chat.notes"] + "</a> |";
      if (message.user === this.username) {
        msRightInfo += "      <a href='#' class='msg-action-edit'>" + chatBundleData["exoplatform.chat.edit"] + "</a> |";
        msRightInfo += "      <a href='#' class='msg-action-delete'>" + chatBundleData["exoplatform.chat.delete"] + "</a> |";
      }
      msRightInfo += "        <a href='#' class='msg-action-quote'>" + chatBundleData["exoplatform.chat.quote"] + "</a>";
      msRightInfo += "       </div>";
    }
    msRightInfo += "       </div>";
    var msUserMes  = "         <div class='msUserMes'>" + tempMsg + "</div>";
    if (this.miniChat === undefined) {
      out += msRightInfo;
      out += msUserMes;
    } else {
      out += msUserMes;
      out += msRightInfo;
    }

    out += '          </div>';

    return out;
  }

  ChatRoom.prototype.getActionMeetingStyleClasses = function(options) {
    var actionType = options.type;
    var out = "";

    if (actionType.indexOf("type-") !== -1 || actionType.indexOf("call-") !== -1) {
      out += "<div class='msUserAvatar'>";
      if ("type-question" === actionType) {
        out += "<i class='uiIconChat32x32Question uiIconChat32x32LightGray'></i>";
      } else if ("type-hand" === actionType) {
        out += "<i class='uiIconChat32x32RaiseHand uiIconChat32x32LightGray'></i>";
      } else if ("type-file" === actionType) {
        out += "<i class='uiIconChat32x32ShareFile uiIconChat32x32LightGray'></i>";
      } else if ("type-link" === actionType) {
        out += "<i class='uiIconChat32x32HyperLink uiIconChat32x32LightGray'></i>";
      } else if ("type-event" === actionType) {
        out += "<i class='uiIconChat32x32Event uiIconChat32x32LightGray'><span class='dayOnCalendar time'>" + options.startDate.substr(3, 2) + "</span></i>";
      } else if ("type-notes" === actionType || "type-meeting-start" === actionType || "type-meeting-stop" === actionType) {
        out += "<i class='uiIconChat32x32Metting uiIconChat32x32LightGray'></i>";
      } else if ("call-on" === actionType) {
        out += "<i class='uiIconChat32x32StartCall uiIconChat32x32LightGray'></i>";
      } else if ("call-join" === actionType) {
        out += "<i class='uiIconChat32x32AddPeopleToMeeting uiIconChat32x32LightGray'></i>";
      } else if ("call-off" === actionType) {
        out += "<i class='uiIconChat32x32FinishCall uiIconChat32x32LightGray'></i>";
      } else if ("call-proceed" === actionType) {
        out += "<i class='uiIconChat32x32AddCall uiIconChat32x32LightGray'></i>";
      } else if (this.plugins[actionType] && this.plugins[actionType].getActionMeetingStyleClasses) {
        var plugin = this.plugins[actionType];
        out += plugin.getActionMeetingStyleClasses(options);
      }
      out += "</div>";
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


  /**
   * HTML Message Beautifier
   *
   * @param message
   * @returns {string} : the html markup
   */
  ChatRoom.prototype.messageBeautifier = function(objMessage, options) {
    var message = objMessage.msg;
    if (!objMessage.msgId) {
      // HTML encoding for local displaying message.
      message = jqchat("<div></div>").text(message).html();
      message = message.replace(/\n/g, '<br>');
    }
    var msg = "";
    var thiss = this;
    if (options!==undefined && !jqchat.isEmptyObject(options)) {
      var out = "";

      if (options.type ==="type-me") {
        var urlProfile = "<a href='" + thiss.portalURI + "profile/"+options.username+"' target='_blank'>"+options.fullname+"</a>";
        var text = message.replace("/me", urlProfile);
        out += "<center>"+text+"</center>";
      } else if (options.type ==="type-file") {
        var urlFile = "<a class='msLinkInMes' href='"+options.restPath+"' target='_blank'>"+options.title+"</a> ";
        var size = "<span class=\"fileSize\">("+options.sizeLabel+")</span>";
        out += urlFile + size;
        var link = options.restPath;
        if (link.endsWith(".png") || link.endsWith(".jpg") || link.endsWith(".gif") ||
          link.endsWith(".PNG") || link.endsWith(".JPG") || link.endsWith(".GIF")) {
          out += "<div class='msAttachmentBox'><div class='msAttachFile'><img src=\""+options.restPath+"\"/></div><div class='msActionAttach'><div class='inner'><div><a href='" + options.restPath + "' target='_blank'><i class='uiIconSearch uiIconWhite'></i> " + chatBundleData["exoplatform.chat.view"] + "</a></div><div><a href='"+options.downloadLink+"' target='_blank'><i class='uiIconDownload uiIconWhite'></i> " + chatBundleData["exoplatform.chat.download"] + "</a></div></div></div></div>";
        }

      } else if (options.type==="type-link") {
        var link = options.link.toLowerCase();
        var url = "<a href='"+options.link+"' target='_blank'>"+options.link+"</a>";
        out += url;
        if (link.endsWith(".png") || link.endsWith(".jpg") || link.endsWith(".gif") ||
          link.endsWith(".PNG") || link.endsWith(".JPG") || link.endsWith(".GIF")) {
          out += "<div><img src=\""+options.link+"\" style=\"max-width: 200px;max-height: 140px;border: 1px solid #CCC;padding: 5px;margin: 5px 0;\"/></div>";
        }
      } else if (options.type==="type-event") {
        var summary = options.summary;
        var location = options.location;
        if (!objMessage.msgId) {
          summary = jqchat("<div></div>").text(summary).html();
          location = jqchat("<div></div>").text(location).html();
        }
        out += "<b>" + summary + "</b>";
        out += "<div class='msTimeEvent'>";
        out += "  <div>";
        out += "    <i class='uiIconChatClock uiIconChatLightGray mgR20'></i><span class='muted'>" + chatBundleData["exoplatform.chat.from"] + ": </span><b class='mgR5'>" + options.startDate + " " + options.startTime + "</b><span class='muted'>" + chatBundleData["exoplatform.chat.to"] + ": </span><b>" + options.endDate + " " + options.endTime + "</b>";
        out += "  </div>";
        out += "  <div>";
        out += "    <i class='uiIconChatCheckin uiIconChatLightGray mgR20'></i>" + location;
        out += "  </div>";
        out += "</div>";
      } else if (options.type==="type-add-team-user") {
        var users = "<b>" + options.users.replace("; ","</b>; <b>") + "</b>";
        out += chatBundleData["exoplatform.chat.team.msg.adduser"].replace("{0}", "<b>" + options.fullname + "</b>").replace("{1}", users);
      } else if (options.type==="type-remove-team-user") {
        var users = "<b>" + options.users.replace("; ","</b>; <b>") + "</b>";
        out += chatBundleData["exoplatform.chat.team.msg.removeuser"].replace("{0}", "<b>" + options.fullname + "</b>").replace("{1}", users);
      } else if (options.type==="type-kicked") {
        out += "<b>" + chatBundleData["exoplatform.chat.team.msg.kicked"] + "</b>";
      } else if (options.type==="type-question" || options.type==="type-hand") {
        out += "<b>" + message + "</b>";
      } else if (options.type==="type-notes") {
        out += "<b>" + chatBundleData["exoplatform.chat.notes.saved"] + "</b>";
        out += "<div class='msMeetingNotes'>";
        out += "  <div>";
        out += "    <i class='uiIconChatSendEmail uiIconChatLightGray mgR10'></i>";
        out += "    <a class='send-meeting-notes' href='#' data-from='" + options.fromTimestamp + "' data-to='" + objMessage.timestamp + "' data-room='" + this.id + "' data-owner='" + this.username +"' data-id='" + objMessage.timestamp + "'>" + chatBundleData["exoplatform.chat.send.notes"] + "</a>";
        out += "  </div>";
        out += "  <div>";
        out += "    <i class='uiIconChatWiki uiIconChatLightGray mgR10'></i>";
        out += "    <a class='save-meeting-notes' href='#' data-from='" + options.fromTimestamp + "' data-to='" + objMessage.timestamp + "' data-room='" + this.id + "' data-owner='" + this.username +"' data-id='" + objMessage.timestamp + "2'>" + chatBundleData["exoplatform.chat.save.wiki"] + "</a>";
        out += "  </div>";
        out += "  <div class='alert alert-success' id='"+objMessage.timestamp+"' style='display:none;'><button type='button' class='close' onclick='jqchat(\"#"+objMessage.timestamp+"\").hide();' style='right: 0;'>×</button><strong>"+chatBundleData["exoplatform.chat.sent"]+"</strong> "+chatBundleData["exoplatform.chat.check.mailbox"]+"</div>";
        out += "  <div class='alert alert-success' id='"+objMessage.timestamp+"2' style='display:none;'><button type='button' class='close' onclick='jqchat(\"#"+objMessage.timestamp+"2\").hide();' style='right: 0;'>×</button><strong>"+chatBundleData["exoplatform.chat.saved"]+"</strong> <a href=\"" + thiss.portalURI + "wiki\">"+chatBundleData["exoplatform.chat.open.wiki"]+"</a>.</div>";
        out += "</div>";
      } else if (options.type==="type-meeting-start") {
        out += "<b>" + chatBundleData["exoplatform.chat.meeting.started"] + "</b>";
        out += "<p><i class='muted'>" + chatBundleData["exoplatform.chat.meeting.started.message"] + "</i></p>";

        thiss.startMeetingTimestamp = objMessage.timestamp;
        if (thiss.miniChat === undefined) {
          chatApplication.updateMeetingButtonStatus(true);
        }
      } else if (options.type==="type-meeting-stop") {
        out += "<b>" + chatBundleData["exoplatform.chat.meeting.finished"] + "</b>";
        var isStopedByCurrentUser = (thiss.username === options.fromUser);
        if (isStopedByCurrentUser) {
          out += "<div class='msMeetingNotes'>";
          out += "  <div>";
          out += "    <i class='uiIconChatSendEmail uiIconChatLightGray mgR10'></i>";
          out += "    <a class='" + (isStopedByCurrentUser ? "send-meeting-notes" : "") + "' href='" + (isStopedByCurrentUser ? "javascript:void(0);" : "javascript:alert(\"Only the participants who stopped the session can send or save meeting notes!\");") + "' data-from='" + thiss.startMeetingTimestamp + "' data-to='" + objMessage.timestamp + "' data-room='" + this.id + "' data-owner='" + this.username + "' data-id='" + objMessage.timestamp + "'>" + chatBundleData["exoplatform.chat.send.notes"] + "</a>";
          out += "  </div>";
          out += "  <div>";
          out += "    <i class='uiIconChatWiki uiIconChatLightGray mgR10'></i>";
          out += "    <a class='" + (isStopedByCurrentUser ? "save-meeting-notes" : "") + "' href='" + (isStopedByCurrentUser ? "javascript:void(0);" : "javascript:alert(\"Only the participants who stopped the session can send or save meeting notes!\");") + "' data-from='" + thiss.startMeetingTimestamp + "' data-to='" + objMessage.timestamp + "' data-room='" + this.id + "' data-owner='" + this.username + "' data-id='" + objMessage.timestamp + "2'>" + chatBundleData["exoplatform.chat.save.wiki"] + "</a>";
          out += "  </div>";
          out += "  <div class='alert alert-success' id='" + objMessage.timestamp + "' style='display:none;'><button type='button' class='close' onclick='jqchat(\"#" + objMessage.timestamp + "\").hide();' style='right: 0;'>×</button><strong>" + chatBundleData["exoplatform.chat.sent"] + "</strong> " + chatBundleData["exoplatform.chat.check.mailbox"] + "</div>";
          out += "  <div class='alert alert-success' id='" + objMessage.timestamp + "2' style='display:none;'><button type='button' class='close' onclick='jqchat(\"#" + objMessage.timestamp + "2\").hide();' style='right: 0;'>×</button><strong>" + chatBundleData["exoplatform.chat.saved"] + "</strong> <a href=\"" + thiss.portalURI + "wiki\">" + chatBundleData["exoplatform.chat.open.wiki"] + "</a>.</div>";
          out += "</div>";
        }
        if (thiss.miniChat === undefined) {
          chatApplication.updateMeetingButtonStatus(false);
        }
      } else if (options.type==="call-on") {
        this.startCallTimestamp = objMessage.timestamp;
        out += "<b>" + chatBundleData["exoplatform.chat.meeting.started"] + "</b>";
      } else if (options.type==="call-join") {
        out += "<b>" + chatBundleData["exoplatform.chat.meeting.joined"] + "</b>";
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
            stime += hours+ " "+chatBundleData["exoplatform.chat.hour"]+" ";
          else
            stime += hours+ " "+chatBundleData["exoplatform.chat.hours"]+" ";
        }
        if (minutes>0) {
          if (minutes===1)
            stime += minutes+ " "+chatBundleData["exoplatform.chat.minute"]+" ";
          else
            stime += minutes+ " "+chatBundleData["exoplatform.chat.minutes"]+" ";
        }
        if (seconds>0) {
          if (seconds===1)
            stime += seconds+ " "+chatBundleData["exoplatform.chat.second"];
          else
            stime += seconds+ " "+chatBundleData["exoplatform.chat.seconds"];
        }
        stime += "</span>";
        out += "<b>" + chatBundleData["exoplatform.chat.meeting.finished"] + "</b> " + stime;

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
            ">"+chatBundleData["exoplatform.chat.send.notes"]+"</a>" +
            " - " +
            "<a href='#' class='save-meeting-notes' " +
            "data-from='"+jzGetParam("weemoCallHandlerFrom")+"' " +
            "data-to='"+jzGetParam("weemoCallHandlerTo")+"' " +
            "data-room='"+this.id+"' " +
            "data-owner='"+this.username +"' " +
            "data-id='"+options.timestamp+"2' " +
            ">"+chatBundleData["exoplatform.chat.save.wiki"]+"</a>" +
            "</span>" +
            "<div class='alert alert-success' id='"+options.timestamp+"' style='display:none;'><button type='button' class='close' onclick='jqchat(\"#"+options.timestamp+"\").hide();' style='right: 0;'>×</button><strong>"+chatBundleData["exoplatform.chat.sent"]+"</strong> "+chatBundleData["exoplatform.chat.check.mailbox"]+"</div>" +
            "<div class='alert alert-success' id='"+options.timestamp+"2' style='display:none;'><button type='button' class='close' onclick='jqchat(\"#"+options.timestamp+"2\").hide();' style='right: 0;'>×</button><strong>"+chatBundleData["exoplatform.chat.saved"]+"</strong> <a href=\"" + thiss.portalURI + "wiki\">"+chatBundleData["exoplatform.chat.open.wiki"]+"</a>.</div>" +
            "</div>";
        }
      } else if (options.type==="call-proceed") {
        out += "<b>" + chatBundleData["exoplatform.chat.call.comming"]  + "...</b>";
      } else if (this.plugins[options.type] && this.plugins[options.type].messageBeautifier) {
        var plugin = this.plugins[options.type];
        out += plugin.messageBeautifier(objMessage, options);
      } else {
        out += message;
      }

      return out;
    }


    if (message.indexOf("java:")===0) {
      msg = "<div class='sh_container '><pre class='sh_java'>"+message.substr(5)+"</pre></div>";
      this.highlightCodeStyle();
      return msg;
    } else if (message.indexOf("html:")===0) {
      msg = "<div class='sh_container '><pre class='sh_html'>"+message.substr(5)+"</pre></div>";
      this.highlightCodeStyle();
      return msg;
    } else if (message.indexOf("js:")===0) {
      msg = "<div class='sh_container '><pre class='sh_javascript'>"+message.substr(3)+"</pre></div>";
      this.highlightCodeStyle();
      return msg;
    } else if (message.indexOf("css:")===0) {
      msg = "<div class='sh_container '><pre class='sh_css'>"+message.substr(4)+"</pre></div>";
      this.highlightCodeStyle();
      return msg;
    }



    var lines = message.split("<br/>");
    var il,l;
    for (il=0 ; il<lines.length ; il++) {
      l = lines[il];
      if (l.indexOf("google:")===0) {
        msg += "google:<a href='http://www.google.com/search?q="+l.substr(7, l.length-7)+"' target='_blank'>"+l.substr(7, l.length-7)+"</a> ";
      } else if (l.indexOf("wolfram:")===0) {
        msg += "wolfram:<a href='http://www.wolframalpha.com/input/?i="+l.substr(8, l.length-8)+"' target='_blank'>"+l.substr(8, l.length-8)+"</a> ";
      } else {
        var tab = l.split(" ");
        var it,w;
        for (it=0 ; it<tab.length ; it++) {
          w = tab[it];
          if (w.indexOf("google:")===0) {
            w = "google:<a href='http://www.google.com/search?q="+w.substr(7, w.length-7)+"' target='_blank'>"+w.substr(7, w.length-7)+"</a>";
          } else if (w.indexOf("wolfram:")===0) {
            w = "wolfram:<a href='http://www.wolframalpha.com/input/?i="+w.substr(8, w.length-8)+"' target='_blank'>"+w.substr(8, w.length-8)+"</a>";
          } else if (w.indexOf("/")>-1 && w.indexOf("&lt;/")===-1 && w.indexOf("/&gt;")===-1) {
            var link = w;
            if (w.endsWith(".jpg") || w.endsWith(".png") || w.endsWith(".gif") || w.endsWith(".JPG") || w.endsWith(".PNG") || w.endsWith(".GIF")) {
              w = "<a href='"+w+"' target='_blank'><img src='"+w+"' width='100%' /></a>";
              w += "<span class='invisible-text'>"+link+"</span>";
            } else if (w.indexOf("http://www.youtube.com/watch?v=")===0 && !this.IsIE8Browser() ) {
              var id = w.substr(31);
              w = "<iframe width='100%' src='http://www.youtube.com/embed/"+id+"' frameborder='0' allowfullscreen></iframe>";
              w += "<span class='invisible-text'>"+link+"</span>";
            } else if (w.indexOf("[/quote]")===-1 && (w.indexOf("http:")===0 || w.indexOf("https:")===0 || w.indexOf("ftp:")===0) ) {
              w = "<a href='"+w+"' target='_blank'>"+w+"</a>";
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

  ChatRoom.prototype.getMessageInfo = function (msg) {
    if (msg.msgId) {
      return '<span class="msg-date time">' + this.getDate(msg.timestamp) + "</span>";
    } else {
      return '<i class="uiIconNotification" data-toggle="tooltip" data-placement="top" title="' + chatBundleData["exoplatform.chat.msg.notDelivered"] + '"></i>';
    }
  }
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
    var thiss = this;
    requireChatCometd(function(cCometD) {
      cCometD.publish('/service/chat', JSON.stringify({
        "event": "message-read",
        "room": thiss.id,
        "sender": thiss.username,
        "dbName": thiss.dbName,
        "token": thiss.token
      }), function(publishAck) {
        if (publishAck.successful) {
          if (typeof callback === "function") {
            callback();
          }
        }
      });
    });
  };

  ChatRoom.prototype.highlightCodeStyle = function() {
    // Use setTimeout as a workaround to invoke sh_highlightDocument() once
    // when showing many messages at the same time.
    if (this.isSetHighlightCodeStyle) {
      clearTimeout(this.isSetHighlightCodeStyle);
    }

    this.isSetHighlightCodeStyle = setTimeout(function() {
      window.require(['SHARED/shjs'], function(sh_highlightDocument) {
        sh_highlightDocument();
      });
    }, 50);
  }

  return ChatRoom;
})($, taffy);