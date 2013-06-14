var chatApplication = new ChatApplication();

$(document).ready(function(){

  /**
   * Init Chat
   */
  var $chatApplication = $("#chat-application");
  chatApplication.setJuzuLabelsElement($chatApplication);
  chatApplication.attachWeemoExtension(weemoExtension);

  chatApplication.username = $chatApplication.attr("data-username");
  chatApplication.token = $chatApplication.attr("data-token");
  var chatServerURL = $chatApplication.attr("data-chat-server-url");
  chatApplication.chatIntervalChat = $chatApplication.attr("data-chat-interval-chat");
  chatApplication.chatIntervalSession = $chatApplication.attr("data-chat-interval-session");
  chatApplication.chatIntervalStatus = $chatApplication.attr("data-chat-interval-status");
  chatApplication.chatIntervalUsers = $chatApplication.attr("data-chat-interval-users");

  chatApplication.publicModeEnabled = $chatApplication.attr("data-public-mode-enabled");
  var chatPublicMode = ($chatApplication.attr("data-public-mode")=="true");
  var chatView = $chatApplication.attr("data-view");
  var chatFullscreen = $chatApplication.attr("data-fullscreen");
  chatApplication.isPublic = (chatPublicMode == "true" && chatView == "public");
  chatApplication.jzInitChatProfile = $chatApplication.jzURL("ChatApplication.initChatProfile");
  chatApplication.jzCreateDemoUser = $chatApplication.jzURL("ChatApplication.createDemoUser");
  chatApplication.jzMaintainSession = $chatApplication.jzURL("ChatApplication.maintainSession");
  chatApplication.jzGetStatus = chatServerURL+"/getStatus";
  chatApplication.jzSetStatus = chatServerURL+"/setStatus";
  chatApplication.jzChatWhoIsOnline = chatServerURL+"/whoIsOnline";
  chatApplication.jzChatSend = chatServerURL+"/send";
  chatApplication.jzChatGetRoom = chatServerURL+"/getRoom";
  chatApplication.jzChatToggleFavorite = chatServerURL+"/toggleFavorite";
  chatApplication.jzChatUpdateUnreadMessages = chatServerURL+"/updateUnreadMessages";
  chatApplication.room = "<%=room%>";

  chatApplication.initChat();
  chatApplication.initChatProfile();

  /**
   * Init Global Variables
   *
   */
  //needed for #chat text area
  var keydown = -1;
  //needed for Fluid Integration
  var labelAvailable = $chatApplication.attr("data-label-available");
  var labelAway = $chatApplication.attr("data-label-away");
  var labelDoNotDisturb = $chatApplication.attr("data-label-donotdisturb");
  var labelInvisible = $chatApplication.attr("data-label-invisible");

  /**
   ##################                           ##################
   ##################                           ##################
   ##################   JQUERY UI EVENTS        ##################
   ##################                           ##################
   ##################                           ##################
   */

  $("#PlatformAdminToolbarContainer").addClass("no-user-selection");

  if (chatFullscreen == "true") {
    $("#PlatformAdminToolbarContainer").css("display", "none");
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
    chatApplication.hidePanels();
  });

  $('#msg').focus(function() {
//    console.log("focus on msg : "+chatApplication.targetUser+":"+chatApplication.room);
    chatApplication.updateUnreadMessages();
  });

  $('#msg').keydown(function(event) {
//    console.log("keydown : "+ event.which+" ; "+keydown);
    if ( event.which == 18 ) {
      keydown = 18;
    }
  });

  $('#msg').keyup(function(event) {
    var msg = $(this).val();
//    console.log("keyup : "+event.which + ";"+msg.length+";"+keydown);
    if ( event.which === 13 && keydown !== 18 && msg.length>1) {
      //console.log("sendMsg=>"+username + " : " + room + " : "+msg);
      if(!msg)
      {
        return;
      }
//      console.log("*"+msg+"*");
      chatApplication.sendMessage(msg);

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
    chatApplication.setStatus(status, function() {
      $(".chat-status-panel").css('display', 'none');
    });
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
    chatApplication.showHelpPanel();
  });

  $(".chat-help-panel").on("click", function() {
    chatApplication.hidePanel(".chat-help-panel");
  });

  $(".filter").on("click", function() {
    $(this).toggleClass("active");
    chatApplication.refreshWhoIsOnline();
  });


  $('#chat-search').keyup(function(event) {
    var filter = $(this).val();
    chatApplication.search(filter);
  });



  function strip(html)
  {
    var tmp = document.createElement("DIV");
    tmp.innerHTML = html;
    return tmp.textContent||tmp.innerText;
  }


  $(".btn-weemo").on("click", function() {
    if (!$(this).hasClass("disabled"))
      chatApplication.createWeemoCall();
  });

  $(".btn-weemo-conf").on("click", function() {
    if (!$(this).hasClass("disabled"))
      weemoExtension.joinWeemoCall();
  });


  if (window.fluid!==undefined) {
    chatApplication.activateMaintainSession();
  }


  function initFluidApp() {
    if (window.fluid!==undefined) {
      window.fluid.addDockMenuItem(labelAvailable, chatApplication.setStatusAvailable);
      window.fluid.addDockMenuItem(labelAway, chatApplication.setStatusAway);
      window.fluid.addDockMenuItem(labelDoNotDisturb, chatApplication.setStatusDoNotDisturb);
      window.fluid.addDockMenuItem(labelInvisible, chatApplication.setStatusInvisible);
    }
  }
  initFluidApp();


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


  String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
  };



});



