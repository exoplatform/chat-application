$(document).ready(function(){

  notifEventURL = jzNotification+'?user='+username+'&sessionId='+sessionId;
  notifEventInt = window.clearInterval(notifEventInt);
  notifEventInt = setInterval(refreshNotif, 3000);
  refreshNotif();

  function refreshNotif() {
    $.getJSON(notifEventURL, function(data) {
      if(oldNotifTotal!=data.total){
        var total = data.total;
        console.log('Notif :: '+total);
        if (total>0) {
          $("#chatnotification").html('<span>'+total+'</span>');
          $("#chatnotification").css('display', 'block');
        } else {
          $("#chatnotification").html('<span></span>');
          $("#chatnotification").css('display', 'none');
        }
        oldNotifTotal = data.total;
      }

    });
  }

  $.ajax({
    url: jzGetStatus,
    data: { "user": username,
            "sessionId": sessionId
            },

    success: function(response){
      console.log("getStatus::"+response);
      $("span.chatstatus").addClass("chatstatus-"+response+"-black");
    }
  });

  $("#chatnotification").click(function(){
    window.location.href = "/portal/default/chat"
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
        $("span.chatstatus").removeClass("chatstatus-available-black");
        $("span.chatstatus").removeClass("chatstatus-donotdisturb-black");
        $("span.chatstatus").removeClass("chatstatus-invisible-black");
        $("span.chatstatus").removeClass("chatstatus-away-black");
        $("span.chatstatus").addClass("chatstatus-"+response+"-black");
      }
    });



    //$(".MenuItemContainer:first",this).css('display', 'none');
  });

  $("#chatstatus").mouseenter(function(){
    $(".MenuItemContainer:first",this).css('display', 'block');
  }).mouseleave(function(){
    $(".MenuItemContainer:first",this).css('display', 'none');
  });

});