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

      //document.getElementById("chats").innerHTML+=strip('<div class="msgln"><b>'+username+'</b>: '+msg+'<br/></div>');
      //$("#chats").animate({ scrollTop: 2000 }, 'normal');


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
        },

        error:function (xhr, status, error){

        }

      });
    }

  });

  /*
  chatEventSource = new EventSource(jzChatSend+'&room='+room);

  chatEventSource.onmessage = function(e){
    //console.log("chatEventSource::onmessage");
    if(old!=e.data){
      //console.log("DATA="+e.data);
      $("#chats").html('<span>'+e.data+'</span>');
      $("#chats").animate({ scrollTop: 2000 }, 'normal');
      old = e.data;
    }
  };
  */

  setInterval(refreshWhoIsOnline, 5000);
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