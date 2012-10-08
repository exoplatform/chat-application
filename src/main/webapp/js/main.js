$(document).ready(function(){

  var username = '<%=user%>';
  var jzChatSend = "@{send()}";

  function sendMsg(){

    var msg = document.getElementById("msg").value;
    if(!msg)
    {
      return;
    }

    document.getElementById("chats").innerHTML+=strip('<div class="msgln"><b>'+username+'</b>: '+msg+'<br/></div>');
    $("#chats").animate({ scrollTop: 2000 }, 'normal');

    $.ajax({
      url: jzChatSend,
      data: {"user": username,
             "room": "<%=room%>",
             "message": msg,
            },

      success:function(response){
        console.log(success);
        document.getElementById("msg").value = '';
      },

      error:function (xhr, status, error){

      }

    });

  }

  var old = '';
  var source = new EventSource(jzChatSend+'&room=<%=room%>');

  source.onmessage = function(e)
  {
    if(old!=e.data){
      document.getElementById("chats").innerHTML='<span>'+e.data+'</span>';
      old = e.data;
    }
  };

  function strip(html)
  {
    var tmp = document.createElement("DIV");
    tmp.innerHTML = html;
    return tmp.textContent||tmp.innerText;
  }

});