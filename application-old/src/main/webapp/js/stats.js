(function ($) {

  $(document).ready(function () {

    var $statsApplication = $("#chat-statistics");
    var chatServerURL = $statsApplication.attr("data-chat-server-url");
    var jzStatistics = chatServerURL + "/statistics";


    function getStats() {
      $.getJSON(jzStatistics, function (data) {
        var html = "<div class='stats-row'><span class='stats-label'>" + chatBundleData["exoplatform.stats.users"] + "</span><span class='stats-data'>" + data.users + "</span><span class='stats-sep'></span></div>";
        html += "<div class='stats-row'><span class='stats-label'>" + chatBundleData["exoplatform.stats.rooms"] + "</span><span class='stats-data'>" + data.rooms + "</span><span class='stats-sep'></span></div>";
        html += "<div class='stats-row'><span class='stats-label'>" + chatBundleData["exoplatform.stats.messages"] + "</span><span class='stats-data'>" + data.messages + "</span><span class='stats-sep'></span></div>";
        html += "<div class='stats-row'><span class='stats-label'>" + chatBundleData["exoplatform.stats.notifications"] + "</span><span class='stats-data'>" + data.notifications + "</span><span class='stats-sep'></span></div>";
        html += "<div class='stats-row'><span class='stats-label'>" + chatBundleData["exoplatform.stats.unread.notifications"] + "</span><span class='stats-data'>" + data.notificationsUnread + "</span><span class='stats-sep'></span></div>";
        $("#chat-statistics").html(html);
        //setTimeout(getStats, 1000);
      })
        .error(function (response) {
          console.log("error::" + response);
          //retry in 3 sec
          //setTimeout(getStats, 3000);
        });
    }

    getStats();

  });

})($);