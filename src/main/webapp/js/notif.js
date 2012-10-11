$(document).ready(function(){

  var old = '';
  var notifEventSource = new EventSource(jzNotification+'&user='+username);

  notifEventSource.onmessage = function(e){
    console.log("notifEventSource::onmessage");
    if(old!=e.data){
      var newts = e.data;
      if (newts!==lastRead) {
        document.getElementById("chatnotification").innerHTML='<span>NEW</span>';
      } else {
        document.getElementById("chatnotification").innerHTML='<span>OLD</span>';
      }
      old = e.data;
    }
  };

});