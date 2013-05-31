var console = console || {
  log:function(){},
  warn:function(){},
  error:function(){}
};

$(document).ready(function(){

  var highlight = "";
  var ANONIM_USER = "__anonim_";
  var SUPPORT_USER = "__support_";
  var isAdmin = false;

  var $chatApplication = $("#chat-application");
  var username = $chatApplication.attr("data-username");
  var token = $chatApplication.attr("data-token");
  var chatServerURL = $chatApplication.attr("data-chat-server-url");
  var chatIntervalChat = $chatApplication.attr("data-chat-interval-chat");
  var chatIntervalSession = $chatApplication.attr("data-chat-interval-session");
  var chatIntervalStatus = $chatApplication.attr("data-chat-interval-status");
  var chatIntervalUsers = $chatApplication.attr("data-chat-interval-users");
  var chatPublicMode = $chatApplication.attr("data-public-mode");
  var chatView = $chatApplication.attr("data-view");
  var chatFullscreen = $chatApplication.attr("data-fullscreen");
  var weemoKey = $chatApplication.attr("data-weemo-key");
  var isPublic = (chatPublicMode == "true" && chatView == "public");

  var labelPanelError1 = $chatApplication.attr("data-label-panel-error1");
  var labelPanelError2 = $chatApplication.attr("data-label-panel-error2");
  var labelPanelLogin1 = $chatApplication.attr("data-label-panel-login1");
  var labelPanelLogin2 = $chatApplication.attr("data-label-panel-login2");
  var labelPanelDemo = $chatApplication.attr("data-label-panel-demo");
  var labelPanelPublic = $chatApplication.attr("data-label-panel-public");
  var labelDisplayName = $chatApplication.attr("data-label-display-name");
  var labelEmail = $chatApplication.attr("data-label-email");
  var labelSaveProfile = $chatApplication.attr("data-label-save-profile");
  var labelTitle = $chatApplication.attr("data-label-title");
  var labelNewMessages = $chatApplication.attr("data-label-new-messages");
  var labelAvailable = $chatApplication.attr("data-label-available");
  var labelAway = $chatApplication.attr("data-label-away");
  var labelDoNotDisturb = $chatApplication.attr("data-label-donotdisturb");
  var labelInvisible = $chatApplication.attr("data-label-invisible");
  var labelCurrentStatus = $chatApplication.attr("data-label-current-status");
  var labelNoMessages = $chatApplication.attr("data-label-no-messages");
  var labelPublicWelcome = $chatApplication.attr("data-label-public-welcome");
  var labelSupportFullname = $chatApplication.attr("data-label-support-fullname");


  var jzInitChatProfile = $chatApplication.jzURL("ChatApplication.initChatProfile");
  var jzCreateDemoUser = $chatApplication.jzURL("ChatApplication.createDemoUser");
  var jzMaintainSession = $chatApplication.jzURL("ChatApplication.maintainSession");
  var jzGetStatus = chatServerURL+"/getStatus";
  var jzSetStatus = chatServerURL+"/setStatus";
  var jzChatWhoIsOnline = chatServerURL+"/whoIsOnline";
  var jzChatSend = chatServerURL+"/send";
  var jzChatGetRoom = chatServerURL+"/getRoom";
  var jzChatToggleFavorite = chatServerURL+"/toggleFavorite";
  var jzChatUpdateUnreadMessages = chatServerURL+"/updateUnreadMessages";
  var room = "<%=room%>";
  var old = '';
  var chatEventSource;
  var targetUser;
  var fullname;
  var targetFullname;

  var chatEventURL;
  var chatEventInt;
  var chatOnlineInt;
  var chatSessionInt;
  var notifStatusInt;
  var firstLoad = true;
  var userFilter = "";
  var isLoaded = false;
  var messages = [];
  var filterInt;
  var keydown = -1;
  var profileStatus = "offline";
  var whoIsOnlineMD5 = 0;
  var callOwner=false;
  var messageWeemo, uidToCall, displaynameToCall, callType, callActive=false;
  var isSystemMessage;

  $("#PlatformAdminToolbarContainer").addClass("no-user-selection");

  if (chatFullscreen == "true") {
    $("#PlatformAdminToolbarContainer").css("display", "none");
  }


  function checkViewportStatus() {
    return ($(".btn-mobile").css("display")!=="none");
  }

  function isMobileView() {
    return checkViewportStatus();
  }

  function isDesktopView() {
    return !checkViewportStatus();
  }

  $.fn.setCursorPosition = function(position){
    if(this.length === 0) return this;
    return $(this).setSelection(position, position);
  };

  $.fn.setSelection = function(selectionStart, selectionEnd) {
    if(this.length === 0) return this;
    input = this[0];

    if (input.createTextRange) {
      var range = input.createTextRange();
      range.collapse(true);
      range.moveEnd('character', selectionEnd);
      range.moveStart('character', selectionStart);
      range.select();
    } else if (input.setSelectionRange) {
      input.focus();
      input.setSelectionRange(selectionStart, selectionEnd);
    }

    return this;
  };

  $.fn.focusEnd = function(){
    this.setCursorPosition(this.val().length);
    return this;
  };

  $(window).unload(function() {
    hidePanels();
  });

  $('#msg').focus(function() {
    //console.log("focus on msg : "+targetUser+":"+room);
    $.ajax({
      url: jzChatUpdateUnreadMessages,
      data: {"room": room,
        "user": username,
        "token": token,
        "timestamp": new Date().getTime()
      },

      success:function(response){
        //console.log("success");
      },

      error:function (xhr, status, error){

      }

    });

  });

  $('#msg').keydown(function(event) {
    if ( event.which == 18 ) {
      keydown = 18;
    }
  });

  $('#msg').keyup(function(event) {
    var msg = $(this).val();
    // console.log(event.which + " ;"+msg.length+";");
    if ( event.which == 13 && keydown !== 18 && msg.length>1) {
      //console.log("sendMsg=>"+username + " : " + room + " : "+msg);
      if(!msg)
      {
        return;
      }
//      console.log("*"+msg+"*");
      isSystemMessage = (msg.indexOf("/")===0) ;

      if (isSystemMessage) {
        msg = msg.replace("/me", fullname);

        if (msg.indexOf("/call")===0) {
          createWeemoCall();
          document.getElementById("msg").value = '';
          return;
        } else if (msg.indexOf("/join")===0) {
          joinWeemoCall();
          document.getElementById("msg").value = '';
          return;
        } else if (msg.indexOf("/terminate")===0) {
          document.getElementById("msg").value = '';
          ts = Math.round(new Date().getTime() / 1000);
          msg = "Call terminated&"+ts;
          callOwner = false;
          callActive = false;
        }
      }

      var im = messages.length;
      messages[im] = {"user": username,
        "fullname": "You",
        "date": "pending",
        "message": msg,
        "isSystem": isSystemMessage};
      showMessages();
      document.getElementById("msg").value = '';

      $.ajax({
        url: jzChatSend,
        data: {"user": username,
          "targetUser": targetUser,
          "room": room,
          "message": msg,
          "token": token,
          "timestamp": new Date().getTime(),
          "isSystem": isSystemMessage
        },

        success:function(response){
          //console.log("success");
          refreshChat();
        },

        error:function (xhr, status, error){

        }

      });
    }
    if ( event.which == 13 && keydown == 18 ) {
      keydown = -1;
    }
    if ( event.which == 13 && msg.length == 1) {
      document.getElementById("msg").value = '';
    }


  });

  $(".chat-status-chat").on("click", function() {
    var $chatStatusPanel = $(".chat-status-panel");
    if ($chatStatusPanel.css("display")==="none")
      $chatStatusPanel.css("display", "inline-block");
    else
      $chatStatusPanel.css("display", "none");
  });

  $("div.chat-menu").click(function(){
    var status = $(this).attr("status");
    if (status !== undefined) {
      //console.log("setStatus :: "+status);

      $.ajax({
        url: jzSetStatus,
        data: { "user": username,
          "token": token,
          "status": status,
          "timestamp": new Date().getTime()
        },

        success: function(response){
          //console.log("SUCCESS:setStatus::"+response);
          changeStatusChat(response);
          $(".chat-status-panel").css('display', 'none');
        },
        error: function(response){
          changeStatusChat("offline");
        }

      });
    }

  });

  $(".msg-emoticons").on("click", function() {
    var $msgEmoticonsPanel = $(".msg-emoticons-panel");
    if ($msgEmoticonsPanel.css("display")==="none")
      $msgEmoticonsPanel.css("display", "inline-block");
    else
      $msgEmoticonsPanel.css("display", "none");
  });

  $(".emoticon-btn").on("click", function() {
    var sml = $(this).attr("data");
    $(".msg-emoticons-panel").css("display", "none");
    $msg = $('#msg');
    var val = $msg.val();
    if (val.charAt(val.length-1)!==' ') val +=" ";
    val += sml + " ";
    $msg.val(val);
    $msg.focusEnd();

  });


  $(".btn-mobile").on("click", function() {
    var $menuMobile = $(".menu-mobile");
    if ($menuMobile.css("display")==="none")
      $menuMobile.css("display", "block");
    else
      $menuMobile.css("display", "none");
  });

  $(".btn-rooms").on("click", function() {
    $(".left-chat").css("display", "block");
    $(".right-chat").css("display", "none");
  });

  $(".room-mobile").on("click", function() {
    $(".left-chat").css("display", "block");
    $(".right-chat").css("display", "none");
  });

  $(".msg-help").on("click", function() {
    showHelpPanel();
  });

  $(".chat-help-panel").on("click", function() {
    hidePanel(".chat-help-panel");
  });

  $(".filter").on("click", function() {
    var child = $("span:first",this);
    if (child.hasClass("filter-on")) {
      child.removeClass("filter-on").addClass("filter-off");
      if (!isAdmin) {
        if ($(this).hasClass("filter-user")) {
          $(".filter-space span:first-child").removeClass("filter-off").addClass("filter-on");
        } else {
          $(".filter-user span:first-child").removeClass("filter-off").addClass("filter-on");
        }
      }
    } else {
      child.removeClass("filter-off").addClass("filter-on");
    }
    refreshWhoIsOnline();
  });

  $('#chat-search').keyup(function(event) {
    var filter = $(this).val();
    if (filter == ":aboutme" || filter == ":about me") {
      showAboutPanel();
    }
    if (filter.indexOf("@")!==0) {
      highlight = filter;
      showMessages();
    } else {
      userFilter = filter.substr(1, filter.length-1);
      filterInt = clearTimeout(filterInt);
      filterInt = setTimeout(refreshWhoIsOnline, 500);
    }
  });

  chatOnlineInt = clearInterval(chatOnlineInt);
  chatOnlineInt = setInterval(refreshWhoIsOnline, chatIntervalUsers);
  refreshWhoIsOnline();

  if (username!==ANONIM_USER) setTimeout(showSyncPanel, 1000);

  function strip(html)
  {
    var tmp = document.createElement("DIV");
    tmp.innerHTML = html;
    return tmp.textContent||tmp.innerText;
  }



  function initChatProfile() {

    if (username===ANONIM_USER) {
      var anonimFullname = jzGetParam("anonimFullname");
      var anonimUsername = jzGetParam("anonimUsername");
      var anonimEmail = jzGetParam("anonimEmail");

      if (anonimUsername===undefined || anonimUsername===null) {
        showDemoPanel();
      } else {
        createDemoUser(anonimFullname, anonimEmail);
      }
    } else {
      $.getJSON(jzInitChatProfile, function(data){
        //console.log("Chat Profile Update : "+data.msg);
        //console.log("Chat Token          : "+data.token);
        //console.log("Chat Fullname       : "+data.fullname);
        //console.log("Chat isAdmin        : "+data.isAdmin);
        token = data.token;
        var $chatApplication = $("#chat-application");
        $chatApplication.attr("data-token", token);
        var $labelUser = $(".label-user");
        $labelUser.text(data.fullname);
        fullname = data.fullname;
        if (data.isAdmin == "true") {
          $(".filter-public").css("display", "inline-block");
          $(".filter-empty").css("display", "none");
        }
        isAdmin = (data.isAdmin=="true");
        //initCall(username, fullname);


        refreshWhoIsOnline();
        notifStatusInt = window.clearInterval(notifStatusInt);
        notifStatusInt = setInterval(refreshStatusChat, chatIntervalStatus);
        refreshStatusChat();

      })
        .error(function (response){
          //retry in 3 sec
          setTimeout(initChatProfile, 3000);
        });
    }

  }
  initChatProfile();

  function createWeemoCall() {
    if (weemoKey!=="") {

      if (targetUser.indexOf("space-")===-1) {
        uidToCall = "weemo"+targetUser;
        displaynameToCall=fullname;
        callType="internal";
      } else {
        uidToCall = weemo.getUid();
        displaynameToCall = weemo.getDisplayname();
        callType = "host";
      }
      callOwner = true;
      callActive = false;
      weemo.createCall(uidToCall, callType, displaynameToCall);


      weemo.onCallHandler = function(type, status)
      {
        if(callOwner && type==="call" && ( status==="active" || status==="terminated" ))
        {
          console.log("Call Handler : " + type + ": " + status);
          ts = Math.round(new Date().getTime() / 1000);

          if (status === "terminated") callOwner = false;

          if (callType==="internal" || status==="terminated") {
            messageWeemo = "Call "+status+"&"+ts;
          } else if (callType==="host") {
            messageWeemo = "Call "+status+"&"+ts+"&"+uidToCall+"&"+displaynameToCall;
          }

          if (status==="active" && callActive) return; //Call already active, no need to push a new message
          if (status==="terminated" && !callActive) return; //Terminate a non started call, no message needed


          if (status==="active") callActive = true;
          else if (status==="terminated") callActive = false;

          if (callType!=="attendee") {
            $.ajax({
              url: jzChatSend,
              data: {"user": username,
                "targetUser": targetUser,
                "room": room,
                "message": messageWeemo,
                "token": token,
                "timestamp": new Date().getTime(),
                "isSystem": "true"
              },

              success:function(response){
                //console.log("success");
                refreshChat();
              },

              error:function (xhr, status, error){

              }

            });
          }
        }
      }
    }
  }
  function joinWeemoCall() {
    if (weemoKey!=="") {
      callType = "attendee";
      callOwner = false;
      weemo.createCall(uidToCall, callType, displaynameToCall);

    }
  }

  $(".btn-weemo").on("click", function() {
    createWeemoCall();
  });

  $(".btn-weemo-conf").on("click", function() {
    joinWeemoCall();
  });


  function maintainSession() {
    $.ajax({
      url: jzMaintainSession,
      success: function(response){
        //console.log("Chat Session Maintained : "+response);
      },
      error: function(response){
        chatSessionInt = clearInterval(chatSessionInt);
      }
    });
  }

  if (window.fluid!==undefined) {
    chatSessionInt = clearInterval(chatSessionInt);
    chatSessionInt = setInterval(maintainSession, chatIntervalSession);
  }


  function hidePanel(panel) {
    $(panel).css("display", "none");
    $(panel).html("");
  }

  function hidePanels() {
    hidePanel(".chat-sync-panel");
    hidePanel(".chat-error-panel");
    hidePanel(".chat-login-panel");
    hidePanel(".chat-about-panel");
    hidePanel(".chat-demo-panel");
  }

  function showSyncPanel() {
    if (!isLoaded) {
      hidePanels();
      var $chatSyncPanel = $(".chat-sync-panel");
      $chatSyncPanel.html("<img src=\"/chat/img/sync.gif\" width=\"64px\" class=\"chatSync\" />");
      $chatSyncPanel.css("display", "block");
    }
  }

  function showErrorPanel() {
    hidePanels();
    //console.log("show-error-panel");
    var $chatErrorPanel = $(".chat-error-panel");
    $chatErrorPanel.html(labelPanelError1+"<br/><br/>"+labelPanelError2);
    $chatErrorPanel.css("display", "block");
  }

  function showLoginPanel() {
    hidePanels();
    //console.log("show-login-panel");
    var $chatLoginPanel = $(".chat-login-panel");
    $chatLoginPanel.html(labelPanelLogin1+"<br><br><a href=\"#\" onclick=\"javascript:reloadWindow();\">"+labelPanelLogin2+"</a>");
    $chatLoginPanel.css("display", "block");
  }

  function showDemoPanel() {
    hidePanels();
    //console.log("show-demo-panel");
    var $chatDemoPanel = $(".chat-demo-panel");
    var intro = labelPanelDemo;
    if (isPublic) intro = labelPanelPublic;
    $chatDemoPanel.html(intro+"<br><br><div class='welcome-panel'>" +
      "<br><br>"+labelDisplayName+"&nbsp;&nbsp;<input type='text' id='anonim-name'>" +
      "<br><br>"+labelEmail+"&nbsp;&nbsp;<input type='text' id='anonim-email'></div>" +
      "<br><a href='#' id='anonim-save'>"+labelSaveProfile+"</a>");
    $chatDemoPanel.css("display", "block");

    $("#anonim-save").on("click", function() {
      var fullname = $("#anonim-name").val();
      var email = $("#anonim-email").val();
      createDemoUser(fullname, email);
    });
  }

  function createDemoUser(fullname, email) {
    setTimeout(showSyncPanel, 1000);
    $.getJSON(jzCreateDemoUser,
      {
        "fullname": fullname,
        "email": email,
        "isPublic": isPublic
      },
      function(data) {
        //console.log("username : "+data.username);
        //console.log("token    : "+data.token);

        jzStoreParam("anonimUsername", data.username, 600000);
        jzStoreParam("anonimFullname", fullname, 600000);
        jzStoreParam("anonimEmail", email, 600000);

        username = data.username;
        $chatApplication.attr("data-username", username);
        token = data.token;
        $chatApplication.attr("data-token", token);

        $(".label-user").html(fullname);
        $(".avatar-image:first").attr("src", gravatar(email));
        hidePanels();

        refreshWhoIsOnline();
        notifStatusInt = window.clearInterval(notifStatusInt);
        notifStatusInt = setInterval(refreshStatusChat, chatIntervalStatus);
        refreshStatusChat();

        if (isPublic) {
          targetUser = SUPPORT_USER;
          targetFullname = labelSupportFullname;
        }

        loadRoom();

      });

  }

  function showAboutPanel() {
    var about = "eXo Community Chat<br>";
    about += "Version 0.6-SNAPSHOT<br><br>";
    about += "Designed and Developed by <a href=\"mailto:bpaillereau@gmail.com\">Benjamin Paillereau</a><br>";
    about += "Sources available on <a href=\"https://github.com/exo-addons/chat-application\" target=\"_new\">https://github.com/exo-addons/chat-application</a>";
    about += "<br><br><a href=\"#\" id=\"about-close-btn\" >Close</a>";
    hidePanels();
    var $chatAboutPanel = $(".chat-about-panel");
    $chatAboutPanel.html(about);
    $chatAboutPanel.css("display", "block");

    $("#about-close-btn").on("click", function() {
      hidePanel('.chat-about-panel');
      $('#chat-search').attr("value", "");
    });
  }


  function showHelpPanel() {
    hidePanels();
    $(".chat-help-panel").css("display", "block");
  }

  var totalNotif = 0;
  var oldNotif = 0;

  function refreshWhoIsOnline() {
    var withSpaces = $(".filter-space span:first-child").hasClass("filter-on");
    var withUsers = $(".filter-user span:first-child").hasClass("filter-on");
    var withPublic = $(".filter-public span:first-child").hasClass("filter-on");

    if (username.indexOf(ANONIM_USER)>-1) {
      withUsers = true;
      withSpaces = true;
      withPublic = false;
    }

    if (username !== ANONIM_USER && token !== "---") {
      $.getJSON(jzChatWhoIsOnline,
        { "user": username,
          "token": token,
          "filter": userFilter,
          "withSpaces": withSpaces,
          "withUsers": withUsers,
          "withPublic": withPublic,
          "isAdmin": isAdmin,
          "timestamp": new Date().getTime()
        }, function(response){
          var tmpMD5 = response.md5;
          if (tmpMD5 !== whoIsOnlineMD5) {
            var rooms = TAFFY(response.rooms);
            whoIsOnlineMD5 = tmpMD5;
            isLoaded = true;
            hidePanel(".chat-error-panel");
            hidePanel(".chat-sync-panel");
            showRooms(rooms)
            jQueryForUsersTemplate();
            if (window.fluid!==undefined) {
              totalNotif = 0;
              $('span.room-total').each(function(index) {
                totalNotif = parseInt(totalNotif,10) + parseInt($(this).attr("data"),10);
              });
              if (totalNotif>0)
                window.fluid.dockBadge = totalNotif;
              else
                window.fluid.dockBadge = "";
              if (totalNotif>oldNotif && profileStatus !== "donotdisturb" && profileStatus !== "offline") {
                window.fluid.showGrowlNotification({
                  title: labelTitle,
                  description: labelNewMessages,
                  priority: 1,
                  sticky: false,
                  identifier: "messages"
                });
              }
              oldNotif = totalNotif;
            } else if (window.webkitNotifications!==undefined) {
              totalNotif = 0;
              $('span.room-total').each(function(index) {
                totalNotif = parseInt(totalNotif,10) + parseInt($(this).attr("data"),10);
              });
              if (totalNotif>oldNotif && profileStatus !== "donotdisturb" && profileStatus !== "offline") {

                var havePermission = window.webkitNotifications.checkPermission();
                if (havePermission == 0) {
                  // 0 is PERMISSION_ALLOWED
                  var notification = window.webkitNotifications.createNotification(
                    '/chat/img/chat.png',
                    'Chat notification!',
                    'You have new message'
                  );

                  notification.onclick = function () {
                    window.open("http://stackoverflow.com/a/13328397/1269037");
                    notification.close();
                  }
                  notification.show();
                } else {
                  window.webkitNotifications.requestPermission();
                }
              }

              oldNotif = totalNotif;
            }

          }
        }).error(function (response){
          //console.log("chat-users :: "+response);
          setTimeout(errorOnRefresh, 1000);
        });
    }

  }

  function showRooms(rooms) {

    var roomPrevUser = "";
    var out = '<table class="table">';
    var fav=null;
    rooms().order("isFavorite desc, unreadTotal desc, escapedFullname logical").each(function (room) {
//      console.log("info = "+room.user+" :"+fav+":"+ room.isFavorite);
      if (room.user!==roomPrevUser) {
        if (fav==null && room.isFavorite=="true") fav="";
        else if (fav=="" && room.isFavorite=="false") fav="border-top:1px solid #CCC;";
        else if (fav=="border-top:1px solid #CCC;") fav=" ";

        out += '<tr id="users-online-'+room.user.replace(".", "-")+'" class="users-online" style="'+fav+'">';
        out += '<td class="td-status">';
        out += '<span class="';
        if (room.isFavorite == "true") {
          out += 'user-favorite';
        } else {
          out += 'user-status';
        }
        out +='" user-data="'+room.user+'"></span><span class="user-'+room.status+'"></span>';
        out += '</td>';
        out +=  '<td>';
        if (room.isActive=="true") {
          out += '<span user-data="'+room.user+'" room-data="'+room.room+'" class="room-link" data-fullname="'+room.escapedFullname+'">'+room.escapedFullname+'</span>';
        } else {
          out += '<span class="room-inactive">'+room.user+'</span>';
        }
        out += '</td>';
        out += '<td>';
        if (Math.round(room.unreadTotal)>0) {
          out += '<span class="room-total" style="float:right;" data="'+room.unreadTotal+'">'+room.unreadTotal+'</span>';
        }
        out += '</td>';
        out += '</tr>';
        roomPrevUser = room.user;
      }
    });
    out += '</table>';


    $("#chat-users").html(out);

  }

  function jQueryForUsersTemplate() {
    var value = jzGetParam("lastUser"+username);
    if (value && firstLoad) {
      //console.log("firstLoad with user : *"+value+"*");
      targetUser = value;
      targetFullname = jzGetParam("lastFullName"+username);
      if (username!==ANONIM_USER) {
        loadRoom();
      }
      firstLoad = false;
    }

    if (isDesktopView()) {
      var $targetUser = $("#users-online-"+targetUser.replace(".", "-"));
      $targetUser.addClass("info");
      $(".room-total").removeClass("room-total-white");
      $targetUser.find(".room-total").addClass("room-total-white");
    }


    $('.users-online').on("click", function() {
      targetUser = $(".room-link:first",this).attr("user-data");
      targetFullname = $(".room-link:first",this).attr("data-fullname");
      loadRoom();
      if (isMobileView()) {
        $(".right-chat").css("display", "block");
        $(".left-chat").css("display", "none");
        $(".room-name").html(targetFullname);
      }
    });


    $('.room-link').on("click", function() {
      targetUser = $(this).attr("user-data");
      targetFullname = $(this).attr("data-fullname");
      loadRoom();
      if (isMobileView()) {
        $(".right-chat").css("display", "block");
        $(".left-chat").css("display", "none");
        $(".room-name").html(targetFullname);
      }
    });

    $('.user-status').on("click", function() {
      var targetFav = $(this).attr("user-data");
      toggleFavorite(targetFav);
    });
    $('.user-favorite').on("click", function() {
      var targetFav = $(this).attr("user-data");
      toggleFavorite(targetFav);
    });

  }

  function errorOnRefresh() {
    isLoaded = true;
    hidePanel(".chat-sync-panel");
    $("#chat-users").html("<span>&nbsp;</span>");
    hidePanel(".chat-login-panel");
    changeStatusChat("offline");
    showErrorPanel();
  }

  function setStatus(status) {
    $.ajax({
      url: jzSetStatus,
      data: { "user": username,
        "token": token,
        "status": status,
        "timestamp": new Date().getTime()
      },

      success: function(response){
        //console.log("SUCCESS:setStatus::"+response);
        changeStatusChat(status);
      },
      error: function(response){
      }

    });

  }

  function setStatusAvailable() {
    setStatus("available");
  }

  function setStatusAway() {
    setStatus("away");
  }

  function setStatusDoNotDisturb() {
    setStatus("donotdisturb");
  }

  function setStatusInvisible() {
    setStatus("invisible");
  }

  function initFluidApp() {
    if (window.fluid!==undefined) {
      window.fluid.addDockMenuItem(labelAvailable, setStatusAvailable);
      window.fluid.addDockMenuItem(labelAway, setStatusAway);
      window.fluid.addDockMenuItem(labelDoNotDisturb, setStatusDoNotDisturb);
      window.fluid.addDockMenuItem(labelInvisible, setStatusInvisible);
    }


  }
  initFluidApp();


  function refreshStatusChat() {
    $.ajax({
      url: jzGetStatus,
      data: {
        "user": username,
        "token": token,
        "timestamp": new Date().getTime()
      },
      success: function(response){
        changeStatusChat(response);
      },
      error: function(response){
        changeStatusChat("offline");
      }
    });
  }


  function changeStatusChat(status) {
    profileStatus = status;
    var $statusLabel = $(".chat-status-label");
    $statusLabel.html(labelCurrentStatus+" "+getStatusLabel(status));
    var $chatStatus = $("span.chat-status");
    $chatStatus.removeClass("chat-status-available-black");
    $chatStatus.removeClass("chat-status-donotdisturb-black");
    $chatStatus.removeClass("chat-status-invisible-black");
    $chatStatus.removeClass("chat-status-away-black");
    $chatStatus.removeClass("chat-status-offline-black");
    $chatStatus.addClass("chat-status-"+status+"-black");
    var $chatStatusChat = $(".chat-status-chat");
    $chatStatusChat.removeClass("chat-status-available");
    $chatStatusChat.removeClass("chat-status-donotdisturb");
    $chatStatusChat.removeClass("chat-status-invisible");
    $chatStatusChat.removeClass("chat-status-away");
    $chatStatusChat.removeClass("chat-status-offline");
    $chatStatusChat.addClass("chat-status-"+status);

  }

  function getStatusLabel(status) {
    switch (status) {
      case "available":
        return labelAvailable;
      case "donotdisturb":
        return labelDoNotDisturb;
      case "away":
        return labelAway;
      case "invisible":
        return labelInvisible;
      case "offline":
        return "Offline";

    }

  }


  function showMessages(msgs) {
    var im, message, out="", prevUser="";
    if (msgs!==undefined) {
      messages = msgs;
    }

    if (messages.length===0) {
      out = "<div class='msgln' style='padding:22px 20px;'>";
      if (isPublic)
        out += "<b><center>"+labelPublicWelcome+"</center></b>";
      else
        out += "<b><center>"+labelNoMessages+"</center></b>";
      out += "</div>";
    } else {
      for (im=0 ; im<messages.length ; im++) {
        message = messages[im];

        if (message.isSystem!=="true")
        {
          if (prevUser != message.user)
          {
            if (prevUser !== "")
              out += "</span></div>";
            if (message.user != username) {
              out += "<div class='msgln-odd'>";
              out += "<span style='position:relative; padding-right:16px;padding-left:4px;top:8px'>";
              if (isPublic)
                out += "<img src='/chat/img/support-avatar.png' width='30px' style='width:30px;'>";
              else
                out += "<img onerror=\"this.src=gravatar('"+message.email+"');\" src='/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:"+message.user+"/soc:profile/soc:avatar' width='30px' style='width:30px;'>";
              out += "</span>";
              out += "<span>";
              if (isPublic)
                out += "<span class='invisible-text'>- </span><a href='#'>"+labelSupportFullname+"</a><span class='invisible-text'> : </span><br/>";
              else
                out += "<span class='invisible-text'>- </span><a href='/portal/intranet/profile/"+message.user+"' class='user-link' target='_new'>"+message.fullname+"</a><span class='invisible-text'> : </span><br/>";
            } else {
              out += "<div class='msgln'>";
              out += "<span style='position:relative; padding-right:16px;padding-left:4px;top:8px'>";
              out += "<img src='/chat/img/empty.png' width='30px' style='width:30px;'>";
              out += "</span>";
              out += "<span>";
              //out += "<span style='float:left; '>&nbsp;</span>";
              out += "<span class='invisible-text'>- </span><a href='/portal/intranet/profile/"+message.user+"' class='user-link' target='_new'>"+message.fullname+"</a><span class='invisible-text'> : </span><br/>";
            }
          }
          else
          {
            out += "<hr style='margin:0px;'>";
          }
          out += "<div style='margin-left:50px;'><span style='float:left'>"+messageBeautifier(message.message)+"</span>" +
            "<span class='invisible-text'> [</span>"+
            "<span style='float:right;color:#CCC;font-size:10px'>"+message.date+"</span>" +
            "<span class='invisible-text'>]</span></div>"+
            "<div style='clear:both;'></div>";
          prevUser = message.user;
        }
        else
        {
          if (prevUser !== "")
            out += "</span></div>";
          if (prevUser !== "__system")
            out += "<hr style='margin: 0'>";
          out += "<div class='msgln-odd'>";
          out += "<span style='position:relative; padding-right:16px;padding-left:4px;top:8px'>";
          var msgArray = message.message.split("&");

          if (msgArray[0]==="Call active") {
            out += "<img src='/chat/img/2x/call-on.png' width='30px' style='width:30px;'>";
            if (msgArray.length>1) {
              jzStoreParam("weemoCallHandler", msgArray[1], 600000)
            }
            $(".btn-weemo").addClass('disabled');
          } else if (msgArray[0]==="Call terminated") {
            out += "<img src='/chat/img/2x/call-off.png' width='30px' style='width:30px;'>";
            $(".btn-weemo").removeClass('disabled');
          } else {
            out += "<img src='/chat/img/empty.png' width='30px' style='width:30px;'>";
          }
          out += "</span>";
          out += "<span>";
          if (msgArray.length===1) out+="<center>";
          out += "<b style=\"line-height: 12px;vertical-align: bottom;\">"+msgArray[0]+"</b>";
          if (msgArray.length===1) out+="</center>";

          if (msgArray[0]==="Call terminated" && msgArray.length>1) {
            var tsold = Math.round(jzGetParam("weemoCallHandler"));
            var time = Math.round(msgArray[1])-tsold;
            var hours = Math.floor(time / 3600);
            time -= hours * 3600;
            var minutes = Math.floor(time / 60);
            time -= minutes * 60;
            var seconds = parseInt(time % 60, 10);
            var stime = "<span class=\"msg-time\">";
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
          }

          if (msgArray.length===4) {
            uidToCall = msgArray[2];
            displaynameToCall = msgArray[3];
            $(".btn-weemo").css("display", "none");
            $(".btn-weemo-conf").css("display", "block");
            if (msgArray[2]!=="weemo"+username)
              $(".btn-weemo-conf").removeClass("disabled");
            else
              $(".btn-weemo-conf").addClass("disabled");
          } else {
            $(".btn-weemo").css("display", "block");
            $(".btn-weemo-conf").css("display", "none");
          }


          message.message = "";
          out += "<div style='margin-left:50px;'><span style='float:left'>"+messageBeautifier(message.message)+"</span>" +
            "<span class='invisible-text'> [</span>"+
            "<span style='float:right;color:#CCC;font-size:10px'>"+message.date+"</span>" +
            "<span class='invisible-text'>]</span></div>"+
            "<div style='clear:both;'></div>";
          out += "</span></div>";
          out += "<hr style='margin: 0'>";
          out += "<div><span>";
          prevUser = "__system";

        }
      }
    }
    var $chats = $("#chats");
    $chats.html('<span>'+out+'</span>');
    sh_highlightDocument();
    $chats.animate({ scrollTop: 20000 }, 'fast');

  }

  function refreshChat() {
    if (username !== ANONIM_USER) {

      $.getJSON(chatEventURL, function(data) {
        var lastTS = jzGetParam("lastTS"+username);
        //console.log("chatEvent :: lastTS="+lastTS+" :: serverTS="+data.timestamp);
        var im, message, out="", prevUser="";
        if (data.messages.length===0) {
          showMessages(data.messages);
        } else {
          var ts = data.timestamp;
          if (ts != lastTS) {
            jzStoreParam("lastTS"+username, ts, 600);
            //console.log("new data to show");
            showMessages(data.messages);
          }
        }
        if (isDesktopView()) $(".right-chat").css("display", "block");
        hidePanel(".chat-login-panel");
        hidePanel(".chat-error-panel");
      })
        .error(function() {
          if (isDesktopView()) $(".right-chat").css("display", "none");
          if ( $(".chat-error-panel").css("display") == "none") {
            showLoginPanel();
          } else {
            hidePanel(".chat-login-panel");
          }
        });

    }

  }


  function toggleFavorite(targetFav) {
    console.log("FAVORITE::"+targetFav);
    $.ajax({
      url: jzChatToggleFavorite,
      data: {"targetUser": targetFav,
        "user": username,
        "token": token,
        "timestamp": new Date().getTime()
      },
      success: function(response){
        refreshWhoIsOnline();
      },
      error: function(xhr, status, error){
      }
    });
  }

  function loadRoom() {
    //console.log("TARGET::"+targetUser+" ; ISADMIN::"+isAdmin);
    if (targetUser!==undefined) {
      $(".users-online").removeClass("info");
      if (isDesktopView()) {
        var $targetUser = $("#users-online-"+targetUser.replace(".", "-"));
        $targetUser.addClass("info");
        $(".room-total").removeClass("room-total-white");
        $targetUser.find(".room-total").addClass("room-total-white");
      }

      $("#room-detail").css("display", "block");
      $(".target-user-fullname").text(targetFullname);
      if (targetUser.indexOf("space-")===-1) {
        $(".target-avatar-link").attr("href", "/portal/intranet/profile/"+targetUser);
        $(".target-avatar-image").attr("src", "/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:"+targetUser+"/soc:profile/soc:avatar");
      }
      else
      {
        var spaceName = targetFullname.toLowerCase().replace(" ", "_");
        $(".target-avatar-link").attr("href", "/portal/g/:spaces:"+spaceName+"/"+spaceName);
        $(".target-avatar-image").attr("src", "/rest/jcr/repository/social/production/soc:providers/soc:space/soc:"+spaceName+"/soc:profile/soc:avatar");
      }


      $.ajax({
        url: jzChatGetRoom,
        data: {"targetUser": targetUser,
          "user": username,
          "token": token,
          "isAdmin": isAdmin,
          "timestamp": new Date().getTime()
        },

        success: function(response){
          //console.log("SUCCESS::getRoom::"+response);
          room = response;
          var $msg = $('#msg');
          $msg.removeAttr("disabled");
          if (isDesktopView()) $msg.focus();
          chatEventURL = jzChatSend+'?room='+room+'&user='+username+'&token='+token+'&event=0';

          jzStoreParam("lastUser"+username, targetUser, 60000);
          jzStoreParam("lastFullName"+username, targetFullname, 60000);
          jzStoreParam("lastTS"+username, "0");
          chatEventInt = window.clearInterval(chatEventInt);
          chatEventInt = setInterval(refreshChat, chatIntervalChat);
          refreshChat();

        },

        error: function(xhr, status, error){
          //console.log("ERROR::"+xhr.responseText);
        }

      });
    }

  }

  function reloadWindow() {
    var sURL = unescape(window.location.href);
    //console.log(sURL);
    window.location.href = sURL;
    //window.location.reload( false );
  }

  // We change the current history by removing get parameters so they won't be visible in the popup
  // Having a location bar with ?noadminbar=true is not User Friendly ;-)
  function removeParametersFromLocation() {
    var sURL = window.location.href;
    if (sURL.indexOf("?")>-1) {
      sURL = sURL.substring(0,sURL.indexOf("?"));
      window.history.replaceState("#", "Chat", sURL);
    }
  }

  //removeParametersFromLocation();

  function messageBeautifier(message) {
    var msg = "";
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
            } else if (w.indexOf("http://www.youtube.com/watch?v=")===0 && !IsIE8Browser() ) {
              var id = w.substr(31);
              w = "<iframe width='100%' src='http://www.youtube.com/embed/"+id+"' frameborder='0' allowfullscreen></iframe>";
              w += "<span class='invisible-text'>"+link+"</span>";
            } else {
              w = "<a href='"+w+"' target='_new'>"+w+"</a>";
            }
          } else if (w == ":-)" || w==":)") {
            w = "<span class='emoticon emoticon-smile'><span class='emoticon-text'>:)</span></span>";
          } else if (w == ":-D" || w==":D") {
            w = "<span class='emoticon emoticon-big-smile'><span class='emoticon-text'>:D</span></span>";
          } else if (w == ":-|" || w==":|") {
            w = "<span class='emoticon emoticon-no-voice'><span class='emoticon-text'>:|</span></span>";
          } else if (w == ":-(" || w==":(") {
            w = "<span class='emoticon emoticon-sad'><span class='emoticon-text'>:(</span></span>";
          } else if (w == ";-)" || w==";)") {
            w = "<span class='emoticon emoticon-eye-blink'><span class='emoticon-text'>;)</span></span>";
          } else if (w == ":-O" || w==":O") {
            w = "<span class='emoticon emoticon-surprise'><span class='emoticon-text'>:O</span></span>";
          } else if (highlight.length >1) {
            w = w.replace(eval("/"+highlight+"/g"), "<span style='background-color:#FF0;font-weight:bold;'>"+highlight+"</span>");
          }
          msg += w+" ";
        }
      }
      // console.log(il + "::" + lines.length);
      if (il < lines.length-1) {
        msg += "<br/>";
      }
    }

    // if (highlight.length >2) {
    //   msg = msg.replace(eval("/"+highlight+"/g"), "<span style='background-color:#FF0;font-weight:bold;'>"+highlight+"</span>");
    // }

    return msg;
  }

  function IsIE8Browser() {
    var rv = -1;
    var ua = navigator.userAgent;
    var re = new RegExp("Trident\/([0-9]{1,}[\.0-9]{0,})");
    if (re.exec(ua) != null) {
      rv = parseFloat(RegExp.$1);
    }
    return (rv == 4);
  }

  String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
  };



});

