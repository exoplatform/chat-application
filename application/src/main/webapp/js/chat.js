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
  chatApplication.jzChatRead = chatServerURL+"/read";
  chatApplication.jzChatGetRoom = chatServerURL+"/getRoom";
  chatApplication.jzChatGetCreator = chatServerURL+"/getCreator";
  chatApplication.jzChatToggleFavorite = chatServerURL+"/toggleFavorite";
  chatApplication.jzChatUpdateUnreadMessages = chatServerURL+"/updateUnreadMessages";
  chatApplication.jzUsers = chatServerURL+"/users";
  chatApplication.jzDelete = chatServerURL+"/delete";
  chatApplication.jzEdit = chatServerURL+"/edit";
  chatApplication.jzSaveTeamRoom = chatServerURL+"/saveTeamRoom";
  chatApplication.room = "<%=room%>";

  chatApplication.initChat();
  chatApplication.initChatProfile();

  /**
   * Init Global Variables
   *
   */
  //needed for #chat text area
  var keydown = -1;
  //needed for #edit-modal-text area
  var keydownModal = -1;
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
    if ( keydown === 18 ) {
      keydown = -1;
    }
    if ( event.which === 13 && msg.length === 1) {
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

  $(".room-detail-fullname").on("click", function() {
    if (chatApplication.isMobileView()) {
      $(".uiLeftContainerArea").css("display", "block");
      $(".uiRightContainerArea").css("display", "none");
    }
  });


  $('#chat-search').keyup(function(event) {
    var filter = $(this).val();
    chatApplication.search(filter);
  });

  $('#team-add-user').keyup(function(event) {
    if ( event.which === 13 ) { // ENTER
      $(".team-user").each(function() {
        if ($(this).hasClass("team-user-selected")) {
          var name = $(this).attr("data-name");
          var fullname = $(this).attr("data-fullname");
          addTeamUserLabel(name, fullname);
        }
      });
    } else if ( event.which === 40 || event.which === 38) { // 40:DOWN || 38:UP
      var isUp = (event.which === 38);
      var total = $(".team-user").size();
      var done = false;
      $(".team-user").each(function(index) {
        if (!done && $(this).hasClass("team-user-selected")) {
          done = true;
          $(".team-user").removeClass("team-user-selected");
          if (isUp) {
            if (index === 0)
              $(".team-user").last().addClass("team-user-selected");
            else
              $(this).prev().addClass("team-user-selected");
          } else {
            if (index === total-1)
              $(".team-user").first().addClass("team-user-selected");
            else
              $(this).next().addClass("team-user-selected");
          }
        }
      });
      return;
    }
    var filter = $(this).val();
    if (filter === "") {
      var $userResults = $(".team-users-results");
      $userResults.css("display", "none");
      $userResults.html("");
    } else {
      chatApplication.getAllUsers(filter, function (jsonData) {
        var users = TAFFY(jsonData.users);
        var users = users();
        var $userResults = $(".team-users-results");
        $userResults.css("display", "none");
        var html = "";
        users = users.filter({name:{"!is":chatApplication.username}});
        $(".team-user-label").each(function() {
          var name = $(this).attr("data-name");
          users = users.filter({name:{"!is":name}});
        });

        users.order("fullname").limit(5).each(function (user, number) {
          $userResults.css("display", "block");
          if (user.status == "offline") user.status = "invisible";
          var classSel = "";
          if (number === 0) classSel = "team-user-selected"
          html += "<div class='team-user "+classSel+"' data-name='"+user.name+"' data-fullname='"+user.fullname+"'>";
          html += "  <span class='team-user-logo'><img src='/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:"+user.name+"/soc:profile/soc:avatar' width='30px' style='width:30px;'></span>";
          html += "  <span class='chat-status-team chat-status-"+user.status+"'></span>";
          html += "  <span class='team-user-fullname'>"+user.fullname+"</span>";
          html += "  <span class='team-user-name'>"+user.name+"</span>";
          html += "</div>";
        });
        $userResults.html(html);

        $('.team-user').on("mouseover", function() {
          $(".team-user").removeClass("team-user-selected");
          $(this).addClass("team-user-selected");
        });

        $('.team-user').on("click", function() {
          var name = $(this).attr("data-name");
          var fullname = $(this).attr("data-fullname");
          addTeamUserLabel(name, fullname);
        });

      });
    }
  });

  function addTeamUserLabel(name, fullname) {
    var $usersList = $('.team-users-list');
    var html = $usersList.html();
    html += "<span class='label team-user-label' data-name='"+name+"'>"+fullname+"&nbsp;&nbsp;<i class='icon-remove icon-white team-user-remove'></i></span>";
    $usersList.html(html);
    var $teamAddUser = $('#team-add-user');
    $teamAddUser.val("");
    $teamAddUser.focus();
    var $userResults = $(".team-users-results");
    $userResults.css("display", "none");
    $userResults.html("");

    $(".team-user-remove").on("click", function() {
      $(this).parent().remove();
    });

  }

  function strip(html)
  {
    var tmp = document.createElement("DIV");
    tmp.innerHTML = html;
    return tmp.textContent||tmp.innerText;
  }



  $(".team-edit-button").on("click", function() {
    var $uitext = $("#team-modal-name");
    $uitext.val(chatApplication.targetFullname);
    $uitext.attr("data-id", chatApplication.targetUser);

    chatApplication.getUsers(chatApplication.targetUser, function (jsonData) {
      $(".team-user-label").remove();

      var users = TAFFY(jsonData.users);
      var users = users();
      users.order("fullname").each(function (user, number) {
        if (user.name !== chatApplication.username) {
          addTeamUserLabel(user.name, user.fullname);
        }
      });

      $('#team-modal').modal({"backdrop": false});
      $uitext.focus();

    });

  });

  $(".team-modal-cancel").on("click", function() {
    $('#team-modal').modal('hide');
    var $uitext = $("#team-modal-name");
    $uitext.val("");
    $uitext.attr("data-id", "---");
  });

  $(".team-modal-save").on("click", function() {
    var $uitext = $("#team-modal-name");
    var teamName = $uitext.val();
    var teamId = $uitext.attr("data-id");
    $('#team-modal').modal('hide');

    var users = chatApplication.username;
    $(".team-user-label").each(function(index) {
      var name = $(this).attr("data-name");
      users += ","+name;
    });

    chatApplication.saveTeamRoom(teamName, teamId, users, function(data) {
      var teamName = data.name;
      var roomId = "team-"+data.room;
      chatApplication.refreshWhoIsOnline(roomId, teamName);
    });

    $uitext.val("");
    $uitext.attr("data-id", "---");

  });


  $(".btn-weemo").on("click", function() {
    if (!$(this).hasClass("disabled"))
      chatApplication.createWeemoCall();
  });

  $(".btn-weemo-conf").on("click", function() {
    if (!$(this).hasClass("disabled"))
      weemoExtension.joinWeemoCall();
  });

  $(".text-modal-close").on("click", function() {
    $('#text-modal').modal('hide');
  });

  $(".edit-modal-cancel").on("click", function() {
    $('#edit-modal').modal('hide');
    $("#edit-modal-area").val("");
  });

  $(".edit-modal-save").on("click", function() {
    var $uitext = $("#edit-modal-area");
    var id = $uitext.attr("data-id");
    var message = $uitext.val();
    $uitext.val("");
    $('#edit-modal').modal('hide');

    chatApplication.editMessage(id, message, function() {
      chatApplication.refreshChat(true);
    });

  });

  $('#edit-modal-area').keydown(function(event) {
//    console.log("keydown : "+ event.which+" ; "+keydown);
    if ( event.which == 18 ) {
      keydownModal = 18;
    }
  });

  $('#edit-modal-area').keyup(function(event) {
    var id = $(this).attr("data-id");
    var msg = $(this).val();
//    console.log("keyup : "+event.which + ";"+msg.length+";"+keydown);
    if ( event.which === 13 && keydownModal !== 18 && msg.length>1) {
      //console.log("sendMsg=>"+username + " : " + room + " : "+msg);
      if(!msg)
      {
        return;
      }
//      console.log("*"+msg+"*");
      $(this).val("");
      $('#edit-modal').modal('hide');

      chatApplication.editMessage(id, msg, function() {
        chatApplication.refreshChat(true);
      });

    }
    if ( keydownModal === 18 ) {
      keydownModal = -1;
    }
    if ( event.which === 13 && msg.length === 1) {
      $(this).val('');
    }

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
  this.rooms = "";
  this.username = "";
  this.fullname = "";
  this.targetUser = "";
  this.targetFullname = "";
  this.token = "";
  this.jzInitChatProfile = "";
  this.jzWhoIsOnline = "";
  this.jzChatGetRoom = "";
  this.jzChatGetCreator = "";
  this.jzChatToggleFavorite = "";
  this.jzCreateDemoUser = "";
  this.jzChatUpdateUnreadMessages = "";
  this.jzChatSend = "";
  this.jzChatRead = "";
  this.jzGetStatus = "";
  this.jzSetStatus = "";
  this.jzMaintainSession = "";
  this.jzUsers = "";
  this.jzDelete = "";
  this.jzEdit = "";
  this.jzSaveTeamRoom = "";
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
  this.isTeamAdmin = false;

  this.old = '';
  this.firstLoad = true;

  this.profileStatus = "offline";
  this.whoIsOnlineMD5 = 0;
  this.totalNotif = 0;
  this.oldNotif = 0;

  this.showFavorites = true;
  this.showPeople = true;
  this.showSpaces = true;
  this.showTeams = true;


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

      this.refreshWhoIsOnline();
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
 * Delete the message with id in the room
 *
 * @param id
 * @param callback
 */
ChatApplication.prototype.deleteMessage = function(id, callback) {
  $.ajax({
    url: this.jzDelete,
    data: {"room": this.room,
      "user": this.username,
      "token": this.token,
      "messageId": id
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
 * Edit the message with id with a new message
 *
 * @param id
 * @param newMessage
 * @param callback
 */
ChatApplication.prototype.editMessage = function(id, newMessage, callback) {
  $.ajax({
    url: this.jzEdit,
    data: {"room": this.room,
      "user": this.username,
      "token": this.token,
      "messageId": id,
      "message": newMessage
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
 * Saves a Team room for current user
 *
 * @param teamName
 * @param room
 * @param callback : callback method with roomId as a parameter
 */
ChatApplication.prototype.saveTeamRoom = function(teamName, room, users, callback) {
  $.ajax({
    url: this.jzSaveTeamRoom,
    dataType: "json",
    data: {"teamName": teamName,
      "room": room,
      "users": users,
      "user": this.username,
      "token": this.token
    },

    success:function(response){
      //console.log("success");
      if (typeof callback === "function") {
        callback(response);
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

  var homeLinkHtml = $("#HomeLink").html();
  homeLinkHtml = '<a href="#" class="btn-home-responsive"></a>'+homeLinkHtml;
  $("#HomeLink").html(homeLinkHtml);

  $(".btn-home-responsive").on("click", function() {
    var $leftNavigationTDContainer = $(".LeftNavigationTDContainer");
    if ($leftNavigationTDContainer.css("display")==="none") {
      $leftNavigationTDContainer.animate({width: 'show', duration: 200});
    } else {
      $leftNavigationTDContainer.animate({width: 'hide', duration: 200});
    }
  });

  this.initChatPreferences();

  this.chatOnlineInt = clearInterval(this.chatOnlineInt);
  this.chatOnlineInt = setInterval($.proxy(this.refreshWhoIsOnline, this), this.chatIntervalUsers);
  this.refreshWhoIsOnline();

  if (this.username!==this.ANONIM_USER) setTimeout($.proxy(this.showSyncPanel, this), 1000);
};



/**
 * Init Chat Preferences
 */
ChatApplication.prototype.initChatPreferences = function() {
  this.showFavorites = true;
  if (jzGetParam("chatShowFavorites"+this.username) === "false") this.showFavorites = false;
  this.showPeople = true;
  if (jzGetParam("chatShowPeople"+this.username) === "false") this.showPeople = false;
  this.showSpaces = true;
  if (jzGetParam("chatShowSpaces"+this.username) === "false") this.showSpaces = false;
  this.showTeams = true;
  if (jzGetParam("chatShowTeams"+this.username) === "false") this.showTeams = false;
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
        this.isTeamAdmin = (data.isTeamAdmin=="true");

        var $chatApplication = $("#chat-application");
        $chatApplication.attr("data-token", this.token);
        var $labelUser = $(".label-user");
        $labelUser.text(data.fullname);

        this.refreshWhoIsOnline();
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
 * Get the users of the space
 *
 * @param spaceId : the ID of the space
 * @param callback : return the json users data list as a parameter of the callback function
 */
ChatApplication.prototype.getUsers = function(roomId, callback) {
  $.ajax({
    url: this.jzUsers,
    data: {"room": roomId,
      "user": this.username,
      "token": this.token
    },
    dataType: "json",
    context: this,
    success: function(response){
      if (typeof callback === "function") {
        callback(response);
      }
    }
  });
};

/**
 * Get all users corresponding to filter
 *
 * @param filter : the filter (ex: Ben Pa)
 * @param callback : return the json users data list as a parameter of the callback function
 */
ChatApplication.prototype.getAllUsers = function(filter, callback) {
  $.ajax({
    url: this.jzUsers,
    data: {"filter": filter,
      "user": this.username,
      "token": this.token
    },
    dataType: "json",
    context: this,
    success: function(response){
      if (typeof callback === "function") {
        callback(response);
      }
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

    var messages = TAFFY(this.messages);
    var thiss = this;
    messages().order("timestamp asec").each(function (message) {

      if (message.isSystem!=="true")
      {
        if (prevUser != message.user)
        {
          if (prevUser !== "")
            out += "</span></div>";
          if (message.user != thiss.username) {
            out += "<div class='msgln-odd'>";
            out += "<span style='position:relative; padding-right:16px;padding-left:4px;top:8px'>";
            if (thiss.isPublic)
              out += "<img src='/chat/img/support-avatar.png' width='30px' style='width:30px;'>";
            else
              out += "<img src='/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:"+message.user+"/soc:profile/soc:avatar' width='30px' style='width:30px;'>";
            out += "</span>";
            out += "<span>";
            if (thiss.isPublic)
              out += "<span class='invisible-text'>- </span><a href='#'>"+thiss.labels.get("label-support-fullname")+"</a><span class='invisible-text'> : </span><br/>";
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
        var msgtemp = message.message;
        if (message.type === "DELETED") {
          msgtemp = "<span class='contentDeleted'>"+thiss.labels.get("label-deleted")+"</span>";
        } else {
          msgtemp = thiss.messageBeautifier(message.message);
        }
        out += "<div style='margin-left:50px;' class='msg-text'><span style='float:left'>"+msgtemp+"</span>" +
          "<span class='invisible-text'> [</span>";
        out += "<span style='float:right;color:#CCC;font-size:10px' class='msg-date'>";
        if (message.type === "DELETED" || message.type === "EDITED") {
          out += "<span class='message-changed'></span>";
        }
        out += message.date+"</span>";
        if (message.type !== "DELETED") {
          out += "<span style='float:right;color:#CCC;font-size:10px;display:none;' class='msg-actions'>" +
            "<span style='display: none;' class='msg-data' data-id='"+message.id+"' data-fn='"+message.fullname+"'>"+message.message+"</span>";
          if (message.user === thiss.username) {
            out += "&nbsp;<a href='#' class='msg-action-delete'>"+thiss.labels.get("label-delete")+"</a>&nbsp;|";
            out += "&nbsp;<a href='#' class='msg-action-edit'>"+thiss.labels.get("label-edit")+"</a>&nbsp;|";
          }
          out += "&nbsp;<a href='#' class='msg-action-quote'>"+thiss.labels.get("label-quote")+"</a>";
          out += "</span>";
        }
        out += "<span class='invisible-text'>]</span></div>"+
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
        out += "<span style='position:relative; padding-right:14px;padding-left:4px;top:8px'>";
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

        if (message.message==="Call active") {
          out += "<img class='call-on' src='/chat/img/empty.png' width='32px' style='width:32px;'>";
          if (options.timestamp!==undefined) {
            jzStoreParam("weemoCallHandler", options.timestamp, 600000)
          }
          $(".btn-weemo").addClass('disabled');
        } else if (message.message==="Call terminated") {
          out += "<img class='call-off' src='/chat/img/empty.png' width='32px' style='width:32px;'>";
          $(".btn-weemo").removeClass('disabled');
        } else {
          out += "<img src='/chat/img/empty.png' width='32px' style='width:32px;'>";
        }
        out += "</span>";
        out += "<span>";
        if (nbOptions===0) out+="<center>";
        out += "<b style=\"line-height: 12px;vertical-align: bottom;\">"+message.message+"</b>";
        if (nbOptions===0) out+="</center>";

        if (message.message==="Call terminated" && nbOptions>0) {
          var tsold = Math.round(jzGetParam("weemoCallHandler"));
          var time = Math.round(options.timestamp)-tsold;
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

        if (nbOptions===3) {
          thiss.weemoExtension.setUidToCall(options.uidToCall);
          thiss.weemoExtension.setDisplaynameToCall(options.displaynameToCall);
          $(".btn-weemo").css("display", "none");
          $(".btn-weemo-conf").css("display", "block");
          if (options.uidToCall!=="weemo"+thiss.username)
            $(".btn-weemo-conf").removeClass("disabled");
          else
            $(".btn-weemo-conf").addClass("disabled");
        } else {
          $(".btn-weemo").css("display", "block");
          $(".btn-weemo-conf").css("display", "none");
        }


        message.message = "";
        out += "<div style='margin-left:50px;'><span style='float:left'>"+thiss.messageBeautifier(message.message)+"</span>" +
          "<span class='invisible-text'> [</span>"+
          "<span style='float:right;color:#CCC;font-size:10px'>"+message.date+"</span>" +
          "<span class='invisible-text'>]</span></div>"+
          "<div style='clear:both;'></div>";
        out += "</span></div>";
        out += "<hr style='margin: 0'>";
        out += "<div><span>";
        prevUser = "__system";

      }
    });

  }
  var $chats = $("#chats");
  $chats.html('<span>'+out+'</span>');
  sh_highlightDocument();
  $chats.animate({ scrollTop: 20000 }, 'fast');

  $(".msg-text").mouseover(function() {
    if ($(".msg-actions", this).children().length > 0) {
      $(".msg-date", this).css("display", "none");
      $(".msg-actions", this).css("display", "inline-block");
    }
  });

  $(".msg-text").mouseout(function() {
    $(".msg-date", this).css("display", "inline-block");
    $(".msg-actions", this).css("display", "none");
  });

  $(".msg-action-quote").on("click", function() {
    var $uimsg = $(this).siblings(".msg-data");
    var msgHtml = $uimsg.html();
    //if (msgHtml.endsWith("<br>")) msgHtml = msgHtml.substring(0, msgHtml.length-4);
    msgHtml = msgHtml.replace(/<br>/g, '\n');
    var msgFullname = $uimsg.attr("data-fn");
    $("#msg").focus().val('').val("[quote="+msgFullname+"]"+msgHtml+" [/quote] ");

  });

  $(".msg-action-delete").on("click", function() {
    var $uimsg = $(this).siblings(".msg-data");
    var msgId = $uimsg.attr("data-id");
    chatApplication.deleteMessage(msgId, function() {
      chatApplication.refreshChat(true);
    });
    //if (msgHtml.endsWith("<br>")) msgHtml = msgHtml.substring(0, msgHtml.length-4);

  });

  $(".msg-action-edit").on("click", function() {
    var $uimsg = $(this).siblings(".msg-data");
    var msgId = $uimsg.attr("data-id");
    var msgHtml = $uimsg.html();
    msgHtml = msgHtml.replace(eval("/<br>/g"), "\n");

    $("#edit-modal-area").val(msgHtml);
    $("#edit-modal-area").attr("data-id", msgId);
    $('#edit-modal').modal({"backdrop": false});

//    chatApplication.deleteMessage(msgId, function() {
//      chatApplication.refreshChat(true);
//    });

  });

};

/**
 * Refresh Chat : refresh messages and panels
 */
ChatApplication.prototype.refreshChat = function(forceRefresh) {
  //var thiss = chatApplication;
  if (this.username !== this.ANONIM_USER) {
    var lastTS = jzGetParam("lastTS"+this.username);

    //url: this.chatEventURL+"&fromTimestamp="+lastTS,
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
          if (ts != lastTS || (forceRefresh === true)) {
            jzStoreParam("lastTS"+this.username, ts, 600);
            //console.log("new data to show");
            this.showMessages(data.messages);
          }
        }
//        if (this.isDesktopView()) $(".right-chat").css("display", "block");
        this.hidePanel(".chat-login-panel");
        this.hidePanel(".chat-error-panel");
      },
      error: function(jqXHR, textStatus, errorThrown) {
//        if (this.isDesktopView()) $(".right-chat").css("display", "none");
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

  var quote = "";
  if (message.indexOf("[quote=")===0) {
    var iq = message.indexOf("]");
    quote = "<div class='contentQuote'><b>"+message.substring(7, iq)+":</b><br>";
    message = message.substr(iq+1);
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
          } else if (w.indexOf("[/quote]")===-1) {
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

  if (quote !== "") {
    if (msg.indexOf("[/quote]")>0)
      msg = msg.replace("[/quote]", "</div>");
    else
      msg = msg + "</div>";
    msg = quote + msg;
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

ChatApplication.prototype.getObjectSize = function(obj) {
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

ChatApplication.prototype.updateTotal = function(total) {
  this.totalNotif = total;//Math.abs(this.getOfflineNotif())+Math.abs(this.getOnlineNotif())+Math.abs(this.getSpacesNotif());
};

ChatApplication.prototype.updateTitle = function() {
  if (this.totalNotif>0) {
    document.title = "Chat ("+this.totalNotif+")";
  } else {
    document.title = "Chat";
  }
};

/**
 * Refresh Who Is Online : server call
 */
ChatApplication.prototype.refreshWhoIsOnline = function(targetUser, targetFullname) {
  var withSpaces = jzGetParam("chat.button.space", "true");
  var withUsers = jzGetParam("chat.button.user", "true");
  var withPublic = jzGetParam("chat.button.public", "false");
  var withOffline = jzGetParam("chat.button.offline", "false");

  if (this.username.indexOf(this.ANONIM_USER)>-1) {
    withUsers = "true";
    withSpaces = "true";
    withPublic = "false";
    withOffline = "false";
  }

  if (this.username !== this.ANONIM_USER && this.token !== "---") {
    $.ajax({
      url: this.jzChatWhoIsOnline,
      dataType: "json",
      data: { "user": this.username,
        "token": this.token,
        "filter": this.userFilter,
        "isAdmin": this.isAdmin,
        "timestamp": new Date().getTime()},
      context: this,
      success: function(response){
        if (targetUser !== undefined && targetFullname !== undefined) {
          this.targetUser = targetUser;
          this.targetFullname = targetFullname;
          jzStoreParam("lastUsername"+this.username, this.targetUser, 60000);
          jzStoreParam("lastFullName"+this.username, this.targetFullname, 60000);
          jzStoreParam("lastTS"+this.username, "0");
          this.firstLoad = true;
        }
//        console.log("refreshWhoIsOnline : "+this.targetUser+" : "+this.targetFullname);

        var tmpMD5 = response.md5;
        if (tmpMD5 !== this.whoIsOnlineMD5) {
          var rooms = TAFFY(response.rooms);
          this.whoIsOnlineMD5 = tmpMD5;
          this.isLoaded = true;
          this.hidePanel(".chat-error-panel");
          this.hidePanel(".chat-sync-panel");
          this.showRooms(rooms);



          this.updateTotal(Math.abs(response.unreadOffline)+Math.abs(response.unreadOnline)+Math.abs(response.unreadSpaces)+Math.abs(response.unreadTeams));
          if (window.fluid!==undefined) {
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
          } else if (window.webkitNotifications!==undefined) {
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
          }
          this.oldNotif = this.totalNotif;
          this.updateTitle();
        }
        if (this.isTeamAdmin) {
          $(".btn-top-add-actions").css("display", "inline-block");
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
  this.rooms = rooms;
  var roomPrevUser = "";
  var out = '<table class="table list-rooms">';
  var classArrow;
  var totalFavorites = 0, totalPeople = 0, totalSpaces = 0, totalTeams = 0;

  out += "<tr class='header-room header-favorites'><td colspan='3' style='border-top: 0;'>";
  if (this.showFavorites) classArrow="uiIconArrowDown"; else classArrow = "uiIconArrowRight";
  out += "<div class='nav pull-left uiDropdownWithIcon'><div class='uiAction'><i class='"+classArrow+" uiIconLightGray'></i></div></div>";
  out += chatApplication.labels.get("label-header-favorites");
  out += '<span class="room-total total-favorites"></span>';
  out += "</td></tr>"

  var roomsFavorites = rooms();
  roomsFavorites = roomsFavorites.filter({isFavorite:{is:"true"}});
  roomsFavorites.order("isFavorite desc, timestamp desc, escapedFullname logical").each(function (room) {
//    console.log("FAVORITES : "+room.escapedFullname);
    var rhtml = chatApplication.getRoomHtml(room, roomPrevUser);
    if (rhtml !== "") {
      roomPrevUser = room.user;
      if (chatApplication.showFavorites) {
        out += rhtml;
      } else {
        if (Math.round(room.unreadTotal)>0) {
          totalFavorites += Math.round(room.unreadTotal);
        }
      }
    }
  });


  out += "<tr class='header-room header-people'><td colspan='3'>";
  if (this.showPeople) classArrow="uiIconArrowDown"; else classArrow = "uiIconArrowRight";
  out += "<div class='nav pull-left uiDropdownWithIcon'><div class='uiAction'><i class='"+classArrow+" uiIconLightGray'></i></div></div>";
  out += chatApplication.labels.get("label-header-people");
  out += '<span class="room-total total-people"></span>';
  out += "</td></tr>";

  var roomsPeople = rooms();
  roomsPeople = roomsPeople.filter({status:{"!is":"space"}});
  roomsPeople = roomsPeople.filter({status:{"!is":"team"}});
  roomsPeople = roomsPeople.filter({isFavorite:{"!is":"true"}});
  roomsPeople.order("isFavorite desc, timestamp desc, escapedFullname logical").each(function (room) {
//    console.log("PEOPLE : "+room.escapedFullname);
    var rhtml = chatApplication.getRoomHtml(room, roomPrevUser);
    if (rhtml !== "") {
      roomPrevUser = room.user;
      if (chatApplication.showPeople) {
        out += rhtml;
      } else {
        if (Math.round(room.unreadTotal)>0) {
          totalPeople += Math.round(room.unreadTotal);
        }
      }
    }
  });


  out += "<tr class='header-room header-spaces'><td colspan='3'>";
  if (this.showSpaces) classArrow="uiIconArrowDown"; else classArrow = "uiIconArrowRight";
  out += "<div class='nav pull-left uiDropdownWithIcon'><div class='uiAction'><i class='"+classArrow+" uiIconLightGray'></i></div></div>";
  out += chatApplication.labels.get("label-header-spaces");
  out += '<span class="room-total total-spaces"></span>';
  out += "</td></tr>";

  var roomsSpaces = rooms();
  roomsSpaces = roomsSpaces.filter({status:{"is":"space"}});
  roomsSpaces = roomsSpaces.filter({isFavorite:{"!is":"true"}});
  roomsSpaces.order("isFavorite desc, timestamp desc, escapedFullname logical").each(function (room) {
//    console.log("SPACES : "+room.escapedFullname);
    var rhtml = chatApplication.getRoomHtml(room, roomPrevUser);
    if (rhtml !== "") {
      roomPrevUser = room.user;
      if (chatApplication.showSpaces) {
        out += rhtml;
      } else {
        if (Math.round(room.unreadTotal)>0) {
          totalSpaces += Math.round(room.unreadTotal);
        }
      }
    }
  });

  out += "<tr class='header-room header-teams'><td colspan='3'>";
  if (this.showTeams) classArrow="uiIconArrowDown"; else classArrow = "uiIconArrowRight";
  out += "<div class='nav pull-left uiDropdownWithIcon'><div class='uiAction'><i class='"+classArrow+" uiIconLightGray'></i></div></div>";
  out += chatApplication.labels.get("label-header-teams");
  out += '<span class="room-total total-teams"></span>';
  out += "<ul class='nav pull-right uiDropdownWithIcon btn-top-add-actions' style='margin-right: 5px;'><li><div class='uiActionWithLabel btn-add-team' href='javaScript:void(0)'><i class='uiIconSimplePlusMini uiIconLightGray'></i></div></li></ul>";
  out += "</td></tr>";

  var roomsTeams = rooms();
  roomsTeams = roomsTeams.filter({status:{"is":"team"}});
  roomsTeams = roomsTeams.filter({isFavorite:{"!is":"true"}});
  roomsTeams.order("isFavorite desc, timestamp desc, escapedFullname logical").each(function (room) {
//    console.log("TEAMS : "+room.escapedFullname);
    var rhtml = chatApplication.getRoomHtml(room, roomPrevUser);
    if (rhtml !== "") {
      roomPrevUser = room.user;
      if (chatApplication.showTeams) {
        out += rhtml;
      } else {
        if (Math.round(room.unreadTotal)>0) {
          totalTeams += Math.round(room.unreadTotal);
        }
      }
    }
  });

  out += '</table>';

  $("#chat-users").html(out);

  this.jQueryForUsersTemplate();


  if (chatApplication.isTeamAdmin) {
    $(".btn-top-add-actions").css("display", "inline-block");
  }

  if (totalFavorites>0) {
    $(".total-favorites").html(totalFavorites);
    $(".total-favorites").css("display", "inline-block");
  }

  if (totalPeople>0) {
    $(".total-people").html(totalPeople);
    $(".total-people").css("display", "inline-block");
  }

  if (totalSpaces>0) {
    $(".total-spaces").html(totalSpaces);
    $(".total-spaces").css("display", "inline-block");
  }

  if (totalTeams>0) {
    $(".total-teams").html(totalTeams);
    $(".total-teams").css("display", "inline-block");
  }

};

ChatApplication.prototype.getRoomHtml = function(room, roomPrevUser) {
  var out = "";
  if (room.user!==roomPrevUser) {
    out += '<tr id="users-online-'+room.user.replace(".", "-")+'" class="users-online">';
    out += '<td class="td-status">';
    out += '<span class="';
    if (room.isFavorite == "true") {
      out += 'user-favorite';
    } else {
      out += 'user-status';
    }
    if (room.status === "space" || room.status === "team") {
      out += ' user-space-front';
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
  }
  return out;
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
    $(".team-button").css("display", "none");
    $(".target-user-fullname").text(this.targetFullname);
    if (this.targetUser.indexOf("space-")===-1 && this.targetUser.indexOf("team-")===-1)
    {
      $(".target-avatar-link").attr("href", "/portal/intranet/profile/"+this.targetUser);
      $(".target-avatar-image").attr("src", "/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:"+this.targetUser+"/soc:profile/soc:avatar");
    }
    else if (this.targetUser.indexOf("team-")===-1)
    {
      var spaceName = this.targetFullname.toLowerCase().replace(" ", "_");
      $(".target-avatar-link").attr("href", "/portal/g/:spaces:"+spaceName+"/"+spaceName);
      $(".target-avatar-image").attr("src", "/rest/jcr/repository/social/production/soc:providers/soc:space/soc:"+spaceName+"/soc:profile/soc:avatar");
    }
    else
    {

      $.ajax({
        url: this.jzChatGetCreator,
        data: {"room": this.targetUser,
          "user": this.username,
          "token": this.token
        },
        context: this,
        success: function(response){
          //console.log("SUCCESS::getRoom::"+response);
          var creator = response;
          if (creator === this.username) {
            $(".team-button").css("display", "block");
          }
        },
        error: function(xhr, status, error){
          //console.log("ERROR::"+xhr.responseText);
        }
      });
      $(".target-avatar-link").attr("href", "#");
      $(".target-avatar-image").attr("src", "/social-resources/skin/images/ShareImages/SpaceAvtDefault.png");
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
        this.chatEventURL = this.jzChatRead+'?room='+this.room+'&user='+this.username+'&token='+this.token;

        jzStoreParam("lastUsername"+this.username, this.targetUser, 60000);
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
      this.refreshWhoIsOnline();
    },
    error: function(xhr, status, error){
    }
  });
};

/**
 * jQuery bindings on dom elements created by Who Is Online methods
 */
ChatApplication.prototype.jQueryForUsersTemplate = function() {
  var value = jzGetParam("lastUsername"+this.username);
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

  $(".header-room").on("click", function() {
    if ($(this).hasClass("header-favorites"))
      chatApplication.showFavorites = !chatApplication.showFavorites;
    else if ($(this).hasClass("header-people"))
      chatApplication.showPeople = !chatApplication.showPeople;
    else if ($(this).hasClass("header-spaces"))
      chatApplication.showSpaces = !chatApplication.showSpaces;
    else if ($(this).hasClass("header-teams"))
      chatApplication.showTeams = !chatApplication.showTeams;

    jzStoreParam("chatShowFavorites"+chatApplication.username, chatApplication.showFavorites, 600000);
    jzStoreParam("chatShowPeople"+chatApplication.username, chatApplication.showPeople, 600000);
    jzStoreParam("chatShowSpaces"+chatApplication.username, chatApplication.showSpaces, 600000);
    jzStoreParam("chatShowTeams"+chatApplication.username, chatApplication.showTeams, 600000);

    chatApplication.showRooms(chatApplication.rooms);

  });

  $(".btn-add-team").on("click", function() {
    var $uitext = $("#team-modal-name");
    $uitext.val("");
    $uitext.attr("data-id", "---");
    $(".team-user-label").remove();
    $('#team-modal').modal({"backdrop": false});
    $uitext.focus();
  });


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
      $(".uiRightContainerArea").css("display", "block");
      $(".uiLeftContainerArea").css("display", "none");
//      $(".room-name").html(thiss.targetFullname);
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
  return ($("#NavigationPortlet").css("display")==="none");
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

ChatApplication.prototype.showAsText = function() {
  $.ajax({
    url: this.chatEventURL,
    data: {
      "isTextOnly": "true"},
    context: this,

    success: function(response){
      //console.log("SUCCESS:setStatus::"+response);
      $("#text-modal-area").html(response);
      $('#text-modal-area').on("click", function() {
        this.select();
      });
      $('#text-modal').modal({"backdrop": false});

    },
    error: function(response){
    }

  });

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


  var isSystemMessage = (msg.indexOf("/")===0 && msg.length>2) ;
  var options = {};

  if (isSystemMessage) {
    if (msg.indexOf("/me")===0) {
      msg = msg.replace("/me", this.fullname);
    } else if (msg.indexOf("/call")===0) {
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
      msg = "Call terminated";
      options.timestamp = ts;
      this.weemoExtension.setCallOwner(false);
      this.weemoExtension.setCallActive(false);
    } else if (msg.indexOf("/export")===0) {
      this.showAsText();
      document.getElementById("msg").value = '';
      return;
    } else {
      //this is not a supported system message
      document.getElementById("msg").value = '';
      return;
    }
  }

  var im = this.messages.length;
  this.messages[im] = {"user": this.username,
    "fullname": "You",
    "date": "pending",
    "message": msg,
    "options": options,
    "isSystem": isSystemMessage};
  this.showMessages();
  document.getElementById("msg").value = '';

  $.ajax({
    url: this.jzChatSend,
    data: {"user": this.username,
      "targetUser": this.targetUser,
      "room": this.room,
      "message": msg,
      "options": JSON.stringify(options),
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
  var $uiPanel = $(panel);
  $uiPanel.width($('#chat-application').width()+40);
  $uiPanel.height($('#chat-application').height());
  $uiPanel.css("display", "none");
  $uiPanel.html("");
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
  var about = "eXo Chat<br>";
  about += "Version 0.7.1 (build 130731)<br><br>";
  about += "Designed and Developed by <a href=\"mailto:bpaillereau@exoplatform.com\">Benjamin Paillereau</a><br>";
  about += "for <a href=\"http://www.exoplatform.com\" target=\"_new\">eXo Platform</a><br><br>";
  about += "Sources available on <a href=\"https://github.com/exo-addons/chat-application\" target=\"_new\">https://github.com/exo-addons/chat-application</a>";
  about += "<br><br><a href=\"#\" id=\"about-close-btn\" >Close</a>";
  this.hidePanels();
  var $chatAboutPanel = $(".chat-about-panel");
  $chatAboutPanel.html(about);
  $chatAboutPanel.width($('#chat-application').width()+40);
  $chatAboutPanel.height($('#chat-application').height());
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
