$(document).ready(function(){

  $.ajax({
    url: jzLastReadNotificationTimestamp,
    data: {"user": username},

    success:function(response){
      lastRead = response;
    },

    error:function (xhr, status, error){

    }

  });

  var old = '';
  var notifEventSource = new EventSource(jzNotification+'?user='+username);

  notifEventSource.onmessage = function(e){
    //console.log("notifEventSource::onmessage::"+e.data);
    if(old!=e.data){
      var obj = $.parseJSON(e.data);
      var newts = obj.last;
      var newlr = obj.lastRead;
      var total = obj.total;
      console.log(newts+"::"+newlr+"::"+total);
      if (newts!==newlr) {
        $("#chatnotification").html('<span>'+total+'</span>');
        $("#chatnotification").css('display', 'block');
      } else {
        $("#chatnotification").html('<span></span>');
        $("#chatnotification").css('display', 'none');
      }
      old = e.data;
    }
  };

  $("#chatnotification").click(function(){
    $.ajax({
      url: jzReadNotification,
      data: {"user": username},

      success:function(response){
        window.location.href = "/portal/intranet/chat"
      },

      error:function (xhr, status, error){

      }

    });

  });

});