/**
 ##################                           ##################
 ##################                           ##################
 ##################   CHAT APPLICATION        ##################
 ##################                           ##################
 ##################                           ##################
 */

/**
 * ChatApplication Class
 * @constructor
 */
function ChatApplication() {
  this.isLoaded = false;
  this.labels = new JuzuLabels();
  this.weemoExtension = "";
  this.isPublic = false;
  this.publicModeEnabled = false;

  this.room = "";
  this.username = "";
  this.fullname = "";
  this.targetUser = "";
  this.targetFullname = "";
  this.token = "";
  this.jzInitChatProfile = "";
  this.jzWhoIsOnline = "";
  this.jzChatGetRoom = "";
  this.jzChatToggleFavorite = "";
  this.jzCreateDemoUser = "";
  this.jzChatUpdateUnreadMessages = "";
  this.jzChatSend = "";
  this.jzGetStatus = "";
  this.jzSetStatus = "";
  this.jzMaintainSession = "";
  this.highlight = "";    //not set
  this.userFilter = "";    //not set
  this.chatIntervalChat = "";
  this.chatIntervalUsers = "";
  this.chatIntervalSession = "";
  this.chatIntervalStatus = "";
  this.chatEventURL  ="";          //NOT SET

  this.chatSessionInt = -1; //not set
  this.filterInt;
  this.messages = [];

  this.chatOnlineInt = -1;
  this.notifStatusInt = -1;
  this.ANONIM_USER = "__anonim_";
  this.SUPPORT_USER = "__support_";
  this.isAdmin = false;

  this.old = '';
  this.firstLoad = true;

  this.profileStatus = "offline";
  this.whoIsOnlineMD5 = 0;
  this.totalNotif = 0;
  this.oldNotif = 0;
}

/**
 * Set Labels
 * @param element : a dom element with data- labels
 */
ChatApplication.prototype.setJuzuLabelsElement = function(element) {
  this.labels.setElement(element);
};

/**
 * Attach Weemo Extension
 * @param weemoExtension WeemoExtension Object
 */
ChatApplication.prototype.attachWeemoExtension = function(weemoExtension) {
  this.weemoExtension = weemoExtension;
};


/**
 * Create demo user
 *
 * @param fullname
 * @param email
 */
ChatApplication.prototype.createDemoUser = function(fullname, email) {

  setTimeout($.proxy(this.showSyncPanel, this), 1000);
  $.ajax({
    url:this.jzCreateDemoUser,
    data: {
      "fullname": fullname,
      "email": email,
      "isPublic": this.isPublic
    },
    dataType: "json",
    context: this,
    success: function(data) {
      //console.log("username : "+data.username);
      //console.log("token    : "+data.token);

      jzStoreParam("anonimUsername", data.username, 600000);
      jzStoreParam("anonimFullname", fullname, 600000);
      jzStoreParam("anonimEmail", email, 600000);

      this.username = data.username;
      this.labels.set("username", this.username);
      this.token = data.token;
      this.labels.set("token", this.token);


      $(".label-user").html(fullname);
      $(".avatar-image:first").attr("src", gravatar(email));
      this.hidePanels();

      this.refreshWhoIsOnline(this);
      this.initStatusChat();

      if (this.isPublic) {
        this.targetUser = this.SUPPORT_USER;
        this.targetFullname = this.labels.get("label-support-fullname");
      }

      this.loadRoom();

    }
  });

};

/**
 * Update Unread Messages
 *
 * @param callback
 */
