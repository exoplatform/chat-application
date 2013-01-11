$(document).ready(function(){

  var highlight = "";
  var ANONIM_USER = "__anonim_";

  var $chatApplication = $("#chat-application");
  var username = $chatApplication.attr("data-username");
  var sessionId = $chatApplication.attr("data-session-id");
  var chatServerURL = $chatApplication.attr("data-chat-server-url");
  var chatIntervalChat = $chatApplication.attr("data-chat-interval-chat");
  var chatIntervalSession = $chatApplication.attr("data-chat-interval-session");
  var chatIntervalStatus = $chatApplication.attr("data-chat-interval-status");
  var chatIntervalUsers = $chatApplication.attr("data-chat-interval-users");
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
              "sessionId": sessionId
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
    var msg = $(this).attr("value");
    // console.log(event.which + " ;"+msg.length+";");
    if ( event.which == 13 && keydown !== 18 && msg.length>1) {
      //console.log("sendMsg=>"+username + " : " + room + " : "+msg);
      if(!msg)
      {
        return;
      }
      var im = messages.length;
      messages[im] = {"user": username,
                      "fullname": "You",
                      "date": "pending",
                      "message": msg};
      showMessages();
      document.getElementById("msg").value = '';

      $.ajax({
        url: jzChatSend,
        data: {"user": username,
               "targetUser": targetUser,
               "room": room,
               "message": msg,
               "sessionId": sessionId
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
      console.log("setStatus :: "+status);

      $.ajax({
        url: jzSetStatus,
        data: { "user": username,
                "sessionId": sessionId,
                "status": status
                },

        success: function(response){
          console.log("SUCCESS:setStatus::"+response);
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
      if ($(this).hasClass("filter-user")) {
        $(".filter-space span:first-child").removeClass("filter-off").addClass("filter-on");
      } else {
        $(".filter-user span:first-child").removeClass("filter-off").addClass("filter-on");
      }
    } else {
      child.removeClass("filter-off").addClass("filter-on");
    }
    refreshWhoIsOnline();
  });

  $('#chat-search').keyup(function(event) {
    var filter = $(this).attr("value");
    if (filter == ":aboutme" || filter == ":about me") {
      showAboutPanel();
    }
    if (filter.indexOf(":")===-1) {
      userFilter = filter;
      filterInt = clearTimeout(filterInt);
      filterInt = setTimeout(refreshWhoIsOnline, 500);
    } else {
      highlight = filter.substr(1, filter.length-1);
      showMessages();
    }
  });

  chatOnlineInt = clearInterval(chatOnlineInt);
  chatOnlineInt = setInterval(refreshWhoIsOnline, chatIntervalUsers);
  refreshWhoIsOnline();

  setTimeout(showSyncPanel, 1000);

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
      $.ajax({
        url: jzInitChatProfile,
        success: function(response){
          console.log("Chat Profile Update : "+response);

          refreshWhoIsOnline();
          notifStatusInt = window.clearInterval(notifStatusInt);
          notifStatusInt = setInterval(refreshStatusChat, chatIntervalStatus);
          refreshStatusChat();

        },
        error: function(response){
          //retry in 3 sec
          setTimeout(initChatProfile, 3000);
        }
      });
    }

  }
  initChatProfile();

  function maintainSession() {
    $.ajax({
      url: jzMaintainSession,
      success: function(response){
        console.log("Chat Session Maintained : "+response);
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
   console.log("show-error-panel");
   var $chatErrorPanel = $(".chat-error-panel");
   $chatErrorPanel.html("Service Not Available.<br/><br/>Please, come back later.");
   $chatErrorPanel.css("display", "block");
 }

 function showLoginPanel() {
   hidePanels();
   console.log("show-login-panel");
   var $chatLoginPanel = $(".chat-login-panel");
   $chatLoginPanel.html("You must be logged in to use the Chat.<br><br><a href=\"#\" onclick=\"javascript:reloadWindow();\">Click here to reload</a>");
   $chatLoginPanel.css("display", "block");
 }

 function showDemoPanel() {
   hidePanels();
   console.log("show-demo-panel");
   var $chatDemoPanel = $(".chat-demo-panel");
   $chatDemoPanel.html("Welcome in the Demo mode.<br><br><div class='welcome-panel'>" +
           "<br><br>Display Name&nbsp;&nbsp;<input type='text' id='anonim-name'>" +
           "<br><br>Email&nbsp;&nbsp;<input type='text' id='anonim-email'></div>" +
           "<br><a href='#' id='anonim-save'>Save Your Profile</a>");
   $chatDemoPanel.css("display", "block");

   $("#anonim-save").on("click", function() {
     var fullname = $("#anonim-name").val();
     var email = $("#anonim-email").val();
     createDemoUser(fullname, email);
   });
 }

 function createDemoUser(fullname, email) {
   $.ajax({
     url: jzCreateDemoUser,
     data: { "fullname": fullname,
             "email": email
     },
     success: function(response){

       jzStoreParam("anonimUsername", response, 600000);
       jzStoreParam("anonimFullname", fullname, 600000);
       jzStoreParam("anonimEmail", email, 600000);

       username = response;
       $(".label-user").html(fullname);
       hidePanels();

       refreshWhoIsOnline();
       notifStatusInt = window.clearInterval(notifStatusInt);
       notifStatusInt = setInterval(refreshStatusChat, chatIntervalStatus);
       refreshStatusChat();

     }
   });

 }

 function showAboutPanel() {
   var about = "eXo Community Chat<br>";
   about += "Version 0.6-SNAPSHOT<br><br>";
   about += "Designed and Developed by <a href=\"mailto:bpaillereau@gmail.com\">Benjamin Paillereau</a><br>";
   about += "Sources available on <a href=\"https://github.com/exo-addons/chat-application\" target=\"_new\">https://github.com/exo-addons/chat-application</a>";
   about += "<br><br><a href=\"#\" onclick=\"javascript:closeAbout();\">Close</a>";
   hidePanels();
   var $chatAboutPanel = $(".chat-about-panel");
   $chatAboutPanel.html(about);
   $chatAboutPanel.css("display", "block");
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

   if (username.indexOf(ANONIM_USER)>-1) {
     withUsers = true;
     withSpaces = true;
   }

   $.ajax({
     url: jzChatWhoIsOnline,
     data: { "user": username,
             "sessionId": sessionId,
             "filter": userFilter,
             "withSpaces": withSpaces,
             "withUsers": withUsers
             },
     dataType: 'html',
     success: function(response){
       isLoaded = true;
       hidePanel(".chat-error-panel");
       hidePanel(".chat-sync-panel");
       $("#chat-users").html(response);
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
               title: "eXo Chat",
               description: "You have new messages",
               priority: 1,
               sticky: false,
               identifier: "messages"
           });
         }
         oldNotif = totalNotif;
       }
     },
     error: function(jqXHR, textStatus, errorThrown){
       console.log("chat-users :: "+textStatus+" :: "+errorThrown);
       setTimeout(errorOnRefresh, 1000);
     }
   });
 }


 function jQueryForUsersTemplate() {
   var value = jzGetParam("lastUser");
   if (value && firstLoad) {
     //console.log("firstLoad with user : *"+value+"*");
     targetUser = value;
     loadRoom();
     firstLoad = false;
   }

   if (isDesktopView()) $("#users-online-"+targetUser).addClass("info");


   $('.users-online').on("click", function() {
     targetUser = $(".room-link:first",this).attr("user-data");
     fullname = $(".room-link:first",this).attr("data-fullname");
     loadRoom();
     if (isMobileView()) {
       $(".right-chat").css("display", "block");
       $(".left-chat").css("display", "none");
       $(".room-name").html(fullname);
     }
   });


   $('.room-link').on("click", function() {
     targetUser = $(this).attr("user-data");
     fullname = $(this).attr("data-fullname");
     loadRoom();
     if (isMobileView()) {
       $(".right-chat").css("display", "block");
       $(".left-chat").css("display", "none");
       $(".room-name").html(fullname);
     }
   });

   $('.user-status').on("click", function(e) {
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
             "sessionId": sessionId,
             "status": status
             },

     success: function(response){
       console.log("SUCCESS:setStatus::"+response);
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
     window.fluid.addDockMenuItem("Available", setStatusAvailable);
     window.fluid.addDockMenuItem("Away", setStatusAway);
     window.fluid.addDockMenuItem("Do not disturb", setStatusDoNotDisturb);
     window.fluid.addDockMenuItem("Invisible", setStatusInvisible);
   }


 }
 initFluidApp();


 function refreshStatusChat() {
   $.ajax({
     url: jzGetStatus,
     data: {
       "user": username,
       "sessionId": sessionId
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
   $statusLabel.html("Your current status is : "+getStatusLabel(status));
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
       return "Available";
     case "donotdisturb":
       return "Do not disturb";
     case "away":
       return "Away";
     case "invisible":
       return "Invisible";
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
     out += "<b><center>No messages yet.</center></b>";
     out += "</div>";
   } else {
     for (im=0 ; im<messages.length ; im++) {
       message = messages[im];

       if (prevUser != message.user)
       {
         if (prevUser !== "")
           out += "</span></div>";
         if (message.user != username) {
           out += "<div class='msgln-odd'>";
           out += "<span style='position:relative; padding-right:9px;top:8px'>";
           out += "<img onerror=\"this.src='/chat/img/Avatar.gif;'\" src='/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:"+message.user+"/soc:profile/soc:avatar' width='30px'>";
           out += "</span>";
           out += "<span>";
         } else {
           out += "<div class='msgln'>";
           out += "<span style='margin-left:40px;'>";
           //out += "<span style='float:left; '>&nbsp;</span>";
         }
         out += "<span class='invisible-text'>- </span><a href='/portal/intranet/profile/"+message.user+"' class='user-link' target='_new'>"+message.fullname+"</a><span class='invisible-text'> : </span><br/>";
       }
       else
       {
         out += "<hr style='margin:0px;'>";
       }
       out += "<div style='margin-left:40px;'><span style='float:left'>"+messageBeautifier(message.message)+"</span>" +
               "<span class='invisible-text'> [</span>"+
               "<span style='float:right;color:#CCC;font-size:10px'>"+message.date+"</span>" +
               "<span class='invisible-text'>]</span></div>"+
               "<div style='clear:both;'></div>";
       prevUser = message.user;
     }
   }
   var $chats = $("#chats");
   $chats.html('<span>'+out+'</span>');
   sh_highlightDocument();
   $chats.animate({ scrollTop: 20000 }, 'fast');

 }

 function refreshChat() {
     $.getJSON(chatEventURL, function(data) {
       var lastTS = jzGetParam("lastTS");
       //console.log("chatEvent :: lastTS="+lastTS+" :: serverTS="+data.timestamp);
       var im, message, out="", prevUser="";
       if (data.messages.length===0) {
         showMessages(data.messages);
       } else {
         var ts = data.timestamp;
         if (ts != lastTS) {
           jzStoreParam("lastTS", ts, 600);
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


 function toggleFavorite(targetFav) {
   console.log("FAVORITE::"+targetFav);
   $.ajax({
     url: jzChatToggleFavorite,
     data: {"targetUser": targetFav,
             "user": username,
             "sessionId": sessionId
             },
     success: function(response){
       refreshWhoIsOnline();
     },
     error: function(xhr, status, error){
     }
   });
 }

 function loadRoom() {
   console.log("TARGET::"+targetUser);
   $(".users-online").removeClass("info");
   if (isDesktopView()) $("#users-online-"+targetUser).addClass("info");

   $.ajax({
     url: jzChatGetRoom,
     data: {"targetUser": targetUser,
             "user": username,
             "sessionId": sessionId
             },

     success: function(response){
       console.log("SUCCESS::getRoom::"+response);
       room = response;
       var $msg = $('#msg');
       $msg.removeAttr("disabled");
       if (isDesktopView()) $msg.focus();
       chatEventURL = jzChatSend+'?room='+room+'&user='+username+'&sessionId='+sessionId+'&event=0';

       jzStoreParam("lastUser", targetUser, 60000);
       jzStoreParam("lastTS", "0");
       chatEventInt = window.clearInterval(chatEventInt);
       chatEventInt = setInterval(refreshChat, chatIntervalChat);
       refreshChat();

     },

     error: function(xhr, status, error){
       console.log("ERROR::"+xhr.responseText);
     }

   });

 }



 function closeAbout() {
   hidePanel('.chat-about-panel');
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
           w = "<a href='"+w+"' target='_new'>"+w+"</a>";
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



});

