
var chatApplication = new ChatApplication();

(function($){

  $(document).ready(function(){

    /**
     * Init Chat
     */
    var $chatApplication = $("#chat-application");

    chatApplication.username = $chatApplication.attr("data-username");
    chatApplication.token = $chatApplication.attr("data-token");
    var chatServerURL = $chatApplication.attr("data-chat-server-url");
    chatApplication.chatIntervalChat = $chatApplication.attr("data-chat-interval-chat");
    chatApplication.chatIntervalSession = $chatApplication.attr("data-chat-interval-session");
    chatApplication.chatIntervalStatus = $chatApplication.attr("data-chat-interval-status");
    chatApplication.chatIntervalUsers = $chatApplication.attr("data-chat-interval-users");
    chatApplication.plfUserStatusUpdateUrl = $chatApplication.attr("data-plf-user-status-update-url");

    chatApplication.publicModeEnabled = $chatApplication.attr("data-public-mode-enabled");
    chatApplication.dbName = $chatApplication.attr("data-db-name");
    var chatPublicMode = ($chatApplication.attr("data-public-mode")=="true");
    var chatView = $chatApplication.attr("data-view");
    chatApplication.chatFullscreen = $chatApplication.attr("data-fullscreen");
    chatApplication.portalURI = $chatApplication.attr("data-portal-uri");
    chatApplication.isPublic = (chatPublicMode == "true" && chatView == "public");
    chatApplication.jzInitChatProfile = $chatApplication.jzURL("ChatApplication.initChatProfile");
    chatApplication.jzCreateDemoUser = $chatApplication.jzURL("ChatApplication.createDemoUser");
    chatApplication.jzMaintainSession = $chatApplication.jzURL("ChatApplication.maintainSession");
    chatApplication.jzUpload = $chatApplication.jzURL("ChatApplication.upload");
    chatApplication.jzCreateTask = $chatApplication.jzURL("ChatApplication.createTask");
    chatApplication.jzCreateEvent = $chatApplication.jzURL("ChatApplication.createEvent");
    chatApplication.jzSaveWiki = $chatApplication.jzURL("ChatApplication.saveWiki");
    chatApplication.jzGetStatus = chatServerURL+"/getStatus";
    chatApplication.jzSetStatus = chatServerURL+"/setStatus";
    chatApplication.jzChatWhoIsOnline = chatServerURL+"/whoIsOnline";
    chatApplication.jzChatSend = chatServerURL+"/send";
    chatApplication.jzChatRead = chatServerURL+"/read";
    chatApplication.jzChatSendMeetingNotes = chatServerURL+"/sendMeetingNotes";
    chatApplication.jzChatGetMeetingNotes = chatServerURL+"/getMeetingNotes";
    chatApplication.jzChatGetRoom = chatServerURL+"/getRoom";
    chatApplication.jzChatGetCreator = chatServerURL+"/getCreator";
    chatApplication.jzChatToggleFavorite = chatServerURL+"/toggleFavorite";
    chatApplication.jzChatIsFavorite = chatServerURL+"/isFavorite";
    chatApplication.jzChatSetPreferredNotification = chatServerURL+"/setPreferredNotification";
    chatApplication.jzChatSetNotificationTrigger = chatServerURL+"/setNotificationTrigger";
    chatApplication.jzChatSetRoomNotificationTrigger = chatServerURL + "/setRoomNotificationTrigger";
    chatApplication.jzChatGetUserDesktopNotificationSettings = chatServerURL+"/getUserDesktopNotificationSettings";
    chatApplication.jzChatUpdateUnreadMessages = chatServerURL+"/updateUnreadMessages";
    chatApplication.jzUsers = chatServerURL+"/users";
    chatApplication.jzDelete = chatServerURL+"/delete";
    chatApplication.jzDeleteTeamRoom = chatServerURL+"/deleteTeamRoom";
    chatApplication.jzEdit = chatServerURL+"/edit";
    chatApplication.jzSaveTeamRoom = chatServerURL+"/saveTeamRoom";
    chatApplication.room = "";

    chatApplication.initChat();
    chatApplication.initChatProfile();

    // Attach weemo call button into chatApplication
    chatApplication.displayVideoCallOnChatApp();

    /**
     * Init Global Variables
     *
     */
    //needed for #chat text area
    chatApplication.keydown = -1;
    //needed for #edit-modal-text area
    var keydownModal = -1;
    //needed for Fluid Integration
    var labelAvailable = $chatApplication.attr("data-label-available");
    var labelAway = $chatApplication.attr("data-label-away");
    var labelDoNotDisturb = $chatApplication.attr("data-label-donotdisturb");
    var labelInvisible = $chatApplication.attr("data-label-invisible");


    /**
     ##################                           ##################
     ##################                           ##################
     ##################   JQUERY UI EVENTS        ##################
     ##################                           ##################
     ##################                           ##################
     */

    $("#PlatformAdminToolbarContainer").addClass("no-user-selection");



    $.fn.setCursorPosition = function(position){
      if(this.length === 0) return this;
      return $(this).setSelection(position, position);
    };

    $.fn.setSelection = function(selectionStart, selectionEnd) {
      if(this.length === 0) return this;
      input = this[0];

      if (input.createTextRange) {
        var range = input.createTextRange();
        range.collapse(true);
        range.moveEnd('character', selectionEnd);
        range.moveStart('character', selectionStart);
        range.select();
      } else if (input.setSelectionRange) {
        input.focus();
        input.setSelectionRange(selectionStart, selectionEnd);
      }

      return this;
    };

    $.fn.focusEnd = function(){
      this.setCursorPosition(this.val().length);
      return this;
    };

    $(window).unload(function() {
      chatApplication.hidePanels();
    });

    $('#msg').focus(function() {
  //    console.log("focus on msg : "+chatApplication.targetUser+":"+chatApplication.room);
       var chatheight = document.getElementById("chats");
       chatheight.scrollTop = chatheight.scrollHeight;
       chatApplication.updateUnreadMessages();
       chatheight.scrollHeight;
    });

    $('#msg').keydown(function(event) {
      //prevent the default behavior of the enter button
      if ( event.which == 13 ) {
        event.preventDefault();
      }
      //adding (shift or ctl or alt) + enter for adding carriage return in a specific cursor
      if ( event.keyCode == 13 && (event.shiftKey||event.ctrlKey||event.altKey) ) {
        this.value = this.value.substring(0, this.selectionStart)+"\n"+this.value.substring(this.selectionEnd,this.value.length);
        var textarea =  $('#msg');
        $('#msg').scrollTop(textarea[0].scrollHeight - textarea.height());
      }
  //    console.log("keydown : "+ event.which+" ; "+keydown);
      if ( event.which == 18 ) {
        chatApplication.keydown = 18;
      }
    });

    $('#msg').keyup(function(event) {
      var msg = $(this).val();
      if ( event.which === 13 && msg.trim().length>=1) {
        if ( !msg || event.keyCode == 13 && (event.shiftKey||event.ctrlKey||event.altKey) ) {
          return false;
        }
        chatApplication.sendMessage(msg);

      }
      // UP Arrow
      if (event.which === 38 && msg.length === 0) {
        var $uimsg = chatApplication.chatRoom.getUserLastMessage();
        var $uimsgdata = $uimsg.find(".msg-data");
        if ($uimsgdata.length === 1) {
          chatApplication.openEditMessagePopup($uimsgdata.attr("data-id"), $uimsgdata.html());
        }
      }

      if ( chatApplication.keydown === 18 ) {
        chatApplication.keydown = -1;
      }
    });

    $(document).on('click.meeting-action-toggle', function(e) {
      if ($(e.target).closest('.meeting-actions').length == 0 && $('.meeting-action-popup').css('display') == 'none') {
        $('.meeting-action-toggle').removeClass('active');
      }
    });


    $("#submit").on("click", function() {

            var msg = $("#msg").val();
            chatApplication.sendMessage(msg);
            $("#msg").val("");
            $("#msg, #msg_editable").focus();

    });



    $(".meeting-action-toggle").on("click", function() {
      if ($(this).hasClass("disabled")) return;

      if ($('.meeting-action-popup').css('display') == 'none') {
        $(this).toggleClass('active');
      }
      $(".meeting-action-popup").hide();
    });

    $(".meeting-action-link").on("click", function() {
      var toggleClass = $(this).attr("data-toggle");

      if (toggleClass === "meeting-action-flag-panel") return;

      $(".meeting-action-panel").hide();
      $(".input-with-value").each(function() {
        $(this).val($(this).attr("data-value"));
        $(this).addClass("input-default");
      });
      var $toggle = $("."+toggleClass);
      var pheight = $toggle.attr("data-height");
      var ptitle = $toggle.attr("data-title");

      var $popup = $(".meeting-action-popup");
      $popup.css("height", pheight+"px");
      $popup.css("top", (-Math.abs(pheight)-4)+"px");
      $toggle.show();
      $(".meeting-action-title").html(ptitle);
      $popup.show();
      if (toggleClass === "meeting-action-event-panel") {
        if (chatApplication.targetUser.indexOf("team-") > -1) {
          chatApplication.getUsers(chatApplication.targetUser, function (users) { // Team chat room
            $("#chat-file-target-user").val(users);
          }, true);
        } else if (chatApplication.targetUser.indexOf("space-") == -1) { // 1:1 chat room
          $("#chat-file-target-user").val(chatApplication.username + "," + chatApplication.targetUser);
        }
      }

      if (toggleClass === "meeting-action-file-panel") {
        $("#chat-file-form").attr("action", chatApplication.jzUpload);
        $("#chat-file-room").val(chatApplication.room);
        $("#chat-file-target-user").val(chatApplication.targetUser);
        $("#chat-file-target-fullname").val(chatApplication.targetFullname);
        $("#chat-file-file").val("");

        chatApplication.getUsers(chatApplication.targetUser, function (users) {

          $(function(){

            var targetUser = chatApplication.targetUser;
            if (targetUser.indexOf("team-")>-1) {
              targetUser = users;
              $("#chat-file-target-user").val(targetUser);
            }
            $('#dropzone').remove();
            var dropzone = '<div class="progressBar" id="dropzone">'
                            +'<div class="progress">'
                              +'<div class="bar" style="width: 0.0%;"></div>'
                              +'<div class="label"><div class="label-inner">'+chatBundleData["exoplatform.chat.file.drop"]+'</div></div>'
                            +'</div>'
                          +'</div>';

            $('#dropzone-container').html(dropzone);
            $('#dropzone').filedrop({
//          fallback_id: 'upload_button',   // an identifier of a standard file input element
              url: chatApplication.jzUpload,              // upload handler, handles each file separately, can also be a function taking the file and returning a url
              paramname: 'userfile',          // POST parameter name used on serverside to reference file
              data: {
                room: chatApplication.room,
                targetUser: targetUser,
                targetFullname: encodeURIComponent(chatApplication.targetFullname)
              },
              error: function(err, file) {
                switch(err) {
                  case 'BrowserNotSupported':
                    alert(chatBundleData["exoplatform.chat.dnd.support"]);
                    break;
                  case 'TooManyFiles':
                    // user uploaded more than 'maxfiles'
                    break;
                  case 'FileTooLarge':
                    alert(chatBundleData["exoplatform.chat.dnd.filesize"]);
                    // program encountered a file whose size is greater than 'maxfilesize'
                    // FileTooLarge also has access to the file which was too large
                    // use file.name to reference the filename of the culprit file
                    break;
                  case 'FileTypeNotAllowed':
                  // The file type is not in the specified list 'allowedfiletypes'
                  default:
                    break;
                }
              },
              allowedfiletypes: [],   // filetypes allowed by Content-Type.  Empty array means no restrictions
              maxfiles: 1,
              maxfilesize: 100,    // max file size in MBs
              uploadStarted: function(i, file, len){
                console.log("upload started : "+i+" : "+file.name+" : "+len);
                // a file began uploading
                // i = index => 0, 1, 2, 3, 4 etc
                // file is the actual file of the index
                // len = total files user dropped
              },
              uploadFinished: function(i, file, response, time) {
                console.log("upload finished : "+i+" : "+file.name+" : "+time+" : "+response.status+" : "+response.name);
                // response is the data you got back from server in JSON format.
                var msg = response.name;
                var options = response;
                options.type = "type-file";
                options.username = chatApplication.username;
                options.fullname = chatApplication.fullname;
                chatApplication.chatRoom.sendMessage(msg, options, "true", function() {
                  $("#dropzone").find('.bar').width("0%");
                  $("#dropzone").find('.bar').html("");
                  hideMeetingPanel();
                });

              },
              progressUpdated: function(i, file, progress) {
                console.log("progress updated : "+i+" : "+file.name+" : "+progress);
                $("#dropzone").find('.bar').width(progress+"%");
                $("#dropzone").find('.bar').html(progress+"%");
                // this function is used for large files and updates intermittently
                // progress is the integer value of file being uploaded percentage to completion
              }
            });

          });


        }, true);

      }

      if (toggleClass === "meeting-action-task-panel") {
        $("#task-add-task").val("");
        $("#task-add-user").val("");
        $("#task-add-date").val("");
        jqchat(".task-user-label").parent().remove();
        hideResults();
      } else if (toggleClass === "meeting-action-event-panel") {
        $("#event-add-summary").val("");
        $("#event-add-start-date").val("");
        $("#event-add-end-date").val("");
        $("#event-add-location").val("");
        $("#event-add-start-time").val("all-day");
        $("#event-add-end-time").val("all-day");

      }

    });

    function hideMeetingPanel() {
      $(".meeting-action-popup").css("display", "none");
      $(".meeting-action-toggle").removeClass("active");
    }

    $(".input-with-value").on("click", function() {
      if ($(this).hasClass("input-default")) {
        $(this).val("");
        $(this).removeClass("input-default");
      }
    });

    $(".input-with-value").on("focus", function() {
      if ($(this).hasClass("input-default")) {
        $(this).val("");
        $(this).removeClass("input-default");
      }
    });


    var handleGlobalNotifLayout = function () {
        jqchat("#room-detail .room-detail-fullname").html(chatBundleData["exoplatform.chat.settings.button.tip"]);
        desktopNotification.getPreferredNotification().forEach(function (prefNotif, index, array) {
          $("#global-config input[notif-type='"+prefNotif+"']").attr("checked","checked");
        });
        desktopNotification.getPreferredNotificationTrigger().forEach(function (prefNotif, index, array) {
          $("#global-config input[notif-trigger='"+prefNotif+"']").attr("checked","checked");
        });

        if(desktopNotification.getPreferredNotification().length===0){ //if there is no preffered channel
          $("#global-config input[notif-trigger]").attr('disabled',true);
          $("#global-config input[notif-trigger]").parent().addClass("switchBtnDisabled");
        } else {
          $("#global-config input[notif-trigger]").removeAttr('disabled');
          $("#global-config input[notif-trigger]").parent().removeClass("switchBtnDisabled");
        }
        jqchat('#global-config :checkbox').iphoneStyle();
        enableMessageComposer(false);
        jqchat("#chat-team-button-dropdown").hide();
        jqchat("#userRoomStatus").removeClass("hide").show();
    };

    $(document).on("click", "#global-config #close-global-notif-config, #back", function() {//close the setting page and go for the previous screen
        if(window.innerWidth <= 767){

            jqchat("#chat-room-detail-avatar").css("display", "block");
            jqchat(".chat-message.footer").css("display", "block");
            jqchat(".uiLeftContainerArea").addClass("displayContent");

            jqchat(".uiGlobalRoomsContainer").removeClass("displayContent");
            setTimeout(function(){
                jqchat(".uiGlobalRoomsContainer").css("display", "none");
            }, 500);

            jqchat("#chats").css("min-height", "0");

            jqchat("#chat-room-detail-avatar, .chat-message.footer, #searchButtonResp").css("display", "block");
            jqchat("#userRoomStatus").removeClass("hide");

            $serachText = jqchat('#chat-search').attr('placeholder');
            if($serachText.search("@") == -1){
                jqchat("#chat-search").attr("placeholder", "@"+$serachText);
            }
        }
        chatApplication.loadRoom();
    });

    $(document).on("click", "#room-config #close-room-notif-config", function() {//close the setting page and go for the previous screen

        if(window.innerWidth > 767){
            chatApplication.loadRoom();
        }else{
            jqchat("#chat-room-detail-avatar").css("display", "block");
            jqchat(".chat-message.footer").css("display", "block");
            jqchat(".uiLeftContainerArea").addClass("displayContent");
            jqchat(".uiGlobalRoomsContainer").css("display", "none");
            setTimeout(function(){
                jqchat(".uiGlobalRoomsContainer").removeClass("displayContent");
            }, 200);
            jqchat("#chats").css("min-height", "0");
        }
      });

   $(document).on("click", "#room-config input:radio[room-notif-trigger]", function(evt) {//choose a room trigger
     var roomTriggerType = jqchat(this).attr('room-notif-trigger');
     var roomTriggerWhenKeyWordValue = jqchat("#room-config #" + desktopNotification.ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD_VALUE).val();

     if (ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD === roomTriggerType) {
       $("#room-config input#room-notif-trigger-when-key-word-value").prop('disabled', false);
     } else {
       $("#room-config input#room-notif-trigger-when-key-word-value").prop('disabled', true);
     }
     var roomName = chatApplication.targetFullname;
     var roomId = chatApplication.room;

     jqchat.ajax({
       url: chatApplication.jzChatSetRoomNotificationTrigger,
       data: {
         "user": chatApplication.username,
         "room": roomId,
         "notifCondition" : roomTriggerWhenKeyWordValue,
         "notifConditionType" : roomTriggerType,
         "dbName": chatApplication.dbName,
         "time": (new Date()).getTime()
       },
       headers: {
         'Authorization': 'Bearer ' + chatApplication.token
       },

       success:function(operation){
         operation = JSON.parse(operation);
         if(operation.done) {
           var val = roomTriggerType+':'+roomTriggerWhenKeyWordValue;
           desktopNotification.setRoomPreferredNotificationTrigger(roomId, val);//set into the memory
         } else {
           alert("Request received but operation done without success..");
         }
       },
       error:function (xhr, status, error){
         console.log('an error has been occured', error);
       }

     });

    });

    $(document).on("keyup", "#room-config input[name='keyWords']", function(event) {//when entering keywords save them imediately
        console.log(event.target.value);
    });

    //global desktop notification settings
   $("#configButton, #configButtonResp").on("click", function() {
       jqchat("#chat-video-button").css("display", "none");
       chatApplication.configMode = true;
       var $div = jqchat("#global-config-template");

       $div = $div.clone();
       $div.attr("id", "global-config");
       $div.css("display", "inline-block");

       jqchat('#chats').html("");
       $div.appendTo('#chats');

       handleGlobalNotifLayout();

       if (window.innerWidth <= 767) {
           jqchat(".uiExtraLeftContainer").removeClass("displayContent");
           jqchat(".uiGlobalRoomsContainer").css("display", "block");

           setTimeout(function () {
             jqchat(".uiGlobalRoomsContainer").addClass("displayContent");
           }, 200);

           jqchat("#chat-room-detail-avatar, .chat-message.footer, #searchButtonResp").css("display", "none");
           jqchat("#userRoomStatus").addClass("hide");
           jqchat("#chats").css("min-height", window.innerHeight + "px");
       }
   });

    $("#menuButton").on("click", function() {

        $(".uiExtraLeftContainer").toggleClass("displayContent");

        var $chatStatusPanel = $(".chat-status-panel");

        $chatStatusPanel.css("display", "none");
        $(" .chat-status-chat").parent().removeClass('active');

    });
    $(".uiExtraLeftContainer > .bg, .uiExtraLeftContainer > .uiExtraLeftGlobal > .close").on("click", function() {
            $(".uiExtraLeftContainer").removeClass("displayContent");

            var $chatStatusPanel = $(".chat-status-panel");

            $chatStatusPanel.css("display", "none");
            $(" .chat-status-chat").parent().removeClass('active');

    });

    $("#searchButton, #searchButtonResp").on("click", function() {
            $("#chat-application .uiGrayLightBox .uiSearchInput").toggleClass("displayContent");

            var $chatStatusPanel = $(".chat-status-panel");

            $chatStatusPanel.css("display", "none");
            $(" .chat-status-chat").parent().removeClass('active');
    });
    $("#chat-application .uiSearchForm .uiIconClose").on("click", function() {
            $("#chat-application .uiGrayLightBox .uiSearchInput").removeClass("displayContent");
            $('input#chat-search.input-with-value.span4').val('');
            var filter = $('input#chat-search.input-with-value.span4').val();
            chatApplication.search(filter);
    });


var handleRoomNotifLayout = function() {
  chatApplication.chatRoom.loadSetting(function() {
    jqchat("#room-detail .room-detail-fullname").html(chatApplication.targetFullname + " " + chatBundleData["exoplatform.stats.notifications"]);
    var roomPrefTrigger = desktopNotification.getRoomPreferredNotificationTrigger()[chatApplication.room];
    if (roomPrefTrigger) {
      if (roomPrefTrigger === desktopNotification.ROOM_NOTIF_TRIGGER_NORMAL ||
            roomPrefTrigger === desktopNotification.ROOM_NOTIF_TRIGGER_SILENCE) {
        $("#room-config input#room-notif-trigger-when-key-word-value").prop('disabled', true);
        $("#room-config input[room-notif-trigger='"+roomPrefTrigger+"']").attr("checked","checked");
      } else {
        $("#room-config input#room-notif-trigger-when-key-word-value").prop('disabled', false);
        $("#room-config input[room-notif-trigger='"+desktopNotification.ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD+"']").attr("checked","checked");
        var keywords = roomPrefTrigger.split(":")[1];
        $("#room-config input[id='"+desktopNotification.ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD_VALUE+"']").val(keywords);
      }
    } else {
      $("#room-config input#room-notif-trigger-when-key-word-value").prop('disabled', true);
    }
    $("#room-config input#room-notif-trigger-when-key-word-value").unbind("blur");
    $("#room-config input#room-notif-trigger-when-key-word-value").blur(function(evt) {
      $("#room-config input[room-notif-trigger='"+desktopNotification.ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD+"']").click();
    });
  },false);
};

    //team/room desktop notification settings
    $("#team-notification-button").on("click", function() {
      chatApplication.configMode = true;
      enableMessageComposer(false)
      var $div = jqchat("#room-config-template");

      $div = $div.clone();
      $div.attr("id", "room-config");
      $div.css("display", "inline-block");

      jqchat('#chats').html("");
      $div.appendTo('#chats');

      handleRoomNotifLayout();

      jqchat("#chat-room-detail-avatar, .chat-message.footer, #searchButtonResp").css("display", "none");
      jqchat("#userRoomStatus").addClass("hide");
      jqchat("#chats").css("min-height", window.innerHeight+"px");

    });

    $(document).on("click touchstart", "#global-config div.notif-manner div.uiSwitchBtn", function() {
      var notifManner = $(this).find('input[notif-type]').attr("notif-type");
      jqchat.ajax({
        url: chatApplication.jzChatSetPreferredNotification,
        data: {
          "user": chatApplication.username,
          "notifManner" : notifManner,
          "dbName": chatApplication.dbName
          },
        headers: {
          'Authorization': 'Bearer ' + chatApplication.token
        },

        success: function(operation){
          operation = JSON.parse(operation);
          if(operation.done) {
            desktopNotification.setPreferredNotification(notifManner);//set into the memory
            handleGlobalNotifLayout();
          } else {
            console.log("Request received but operation done without success..");
          }
        },
        error: function (xhr, status, error){
          console.log('an error has been occured', error);
        }

      });
    });

    $(document).on("click touchstart", "#global-config div.notif-trigger div.uiSwitchBtn", function() {
    if($(this).hasClass("switchBtnDisabled")) {//if the checkbox are disabled
      return;
    }
      var notifTrigger = $(this).find('input[notif-trigger]').attr("notif-trigger");
      jqchat.ajax({
        url: chatApplication.jzChatSetNotificationTrigger,
        data: {
          "user": chatApplication.username,
          "notifCondition" : notifTrigger,
          "dbName": chatApplication.dbName
        },
        headers: {
          'Authorization': 'Bearer ' + chatApplication.token
        },

        success:function(operation){
          operation = JSON.parse(operation);
          if(operation.done) {
            desktopNotification.setPreferredNotificationTrigger(notifTrigger);//set into the memory
            handleGlobalNotifLayout();
          } else {
            console.log("Request received but operation done without success..");
          }
        },
        error:function (xhr, status, error){
          console.log('an error has been occured', error);
        }

      });
    });



    $(".meeting-close-panel").on("click", function() {
      hideMeetingPanel();
    });

    $(".btnClosePopup").on("click", function() {
      hideMeetingPanel();
    });

    $(".share-link-button").on("click", function() {
      var $uiText = $("#share-link-text");
      var text = $uiText.val();
      if (text === "" || text === $uiText.attr("data-value")) {
        return;
      }

      // if user has not entered http:// https:// or ftp:// assume they mean http://
      if(!/^(https?|ftp):\/\//i.test(text)) {
        text = 'http://'+text; // set both the value
      }

      // Validate url
      var isValid = /^(https?|ftp):\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&amp;'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&amp;'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&amp;'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&amp;'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(\#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&amp;'\(\)\*\+,;=]|:|@)|\/|\?)*)?$/i.test(text);
      if (!isValid) {
        bootbox.alertError(chatBundleData["exoplatform.chat.link.invalid.message"], function (e) {
          e.stopPropagation();
          $("#share-link-text").select();
        });
        return;
      }

      var options = {
        type: "type-link",
        link: text,
        from: chatApplication.username,
        fullname: chatApplication.fullname
      };
      var msg = "";

      chatApplication.chatRoom.sendMessage(msg, options, "true");
      hideMeetingPanel();

        $("i.uiIconPLF24x24Search.search_chatIcon.btn").on("click", function() {
            $("#chat-search, #chat-searchResp, #chat-application").toggleClass("open_searchChat");
        });


    });



    $(".raise-hand-button").on("click", function() {
      var $uiText = $("#raise-hand-comment-text");
      var text = $uiText.val();
      if (text === $uiText.attr("data-value")) {
        text = "";
      }

      var options = {
        type: "type-hand",
        from: chatApplication.username,
        fullname: chatApplication.fullname
      };
      var msg = text;

      chatApplication.chatRoom.sendMessage(msg, options, "true");
      hideMeetingPanel();

    });

    $(".question-button").on("click", function() {
      var $uiText = $("#question-text");
      var text = $uiText.val();
      if (text === "" || text === $uiText.attr("data-value")) {
        return;
      }

      var options = {
        type: "type-question",
        from: chatApplication.username,
        fullname: chatApplication.fullname
      };
      var msg = text;

      chatApplication.chatRoom.sendMessage(msg, options, "true");
      hideMeetingPanel();

    });

    $('#chat-file-form').ajaxForm({
      beforeSubmit: function(formData, jqForm, options) {
        $("#dropzone").find('.bar').width("0%");
        $("#dropzone").find('.bar').html("0%");
        for (index = 0; index < formData.length; index++) {
          if (formData[index].name === "targetFullname") {
            formData[index].value = encodeURIComponent(formData[index].value);
            break;
          }
        }
      },
      uploadProgress: function(event, position, total, percentComplete) {
        console.log("progress updated : "+percentComplete);
        $("#dropzone").find('.bar').width(percentComplete+"%");
        $("#dropzone").find('.bar').html(percentComplete+"%");
      },
      complete: function(xhr) {
//        console.log(xhr.responseText);
        var response = $.parseJSON(xhr.responseText);

        var msg = response.name;
        var options = response;
        options.type = "type-file";
        options.username = chatApplication.username;
        options.fullname = chatApplication.fullname;
        chatApplication.chatRoom.sendMessage(msg, options, "true", function() {
          $("#dropzone").find('.bar').width("0%");
          $("#dropzone").find('.bar').html("");
          $("#chat-file-file").val("");
          hideMeetingPanel();
        });
      }
    });

    $("#chat-file-file").on("change", function() {
      if ($(this).val()!=="") {
          var originalName = encodeURIComponent($(this).val().split(/(\\|\/)/g).pop());
          $("#chat-encoded-file-name").val(originalName);
        $("#chat-file-submit").trigger("click");
      }
    });

    $('.uiRightContainerArea').on('dragenter', function() {
      $("#meeting-action-upload-link").trigger("click");
    });

    $(" .chat-status-chat").parent().on("click", function() {
      var $chatStatusPanel = $(".chat-status-panel");
      if ($chatStatusPanel.css("display")==="none") {
        $chatStatusPanel.css("display", "inline-block");
        $(this).addClass('active');
      }
      else {
        $chatStatusPanel.css("display", "none");
        $(this).removeClass('active');
      }
    });

    $("li.chat-menu").click(function(){
      var status = $(this).attr("status");
      chatApplication.setStatus(status, function() {
        $(".chat-status-panel").css('display', 'none');
         $(".chat-status-chat").parent().removeClass('active');
      });
    });

    $(".msg-emoticons").on("click", function() {
      if ($(this).parent().hasClass("disabled")) return;

      var $msgEmoticonsPanel = $(".msg-emoticons-panel");
      if ($msgEmoticonsPanel.css("display")==="none") {
        $(this).parent().addClass('active');
        $msgEmoticonsPanel.css("display", "inline-block");
      }

      else{
        $msgEmoticonsPanel.css("display", "none");
         $(this).parent().removeClass('active');
      }
    });

    $(".emoticon-btn").on("click", function() {
      var sml = $(this).attr("data");
      $(".msg-emoticons-panel").css("display", "none");
      $msg = $('#msg');
      var val = $msg.val();
      if (val.charAt(val.length-1)!==' ') val +=" ";
      val += sml + " ";
      $msg.val(val);

      if (!chatApplication.$mentionEditor) {
        $msg.focusEnd();
      } else {
        var el = $msg.next('div');
        val = el.html().replace(/<br>/g, '');
        val = el.html().replace(/&nbsp;/g, ' ');
        el.html(val + sml + " ");
      }
      $(".msg-emoticons").parent().removeClass("active");

    });

    $('#chat-search, #chat-searchResp').keyup(function(event) {
      if (event.keyCode == 27 || event.which == 27) {
        $(this).val('');
      }
      var filter = $(this).val();
      chatApplication.search(filter);
    });

    function setMiniCalendarToDateField(dateFieldId) {
      var dateField = document.getElementById(dateFieldId);
      dateField.onfocus=function(){
        uiMiniCalendar.init(this,false,"MM/dd/yyyy","", chatBundleData["exoplatform.chat.monthNames"]);
      };
      dateField.onkeyup=function(){
        uiMiniCalendar.show();
      };
      dateField.onkeydown=function(event){
        uiMiniCalendar.onTabOut(event);
      };
      dateField.onclick=function(event){
        event.cancelBubble = true;
      };
    };

    $(".create-event-button").on("click", function() {
      var space = chatApplication.targetFullname;
      var summary = $("#event-add-summary").val();
      var startDate = $("#event-add-start-date").val();
      var startTime = $("#event-add-start-time").val();
      var endDate = $("#event-add-end-date").val();
      var endTime = $("#event-add-end-time").val();
      var location = $("#event-add-location").val();
      if (space === "" || startDate === "" || startTime === "" || endDate === "" || endTime === "" || summary === "" || summary === $("#event-add-summary").attr("data-value") || location === "" || location ===$("#event-add-location").attr("data-value")) {
        bootbox.alertError(chatBundleData["exoplatform.chat.event.invalid.entry"], function(){
        });
        return;
      }
      if (!uiMiniCalendar.isDate(startDate)) {
        bootbox.alertError(chatBundleData["exoplatform.chat.date.invalid.message"], function(e){
          e.stopPropagation();
          $("#event-add-start-date").select();
        });
        return;
      }
      if (!uiMiniCalendar.isDate(endDate)) {
        bootbox.alertError(chatBundleData["exoplatform.chat.date.invalid.message"], function(e){
          e.stopPropagation();
          $("#event-add-end-date").select();
        });
        return;
      }
      if (Date.parse(startDate)>Date.parse(endDate)) {
          bootbox.alertError(chatBundleData["exoplatform.chat.compareddate.invalid.message"], function(e){
            e.stopPropagation();
            $("#event-add-start-date").select();
          });
          return;
        }
      if (startTime==="all-day") startTime = "12:00 AM";
      if (endTime==="all-day") endTime = "11:59 PM";
      var users = "";
      var targetUser = chatApplication.targetUser;
      if (targetUser.indexOf("space-") == -1) {
        users = $("#chat-file-target-user").val();
      }
      hideMeetingPanel();
      setActionButtonEnabled('.create-event-button', false);

      $.ajax({
        url: chatApplication.jzCreateEvent,
        data: {"space": space,
          "users": users,
          "summary": summary,
          "startDate": startDate,
          "startTime": startTime,
          "endDate": endDate,
          "endTime": endTime,
          "location": location
        },
        success:function(response){

          var options = {
            type: "type-event",
            summary: summary,
            space: space,
            startDate: startDate,
            startTime: startTime,
            endDate: endDate,
            endTime: endTime,
            location:location
          };
          var msg = summary;

          chatApplication.chatRoom.sendMessage(msg, options, "true");
          setActionButtonEnabled('.create-event-button', true);

        },
        error:function (xhr, status, error){
          console.log("error");
          setActionButtonEnabled('.create-event-button', true);
        }
      });
    });

    function setActionButtonEnabled(btnClass, isEnabled) {
      if (isEnabled) {
        $(btnClass).css('cursor', "default");
        $(btnClass).removeAttr('disabled');
      } else {
        $(btnClass).css('cursor', "progress");
        $(btnClass).attr('disabled','disabled');
      }
    };

    $("#event-add-start-time").on("change", function() {
      var time = $(this).val();
      var h = Math.round(time.split(":")[0]) + 1;
      var hh = h;
      if (h<10) hh = "0"+h;
      $("#event-add-end-time").val(hh+":"+time.split(":")[1]);
    });

    setMiniCalendarToDateField('event-add-start-date');
    setMiniCalendarToDateField('event-add-end-date');

    addTimeOptions("#event-add-start-time");
    addTimeOptions("#event-add-end-time");

    function addTimeOptions(id) {
      var select = $(id);
      for (var h = 0; h < 24; h++) {
        for (var m = 0; m < 60; m += 30) {
          var hh = h;
          var mm = m;
          var h12 = h % 12 || 12;
          var hh12 = h12;
          var ampm = h < 12 ? "AM" : "PM";
          if (h < 10) hh = "0" + hh;
          if (m < 10) mm = "0" + mm;
          if (h12 < 10) hh12 = "0" + hh12;
          var time = hh + ":" + mm;
          var time12 = hh12 + ":" + mm + " " + ampm;
          select.append('<option value="' + time12 + '">' + time12 + '</option>');
        }
      }
    }

    function hideResults() {
      var $userResults = $(".task-users-results");
      $userResults.css("display", "none");
      $userResults.html("");
    }

    $('#team-add-user').keyup(function(event) {
      var prefix = "team";
      var filter = $(this).val();

      searchUsers(filter, prefix, event, false, function(name, fullname) {
        addTeamUserLabel(name, fullname);
      });


    });

    function searchUsers(filter, prefix, event, withCurrentUser,callback) {

      if ( event.which === 13 ) { // ENTER
        $("."+prefix+"-user").each(function() {
          if ($(this).hasClass("selected")) {
            var name = $(this).attr("data-name");
            var fullname = $(this).attr("data-fullname");
            if (typeof callback === "function") {
              callback(name, fullname);
            }
          }
        });
      } else if ( event.which === 40 || event.which === 38) { // 40:DOWN || 38:UP
        var isUp = (event.which === 38);
        var total = $("."+prefix+"-user").size();
        var done = false;
        $("."+prefix+"-user").each(function(index) {
          if (!done && $(this).hasClass("selected")) {
            done = true;
            $("."+prefix+"-user").removeClass("selected");
            if (isUp) {
              if (index === 0)
                $("."+prefix+"-user").last().addClass("selected");
              else
                $(this).prev().addClass("selected");
            } else {
              if (index === total-1)
                $("."+prefix+"-user").first().addClass("selected");
              else
                $(this).next().addClass("selected");
            }
          }
        });
        return;
      }

      if (filter === "") {
        var $userResults = $("."+prefix+"-users-results");
        $userResults.css("display", "none");
        $userResults.html("");
      } else {
        chatApplication.getAllUsers(filter, function (jsonData) {
          var users = TAFFY(jsonData.users);
          var users = users();
          var $userResults = $("."+prefix+"-users-results");
          $userResults.css("display", "none");
          var html = "";
          if (!withCurrentUser) {
              users = users.filter({name:{"!is":chatApplication.username}});
          }
          $("."+prefix+"-user-label").each(function() {
            var name = $(this).attr("data-name");
            users = users.filter({name:{"!is":name}});
          });
          var filterRegExp = new RegExp( "(" + filter + ")" , 'gi' );
          users.order("fullname").limit(5).each(function (user, number) {
            $userResults.css("display", "block");
            if (user.status == "offline") user.status = "invisible";
            var classSel = "";
            if (number === 0) classSel = "selected"
            html += "<div class='"+prefix+"-user item "+classSel+"' data-name='"+user.name+"' data-fullname='"+user.fullname+"'>";
            html += " <span class='chat-user-name'><span class='inner'>";
            html += "  <span class='"+prefix+"-user-fullname'>"+ user.fullname.replace(filterRegExp,"<b>$1</b>") +"</span>";
            html += "  <span class='"+prefix+"-user-name'>("+user.name+")</span>";
            html += " </span></span>";
            html += "  <span class='"+prefix+"-user-logo'><img onerror=\"this.src='/chat/img/Avatar.gif;'\" src='/rest/v1/social/users/"+user.name+"/avatar' width='30px' style='width:30px;'></span>";
            html += " <span class='chat-status-"+prefix+" chat-status-"+user.status+"'></span>";
            html += "</div>";
          });
          $userResults.html(html);

          $('.'+prefix+'-user').on("mouseover", function() {
            $("."+prefix+"-user").removeClass("selected");
            $(this).addClass("selected");
          });

          $('.'+prefix+'-user').on("click", function() {
            var name = $(this).attr("data-name");
            var fullname = $(this).attr("data-fullname");
            if (typeof callback === "function") {
              callback(name, fullname);
            }
          });

        });
      }
    }

    /**
     * Add a user mention html component in the uiContainer element.
     * @param uiContainer the html element to insert the mention in
     * @param username the username (invisible)
     * @param fullName the full name (visible)
     * @param removable is the mention html component removable (it add a clickable X in the component) (default is true)
       */
    function addTeamUserMention(uiContainer, username, fullName, removable) {
      if (typeof removable === 'undefined') removable = true;
      var html = "<span class='uiMention'><a href='javascript:void(0)' class='team-user-label' data-name='"+username+"'>"+fullName;
      if (removable) {
        html += "&nbsp;&nbsp;<i class='uiIconClose uiIconLightGray team-user-remove'></i>";
      }
      html +="<a/></span>";
      var $__return = uiContainer.append(html);

      if (removable) {
        $__return.find(".team-user-remove").on("click", function() {
          $(this).parents('.uiMention').remove();
        });
      }
    }

    function addTeamUserLabel(name, fullname) {
      var $usersList = $('.team-users-list');
      addTeamUserMention($usersList, name, fullname, true);
      var $teamAddUser = $('#team-add-user');
      $teamAddUser.val("");
      $teamAddUser.focus();
      var $userResults = $(".team-users-results");
      $userResults.css("display", "none");
      $userResults.html("");
    }

    function strip(html)
    {
      var tmp = document.createElement("DIV");
      tmp.innerHTML = html;
      return tmp.textContent||tmp.innerText;
    }

    $("#team-edit-button").on("click", function() {
      // Only the room owner can manage room users and can see the System popup.
      // We do a check to be sure the current user is the room owner.
      if (chatApplication.username === chatApplication.chatRoom.owner) {
        chatApplication.getUsers(chatApplication.targetUser, function (jsonData) {
          var users = TAFFY(jsonData.users);
          var users = users();

          var $userResults = $(".team-users-results");
          $userResults.css("display", "none");
          $userResults.html("");
          var $uitext = $("#team-modal-name");
          $uitext.val(chatApplication.targetFullname);
          $uitext.attr("data-id", chatApplication.targetUser);
          $(".team-users-list").empty();
          users.order("fullname").each(function (user, number) {
            if (user.name !== chatApplication.username) {
              addTeamUserLabel(user.name, user.fullname);
            }
          });

          uiChatPopupWindow.show("team-modal-form", true);
          jqchat("#team-modal-form .setting").css("display", "block");
          jqchat("#team-modal-form .add").css("display", "none");
          $uitext.focus();

          // Set form position to screen center
          chatApplication.setModalToCenter('.team-modal');
        });
      }
    });

    $("#team-delete-button").on("click", function(e) {
      jqchat("#team-delete-window-chat-name").text(chatBundleData["exoplatform.chat.team.delete.message"].replace("{0}", chatApplication.targetFullname));
      uiChatPopupWindow.show("team-delete-window",true, true);
    });

    $("#team-delete-button-ok").on("click", function() {
      uiChatPopupWindow.hide("team-delete-window",true);
      jqchat("#team-delete-window-chat-name").empty();
      chatApplication.deleteTeamRoom(function() {
        chatApplication.chatRoom.emptyChatZone();
        jqchat("#users-online-team-"+chatApplication.room).hide();
        chatApplication.room="";
        chatApplication.chatRoom.id="";
      });

      if(window.innerWidth <= 767){
      		jqchat(".uiLeftContainerArea").addClass("displayContent");
      		jqchat(".uiLeftContainerArea").removeClass("hideContent");

      		jqchat(".uiGlobalRoomsContainer").addClass("hideContent").removeClass("displayContent");

      		setTimeout(function(){
      			 jqchat(".uiGlobalRoomsContainer").css("display", "none");
      		}, 500);
        }

    });

    $("#team-delete-button-cancel").on("click", function() {
      uiChatPopupWindow.hide("team-delete-window",true);
      jqchat("#team-delete-window-chat-name").empty();
    });

    $(".team-settings-modal-close").on("click", function() {
      uiChatPopupWindow.hide("team-settings-modal-view", true);
    });

    $(".msButtonRecord").on("click", function() {
      var $icon = $(this).children("i");

      var msgType = $icon.hasClass("uiIconChatRecordStart") ? "type-meeting-start" : "type-meeting-stop";
      var options = {
        type: msgType,
        fromUser: chatApplication.username,
        fromFullname: chatApplication.fullname
      };

      var msg = "";

      chatApplication.chatRoom.sendMessage(msg, options, "true");
    });

    $(".team-modal-cancel").on("click", function() {
      uiChatPopupWindow.hide("team-modal-form", true);
      var $uitext = $("#team-modal-name");
      $uitext.val("");
      $uitext.attr("data-id", "---");
    });

    $(".team-modal-save").on("click", function() {
      var $uitext = $("#team-modal-name");
      var teamName = $uitext.val();
      if ($.trim(teamName) === "") {
        $uitext.focus();
        return;
      }
      var teamId = $uitext.attr("data-id");
      uiChatPopupWindow.hide("team-modal-form", true);

      var users = chatApplication.username;
      $(".team-user-label").each(function(index) {
        var name = $(this).attr("data-name");
        users += ","+name;
      });

      chatApplication.saveTeamRoom(teamName, teamId, users, function(data) {
        var teamName = data.name;
        var roomId = "team-"+data.room;
        chatApplication.refreshWhoIsOnline(roomId, teamName);
        // Refresh the chatRoom after creating the team room
        chatApplication.targetUser = roomId;
        chatApplication.targetFullname = teamName;
        chatApplication.loadRoom();
      });

      $uitext.val("");
      $uitext.attr("data-id", "---");

    });

    $(".text-modal-close").on("click", function() {
      $('.text-modal').modal('hide');
    });

    $(".edit-modal-cancel").on("click", function() {
      $('.edit-modal').modal('hide');
      jqchat("#msg").focus();
      $("#edit-modal-area").val("");
    });

    $(".edit-modal-save").on("click", function() {
      var $uitext = $("#edit-modal-area");
      var id = $uitext.attr("data-id");
      var message = $uitext.val();
      $uitext.val("");
      $('.edit-modal').modal('hide');

      chatApplication.editMessage(id, message, function() {
        chatApplication.chatRoom.refreshChat(true);
        jqchat("#msg").focus();
      });

    });

    $('#edit-modal-area').keydown(function(event) {
      //prevent the default behavior of the enter button
      if ( event.which == 13 ) {
        event.preventDefault();
      }
      //adding (shift or ctl or alt) + enter for adding carriage return in a specific cursor
      if ( event.keyCode == 13 && (event.shiftKey||event.ctrlKey||event.altKey) ) {
        this.value = this.value.substring(0, this.selectionStart)+"\n"+this.value.substring(this.selectionEnd,this.value.length);
        var textarea =  jqchat(this);
        jqchat(this).scrollTop(textarea[0].scrollHeight - textarea.height());
      }
      if ( event.which == 18 ) {
        keydownModal = 18;
      }
    });

    $('#edit-modal-area').keyup(function(event) {
      var id = $(this).attr("data-id");
      var msg = $(this).val();
      if ( event.which === 13 && keydownModal !== 18 && msg.length>1) {
        if( !msg || event.keyCode == 13 && (event.shiftKey||event.ctrlKey||event.altKey) )
        {
          return false;
        }
        $(this).val("");
        $('.edit-modal').modal('hide');

        chatApplication.editMessage(id, msg, function() {
          chatApplication.chatRoom.refreshChat(true);
          jqchat("#msg").focus();
        });

      }
      if ( keydownModal === 18 ) {
        keydownModal = -1;
      }
      if ( event.which === 13 && msg.length === 1) {
        $(this).val('');
      }
      // press Escape
      if (event.which === 27) {
        $('.edit-modal').modal('hide');
        jqchat("#msg").focus();
      }

    });


    if (window.fluid!==undefined) {
      chatApplication.activateMaintainSession();
    }


    function initFluidApp() {
      if (window.fluid!==undefined) {
        window.fluid.addDockMenuItem(labelAvailable, chatApplication.setStatusAvailable);
        window.fluid.addDockMenuItem(labelAway, chatApplication.setStatusAway);
        window.fluid.addDockMenuItem(labelDoNotDisturb, chatApplication.setStatusDoNotDisturb);
        window.fluid.addDockMenuItem(labelInvisible, chatApplication.setStatusInvisible);
      }
    }
    initFluidApp();


    function reloadWindow() {
      var sURL = unescape(window.location.href);
      //console.log(sURL);
      window.location.href = sURL;
      //window.location.reload( false );
    }

    // We change the current history by removing get parameters so they won't be visible in the popup
    // Having a location bar with ?noadminbar=true is not User Friendly ;-)
    function removeParametersFromLocation() {
      var sURL = window.location.href;
      if (sURL.indexOf("?")>-1) {
        sURL = sURL.substring(0,sURL.indexOf("?"));
        window.history.replaceState("#", "Chat", sURL);
      }
    }

    String.prototype.endsWith = function(suffix) {
      return this.indexOf(suffix, this.length - suffix.length) !== -1;
    };

    $("#room-users-btn-offline").on("click", function() {
      chatApplication.showRoomOfflinePeople = !chatApplication.showRoomOfflinePeople;
      // toggle button state
        var showRoomOfflinePeopleButton = $(this);
      if(chatApplication.showRoomOfflinePeople == true) {
        showRoomOfflinePeopleButton.addClass("active");
        showRoomOfflinePeopleButton.attr("title", showRoomOfflinePeopleButton.attr("data-title-active"));
      } else {
        showRoomOfflinePeopleButton.removeClass("active");
        showRoomOfflinePeopleButton.attr("title", showRoomOfflinePeopleButton.attr("data-title-inactive"));
      }
      showRoomOfflinePeopleButton.tooltip("fixTitle");
      showRoomOfflinePeopleButton.tooltip("show");
      // toggle offline users visibility
      chatApplication.toggleOfflineRoomUsers(chatApplication.showRoomOfflinePeople);
    });

    $("#room-users-collapse-bar, .participantsList").on("click", function() {
      // toggle room users
      var roomUsersContainer = $(".uiRoomUsersContainerArea");
      var roomUsersHeaderTitle = roomUsersContainer.find("#room-users-title");
      var collpaseBarIcon = $(this).children("i");
      var allContacts = $("#room-users-button > ul > li:nth-child(1)");

      if(roomUsersContainer.is(".room-users-expanded")) {
        roomUsersContainer.removeClass("room-users-expanded");
        roomUsersContainer.addClass("room-users-collapsed");
        collpaseBarIcon.removeClass("uiIconArrowRight uiIconArrowDefault");
        collpaseBarIcon.addClass("uiIconArrowLeft");
      } else {
        roomUsersContainer.removeClass("room-users-collapsed");
        roomUsersContainer.addClass("room-users-expanded");
        collpaseBarIcon.removeClass("uiIconArrowLeft uiIconArrowDefault");
        collpaseBarIcon.addClass("uiIconArrowRight");

        allContacts.css("display", "block");
      }
    });
    $("#closeListPart").on("click", function() {

        var roomUsersContainer = $(".uiRoomUsersContainerArea");
        var collpaseBarIcon = $(this).children("i");
        var allContacts = $("#room-users-button > ul > li:nth-child(1)");
        roomUsersContainer.removeClass("room-users-expanded");
        roomUsersContainer.addClass("room-users-collapsed");
        collpaseBarIcon.removeClass("uiIconArrowRight uiIconArrowDefault");
        collpaseBarIcon.addClass("uiIconArrowLeft");
        allContacts.css("display", "none");


    });

    /* ADDING @ TO Search placeholder */
        if(window.innerWidth <= 767){
            $serachText = $('#chat-search').attr('placeholder');
            $("#chat-search").attr("placeholder", "@"+$serachText);
        }

  });


})(jqchat);

/**
 ##################                           ##################
 ##################                           ##################
 ##################   CHAT APPLICATION        ##################
 ##################                           ##################
 ##################                           ##################
 */

/**
 * ChatApplication Class
 * @constructor
 */
function ChatApplication() {
  //check if we're on the config mode or not
  this.configMode = false;
  this.isLoaded = false;
  this.isPublic = false;
  this.publicModeEnabled = false;
  this.portalURI = "";
  this.dbName = "";
  this.chatFullscreen = "false";

  this.chatRoom;
  this.room = "";
  this.rooms = "";
  this.username = "";
  this.fullname = "";
  this.targetUser = "";
  this.targetFullname = "";
  this.token = "";
  this.jzInitChatProfile = "";
  this.jzWhoIsOnline = "";
  this.jzChatGetRoom = "";
  this.jzChatGetCreator = "";
  this.jzChatToggleFavorite = "";
  this.jzCreateDemoUser = "";
  this.jzChatUpdateUnreadMessages = "";
  this.jzChatSend = "";
  this.jzChatRead = "";
  this.jzChatSendMeetingNotes = "";
  this.jzChatGetMeetingNotes = "";
  this.jzGetStatus = "";
  this.jzSetStatus = "";
  this.jzMaintainSession = "";
  this.jzUpload = "";
  this.jzCreateTask = "";
  this.jzCreateEvent = "";
  this.jzSaveWiki = "";
  this.jzUsers = "";
  this.jzDelete = "";
  this.jzDeleteTeamRoom = "";
  this.jzEdit = "";
  this.jzSaveTeamRoom = "";
  this.userFilter = "";    //not set
  this.chatIntervalChat = "";
  this.chatIntervalUsers = "";
  this.plfUserStatusUpdateUrl = "";
  this.chatIntervalSession = "";
  this.chatIntervalStatus = "";

  this.chatSessionInt = -1; //not set
  this.filterInt;
  this.messages = [];

  this.chatOnlineInt = -1;
  this.notifStatusInt = -1;
  this.ANONIM_USER = "__anonim_";
  this.SUPPORT_USER = "__support_";
  this.isAdmin = false;
  this.isTeamAdmin = false;

  this.old = '';
  this.firstLoad = true;

  this.profileStatus = "offline";
  this.whoIsOnlineMD5 = 0;
  this.totalNotif = 0;
  this.oldNotif = 0;

  this.showFavorites = true;
  this.showPeople = true;
  this.showOffline = false;
  this.showSpaces = true;
  this.showTeams = true;

  this.showPeopleHistory = false;
  this.showSpacesHistory = false;
  this.showTeamsHistory = false;

  this.showRoomOfflinePeople = false;
  this.plugins = [];
}

ChatApplication.prototype.registerEvent = function(plugin) {
  this.plugins.push(plugin);
}

ChatApplication.prototype.trigger = function(event, context) {
  jqchat.each(this.plugins, function(idx, plugin) {
    if (context.continueSend && plugin[event]) {
      plugin[event](context);
    }
  });
}

/**
 * Create demo user
 *
 * @param fullname
 * @param email
 */
ChatApplication.prototype.createDemoUser = function(fullname, email) {

  setTimeout(jqchat.proxy(this.showSyncPanel, this), 1000);
  jqchat.ajax({
    url:this.jzCreateDemoUser,
    data: {
      "fullname": fullname,
      "email": email,
      "isPublic": this.isPublic,
      "dbName": this.dbName
    },
    dataType: "json",
    context: this,
    success: function(data) {
      //console.log("username : "+data.username);
      //console.log("token    : "+data.token);

      jzStoreParam("anonimUsername", data.username, 600000);
      jzStoreParam("anonimFullname", fullname, 600000);
      jzStoreParam("anonimEmail", email, 600000);

      this.username = data.username;
      this.token = data.token;

      if(window.innerWidth > 767){
        jqchat(".uiGrayLightBox .label-user").html(fullname);
      }
      jqchat(".uiExtraLeftContainer .label-user").text(data.fullname);
      jqchat(".avatar-image:first").attr("src", gravatar(email));
      this.hidePanels();

      this.refreshWhoIsOnline();

      if (this.isPublic) {
        this.targetUser = this.SUPPORT_USER;
        this.targetFullname = chatBundleData["exoplatform.chat.support.fullname"];
      }

      this.loadRoom();

    }
  });

};

/**
 * Activate tooltip in this page
 */
ChatApplication.prototype.activateTootips = function() {
  jqchat("[data-toggle='tooltip']").tooltip();
}
/**
 * Update Unread Messages
 *
 * @param callback
 */
ChatApplication.prototype.updateUnreadMessages = function(callback) {
  jqchat.ajax({
    url: this.jzChatUpdateUnreadMessages,
    data: {"room": this.room,
      "user": this.username,
      "timestamp": new Date().getTime(),
      "dbName": this.dbName
    },
    headers: {
      'Authorization': 'Bearer ' + this.token
    },

    success:function(response){
      //console.log("success");
      if (typeof callback === "function") {
        callback();
      }
    },

    error:function (xhr, status, error){

    }

  });

};

/**
 * Edit a chat message in a popup
 * @param msgDataId the data-id value of the message
 * @param msgData the raw text of the message to edit
 */
ChatApplication.prototype.openEditMessagePopup = function (msgDataId, msgData) {
  if (msgDataId === null || msgDataId === undefined || msgDataId === "") {
    return;
  }
  if (msgData === null || msgData === undefined || msgData === "") {
    return;
  }

  msgHtml = msgData.replace(eval("/<br>/g"), "\n");

  var $uitextarea = jqchat("#edit-modal-area");
  $uitextarea.val(msgHtml);
  $uitextarea.attr("data-id", msgDataId);
  jqchat('.edit-modal').modal({"backdrop": false});
  chatApplication.setModalToCenter('.edit-modal');
  $uitextarea.focus();
}

/**
 * Delete the message with id in the room
 *
 * @param id
 * @param callback
 */
ChatApplication.prototype.deleteMessage = function(id, callback) {
  jqchat.ajax({
    url: this.jzDelete,
    data: {"room": this.room,
      "user": this.username,
      "messageId": id,
      "dbName": this.dbName
    },
    headers: {
      'Authorization': 'Bearer ' + this.token
    },

    success:function(response){
      //console.log("success");
      if (typeof callback === "function") {
        callback();
      }
    },

    error:function (xhr, status, error){

    }

  });

};

/**
 * Delete the selected team room
 *
 * @param callback
 */
ChatApplication.prototype.deleteTeamRoom = function(callback) {
  jqchat.ajax({
    url: this.jzDeleteTeamRoom,
    data: {"room": this.room,
      "user": this.username,
      "dbName": this.dbName
    },
    headers: {
      'Authorization': 'Bearer ' + this.token
    },
    success:function(response){
      if (typeof callback === "function") {
        callback();
      }
    },
    error:function (xhr, status, error){
      console.log(chatBundleData["exoplatform.chat.team.delete.error"] + " (" + error + ")");
      bootbox.alertError(chatBundleData["exoplatform.chat.team.delete.error"]);
    }
  });
};

/**
 * Edit the message with id with a new message
 *
 * @param id
 * @param newMessage
 * @param callback
 */
ChatApplication.prototype.editMessage = function(id, newMessage, callback) {
  jqchat.ajax({
    type: 'POST',
    url: this.jzEdit,
    data: {"room": this.room,
      "user": this.username,
      "messageId": id,
      "message": encodeURIComponent(newMessage),
      "dbName": this.dbName
    },
    headers: {
      'Authorization': 'Bearer ' + this.token
    },

    success:function(response){
      //console.log("success");
      if (typeof callback === "function") {
        callback();
      }
    },

    error:function (xhr, status, error){

    }

  });

};

/**
 * Saves a Team room for current user
 *
 * @param teamName
 * @param room
 * @param callback : callback method with roomId as a parameter
 */
ChatApplication.prototype.saveTeamRoom = function(teamName, room, users, callback) {
  jqchat.ajax({
    type: 'POST',
    url: this.jzSaveTeamRoom,
    dataType: "json",
    data: {"teamName": encodeURIComponent(teamName),
      "room": room,
      "users": users,
      "user": this.username,
      "dbName": this.dbName
    },
    headers: {
      'Authorization': 'Bearer ' + this.token
    },

    success:function(response){
      //console.log("success");
      if (typeof callback === "function") {
        callback(response);
      }
    },

    error:function (xhr, status, error){
      alert(error);
      jqchat(".btn-add-team").trigger("click");
    }

  });

};

ChatApplication.prototype.resize = function() {
  var $chatApplication = jqchat("#chat-application");
  var off = 80;
  if (fromChromeApp) {
    $chatApplication.css("padding", "0");
    off = 40;
    jqchat(".uiBox").css("margin", "0");
  }
  if (chatApplication.chatFullscreen == "true") {
    jqchat("#PlatformAdminToolbarContainer").css("display", "none");
  }

  var top = $chatApplication.offset().top;
  var height = jqchat(window).height();
  var heightChat = height - top - off;

  $chatApplication.height(heightChat);
  /* TCHAT HEIGHT ON MOBILE  */
  if(window.innerWidth > 767){
      jqchat("#chats").height(heightChat - 105 - 61);
      jqchat("#chat-users").height(heightChat - 44);
      jqchat("#room-users-list").height(heightChat - 44 - 61 - 20); // remove header and padding
      jqchat("#room-users-collapse-bar").css("line-height", (heightChat - 44) + "px");

  }else{
      jqchat("#chats").height(heightChat - 38);
      jqchat("#chat-users").height(heightChat -44);
      jqchat("#room-users-list").height(heightChat); // remove header and padding
      jqchat("#room-users-collapse-bar").css("line-height", (heightChat -44) + "px");
      jqchat(".uiExtraLeftGlobal, .uiExtraLeftContainer").height(heightChat + 80); // remove header and padding
      jqchat(".uiExtraLeftGlobal, .uiExtraLeftContainer").css("min-height", window.innerHeight+"px"); // remove header and padding
   }

};

/**
 * Init Chat Interval
 */
ChatApplication.prototype.initChat = function() {

  this.chatRoom = new ChatRoom(this.jzChatRead, this.jzChatSend, this.jzChatGetRoom, this.jzChatUpdateUnreadMessages, this.jzChatSendMeetingNotes, this.jzChatGetMeetingNotes, this.chatIntervalChat, this.isPublic, this.portalURI);
  this.chatRoom.onRefresh(this.onRefreshCallback);
  this.chatRoom.onShowMessages(this.onShowMessagesCallback);

  var homeLinkHtml = jqchat("#HomeLink").html();
  homeLinkHtml = '<a href="#" class="btn-home-responsive"></a>'+homeLinkHtml;
  jqchat("#HomeLink").html(homeLinkHtml);

  jqchat(".btn-home-responsive").on("click", function() {
    var $leftNavigationTDContainer = jqchat(".LeftNavigationTDContainer");
    if ($leftNavigationTDContainer.css("display")==="none") {
      $leftNavigationTDContainer.animate({width: 'show', duration: 200});
    } else {
      $leftNavigationTDContainer.animate({width: 'hide', duration: 200});
    }
  });

//  var $roomDetailButton = jqchat(".room-detail-button");
//  if ($roomDetailButton.children().length === 0) {
//    $roomDetailButton.hide();
//  } else {
//    $roomDetailButton.show();
//  }

  this.resize();
  jqchat(window).resize(function() {
    chatApplication.resize();
  });

  this.initChatPreferences();

  this.chatOnlineInt = clearInterval(this.chatOnlineInt);
  this.chatOnlineInt = setInterval(jqchat.proxy(this.refreshWhoIsOnline, this), this.chatIntervalUsers);
  this.refreshWhoIsOnline();

  if (this.username!==this.ANONIM_USER) setTimeout(jqchat.proxy(this.showSyncPanel, this), 1000);
};



/**
 * Init Chat Preferences
 */
ChatApplication.prototype.initChatPreferences = function() {
  this.showFavorites = true;
  if (jzGetParam("chatShowFavorites"+this.username) === "false") this.showFavorites = false;
  this.showPeople = true;
  if (jzGetParam("chatShowPeople"+this.username) === "false") this.showPeople = false;
  this.showOffline = false;
  if (jzGetParam("chatShowOffline"+this.username) === "true") this.showOffline = true;
  this.showSpaces = true;
  if (jzGetParam("chatShowSpaces"+this.username) === "false") this.showSpaces = false;
  this.showTeams = true;
  if (jzGetParam("chatShowTeams"+this.username) === "false") this.showTeams = false;
};

/**
 * Init Chat Profile
 */
ChatApplication.prototype.initChatProfile = function() {
  //var thiss = chatApplication; // TODO: IMPROVE THIS

  if (this.username===this.ANONIM_USER) {
    var anonimFullname = jzGetParam("anonimFullname");
    var anonimUsername = jzGetParam("anonimUsername");
    var anonimEmail = jzGetParam("anonimEmail");

    if (anonimUsername===undefined || anonimUsername===null) {
      this.showDemoPanel();
    } else {
      this.createDemoUser(anonimFullname, anonimEmail);
    }
  } else {
    jqchat.ajax({
      url: this.jzInitChatProfile,
      dataType: "json",
      context: this,
      success: function(data){
        //console.log("Chat Profile Update : "+data.msg);
        //console.log("Chat Token          : "+data.token);
        //console.log("Chat Fullname       : "+data.fullname);
        //console.log("Chat isAdmin        : "+data.isAdmin);
        this.token = data.token;
        this.fullname = data.fullname;
        this.isAdmin = (data.isAdmin=="true");
        this.isTeamAdmin = (data.isTeamAdmin=="true");

        var $chatApplication = jqchat("#chat-application");
        $chatApplication.attr("data-token", this.token);
        var $labelUser = jqchat(".uiGrayLightBox  .label-user");
        if(window.innerWidth > 767){
            $labelUser.text(data.fullname);
        }else{
            $labelUser.removeAttr("href").text("Discussion");
        }
        jqchat(".uiExtraLeftContainer .label-user").text(data.fullname);
        this.refreshWhoIsOnline();
        chatNotification.refreshStatusChat();

      },
      error: function (response){
        //retry in 3 sec
        setTimeout(jqchat.proxy(this.initChatProfile, this), 3000);
      }
    });
  }
};

/**
 * Maintain Session : Only on Fluid app context
 */
ChatApplication.prototype.maintainSession = function() {
  jqchat.ajax({
    url: this.jzMaintainSession,
    context: this,
    success: function(response){
      //console.log("Chat Session Maintained : "+response);
    },
    error: function(response){
      this.chatSessionInt = clearInterval(this.chatSessionInt);
    }
  });
};

/**
 * Get the users of the space
 *
 * @param spaceId : the ID of the space
 * @param callback : return the json users data list as a parameter of the callback function
 */
ChatApplication.prototype.getUsers = function(roomId, callback, asString) {
  jqchat.ajax({
    url: this.jzUsers,
    data: {"room": roomId,
      "user": this.username,
      "dbName": this.dbName
    },
    headers: {
      'Authorization': 'Bearer ' + this.token
    },
    dataType: "json",
    context: this,
    success: function(response){
      if (typeof callback === "function") {
        var users = response;
        if (asString) {
          var userst = TAFFY(response.users);
          users = "";
          userst().each(function (user, number) {
            if (number>0) users += ",";
            users += user.name;
          });
        }

        callback(users);
      }
    },
    error: function() {
      if (typeof callback === "function") {
        callback();
      }
    }
  });
};

/**
 * Get all users corresponding to filter
 *
 * @param filter : the filter (ex: Ben Pa)
 * @param callback : return the json users data list as a parameter of the callback function
 */
ChatApplication.prototype.getAllUsers = function(filter, callback) {
  jqchat.ajax({
    url: this.jzUsers,
    data: {"filter": filter,
      "user": this.username,
      "dbName": this.dbName
    },
    headers: {
      'Authorization': 'Bearer ' + this.token
    },
    dataType: "json",
    context: this,
    success: function(response){
      if (typeof callback === "function") {
        callback(response);
      }
    }
  });
};

ChatApplication.prototype.synGetAllUsers = function(filter, callback) {
  jqchat.ajax({
    async: false,
    url: this.jzUsers,
    data: {"filter": filter,
      "user": this.username,
      "dbName": this.dbName
    },
    headers: {
      'Authorization': 'Bearer ' + this.token
    },
    dataType: "json",
    context: this,
    success: function(response){
      if (typeof callback === "function") {
        callback(response);
      }
    }
  });
};

/**
 * Activate Maintain Session Loop
 */
ChatApplication.prototype.activateMaintainSession = function() {
  this.chatSessionInt = clearInterval(this.chatSessionInt);
  this.chatSessionInt = setInterval(jqchat.proxy(this.maintainSession, this), this.chatIntervalSession);
};


ChatApplication.prototype.updateTotal = function(total) {
  this.totalNotif = total;//Math.abs(this.getOfflineNotif())+Math.abs(this.getOnlineNotif())+Math.abs(this.getSpacesNotif());
};

ChatApplication.prototype.updateTitle = function() {
  if (this.totalNotif>0) {
    document.title = "Chat ("+this.totalNotif+")";
  } else {
    document.title = "Chat";
  }
};

/**
 * Refresh Who Is Online : server call
 */
ChatApplication.prototype.refreshWhoIsOnline = function(targetUser, targetFullname) {
  // Avoid having two whoIsOnline requests in parallel
  if(this.whoIsOnlineRequest) {
    return;
  }
  var withSpaces = jzGetParam("chat.button.space", "true");
  var withUsers = jzGetParam("chat.button.user", "true");
  var withPublic = jzGetParam("chat.button.public", "false");
  var withOffline = jzGetParam("chat.button.offline", "false");

  if (this.username.indexOf(this.ANONIM_USER)>-1) {
    withUsers = "true";
    withSpaces = "true";
    withPublic = "false";
    withOffline = "false";
  }

  if (this.username !== this.ANONIM_USER && this.token !== "---") {
    this.whoIsOnlineRequest = jqchat.ajax({
      url: this.jzChatWhoIsOnline,
      dataType: "json",
      data: { "user": this.username,
        "filter": this.userFilter,
        "isAdmin": this.isAdmin,
        "timestamp": new Date().getTime(),
        "dbName": this.dbName
      },
      headers: {
        'Authorization': 'Bearer ' + this.token
      },
      context: this,
      success: function(response){
        this.whoIsOnlineRequest = null;
        if (targetUser !== undefined && targetFullname !== undefined) {
          this.targetUser = targetUser;
          this.targetFullname = targetFullname;
          jzStoreParam("lastUsername"+this.username, this.targetUser, 60000);
          jzStoreParam("lastFullName"+this.username, this.targetFullname, 60000);
          jzStoreParam("lastTS"+this.username, "0");
          this.firstLoad = true;
        }
//        console.log("refreshWhoIsOnline : "+this.targetUser+" : "+this.targetFullname);

        var tmpMD5 = response.md5;
        if (tmpMD5 !== this.whoIsOnlineMD5) {
          var rooms = TAFFY(response.rooms);
          this.whoIsOnlineMD5 = tmpMD5;
          this.isLoaded = true;
          this.hidePanel(".chat-error-panel");
          this.hidePanel(".chat-sync-panel");
          this.showRooms(rooms);

          // reload room users if the panel is displayed
          var roomUsersContainer = jqchat(".uiRoomUsersContainerArea");
          if(roomUsersContainer.is(":visible")) {
            this.loadRoomUsers();
          }

          this.updateTotal(Math.abs(response.unreadOffline)+Math.abs(response.unreadOnline)+Math.abs(response.unreadSpaces)+Math.abs(response.unreadTeams));
          if (fromChromeApp) {
            if (this.totalNotif>this.oldNotif && this.profileStatus !== "donotdisturb" && this.profileStatus !== "offline") {
              chatNotification.refreshNotifDetails();
            }
          } else if (window.fluid!==undefined) {
            if (this.totalNotif>0)
              window.fluid.dockBadge = this.totalNotif;
            else
              window.fluid.dockBadge = "";
            if (this.totalNotif>this.oldNotif && this.profileStatus !== "donotdisturb" && this.profileStatus !== "offline") {
              window.fluid.showGrowlNotification({
                title: chatBundleData["exoplatform.chat.title"],
                description: chatBundleData["exoplatform.chat.new.messages"],
                priority: 1,
                sticky: false,
                identifier: "messages"
              });
            }
          } else if (window.webkitNotifications!==undefined) {
            if (this.totalNotif>this.oldNotif && this.profileStatus !== "donotdisturb" && this.profileStatus !== "offline") {

              var havePermission = window.webkitNotifications.checkPermission();
              if (havePermission == 0) {
                // 0 is PERMISSION_ALLOWED
                var notification = window.webkitNotifications.createNotification(
                  '/chat/img/chat.png',
                  chatBundleData["exoplatform.chat.title"],
                  chatBundleData["exoplatform.chat.new.messages"]
                );

                notification.onclick = function () {
                  window.open("http://localhost:8080" + chatApplication.portalURI + "chat");
                  notification.close();
                }
                notification.show();
              } else {
                window.webkitNotifications.requestPermission();
              }
            }
          }
          this.oldNotif = this.totalNotif;
          this.updateTitle();
        }
        if (this.isTeamAdmin) {
          jqchat(".btn-top-add-actions").css("display", "inline-block");
        }

      },
      error: function (response){
        //console.log("chat-users :: "+response);
        setTimeout(jqchat.proxy(this.errorOnRefresh, this), 1000);
      }
    });
  }
};

/**
 * Show rooms : convert json to html
 * @param rooms : a json object
 */
ChatApplication.prototype.showRooms = function(rooms) {
  this.rooms = rooms;
  var roomPrevUser = "";
  var out = '<table class="table list-rooms">';
  var classArrow;
  var totalFavorites = 0, totalPeople = 0, totalSpaces = 0, totalTeams = 0;

  // If the selected room is not present in the room list we cleanup the Chat Zone
  // and stop refreshing the Chat data
  if (rooms({room:chatApplication.room}).count()===0) {
    chatApplication.chatRoom.clearInterval();
    chatApplication.room="";
    chatApplication.targetUser="";
    chatApplication.chatRoom.emptyChatZone();
  }

  /**
   * FAVORITES
   */
  out += "<tr class='header-room accordion-heading header-favorites "+(this.showFavorites ? "open":"") + "'><td colspan='3' style='border-top: 0;'>";
  if (this.showFavorites) classArrow="uiIconChatArrowDown uiIconChatLightGray"; else classArrow = "uiIconChatArrowRight uiIconChatLightGray";
  out += chatBundleData["exoplatform.chat.favorites"];
  out += "<div class='nav pull-right uiDropdownWithIcon'><div class='uiAction iconDynamic'><i class='"+classArrow+" uiIconLightGray'></i></div></div>";
  out += '<span class="room-total total-favorites badgeDefault badgePrimary mini">' + chatBundleData["exoplatform.chat.no.favorite"] + '</span>';
  out += "</td></tr>"

  var roomsFavorites = rooms();
  roomsFavorites = roomsFavorites.filter({isFavorite:{is:"true"}});
  roomsFavorites.order("isFavorite desc, timestamp desc, escapedFullname logical").each(function (room) {
//    console.log("FAVORITES : "+room.escapedFullname);
    var rhtml = chatApplication.getRoomHtml(room, roomPrevUser);
    if (rhtml !== "") {
      roomPrevUser = room.user;
      if (chatApplication.showFavorites) {
        out += rhtml;
        chatApplication.reloadCurrentItem(room);
      } else {
        if (Math.round(room.unreadTotal)>0) {
          totalFavorites += Math.round(room.unreadTotal);
        }
      }
    }
  });
  if (roomsFavorites.count() === 0 && this.showFavorites) {
    out += "<tr class='users-online accordion-body empty'><th colspan='3'>" + chatBundleData["exoplatform.chat.no.favorite"] + "</th></tr>";
  }

  var xOffline = ""; if (chatApplication.showOffline) xOffline=" btn active";
  var xPeopleHistory = ""; if (chatApplication.showPeopleHistory) xPeopleHistory=" btn active";
  var xSpacesHistory = ""; if (chatApplication.showSpacesHistory) xSpacesHistory=" btn active";
  var xTeamsHistory = ""; if (chatApplication.showTeamsHistory) xTeamsHistory=" btn active";

  /**
   * USERS
   */
  out += "<tr class='header-room accordion-heading header-people "+(this.showPeople ? "open":"") + "'><td colspan='3'>";
  if (this.showPeople) classArrow="uiIconChatArrowDown uiIconChatLightGray"; else classArrow = "uiIconChatArrowRight uiIconChatLightGray";
  out += chatBundleData["exoplatform.chat.people"];
  out += '<span class="room-total total-people badgeDefault badgePrimary mini"></span>';
  out += "<div class='nav pull-right uiDropdownWithIcon'><div class='uiAction iconDynamic'><i class='"+classArrow+" uiIconLightGray'></i></div></div>";
  out += "<ul class='nav pull-right uiDropdownWithIcon btn-top-history btn-top-history-people' style='margin-right: 5px;'><li><div class='actionIcon btn-history"+xPeopleHistory+"' data-type='people' href='javaScript:void(0)' data-toggle='tooltip' data-placement='bottom' title='" + chatBundleData["exoplatform.chat.show.history"] + "'><i class='uiIconChatClock uiIconChatLightGray'></i></div></li></ul>";
  out += "<ul class='nav pull-right uiDropdownWithIcon btn-top-offline' style='margin-right: 5px;'><li><div class='actionIcon btn-offline"+xOffline+"' data-type='people' href='javaScript:void(0)' data-toggle='tooltip' data-placement='bottom' title='";
  if(chatApplication.showOffline) {
    out += chatBundleData["exoplatform.chat.hide.users"];
  } else {
    out += chatBundleData["exoplatform.chat.show.users"];
  }
  out += "'><i class='uiIconChatMember uiIconChatLightGray'></i></div></li></ul>";
  out += "</td></tr>";

  var roomsPeople = rooms();
  roomsPeople = roomsPeople.filter({status:{"!is":"space"}});
  roomsPeople = roomsPeople.filter({status:{"!is":"team"}});
  roomsPeople = roomsPeople.filter({isFavorite:{"!is":"true"}});
  roomsPeople.order("isFavorite desc, timestamp desc, escapedFullname logical").each(function (room, roomnumber) {
//    console.log("PEOPLE : "+room.escapedFullname);
    if (roomnumber<5 || chatApplication.showPeopleHistory || Math.round(room.unreadTotal)>0) {
      var rhtml = chatApplication.getRoomHtml(room, roomPrevUser);
      if (rhtml !== "") {
        roomPrevUser = room.user;
        if (chatApplication.showPeople && (chatApplication.showOffline || (!chatApplication.showOffline && room.status!=="invisible"))) {
          out += rhtml;
          chatApplication.reloadCurrentItem(room);
        } else {
          if (Math.round(room.unreadTotal)>0) {
            totalPeople += Math.round(room.unreadTotal);
          }
        }
      }
    }
  });
  if (roomsPeople.count() === 0 && this.showPeople) {
    out += "<tr class='users-online accordion-body empty'><th colspan='3'>" + chatBundleData["exoplatform.chat.no.connection"] + "</th></tr>";
  }

  /**
   * TEAMS
   */
  out += "<tr class='header-room accordion-heading header-teams "+(this.showTeams ? "open":"") + "'><td colspan='3'>";
  if (this.showTeams) classArrow="uiIconChatArrowDown uiIconChatLightGray"; else classArrow = "uiIconChatArrowRight uiIconChatLightGray";
  out += chatBundleData["exoplatform.chat.teams"];
  out += '<span class="room-total total-teams badgeDefault badgePrimary mini"></span>';
  out += "<div class='nav pull-right uiDropdownWithIcon'><div class='uiAction iconDynamic'><i class='"+classArrow+" uiIconLightGray'></i></div></div>";
  out += "<ul class='nav pull-right uiDropdownWithIcon btn-top-history btn-top-history-teams' style='margin-right: 5px;'><li><div class='actionIcon btn-history"+xTeamsHistory+"' data-type='team' href='javaScript:void(0)' data-toggle='tooltip' title='" + chatBundleData["exoplatform.chat.show.history"] + "'><i class='uiIconChatClock uiIconChatLightGray'></i></div></li></ul>";
  out += "<ul class='nav pull-right uiDropdownWithIcon btn-top-add-actions' style='margin-right: 5px;'><li><div class='actionIcon btn-add-team' href='javaScript:void(0)' data-toggle='tooltip' data-placement='bottom' title='" + chatBundleData["exoplatform.chat.create.team"] + "'><i class='uiIconChatSimplePlusMini uiIconChatLightGray'></i></div></li></ul>";
  out += "</td></tr>";

  var roomsTeams = rooms();
  roomsTeams = roomsTeams.filter({status:{"is":"team"}});
  roomsTeams = roomsTeams.filter({isFavorite:{"!is":"true"}});
  roomsTeams.order("isFavorite desc, timestamp desc, escapedFullname logical").each(function (room, roomnumber) {
//    console.log("TEAMS : "+room.escapedFullname);
    if (roomnumber<5 || chatApplication.showTeamsHistory || Math.round(room.unreadTotal)>0) {
      var rhtml = chatApplication.getRoomHtml(room, roomPrevUser);
      if (rhtml !== "") {
        roomPrevUser = room.user;
        if (chatApplication.showTeams) {
          out += rhtml;
          chatApplication.reloadCurrentItem(room);
        } else {
          if (Math.round(room.unreadTotal)>0) {
            totalTeams += Math.round(room.unreadTotal);
          }
        }
      }
    }
  });

  if (roomsTeams.count() === 0 && this.showTeams) {
    out += "<tr class='users-online accordion-body empty'><th colspan='3'>" + chatBundleData["exoplatform.chat.no.team"] + "</th></tr>";
  }

  /**
   * SPACES
   */
  out += "<tr class='header-room accordion-heading header-spaces "+(this.showSpaces ? "open":"") + "'><td colspan='3'>";
  if (this.showSpaces) classArrow="uiIconChatArrowDown uiIconChatLightGray"; else classArrow = "uiIconChatArrowRight uiIconChatLightGray";
  out += chatBundleData["exoplatform.chat.spaces"];
  out += '<span class="room-total total-spaces badgeDefault badgePrimary mini"></span>';
  out += "<div class='nav pull-right uiDropdownWithIcon'><div class='uiAction iconDynamic'><i class='"+classArrow+" uiIconLightGray'></i></div></div>";
  out += "<ul class='nav pull-right uiDropdownWithIcon btn-top-history btn-top-history-spaces' style='margin-right: 5px;'><li><div class='actionIcon btn-history"+xSpacesHistory+"' data-type='space' href='javaScript:void(0)' data-toggle='tooltip' title='" + chatBundleData["exoplatform.chat.show.history"] + "'><i class='uiIconChatClock uiIconChatLightGray'></i></div></li></ul>";
  out += "</td></tr>";

  var roomsSpaces = rooms();
  roomsSpaces = roomsSpaces.filter({status:{"is":"space"}});
  roomsSpaces = roomsSpaces.filter({isFavorite:{"!is":"true"}});
  roomsSpaces.order("isFavorite desc, timestamp desc, escapedFullname logical").each(function (room, roomnumber) {
//    console.log("SPACES : "+room.escapedFullname);
    if (roomnumber<3 || chatApplication.showSpacesHistory || Math.round(room.unreadTotal)>0) {
      var rhtml = chatApplication.getRoomHtml(room, roomPrevUser);
      if (rhtml !== "") {
        roomPrevUser = room.user;
        if (chatApplication.showSpaces) {
          out += rhtml;
          chatApplication.reloadCurrentItem(room);
        } else {
          if (Math.round(room.unreadTotal)>0) {
            totalSpaces += Math.round(room.unreadTotal);
          }
        }
      }
    }
  });

  if (roomsSpaces.count() === 0 && this.showSpaces) {
    out += "<tr class='users-online accordion-body empty'><th colspan='3'>" + chatBundleData["exoplatform.chat.no.space"] + "</th></tr>";
  }

  out += '</table>';

  jqchat("#chat-users").html(out);

  this.jQueryForUsersTemplate();
  this.activateTootips();

  if (roomsPeople.count()<=5) {
    jqchat(".btn-top-history-people").hide();
  }
  if (roomsTeams.count()<=5) {
    jqchat(".btn-top-history-teams").hide();
  }
  if (roomsSpaces.count()<=3) {
    jqchat(".btn-top-history-spaces").hide();
  }


  if (chatApplication.isTeamAdmin) {
    jqchat(".btn-top-add-actions").css("display", "inline-block");
  }

  if (totalFavorites>0) {
    jqchat(".total-favorites").html(totalFavorites);
    jqchat(".total-favorites").css("display", "inline-block");
  }

  if (totalPeople>0) {
    jqchat(".total-people").html(totalPeople);
    jqchat(".total-people").css("display", "inline-block");
  }

  if (totalSpaces>0) {
    jqchat(".total-spaces").html(totalSpaces);
    jqchat(".total-spaces").css("display", "inline-block");
  }

  if (totalTeams>0) {
    jqchat(".total-teams").html(totalTeams);
    jqchat(".total-teams").css("display", "inline-block");
  }

};

ChatApplication.prototype.reloadCurrentItem = function(room) {
  if (chatApplication.room === room.room) {
    var spaceFullName = jqchat("<div/>").html(room.escapedFullname).text();
    if (chatApplication.targetFullname !== spaceFullName) {
      jqchat('.target-user-fullname').text(spaceFullName);
      chatApplication.targetUser = room.user;
      chatApplication.targetFullname = spaceFullName;
      chatApplication.loadRoom();
    }
  }
}
ChatApplication.prototype.getRoomHtml = function(room, roomPrevUser) {
  var out = "";
  if (room.user!==roomPrevUser) {
    out += '<tr id="users-online-'+room.user.replace(".", "-")+'" class="users-online accordion-body">';
    out += '<td class="td-status">';
    out += '<i class="';
    if (room.status === "space" || room.status === "team") {
      out += 'uiIconChatTeam uiIconChatLightGray';
    }
    out += ' user-'+room.status+ '';
    if (room.isFavorite == "true") {
      out += ' user-favorite';
    } else {
      out += ' user-status';
    }
    out += '"';
    out += '></i>';
    out += '</td>';
    out +=  '<td>';
    if (room.isActive=="true") {
      out += '<span user-data="'+room.user+'" room-data="'+room.room+'" class="room-link" data-fullname="'+room.escapedFullname+'">'+ decodeRoomName(room.escapedFullname) +'</span>';
    } else {
      out += '<span class="room-inactive muted">'+room.user+'</span>';
    }
    out += '</td>';
    out += '<td>';
    if (Math.round(room.unreadTotal)>0) {
      out += '<span class="room-total badgeDefault badgePrimary mini" style="float:right;" data="'+room.unreadTotal+'">'+room.unreadTotal+'</span>';
    }
    else {
      out+= '<i class="uiIconChatFavorite pull-right';
      if (room.isFavorite == "true") {
        out += ' user-favorite';
      } else {
        out += ' user-status';
      }
      out +='" user-data="'+room.user+'" data-toggle="tooltip" data-placement="bottom"';
      if (room.isFavorite == "true") {
        out += ' title="' + chatBundleData["exoplatform.chat.remove.favorites"] + '"';
      } else {
        out += ' title="' + chatBundleData["exoplatform.chat.add.favorites"] + '"';
      }
      out+= '></i>';
    }
    out += '</td>';
    out += '</tr>';
  }
  return out;
};

var enableMessageComposer = function(bool){
  if(bool) {//enable
    jqchat("div.chat-message").css({//enable the chat footer again
      "pointer-events": "",
      "opacity": ""
    });
  } else {//disable
    jqchat("div.chat-message").css({
      "pointer-events": "none",
      "opacity": "0.3"
    });
  }
}
/**
 * Load Room : server call
 */
function userRoomStatus(targetUser, status) {
        jqchat("#userRoomStatus > i").attr("class", "");
        jqchat("#userRoomStatus > i").addClass("user-"+status);
}

ChatApplication.prototype.loadRoom = function() {
  //console.log("TARGET::"+this.targetUser+" ; ISADMIN::"+this.isAdmin);
  if(this.configMode==true) {
    this.configMode=false;//we're not on the config mode anymore
    enableMessageComposer(true);
  }

  /*
    Retrieving the info related to the destination room used when clicking on the Desktop Notification's popup to show the correct Room.
  */
  if(localStorage.getItem('eXoChat.targetUser')!=""&&localStorage.getItem('eXoChat.targetFullname')!=""){
    this.targetUser = localStorage.getItem('eXoChat.targetUser');
    this.targetFullname = localStorage.getItem('eXoChat.targetFullname');
    localStorage.setItem('eXoChat.targetFullname',"");
    localStorage.setItem('eXoChat.targetUser',"");
  }

  var thiss = this;
  this.chatRoom.owner = "";
  if (this.targetUser!==undefined) {
    // hide admin actions - we need to check if the current is the admin of the room before displaying them
    jqchat("#chat-team-button-dropdown .only-admin").hide();
    // reset room users panel
    this.chatRoom.users = [];
    jqchat("#room-users-list").html("");
    jqchat("#room-users-title-nb-users").html("()");

    if(this.chatRoom.lastCallOwner !== this.targetUser) {
      // Disable composer while switching from a room to another
      enableMessageComposer(false);
      // Empty room messages and add loading icon
      this.chatRoom.emptyChatZone(true);

      // Add a flag for room loading operation with the id of the room
      this.chatRoom.loadingNewRoom = true;
      this.chatRoom.callingOwner = this.targetUser;
    }

    jqchat(".users-online").removeClass("accordion-active");
    if (this.isDesktopView()) {
      var escapedTargetUser = this.targetUser.replace(".", "-").replace("@", "\\@") ;
      var $targetUser = jqchat("#users-online-"+escapedTargetUser);
      $targetUser.addClass("accordion-active");
      jqchat(".room-total").removeClass("badgeWhite");
      $targetUser.find(".room-total").addClass("badgeWhite");
    }


    jqchat("#room-detail").css("display", "block");
    jqchat("#chat-team-button").css("display", "none");
    this.targetFullname = jqchat("<div/>").html(this.targetFullname).text();
    jqchat(".target-user-fullname").text(this.targetFullname);


    if(navigator.platform.indexOf("Linux") === -1 || jqchat.browser.chrome) {
      jqchat(".btn-weemo-conf").css("display", "none");
      jqchat(".btn-weemo-conf").addClass("disabled");
      if (typeof weemoExtension !== 'undefined') {
        jqchat(".btn-weemo").css("display", "block");
        jqchat(".btn-weemo").addClass("disabled");
        jqchat(".room-detail-button").show();
      }
    }
    if (this.targetUser.indexOf("space-")===-1 && this.targetUser.indexOf("team-")===-1)
    ////// USER
    {
      jqchat(".uiRoomUsersContainerArea").hide();
      jqchat(".meeting-action-task").css("display", "block");
      jqchat(".room-detail-avatar").show();
      jqchat("#chat-team-button-dropdown").hide();
      jqchat("#userRoomStatus").removeClass("hide").show();
      jqchat(".target-avatar-link").attr("href", chatApplication.portalURI + "profile/"+this.targetUser);
      jqchat(".target-avatar-image").attr("onerror", "this.src='/chat/img/user-default.jpg';");
      jqchat(".target-avatar-image").attr("src", "/rest/v1/social/users/" + this.targetUser  + "/avatar");
    }
    else if (this.targetUser.indexOf("team-")===-1)
    ////// SPACE
    {
      this.loadRoomUsers();
      jqchat(".meeting-action-task").css("display", "block");
      var spaceName = this.targetFullname.toLowerCase().split(" ").join("_");
      jqchat(".room-detail-avatar").show();
      jqchat(".target-avatar-link").attr("href", "/portal/g/:spaces:"+spaceName+"/"+spaceName);
      jqchat(".target-avatar-image").attr("onerror", "this.src='/eXoSkin/skin/images/themes/default/social/skin/ShareImages/SpaceAvtDefault.png';");
      jqchat(".target-avatar-image").attr("src", "/rest/v1/social/spaces/"+spaceName+"/avatar");
    }
    else
    ////// TEAM
    {

      jqchat.ajax({
        url: this.jzChatGetCreator,
        data: {"room": this.targetUser,
          "user": this.username,
          "dbName": this.dbName
        },
        headers: {
          'Authorization': 'Bearer ' + this.token
        },
        context: this,
        success: function(response){
          //console.log("SUCCESS::getRoom::"+response);
          var creator = response;
          this.chatRoom.owner = creator;
          jqchat(".team-button > .uiDropdownWithIcon").css("display", "block");
          jqchat("#chat-team-button-dropdown").show();//we should always show the dropdown list when we click on a room/team
          jqchat("#userRoomStatus").hide();
          if (creator === this.username) {
            jqchat("#chat-team-button-dropdown .only-admin").show();
            jqchat("#userRoomStatus").hide();
            jqchat("#chat-team-button").show();
            jqchat("#team-delete-button").show();
            jqchat("#chat-team-button-dropdown").show();
            jqchat("#userRoomStatus").hide();
          } else {
            jqchat("#userRoomStatus").removeClass("hide").show();
          }
        },
        error: function(xhr, status, error){
          //console.log("ERROR::"+xhr.responseText);
        }
      });
      this.loadRoomUsers();
      jqchat(".meeting-action-task").css("display", "block");
      jqchat(".room-detail-avatar").show();
      jqchat(".target-avatar-link").attr("href", "#");
      jqchat(".target-avatar-image").attr("src", "/eXoSkin/skin/images/themes/default/social/skin/ShareImages/SpaceAvtDefault.png");
    }

    if (this.targetUser.indexOf("space-") >=0 || this.targetUser.indexOf("team-") >= 0) {
      jqchat.ajax({
        url: this.jzChatIsFavorite,
        data: {"user": this.username,
          "targetUser": this.targetUser,
          "dbName": this.dbName
        },
        headers: {
          'Authorization': 'Bearer ' + this.token
        },
        context: this,
        success: function(response){
          var $teamRemoveFromFavoritButton = jqchat("#team-remove-from-favorites-button");
          var $teamAddToFavoritButton = jqchat("#team-add-to-favorites-button");

          $teamRemoveFromFavoritButton.unbind('click');
          $teamAddToFavoritButton.unbind('click');
          var toggleFav = function() {
              thiss.toggleFavorite(thiss.targetUser);
              $teamRemoveFromFavoritButton.toggle();
              $teamAddToFavoritButton.toggle();
          }
          $teamRemoveFromFavoritButton.click(toggleFav);
          $teamAddToFavoritButton.click(toggleFav);
          if (response == "true") {
            $teamRemoveFromFavoritButton.show();
            $teamAddToFavoritButton.hide();
          } else {
            $teamAddToFavoritButton.show();
            $teamRemoveFromFavoritButton.hide();
          }
        },
        error: function(xhr, status, error){
          console.log("ERROR::"+xhr.responseText);
        }
      });
    }

    var thiss = this;
    this.chatRoom.init(this.username, this.token, this.targetUser, this.targetFullname, this.isAdmin, this.dbName, function(room) {
      thiss.room = room;
      var $msg = jqchat('#msg');
      thiss.activateRoomButtons();
      if (thiss.isDesktopView()) $msg.focus();
    });
  }
};

ChatApplication.prototype.activateRoomButtons = function() {
  var $msg = jqchat('#msg');
  var $msButtonRecord = jqchat(".msButtonRecord");
  var $msgEmoticons = jqchat(".msg-emoticons");
  var $meetingActionToggle = jqchat(".meeting-action-toggle");
  $msg.removeAttr("disabled");
  jqchat("#chat-record-button").show();
  $msButtonRecord.removeAttr(("disabled"));
  $msButtonRecord.attr("data-toggle","tooltip");
  $msgEmoticons.parent().removeClass("disabled");
  $msgEmoticons.parent().attr("data-toggle", "tooltip");
  $meetingActionToggle.removeClass("disabled");
  $meetingActionToggle.children("span").attr("data-toggle", "tooltip");
  jqchat("[data-toggle='tooltip']").tooltip("enable");
};

ChatApplication.prototype.onRefreshCallback = function(code) {
  if (code === 0) { // SUCCESS
    chatApplication.hidePanel(".chat-login-panel");
    chatApplication.hidePanel(".chat-error-panel");
  } else if (code === 1) { //ERROR
/*
    if ( jqchat(".chat-error-panel").css("display") == "none") {
      chatApplication.showLoginPanel();
    } else {
      chatApplication.hidePanel(".chat-login-panel");
    }
*/
  }
}

ChatApplication.prototype.onShowMessagesCallback = function(out) {

  var $chats = jqchat("#chats");
  // check if scroll was at max before the new message
  var scrollTopMax = $chats.prop('scrollHeight') - $chats.innerHeight();
  var scrollAtMax = ($chats.scrollTop() == scrollTopMax);
  $chats.html('<span>' + out + '</span>');
  sh_highlightDocument();
  // if scroll was at max, scroll to the new max to display the new message. Otherwise don't move the scroll.
  if (scrollAtMax) {
    var newScrollTopMax = $chats.prop('scrollHeight') - $chats.innerHeight();
    $chats.scrollTop(newScrollTopMax);
  }

  jqchat(".msg-text").mouseover(function() {
    if (jqchat(".msg-actions", this).children().length > 0) {
      jqchat(".msg-date", this).css("display", "none");
      jqchat(".msg-actions", this).css("visibility", "");
    }
  });

  jqchat(".msg-text").mouseout(function() {
    jqchat(".msg-date", this).css("display", "");
    jqchat(".msg-actions", this).css("visibility", "hide");
  });

  jqchat(".msg-action-quote").on("click", function() {
    var $uimsg = jqchat(this).siblings(".msg-data");
    var msgHtml = $uimsg.html();
    //if (msgHtml.endsWith("<br>")) msgHtml = msgHtml.substring(0, msgHtml.length-4);
    msgHtml = msgHtml.replace(/<br>/g, '\n');
    var msgFullname = $uimsg.attr("data-fn");
    jqchat("#msg").focus().val('').val("[quote="+msgFullname+"]"+msgHtml+" [/quote] ");

  });

  jqchat(".msg-action-delete").on("click", function() {
    var $uimsg = jqchat(this).siblings(".msg-data");
    var msgId = $uimsg.attr("data-id");
    chatApplication.deleteMessage(msgId, function() {
      chatApplication.chatRoom.refreshChat(true);
    });
    //if (msgHtml.endsWith("<br>")) msgHtml = msgHtml.substring(0, msgHtml.length-4);

  });

  jqchat(".msg-action-edit").on("click", function() {
    var $uimsgdata = jqchat(this).siblings(".msg-data");
    chatApplication.openEditMessagePopup($uimsgdata.attr("data-id"), $uimsgdata.html());
  });

  jqchat(".msg-action-savenotes").on("click", function() {
    var $uimsg = jqchat(this).siblings(".msg-data");
    var msgTimestamp = $uimsg.attr("data-timestamp");

    var options = {
      type: "type-notes",
      fromTimestamp: msgTimestamp,
      fromUser: chatApplication.username,
      fromFullname: chatApplication.fullname
    };

    var msg = "";

    chatApplication.chatRoom.sendMessage(msg, options, "true");
  });

  jqchat(".send-meeting-notes").on("click", function () {
    var $this = jqchat(this);
    var $meetingNotes =  $this.closest(".msMeetingNotes");
    $meetingNotes.animate({
      opacity: "toggle"
    }, 200, function() {
      var room = $this.attr("data-room");
      var from = $this.attr("data-from");
      var to = $this.attr("data-to");
      var id = $this.attr("data-id");

      from = Math.round(from)-1;
      to = Math.round(to)+1;
      chatApplication.chatRoom.sendMeetingNotes(room, from, to, function (response) {
        if (response === "sent") {
          console.log("sent");
          jqchat("#"+id).animate({
            opacity: "toggle"
          }, 200 , function() {
            $meetingNotes.animate({
              opacity: "toggle"
            }, 3000);
          });
        }
      });

    });

  });

  jqchat(".save-meeting-notes").on("click", function () {
    var $this = jqchat(this);
    var $meetingNotes =  $this.closest(".msMeetingNotes");
    $meetingNotes.animate({
      opacity: "toggle"
    }, 200, function() {
      var room = $this.attr("data-room");
      var from = $this.attr("data-from");
      var to = $this.attr("data-to");
      var id = $this.attr("data-id");

      from = Math.round(from)-1;
      to = Math.round(to)+1;
      chatApplication.chatRoom.getMeetingNotes(room, from, to, function (response) {
        if (response !== "ko") {
//          console.log(response);
          jqchat.ajax({
            type: "POST",
            url: chatApplication.jzSaveWiki,
            data: {"targetFullname": chatApplication.targetFullname,
              "content": response
            },
            context: this,
            dataType: "json",
            success: function(data){
//              console.log(data.path);
              if (data.path !== "") {
                var baseUrl = location.protocol + "//" + location.hostname;
                if (location.port) {
                  baseUrl += ":" + location.port;
                }
                var options = {
                  type: "type-link",
                  link: baseUrl+data.path,
                  from: chatApplication.username,
                  fullname: chatApplication.fullname
                };
                var msg = chatBundleData["exoplatform.chat.meeting.notes"];

                chatApplication.chatRoom.sendMessage(msg, options, "true");

              }

              jqchat("#"+id).animate({
                opacity: "toggle"
              }, 3000 , function() {
                $meetingNotes.animate({
                  opacity: "toggle"
                }, 2000);
                jqchat("#"+id).hide();
              });
            },
            error: function(xhr, status, error){
            }
          });

        }
      });

    });

  });

}

/**
 * Error On Refresh
 */
ChatApplication.prototype.errorOnRefresh = function() {
  this.isLoaded = true;
  this.hidePanel(".chat-sync-panel");
  this.hidePanel(".chat-login-panel");
  chatNotification.changeStatusChat("offline");
  this.showErrorPanel();
};

ChatApplication.prototype.setModalToCenter = function(modalFormClass) {
  if (modalFormClass !== undefined) {

    // Set form position to screen center
    var centerTop = (jqchat(window).height() - jqchat(modalFormClass).height()) / 2;
    centerTop = centerTop >= 0 ? centerTop : jqchat(modalFormClass).offset().top;
    var centerLeft = (jqchat(window).width() - jqchat(modalFormClass).width()) / 2;
    centerLeft = centerLeft >= 0 ? centerLeft : jqchat(modalFormClass).offset().left;
    jqchat(modalFormClass).offset({top: centerTop, left: centerLeft})
  }
};


/**
 * return a status if a meeting is started or not :
 * -1 : no meeting in chat history
 * 0 : meeting terminated
 * 1 : obgoing meeting
 *
 * @param callback (callStatus)
 */
ChatApplication.prototype.checkIfMeetingStarted = function (room, callback) {
  if (room !== "" && room !== chatApplication.chatRoom.id) {
    chatApplication.chatRoom.getChatMessages(room, function (msgs) {
      var callStatus = -1; // -1:no call ; 0:terminated call ; 1:ongoing call
      var recordStatus = -1;
      for (var i = 0; i < msgs.length - 1 && callStatus === -1; i++) {
        var msg = msgs[i];
        var type = msg.options.type;
        if (type === "call-off") {
          callStatus = 0;
        } else if (type === "call-on") {
          callStatus = 1;
        }
      }
      for (var i = 0; i < msgs.length - 1 && recordStatus === -1; i++) {
        var msg = msgs[i];
        var type = msg.options.type;
        if (type === "type-meeting-stop") {
          recordStatus = 0;
        } else if (type === "type-meeting-start") {
          recordStatus = 1;
        }
      }
      if (callback !== undefined) {
        callback(callStatus, recordStatus);
      }
    });
  } else {
    chatApplication.chatRoom.refreshChat(true, function (msgs) {
      var callStatus = -1; // -1:no call ; 0:terminated call ; 1:ongoing call
      var recordStatus = -1;

      for (var i = 0; i < msgs.length - 1 && callStatus === -1; i++) {
        var msg = msgs[i];
        var type = msg.options.type;
        if (type === "call-off") {
          callStatus = 0;
        } else if (type === "call-on") {
          callStatus = 1;
        }
      }
      for (var i = 0; i < msgs.length - 1 && recordStatus === -1; i++) {
        var msg = msgs[i];
        var type = msg.options.type;
        if (type === "type-meeting-stop") {
          recordStatus = 0;
        } else if (type === "type-meeting-start") {
          recordStatus = 1;
        }
      }
      if (callback !== undefined) {
        callback(callStatus, recordStatus);
      }
    });
  }
};

/**
 * Toggle Favorite : server call
 * @param targetFav : the user or space to put/remove in favorite
 */
ChatApplication.prototype.toggleFavorite = function(targetFav) {
  console.log("FAVORITE::"+targetFav);
  jqchat.ajax({
    url: this.jzChatToggleFavorite,
    data: {"targetUser": targetFav,
      "user": this.username,
      "timestamp": new Date().getTime(),
      "token": this.token
    },
    headers: {
      'Authorization': 'Bearer ' + this.token
    },
    context: this,
    success: function(response){
      this.refreshWhoIsOnline();
    },
    error: function(xhr, status, error){
    }
  });
};

/**
 * Update Meeting Button status
 *
 * @param: status: 'started' or 'stoped'
 */
ChatApplication.prototype.updateMeetingButtonStatus = function(status) {
  var $icon = jqchat(".msButtonRecord").children("i");
  var $span = jqchat(".msButtonRecord").children("span");
  if ('started' === status) {
    $icon.addClass("uiIconChatRecordStop");
    $icon.removeClass("uiIconChatRecordStart");
  } else {
    $icon.addClass("uiIconChatRecordStart");
    $icon.removeClass("uiIconChatRecordStop");
  }

  var tooltipText = $icon.hasClass("uiIconChatRecordStart") ? chatBundleData["exoplatform.chat.meeting.start"] : chatBundleData["exoplatform.chat.meeting.stop"];
  $icon.parent().tooltip('hide')
    .attr('data-original-title', tooltipText)
    .tooltip('fixTitle');

  $span.html(tooltipText);
};

/**
 * jQuery bindings on dom elements created by Who Is Online methods
 */
ChatApplication.prototype.jQueryForUsersTemplate = function() {
  var $targetUser;
  var value = jzGetParam("lastUsername"+this.username);
  var thiss = this;

  if (value && this.firstLoad) {
    //console.log("firstLoad with user : *"+value+"*");
    this.targetUser = value;
    this.targetFullname = jzGetParam("lastFullName"+this.username);
    var escapedTargetUser = this.targetUser.replace(".", "-").replace("@", "\\@") ;
    $targetUser = jqchat("#users-online-"+escapedTargetUser);
    if (!$targetUser.length) {
      this.targetUser = "";
      this.targetFullname = "";
      jzStoreParam("lastUsername"+this.username, this.targetUser, 60000);
      jzStoreParam("lastFullName"+this.username, this.targetFullname, 60000);
    } else {
      if (this.username!==this.ANONIM_USER) {
        this.loadRoom();
      }
      this.firstLoad = false;
    }
  }

  if (this.isDesktopView() && $targetUser!==undefined) {
    $targetUser.addClass("accordion-active");
    jqchat(".room-total").removeClass("badgeWhite");
    $targetUser.find(".room-total").addClass("badgeWhite");
  }

  jqchat(".header-room").on("click", function() {
    if (jqchat(this).hasClass("header-favorites"))
      chatApplication.showFavorites = !chatApplication.showFavorites;
    else if (jqchat(this).hasClass("header-people"))
      chatApplication.showPeople = !chatApplication.showPeople;
    else if (jqchat(this).hasClass("header-spaces"))
      chatApplication.showSpaces = !chatApplication.showSpaces;
    else if (jqchat(this).hasClass("header-teams"))
      chatApplication.showTeams = !chatApplication.showTeams;

    jzStoreParam("chatShowFavorites"+chatApplication.username, chatApplication.showFavorites, 600000);
    jzStoreParam("chatShowPeople"+chatApplication.username, chatApplication.showPeople, 600000);
    jzStoreParam("chatShowSpaces"+chatApplication.username, chatApplication.showSpaces, 600000);
    jzStoreParam("chatShowTeams"+chatApplication.username, chatApplication.showTeams, 600000);

    chatApplication.showRooms(chatApplication.rooms);

  });

  jqchat(".btn-add-team").on("click", function() {
    chatApplication.showTeams = true;
    jzStoreParam("chatShowTeams"+chatApplication.username, chatApplication.showTeams, 600000);
    chatApplication.showRooms(chatApplication.rooms);

    var $uitext = jqchat("#team-modal-name");
    $uitext.val("");
    $uitext.attr("data-id", "---");
    jqchat(".team-user-label").parent().remove();
    var $userResults = jqchat(".team-users-results");
    $userResults.css("display", "none");
    $userResults.html("");
    jqchat("#team-add-user").val("");
    uiChatPopupWindow.show("team-modal-form", true);
    jqchat("#team-modal-form .add").css("display", "block");
    jqchat("#team-modal-form .setting").css("display", "none");
    $uitext.focus();

    chatApplication.setModalToCenter('.team-modal');
  });

  jqchat("#chat-users .btn-history").on("click", function() {
    var type = jqchat(this).attr("data-type");
    if (type === "people") {
      chatApplication.showPeople = true;
      chatApplication.showPeopleHistory = !chatApplication.showPeopleHistory;
      jzStoreParam("chatShowPeople"+chatApplication.username, true, 600000);
    } else if (type === "space") {
      chatApplication.showSpaces = true;
      chatApplication.showSpacesHistory = !chatApplication.showSpacesHistory;
      jzStoreParam("chatShowSpaces"+chatApplication.username, true, 600000);
    } else if (type === "team") {
      chatApplication.showTeams = true;
      chatApplication.showTeamsHistory = !chatApplication.showTeamsHistory;
      jzStoreParam("chatShowTeams"+chatApplication.username, true, 600000);
    }
    chatApplication.showRooms(chatApplication.rooms);

  });

  jqchat("#chat-users .btn-offline").on("click", function() {
    chatApplication.showPeople = true;
    jzStoreParam("chatShowPeople"+chatApplication.username, true, 600000);
    chatApplication.showOffline = !chatApplication.showOffline;
    jzStoreParam("chatShowOffline"+chatApplication.username, chatApplication.showOffline, 600000);
    chatApplication.showRooms(chatApplication.rooms);
  });

jqchat('#back').on("click", function() {

    jqchat(".uiLeftContainerArea").addClass("displayContent");
    jqchat(".uiLeftContainerArea").removeClass("hideContent");

    jqchat(".uiGlobalRoomsContainer").addClass("hideContent").removeClass("displayContent");

    setTimeout(function(){
         jqchat(".uiGlobalRoomsContainer").css("display", "none");
    }, 500);
    jqchat("#chat-video-button").attr("style", "");
});
  jqchat('#chat-users .users-online > td:nth-child(1),#chat-users .users-online > td:nth-child(2)').on("click", function() {
    if(window.innerWidth <= 767){

        jqchat("#chat-application .uiGrayLightBox .uiSearchInput").removeClass("displayContent");
        jqchat('input#chat-search.input-with-value.span4').val('');
        var filter = jqchat('input#chat-search.input-with-value.span4').val();
        chatApplication.search(filter);


        var $chatStatusPanel = jqchat(".chat-status-panel");

        $chatStatusPanel.css("display", "none");
        jqchat(" .chat-status-chat").parent().removeClass('active');

        jqchat(".uiLeftContainerArea").removeClass("displayContent");
        jqchat(".uiLeftContainerArea").addClass("hideContent");
        jqchat(".uiGlobalRoomsContainer").css("display", "block");


        setTimeout(function(){
            jqchat(".uiGlobalRoomsContainer").addClass("displayContent").removeClass("hideContent");
        }, 200);

        $serachText = jqchat('#chat-search').attr('placeholder');
        $serachText = $serachText.replace("@", "");
        jqchat("#chat-search").attr("placeholder", $serachText);

    }

    thiss.targetUser = jqchat(".room-link:first",this).attr("user-data");
    thiss.targetFullname = jqchat(".room-link:first",this).attr("data-fullname");

    chatNotification.getStatus(thiss.targetUser, userRoomStatus);


    thiss.loadRoom();
    if (thiss.isMobileView()) {
      jqchat(".right-chat").css("display", "block");
      jqchat(".left-chat").css("display", "none");
      jqchat(".room-name").html(thiss.targetFullname);
    }
  });

  jqchat('#chat-users .users-online').on("mouseenter", function() {
    var $uiIconChatFavorite = jqchat(".uiIconChatFavorite", this);
    $uiIconChatFavorite.css("display", "block");
    $uiIconChatFavorite.css("margin-right", "1px");
  });

  jqchat('#chat-users .users-online').on("mouseleave", function() {
    var $uiIconChatFavorite = jqchat(".uiIconChatFavorite", this);
    $uiIconChatFavorite.css("display", "none");
  });

  jqchat('.user-status').on("click", function() {
    var targetFav = jqchat(this).attr("user-data");
    thiss.toggleFavorite(targetFav);
  });
  jqchat('#chat-users .user-favorite').on("click", function() {
    var targetFav = jqchat(this).attr("user-data");
    thiss.toggleFavorite(targetFav);
  });
};

/**
 * Search and filter (filter on users or spaces if starts with @
 * @param filter
 */
ChatApplication.prototype.search = function(filter) {
  if (filter == ":aboutme" || filter == ":about me") {
    this.showAboutPanel();
  }

  var index = filter.indexOf("@");
  if (index !== 0 || filter.length == 0) {
    this.chatRoom.highlight = filter;
    this.chatRoom.showMessages();
  }

  if (index === 0 || filter.length == 0) {
    var userFilter = filter.length == 0 ? filter : filter.substr(1, filter.length-1);
    if (userFilter == this.userFilter) {
      return;
    }
    this.userFilter = userFilter;
    this.filterInt = clearTimeout(this.filterInt);
    this.filterInt = setTimeout(jqchat.proxy(this.refreshWhoIsOnline, this), 500);
  }
};

/**
 * Check Browser Viewport Status
 * @returns {boolean}
 */
ChatApplication.prototype.checkViewportStatus = function() {
  return (jqchat("#NavigationPortlet").css("display")==="none");
};

ChatApplication.prototype.isMobileView = function() {
  return this.checkViewportStatus();
};

ChatApplication.prototype.isDesktopView = function() {
  return !this.checkViewportStatus();
};


/**
 * Set Current Status
 * @param status
 * @param callback
 */
ChatApplication.prototype.setStatus = function(status, callback) {

  if (status !== undefined) {
    // Update mongodb chat status

    jqchat.ajax({
      url: this.jzSetStatus,
      data: { "user": this.username,
        "status": status,
        "timestamp": new Date().getTime(),
        "dbName": this.dbName
      },
      headers: {
        'Authorization': 'Bearer ' + this.token
      },
      context: this,

      success: function(response){
        //console.log("SUCCESS:setStatus::"+response);
        chatNotification.changeStatusChat(response);
        if (typeof callback === "function") {
          callback(response);
        }

      },
      error: function(response){
        chatNotification.changeStatusChat("offline");
        if (typeof callback === "function") {
          callback("offline");
        }
      }
    });

    // Update platform user status
    var url = this.plfUserStatusUpdateUrl + this.username  + "?status=" + status;
    jqchat.ajax({
      url: url,
      type: 'PUT',
      context: this,

      success: function(response){
      },
      error: function(response){
      }
    });

  }

};

ChatApplication.prototype.showHelp = function() {
  jqchat('.help-modal').modal({"backdrop": false});
};

ChatApplication.prototype.showAsText = function() {

  this.chatRoom.showAsText(function(response) {
    //console.log("SUCCESS:setStatus::"+response);
    jqchat("#text-modal-area").html(response);
    jqchat('#text-modal-area').on("click", function() {
      this.select();
    });
    jqchat('.text-modal').modal({"backdrop": false});
  });

};

ChatApplication.prototype.setStatusAvailable = function() {
  chatApplication.setStatus("available");
};

ChatApplication.prototype.setStatusAway = function() {
  chatApplication.setStatus("away");
};

ChatApplication.prototype.setStatusDoNotDisturb = function() {
  chatApplication.setStatus("donotdisturb");
};

ChatApplication.prototype.setStatusInvisible = function() {
  chatApplication.setStatus("invisible");
};

/**
 * Send message to server
 * @param msg : the msg to send
 * @param callback : the method to execute on success
 */
ChatApplication.prototype.sendMessage = function(msg, callback) {

  var options = {};
  var context = {"msg": msg, "options": options, "callback": callback, "continueSend": true};

  this.trigger("beforeSend", context);
  if (!context.continueSend) {
    return;
  }
  msg = context.msg;
  options = context.options;
  callback = context.callback;

  var isSystemMessage = (msg.indexOf("/")===0 && msg.length>2) ;
  var sendMessageToServer = true;
  if (isSystemMessage) {
    sendMessageToServer = false;
    if (msg.indexOf("/me")===0) {
//      msg = msg.replace("/me", this.fullname);
      options.type = "type-me";
      options.username = this.username;
      options.fullname = this.fullname;
      sendMessageToServer = true;
    } else if (msg.indexOf("/terminate")===0) {
      if (msg.indexOf("/terminate")===0) {
        var optionsStop = {
          type: "type-meeting-stop",
          fromUser: chatApplication.username,
          fromFullname: chatApplication.fullname
        };
        this.chatRoom.sendMessage("", optionsStop, isSystemMessage, callback);
      }

      ts = Math.round(new Date().getTime() / 1000);
      msg = chatBundleData["exoplatform.chat.call.terminated"];
      options.timestamp = ts;
      options.type = "call-off";
      sendMessageToServer = true;
    } else if (msg.indexOf("/export")===0) {
      this.showAsText();
    } else if (msg.indexOf("/help")===0) {
      this.showHelp();
    }
  }

  if (sendMessageToServer) {
    this.chatRoom.sendMessage(msg, options, isSystemMessage, callback);
  }
  jqchat("#msg").val("");
};



/**
 ##################                           ##################
 ##################                           ##################
 ##################   CHAT PANELS             ##################
 ##################                           ##################
 ##################                           ##################
 */

ChatApplication.prototype.hidePanel = function(panel) {
  var $uiPanel = jqchat(panel);
  $uiPanel.width(jqchat('#chat-application').width()+40);
  $uiPanel.height(jqchat('#chat-application').height());
  $uiPanel.css("display", "none");
  $uiPanel.html("");
};

ChatApplication.prototype.hidePanels = function() {
  this.hidePanel(".chat-sync-panel");
  this.hidePanel(".chat-error-panel");
  this.hidePanel(".chat-login-panel");
  this.hidePanel(".chat-about-panel");
  this.hidePanel(".chat-demo-panel");
};

ChatApplication.prototype.showSyncPanel = function() {
  if (!this.isLoaded) {
    this.hidePanels();
    var $chatSyncPanel = jqchat(".chat-sync-panel");
    var marginTop = Math.round($chatSyncPanel.height()/2)-32;
    $chatSyncPanel.html("<img src=\"/chat/img/sync.gif\" width=\"64px\" class=\"chatSync\" style=\"margin-top: "+marginTop+"px;\" />");
    $chatSyncPanel.css("display", "block");
  }
};

ChatApplication.prototype.showErrorPanel = function() {
  this.whoIsOnlineMD5 = "";
  this.hidePanels();
  //console.log("show-error-panel");
  var $chatErrorPanel = jqchat(".chat-error-panel");
  $chatErrorPanel.html(chatBundleData["exoplatform.chat.panel.error1"]+"<br/><br/>"+chatBundleData["exoplatform.chat.panel.error2"]);
  $chatErrorPanel.css("display", "block");
};

ChatApplication.prototype.showLoginPanel = function() {
  this.hidePanels();
  //console.log("show-login-panel");
  var $chatLoginPanel = jqchat(".chat-login-panel");
  $chatLoginPanel.html(chatBundleData["exoplatform.chat.panel.login1"]+"<br><br><a href=\"#\" onclick=\"javascript:reloadWindow();\">"+chatBundleData["exoplatform.chat.panel.login2"]+"</a>");
  $chatLoginPanel.css("display", "block");
};

ChatApplication.prototype.showAboutPanel = function() {
  var about = "eXo Chat<br>";
  about += "Version "+chatBundleData["version"]+"<br><br>";
  about += chatBundleData["exoplatform.chat.designed"]+" <a href=\"mailto:bpaillereau@exoplatform.com\">Benjamin Paillereau</a><br>";
  about += chatBundleData["exoplatform.chat.for"]+" <a href=\"http://www.exoplatform.com\" target=\"_blank\">eXo Platform 4</a><br><br>";
  about += chatBundleData["exoplatform.chat.sources"]+" <a href=\"https://github.com/exo-addons/chat-application\" target=\"_blank\">https://github.com/exo-addons/chat-application</a>";
  about += "<br><br><a href=\"#\" id=\"about-close-btn\" >"+chatBundleData["exoplatform.chat.close"]+"</a>";
  this.hidePanels();
  var $chatAboutPanel = jqchat(".chat-about-panel");
  $chatAboutPanel.html(about);
  $chatAboutPanel.width(jqchat('#chat-application').width()+40);
  $chatAboutPanel.height(jqchat('#chat-application').height());
  $chatAboutPanel.css("display", "block");

  var thiss = this;
  jqchat("#about-close-btn").on("click", function() {
    thiss.hidePanel('.chat-about-panel');
    jqchat('#chat-search, #chat-searchResp').attr("value", "");
  });
};

ChatApplication.prototype.showDemoPanel = function() {
  this.hidePanels();
  //console.log("show-demo-panel");
  var $chatDemoPanel = jqchat(".chat-demo-panel");
  var intro = chatBundleData["exoplatform.chat.panel.demo"];
  if (this.isPublic) intro = chatBundleData["exoplatform.chat.panel.public"];
  $chatDemoPanel.html(intro+"<br><br><div class='welcome-panel'>" +
    "<br><br>"+chatBundleData["exoplatform.chat.display.name"]+"&nbsp;&nbsp;<input type='text' id='anonim-name'>" +
    "<br><br>"+chatBundleData["exoplatform.chat.email"]+"&nbsp;&nbsp;<input type='text' id='anonim-email'></div>" +
    "<br><a href='#' id='anonim-save'>"+chatBundleData["exoplatform.chat.save.profile"]+"</a>");
  $chatDemoPanel.css("display", "block");

  jqchat("#anonim-save").on("click", function() {
    var fullname = jqchat("#anonim-name").val();
    var email = jqchat("#anonim-email").val();
    this.createDemoUser(fullname, email);
  });
};

ChatApplication.prototype.displayVideoCallOnChatApp = function () {
  if (typeof weemoExtension === 'undefined' || window.location.href.indexOf(chatApplication.portalURI + "chat") === -1) {
    return;
  }

  var isTurnOnWeemoCallButton = (
    (weemoExtension.isTurnOffForUser === "false" && (this.targetUser.indexOf("space-") === -1 && this.targetUser.indexOf("team-") === -1 && this.targetUser !== ""))
    || (weemoExtension.isTurnOffForGroupCall === "false" && (this.targetUser.indexOf("space-") !== -1 || this.targetUser.indexOf("team-") !== -1 && this.targetUser !== ""))
    );

  jqchat(".btn-weemo").unbind("click").one("click", function () {
    if (!jqchat(this).hasClass("disabled")) {
      if (isTurnOnWeemoCallButton) {
        var targetUser = chatApplication.targetUser.trim();
        var targetFullname = chatApplication.targetFullname.trim();
        if (targetUser.indexOf("space-") === -1
          && targetUser.indexOf("team-") === -1
          && targetUser !== ""
          && weemoExtension.hasOneOneCallPermission(targetUser) === "false") {
          eXo.ecm.VideoCalls.showReceivingPermissionInterceptor(targetFullname);
          chatApplication.setModalToCenter('#receive-permission-interceptor');
        } else {
          //sightCallExtension.createWeemoCall(targetUser, targetFullname, chatMessage);
          jzStoreParam("jzChatSend", chatApplication.jzChatSend);
          jzStoreParam("room", chatApplication.room);
          jzStoreParam("targetFullname", targetFullname);
          jzStoreParam("targetUser", targetUser);

          if (targetUser.indexOf("space-") === -1 && targetUser.indexOf("team-") === -1) {
            weemoExtension.showVideoPopup(chatApplication.portalURI + 'videocallpopup?callee=' + targetUser.trim() + '&mode=one&hasChatMessage=true');
          } else {
            var isSpace = (targetUser.indexOf("space-") !== -1);
            var spaceOrTeamName = targetFullname.toLowerCase().split(" ").join("_");

            jzStoreParam("isSpace", isSpace);
            weemoExtension.showVideoPopup(chatApplication.portalURI + 'videocallpopup?mode=host&isSpace=' + isSpace + "&spaceOrTeamName=" + spaceOrTeamName);
          }
        }
      } else {
        eXo.ecm.VideoCalls.showPermissionInterceptor();
        chatApplication.setModalToCenter('#permission-interceptor');
      }
    }
  });

  jqchat(".btn-weemo-conf").unbind("click").one("click", function () {
    if (!jqchat(this).hasClass("disabled")) {
      var targetUser = chatApplication.targetUser.trim();
      var targetFullname = chatApplication.targetFullname.trim();
      var isSpace = (targetUser.indexOf("space-") !== -1);
      var spaceOrTeamName = targetFullname.toLowerCase().split(" ").join("_");

      jzStoreParam("jzChatSend", chatApplication.jzChatSend);
      jzStoreParam("room", chatApplication.room);
      jzStoreParam("targetFullname", targetFullname);
      jzStoreParam("targetUser", targetUser);
      jzStoreParam("meetingPointId", weemoExtension.meetingPointId);
      weemoExtension.showVideoPopup(chatApplication.portalURI + 'videocallpopup?mode=attendee&isSpace=' + isSpace + "&spaceOrTeamName=" + spaceOrTeamName);
      //weemoExtension.joinWeemoCall(chatApplication.targetUser, chatApplication.targetFullname, chatMessage);
    }
  });

  function cbGetConnectionStatus(targetUser, activity) {
    if (targetUser.indexOf("space-") === -1 && targetUser.indexOf("team-") === -1) {
      if (activity !== "offline" && activity !== "invisible") {
        jqchat(".btn-weemo").removeClass("disabled");
        jqchat(".btn-weemo-conf").removeClass("disabled");
      } else {
        jqchat(".btn-weemo").addClass("disabled");
        jqchat(".btn-weemo-conf").addClass("disabled");
      }
    } else {
        jqchat(".btn-weemo").removeClass("disabled");
        //jqchat(".btn-weemo-conf").removeClass("disabled");
    }
  }

  chatNotification.getStatus(chatApplication.targetUser, cbGetConnectionStatus);

  setTimeout(function () {
    chatApplication.displayVideoCallOnChatApp();
    var chatMessage = JSON.parse( jzGetParam("chatMessage", '{}') );
      if ((chatMessage.url !== undefined) && (chatNotification !== undefined) && jzGetParam("isSightCallConnected",false) === "false"
        && (jzGetParam("callMode") === "one" || jzGetParam("callMode") === "host")) {
        var roomToCheck = chatMessage.room;

        chatNotification.checkIfMeetingStarted(roomToCheck, function(callStatus, recordStatus) {

            if (callStatus !== 1) { // Already terminated
               jzStoreParam("chatMessage", JSON.stringify({}));
               return;
            }

            // Also Update record status
            if (recordStatus === 1) {
                var options = {
                    type: "type-meeting-stop",
                    fromUser: chatNotification.username,
                    fromFullname: chatNotification.username
                };
                chatNotification.sendFullMessage(
                  chatMessage.user,
                  chatMessage.token,
                  chatMessage.targetUser,
                  roomToCheck,
                  "",
                  options,
                  "true"
                );
            }

            var options = {};
            options.timestamp = Math.round(new Date().getTime() / 1000);
            options.type = "call-off";
            chatNotification.sendFullMessage(
              chatMessage.user,
              chatMessage.token,
              chatMessage.targetUser,
              roomToCheck,
              chatBundleData["exoplatform.chat.call.terminated"],
              options,
              "true"
            );

            localStorage.removeItem("chatMessage");
            localStorage.removeItem("isSightCallConnected");
            localStorage.removeItem("callMode");
        });
    }
  }, 3000);
};

/**
 * Load Room Users : server call
 */
ChatApplication.prototype.loadRoomUsers = function() {
  var thiss = this;
  var roomUsersContainer = jqchat(".uiRoomUsersContainerArea");
  if (this.targetUser !== undefined) {
    roomUsersContainer.show();
	if(!roomUsersContainer.is(".room-users-collapsed")) {
		roomUsersContainer.addClass("room-users-collapsed");//need a default class for responsive
	}

    var roomUsersList = jqchat("#room-users-list");
    if(roomUsersList !== undefined) {
      // fetch room users
      chatApplication.getUsers(this.targetUser, function (jsonData) {
        var users = TAFFY(jsonData.users);

        // generate room users array
        var roomUsers = [];
        var sortedStatuses = ["available", "away", "donotdisturb", ["offline", "invisible"]];
        sortedStatuses.forEach(function(status) {
          users({status: status}).order("fullname").each(function (user) {
            roomUsers.push(user);
          });
        });

        // check if there are changes
        var roomUserHasChanged = false;
        if(roomUsers.length === thiss.chatRoom.users.length) {
          roomUserHasChanged = !roomUsers.every(function(roomUser, index) {
             return (roomUser.name == thiss.chatRoom.users[index].name
              && roomUser.fullname == thiss.chatRoom.users[index].fullname
              && roomUser.status == thiss.chatRoom.users[index].status);
          });
        } else {
          roomUserHasChanged = true;
        }

        // if the room users have changed, update the panel
        if(roomUserHasChanged === true) {
          thiss.chatRoom.users = roomUsers;

          // generate room users list DOM
          var html = "";
          roomUsers.forEach(function (user) {
            html += thiss.renderRoomUser(user, thiss.showRoomOfflinePeople);
          });
          roomUsersList.html(html);
        }

        // User Profile Popup initialize
        var portal = eXo.env.portal;
        var restUrl = window.location.origin + portal.context + '/' + portal.rest + '/social/people/getPeopleInfo/{0}.json';
        var usersContainers = jqchat(roomUsersList).find('.room-user');
        jqchat.each(usersContainers, function (idx, el) {
          var userId = jqchat(el).attr('data-name');

          jqchat(el).userPopup({
            restURL: restUrl,
            userId: userId,
            labels: {
              StatusTitle: chatBundleData["exoplatform.chat.user.popup.status"],
              Connect: chatBundleData["exoplatform.chat.user.popup.connect"],
              Confirm: chatBundleData["exoplatform.chat.user.popup.confirm"],
              CancelRequest: chatBundleData["exoplatform.chat.user.popup.cancel"],
              RemoveConnection: chatBundleData["exoplatform.chat.user.popup.remove.connection"]
            },
            content: false,
            defaultPosition: "left",
            keepAlive: true,
            maxWidth: "240px"
          });
        });

        // update nb of users in the room
        jqchat("#room-users-title-nb-users").html("(" + (users().count() - 1) + ")");
      }, false);
    }
  } else {
    // hide room users since the room id is not defined
    roomUsersContainer.hide();
  }
};

/**
 * Generate room user DOM
 * @param user User to render
 * @returns {string} The DOM representing the user
 */
ChatApplication.prototype.renderRoomUser = function(user, showOfflineUsers) {
  var html = "";
  if (user.name !== chatApplication.username) {
    html += "<div class='room-user' data-name='"+user.name+"'"
    if(!showOfflineUsers && (user.status == 'offline' || user.status == 'invisible')) {
      html += "style='display: none;'";
    }
    html += ">";
    html += "  <div class='msUserAvatar pull-left'>";
    html += "    <span class='msAvatarLink avatarCircle'><img onerror=\"this.src='/chat/img/user-default.jpg'\" src='/" + eXo.env.portal.rest +"/v1/social/users/" + user.name + "/avatar' alt='" + user.fullname + "'></span>";
    html += "  </div>";
    html += "  <div class='room-user-status pull-right'>";
    html += "    <i class='user-" + user.status + "'></i>";
    html += "  </div>";
    html += "  <div class='room-user-name'>" + user.fullname + "</div>";
    html += "</div>"
  }
  return html;
};

/**
 *
 * @param showPeople Show people if true, otherwise hide them
 */
ChatApplication.prototype.toggleOfflineRoomUsers = function(showPeople) {
  var offlineUsers = jqchat("#room-users-list").find(".room-user-status .user-offline, .room-user-status .user-invisible").parents(".room-user");
  if(showPeople == true) {
    offlineUsers.show();
  } else {
    offlineUsers.hide();
  }
};
