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

  $("#chatnotification").click(function(){
    window.location.href = "/portal/default/chat"
  });

});