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

  setInterval(refreshWhoIsOnline, 3000);
  refreshWhoIsOnline();

  function refreshWhoIsOnline() {
    $('#whoisonline').load(jzChatWhoIsOnline, {"user": username, "sessionId": sessionId}, function () { });
  }

  function strip(html)
  {
    var tmp = document.createElement("DIV");
    tmp.innerHTML = html;
    return tmp.textContent||tmp.innerText;
  }

});