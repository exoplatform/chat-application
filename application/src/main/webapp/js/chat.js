
var chatApplication = new ChatApplication();

(function($){

  $(document).ready(function(){

    /**
     * Init Chat
     */
    var $chatApplication = $("#chat-application");
    chatApplication.setJuzuLabelsElement($chatApplication);
    chatApplication.attachWeemoExtension(weemoExtension);

    chatApplication.username = $chatApplication.attr("data-username");
    chatApplication.token = $chatApplication.attr("data-token");
    var chatServerURL = $chatApplication.attr("data-chat-server-url");
    chatApplication.chatIntervalChat = $chatApplication.attr("data-chat-interval-chat");
    chatApplication.chatIntervalSession = $chatApplication.attr("data-chat-interval-session");
    chatApplication.chatIntervalStatus = $chatApplication.attr("data-chat-interval-status");
    chatApplication.chatIntervalUsers = $chatApplication.attr("data-chat-interval-users");

    chatApplication.publicModeEnabled = $chatApplication.attr("data-public-mode-enabled");
    var chatPublicMode = ($chatApplication.attr("data-public-mode")=="true");
    var chatView = $chatApplication.attr("data-view");
    chatApplication.chatFullscreen = $chatApplication.attr("data-fullscreen");
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
    chatApplication.jzChatUpdateUnreadMessages = chatServerURL+"/updateUnreadMessages";
    chatApplication.jzUsers = chatServerURL+"/users";
    chatApplication.jzDelete = chatServerURL+"/delete";
    chatApplication.jzEdit = chatServerURL+"/edit";
    chatApplication.jzSaveTeamRoom = chatServerURL+"/saveTeamRoom";
    chatApplication.room = "";

    chatApplication.initChat();
    chatApplication.initChatProfile();

    /**
     * Init Global Variables
     *
     */
    //needed for #chat text area
    var keydown = -1;
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
      chatApplication.updateUnreadMessages();
    });

    $('#msg').keydown(function(event) {
  //    console.log("keydown : "+ event.which+" ; "+keydown);
      if ( event.which == 18 ) {
        keydown = 18;
      }
    });

    $('#msg').keyup(function(event) {
      var msg = $(this).val();
  //    console.log("keyup : "+event.which + ";"+msg.length+";"+keydown);
      if ( event.which === 13 && keydown !== 18 && msg.length>1) {
        //console.log("sendMsg=>"+username + " : " + room + " : "+msg);
        if(!msg)
        {
          return;
        }
  //      console.log("*"+msg+"*");
        chatApplication.sendMessage(msg);

      }
      if ( keydown === 18 ) {
        keydown = -1;
      }
      if ( event.which === 13 && msg.length === 1) {
        document.getElementById("msg").value = '';
      }

    });



    $(".meeting-action-toggle").on("click", function() {
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
      if (toggleClass === "meeting-action-event-panel" && chatApplication.targetUser.indexOf("team-")>-1) {
        chatApplication.getUsers(chatApplication.targetUser, function (users) {
          $("#chat-file-target-user").val(users);
        }, true);
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
                              +'<div class="label">Drop your file here</div>'
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
                targetFullname: chatApplication.targetFullname
              },
              error: function(err, file) {
                switch(err) {
                  case 'BrowserNotSupported':
                    alert('browser does not support HTML5 drag and drop')
                    break;
                  case 'TooManyFiles':
                    // user uploaded more than 'maxfiles'
                    break;
                  case 'FileTooLarge':
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
                console.log("upload started : "+i+" : "+file+" : "+len);
                // a file began uploading
                // i = index => 0, 1, 2, 3, 4 etc
                // file is the actual file of the index
                // len = total files user dropped
              },
              uploadFinished: function(i, file, response, time) {
                console.log("upload finished : "+i+" : "+file+" : "+time+" : "+response.status+" : "+response.name);
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
                console.log("progress updated : "+i+" : "+file+" : "+progress);
                $("#dropzone").find('.bar').width(progress+"%");
                $("#dropzone").find('.bar').html(progress+"%");
                // this function is used for large files and updates intermittently
                // progress is the integer value of file being uploaded percentage to completion
              }
            });

          });


        }, true);

      }


    });

    function hideMeetingPanel() {
      $(".meeting-action-popup").css("display", "none");
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

    $(".meeting-close-panel").on("click", function() {
      hideMeetingPanel();
    });

    $(".share-link-button").on("click", function() {
      var $uiText = $("#share-link-text");
      var text = $uiText.val();
      if (text === "" || text === $uiText.attr("data-value")) {
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
      beforeSend: function() {
        $("#dropzone").find('.bar').width("0%");
        $("#dropzone").find('.bar').html("0%");
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
      if ($(this).val()!=="")
        $("#chat-file-submit").trigger("click");
    });

    $('.uiRightContainerArea').on('dragenter', function() {
      $("#meeting-action-upload-link").trigger("click");
    });

    $(".chat-status-chat").on("click", function() {
      var $chatStatusPanel = $(".chat-status-panel");
      if ($chatStatusPanel.css("display")==="none")
        $chatStatusPanel.css("display", "inline-block");
      else
        $chatStatusPanel.css("display", "none");
    });

    $("div.chat-menu").click(function(){
      var status = $(this).attr("status");
      chatApplication.setStatus(status, function() {
        $(".chat-status-panel").css('display', 'none');
      });
    });

    $(".msg-emoticons").on("click", function() {
      var $msgEmoticonsPanel = $(".msg-emoticons-panel");
      if ($msgEmoticonsPanel.css("display")==="none")
        $msgEmoticonsPanel.css("display", "inline-block");
      else
        $msgEmoticonsPanel.css("display", "none");
    });

    $(".emoticon-btn").on("click", function() {
      var sml = $(this).attr("data");
      $(".msg-emoticons-panel").css("display", "none");
      $msg = $('#msg');
      var val = $msg.val();
      if (val.charAt(val.length-1)!==' ') val +=" ";
      val += sml + " ";
      $msg.val(val);
      $msg.focusEnd();

    });

    $(".room-detail-fullname").on("click", function() {
      if (chatApplication.isMobileView()) {
        $(".uiLeftContainerArea").css("display", "block");
        $(".uiRightContainerArea").css("display", "none");
      }
    });


    $('#chat-search').keyup(function(event) {
      var filter = $(this).val();
      chatApplication.search(filter);
    });

    $(".create-task-button").on("click", function() {
      var username = $("#task-add-user").val();
      var fullname = $("#task-add-fullname").val();
      var task = $("#task-add-task").val();
      var dueDate = $("#task-add-date").val();
      if (username === "" || fullname === "" || task === "" || dueDate === "") {
        return;
      }

      $.ajax({
        url: chatApplication.jzCreateTask,
        data: {"username": username,
          "dueDate": dueDate,
          "task": task
        },
        success:function(response){

          var options = {
            type: "type-task",
            username: username,
            fullname: fullname,
            dueDate: dueDate,
            task: task
          };
          var msg = task;

          chatApplication.chatRoom.sendMessage(msg, options, "true");
          hideMeetingPanel();


        },
        error:function (xhr, status, error){
          console.log("error");
        }
      });

    });

    $('#task-add-user').keyup(function(event) {
      var prefix = "task";
      var filter = $(this).val();

      searchUsers(filter, prefix, event, true, function(name, fullname) {
        //addTeamUserLabel(name, fullname);
        console.log(name+" : "+fullname);
        $('#task-add-user').val(name);
        $('#task-add-fullname').val(fullname);
        $(".meeting-action-task-panel").trigger("click");
        setTimeout(hideResults, 100);
      });

    });

    var taskDate = $('#task-add-date').datepicker({
      onRender: function(date) {
        var nowTemp = new Date();
        var now = new Date(nowTemp.getFullYear(), nowTemp.getMonth(), nowTemp.getDate(), 0, 0, 0, 0);
        return date.valueOf() < now.valueOf() ? 'disabled' : '';
      }
    }).on('changeDate', function(ev){
        taskDate.hide();
    }).on('show', function(ev){
        jqchat('.datepicker').each(function (){
          var top = jqchat(this).position().top - 160;
          var left = jqchat(this).position().left - 230;
          jqchat(this).css("top", top+"px");
          jqchat(this).css("left", left+"px");
        });
    }).data('datepicker');

    $(".create-event-button").on("click", function() {
      var space = chatApplication.targetFullname;
      var summary = $("#event-add-summary").val();
      var startDate = $("#event-add-start-date").val();
      var startTime = $("#event-add-start-time").val();
      var endDate = $("#event-add-end-date").val();
      var endTime = $("#event-add-end-time").val();
      if (space === "" || startDate === "" || startTime === "" || endDate === "" || endTime === "") {
        return;
      }
      if (startTime==="all-day") startTime = "00:00";
      if (endTime==="all-day") endTime = "23:59";
      var users = "";
      var targetUser = chatApplication.targetUser;
      if (targetUser.indexOf("team-")>-1) {
        users = $("#chat-file-target-user").val();
      }


      $.ajax({
        url: chatApplication.jzCreateEvent,
        data: {"space": space,
          "users": users,
          "summary": summary,
          "startDate": startDate,
          "startTime": startTime,
          "endDate": endDate,
          "endTime": endTime
        },
        success:function(response){

          var options = {
            type: "type-event",
            summary: summary,
            space: space,
            startDate: startDate,
            startTime: startTime,
            endDate: endDate,
            endTime: endTime
          };
          var msg = summary;

          chatApplication.chatRoom.sendMessage(msg, options, "true");
          hideMeetingPanel();


        },
        error:function (xhr, status, error){
          console.log("error");
        }
      });

    });

    $("#event-add-start-time").on("change", function() {
      var time = $(this).val();
      var h = Math.round(time.split(":")[0]) + 1;
      var hh = h;
      if (h<10) hh = "0"+h;
      $("#event-add-end-time").val(hh+":"+time.split(":")[1]);
    });

    var startDate = $('#event-add-start-date').datepicker({
      onRender: function(date) {
        var nowTemp = new Date();
        var now = new Date(nowTemp.getFullYear(), nowTemp.getMonth(), nowTemp.getDate(), 0, 0, 0, 0);
        return date.valueOf() < now.valueOf() ? 'disabled' : '';
      }
    }).on('changeDate', function(ev){
        var newDate = new Date(ev.date);
        if (ev.date.valueOf() > endDate.date.valueOf()) {
          endDate.setValue(newDate);
        }
        startDate.setValue(newDate);
        startDate.hide();
    }).on('show', function(ev){
      jqchat('.datepicker').each(function (){
        var top = jqchat(this).position().top - 160;
        var left = jqchat(this).position().left - 230;
        jqchat(this).css("top", top+"px");
        jqchat(this).css("left", left+"px");
      });
    }).data('datepicker');

    var endDate = $('#event-add-end-date').datepicker({
      onRender: function(date) {
        var nowTemp = new Date();
        var now = new Date(nowTemp.getFullYear(), nowTemp.getMonth(), nowTemp.getDate(), 0, 0, 0, 0);
        return ( date.valueOf() < now.valueOf() || date.valueOf() < startDate.date.valueOf()) ? 'disabled' : '';
      }
    }).on('changeDate', function(ev){
        endDate.hide();
    }).on('show', function(ev){
      jqchat('.datepicker').each(function (){
        var top = jqchat(this).position().top - 160;
        var left = jqchat(this).position().left - 230;
        jqchat(this).css("top", top+"px");
        jqchat(this).css("left", left+"px");
      });
    }).data('datepicker');

    addTimeOptions("#event-add-start-time");
    addTimeOptions("#event-add-end-time");

    function addTimeOptions(id) {
      var select = $(id);
      for (var h=0 ; h<24 ; h++) {
        for (var m=0 ; m<60 ; m+=30) {
          var hh = h;
          var mm = m;
          if (h<10) hh = "0"+hh;
          if (m<10) mm = "0"+mm;
          var time = hh+":"+mm;
          select.append('<option value="'+time+'">'+time+'</option>');

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
          if ($(this).hasClass(prefix+"-user-selected")) {
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
          if (!done && $(this).hasClass(prefix+"-user-selected")) {
            done = true;
            $("."+prefix+"-user").removeClass(prefix+"-user-selected");
            if (isUp) {
              if (index === 0)
                $("."+prefix+"-user").last().addClass(prefix+"-user-selected");
              else
                $(this).prev().addClass(prefix+"-user-selected");
            } else {
              if (index === total-1)
                $("."+prefix+"-user").first().addClass(prefix+"-user-selected");
              else
                $(this).next().addClass(prefix+"-user-selected");
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

          users.order("fullname").limit(5).each(function (user, number) {
            $userResults.css("display", "block");
            if (user.status == "offline") user.status = "invisible";
            var classSel = "";
            if (number === 0) classSel = prefix+"-user-selected"
            html += "<div class='"+prefix+"-user "+classSel+"' data-name='"+user.name+"' data-fullname='"+user.fullname+"'>";
            html += "  <span class='"+prefix+"-user-logo'><img src='/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:"+user.name+"/soc:profile/soc:avatar' width='30px' style='width:30px;'></span>";
            html += "  <span class='chat-status-"+prefix+" chat-status-"+user.status+"'></span>";
            html += "  <span class='"+prefix+"-user-fullname'>"+user.fullname+"</span>";
            html += "  <span class='"+prefix+"-user-name'>"+user.name+"</span>";
            html += "</div>";
          });
          $userResults.html(html);

          $('.'+prefix+'-user').on("mouseover", function() {
            $("."+prefix+"-user").removeClass(prefix+"-user-selected");
            $(this).addClass(prefix+"-user-selected");
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

    function addTeamUserLabel(name, fullname) {
      var $usersList = $('.team-users-list');
      var html = $usersList.html();
      html += "<span class='label team-user-label' data-name='"+name+"'>"+fullname+"&nbsp;&nbsp;<i class='icon-remove icon-white team-user-remove'></i></span>";
      $usersList.html(html);
      var $teamAddUser = $('#team-add-user');
      $teamAddUser.val("");
      $teamAddUser.focus();
      var $userResults = $(".team-users-results");
      $userResults.css("display", "none");
      $userResults.html("");

      $(".team-user-remove").on("click", function() {
        $(this).parent().remove();
      });

    }

    function strip(html)
    {
      var tmp = document.createElement("DIV");
      tmp.innerHTML = html;
      return tmp.textContent||tmp.innerText;
    }



    $(".team-edit-button").on("click", function() {
      var $uitext = $("#team-modal-name");
      $uitext.val(chatApplication.targetFullname);
      $uitext.attr("data-id", chatApplication.targetUser);

      chatApplication.getUsers(chatApplication.targetUser, function (jsonData) {
        $(".team-user-label").remove();

        var users = TAFFY(jsonData.users);
        var users = users();
        users.order("fullname").each(function (user, number) {
          if (user.name !== chatApplication.username) {
            addTeamUserLabel(user.name, user.fullname);
          }
        });

        $('.team-modal').modal({"backdrop": false});
        $uitext.focus();

      });

    });

    $(".team-modal-cancel").on("click", function() {
      $('.team-modal').modal('hide');
      var $uitext = $("#team-modal-name");
      $uitext.val("");
      $uitext.attr("data-id", "---");
    });

    $(".team-modal-save").on("click", function() {
      var $uitext = $("#team-modal-name");
      var teamName = $uitext.val();
      var teamId = $uitext.attr("data-id");
      $('.team-modal').modal('hide');

      var users = chatApplication.username;
      $(".team-user-label").each(function(index) {
        var name = $(this).attr("data-name");
        users += ","+name;
      });

      chatApplication.saveTeamRoom(teamName, teamId, users, function(data) {
        var teamName = data.name;
        var roomId = "team-"+data.room;
        chatApplication.refreshWhoIsOnline(roomId, teamName);
      });

      $uitext.val("");
      $uitext.attr("data-id", "---");

    });


    $(".btn-weemo").on("click", function() {
      if (!$(this).hasClass("disabled"))
        chatApplication.createWeemoCall();
    });

    $(".btn-weemo-conf").on("click", function() {
      if (!$(this).hasClass("disabled"))
        chatApplication.joinWeemoCall();
    });

    $(".text-modal-close").on("click", function() {
      $('.text-modal').modal('hide');
    });

    $(".edit-modal-cancel").on("click", function() {
      $('.edit-modal').modal('hide');
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
      });

    });

    $('#edit-modal-area').keydown(function(event) {
  //    console.log("keydown : "+ event.which+" ; "+keydown);
      if ( event.which == 18 ) {
        keydownModal = 18;
      }
    });

    $('#edit-modal-area').keyup(function(event) {
      var id = $(this).attr("data-id");
      var msg = $(this).val();
  //    console.log("keyup : "+event.which + ";"+msg.length+";"+keydown);
      if ( event.which === 13 && keydownModal !== 18 && msg.length>1) {
        //console.log("sendMsg=>"+username + " : " + room + " : "+msg);
        if(!msg)
        {
          return;
        }
  //      console.log("*"+msg+"*");
        $(this).val("");
        $('.edit-modal').modal('hide');

        chatApplication.editMessage(id, msg, function() {
          chatApplication.chatRoom.refreshChat(true);
        });

      }
      if ( keydownModal === 18 ) {
        keydownModal = -1;
      }
      if ( event.which === 13 && msg.length === 1) {
        $(this).val('');
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

    //removeParametersFromLocation();


    String.prototype.endsWith = function(suffix) {
      return this.indexOf(suffix, this.length - suffix.length) !== -1;
    };



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
  this.isLoaded = false;
  this.labels = new JuzuLabels();
  this.weemoExtension = "";
  this.isPublic = false;
  this.publicModeEnabled = false;
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
  this.jzEdit = "";
  this.jzSaveTeamRoom = "";
  this.userFilter = "";    //not set
  this.chatIntervalChat = "";
  this.chatIntervalUsers = "";
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


}

/**
 * Set Labels
 * @param element : a dom element with data- labels
 */
ChatApplication.prototype.setJuzuLabelsElement = function(element) {
  this.labels.setElement(element);
};

/**
 * Attach Weemo Extension
 * @param weemoExtension WeemoExtension Object
 */
ChatApplication.prototype.attachWeemoExtension = function(weemoExtension) {
  this.weemoExtension = weemoExtension;
};


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
      "isPublic": this.isPublic
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
      this.labels.set("username", this.username);
      this.token = data.token;
      this.labels.set("token", this.token);


      jqchat(".label-user").html(fullname);
      jqchat(".avatar-image:first").attr("src", gravatar(email));
      this.hidePanels();

      this.refreshWhoIsOnline();

      if (this.isPublic) {
        this.targetUser = this.SUPPORT_USER;
        this.targetFullname = this.labels.get("label-support-fullname");
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
      "token": this.token,
      "timestamp": new Date().getTime()
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
      "token": this.token,
      "messageId": id
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
 * Edit the message with id with a new message
 *
 * @param id
 * @param newMessage
 * @param callback
 */
ChatApplication.prototype.editMessage = function(id, newMessage, callback) {
  jqchat.ajax({
    url: this.jzEdit,
    data: {"room": this.room,
      "user": this.username,
      "token": this.token,
      "messageId": id,
      "message": newMessage
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
    url: this.jzSaveTeamRoom,
    dataType: "json",
    data: {"teamName": teamName,
      "room": room,
      "users": users,
      "user": this.username,
      "token": this.token
    },

    success:function(response){
      //console.log("success");
      if (typeof callback === "function") {
        callback(response);
      }
    },

    error:function (xhr, status, error){

    }

  });

};

ChatApplication.prototype.resize = function() {
  if (chatApplication.chatFullscreen == "true") {
    jqchat("#PlatformAdminToolbarContainer").css("display", "none");
  }

  var $chatApplication = jqchat("#chat-application");
  var top = $chatApplication.offset().top;
  var height = jqchat(window).height();
  var off = 80;
  var heightChat = height - top - off;

  $chatApplication.height(heightChat);
  jqchat("#chats").height(heightChat - 105 - 61);
  jqchat("#chat-users").height(heightChat - 44);

};

/**
 * Init Chat Interval
 */
ChatApplication.prototype.initChat = function() {

  this.chatRoom = new ChatRoom(this.jzChatRead, this.jzChatSend, this.jzChatGetRoom, this.jzChatSendMeetingNotes, this.jzChatGetMeetingNotes, this.chatIntervalChat, this.isPublic, this.labels);
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
        var $labelUser = jqchat(".label-user");
        $labelUser.text(data.fullname);

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
      "token": this.token
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
      "token": this.token
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
    jqchat.ajax({
      url: this.jzChatWhoIsOnline,
      dataType: "json",
      data: { "user": this.username,
        "token": this.token,
        "filter": this.userFilter,
        "isAdmin": this.isAdmin,
        "timestamp": new Date().getTime()},
      context: this,
      success: function(response){
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



          this.updateTotal(Math.abs(response.unreadOffline)+Math.abs(response.unreadOnline)+Math.abs(response.unreadSpaces)+Math.abs(response.unreadTeams));
          if (window.fluid!==undefined) {
            if (this.totalNotif>0)
              window.fluid.dockBadge = this.totalNotif;
            else
              window.fluid.dockBadge = "";
            if (this.totalNotif>this.oldNotif && this.profileStatus !== "donotdisturb" && this.profileStatus !== "offline") {
              window.fluid.showGrowlNotification({
                title: this.labels.get("label-title"),
                description: this.labels.get("label-new-messages"),
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
                  this.labels.get("label-title"),
                  this.labels.get("label-new-messages")
                );

                notification.onclick = function () {
                  window.open("http://localhost:8080/portal/intranet/chat");
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
  /**
   * FAVORITES
   */
  out += "<tr class='header-room header-favorites'><td colspan='3' style='border-top: 0;'>";
  if (this.showFavorites) classArrow="uiIconArrowDown"; else classArrow = "uiIconArrowRight";
  out += "<div class='nav pull-left uiDropdownWithIcon'><div class='uiAction'><i class='uiIconEcmsFavorite uiIconEcmsLightGrey'></i></div></div>";
  out += chatApplication.labels.get("label-header-favorites");
  out += "<div class='nav pull-right uiDropdownWithIcon'><div class='uiAction'><i class='"+classArrow+" uiIconLightGray'></i></div></div>";
  out += '<span class="room-total total-favorites">No Favorite</span>';
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
      } else {
        if (Math.round(room.unreadTotal)>0) {
          totalFavorites += Math.round(room.unreadTotal);
        }
      }
    }
  });

  var xOffline = ""; if (chatApplication.showOffline) xOffline=" btn active";
  var xPeopleHistory = ""; if (chatApplication.showPeopleHistory) xPeopleHistory=" btn active";
  var xSpacesHistory = ""; if (chatApplication.showSpacesHistory) xSpacesHistory=" btn active";
  var xTeamsHistory = ""; if (chatApplication.showTeamsHistory) xTeamsHistory=" btn active";

  /**
   * USERS
   */
  out += "<tr class='header-room header-people'><td colspan='3'>";
  if (this.showPeople) classArrow="uiIconArrowDown"; else classArrow = "uiIconArrowRight";
  out += "<div class='nav pull-left uiDropdownWithIcon'><div class='uiAction'><i class='uiIconGroup uiIconLightGray'></i></div></div>";
  out += chatApplication.labels.get("label-header-people");
  out += '<span class="room-total total-people"></span>';
  out += "<div class='nav pull-right uiDropdownWithIcon'><div class='uiAction'><i class='"+classArrow+" uiIconLightGray'></i></div></div>";
  out += "<ul class='nav pull-right uiDropdownWithIcon btn-top-history btn-top-history-people' style='margin-right: 5px;'><li><div class='uiActionWithLabel btn-history"+xPeopleHistory+"' data-type='people' href='javaScript:void(0)' data-toggle='tooltip' title='Show/hide history'><i class='uiIconClock uiIconLightGray'></i></div></li></ul>";
  out += "<ul class='nav pull-right uiDropdownWithIcon btn-top-offline' style='margin-right: 5px;'><li><div class='uiActionWithLabel btn-offline"+xOffline+"' data-type='people' href='javaScript:void(0)' data-toggle='tooltip' title='Show/hide offline users'><i class='uiIconMembership uiIconLightGray'></i></div></li></ul>";
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
        } else {
          if (Math.round(room.unreadTotal)>0) {
            totalPeople += Math.round(room.unreadTotal);
          }
        }
      }
    }
  });

  /**
   * TEAMS
   */
  out += "<tr class='header-room header-teams'><td colspan='3'>";
  if (this.showTeams) classArrow="uiIconArrowDown"; else classArrow = "uiIconArrowRight";
  out += "<div class='nav pull-left uiDropdownWithIcon'><div class='uiAction'><i class='uiIconChatTeam uiIconChatLightGray'></i></div></div>";
  out += chatApplication.labels.get("label-header-teams");
  out += '<span class="room-total total-teams"></span>';
  out += "<div class='nav pull-right uiDropdownWithIcon'><div class='uiAction'><i class='"+classArrow+" uiIconLightGray'></i></div></div>";
  out += "<ul class='nav pull-right uiDropdownWithIcon btn-top-history btn-top-history-teams' style='margin-right: 5px;'><li><div class='uiActionWithLabel btn-history"+xTeamsHistory+"' data-type='team' href='javaScript:void(0)' data-toggle='tooltip' title='Show/hide history'><i class='uiIconClock uiIconLightGray'></i></div></li></ul>";
  out += "<ul class='nav pull-right uiDropdownWithIcon btn-top-add-actions' style='margin-right: 5px;'><li><div class='uiActionWithLabel btn-add-team' href='javaScript:void(0)' data-toggle='tooltip' title='Create a new team'><i class='uiIconSimplePlusMini uiIconLightGray'></i></div></li></ul>";
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
        } else {
          if (Math.round(room.unreadTotal)>0) {
            totalTeams += Math.round(room.unreadTotal);
          }
        }
      }
    }
  });


  /**
   * SPACES
   */
  out += "<tr class='header-room header-spaces'><td colspan='3'>";
  if (this.showSpaces) classArrow="uiIconArrowDown"; else classArrow = "uiIconArrowRight";
  out += "<div class='nav pull-left uiDropdownWithIcon'><div class='uiAction'><i class='uiIconChatSpace uiIconChatLightGray'></i></div></div>";
  out += chatApplication.labels.get("label-header-spaces");
  out += '<span class="room-total total-spaces"></span>';
  out += "<div class='nav pull-right uiDropdownWithIcon'><div class='uiAction'><i class='"+classArrow+" uiIconLightGray'></i></div></div>";
  out += "<ul class='nav pull-right uiDropdownWithIcon btn-top-history btn-top-history-spaces' style='margin-right: 5px;'><li><div class='uiActionWithLabel btn-history"+xSpacesHistory+"' data-type='space' href='javaScript:void(0)' data-toggle='tooltip' title='Show/hide history'><i class='uiIconClock uiIconLightGray'></i></div></li></ul>";
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
        } else {
          if (Math.round(room.unreadTotal)>0) {
            totalSpaces += Math.round(room.unreadTotal);
          }
        }
      }
    }
  });

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

ChatApplication.prototype.getRoomHtml = function(room, roomPrevUser) {
  var out = "";
  if (room.user!==roomPrevUser) {
    out += '<tr id="users-online-'+room.user.replace(".", "-")+'" class="users-online">';
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
    out +='" user-data="'+room.user+'" data-toggle="tooltip"';
    if (room.isFavorite == "true") {
      out += ' title="Remove from favorites"';
    } else {
      out += ' title="Add to favorites"';
    }
    out += '></i>';
    out += '</td>';
    out +=  '<td>';
    if (room.isActive=="true") {
      out += '<span user-data="'+room.user+'" room-data="'+room.room+'" class="room-link" data-fullname="'+room.escapedFullname+'">'+room.escapedFullname+'</span>';
    } else {
      out += '<span class="room-inactive">'+room.user+'</span>';
    }
    out += '</td>';
    out += '<td>';
    if (Math.round(room.unreadTotal)>0) {
      out += '<span class="room-total" style="float:right;" data="'+room.unreadTotal+'">'+room.unreadTotal+'</span>';
    }
    out += '</td>';
    out += '</tr>';
  }
  return out;
};

/**
 * Load Room : server call
 */
ChatApplication.prototype.loadRoom = function() {
  //console.log("TARGET::"+this.targetUser+" ; ISADMIN::"+this.isAdmin);
  if (this.targetUser!==undefined) {
    jqchat(".users-online").removeClass("info");
    if (this.isDesktopView()) {
      var $targetUser = jqchat("#users-online-"+this.targetUser.replace(".", "-"));
      $targetUser.addClass("info");
      jqchat(".room-total").removeClass("room-total-white");
      $targetUser.find(".room-total").addClass("room-total-white");
    }

    jqchat("#room-detail").css("display", "block");
    jqchat(".team-button").css("display", "none");
    jqchat(".target-user-fullname").text(this.targetFullname);
    if (this.targetUser.indexOf("space-")===-1 && this.targetUser.indexOf("team-")===-1)
    ////// USER
    {
      jqchat(".meeting-action-event").css("display", "none");
      jqchat(".meeting-action-task").css("display", "none");
//      jqchat(".meeting-actions").css("display", "none");
      jqchat(".target-avatar-link").attr("href", "/portal/intranet/profile/"+this.targetUser);
      jqchat(".target-avatar-image").attr("onerror", "this.src='/chat/img/Avatar.gif';");
      jqchat(".target-avatar-image").attr("src", "/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:"+this.targetUser+"/soc:profile/soc:avatar");
    }
    else if (this.targetUser.indexOf("team-")===-1)
    ////// SPACE
    {
//      jqchat(".meeting-actions").css("display", "inline-block");
      jqchat(".meeting-action-event").css("display", "block");
      jqchat(".meeting-action-task").css("display", "block");
      var spaceName = this.targetFullname.toLowerCase().split(" ").join("_");
      jqchat(".target-avatar-link").attr("href", "/portal/g/:spaces:"+spaceName+"/"+spaceName);
      jqchat(".target-avatar-image").attr("onerror", "this.src='/social-resources/skin/images/ShareImages/SpaceAvtDefault.png';");
      jqchat(".target-avatar-image").attr("src", "/rest/jcr/repository/social/production/soc:providers/soc:space/soc:"+spaceName+"/soc:profile/soc:avatar");
    }
    else
    ////// TEAM
    {

      jqchat.ajax({
        url: this.jzChatGetCreator,
        data: {"room": this.targetUser,
          "user": this.username,
          "token": this.token
        },
        context: this,
        success: function(response){
          //console.log("SUCCESS::getRoom::"+response);
          var creator = response;
          if (creator === this.username) {
            jqchat(".team-button").css("display", "block");
          }
        },
        error: function(xhr, status, error){
          //console.log("ERROR::"+xhr.responseText);
        }
      });
//      jqchat(".meeting-actions").css("display", "inline-block");
      jqchat(".meeting-action-event").css("display", "block");
      jqchat(".meeting-action-task").css("display", "block");
      jqchat(".target-avatar-link").attr("href", "#");
      jqchat(".target-avatar-image").attr("src", "/social-resources/skin/images/ShareImages/SpaceAvtDefault.png");
    }

    var thiss = this;
    this.chatRoom.init(this.username, this.token, this.targetUser, this.targetFullname, this.isAdmin, function(room) {
      thiss.room = room;
      var $msg = jqchat('#msg');
      $msg.removeAttr("disabled");
      if (thiss.weemoExtension.isConnected) {
        jqchat(".btn-weemo").removeClass('disabled');
      }
      if (thiss.isDesktopView()) $msg.focus();

    });

  }
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
  $chats.html('<span>'+out+'</span>');
  sh_highlightDocument();
  $chats.animate({ scrollTop: 20000 }, 'fast');

  jqchat(".msg-text").mouseover(function() {
    if (jqchat(".msg-actions", this).children().length > 0) {
      jqchat(".msg-date", this).css("display", "none");
      jqchat(".msg-actions", this).css("display", "inline-block");
    }
  });

  jqchat(".msg-text").mouseout(function() {
    jqchat(".msg-date", this).css("display", "inline-block");
    jqchat(".msg-actions", this).css("display", "none");
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
    var $uimsg = jqchat(this).siblings(".msg-data");
    var msgId = $uimsg.attr("data-id");
    var msgHtml = $uimsg.html();
    msgHtml = msgHtml.replace(eval("/<br>/g"), "\n");

    jqchat("#edit-modal-area").val(msgHtml);
    jqchat("#edit-modal-area").attr("data-id", msgId);
    jqchat('.edit-modal').modal({"backdrop": false});

  });

  jqchat(".send-meeting-notes").on("click", function () {
    var $this = $(this);
    jqchat(".meeting-notes").animate({
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
            jqchat(".meeting-notes").animate({
              opacity: "toggle"
            }, 3000);
          });
        }
      });

    });

  });

  jqchat(".save-meeting-notes").on("click", function () {
    var $this = $(this);
    jqchat(".meeting-notes").animate({
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
          console.log(response);
          jqchat.ajax({
            type: "POST",
            url: chatApplication.jzSaveWiki,
            data: {"targetFullname": chatApplication.targetFullname,
              "content": response
            },
            context: this,
            dataType: "json",
            success: function(data){
              console.log(data.path);
              if (data.path !== "") {
                var options = {
                  type: "type-link",
                  link: data.path,
                  from: chatApplication.username,
                  fullname: chatApplication.fullname
                };
                var msg = "Meeting Notes";

                chatApplication.chatRoom.sendMessage(msg, options, "true");

              }

              jqchat("#"+id).animate({
                opacity: "toggle"
              }, 3000 , function() {
                jqchat(".meeting-notes").animate({
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

/**
 * return a status if a meeting is started or not :
 * -1 : no meeting in chat history
 * 0 : meeting terminated
 * 1 : obgoing meeting
 *
 * @param callback (callStatus)
 */
ChatApplication.prototype.checkIfMeetingStarted = function(callback) {
  chatApplication.chatRoom.refreshChat(true, function(msgs) {
    var callStatus = -1; // -1:no call ; 0:terminated call ; 1:ongoing call
    for (var i=0 ; i<msgs.length-1 && callStatus === -1 ; i++) {
      var msg = msgs[i];
      var type = msg.options.type;
      if (type === "call-off") {
        callStatus = 0;
      } else if (type === "call-on") {
        callStatus = 1;
      }
    }
    if (callback !== undefined) {
      callback(callStatus);
    }
  });
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
      "token": this.token,
      "timestamp": new Date().getTime()
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
 * jQuery bindings on dom elements created by Who Is Online methods
 */
ChatApplication.prototype.jQueryForUsersTemplate = function() {
  var value = jzGetParam("lastUsername"+this.username);
  var thiss = this;

  if (value && this.firstLoad) {
    //console.log("firstLoad with user : *"+value+"*");
    this.targetUser = value;
    this.targetFullname = jzGetParam("lastFullName"+this.username);
    if (this.username!==this.ANONIM_USER) {
      this.loadRoom();
    }
    this.firstLoad = false;
  }

  if (this.isDesktopView() && this.targetUser!==undefined) {
    var $targetUser = jqchat("#users-online-"+this.targetUser.replace(".", "-"));
    $targetUser.addClass("info");
    jqchat(".room-total").removeClass("room-total-white");
    $targetUser.find(".room-total").addClass("room-total-white");
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
    jqchat(".team-user-label").remove();
    jqchat('.team-modal').modal({"backdrop": false});
    $uitext.focus();
  });

  jqchat(".btn-history").on("click", function() {
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

  jqchat(".btn-offline").on("click", function() {
    chatApplication.showPeople = true;
    jzStoreParam("chatShowPeople"+chatApplication.username, true, 600000);
    chatApplication.showOffline = !chatApplication.showOffline;
    jzStoreParam("chatShowOffline"+chatApplication.username, chatApplication.showOffline, 600000);
    chatApplication.showRooms(chatApplication.rooms);
  });


  jqchat('.users-online').on("click", function() {
    thiss.targetUser = jqchat(".room-link:first",this).attr("user-data");
    thiss.targetFullname = jqchat(".room-link:first",this).attr("data-fullname");
    thiss.loadRoom();
    if (thiss.isMobileView()) {
      jqchat(".right-chat").css("display", "block");
      jqchat(".left-chat").css("display", "none");
      jqchat(".room-name").html(thiss.targetFullname);
    }
  });


  jqchat('.room-link').on("click", function() {
    thiss.targetUser = jqchat(this).attr("user-data");
    thiss.targetFullname = jqchat(this).attr("data-fullname");
    thiss.loadRoom();
    if (thiss.isMobileView()) {
      jqchat(".uiRightContainerArea").css("display", "block");
      jqchat(".uiLeftContainerArea").css("display", "none");
//      jqchat(".room-name").html(thiss.targetFullname);
    }
  });

  jqchat('.user-status').on("click", function() {
    var targetFav = jqchat(this).attr("user-data");
    thiss.toggleFavorite(targetFav);
  });
  jqchat('.user-favorite').on("click", function() {
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
  if (filter.indexOf("@")!==0) {
    this.chatRoom.highlight = filter;
    this.chatRoom.showMessages();
  } else {
    this.userFilter = filter.substr(1, filter.length-1);
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
    //console.log("setStatus :: "+status);

    jqchat.ajax({
      url: this.jzSetStatus,
      data: { "user": this.username,
        "token": this.token,
        "status": status,
        "timestamp": new Date().getTime()
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

ChatApplication.prototype.createWeemoCall = function() {
  console.log("targetUser : "+chatApplication.targetUser);
  console.log("targetFullname   : "+chatApplication.targetFullname);

  var chatMessage = {
    "url" : chatApplication.jzChatSend,
    "user" : chatApplication.username,
    "fullname" : chatApplication.fullname,
    "targetUser" : chatApplication.targetUser,
    "room" : chatApplication.room,
    "token" : chatApplication.token
  };
  weemoExtension.createWeemoCall(chatApplication.targetUser, chatApplication.targetFullname, chatMessage);

  //this.weemoExtension.createWeemoCall(this.targetUser, this.fullname);
};

ChatApplication.prototype.joinWeemoCall = function() {
  console.log("targetUser : "+chatApplication.targetUser);
  console.log("targetFullname   : "+chatApplication.targetFullname);

  var chatMessage = {
    "url" : chatApplication.jzChatSend,
    "user" : chatApplication.username,
    "fullname" : chatApplication.fullname,
    "targetUser" : chatApplication.targetUser,
    "room" : chatApplication.room,
    "token" : chatApplication.token
  };
  weemoExtension.joinWeemoCall(chatMessage);
};

/**
 * Send message to server
 * @param msg : the msg to send
 * @param callback : the method to execute on success
 */
ChatApplication.prototype.sendMessage = function(msg, callback) {


  var isSystemMessage = (msg.indexOf("/")===0 && msg.length>2) ;
  var options = {};
  var sendMessageToServer = true;
  if (isSystemMessage) {
    sendMessageToServer = false;
    if (msg.indexOf("/me")===0) {
//      msg = msg.replace("/me", this.fullname);
      options.type = "type-me";
      options.username = this.username;
      options.fullname = this.fullname;
      sendMessageToServer = true;
    } else if (msg.indexOf("/call")===0) {
      this.createWeemoCall();
    } else if (msg.indexOf("/join")===0) {
      this.joinWeemoCall();
    } else if (msg.indexOf("/terminate")===0) {
      ts = Math.round(new Date().getTime() / 1000);
      msg = "Call terminated";
      options.timestamp = ts;
      options.type = "call-off";
      this.weemoExtension.setCallOwner(false);
      this.weemoExtension.setCallActive(false);
      sendMessageToServer = true;
      this.weemoExtension.hangup();
    } else if (msg.indexOf("/export")===0) {
      this.showAsText();
    } else if (msg.indexOf("/help")===0) {
      this.showHelp();
    }
  }

  jqchat("#msg").val("");
  if (sendMessageToServer) {
    this.chatRoom.sendMessage(msg, options, isSystemMessage, callback);
  }

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
  $chatErrorPanel.html(this.labels.get("label-panel-error1")+"<br/><br/>"+this.labels.get("label-panel-error2"));
  $chatErrorPanel.css("display", "block");
};

ChatApplication.prototype.showLoginPanel = function() {
  this.hidePanels();
  //console.log("show-login-panel");
  var $chatLoginPanel = jqchat(".chat-login-panel");
  $chatLoginPanel.html(this.labels.get("label-panel-login1")+"<br><br><a href=\"#\" onclick=\"javascript:reloadWindow();\">"+this.labels.get("label-panel-login2")+"</a>");
  $chatLoginPanel.css("display", "block");
};

ChatApplication.prototype.showAboutPanel = function() {
  var about = "eXo Chat<br>";
  about += "Version 0.8.0-CR1 (build 131018)<br><br>";
  about += "Designed and Developed by <a href=\"mailto:bpaillereau@exoplatform.com\">Benjamin Paillereau</a><br>";
  about += "for <a href=\"http://www.exoplatform.com\" target=\"_new\">eXo Platform 4</a><br><br>";
  about += "Sources available on <a href=\"https://github.com/exo-addons/chat-application\" target=\"_new\">https://github.com/exo-addons/chat-application</a>";
  about += "<br><br><a href=\"#\" id=\"about-close-btn\" >Close</a>";
  this.hidePanels();
  var $chatAboutPanel = jqchat(".chat-about-panel");
  $chatAboutPanel.html(about);
  $chatAboutPanel.width(jqchat('#chat-application').width()+40);
  $chatAboutPanel.height(jqchat('#chat-application').height());
  $chatAboutPanel.css("display", "block");

  var thiss = this;
  jqchat("#about-close-btn").on("click", function() {
    thiss.hidePanel('.chat-about-panel');
    jqchat('#chat-search').attr("value", "");
  });
};

ChatApplication.prototype.showDemoPanel = function() {
  this.hidePanels();
  //console.log("show-demo-panel");
  var $chatDemoPanel = jqchat(".chat-demo-panel");
  var intro = this.labels.get("label-panel-demo");
  if (this.isPublic) intro = this.labels.get("label-panel-public");
  $chatDemoPanel.html(intro+"<br><br><div class='welcome-panel'>" +
    "<br><br>"+this.labels.get("label-display-name")+"&nbsp;&nbsp;<input type='text' id='anonim-name'>" +
    "<br><br>"+this.labels.get("label-email")+"&nbsp;&nbsp;<input type='text' id='anonim-email'></div>" +
    "<br><a href='#' id='anonim-save'>"+this.labels.get("label-save-profile")+"</a>");
  $chatDemoPanel.css("display", "block");

  jqchat("#anonim-save").on("click", function() {
    var fullname = jqchat("#anonim-name").val();
    var email = jqchat("#anonim-email").val();
    this.createDemoUser(fullname, email);
  });
};
