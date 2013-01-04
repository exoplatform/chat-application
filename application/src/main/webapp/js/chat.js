var highlight = "";
var ANONIM_USER = "__anonim_";

$(document).ready(function(){

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

  $(".chatstatus-chat").on("click", function() {
    var $chatStatusPanel = $(".chatStatusPanel");
    if ($chatStatusPanel.css("display")==="none")
      $chatStatusPanel.css("display", "inline-block");
    else
      $chatStatusPanel.css("display", "none");
  });

  $("div.chatMenu").click(function(){
    var status = $(this).attr("status");
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
        $(".chatStatusPanel").css('display', 'none');
      },
      error: function(response){
        changeStatusChat("offline");
      }

    });

  });

  $(".msgEmoticons").on("click", function() {
    var $msgEmoticonsPanel = $(".msgEmoticonsPanel");
    if ($msgEmoticonsPanel.css("display")==="none")
      $msgEmoticonsPanel.css("display", "inline-block");
    else
      $msgEmoticonsPanel.css("display", "none");
  });

  $(".smileyBtn").on("click", function() {
    var sml = $(this).attr("data");
    $(".msgEmoticonsPanel").css("display", "none");
    $msg = $('#msg');
    var val = $msg.val();
    if (val.charAt(val.length-1)!==' ') val +=" ";
    val += sml + " ";
    $msg.val(val);
    $msg.focusEnd();

  });

  $(".msgHelp").on("click", function() {
    showHelpPanel();
  });

  $(".chatHelpPanel").on("click", function() {
    hidePanel(".chatHelpPanel");
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

  $('#chatSearch').keyup(function(event) {
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



});

function hidePanel(panel) {
  $(panel).css("display", "none");
  $(panel).html("");
}

function hidePanels() {
  hidePanel(".chatSyncPanel");
  hidePanel(".chatErrorPanel");
  hidePanel(".chatLoginPanel");
  hidePanel(".chatAboutPanel");
  hidePanel(".chatDemoPanel");
}

function showSyncPanel() {
  if (!isLoaded) {
  hidePanels();
  var $chatSyncPanel = $(".chatSyncPanel");
  $chatSyncPanel.html("<img src=\"/chat/img/sync.gif\" width=\"64px\" class=\"chatSync\" />");
  $chatSyncPanel.css("display", "block");
  }
}

function showErrorPanel() {
  hidePanels();
  console.log("showErrorPanel");
  var $chatErrorPanel = $(".chatErrorPanel");
  $chatErrorPanel.html("Service Not Available.<br/><br/>Please, come back later.");
  $chatErrorPanel.css("display", "block");
}

function showLoginPanel() {
  hidePanels();
  console.log("showLoginPanel");
  var $chatLoginPanel = $(".chatLoginPanel");
  $chatLoginPanel.html("You must be logged in to use the Chat.<br><br><a href=\"#\" onclick=\"javascript:reloadWindow();\">Click here to reload</a>");
  $chatLoginPanel.css("display", "block");
}

function showDemoPanel() {
  hidePanels();
  console.log("showDemoPanel");
  var $chatDemoPanel = $(".chatDemoPanel");
  $chatDemoPanel.html("Welcome in the Demo mode.<br><br><div style='text-align:right;width:80%;'>" +
          "<br><br>Display Name : <input type='text' id='anonimName'>" +
          "<br><br>Email : <input type='text' id='anonimEmail'></div>" +
          "<br><a href='#' id='anonimSave'>Save Your Profile</a>");
  $chatDemoPanel.css("display", "block");

  $("#anonimSave").on("click", function() {
    var fullname = $("#anonimName").val();
    var email = $("#anonimEmail").val();
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
  about += "Version 0.4-SNAPSHOT<br><br>";
  about += "Designed and Developed by <a href=\"mailto:bpaillereau@gmail.com\">Benjamin Paillereau</a><br>";
  about += "Sources available on <a href=\"https://github.com/exo-addons/chat-application\" target=\"_new\">https://github.com/exo-addons/chat-application</a>";
  about += "<br><br><a href=\"#\" onclick=\"javascript:closeAbout();\">Close</a>";
  hidePanels();
  var $chatAboutPanel = $(".chatAboutPanel");
  $chatAboutPanel.html(about);
  $chatAboutPanel.css("display", "block");
}

function showHelpPanel() {
  hidePanels();
  $(".chatHelpPanel").css("display", "block");
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
      hidePanel(".chatErrorPanel");
      hidePanel(".chatSyncPanel");
      $("#whoisonline").html(response);
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
      console.log("whoisonline :: "+textStatus+" :: "+errorThrown);
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

  $("#users-online-"+targetUser).addClass("info");


  $('.users-online').on("click", function() {
    targetUser = $(".user-link:first",this).attr("user-data");
    loadRoom();
  });


  $('.user-link').on("click", function() {
    targetUser = $(this).attr("user-data");
    loadRoom();
  });

  $('.user-status').on("click", function(e) {
    var targetFav = $(this).attr("user-data");
    toggleFavorite(targetFav);
  });

}

function errorOnRefresh() {
  isLoaded = true;
  hidePanel(".chatSyncPanel");
  $("#whoisonline").html("<span>&nbsp;</span>");
  hidePanel(".chatLoginPanel");
  changeStatusChat("offline");
  showErrorPanel();
}

function setStatus(status) {
  $.ajax({
    url: "http://localhost:8888/chatServer/setStatus",
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

  var $chatStatus = $("span.chatstatus");
  $chatStatus.removeClass("chatstatus-available-black");
  $chatStatus.removeClass("chatstatus-donotdisturb-black");
  $chatStatus.removeClass("chatstatus-invisible-black");
  $chatStatus.removeClass("chatstatus-away-black");
  $chatStatus.removeClass("chatstatus-offline-black");
  $chatStatus.addClass("chatstatus-"+status+"-black");

  var $chatStatusChat = $("span.chatstatus-chat");
  $chatStatusChat.removeClass("chatstatus-available");
  $chatStatusChat.removeClass("chatstatus-donotdisturb");
  $chatStatusChat.removeClass("chatstatus-invisible");
  $chatStatusChat.removeClass("chatstatus-away");
  $chatStatusChat.removeClass("chatstatus-offline");
  $chatStatusChat.addClass("chatstatus-"+status);

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
        out += "<span class='invisibleText'>- </span><a href='/portal/intranet/profile/"+message.user+"' class='userLink' target='_new'>"+message.fullname+"</a><span class='invisibleText'> : </span><br/>";
      }
      else
      {
        out += "<hr style='margin:0px;'>";
      }
      out += "<div style='margin-left:40px;'><span style='float:left'>"+messageBeautifier(message.message)+"</span>" +
              "<span class='invisibleText'> [</span>"+
              "<span style='float:right;color:#CCC;font-size:10px'>"+message.date+"</span>" +
              "<span class='invisibleText'>]</span></div>"+
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
      $(".rightchat").css("display", "block");
      hidePanel(".chatLoginPanel");
      hidePanel(".chatErrorPanel");
    })
    .error(function() {
      $(".rightchat").css("display", "none");
      if ( $(".chatErrorPanel").css("display") == "none") {
        showLoginPanel();
      } else {
        hidePanel(".chatLoginPanel");
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
  $("#users-online-"+targetUser).addClass("info");

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
      $msg.focus();
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
  hidePanel('.chatAboutPanel');
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
          w = "<span class='smiley smileySmile'><span class='smileyText'>:)</span></span>";
        } else if (w == ":-D" || w==":D") {
          w = "<span class='smiley smileyBigSmile'><span class='smileyText'>:D</span></span>";
        } else if (w == ":-|" || w==":|") {
          w = "<span class='smiley smileyNoVoice'><span class='smileyText'>:|</span></span>";
        } else if (w == ":-(" || w==":(") {
          w = "<span class='smiley smileySad'><span class='smileyText'>:(</span></span>";
        } else if (w == ";-)" || w==";)") {
          w = "<span class='smiley smileyEyeBlink'><span class='smileyText'>;)</span></span>";
        } else if (w == ":-O" || w==":O") {
          w = "<span class='smiley smileySurprise'><span class='smileyText'>:O</span></span>";
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