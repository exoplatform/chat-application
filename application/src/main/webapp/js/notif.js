var jq171 = jQuery.noConflict(true);

(function($) {

  $(document).ready(function(){

    var $notificationApplication = $("#chat-status");
    var username = $notificationApplication.attr("data-username");
    var sessionId = $notificationApplication.attr("data-session-id");
    var chatPage = $notificationApplication.attr("data-chat-page");
    var chatServerURL = $notificationApplication.attr("data-chat-server-url");
    var jzInitUserProfile = $notificationApplication.jzURL("NotificationApplication.initUserProfile");
    var jzNotification = chatServerURL+"/notification";
    var jzGetStatus = chatServerURL+"/getStatus";
    var jzSetStatus = chatServerURL+"/setStatus";
    var notifEventInt;
    var notifStatusInt;
    var notifEventURL;
    var oldNotifTotal = '';


    function initUserProfile() {
      $.ajax({
        url: jzInitUserProfile,
        success: function(response){
          console.log("Profile Update : "+response);

          notifEventURL = jzNotification+'?user='+username+'&sessionId='+sessionId;
          notifEventInt = window.clearInterval(notifEventInt);
          notifEventInt = setInterval(refreshNotif, 3000);
          refreshNotif();

          notifStatusInt = window.clearInterval(notifStatusInt);
          notifStatusInt = setInterval(refreshStatus, 60000);
          refreshStatus();

        },
        error: function(response){
          //retry in 3 sec
          setTimeout(initUserProfile, 3000);
        }
      });
    }
    initUserProfile();

    function refreshNotif() {
      if ( ! $("span.chat-status").hasClass("chat-status-offline-black") ) {
        $.getJSON(notifEventURL, function(data) {
          if(oldNotifTotal!=data.total){
            var total = data.total;
            console.log('Notif :: '+total);
            var $chatNotification = $("#chat-notification");
            if (total>0) {
              $chatNotification.html('<span class="notif-total">'+total+'</span>');
              $chatNotification.css('display', 'block');
            } else {
              $chatNotification.html('<span></span>');
              $chatNotification.css('display', 'none');
            }
            oldNotifTotal = data.total;
          }

        })
        .error(function() {
          var $chatNotification = $("#chat-notification");
          changeStatus("offline");
          $chatNotification.html('<span></span>');
          $chatNotification.css('display', 'none');
          oldNotifTotal = -1;
        });
       } else {
          var $chatNotification = $("#chat-notification");
          $chatNotification.html('<span></span>');
          $chatNotification.css('display', 'none');
          oldNotifTotal = -1;
       }
    }


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


    $("#chat-notification").click(function(){
      window.location.href = chatPage;
    });

    $("a.popup-chat").click(function(){
       $(".MenuItemContainer").css('display', 'none');
       window.open(chatPage+"?noadminbar=true","chat-popup","menubar=no, status=no, scrollbars=no, titlebar=no, resizable=no, location=no, width=536, height=647");
    });

    $("a.chat-status").click(function(){
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

    $("#chat-status").mouseenter(function(){
      $(".MenuItemContainer:first",this).css('display', 'block');
    }).mouseleave(function(){
      $(".MenuItemContainer:first",this).css('display', 'none');
    });

    function changeStatus(status) {
      var $spanStatus = $("span.chat-status");
      $spanStatus.removeClass("chat-status-available-black");
      $spanStatus.removeClass("chat-status-donotdisturb-black");
      $spanStatus.removeClass("chat-status-invisible-black");
      $spanStatus.removeClass("chat-status-away-black");
      $spanStatus.removeClass("chat-status-offline-black");
      $spanStatus.addClass("chat-status-"+status+"-black");
      var $spanStatusChat = $("span.chat-status-chat");
      $spanStatusChat.removeClass("chat-status-available");
      $spanStatusChat.removeClass("chat-status-donotdisturb");
      $spanStatusChat.removeClass("chat-status-invisible");
      $spanStatusChat.removeClass("chat-status-away");
      $spanStatusChat.removeClass("chat-status-offline");
      $spanStatusChat.addClass("chat-status-"+status);
    }


  });

})(jq171);
