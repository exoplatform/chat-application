$(document).ready(function(){

  notifEventURL = jzNotification+'?user='+username+'&sessionId='+sessionId;
  notifEventInt = window.clearInterval(notifEventInt);
  notifEventInt = setInterval(refreshNotif, 3000);
  refreshNotif();

  function refreshNotif() {
    if ( ! $("span.chatstatus").hasClass("chatstatus-offline-black") ) {
      $.getJSON(notifEventURL, function(data) {
        if(oldNotifTotal!=data.total){
          var total = data.total;
          console.log('Notif :: '+total);
          if (total>0) {
            $("#chatnotification").html('<span class="notiftotal">'+total+'</span>');
            $("#chatnotification").css('display', 'block');
          } else {
            $("#chatnotification").html('<span></span>');
            $("#chatnotification").css('display', 'none');
          }
          oldNotifTotal = data.total;
        }

      })
      .error(function() {
        changeStatus("offline");
        $("#chatnotification").html('<span></span>');
        $("#chatnotification").css('display', 'none');
        oldNotifTotal = -1;
      });
     } else {
        $("#chatnotification").html('<span></span>');
        $("#chatnotification").css('display', 'none');
        oldNotifTotal = -1;
     }
  }

  notifStatusInt = window.clearInterval(notifStatusInt);
  notifStatusInt = setInterval(refreshStatus, 60000);
  refreshStatus();

  function refreshStatus() {
    $.ajax({
      url: jzGetStatus,
      data: {
        "user": username,
        "sessionId": sessionId
      },
      success: function(response){
        changeStatus(response);
      },
      error: function(response){
        changeStatus("offline");
      }
    });
  }


  $("#chatnotification").click(function(){
    window.location.href = "/portal/default/chat"
  });

  $("a.popupchat").click(function(){
     $(".MenuItemContainer").css('display', 'none');
     window.open("/portal/default/chat?noadminbar=true","chat-popup","menubar=no, status=no, scrollbars=no, titlebar=no, resizable=no, location=no, width=565, height=625");
  });

  $("a.chatstatus").click(function(){
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
        changeStatus(response);
        $(".MenuItemContainer").css('display', 'none');
      },
      error: function(response){
        changeStatus("offline");
      }

    });

  });

  $("#chatstatus").mouseenter(function(){
    $(".MenuItemContainer:first",this).css('display', 'block');
  }).mouseleave(function(){
    $(".MenuItemContainer:first",this).css('display', 'none');
  });

});

function changeStatus(status) {
  $("span.chatstatus").removeClass("chatstatus-available-black");
  $("span.chatstatus").removeClass("chatstatus-donotdisturb-black");
  $("span.chatstatus").removeClass("chatstatus-invisible-black");
  $("span.chatstatus").removeClass("chatstatus-away-black");
  $("span.chatstatus").removeClass("chatstatus-offline-black");
  $("span.chatstatus").addClass("chatstatus-"+status+"-black");

}