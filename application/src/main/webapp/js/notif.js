var console = console || {
  log:function(){},
  warn:function(){},
  error:function(){}
};

var jq171 = jQuery.noConflict(true);
var weemo;

(function($) {

  $(document).ready(function(){

    var $notificationApplication = $("#chat-status");
    var username = $notificationApplication.attr("data-username");
    var token = $notificationApplication.attr("data-token");
    var chatPage = $notificationApplication.attr("data-chat-page");
    var chatServerURL = $notificationApplication.attr("data-chat-server-url");
    var chatIntervalNotif = $notificationApplication.attr("data-chat-interval-notif");
    var chatIntervalStatus = $notificationApplication.attr("data-chat-interval-status");
    var weemoKey = $notificationApplication.attr("data-weemo-key");
    var jzInitUserProfile = $notificationApplication.jzURL("NotificationApplication.initUserProfile");
    var jzNotification = chatServerURL+"/notification";
    var jzGetStatus = chatServerURL+"/getStatus";
    var jzSetStatus = chatServerURL+"/setStatus";
    var notifEventInt;
    var notifStatusInt;
    var notifEventURL;
    var oldNotifTotal = '';


    function initUserProfile() {
      $(".uiCompanyNavigations > li")
        .children()
        .filter(function() {
          if ($(this).attr("href") == "/portal/intranet/chat") {
            $(this).css("width", "95%");
            var html = '<i class="uiChatIcon"></i>Chat';
            html += '<span id="chat-notification" style="float: right; display: none;"></span>';
            $(this).html(html);
          }
        })

      $.getJSON(jzInitUserProfile, function(data){
        //console.log("Profile Update : "+data.msg);
        token = data.token;
        //console.log("Token : "+data.token);
        var fullname = username;
        initCall(username, fullname);

        notifEventURL = jzNotification+'?user='+username+'&token='+token;
        notifEventInt = window.clearInterval(notifEventInt);
        notifEventInt = setInterval(refreshNotif, chatIntervalNotif);
        refreshNotif();

        notifStatusInt = window.clearInterval(notifStatusInt);
        notifStatusInt = setInterval(refreshStatus, chatIntervalStatus);
        refreshStatus();
      })
      .error(function(response){
        //console.log("error::"+response);
        //retry in 3 sec
        setTimeout(initUserProfile, 3000);
      });
    }
    initUserProfile();

    function initCall($uid, $name) {
      if (weemoKey!=="") {
        $(".btn-weemo-conf").css('display', 'none');
        weemo = new Weemo(); // Creating a Weemo object instance
        weemo.setMode("debug"); // Activate debugging in browser's log console
        weemo.setEnvironment("production"); // Set environment  (development, testing, staging, production)
        weemo.setPlatform("p1.weemo.com"); // Set connection platform (by default: "p1.weemo.com")
        weemo.setDomain("weemo-poc.com"); // Chose your domain, for POC all apikey are created for "weemo-poc.com" domain
        weemo.setApikey(weemoKey); // Configure your Api Key
        weemo.setUid("weemo"+$uid); // Configure your UID

        //weemo.setDisplayname($name); // Configure the display name
        weemo.connectToWeemoDriver(); // Launches the connection between WeemoDriver and Javascript

        weemo.onConnectionHandler = function(message, code) {
          if(window.console)
            console.log("Connection Handler : " + message + ' ' + code);
          switch(message) {
            case 'connectedWeemoDriver':
              weemo.connectToTheCloud();
              break;
            case 'sipOk':
              $(".btn-weemo").removeClass('disabled');
              var fn = $(".label-user").text();
              if (fn!=="") {
                weemo.setDisplayname(fn); // Configure the display name
              }
              break;
          }
        }

        weemo.onWeemoDriverNotStarted = function(downloadUrl) {
          modal = new Modal('WeemoDriver download', 'Click <a href="'+downloadUrl+'">here</a> to download.');
          modal.show();
        };

      } else {
        $(".btn-weemo").css('display', 'none');
      }
    }


    function refreshNotif() {
      if ( ! $("span.chat-status").hasClass("chat-status-offline-black") ) {
        $.getJSON(notifEventURL, function(data) {
          if(oldNotifTotal!=data.total){
            var total = data.total;
            //console.log('Notif :: '+total);
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
          "token": token
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
       window.open(chatPage+"?noadminbar=true","_blank","menubar=no, status=no, scrollbars=no, titlebar=no, resizable=no, location=no, width=536, height=647");
    });

    $("a.chat-status").click(function(){
      var status = $(this).attr("status");
      //console.log("setStatus :: "+status);

      $.ajax({
        url: jzSetStatus,
        data: { "user": username,
                "token": token,
                "status": status
                },

        success: function(response){
          //console.log("SUCCESS:setStatus::"+response);
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