ChatApplication.prototype.updateUnreadMessages = function(callback) {
  $.ajax({
    url: this.jzChatUpdateUnreadMessages,
    data: {"room": this.room,
      "user": this.username,
      "token": this.token,
      "timestamp": new Date().getTime()
    },

    success:function(response){
      //console.log("success");
      if (typeof callback === "function") {
        callback();
      }
    },

    error:function (xhr, status, error){

    }

  });

};

/**
 * Init Status Chat Loop
 */
ChatApplication.prototype.initStatusChat = function() {
  this.notifStatusInt = window.clearInterval(this.notifStatusInt);
  this.notifStatusInt = setInterval($.proxy(this.refreshStatusChat, this), this.chatIntervalStatus);
  this.refreshStatusChat();
};

/**
 * Init Chat Interval
 */
ChatApplication.prototype.initChat = function() {
  this.chatOnlineInt = clearInterval(this.chatOnlineInt);
  this.chatOnlineInt = setInterval($.proxy(this.refreshWhoIsOnline, this), this.chatIntervalUsers);
  this.refreshWhoIsOnline(this);

  if (this.username!==this.ANONIM_USER) setTimeout($.proxy(this.showSyncPanel, this), 1000);
};

/**
 * Init Chat Profile
 */
ChatApplication.prototype.initChatProfile = function() {
  //var thiss = chatApplication; // TODO: IMPROVE THIS

  if (this.username===this.ANONIM_USER) {
    var anonimFullname = jzGetParam("anonimFullname");
    var anonimUsername = jzGetParam("anonimUsername");
    var anonimEmail = jzGetParam("anonimEmail");

    if (anonimUsername===undefined || anonimUsername===null) {
      this.showDemoPanel();
    } else {
      this.createDemoUser(anonimFullname, anonimEmail);
    }
  } else {
    $.ajax({
      url: this.jzInitChatProfile,
      dataType: "json",
      context: this,
      success: function(data){
        //console.log("Chat Profile Update : "+data.msg);
        //console.log("Chat Token          : "+data.token);
        //console.log("Chat Fullname       : "+data.fullname);
        //console.log("Chat isAdmin        : "+data.isAdmin);
        this.token = data.token;
        this.fullname = data.fullname;
        this.isAdmin = (data.isAdmin=="true");

        var $chatApplication = $("#chat-application");
        $chatApplication.attr("data-token", this.token);
        var $labelUser = $(".label-user");
        $labelUser.text(data.fullname);
        if (this.publicModeEnabled && data.isAdmin == "true") {
          $(".filter-public").css("display", "inline-block");
          $(".filter-empty").css("display", "none");
        }
        this.refreshWhoIsOnline(this);
        this.refreshStatusChat();

      },
      error: function (response){
        //retry in 3 sec
        setTimeout($.proxy(this.initChatProfile, this), 3000);
      }
    });
  }
};

/**
 * Maintain Session : Only on Fluid app context
 */
ChatApplication.prototype.maintainSession = function() {
  $.ajax({
    url: this.jzMaintainSession,
    context: this,
    success: function(response){
      //console.log("Chat Session Maintained : "+response);
    },
    error: function(response){
      this.chatSessionInt = clearInterval(this.chatSessionInt);
    }
  });
};

/**
 * Activate Maintain Session Loop
 */
ChatApplication.prototype.activateMaintainSession = function() {
  this.chatSessionInt = clearInterval(this.chatSessionInt);
  this.chatSessionInt = setInterval($.proxy(this.maintainSession, this), this.chatIntervalSession);
};

/**
 * Show Messages (json to html)
 * @param msgs : json messages data to show
 */
