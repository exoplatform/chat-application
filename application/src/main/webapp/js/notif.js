$(document).ready(function(){

  var old = '';
  var notifEventSource = new EventSource(jzNotification+'?user='+username+'&sessionId='+sessionId);

  notifEventSource.onmessage = function(e){
    //console.log("notifEventSource::onmessage::"+e.data);
    if(old!=e.data){
      var obj = $.parseJSON(e.data);
      var total = obj.total;
      console.log(total);
      if (total>0) {
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
    window.location.href = "/portal/default/chat"
  });

});