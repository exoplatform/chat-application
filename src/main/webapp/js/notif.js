$(document).ready(function(){

  var old = '';
  var notifEventSource = new EventSource(jzNotification+'&user='+username);

  notifEventSource.onmessage = function(e){
    //console.log("notifEventSource::onmessage");
    if(old!=e.data){
      //console.log("DATA="+e.data);
      document.getElementById("chatnotification").innerHTML='<span>'+e.data+'</span>';
      old = e.data;
    }
  };

});