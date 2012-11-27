$(document).ready(function(){



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
    if (filter == "aboutme" || filter == "about me") {
      $('.chatAboutPanel').css("display", "inline");
    }
    userFilter = filter;
    filterInt = clearTimeout(filterInt);
    filterInt = setTimeout(refreshWhoIsOnline, 500);
  });

  chatOnlineInt = clearInterval(chatOnlineInt);
  chatOnlineInt = setInterval(refreshWhoIsOnline, 3000);
  refreshWhoIsOnline();

  setTimeout(showSyncPanel, 1000);

  function strip(html)
  {
    var tmp = document.createElement("DIV");
    tmp.innerHTML = html;
    return tmp.textContent||tmp.innerText;
  }

});

function showSyncPanel() {
  if (!isLoaded)
    $(".chatSyncPanel").css("display", "block");
}

function refreshWhoIsOnline() {
  var withSpaces = $(".filter-space span:first-child").hasClass("filter-on");
  var withUsers = $(".filter-user span:first-child").hasClass("filter-on");
  $('#whoisonline').load(jzChatWhoIsOnline, {"user": username, "sessionId": sessionId,
    "filter": userFilter, "withSpaces": withSpaces, "withUsers": withUsers}, function (response, status, xhr) {
    isLoaded = true;
    $(".chatSyncPanel").css("display", "none");
    $(".leftchat").css("display", "block");
    if (status == "error") {
      $("#whoisonline").html("");
      $(".chatErrorPanel").css("display", "inline");
      $(".chatLoginPanel").css("display", "none");
    } else {
      $(".chatErrorPanel").css("display", "none");
    }
  });
}


function showMessages(msgs) {
  var im, message, out="", prevUser="";
  if (msgs!==undefined) {
    messages = msgs;
  }

  if (messages.length==0) {
    out = "<div class='msgln' style='padding:20px 0px;'><b><center>No messages yet.</center></b></div>";
  } else {
    for (im=0 ; im<messages.length ; im++) {
      message = messages[im];

      if (prevUser != message.user)
      {
        if (prevUser != "")
          out += "</div>";
        if (message.user != username)
          out += "<div class='msgln-odd'><b>";
        else
          out += "<div class='msgln'><b>";
        out += "<span class='invisibleText'>- </span><a href='/portal/intranet/profile/"+message.user+"' class='userLink' target='_new'>"+message.fullname+"</a><span class='invisibleText'> : </span>";
        out += "</b><br/>";
      }
      else
      {
        out += "<hr style='margin:0px;'>";
      }
      out += "<div><span style='float:left'>"+messageBeautifier(message.message)+"</span>" +
              "<span class='invisibleText'> [</span>"+
              "<span style='float:right;color:#CCC;font-size:10px'>"+message.date+"</span>" +
              "<span class='invisibleText'>]</span></div>"+
              "<div style='clear:both;'></div>";
      prevUser = message.user;
    }
  }
  $("#chats").html('<span>'+out+'</span>');
  sh_highlightDocument();
  $("#chats").animate({ scrollTop: 20000 }, 'fast');

}

function refreshChat() {
    $.getJSON(chatEventURL, function(data) {
      var lastTS = jzGetParam("lastTS");
      //console.log("chatEvent :: lastTS="+lastTS+" :: serverTS="+data.timestamp);
      var im, message, out="", prevUser="";
      if (data.messages.length==0) {
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
      $(".chatLoginPanel").css("display", "none");
    })
    .error(function() {
      $(".rightchat").css("display", "none");
      if ( $(".chatErrorPanel").css("display") == "none") {
        $(".chatLoginPanel").css("display", "inline");
      } else {
        $(".chatLoginPanel").css("display", "none");
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

      chatEventURL = jzChatSend+'?room='+room+'&user='+username+'&sessionId='+sessionId+'&event=0';

      jzStoreParam("lastUser", targetUser, 60000);
      jzStoreParam("lastTS", "0");
      chatEventInt = window.clearInterval(chatEventInt);
      chatEventInt = setInterval(refreshChat, 3000);
      refreshChat();

    },

    error: function(xhr, status, error){
      console.log("ERROR::"+xhr.responseText);
    }

  });

}



function closeAbout() {
  $('.chatAboutPanel').css("display", "none");
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

removeParametersFromLocation();

function messageBeautifier(message) {
  var msg = "";
  if (message.indexOf("java:")===0) {
    msg = "<div><pre class='sh_java'>"+message.substr(5, message.length-6)+"</pre></div>";
    return msg;
  } else if (message.indexOf("js:")===0) {
    msg = "<div><pre class='sh_javascript'>"+message.substr(3, message.length-4)+"</pre></div>";
    return msg;
  } else if (message.indexOf("css:")===0) {
    msg = "<div><pre class='sh_css'>"+message.substr(4, message.length-5)+"</pre></div>";
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
}