/*************************************************************************
 *
 * WEEMO INC.
 *
 *  Weemo.js - v 2.0
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

Weemo = function() {
  // Private properties
  var version = "2.0";
  var state = "NOT_CONNECTED";
  var browser = '';
  var browserVersion = '';
  var protocol = "weemodriver-protocol";
  var wsUri = "wss://localhost:34679";
  var longpollUri = "https://localhost:34679?callback=?";
  var openedWebSockets = 0;
  var self = this;
  var myId = null;
  var uid = '';
  var apikey = '';
  var mode = '';
  var platform = 'p1.weemo.com';
  var domain = 'weemo-poc.com';
  var pwd = '';
  var displayname = '';
  var downloadTimeout = null;
  var downloadTimeoutValue = 15000;
  var pollingTimeout = 16000;
  var messageTimeout = 5000;
  var environment = 'production';
  var downloadUrl = '';
  var websock;

  //  Public methods
  this.setUid = function(value) { uid = value; };
  this.setApikey = function(value) { apikey = value; };
  this.setMode = function(value) { mode = value; };
  this.setDomain = function(value) { domain = value; };
  this.setDisplayname = function(value) { displayname = value; sm('setDisplayname'); };

  this.setPlatform = function(value) { platform = value; };
  this.setEnvironment = function(value) { environment = value; };
  this.getVersion = function() { return version; };
  this.getDisplayname = function() { return displayname; };
  this.getStatus = function(uidStatus, keyStatus, platformStatus) {
    var obj = new Object();
    obj.uidStatus = uidStatus;
    obj.keyStatus = keyStatus;
    obj.platformStatus = platformStatus;
    sm('getStatus', obj); };
  this.getUid = function() { return uid; };

  this.connectToWeemoDriver = function() { sm('connect');  };
  this.connectToTheCloud = function() { sm('connect');  };
  this.createCall = function(uidToCall, type, displaynameToCall, key) {
    var obj = new Object();
    obj.uidToCall = uidToCall;
    obj.type = type;
    obj.displaynameToCall = displaynameToCall;
    obj.key = key;
    sm('createCall', obj);
  };

  //this.disconnect = function() { sm('disconnect');  };
  this.setPwd = function(value) { pwd = value; };

  var sm = function(action, params, data) {

    if(data != undefined && data != '') {
      deb = 'WEEMODRIVER TO BROWSER >>>>> ' + data + ' | STATE = ' + state + ' | ACTION = ' + action;
    } else {
      deb = 'STATE = ' + state + ' | ACTION = ' + action;
    }
    debug(deb);

    switch(state) {
      case "NOT_CONNECTED":
      case "RECONNECT":
        if(action != "") {
          switch(action) {
            case 'not_connected':
            case 'connect':
              if(state == 'RECONNECT' && websock != null && websock != undefined) {
                debug('BROWSER WEBSOCKET READYSTATE : ' + websock.readyState);

                /*websock.onopen = null;
                 websock.onopen = null;
                 websock.onopen = null;
                 websock.onopen = null;*/

              }

              if(downloadTimeout == null) {
                downloadTimeout = setTimeout(function() {
                  switch(environment) {
                    case 'production':
                      downloadUrl = 'https://download.weemo.com/poc.php?apikey='+apikey+'&domain_name='+domain;
                      break;

                    case 'staging':
                      downloadUrl = 'https://download-ppr.weemo.com/poc.php?apikey='+apikey+'&domain_name='+domain;
                      break;

                    case 'testing':
                      downloadUrl = 'https://download-qual.weemo.com/poc.php?apikey='+apikey+'&domain_name='+domain;
                      break;

                    case 'development':
                      downloadUrl = 'https://download-dev.weemo.com/poc.php?apikey='+apikey+'&domain_name='+domain;
                      break;

                    default:
                      downloadUrl = 'https://download.weemo.com/poc.php?apikey='+apikey+'&domain_name='+domain;

                  }
                  debug('BROWSER >>>>> WeemoDriver not started');
                  if(typeof(self.onWeemoDriverNotStarted) != undefined && typeof(self.onWeemoDriverNotStarted) == 'function') self.onWeemoDriverNotStarted(downloadUrl); }, downloadTimeoutValue);
              }
              createConnection();
              break;

            case 'connected':
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
              break;

            case 'onConnect':
              //if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('connectedWeemoDriver',0);
              break;

            case 'onConnectionFailed':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('disconnectedWeemoDriver',1);
              break;

            case 'onReadyforauthentication':
              controlUser();
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('connectedCloud',0);
              break;

            case 'onVerifiedUserOk':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('verifiedUserOk',0);
              break;

            case 'audioOk':
            case 'audioNok':
            case 'sipNok':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler(action,0);
              break;

            case 'sipOk':
              state = 'CONNECTED';
              if(displayname != undefined && displayname != '' && displayname != null) sendDisplayname();
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler(action, 0);
              break;


            case 'onVerifiedUserNok':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('verifiedUserNok',0);
              break;

            case 'not_connected':
              state = 'RECONNECT';
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('disconnectedWeemoDriver',2);
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

      case "CONNECTED":
        if(action != "") {
          switch(action) {
            case 'createCall':
              if(displayname != "" && displayname != undefined) {
                if(params.uidToCall != undefined && params.uidToCall != "" && params.type != undefined && params.type != "" && params.displaynameToCall != undefined && params.displaynameToCall != "") {
                  if(params.uidToCall == uid && (params.type == 'internal' || params.type == 'external')) {
                    if(typeof(self.onCallHandler) != undefined && typeof(self.onCallHandler) == 'function') self.onCallHandler('error', '1'); // Uid to call must be different of your own uid
                  } else {
                    createCallInternal(params.uidToCall, params.type, params.displaynameToCall, params.key);
                    state = "CREATE_CALL";
                  }
                } else {
                  debug('uidToCall, type and displaynameToCall must be set');
                }
              } else {
                if(typeof(self.onCallHandler) != undefined && typeof(self.onCallHandler) == 'function') self.onCallHandler('error', '2'); // Displayname is empty
              }
              break;

            case 'setDisplayname':
              sendDisplayname();
              break;

            case 'audioOk':
            case 'audioNok':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler(action,0);
              break;



            case 'sipNok':
              state = 'CONNECTED_WEEMO_DRIVER';
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('sipNok', 0);
              break;

            case 'set':
              if(params.name == 'displayname') {
                displayname = params.value;
              }
              if(typeof(self.onGetHandler) != undefined && typeof(self.onGetHandler) == 'function') self.onGetHandler(params.name, params.value);
              break;

            case 'getDisplayname':
              getDisplaynameInternal();
              break;

            case 'getStatus':
              getStatusInternal(params.uidStatus, params.keyStatus, params.platformStatus);
              break;

            case 'onCallStatusReceived':
              if(typeof(self.onCallHandler) != undefined && typeof(self.onCallHandler) == 'function') self.onCallHandler(params.type, params.status);
              break;

            case 'not_connected':
              state = 'RECONNECT';
              sm('connect');
              break;
          }
        }
        break;

      case "CREATE_CALL":
        if(action != "") {
          switch(action) {
            case 'onCallCreated':
              state = 'CONNECTED';
              if(params.createdCallId == "-1") {
                if(typeof(self.onCallHandler) != undefined && typeof(self.onCallHandler) == 'function') self.onCallHandler('error', '3'); // Call cannot be created
              } else {
                controlCall(params.createdCallId, 'call', 'start');
              }
              break;

            case 'createCall':
              if(typeof(self.onCallHandler) != undefined && typeof(self.onCallHandler) == 'function') self.onCallHandler('error', '4'); // Call ongoing
              break;

            case 'setDisplayname':
              sendDisplayname();
              break;

            case 'audioOk':
            case 'audioNok':
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler(action,0);
              break;

            case 'sipNok':
              state = 'CONNECTED_WEEMO_DRIVER';
              if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('sipNok', 0);
              break;

            case 'set':
              if(params.name == 'displayname') {
                displayname = params.value;
              }
              if(typeof(self.onGetHandler) != undefined && typeof(self.onGetHandler) == 'function') self.onGetHandler(params.name, params.value);
              break;

            case 'getDisplayname':
              getDisplaynameInternal();
              break;

            case 'getStatus':
              getStatusInternal(params.uidStatus, params.keyStatus, params.platformStatus);
              break;

            case 'onCallStatusReceived':
              if(typeof(self.onCallHandler) != undefined && typeof(self.onCallHandler) == 'function') self.onCallHandler(params.type, params.status);
              break;

            case 'not_connected':
              state = 'RECONNECT';
              sm('connect');
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
        case '2':
        case '3':
        case '4':
        case '5':
        case '8':
        case '9':
        case '10':
        case '11':
        case '12':
        case '13':
        case '14':
        case '15':
        case '16':
        case '17':
          debug('Error message : Verify user error');
          break;

        case '6':
          debug('Error message : Bad authentication');
          break;

        case '7':
          debug('Error message : Bad Apikey');
          break;

        case '18':
          state= 'CONNECTED_WEEMO_DRIVER';
          if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('disconnectedCloud',1);
          debug('Error message : No network or proxy error');
          sm('connect');

          break;

        case '19':
          state= 'CONNECTED_WEEMO_DRIVER';
          if(typeof(self.onConnectionHandler) != undefined && typeof(self.onConnectionHandler) == 'function') self.onConnectionHandler('disconnectedCloud',2);
          debug('Error message : Waiting for readyforconnection');
          sm('connect');
          break;

        case '20':
          state= 'CONNECTED_WEEMO_DRIVER';
          debug('Error message : Generic error');
          sm('connect');
          break;

        case '21':
          debug('Error message : Have to send a poll before');
          sm('connect');
          break;

        case '22':
          debug('Error message : Internal error');
          break;

        case '24':
          debug('Error message : Bad xml or bad command');

          break;

        case '25':
          debug('Error message : Weemo Driver not authenticated');
          state = 'CONNECTED_WEEMO_DRIVER';
          sm('connect');
          break;

        default:
          debug("Error message : General error. Please contact support.");
      }
      //if(typeof(self.onErrorHandler) != undefined && typeof(self.onErrorHandler) == 'function') self.onErrorHandler(params.message);
    }
  };

  //  Private methods
  var createConnection = function() {
    if(browser == 'Explorer' && browserVersion < 10) {
      if(myId == null) myId = uniqid();
      polling();
    } else {
      //debug('BROWSER >>>>> ' + openedWebSockets);
      if(openedWebSockets <= 0) {
        try {
          if(typeof MozWebSocket == 'function') WebSocket = MozWebSocket;
          websock = new WebSocket(wsUri, protocol);
          //debug('BROWSER >>>>> CREATE WEBSOCKET');
          openedWebSockets=openedWebSockets+1;
          websock.onopen = function(evt) { debug('OPENED WEBSOCKETS >>>>> ' + openedWebSockets); sm('connected'); debug('BROWSER >>>>> WEBSOCKET OPEN'); };
          websock.onclose = function(evt) { openedWebSockets = 0; debug('BROWSER >>>>> WEBSOCKET CLOSE'); sm('not_connected'); };
          websock.onmessage = function(evt) { handleData(evt.data); };
          websock.onerror = function(evt) {  debug('BROWSER >>>>> WEBSOCKET ERROR'); };
        } catch(exception) {
          debug('BROWSER >>>>> WEBSOCKET EXCEPTION');
          debug(exception);
        }
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
  var debug = function(txt) { if(window.console && mode == 'debug') console.log(txt); };
  var uniqid = function() { var newDate = new Date; return (newDate.getTime()%(2147483648-1)); };
  var connect = function() { if(platform == undefined || platform == null || platform == '') { platform = 'p1.weemo.com'; } sendMessage('<connect techdomain="'+platform+'"></connect>'); };
  var getVersion = function() { sendMessage('<getversion/>'); };
  var showWindow = function(winid) { sendMessage('<showwindow window="'+winid+'"></showwindow>'); };
  var reset = function() { sendMessage('<reset></reset>'); };
  var controlUser = function() { sendMessage('<verifyuser uid="'+uid+'" apikey="'+apikey+'" token="'+pwd+'" provdomain="'+domain+'"></verifyuser>'); };
  var controlCall = function(id, item, action) {	 sendMessage('<controlcall id="'+id+'"><'+item+'>'+action+'</'+item+'></controlcall>'); };
  var sendDisplayname = function(){ sendMessage('<set displayname="'+displayname+'"></set>'); };
  var getDisplaynameInternal = function(){ sendMessage('<get type="displayname"></get>'); };
  var getStatusInternal = function (uidStatus, keyStatus, platformStatus)  { sendMessage('<get type="status" uid="'+uidStatus+'" apikey="'+keyStatus+'" techdomain="'+platformStatus+'"/>')};
  var createCallInternal = function(uidToCall, type, displaynameToCall, key) {
    if(key=='' || key==undefined || key == null) { key = apikey; }
    if(type == 'host' || type == 'attendee') {
      var mvs = uidToCall.substr(0,4);
      if(mvs != 'nyc1' && mvs != 'par1') { uidToCall = 'nyc1'+uidToCall; }
    }
    sendMessage('<createcall uid="'+uidToCall+'" apikey="'+key+'" displayname="'+displaynameToCall+'" type="'+type+'"></createcall>');
  };
  var strpos = function(haystack, needle, offset) { var i = (haystack + '').indexOf(needle, (offset || 0)); return i === -1 ? false : i; };
  var trim = function(str, charlist) {
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
  var sendMessage = function(val, type) {
    var message = new String();
    if(val != "" && val != undefined) { message = val; }

    if(browser == 'Explorer' && browserVersion < 10) {
      jqXHR = jQuery.ajax(longpollUri, {
        timeout: messageTimeout,
        beforeSend: function() { },
        data: { command:myId+':'+message },
        dataType: "jsonp",
        success: function(data) {
          debug('BROWSER TO WEEMODRIVER >>>>>> '+message);
          data = trim(data.x);
          var pos = strpos(data, ":");
          if(pos !== false) { data = data.substring(pos+1); }

          if(data != "" && data != undefined) {
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

    // verifieduser Node
    $verifieduserNode = $xml.find("verifieduser");
    $uid = $verifieduserNode.attr('uid');
    $apikey = $verifieduserNode.attr('apikey');
    $provdomain = $verifieduserNode.attr('provdomain');
    $statusVerified = $verifieduserNode.text();

    //Status Node
    $status = $xml.find("status");
    $xmpp = $status.find('xmpp');
    $sip = $status.find('sip');
    $audio = $status.find('audio');

    //Set Node
    $set = $xml.find("set");
    $displaynameSet = $set.attr('displayname');
    $versionSet = $set.attr('version');
    $statusSet = $set.attr('status');
    $uidSet = $set.attr('uid');
    $apikeySet = $set.attr('apikey');
    $techdomainSet = $set.attr('techdomain');

    // CreatedCall Node
    $createdcall = $xml.find("createdcall");
    $idCreated = $createdcall.attr('id');
    //$jid = $createdcall.attr('jid');
    $direction = $createdcall.attr('direction');
    $displayname = $createdcall.attr('displayname');

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

    if($connectedNode.length > 0) {
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

    if($xmpp.length > 0 && $xmpp.text() == "ok") { action = "xmppOk"; }
    if($xmpp.length > 0 && $xmpp.text() == "ko") { action = "xmppNok"; }
    if($sip.length > 0 && $sip.text() == "ok") { action = "sipOk"; }
    if($sip.length > 0 && $sip.text() == "ko") { action = "sipNok"; }
    if($audio.length > 0 && $audio.text() == "ko") { action = "audioNok"; }
    if($audio.length > 0 && $audio.text() == "ok") { action = "audioOk"; }

    if($disconnectedNode.length > 0 || $disconnectNode.length > 0) { action = "close"; }

    if($closing.length > 0) { action = "closing"; }

    if($set.length > 0) {
      action = 'set';

      if($displaynameSet != undefined && $displaynameSet.length > 0) {
        params.name = 'displayname';
        params.value = $displaynameSet;
      }

      if($versionSet != undefined && $versionSet.length > 0) {
        params.name = 'version';
        params.value = $versionSet;
      }

      if($statusSet != undefined && $statusSet.length > 0 && $uidSet != undefined && $uidSet.length > 0 && $techdomainSet != undefined && $techdomainSet.length > 0) {
        params.name = 'status';
        params.value = $statusSet;
        params.uid = $uidSet;
        params.apikey = $apikeySet;
        params.platform = $techdomainSet;
      }
    }

    if($createdcall.length > 0 && $idCreated.length > 0 && $direction == 'out') {
      params.createdCallId = $idCreated;
      action = "onCallCreated";
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
    }

    if(action != '') sm(action, params, data);
  };

  var polling = function() {
    jQuery.ajax(longpollUri, {
      data: {command:myId+":<poll></poll>"},
      dataType: "jsonp",
      timeout: pollingTimeout,
      beforeSend: function() { },
      success: function(data) {
        sm('connected');

        if(data != "" && data != undefined) {
          var pos = strpos(data.x, ":");
          if(pos !== false) { data.x = data.x.substring(pos+1); }
          handleData(data.x);
          polling();
        }
      },
      error: function (data, textStatus, errorThrown) {
        sm('not_connected');
      }
    });
  };

  // Set browser vars
  setBrowserInfo();
  debug(version);
};