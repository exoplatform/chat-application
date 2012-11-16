$(document).ready(function(){

  var old = '';

  notifEventURL = jzNotification+'?user='+username+'&sessionId='+sessionId;
  console.log(notifEventURL);
  notifEventInt = window.clearInterval(notifEventInt);
  notifEventInt = setInterval(refreshNotif, 3000);
  refreshNotif();

  function refreshNotif() {
    $.getJSON(notifEventURL, function(data) {
      if(old!=data){
        var total = data.total;
        console.log(total);
        if (total>0) {
          $("#chatnotification").html('<span>'+total+'</span>');
          $("#chatnotification").css('display', 'block');
        } else {
          $("#chatnotification").html('<span></span>');
          $("#chatnotification").css('display', 'none');
        }
        old = data;
      }

    });
  }

  $("#chatnotification").click(function(){
    window.location.href = "/portal/default/chat"
  });

});