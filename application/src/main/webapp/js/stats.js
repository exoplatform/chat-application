var console = console || {
  log:function(){},
  warn:function(){},
  error:function(){}
};

var jq171 = jQuery.noConflict(true);

(function($) {

  $(document).ready(function(){

    var $statsApplication = $("#chat-statistics");
    var chatServerURL = $statsApplication.attr("data-chat-server-url");
    var jzStatistics = chatServerURL+"/statistics";


    function getStats() {
      $.getJSON(jzStatistics, function(data){
        var html = "<div class='stats-row'><span class='stats-label'>Users</span><span class='stats-data'>"+data.users+"</span><span class='stats-sep'></span></div>";
        html += "<div class='stats-row'><span class='stats-label'>Rooms</span><span class='stats-data'>"+data.rooms+"</span><span class='stats-sep'></span></div>";
        html += "<div class='stats-row'><span class='stats-label'>Messages</span><span class='stats-data'>"+data.messages+"</span><span class='stats-sep'></span></div>";
        html += "<div class='stats-row'><span class='stats-label'>Notifications</span><span class='stats-data'>"+data.notifications+"</span><span class='stats-sep'></span></div>";
        html += "<div class='stats-row'><span class='stats-label'>Unread Notifications</span><span class='stats-data'>"+data.notificationsUnread+"</span><span class='stats-sep'></span></div>";
        $("#chat-statistics").html(html);
        //setTimeout(getStats, 1000);
      })
      .error(function(response){
        console.log("error::"+response);
        //retry in 3 sec
        //setTimeout(getStats, 3000);
      });
    }
    getStats();

  });

})(jq171);
