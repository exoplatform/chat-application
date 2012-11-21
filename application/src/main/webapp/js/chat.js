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


  $('#msg').keypress(function(event) {
    var msg = $(this).attr("value");
    if ( event.which == 13 ) {
      //console.log("sendMsg=>"+username + " : " + room + " : "+msg);
      if(!msg)
      {
        return;
      }

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
          document.getElementById("msg").value = '';
          refreshChat();
        },

        error:function (xhr, status, error){

        }

      });
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
    if (filter == "aboutme") {
      $('.chatAboutPanel').css("display", "inline");
    }
    userFilter = filter;
    refreshWhoIsOnline();
  });

  chatOnlineInt = clearInterval(chatOnlineInt);
  chatOnlineInt = setInterval(refreshWhoIsOnline, 3000);
  refreshWhoIsOnline();


  function strip(html)
  {
    var tmp = document.createElement("DIV");
    tmp.innerHTML = html;
    return tmp.textContent||tmp.innerText;
  }

});

function refreshWhoIsOnline() {
  var withSpaces = $(".filter-space span:first-child").hasClass("filter-on");
  var withUsers = $(".filter-user span:first-child").hasClass("filter-on");
  $('#whoisonline').load(jzChatWhoIsOnline, {"user": username, "sessionId": sessionId,
    "filter": userFilter, "withSpaces": withSpaces, "withUsers": withUsers}, function (response, status, xhr) {
    if (status == "error") {
      $("#whoisonline").html("");
      $(".chatErrorPanel").css("display", "inline");
      $(".chatLoginPanel").css("display", "none");
    } else {
      $(".chatErrorPanel").css("display", "none");
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
  var tab = message.split(" ");
  var it,w;
  for (it=0 ; it<tab.length ; it++) {
    w = tab[it];
    if (w.indexOf("/")>-1) {
      w = "<a href='"+w+"' target='_new'>"+w+"</a>";
    } else if (w == ":-)" || w==":)") {
      w = "<span class='smiley smileySmile'>&nbsp;&nbsp;</span>"
    } else if (w == ":-D" || w==":D") {
      w = "<span class='smiley smileyBigSmile'>&nbsp;&nbsp;</span>"
    } else if (w == ":-|" || w==":|") {
      w = "<span class='smiley smileyNoVoice'>&nbsp;&nbsp;</span>"
    } else if (w == ":-(" || w==":(") {
      w = "<span class='smiley smileySad'>&nbsp;&nbsp;</span>"
    } else if (w == ";-)" || w==";)") {
      w = "<span class='smiley smileyEyeBlink'>&nbsp;&nbsp;</span>"
    } else if (w == ":-O" || w==":O") {
      w = "<span class='smiley smileySurprise'>&nbsp;&nbsp;</span>"
    }
    msg += w+" ";
  }

  return msg;
}