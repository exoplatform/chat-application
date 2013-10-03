/*************************************************************************
 *
 * WEEMO INC.
 *
 *  Weemo.js - v 4.0.867
 *  [2013] Weemo Inc.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Weemo Inc.
 * The intellectual and technical concepts contained
 * herein are proprietary to Weemo Inc.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Weemo Inc.
 */

var Weemo = function(pAppId, pToken, pType, pHap, pDebugLevel, pDisplayName) {
  // Private properties
  var version = "4.0.867";
  var state = "NOT_CONNECTED";
  var browser = '';
  var browserVersion = '';
  var os = 'Unknown OS';
  var protocol = "weemodriver-protocol";
  var wsUri = "wss://localhost:34679";
  var longpollUri = "https://localhost:34679?callback=?";
  var self = this;
  var longpollId = null;
  var token = pToken;
  var webAppId = pAppId;

  var debugLevel = 0;
  if(pDebugLevel != undefined) debugLevel = pDebugLevel;

  var weemoType = pType;

  var hap ='';
  if(pHap != undefined) hap = pHap;

  var displayName = '';
  if(pDisplayName != undefined) displayName = pDisplayName;

  var downloadTimeout = null;
  var downloadTimeoutValue = 0;

  if(browser == 'Explorer') {
    downloadTimeoutValue = 20000; // 15000
  } else {
    downloadTimeoutValue = 8000; // 6000 is not long enough
  }
  var pollingTimeout = 16000;
  var messageTimeout = 5000;
  var downloadUrl = '';
  var websock;
  var connectionDelay = 2000;
  var connectTimeout = null;
  var callObjects = new Array();
  var utils = new WeemoUtils();
  var calledContact = '';


  //  Public methods
  this.setToken = function(value) { token = value; };
  this.setWebAppId = function(value) { webAppId = value; };
  this.setDebugLevel = function(value) { debugLevel = value; };
  this.setDisplayName = function(value) { displayName = value; sm('setDisplayName'); };
  this.setWeemoType = function(value) { weemoType = value; };
  this.setHap = function(value) { hap = value; };

  this.getVersion = function() { return version; }; // Js version or wd version ?
  this.getDisplayName = function() { return displayName; };
  this.getStatus = function(uidStatus) { var obj = new Object(); obj.uidStatus = uidStatus; sm('getStatus', obj); };
  this.getToken = function() { return token; };
  this.getWebAppId = function() { return webAppId; };

  this.initialize = function() { sm('connect');  };
  this.authenticate = function(force) { if(force != undefined) { var obj = new Object(); obj.force = 1; sm('onReadyforauthentication', obj); } else { sm('connect'); }  };
  this.createCall = function(uidToCall, type, displayNameToCall) {
    var obj = new Object();
    obj.uidToCall = uidToCall;
    obj.type = type;
    obj.displayNameToCall = displayNameToCall;
    sm('createCall', obj);
  };
  this.coredump = function() { sendMessage('<coredump></coredump>'); };
  this.reset = function() { sendMessage('<reset></reset>'); };

  var sm = function(action, params, data) {

    if(data != undefined && data != '') {
      deb = 'WEEMODRIVER TO BROWSER >>>>> ' + data + ' | STATE = ' + state + ' | ACTION = ' + action + ' | TIME = ' + new Date().toLocaleTimeString();
    } else {
      deb = 'STATE = ' + state + ' | ACTION = ' + action + ' | TIME = ' + new Date().toLocaleTimeString();
    }
    debug(deb);

    switch(state) {
      case "NOT_CONNECTED":
      case "RECONNECT":
        if(action != "") {
          switch(action) {
            case 'not_connected':
            case 'connect':
              if(websock != null && websock != undefined) {
                debug('BROWSER WEBSOCKET READYSTATE : ' + websock.readyState);

                websock.onopen = null;
                websock.onclose = null;
                websock.onmessage = null;
                websock.onerror = null;

                websock = null;
              }

              if(downloadTimeout == null) {
                downloadTimeout = setTimeout(function() {
                  switch(hap) {
                    case 'ppr/':
                      downloadUrl = 'https://download-ppr.weemo.com/poc.php?urlreferer='+webAppId;
                      break;

                    case 'qualif/':
                      downloadUrl = 'https://download-qualif.weemo.com/poc.php?urlreferer='+webAppId;
                      break;

                    case 'dev/':
                      downloadUrl = 'https://download-dev.weemo.com/poc.php?urlreferer='+webAppId;
                      break;

                    default:
                      downloadUrl = 'https://download.weemo.com/poc.php?urlreferer='+webAppId;

                  }
                  debug('BROWSER >>>>> WeemoDriver not started | TIME : ' + new Date().toLocaleTimeString());
                  if(typeof(self.onWeemoDriverNotStarted) != undefined && typeof(self.onWeemoDriverNotStarted) == 'function') self.onWeemoDriverNotStarted(downloadUrl); }, downloadTimeoutValue);
              }
              connectTimeout = setTimeout(jQuery.proxy(createConnection,this), connectionDelay);
              break;

            case 'connected':
              clearTimeout(connectTimeout);
              clearTimeout(downloadTimeout);

              if(state=='RECONNECT') { state='CONNECTED_WEEMO_DRIVER'; sm('connect'); }
              else { state = 'CONNECTED_WEEMO_DRIVER'; }

              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('connectedWeemoDriver',0);
              break;

            case 'createCall':
            case 'getStatus':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('initializationIncomplete', 0); // Initialization not completed
              break;
          }
        }
        break;

      case "CONNECTED_WEEMO_DRIVER":
        if(action != "") {
          switch(action) {
            case 'onReadyforconnection':
            case 'connect':
              connect();
              state = 'AUTHENTICATING';
              break;

            case 'not_connected':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('disconnectedWeemoDriver',2);
              state = 'NOT_CONNECTED';

              sm('connect');
              break;

            case 'createCall':
            case 'getStatus':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('initializationIncomplete', 0); // Initialization not completed
              break;

            default:
          }
        }
        break;

      case "AUTHENTICATING":
        if(action != "") {
          switch(action) {

            case 'onConnectionFailed':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('disconnectedWeemoDriver',1);
              break;

            case 'onReadyforconnection':
              clearTimeout(connectTimeout);
              connect();
              break;

            case 'onReadyforauthentication':
              clearTimeout(connectTimeout);
              verifyUser(params.force);
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('connectedCloud',0);
              break;

            case 'onVerifiedUserOk':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('authenticated',0);
              break;

            case 'loggedasotheruser':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('loggedasotheruser',0);
              break;

            case 'audioOk':
            case 'audioNok':
            case 'sipNok':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler(action,0);
              break;

            case 'sipOk':
              callObjects.clear();
              state = 'CONNECTED';
              if(displayName != undefined && displayName != '' && displayName != null) sendDisplayName();
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler(action, 0);
              break;


            case 'onVerifiedUserNok':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('unauthenticated',0);
              state = 'CONNECTED_WEEMO_DRIVER';
              break;

            case 'not_connected':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('disconnectedWeemoDriver',2);
              state = 'RECONNECT';
              sm('connect');
              break;

            case 'createCall':
            case 'getStatus':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('initializationIncomplete', 0); // Initialization not completed
              break;

            case 'kicked':

              break;


            default:
          }
        }
        break;

      case "CONNECTED":
        if(action != "") {
          switch(action) {
            case 'createCall':
              if(displayName != "" && displayName != undefined) {
                if(params.uidToCall != undefined && params.uidToCall != "" && params.type != undefined && params.type != "" && params.displayNameToCall != undefined && params.displayNameToCall != "") {

                  if(params.type == 'host' || params.type == 'attendee') {
                    var mvs = params.uidToCall.substr(0,4);
                    if(mvs != 'nyc1' && mvs != 'par1' && mvs != 'ldn1' && mvs != 'ldn2' && mvs != 'nyc2') { params.uidToCall = 'nyc2'+params.uidToCall; }
                  }
                  calledContact = params.displayNameToCall;
                  sendMessage('<createcall uid="'+params.uidToCall+'" displayname="'+params.displayNameToCall+'" type="'+params.type+'"></createcall>');

                } else {
                  debug('uidToCall, type and displayNameToCall must be set');
                }
              } else {
                if(typeof(self.onErrorHandler) != undefined && typeof(self.onErrorHandler) == 'function') self.onErrorHandler('call', '2'); // Displayname is empty
              }
              break;

            case 'callCreated':
              if(params.createdCallId != -1) {
                var wc = new WeemoCall(params.createdCallId, params.direction, params.displayNameToCall, self);
                callObjects[params.createdCallId] = wc;

                if(params.direction == "out") {
                  if(calledContact == params.displayNameToCall) {
                    callObjects[params.createdCallId].accept();
                  }
                  calledContact = '';
                } else {
                  callObjects[params.createdCallId].status.call = 'incoming';
                  if(typeof(self.onCallHandler) != undefined && typeof(self.onCallHandler) == 'function') self.onCallHandler(callObjects[params.createdCallId], {type:'call', status:'incoming'});
                }
              }
              break;

            case 'kicked':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler(action,params);
              calledContact = '';
              state = "CONNECTED_WEEMO_DRIVER";
              break;


            case 'setDisplayName':
              sendDisplayName();
              break;

            case 'audioOk':
            case 'audioNok':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler(action,0);
              break;

            case 'sipNok':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('sipNok', 0);
              calledContact = '';
              state = 'CONNECTED_WEEMO_DRIVER';
              break;

            case 'set':
              if(params.name == 'displayName') {
                displayName = params.value;
              }
              if(typeof(self.onGetHandler) != undefined && typeof(self.onGetHandler) == 'function') self.onGetHandler(params.name, params.value);
              break;

            case 'getDisplayName':
              getDisplayNameInternal();
              break;

            case 'getStatus':
              getStatusInternal(params.uidStatus);
              break;

            case 'onCallStatusReceived':
              switch(params.type) {
                case 'call':
                  callObjects[params.id].status.call = params.status;
                  break;

                case 'video_local':
                  callObjects[params.id].status.video_local = params.status;
                  break;

                case 'video_remote':
                  callObjects[params.id].status.video_remote = params.status;
                  break;

                case 'sound':
                  callObjects[params.id].status.sound = params.status;
                  break;
              }

              if(typeof(self.onCallHandler) != undefined && typeof(self.onCallHandler) == 'function') self.onCallHandler(callObjects[params.id], {type:params.type, status: params.status});
              if(params.status == 'terminated') {
                callObjects.splice(params.id, 1);
              }
              break;

            case 'not_connected':
              calledContact = '';
              state = 'RECONNECT';
              sm('connect');
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('disconnectedWeemoDriver', 0);
              break;
          }
        }
        break;

      default:

    }

    // Error
    if(action == 'error') {
      debug('Error id : ' + params.message);

      switch(params.message) {

        case '1':
          state= 'CONNECTED_WEEMO_DRIVER';
          debug('Error message : Generic error');
          sm('connect');
          break;

        case '2':
          debug('Error message : Have to send a poll before');
          sm('connect');
          break;

        case '3':
          debug('Error message : Internal error');
          break;

        case '4':
          debug('Error message : long poll connection error');
          break;

        case '5':
          debug('Error message : Bad xml or bad command');
          break;

        case '6':
          debug('Error message : Weemo Driver not authenticated');
          state = 'CONNECTED_WEEMO_DRIVER';
          sm('connect');
          break;

        case '7':
          state= 'CONNECTED_WEEMO_DRIVER';
          if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('disconnectedCloud',1);
          debug('Error message : No network or proxy error');

          clearTimeout(connectTimeout);
          connectTimeout = setTimeout(jQuery.proxy(function() { sm('connect'); },this), 3000);

          break;

        case '8':
          state= 'CONNECTED_WEEMO_DRIVER';
          if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('disconnectedCloud',2);
          debug('Error message : Waiting for readyforconnection');

          clearTimeout(connectTimeout);
          connectTimeout = setTimeout(jQuery.proxy(function() { sm('connect'); },this), 3000);
          break;

        default:
          debug("Error message : General error. Please contact support.");
      }
    }
  };

  //  Private methods
  var createConnection = function() {
    if(browser == 'Explorer') {
      if(longpollId == null) longpollId = uniqid();
      polling();
    } else {
      try {
        if(typeof MozWebSocket == 'function') WebSocket = MozWebSocket;
        websock = new WebSocket(wsUri, protocol);
        websock.onopen = function(evt) { sm('connected'); debug('BROWSER >>>>> WEBSOCKET OPEN'); };
        websock.onclose = function(evt) { debug('BROWSER >>>>> WEBSOCKET CLOSE'); sm('not_connected'); };
        websock.onmessage = function(evt) { handleData(evt.data); };
        websock.onerror = function(evt) {  debug('BROWSER >>>>> WEBSOCKET ERROR'); };
      } catch(exception) {
        debug('BROWSER >>>>> WEBSOCKET EXCEPTION');
        debug(exception);
      }

    }
  };

  var setBrowserInfo = function() {
    if (navigator.userAgent.search("MSIE") >= 0){
      browser = 'Explorer';
      var position = navigator.userAgent.search("MSIE") + 5;
      var end = navigator.userAgent.search("; Windows");
      browserVersion = parseInt(navigator.userAgent.substring(position,end));
    }
    else if (navigator.userAgent.search("Chrome") >= 0){
      browser = 'Chrome';// For some reason in the browser identification Chrome contains the word "Safari" so when detecting for Safari you need to include Not Chrome
      var position = navigator.userAgent.search("Chrome") + 7;
      var end = navigator.userAgent.search(" Safari");
      browserVersion = parseInt(navigator.userAgent.substring(position,end));
    }
    else if (navigator.userAgent.search("Firefox") >= 0){
      browser = 'Firefox';
      var position = navigator.userAgent.search("Firefox") + 8;
      browserVersion = parseInt(navigator.userAgent.substring(position));
    }
    else if (navigator.userAgent.search("Safari") >= 0 && navigator.userAgent.search("Chrome") < 0){//<< Here
      browser = 'Safari';
      var position = navigator.userAgent.search("Version") + 8;
      var end = navigator.userAgent.search(" Safari");
      browserVersion = parseInt(navigator.userAgent.substring(position,end));
    }
    else if (navigator.userAgent.search("Opera") >= 0){
      browser = 'Opera';
      var position = navigator.userAgent.search("Version") + 8;
      browserVersion = parseInt(navigator.userAgent.substring(position));
    }
    else{
      browser = 'Other';
    }
  };
  var setOsInfo = function() {
    if (navigator.appVersion.indexOf("Win")!=-1) os="windows";
    if (navigator.appVersion.indexOf("Mac")!=-1) os="macos";
    if (navigator.appVersion.indexOf("X11")!=-1) os="unix";
    if (navigator.appVersion.indexOf("Linux")!=-1) os="linux";
  };
  var debug = function(txt) { if(window.console && debugLevel > 0) console.log(txt); };
  var uniqid = function() { var newDate = new Date; return (newDate.getTime()%(2147483648-1)); };
  var connect = function() {
    if(hap == undefined || hap == null || hap == '') sendMessage('<connect hap=""></connect>');
    else sendMessage('<connect hap="'+hap+'"></connect>');
  };



  var verifyUser = function(force) { if(force == 1)  sendMessage('<verifyuser token="'+token+'" urlreferer="'+webAppId+'" type="force"></verifyuser>'); else sendMessage('<verifyuser token="'+token+'" urlreferer="'+webAppId+'" type="'+weemoType+'"></verifyuser>'); };

  var sendDisplayName = function(){ sendMessage('<set displayname="'+displayName+'"></set>'); };
  var getStatusInternal = function (uidStatus)  { sendMessage('<get type="status" uid="'+uidStatus+'" />'); };
  var sendMessage = function(val, type) {
    var message = new String();
    if(val != "" && val != undefined) { message = val; }

    if(browser == 'Explorer') {
      jqXHR = jQuery.ajax(longpollUri, {
        timeout: messageTimeout,
        beforeSend: function() { },
        data: { command:longpollId+':'+message },
        dataType: "jsonp",
        success: function(data) {
          debug('BROWSER TO WEEMODRIVER >>>>>> '+message);
          data = utils.trim(data.x);
          var pos = utils.strpos(data, ":");
          if(pos !== false) {
            var responseId = data.substring(0, pos);
            data = data.substring(pos+1);
          }

          if(data != "" && data != undefined && responseId != undefined && responseId == longpollId) {
            try { handleData(data); }
            catch(err) { debug(err); }
          }
        },
        error: function (data) { debug(data); }
      });
    } else {
      if(websock != undefined && websock != null) {
        websock.send(message);
        debug('BROWSER TO WEEMODRIVER >>>>>> '+message);
      }
    }
  };
  var handleData = function(data) {
    var action = "";
    var params = new Object();

    xmlDoc = jQuery.parseXML(data);
    $xml = jQuery( xmlDoc );

    // Connected Node
    $connectedNode = $xml.find("connected");
    $connectedStatus = $connectedNode.text();
    $connectedType = $connectedNode.attr('type');

    // Disconnected Node
    $disconnectedNode = $xml.find("disconnected");
    $disconnectNode = $xml.find("disconnect");

    // Readyforauthentication Node
    $readyforauthenticationNode = $xml.find("readyforauthentication");

    // Readyforconnection Node
    $readyforconnectionNode = $xml.find("readyforconnection");

    // kicked Node
    $kickedNode = $xml.find("kicked");
    $kickedName = $kickedNode.attr("displayname");
    $kickedUrl = $kickedNode.attr("urlreferer");


    // verifieduser Node
    $verifieduserNode = $xml.find("verifieduser");
    $statusVerified = $verifieduserNode.text();

    //Status Node
    $status = $xml.find("status");
    $xmpp = $status.find('xmpp');
    $sip = $status.find('sip');
    $audio = $status.find('audio');

    //Set Node
    $set = $xml.find("set");
    $displayNameSet = $set.attr('displayname');
    $versionSet = $set.attr('version');
    $statusSet = $set.attr('status');
    $uidSet = $set.attr('uid');

    // CreatedCall Node
    $createdcall = $xml.find("createdcall");
    $idCreated = $createdcall.attr('id');
    $direction = $createdcall.attr('direction');
    $displayName = $createdcall.attr('displayname');

    // version Node
    $setversionNode = $xml.find("setversion");
    $version = $setversionNode.text();

    // statuscall Node
    $statuscall = $xml.find("statuscall");
    $id = $statuscall.attr('id');
    $call = $statuscall.find("call");
    $video_local = $statuscall.find('video_local');
    $video_remote = $statuscall.find('video_remote');
    $share_local = $statuscall.find('share_local');
    $share_remote = $statuscall.find('share_remote');
    $sound = $statuscall.find('sound');

    // Error Node
    $error = $xml.find("error");

    // Closing Node
    $closing = $xml.find("closing");


    if($error.length > 0) { action = "error"; params.message = $error.text();}

    if($connectedNode.length > 0) { // A virer
      if($connectedStatus == 'ok') {
        action = "onConnect";
      } else {
        action = "onConnectionFailed";
      }
    }
    if($readyforauthenticationNode.length > 0) { action = "onReadyforauthentication"; }
    if($readyforconnectionNode.length > 0) { action = "onReadyforconnection"; }
    if($statusVerified == "ko") { action = "onVerifiedUserNok"; }
    if($statusVerified == "ok") { action = "onVerifiedUserOk"; }
    if($statusVerified == "loggedasotheruser") { action = "loggedasotheruser"; }

    if($xmpp.length > 0 && $xmpp.text() == "ok") { action = "xmppOk"; }
    if($xmpp.length > 0 && $xmpp.text() == "ko") { action = "xmppNok"; }
    if($sip.length > 0 && $sip.text() == "ok") { action = "sipOk"; }
    if($sip.length > 0 && $sip.text() == "ko") { action = "sipNok"; }
    if($audio.length > 0 && $audio.text() == "ko") { action = "audioNok"; }
    if($audio.length > 0 && $audio.text() == "ok") { action = "audioOk"; }

    if($kickedNode.length > 0) {
      action = "kicked";
      params.kickedName = $kickedName;
      params.kickedUrl = $kickedUrl;
    }


    if($set.length > 0) {
      action = 'set';

      if($displayNameSet != undefined && $displayNameSet.length > 0) {
        params.name = 'displayName';
        params.value = $displayNameSet;
      }

      if($versionSet != undefined && $versionSet.length > 0) {
        params.name = 'version';
        params.value = $versionSet;
      }

      if($statusSet != undefined && $statusSet.length > 0 && $uidSet != undefined && $uidSet.length > 0) {
        params.name = 'status';
        params.value = $statusSet;
        params.uid = $uidSet;
      }
    }

    if($createdcall.length > 0 && $idCreated.length > 0) {
      params.createdCallId = $idCreated;
      params.direction = $direction;
      params.displayNameToCall = $displayName;
      action = "callCreated";
    }

    if($statuscall.length > 0) {
      action = "onCallStatusReceived";
      if($call.length > 0) {
        params.type = 'call';
        params.status = $call.text();
      }

      if($video_local.length > 0)  {
        params.type = 'video_local';
        params.status = $video_local.text();
      }

      if($video_remote.length > 0) {
        params.type = 'video_remote';
        params.status = $video_remote.text();
      }

      if($share_local.length > 0) {
        params.type = 'share_local';
        params.status = $share_local.text();

      }
      if($share_remote.length > 0) {
        params.type = 'share_remote';
        params.status = $share_remote.text();
      }
      if($sound.length > 0) {
        params.type = 'sound';
        params.status = $sound.text();
      }

      params.id = $id;
    }

    if(action != '') sm(action, params, data);
  };

  var polling = function() {
    jQuery.ajax(longpollUri, {
      data: {command:longpollId+":<poll></poll>"},
      dataType: "jsonp",
      timeout: pollingTimeout,
      beforeSend: function() { },
      success: function(data) {
        sm('connected');
        if(data != "" && data != undefined) {
          var pos = utils.strpos(data.x, ":");
          if(pos !== false) {
            var responseId = data.x.substring(0, pos);
            if(window.console) console.log(responseId);
            if(responseId == longpollId) {
              data.x = data.x.substring(pos+1);
              handleData(data.x);
              polling();
            }
          } else {
            polling();
          }
        }
      },
      error: function (data, textStatus, errorThrown) {
        sm('not_connected');
      }
    });
  };

  // WeemoCall Class
  var WeemoCall = function(callId, direction, dn, parent) {
    this.dn = dn;
    this.parent = parent;
    this.direction = direction;
    this.callId = callId;
    this.status = {call: null, video_remote: null, video_local: null, sound: null};

    this.accept = function() { controlCall(this.callId, 'call', 'start'); };
    this.hangup = function() { controlCall(this.callId, 'call', 'stop'); };
    this.videoStart = function() { controlCall(this.callId, 'video_local', 'start'); };
    this.videoStop = function() { controlCall(this.callId, 'video_local', 'stop'); };
    this.audioMute = function() { controlCall(this.callId, 'sound', 'mute'); };
    this.audioUnMute = function() { controlCall(this.callId, 'sound', 'unmute'); };
    this.settings = function() { controlCall(this.callId, 'settings', 'show'); };
    this.shareStart = function() { controlCall(this.callId, 'share_local', 'start'); };
    this.pip = function() { controlCall(this.callId, 'pip', 'show'); };
    this.noPip = function() { controlCall(this.callId, 'pip', 'hide'); };

    var controlCall = function(id, item, action) {	 sendMessage('<controlcall id="'+id+'"><'+item+'>'+action+'</'+item+'></controlcall>'); };
  };

  // Set browser vars
  setBrowserInfo();
  debug(version);
};




// Utils
WeemoUtils = function() {};
WeemoUtils.prototype.strpos = function(haystack, needle, offset) { var i = (haystack + '').indexOf(needle, (offset || 0)); return i === -1 ? false : i; };
WeemoUtils.prototype.trim = function(str, charlist) {
  i = 0;
  str += '';

  if (!charlist) { // default list
    whitespace = " \n\r\t\f\x0b\xa0\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u200b\u2028\u2029\u3000";
  } else {
    // preg_quote custom list
    charlist += ''; whitespace = charlist.replace(/([\[\]\(\)\.\?\/\*\{\}\+\$\^\:])/g, '$1');
  }

  l = str.length;
  for (i = 0; i < l; i++) { if (whitespace.indexOf(str.charAt(i)) === -1) {
    str = str.substring(i);
    break;
  }
  }
  l = str.length;
  for (i = l - 1; i >= 0; i--) {
    if (whitespace.indexOf(str.charAt(i)) === -1) {
      str = str.substring(0, i + 1); break;
    }
  }

  return whitespace.indexOf(str.charAt(0)) === -1 ? str : '';
};

// Add function to Array Object
Array.prototype.clear = function() {
  this.splice(0, this.length);
};