ChatApplication.prototype.showMessages = function(msgs) {
  var im, message, out="", prevUser="";
  if (msgs!==undefined) {
    this.messages = msgs;
  }

  if (this.messages.length===0) {
    out = "<div class='msgln' style='padding:22px 20px;'>";
    if (this.isPublic)
      out += "<b><center>"+this.labels.get("label-public-welcome")+"</center></b>";
    else
      out += "<b><center>"+this.labels.get("label-no-messages")+"</center></b>";
    out += "</div>";
  } else {
    for (im=0 ; im<this.messages.length ; im++) {
      message = this.messages[im];

      if (message.isSystem!=="true")
      {
        if (prevUser != message.user)
        {
          if (prevUser !== "")
            out += "</span></div>";
          if (message.user != this.username) {
            out += "<div class='msgln-odd'>";
            out += "<span style='position:relative; padding-right:16px;padding-left:4px;top:8px'>";
            if (this.isPublic)
              out += "<img src='/chat/img/support-avatar.png' width='30px' style='width:30px;'>";
            else
              out += "<img onerror=\"this.src=gravatar('"+message.email+"');\" src='/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:"+message.user+"/soc:profile/soc:avatar' width='30px' style='width:30px;'>";
            out += "</span>";
            out += "<span>";
            if (this.isPublic)
              out += "<span class='invisible-text'>- </span><a href='#'>"+this.labels.get("label-support-fullname")+"</a><span class='invisible-text'> : </span><br/>";
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
        out += "<div style='margin-left:50px;'><span style='float:left'>"+this.messageBeautifier(message.message)+"</span>" +
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
          this.weemoExtension.setUidToCall(msgArray[2]);
          this.weemoExtension.setDisplaynameToCall(msgArray[3]);
          $(".btn-weemo").css("display", "none");
          $(".btn-weemo-conf").css("display", "block");
          if (msgArray[2]!=="weemo"+this.username)
            $(".btn-weemo-conf").removeClass("disabled");
          else
            $(".btn-weemo-conf").addClass("disabled");
        } else {
          $(".btn-weemo").css("display", "block");
          $(".btn-weemo-conf").css("display", "none");
        }


        message.message = "";
        out += "<div style='margin-left:50px;'><span style='float:left'>"+this.messageBeautifier(message.message)+"</span>" +
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
};

/**
 * Refresh Chat : refresh messages and panels
 */
ChatApplication.prototype.refreshChat = function() {
  //var thiss = chatApplication;
  if (this.username !== this.ANONIM_USER) {

    $.ajax({
      url: this.chatEventURL,
      dataType: "json",
      context: this,
      success: function(data) {
        var lastTS = jzGetParam("lastTS"+this.username);
        //console.log("chatEvent :: lastTS="+lastTS+" :: serverTS="+data.timestamp);
        var im, message, out="", prevUser="";
        if (data.messages.length===0) {
          this.showMessages(data.messages);
        } else {
          var ts = data.timestamp;
          if (ts != lastTS) {
            jzStoreParam("lastTS"+this.username, ts, 600);
            //console.log("new data to show");
            this.showMessages(data.messages);
          }
        }
        if (this.isDesktopView()) $(".right-chat").css("display", "block");
        this.hidePanel(".chat-login-panel");
        this.hidePanel(".chat-error-panel");
      },
      error: function() {
        if (this.isDesktopView()) $(".right-chat").css("display", "none");
        if ( $(".chat-error-panel").css("display") == "none") {
          this.showLoginPanel();
        } else {
          this.hidePanel(".chat-login-panel");
        }
      }
    });

  }
};

/**
 * HTML Message Beautifier
 *
 * @param message
 * @returns {string} : the html markup
 */
ChatApplication.prototype.messageBeautifier = function(message) {
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
          } else if (w.indexOf("http://www.youtube.com/watch?v=")===0 && !this.IsIE8Browser() ) {
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

  return msg;
};

/**
 * Test if IE8
 * @returns {boolean}
 * @constructor
 */
ChatApplication.prototype.IsIE8Browser = function() {
  var rv = -1;
  var ua = navigator.userAgent;
  var re = new RegExp("Trident\/([0-9]{1,}[\.0-9]{0,})");
  if (re.exec(ua) != null) {
    rv = parseFloat(RegExp.$1);
  }
  return (rv == 4);
};

/**
 * Refresh Current Chat Status
 */
ChatApplication.prototype.refreshStatusChat = function() {
  //var thiss = chatApplication; // TODO : IMPROVE THIS
  $.ajax({
    url: this.jzGetStatus,
    data: {
      "user": this.username,
      "token": this.token,
      "timestamp": new Date().getTime()
    },
    context: this,
    success: function(response){
      this.changeStatusChat(response);
    },
    error: function(response){
      this.changeStatusChat("offline");
    }
  });

};

/**
 * Change Current Chat Status
 * @param status
 */
ChatApplication.prototype.changeStatusChat = function(status) {
  this.profileStatus = status;
  var $statusLabel = $(".chat-status-label");
  $statusLabel.html(this.labels.get("label-current-status")+" "+this.getStatusLabel(status));
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
};

/**
 * Get Status label
 * @param status : values can be : available, donotdisturb, away or invisible
 * @returns {*}
 */
ChatApplication.prototype.getStatusLabel = function(status) {
  switch (status) {
    case "available":
      return this.labels.get("label-available");
    case "donotdisturb":
      return this.labels.get("label-donotdisturb");
    case "away":
      return this.labels.get("label-away");
    case "invisible":
      return this.labels.get("label-invisible");
    case "offline":
      return "Offline";
  }
};

/**
 * Refresh Who Is Online : server call
 */
ChatApplication.prototype.refreshWhoIsOnline = function() {
  var withSpaces = !$(".filter-space").first().hasClass("active");
  var withUsers = !$(".filter-user").first().hasClass("active");
  var withPublic = !$(".filter-public").first().hasClass("active");
  var withOffline = !$(".filter-offline").first().hasClass("active");

  if (this.username.indexOf(this.ANONIM_USER)>-1) {
    withUsers = true;
    withSpaces = true;
    withPublic = false;
    withOffline = false;
  }

  if (this.username !== this.ANONIM_USER && this.token !== "---") {
    $.ajax({
      url: this.jzChatWhoIsOnline,
      dataType: "json",
      data: { "user": this.username,
        "token": this.token,
        "filter": this.userFilter,
        "withSpaces": withSpaces,
        "withUsers": withUsers,
        "withPublic": withPublic,
        "withOffline": withOffline,
        "isAdmin": this.isAdmin,
        "timestamp": new Date().getTime()},
      context: this,
      success: function(response){
        var tmpMD5 = response.md5;
        if (tmpMD5 !== this.whoIsOnlineMD5) {
          var rooms = TAFFY(response.rooms);
          this.whoIsOnlineMD5 = tmpMD5;
          this.isLoaded = true;
          this.hidePanel(".chat-error-panel");
          this.hidePanel(".chat-sync-panel");
          this.showRooms(rooms);

          this.jQueryForUsersTemplate();

          if (window.fluid!==undefined) {
            this.totalNotif = 0;
            var thisref = this;
            $('span.room-total').each(function(index) {
              thisref.totalNotif = parseInt(thisref.totalNotif,10) + parseInt($(this).attr("data"),10);
            });
            if (this.totalNotif>0)
              window.fluid.dockBadge = this.totalNotif;
            else
              window.fluid.dockBadge = "";
            if (this.totalNotif>this.oldNotif && this.profileStatus !== "donotdisturb" && this.profileStatus !== "offline") {
              window.fluid.showGrowlNotification({
                title: this.labels.get("label-title"),
                description: this.labels.get("label-new-messages"),
                priority: 1,
                sticky: false,
                identifier: "messages"
              });
            }
            this.oldNotif = this.totalNotif;
          } else if (window.webkitNotifications!==undefined) {
            this.totalNotif = 0;
            var thisref = this;
            $('span.room-total').each(function(index) {
              thisref.totalNotif = parseInt(thisref.totalNotif,10) + parseInt($(this).attr("data"),10);
            });
            if (this.totalNotif>this.oldNotif && this.profileStatus !== "donotdisturb" && this.profileStatus !== "offline") {

              var havePermission = window.webkitNotifications.checkPermission();
              if (havePermission == 0) {
                // 0 is PERMISSION_ALLOWED
                var notification = window.webkitNotifications.createNotification(
                  '/chat/img/chat.png',
                  this.labels.get("label-title"),
                  this.labels.get("label-new-messages")
                );

                notification.onclick = function () {
                  window.open("http://localhost:8080/portal/intranet/chat");
                  notification.close();
                }
                notification.show();
              } else {
                window.webkitNotifications.requestPermission();
              }
            }
            this.oldNotif = this.totalNotif;
          }
        }
      },
      error: function (response){
        //console.log("chat-users :: "+response);
        setTimeout($.proxy(this.errorOnRefresh, this), 1000);
      }
    });
  }
};

/**
 * Show rooms : convert json to html
 * @param rooms : a json object
 */
ChatApplication.prototype.showRooms = function(rooms) {
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

};

/**
 * Load Room : server call
 */
ChatApplication.prototype.loadRoom = function() {
  //console.log("TARGET::"+this.targetUser+" ; ISADMIN::"+this.isAdmin);
  if (this.targetUser!==undefined) {
    $(".users-online").removeClass("info");
    if (this.isDesktopView()) {
      var $targetUser = $("#users-online-"+this.targetUser.replace(".", "-"));
      $targetUser.addClass("info");
      $(".room-total").removeClass("room-total-white");
      $targetUser.find(".room-total").addClass("room-total-white");
    }

    $("#room-detail").css("display", "block");
    $(".target-user-fullname").text(this.targetFullname);
    if (this.targetUser.indexOf("space-")===-1) {
      $(".target-avatar-link").attr("href", "/portal/intranet/profile/"+this.targetUser);
      $(".target-avatar-image").attr("src", "/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:"+this.targetUser+"/soc:profile/soc:avatar");
    }
    else
    {
      var spaceName = this.targetFullname.toLowerCase().replace(" ", "_");
      $(".target-avatar-link").attr("href", "/portal/g/:spaces:"+spaceName+"/"+spaceName);
      $(".target-avatar-image").attr("src", "/rest/jcr/repository/social/production/soc:providers/soc:space/soc:"+spaceName+"/soc:profile/soc:avatar");
    }


    $.ajax({
      url: this.jzChatGetRoom,
      data: {"targetUser": this.targetUser,
        "user": this.username,
        "token": this.token,
        "isAdmin": this.isAdmin,
        "timestamp": new Date().getTime()
      },
      context: this,
      success: function(response){
        //console.log("SUCCESS::getRoom::"+response);
        this.room = response;
        var $msg = $('#msg');
        $msg.removeAttr("disabled");
        if (this.weemoExtension.isConnected) {
          $(".btn-weemo").removeClass('disabled');
        }
        if (this.isDesktopView()) $msg.focus();
        this.chatEventURL = this.jzChatSend+'?room='+this.room+'&user='+this.username+'&token='+this.token+'&event=0';

        jzStoreParam("lastUser"+this.username, this.targetUser, 60000);
        jzStoreParam("lastFullName"+this.username, this.targetFullname, 60000);
        jzStoreParam("lastTS"+this.username, "0");
        this.chatEventInt = window.clearInterval(this.chatEventInt);
        this.chatEventInt = setInterval($.proxy(this.refreshChat, this), this.chatIntervalChat);
        this.refreshChat();

      },

      error: function(xhr, status, error){
        //console.log("ERROR::"+xhr.responseText);
      }

    });
  }
};

/**
 * Error On Refresh
 */
ChatApplication.prototype.errorOnRefresh = function() {
  this.isLoaded = true;
  this.hidePanel(".chat-sync-panel");
  $("#chat-users").html("<span>&nbsp;</span>");
  this.hidePanel(".chat-login-panel");
  this.changeStatusChat("offline");
  this.showErrorPanel();
};

/**
 * Toggle Favorite : server call
 * @param targetFav : the user or space to put/remove in favorite
 */
ChatApplication.prototype.toggleFavorite = function(targetFav) {
  console.log("FAVORITE::"+targetFav);
  $.ajax({
    url: this.jzChatToggleFavorite,
    data: {"targetUser": targetFav,
      "user": this.username,
      "token": this.token,
      "timestamp": new Date().getTime()
    },
    context: this,
    success: function(response){
      this.refreshWhoIsOnline(this);
    },
    error: function(xhr, status, error){
    }
  });
};

/**
 * jQuery bindings on dom elements created by Who Is Online methods
 */
ChatApplication.prototype.jQueryForUsersTemplate = function() {
  var value = jzGetParam("lastUser"+this.username);
  var thiss = this;

  if (value && this.firstLoad) {
    //console.log("firstLoad with user : *"+value+"*");
    this.targetUser = value;
    this.targetFullname = jzGetParam("lastFullName"+this.username);
    if (this.username!==this.ANONIM_USER) {
      this.loadRoom();
    }
    this.firstLoad = false;
  }

  if (this.isDesktopView() && this.targetUser!==undefined) {
    var $targetUser = $("#users-online-"+this.targetUser.replace(".", "-"));
    $targetUser.addClass("info");
    $(".room-total").removeClass("room-total-white");
    $targetUser.find(".room-total").addClass("room-total-white");
  }


  $('.users-online').on("click", function() {
    thiss.targetUser = $(".room-link:first",this).attr("user-data");
    thiss.targetFullname = $(".room-link:first",this).attr("data-fullname");
    thiss.loadRoom();
    if (thiss.isMobileView()) {
      $(".right-chat").css("display", "block");
      $(".left-chat").css("display", "none");
      $(".room-name").html(thiss.targetFullname);
    }
  });


  $('.room-link').on("click", function() {
    thiss.targetUser = $(this).attr("user-data");
    thiss.targetFullname = $(this).attr("data-fullname");
    thiss.loadRoom();
    if (thiss.isMobileView()) {
      $(".right-chat").css("display", "block");
      $(".left-chat").css("display", "none");
      $(".room-name").html(thiss.targetFullname);
    }
  });

  $('.user-status').on("click", function() {
    var targetFav = $(this).attr("user-data");
    thiss.toggleFavorite(targetFav);
  });
  $('.user-favorite').on("click", function() {
    var targetFav = $(this).attr("user-data");
    thiss.toggleFavorite(targetFav);
  });
};

/**
 * Search and filter (filter on users or spaces if starts with @
 * @param filter
 */
ChatApplication.prototype.search = function(filter) {
  if (filter == ":aboutme" || filter == ":about me") {
    this.showAboutPanel();
  }
  if (filter.indexOf("@")!==0) {
    this.highlight = filter;
    this.showMessages();
  } else {
    this.userFilter = filter.substr(1, filter.length-1);
    this.filterInt = clearTimeout(this.filterInt);
    this.filterInt = setTimeout($.proxy(this.refreshWhoIsOnline, this), 500);
  }
};

/**
 * Check Browser Viewport Status
 * @returns {boolean}
 */
ChatApplication.prototype.checkViewportStatus = function() {
  return ($(".btn-mobile").css("display")!=="none");
};

ChatApplication.prototype.isMobileView = function() {
  return this.checkViewportStatus();
};

ChatApplication.prototype.isDesktopView = function() {
  return !this.checkViewportStatus();
};


/**
 * Set Current Status
 * @param status
 * @param callback
 */
ChatApplication.prototype.setStatus = function(status, callback) {

  if (status !== undefined) {
    //console.log("setStatus :: "+status);

    $.ajax({
      url: this.jzSetStatus,
      data: { "user": this.username,
        "token": this.token,
        "status": status,
        "timestamp": new Date().getTime()
      },
      context: this,

      success: function(response){
        //console.log("SUCCESS:setStatus::"+response);
        this.changeStatusChat(response);
        if (typeof callback === "function") {
          callback(response);
        }

      },
      error: function(response){
        this.changeStatusChat("offline");
        if (typeof callback === "function") {
          callback("offline");
        }
      }

    });
  }

};

ChatApplication.prototype.setStatusAvailable = function() {
  chatApplication.setStatus("available");
};

ChatApplication.prototype.setStatusAway = function() {
  chatApplication.setStatus("away");
};

ChatApplication.prototype.setStatusDoNotDisturb = function() {
  chatApplication.setStatus("donotdisturb");
};

ChatApplication.prototype.setStatusInvisible = function() {
  chatApplication.setStatus("invisible");
};

ChatApplication.prototype.createWeemoCall = function() {
  console.log("targetUser : "+chatApplication.targetUser);
  console.log("targetFullname   : "+chatApplication.targetFullname);

  var chatMessage = {
    "url" : chatApplication.jzChatSend,
    "user" : chatApplication.username,
    "targetUser" : chatApplication.targetUser,
    "room" : chatApplication.room,
    "token" : chatApplication.token
  };
  weemoExtension.createWeemoCall(chatApplication.targetUser, chatApplication.targetFullname, chatMessage);

  //this.weemoExtension.createWeemoCall(this.targetUser, this.fullname);
};

/**
 * Send message to server
 * @param msg : the msg to send
 * @param callback : the method to execute on success
 */
ChatApplication.prototype.sendMessage = function(msg, callback) {


  var isSystemMessage = (msg.indexOf("/")===0) ;

  if (isSystemMessage) {
    msg = msg.replace("/me", this.fullname);

    if (msg.indexOf("/call")===0) {
      this.createWeemoCall();

      document.getElementById("msg").value = '';
      return;
    } else if (msg.indexOf("/join")===0) {
      this.weemoExtension.joinWeemoCall();
      document.getElementById("msg").value = '';
      return;
    } else if (msg.indexOf("/terminate")===0) {
      document.getElementById("msg").value = '';
      ts = Math.round(new Date().getTime() / 1000);
      msg = "Call terminated&"+ts;
      this.weemoExtension.setCallOwner(false);
      this.weemoExtension.setCallActive(false);
    }
  }

  var im = this.messages.length;
  this.messages[im] = {"user": this.username,
    "fullname": "You",
    "date": "pending",
    "message": msg,
    "isSystem": isSystemMessage};
  this.showMessages();
  document.getElementById("msg").value = '';

  $.ajax({
    url: this.jzChatSend,
    data: {"user": this.username,
      "targetUser": this.targetUser,
      "room": this.room,
      "message": msg,
      "token": this.token,
      "timestamp": new Date().getTime(),
      "isSystem": isSystemMessage
    },
    context: this,

    success:function(response){
      //console.log("success");
      this.refreshChat();
      if (typeof callback === "function") {
        callback();
      }
    },

    error:function (xhr, status, error){

    }

  });


};



/**
 ##################                           ##################
 ##################                           ##################
 ##################   CHAT PANELS             ##################
 ##################                           ##################
 ##################                           ##################
 */

ChatApplication.prototype.hidePanel = function(panel) {
  $(panel).css("display", "none");
  $(panel).html("");
};

ChatApplication.prototype.hidePanels = function() {
  this.hidePanel(".chat-sync-panel");
  this.hidePanel(".chat-error-panel");
  this.hidePanel(".chat-login-panel");
  this.hidePanel(".chat-about-panel");
  this.hidePanel(".chat-demo-panel");
};

ChatApplication.prototype.showSyncPanel = function() {
  if (!this.isLoaded) {
    this.hidePanels();
    var $chatSyncPanel = $(".chat-sync-panel");
    $chatSyncPanel.html("<img src=\"/chat/img/sync.gif\" width=\"64px\" class=\"chatSync\" />");
    $chatSyncPanel.css("display", "block");
  }
};

ChatApplication.prototype.showHelpPanel = function() {
  this.hidePanels();
  $(".chat-help-panel").css("display", "block");

};

ChatApplication.prototype.showErrorPanel = function() {
  this.hidePanels();
  //console.log("show-error-panel");
  var $chatErrorPanel = $(".chat-error-panel");
  $chatErrorPanel.html(this.labels.get("label-panel-error1")+"<br/><br/>"+this.labels.get("label-panel-error2"));
  $chatErrorPanel.css("display", "block");
};

ChatApplication.prototype.showLoginPanel = function() {
  this.hidePanels();
  //console.log("show-login-panel");
  var $chatLoginPanel = $(".chat-login-panel");
  $chatLoginPanel.html(this.labels.get("label-panel-login1")+"<br><br><a href=\"#\" onclick=\"javascript:reloadWindow();\">"+this.labels.get("label-panel-login2")+"</a>");
  $chatLoginPanel.css("display", "block");
};

ChatApplication.prototype.showAboutPanel = function() {
  var about = "eXo Community Chat<br>";
  about += "Version 0.6-SNAPSHOT<br><br>";
  about += "Designed and Developed by <a href=\"mailto:bpaillereau@gmail.com\">Benjamin Paillereau</a><br>";
  about += "Sources available on <a href=\"https://github.com/exo-addons/chat-application\" target=\"_new\">https://github.com/exo-addons/chat-application</a>";
  about += "<br><br><a href=\"#\" id=\"about-close-btn\" >Close</a>";
  this.hidePanels();
  var $chatAboutPanel = $(".chat-about-panel");
  $chatAboutPanel.html(about);
  $chatAboutPanel.width($('#chat-application').width()+40);
  $chatAboutPanel.css("display", "block");

  var thiss = this;
  $("#about-close-btn").on("click", function() {
    thiss.hidePanel('.chat-about-panel');
    $('#chat-search').attr("value", "");
  });
};

ChatApplication.prototype.showDemoPanel = function() {
  this.hidePanels();
  //console.log("show-demo-panel");
  var $chatDemoPanel = $(".chat-demo-panel");
  var intro = this.labels.get("label-panel-demo");
  if (this.isPublic) intro = this.labels.get("label-panel-public");
  $chatDemoPanel.html(intro+"<br><br><div class='welcome-panel'>" +
    "<br><br>"+this.labels.get("label-display-name")+"&nbsp;&nbsp;<input type='text' id='anonim-name'>" +
    "<br><br>"+this.labels.get("label-email")+"&nbsp;&nbsp;<input type='text' id='anonim-email'></div>" +
    "<br><a href='#' id='anonim-save'>"+this.labels.get("label-save-profile")+"</a>");
  $chatDemoPanel.css("display", "block");

  $("#anonim-save").on("click", function() {
    var fullname = $("#anonim-name").val();
    var email = $("#anonim-email").val();
    this.createDemoUser(fullname, email);
  });
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
    val = $.data(this.element, key);
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
    $.data(this.element, key, value);
};

/**
 * Logs what are the available values (only those created by jQuery)
 */
JuzuLabels.prototype.log = function() {
  if (this.element!=="") {
    console.log($.data( this.element ));
  }
};
