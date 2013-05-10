/*************************************************************************
*
* Weemo, Inc.
*
* Weemo - v 1.4.2.1
* [2013] WEEMO INC
* All Rights Reserved.
*
* NOTICE: All information contained herein is, and remains
* the property of Weemo, Inc.
* The intellectual and technical concepts contained
* herein are proprietary to Weemo, Inc.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from Weemo, Inc.
*/

var Modal = function(title, html) {
var self = this;

this.options = {
height : "200",
width : "350",
title:title,
description: html,
top: "center",
left: "center",
};

var overlay= function() {
var el = $('<div class="weemo_overlay"></div>');
$(el).appendTo('body');
};

var defaultStyles = function() {
/* Weemo overlay*/
var pageHeight = $(document).height();
var pageWidth = $(window).width();
var browser = BrowserDetect.browser;


if(self.options.top == "center") {
self.options.top = (pageHeight / 2) - (self.options.height);
}

if(self.options.left == "center") {
self.options.left = (pageWidth / 2) - (self.options.width/2);
}

switch(browser) {
case "Firefox":
case "Safari":
case "Chrome":
$('.weemo_overlay').css({
'position':'absolute',
'top':'0',
'left':'0',
'background-color':'rgba(0,0,0,0.6)',
'height':pageHeight,
'width':pageWidth,
'z-index':'9999999'
});
break;

case 'Explorer':
$('.weemo_overlay').css({
'position':'absolute',
'top':'0',
'left':'0',
'background-color':'#000',
'height':pageHeight,
'width':pageWidth,
'z-index':'9999999',
'filter':'alpha(opacity = 60)',
'-ms-filter':'alpha(opacity = 60)'
});
break;
}


$('.weemo_modal_box').css({
'position':'absolute',
'left':self.options.left,
'top':self.options.top,
'display':'none',
'height': self.options.height + 'px',
'width': self.options.width + 'px',
'z-index':'50',
});
$('.weemo_modal_close').css({
'position':'relative',
'top':'0',
'left':'-10px',
'float':'right',
'display':'block',
'color':'#000',
'font-size':'10px',
'text-decoration':'none',
});
$('.weemo_inner_modal_box').css({
'background-color':'#fff',
'height':(self.options.height - 50) + 'px',
'width':(self.options.width - 50) + 'px',
'padding':'20px'
});
};

var weemo_box = function() {
var box = $('<div class="weemo_modal_box"><a href="#" class="weemo_modal_close">close</a><div class="weemo_inner_modal_box"><h2>' + self.options.title + '</h2>' + self.options.description + '</div></div>');
$(box).appendTo('.weemo_overlay');
$('.weemo_modal_close').click(function(){
$(this).parent().fadeOut().remove();
$('.weemo_overlay').fadeOut().remove();
});
};

this.show = function() {
overlay();
weemo_box();
defaultStyles();
$('.weemo_modal_box').fadeIn();
};

this.close = function() {
$('.weemo_modal_close').parent().fadeOut().remove();
        $('.weemo_overlay').fadeOut().remove();
};
};

var Lucl = function(uri, key, displayName, type) {
if(type != undefined && type != "") this.type = type;
else this.type = "poc";
this.count = 0;

this.version = "version 1.4.2 - browser error";

this.objectName = "weemoVideoCall";
this.typeCommunication;

this.wsUri = "wss://localhost:34679";
this.protocol = "com-on-protocol";
this.websocket = null;
this.attemptToConnect = 0;
this.browser = BrowserDetect.browser;
this.os = BrowserDetect.OS;

this.timer = null;
this.state = true;
this.frequence = 5000;

// States
this.luclOk = false;
this.mgmtOk = false;
this.xmppOk = false;
this.sipOk = false;
this.longpoll = false;
this.sendroster = false;
this.sendpresence = false;
this.isConnected = false;
this.stateSM = "NOT_CONNECTED";
this.debug = false;

// Weemo
this.uri = uri.toLowerCase();
this.username = null;
this.key = key.toLowerCase();
this.displayName = displayName;

// LongPoll
this.myId =null;
this.longpoll = false;
this.longpollUri = "https://localhost:34679?callback=?";

// Multi-tabs
this.random = this.uniqid();	
this.tDownload = 15000;
this.timerDownload = null;
this.tTabAlreadyExist = 20000;
this.timerTabAlreadyExist = null;
this.tAnotherBrowser = 20000;
this.timerAnotherBrowser = null;

this.activeModalBox = true;

this.message_notstarted = '<p style="font-size: 12px; padding-bottom:10px; margin-top: 10px;">';
this.message_notstarted += 'Oops, the Weemo Driver was not detected!';
this.message_notstarted += '</p>';
this.message_notstarted += '<p style="font-size: 12px; padding-bottom:10px;">';
this.message_notstarted += 'To proceed, please click on download and follow the simple installation steps outlined.';
this.message_notstarted += '</p>';
this.message_notstarted += '<p style="text-align:center;padding-bottom:10px;">';
switch(this.type) {
case 'weemo':
this.message_notstarted += '<a href="https://conversations.com-on.com/download/lucl" target="_blank" class="button2" id="btnDownload">Download</a>';
break;

case 'poc':
this.message_notstarted += '<a href="https://download.weemo.com/poc.php?domain=209&apikey='+this.key+'" class="button2" id="btnDownload">Download</a>';
break;
}

this.message_notstarted += '</p>';


};

Lucl.prototype = {
initWebsocket: function() {
var self = this;

try {
this.typeCommunication = "websocket";
if(typeof MozWebSocket == 'function') WebSocket = MozWebSocket;
this.websocket = new WebSocket(this.wsUri, this.protocol);
this.websocket.onopen = function(evt) { self.count+=1; self._debug(self.count); if(self.timerAnotherBrowser != null) { self._debug(self.timerDownload); clearTimeout(self.timerTabAlreadyExist); clearTimeout(self.timerDownload);} else {self.stopTimer(); console.log(self.timerDownload);} self.lucl_sm("authenticate"); };
this.websocket.onclose = function(evt) { self.count-=1; self.stateSM = "NOT_CONNECTED"; self.lucl_sm("no_response"); self._debug('WEBSOCKET CLOSE');};
this.websocket.onmessage = function(evt) { self.handleData(evt.data); };
this.websocket.onerror = function(evt) {
self._debug('LUCL ONERROR: ');
self._debug(evt.data);
self.isConnected = false;
self.disconnect();
//this.websocket.close();
};
}
catch(exception) {
self._debug('WEBSOCKET EXCEPTION: ');
self._debug(exception);
}
},
stopTimer: function() {
this._debug("Stop Timer");
if(this.timerTabAlreadyExist != null) {
clearTimeout(this.timerTabAlreadyExist);
this.timerTabAlreadyExist = null;
}

if(this.timerDownload != null) {
clearTimeout(this.timerDownload);
this.timerDownload = null;
}

if(this.timerAnotherBrowser != null) {
clearTimeout(this.timerAnotherBrowser);
this.timerAnotherBrowser = null;
}
},
updateTimestamp: function() {
if(typeof(localStorage) == undefined) {
this._debug("Webstorage not supported");
}
else {
if(localStorage.srandom != undefined && localStorage.srandom == this.random) {
var d = new Date();
d.setSeconds(d.getSeconds() + 70);
localStorage.setItem("stimestamp", d.getTime());
d = null;
}
}
},
makePopup: function(name) {
var modal = null;
if(this.activeModalBox === true) {
switch(name) {
case 'not_started':
modal = new Modal("Download the video driver", this.message_notstarted);
modal.show();
if(typeof(this.onLuclNotStarted) != undefined && typeof(this.onLuclNotStarted) == 'function') this.onLuclNotStarted();
break;

case 'already_exist':
modal = new Modal("Warning", "<p style=\"font-size: 12px; padding-bottom:10px; margin-top: 10px;\">You are already connected to the Weemo Video Driver from another browser or from another tab. </p><p style=\"font-size: 12px; padding-bottom:10px; margin-top: 10px; text-align: center;\"><input type=\"button\" class=\"button2\" value=\"close\" onclick=\"$('.weemo_modal_close').parent().fadeOut().remove(); $('.weemo_overlay').fadeOut().remove()\" /></p>");
modal.show();
if(typeof(this.onTabAlreadyExist) != undefined && typeof(this.onTabAlreadyExist) == 'function') this.onTabAlreadyExist();
break;

case 'another_browser':
modal = new Modal("Warning", "<p style=\"font-size: 12px; padding-bottom:10px; margin-top: 10px;\">You are already connected to the Weemo Video Driver from another browser or from another tab. </p><p style=\"font-size: 12px; padding-bottom:10px; margin-top: 10px; text-align: center;\"><input type=\"button\" class=\"button2\" value=\"close\" onclick=\"$('.weemo_modal_close').parent().fadeOut().remove(); $('.weemo_overlay').fadeOut().remove()\" /></p>");
modal.show();
break;

default:
modal = new Modal("Informations", name);
modal.show();
};
} else {
switch(name) {
case 'not_started':
if(typeof(this.onLuclNotStarted) != undefined && typeof(this.onLuclNotStarted) == 'function') this.onLuclNotStarted();
clearTimeout(this.timerDownload);
this.timerDownload = null;
break;

case 'already_exist':
if(typeof(this.onTabAlreadyExist) != undefined && typeof(this.onTabAlreadyExist) == 'function') this.onTabAlreadyExist();
break;

case 'another_browser':
if(typeof(this.onTabAlreadyExist) != undefined && typeof(this.onTabAlreadyExist) == 'function') this.onTabAlreadyExist();
break;

default:

};
}
},
filterUnicode: function(quoted) {
/*
* Fix the "Could not decode a text frame as UTF-8." bug #socket.io #nodejs #websocket
*
* Usage:
* cleanedString = filterUnicode(maybeHarmfulString);
*
* Original work-around from SockJS: https://github.com/sockjs/sockjs-node/commit/e0e7113f0f8bd8e5fea25e1eb2a8b1fe1413da2c
* Other work-around: https://gist.github.com/2024272
*
*/

var escapable = /[\x00-\x1f\ud800-\udfff\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufff0-\uffff]/g;
escapable.lastIndex = 0;
if( !escapable.test(quoted)) return quoted;
return quoted.replace( escapable, function(a){
return '';
});
},
handleData: function(data) {
/**
* PARSE XML
*/
data = this.filterUnicode(data);
this._debug("LUCL =====> " + data);
try {
xmlDoc = $.parseXML(data);
$xml = $( xmlDoc );
$authenticated_lucl = $xml.find("authenticated_lucl");
$refresh = $xml.find("refresh");
$authenticated_mgmt = $xml.find("authenticated_mgmt");
$xmpp = $xml.find("xmpp");
$sip = $xml.find("sip");
$error = $xml.find("error");
$callstatus = $xml.find("callstatus").find("call");
$videolocalstatus = $xml.find("callstatus").find("video_local");
$videoremotestatus = $xml.find("callstatus").find("video_remote");
$sharelocalstatus = $xml.find("callstatus").find("share_local");
$shareremotestatus = $xml.find("callstatus").find("share_remote");
$soundstatus = $xml.find("callstatus").find("sound");
$status = $xml.find("status");
$disconnect = $xml.find("disconnect");
$statusConnected = $status.find("connected");
$callcreated = $xml.find("callcreated");
$idCallCreated = $callcreated.attr("id");
$sType = $callcreated.attr("stype");
$uri = $callcreated.attr("uri");
$keepalive = $xml.find("keepalive");
$audio = $xml.find("audio");

// TEST
var action = "";
if($refresh.length > 0) { action = "refresh"; }
if($disconnect.length > 0) { action = "disconnect"; }
if($keepalive.length > 0) { action = "keepalive"; }
if($statusConnected.text() == "ok") action = 'connected'; else if($statusConnected.text() == "ko") action = 'failed';
if($sip.text() == 'ok') action = 'sipOk';
if($sip.text() == 'ko') action = 'sipKo';
if($xmpp.text() == 'ok') action = 'xmppOk';
if($xmpp.text() == 'ko') action = 'xmppKo';
if($authenticated_mgmt.text() == 'ko') action ='mgmtKo'; else if($authenticated_mgmt.text() == 'ok') action ='mgmtOk';
if($authenticated_lucl.text() == 'ko') action ='luclKo';
if($authenticated_lucl.text() == 'ok') action ='luclOk';
if($audio.text() == 'ko') action ='onAudioKo';
if($audio.text() == 'ok') action ='onAudioOk';
if($idCallCreated != undefined && $sType != undefined && $uri != undefined) action = "startCall";
if($sip.text() == 'ok') action = 'sipOk';

// Error
if($error.text() == "malformed") action = "errorTypeMalformed";

// Call status
if($callstatus.text() == "incoming") action = "onIncomingCall";
if($callstatus.text() == "proceeding") action = "onProceedingCall";
if($callstatus.text() == "active") action = "onActiveCall";
if($callstatus.text() == "paused") action = "onPausedCall";
if($callstatus.text() == "terminated") action = "callTerminated";

// Video status
if($videolocalstatus.text() == "start") action = "onStartLocalVideo";
if($videolocalstatus.text() == "stop") action = "onStopLocalVideo";
if($videoremotestatus.text() == "start") action = "onStartRemoteVideo";
if($videoremotestatus.text() == "stop") action = "onStopRemoteVideo";

// Share status
if($sharelocalstatus.text() == "start") action = "onStartLocalShare";
if($sharelocalstatus.text() == "stop") action = "onStopLocalShare";
if($shareremotestatus.text() == "start") action = "onStartRemoteShare";
if($shareremotestatus.text() == "stop") action = "onStopRemoteShare";

// Sound status
if($soundstatus.text() == "mute") action = "onMuteSound";
if($soundstatus.text() == "unmute") action = "onUnmuteSound";

//Call SM
if(action != undefined && action != "") this.lucl_sm(action);
}catch(err) {
if(window.console) {
console.log("parsing error : ");
console.log(err);
}
}
},
lucl_sm: function(event, obj) {
this._debug("STATE JS =====> " + this.stateSM);
this._debug("EVENT =====> " + event);
switch(this.stateSM) {
case "CONNECTED":
if(event != "") {
switch(event) {
case "createCall":
if(obj != "" && obj != undefined && typeof(obj) == "object") { }
else obj = new Object();
obj.type = this.type;
switch(this.type) {
case "weemo":
if(obj.jidToCall != undefined && obj.jidToCall != null) {

} else {
obj.jidToCall = $(".user").val();
}

break;	
case "poc":
break;
}
this.createCall(obj);
break;
case "createExternalCall":
if(obj != "" && obj != undefined && typeof(obj) == "object") { }
else {
obj = new Object();
}
obj.type = this.type;
this.createCall(obj);
break;
case "errorTypeMalformed":
if(typeof(this.errorTypeMalformed) != undefined && typeof(this.errorTypeMalformed) == 'function') this.errorTypeMalformed();
break;
case "onIncomingCall":
if(typeof(this.onIncomingCall) != undefined && typeof(this.onIncomingCall) == 'function') this.onIncomingCall();
break;
case "onProceedingCall":
if(typeof(this.onProceedingCall) != undefined && typeof(this.onProceedingCall) == 'function') this.onProceedingCall();
break;
case "onActiveCall":
if(typeof(this.onActiveCall) != undefined && typeof(this.onActiveCall) == 'function') this.onActiveCall();
break;
case "onPausedCall":
if(typeof(this.onPausedCall) != undefined && typeof(this.onPausedCall) == 'function') this.onPausedCall();
break;
case "onStartLocalVideo":
if(typeof(this.onStartLocalVideo) != undefined && typeof(this.onStartLocalVideo) == 'function') this.onStartLocalVideo();
break;
case "onStopLocalVideo":
if(typeof(this.onStopLocalVideo) != undefined && typeof(this.onStopLocalVideo) == 'function') this.onStopLocalVideo();
break;
case "onStartRemoteVideo":
if(typeof(this.onStartRemoteVideo) != undefined && typeof(this.onStartRemoteVideo) == 'function') this.onStartRemoteVideo();
break;
case "onStopRemoteVideo":
if(typeof(this.onStopRemoteVideo) != undefined && typeof(this.onStopRemoteVideo) == 'function') this.onStopRemoteVideo();
break;
case "onStartLocalShare":
if(typeof(this.onStartLocalShare) != undefined && typeof(this.onStartLocalShare) == 'function') this.onStartLocalShare();
break;
case "onStopLocalShare":
if(typeof(this.onStopLocalShare) != undefined && typeof(this.onStopLocalShare) == 'function') this.onStopLocalShare();
break;
case "onStartRemoteShare":
if(typeof(this.onStartRemoteShare) != undefined && typeof(this.onStartRemoteShare) == 'function') this.onStartRemoteShare();
break;
case "onStopRemoteShare":
if(typeof(this.onStopRemoteShare) != undefined && typeof(this.onStopRemoteShare) == 'function') this.onStopRemoteShare();
break;
case "onMuteSound":
if(typeof(this.onMuteSound) != undefined && typeof(this.onMuteSound) == 'function') this.onMuteSound();
break;
case "onUnmuteSound":
if(typeof(this.onUnmuteSound) != undefined && typeof(this.onUnmuteSound) == 'function') this.onUnmuteSound();
break;
case "onAudioKo":
if(typeof(this.onAudioKo) != undefined && typeof(this.onAudioKo) == 'function') this.onAudioKo();
break;
case "onAudioOk":
if(typeof(this.onAudioOk) != undefined && typeof(this.onAudioOk) == 'function') this.onAudioOk();
if(this.sipOk == true) {
this.lucl_sm("sipOk");
}
break;

case "startCall":
this.controllCall($idCallCreated, 'call', 'start');
break;

case "startVideo":
this.controllCall($idCallCreated, 'video', 'start');
break;

case "stopCall":
this.controllCall($idCallCreated, 'call', 'stop');
break;

case "stopVideo":
this.controllCall($idCallCreated, 'video', 'stop');
break;

case "callCreated":
break;

case "keepalive":
this.updateTimestamp();
break;

case "forceClose":
this.stopWebsocket();
this.stateSM = "NOT_CONNECTED";
this.isConnected = false;
this.websocket = null;
this.sendroster = false;
this.sendpresence = false;
this.attemptToConnect = 0;
clearTimeout(this.timer);
break;

case "stopWebsocket":
this.stopWebsocket();
this.stateSM = "NOT_CONNECTED";
break;

case "restartWebsocket":
this.stopWebsocket();
this.stateSM = "NOT_CONNECTED";
this.state = true;
this.initWebsocket();
this.stateSM = "INIT";
break;

case "sendPresence":
this.getPresences();
break;

case "sipOk":
this.sipOk = true;
$('.weemo_modal_close').parent().fadeOut().remove();
$('.weemo_overlay').fadeOut().remove();
this.stateSM = "CONNECTED";
if(typeof(this.sipOkAction) != undefined && typeof(this.sipOkAction) == 'function') this.sipOkAction();
break;

case "sipKo":
this.sipOk = false;
if(typeof(this.sipKoAction) != undefined && typeof(this.sipKoAction) == 'function') this.sipKoAction();
break;

case "close":
case "disconnect":
this.stateSM = "NOT_CONNECTED";
this.lucl_sm('close');
break;

case "refresh":
this.stateSM = "INIT";
break;

default:
}
}
break;

case "WAITING_CONNECTION":
switch(event) {
case "connected":
this.stopTimer();
this.stateSM = "INIT";
this.lucl_sm("authenticate");
break;

case "refresh":
this.stateSM = "INIT";
break;

case "failed":
this.stateSM = "NOT_CONNECTED";
setTimeout('weemoVideoCall.lucl_sm("connect")', 5000);
break;

case "disconnect":
this.stateSM = "NOT_CONNECTED";
this.timer = setTimeout("weemoVideoCall.lucl_sm('connect')", 5000);
if (this.timerAnotherBrowser == null) {
if (this.timerDownload != null) {
clearTimeout(this.timerDownload);
this.timerDownload = null;
}
this.timerAnotherBrowser = setTimeout(this.objectName+".makePopup('another_browser')", this.tAnotherBrowser);
}
break;

case "close":
switch(this.type) {
case "weemo":
this.stateSM = "NOT_CONNECTED";
this.isConnected = false;
this.websocket = null;
this.sendroster = false;
this.sendpresence = false;
this.attemptToConnect++;

disableCall();

if(this.timerDownload == null) {
if (this.timerAnotherBrowser != null) {
clearTimeout(this.timerAnotherBrowser);
this.timerAnotherBrowser = null;
}
fn = this.objectName + ".makePopup('not_started')";
this.timerDownload = setTimeout(fn, this.tDownload);
}

this.stateSM = "NOT_CONNECTED";
this.isConnected = false;
this.websocket = null;
this.sendroster = false;
this.sendpresence = false;
this.attemptToConnect++;
if (this.attemptToConnect<3) {
clearTimeout(this.timer);
this.timer = setTimeout("weemoVideoCall.lucl_sm('connect')", this.frequence);
} else {
this.state = false;
//ping.ping_sm("restart");
clearTimeout(this.timer);
this.timer = setTimeout("weemoVideoCall.lucl_sm('connect')", this.frequence);
}

break;

case "poc":
this.stateSM = "NOT_CONNECTED";
this.isConnected = false;
this.websocket = null;
this.sendroster = false;
this.sendpresence = false;
this.attemptToConnect++;

this.timer = setTimeout("weemoVideoCall.lucl_sm('connect')", this.frequence);
var fn = "weemoVideoCall.makePopup('not_started')";
if(this.timerDownload == null) {
this.timerDownload = setTimeout(fn, this.tDownload);
}

this.stateSM = "NOT_CONNECTED";
break;
}
if(typeof(this.onClose) != undefined && typeof(this.onClose) == 'function') this.onClose();
break;

case "no_response":
if(this.timerDownload == null) {
if (this.timerAnotherBrowser != null) {
clearTimeout(this.timerAnotherBrowser);
this.timerAnotherBrowser = null;
}

var fn = "weemoVideoCall.makePopup('not_started')";
this.timerDownload = setTimeout(fn, this.tDownload);
}
this.stateSM = "NOT_CONNECTED";
this.lucl_sm('close');
break;


}
break;

case "NOT_CONNECTED":
switch(event) {
case "connect":
this._debug(this.version);

switch(this.browser) {
case "Firefox":
case "Safari":
case "Chrome":
this.stateSM = "INIT";
if(this.count < 0) this.count = 0;

this._debug(this.count);
if(this.count == 0) {

if(this.timerDownload == null) {
if (this.timerAnotherBrowser != null) {
clearTimeout(this.timerAnotherBrowser);
this.timerAnotherBrowser = null;
}
fn = this.objectName + ".makePopup('not_started')";
this.timerDownload = setTimeout(fn, this.tDownload);
}
this.initWebsocket();
} else {
if(this.timerDownload != null) {
clearTimeout(this.timerDownload);
this.timerDownload = null;
}

if (this.timerAnotherBrowser == null) {
fn = this.objectName + ".makePopup('another_browser')";
this.timerAnotherBrowser = setTimeout(fn, this.timerAnotherBrowser);
}
this.lucl_sm("authenticate");
}
break;

case 'Explorer':
this.stateSM = "WAITING_CONNECTION";
if(this.myId == null) this.myId = this.uniqid();
this.sendMessage(this.myId+":<connect/>");
break;
}
break;

case "refresh":
this.stateSM = "INIT";

break;

case "failed":
this.stateSM = "NOT_CONNECTED";
setTimeout('weemoVideoCall.lucl_sm("connect")', 5000);
break;

case "close":
switch(this.type) {
case "weemo":
this.stateSM = "NOT_CONNECTED";
this.isConnected = false;


this.sendroster = false;
this.sendpresence = false;
this.attemptToConnect++;
if (this.attemptToConnect<3) {
clearTimeout(this.timer);
this.timer = setTimeout("weemoVideoCall.lucl_sm('connect')", this.frequence);
} else {
this.state = false;
clearTimeout(this.timer);
this.timer = setTimeout("weemoVideoCall.lucl_sm('connect')", this.frequence);
}
break;

case "poc":
this.stateSM = "NOT_CONNECTED";
this.isConnected = false;
this.sendroster = false;
this.sendpresence = false;
this.state = false;
clearTimeout(this.timer);
this.timer = setTimeout("weemoVideoCall.lucl_sm('connect')", this.frequence);
break;
}
if(typeof(this.onClose) != undefined && typeof(this.onClose) == 'function') this.onClose();
break;

case "no_response":

this.stateSM = "NOT_CONNECTED";
this.lucl_sm('close');
break;

case "disconnect":
this.lucl_sm('close');
break;
}
break;

case "INIT":
switch(event) {
case "authenticate":
if(typeof(this.tryToAuthenticate) != undefined && typeof(this.tryToAuthenticate) == 'function') {
this.tryToAuthenticate();
this._debug('try');
}
switch(this.type) {
case "weemo":
this.authentication();
$("#popup p.message").html("");
$("#popup").dialog('close');
clearTimeout(this.timer);
break;

case "poc":
this.authentication_wvc();

break;
}

this.isConnected = true;

break;
case "connectedOk":

break;

case "refresh":
this.stateSM = "CONNECTED";
break;

case "connectedKo":
this.stateSM = "NOT_CONNECTED";
disableCall();
break;

case "luclOk":
if(typeof(this.luclOkAction) != undefined && typeof(this.luclOkAction) == 'function') this.luclOkAction();
break;

case "luclKo":
this.lucl_sm("close");
setTimeout("weemoVideoCall.lucl_sm('connect')", 5000);
if(typeof(this.luclKoAction) != undefined && typeof(this.luclKoAction) == 'function') this.luclKoAction();
if(typeof(this.onBadCredential) != undefined && typeof(this.onBadCredential) == 'function') this.onBadCredential();
break;

case "mgmtOk":
if(typeof(this.mgmtOkAction) != undefined && typeof(this.mgmtOkAction) == 'function') this.mgmtOkAction();
break;

case "mgmtKo":
if(typeof(this.mgmtKoAction) != undefined && typeof(this.mgmtKoAction) == 'function') this.mgmtKoAction();
if(typeof(this.onBadAuthentication) != undefined && typeof(this.onBadAuthentication) == 'function') this.onBadAuthentication();
break;

case "xmppOk":
switch(this.type) {
case "poc":
this.getRoster(true);
break;
case "weemo" :
this.getRoster();
break;
}
if(typeof(this.xmppOkAction) != undefined && typeof(this.xmppOkAction) == 'function') this.xmppOkAction();
this.stateSM = "CONNECTED";
break;

case "xmppKo":
//Lost network
if(typeof(this.xmppKoAction) != undefined && typeof(this.xmppKoAction) == 'function') this.xmppKoAction();
this.lucl_sm('close');
break;

case "disconnect":
if (this.timerAnotherBrowser == null) {
if (this.timerDownload != null) {
clearTimeout(this.timerDownload);
this.timerDownload = null;
}
this.timerAnotherBrowser = setTimeout(this.objectName+".makePopup('another_browser')", this.tAnotherBrowser);

}
this.stateSM = "NOT_CONNECTED";
this.lucl_sm('close');
break;

case "no_response":
if(this.timerDownload == null) {
if (this.timerAnotherBrowser != null) {
clearTimeout(this.timerAnotherBrowser);
this.timerAnotherBrowser = null;
}
fn = this.objectName + ".makePopup('not_started')";
this.timerDownload = setTimeout(fn, this.tDownload);
}
this.stateSM = "NOT_CONNECTED";
this.lucl_sm('close');
break;

case "close":
this.stateSM = "NOT_CONNECTED";
this.lucl_sm('close');
break;
}
break;
}
},
getRoster: function(noroster) {
switch(this.browser) {

case 'Explorer':
var self = this;
if(!noroster) {
var roster = null;
if(jaxl != undefined && jaxl.roster != "" && jaxl.roster != undefined) {
roster = jaxl.roster;
}
else {
roster = readStorage('roster', 'local');
}

if(roster != undefined && roster != null) {
var length = Object.keys(roster).length;
var i = 0;
var nbcontact = 0;
var xmlCode = "";
while(i < length) {
$.each(roster, function(key, value) {
xmlCode += "<contact jid='"+key+"'><md5>"+value.name+"</md5></contact>";
nbcontact++;
if(i%5 == 0 || i+1 == length) {
self.sendMessage(self.myId + ":<sendroster nbcontact='"+nbcontact+"'>"+xmlCode+"</sendroster>");

nbcontact = 0;
xmlCode = '';
}
i++;
});
}
}
}
i=0;
xmlCode = '';
this.sendMessage(self.myId + ":<sendroster nbcontact='0'></sendroster>");
this.sendroster = true;
this.getPresences();
break;
case "Firefox":
case "Safari":
case "Chrome":
var leWebsocket = this.websocket;
if(this.websocket) {
if(!noroster){
var roster = null;
if(roster != null && roster != undefined) {
var length = Object.keys(roster).length;
var i = 0;
var nbcontact = 0;
var xmlCode = "";
while(i < length) {
$.each(roster, function(key, value) {
xmlCode += "<contact jid='"+key+"'><md5>"+value.name+"</md5></contact>";
nbcontact++;
if(i%5 == 0 || i+1 == length) {

leWebsocket.send("<sendroster nbcontact='"+nbcontact+"'>"+xmlCode+"</sendroster>");
sleep(500);
nbcontact = 0;
xmlCode = '';
}
i++;
});
}
}
}
i=0;
xmlCode = '';
leWebsocket.send("<sendroster nbcontact='0'></sendroster>");
this._debug("JS =====> LUCL : <sendroster nbcontact='0'></sendroster>");
this.sendroster = true;
if(this.type == "weemo") {
this.getPresences();
}
}
else {
this._debug("Websocket is null : getRoster");
}
break;
}

},
setDisplayName:function() {
if(this.browser == "Explorer") {
//this.sendMessage(this.myId+':<setDisplayname dn='+this.displayName+'></setDisplayname>');
} else {
//if(this.websocket && this.websocket.readyState == 1)
//this.websocket.send('<setDisplayname dn='+this.displayName+'></setDisplayname>');

}

},
getPresences: function(presence) {
switch(this.browser) {
case "Firefox":
case "Safari":
case "Chrome":

var leWebsocket = this.websocket;
var self = this;

if(this.websocket && this.websocket.readyState == 1 && this.sendroster === true) {
if(presence != "" && presence != undefined) {
$.each(presence, function(key, value) {
if (value.type == "unavailable") {
leWebsocket.send("<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type>"+value.type+"</type><delay>"+value.delay+"</delay></sendpresence>");
stackPresence.push("<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type>"+value.type+"</type><delay>"+value.delay+"</delay></sendpresence>");
}
else if(value.type == "" || value.type == undefined) {
leWebsocket.send("<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type></type><delay>"+value.delay+"</delay></sendpresence>");
stackPresence.push("<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type></type><delay>"+value.delay+"</delay></sendpresence>");
}
else {
leWebsocket.send("<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type></type><delay>"+value.delay+"</delay></sendpresence>");
stackPresence.push("<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type></type><delay>"+value.delay+"</delay></sendpresence>");
}
});
}
}
else {
if(presence != "" && presence != undefined) {
$.each(presence, function(key, value) {
if (value.type == "unavailable") {
stackPresence.push("<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type>"+value.type+"</type><delay>"+value.delay+"</delay></sendpresence>");
}
else if (value.type == "" || value.type == undefined) {
stackPresence.push("<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type></type><delay>"+value.delay+"</delay></sendpresence>");
}
else {
stackPresence.push("<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type></type><delay>"+value.delay+"</delay></sendpresence>");
}
});
}
this._debug("Websocket is null : getPresences");
}
break;


case 'Explorer':
var self = this;
if(this.sendroster === true) {


if(presence != "" && presence != undefined) {
$.each(presence, function(key, value) {
if (value.type == "unavailable") {
self.sendMessage(self.myId+":<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type>"+value.type+"</type><delay>"+value.delay+"</delay></sendpresence>");
stackPresence.push("<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type>"+value.type+"</type><delay>"+value.delay+"</delay></sendpresence>");
}
else if(value.type == "" || value.type == undefined) {
self.sendMessage(self.myId+":<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type></type><delay>"+value.delay+"</delay></sendpresence>");
stackPresence.push("<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type></type><delay>"+value.delay+"</delay></sendpresence>");
}
else {
self.sendMessage(self.myId+":<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type></type><delay>"+value.delay+"</delay></sendpresence>");
stackPresence.push("<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type></type><delay>"+value.delay+"</delay></sendpresence>");
}
});
}

}
else {
if(presence != "" && presence != undefined) {
$.each(presence, function(key, value) {
if (value.type == "unavailable") {
stackPresence.push("<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type>"+value.type+"</type><delay>"+value.delay+"</delay></sendpresence>");
}
else if (value.type == "" || value.type == undefined) {
stackPresence.push("<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type></type><delay>"+value.delay+"</delay></sendpresence>");
}
else {
stackPresence.push("<sendpresence from='"+value.username+"'><show>"+value.show+"</show><status>"+value.show+"</status><type></type><delay>"+value.delay+"</delay></sendpresence>");
}
});
}
this._debug("Websocket is null : getPresences");
}
break;

}

},
refresh: function() {
if(this.browser == "Explorer") {
this.sendMessage(this.myId+':<refresh/>');
} else {
this.websocket.send('<refresh/>');
}
},
stopWebsocket: function() {
if (this.isConnected) this.websocket.send('<close></close>');
if (this.websocket) this.websocket.close();
this.state = false;
this.isConnected = false;
this.websocket = null;
},
sendContactInfo: function(jid) {
if(this.websocket) {
this.websocket.send('<sendcontactinfo jid="'+jid+'"></sendcontactinfo>');
this._debug('JS =====> LUCL <sendcontactinfo jid="'+jid+'"></sendcontactinfo>');
}
else {
this._debug("Websocket is null : sendContactInfo");
}
},
_debug: function (chaine) {
if(this.debug === true) {
if(window.console) console.log(chaine);
}
},
createCall: function(obj) {
switch(this.browser) {
case 'Explorer':
switch(obj.type) {
case "poc":
uri = obj.uri;
var cleanedUri = this.filterUnicode(uri);
key = obj.key;
var cleanedkey = this.filterUnicode(key);
displayNameToCall = obj.displayNameToCall;
this._debug("DisplayNameToCall : " + displayNameToCall);
var cleanedDisplayNameToCall = this.filterUnicode(displayNameToCall);
sessionType='call';
this.sendMessage(this.myId+':<createcall stype="'+sessionType+'" inout="out" uri="'+cleanedUri+'" displayname="'+cleanedDisplayNameToCall+'" key="'+cleanedkey+'" />');
this._debug("JS =====> LUCL : " + this.myId+':<createcall stype="'+sessionType+'" inout="out" uri="'+cleanedUri+'" displayname="'+cleanedDisplayNameToCall+'" key="'+cleanedkey+'" />');
break;

case "weemo":
jidToCall = obj.jidToCall;
var cleanedJidToCall = this.filterUnicode(jidToCall);
sessionType='call';
this.sendMessage(this.myId+':<createcall stype="'+sessionType+'" inout="out" uri="'+cleanedJidToCall+'" />');
this._debug("JS =====> LUCL : " + this.myId+':<createcall stype="'+sessionType+'" inout="out" uri="'+cleanedJidToCall+'"/>');
break;
}
break;
case "Firefox":
case "Safari":
case "Chrome":
if(this.websocket) {
switch(obj.type) {
case "poc":
uri = obj.uri;
var cleanedUri = this.filterUnicode(uri);
key = obj.key;
var cleanedkey = this.filterUnicode(key);
displayNameToCall = obj.displayNameToCall;
this._debug("DisplayNameToCall : " + displayNameToCall);
var cleanedDisplayNameToCall = this.filterUnicode(displayNameToCall);
sessionType='call';
this.websocket.send('<createcall stype="'+sessionType+'" inout="out" uri="'+cleanedUri+'" displayname="'+cleanedDisplayNameToCall+'" key="'+cleanedkey+'" />');
this._debug("JS =====> LUCL : " + '<createcall stype="'+sessionType+'" inout="out" uri="'+cleanedUri+'" displayname="'+cleanedDisplayNameToCall+'" key="'+cleanedkey+'" />');
break;

case "weemo":
jidToCall = obj.jidToCall;
var cleanedJidToCall = this.filterUnicode(jidToCall);
sessionType='call';

if(obj.displayNameToCall != undefined && obj.displayNameToCall != null) {
var cleanedDisplayNameToCall = this.filterUnicode(obj.displayNameToCall);
this.websocket.send('<createcall stype="'+sessionType+'" inout="out" uri="'+cleanedJidToCall+'" displayname="'+cleanedDisplayNameToCall+'" />');
this._debug("JS =====> LUCL : " + '<createcall stype="'+sessionType+'" inout="out" uri="'+cleanedJidToCall+'" displayname="'+cleanedDisplayNameToCall+'"/>');
} else {
this.websocket.send('<createcall stype="'+sessionType+'" inout="out" uri="'+cleanedJidToCall+'" />');
this._debug("JS =====> LUCL : " + '<createcall stype="'+sessionType+'" inout="out" uri="'+cleanedJidToCall+'" />');
}
break;
}
}
else {
this._debug("Websocket is null : createCall");
}
break;

}

},
createVideoCall: function() {
if(this.websocket) {
jidToCall = $('.user').val();
sessionType='call';
id='0';
this.websocket.send('<createcall id="'+id+'" inout="out" stype="'+sessionType+'" uri="'+jidToCall+'"/>');
this._debug("JS =====> LUCL : " + '<createcall id="'+id+'" stype="'+sessionType+'" inout="out" uri="'+jidToCall+'"/>');
}
else {
this._debug("Websocket is null : createVideoCall");
}
},
createConf: function(confId) {
		if(this.isConnected) {
			if (!confId) confId = "-conversation@weemo.com";
	    	jidToCall = $('.user').val();
	    	sessionType='conf';
	    	id='0';
			this._debug("JS =====> LUCL : " + '<createcall id="'+id+'" inout="out" stype="'+sessionType+'"  uri="999-nyc1'+confId+'"/>');
	    	this.websocket.send('<createcall id="'+id+'" inout="out" stype="'+sessionType+'" uri="999-nyc1'+confId+'"/>');
	    }
	    else {
	    	$('#dialogdownload').dialog('open');
	    	this._debug("Websocket is null : createConf");
	    }
	},
	createConfAdmin: function(confId) {
		if(this.isConnected) {
	    	if (!confId) confId = "-conversation@weemo.com";
	    	sessionType='conf';
	    	id='0';
			this._debug("JS =====> LUCL : " + '<createcall id="'+id+'" inout="out"stype="'+sessionType+'" uri="998-nyc1'+confId+'"/>');
			this.websocket.send('<createcall id="'+id+'" stype="'+sessionType+'" inout="out" uri="998-nyc1'+confId+'"/>');
	    }
	    else {
	    	$('#dialogdownload').dialog('open');
	    	this._debug("Websocket is null : createConfAdmin");
	    }
	},
askContactInfo: function(jid) {
if(this.websocket) {
this.websocket.send('<askcontactinfo jid="'+jid+'"></askcontactinfo>');
this._debug("JS =====> LUCL : " + '<askcontactinfo jid="'+jid+'"></askcontactinfo>');
}
else {
this._debug("Websocket is null : askContactInfo");
}
},
joinConfAdmin: function() {
		
		switch(this.browser) {
		
			case "Firefox":
			case "Safari":
			case 'Chrome' :
				switch(this.type) {
			    	case "poc":
			    		uri = this.uri;
			    		var cleanedUri = this.filterUnicode(uri);
			    		key = this.key;
			    		var cleanedkey = this.filterUnicode(key);
			    		displayNameToCall = this.displayName;
			    		var cleanedDisplayNameToCall = this.filterUnicode(displayNameToCall);
				    	sessionType='conf';
				    	this.websocket.send('<createcall stype="'+sessionType+'" inout="out" uri="998-nyc1'+cleanedUri+'" displayname="'+cleanedDisplayNameToCall+'" key="'+cleanedkey+'"  />');
				    	this._debug("JS =====> LUCL : " + '<createcall stype="'+sessionType+'" inout="out" uri="998-nyc1'+cleanedUri+'" displayname="'+cleanedDisplayNameToCall+'" key="'+cleanedkey+'"  />');
				    break;
				    
			    	case "weemo":
			    		jidToCall = this.uri;
			    		var cleanedJidToCall = this.filterUnicode(jidToCall);
				    	sessionType='conf';
				    	
				    	if(this.displayName != undefined && this.displayName != null) {
				    		var cleanedDisplayNameToCall = this.filterUnicode(this.displayName);
				    		this.websocket.send('<createcall stype="'+sessionType+'" inout="out" uri="998-nyc1'+cleanedJidToCall+'" displayname="'+cleanedDisplayNameToCall+'" />');
				    		this._debug("JS =====> LUCL : " + '<createcall stype="'+sessionType+'" inout="out" uri="998-nyc1'+cleanedJidToCall+'" displayname="'+cleanedDisplayNameToCall+'"/>');
				    	} else {
				    		this.websocket.send('<createcall stype="'+sessionType+'" inout="out" uri="998-nyc1'+cleanedJidToCall+'" />');
				    		this._debug("JS =====> LUCL : " + '<createcall stype="'+sessionType+'" inout="out" uri="998-nyc1'+cleanedJidToCall+'" />');
				    	}
			    	break;
				}
				/*if(this.key != "" && this.key != undefined) { confId = this.uri+this.key; }
				else { confId = this.uri; }
				
		    	sessionType='conf';
		    	id='0';
				this._debug("JS =====> LUCL : " + '<createcall id="'+id+'" inout="out" stype="'+sessionType+'" uri="998-'+confId+'"/>');
				this.websocket.send('<createcall id="'+id+'" stype="'+sessionType+'" inout="out" uri="998-'+confId+'"/>');*/
			break;
			
			case 'Explorer':
				if(this.isConnected === true)  {
					switch(this.type) {
						case "poc":
				    		uri = this.uri;
				    		var cleanedUri = this.filterUnicode(uri);
				    		key = this.key;
				    		var cleanedkey = this.filterUnicode(key);
				    		displayNameToCall = this.displayName;
				    		
				    		var cleanedDisplayNameToCall = this.filterUnicode(displayNameToCall);
					    	sessionType='conf';
					    	this.sendMessage(this.myId+':<createcall stype="'+sessionType+'" inout="out" uri="998-nyc1'+cleanedUri+'" displayname="'+cleanedDisplayNameToCall+'" key="'+cleanedkey+'"  />');
					    	this._debug("JS =====> LUCL : " + this.myId+':<createcall stype="'+sessionType+'" inout="out" uri="998-nyc1'+cleanedUri+'" displayname="'+cleanedDisplayNameToCall+'" key="'+cleanedkey+'"  />');
					    break;
					    
				    	case "weemo":
				    		jidToCall = this.uri;
				    		var cleanedJidToCall = this.filterUnicode(jidToCall);
					    	sessionType='conf';
					    	this.sendMessage(this.myId+':<createcall stype="'+sessionType+'" inout="out" uri="998-nyc1'+cleanedJidToCall+'" />');
					    	this._debug("JS =====> LUCL : " + this.myId+':<createcall stype="'+sessionType+'" inout="out" uri="998-nyc1'+cleanedJidToCall+'"/>');
				    	break;
					}
			    }
			break;
	    }
	    
	},
	joinConf: function(id, displayname) {
		switch(this.browser) {
			case "Firefox":
			case "Safari":
			case 'Chrome' :
				switch(this.type) {
		    	case "poc":
		    		uri = id;
		    		var cleanedUri = this.filterUnicode(uri);
		    		key = this.key;
		    		var cleanedkey = this.filterUnicode(key);
		    		displayNameToCall = displayname;
		    		var cleanedDisplayNameToCall = this.filterUnicode(displayNameToCall);
			    	sessionType='conf';
			    	this.websocket.send('<createcall stype="'+sessionType+'" inout="out" uri="999-nyc1'+cleanedUri+'" displayname="'+cleanedDisplayNameToCall+'" key="'+cleanedkey+'"  />');
			    	this._debug("JS =====> LUCL : " + '<createcall stype="'+sessionType+'" inout="out" uri="999-nyc1'+cleanedUri+'" displayname="'+cleanedDisplayNameToCall+'" key="'+cleanedkey+'"  />');
			    break;
			    
		    	case "weemo":
		    		jidToCall = id;
		    		var cleanedJidToCall = this.filterUnicode(jidToCall);
			    	sessionType='conf';
			    	
			    	if(displayname) {
			    		var cleanedDisplayNameToCall = this.filterUnicode(displayname);
			    		this.websocket.send('<createcall stype="'+sessionType+'" inout="out" uri="999-nyc1'+cleanedJidToCall+'" displayname="'+cleanedDisplayNameToCall+'" />');
			    		this._debug("JS =====> LUCL : " + '<createcall stype="'+sessionType+'" inout="out" uri="999-nyc1'+cleanedJidToCall+'" displayname="'+cleanedDisplayNameToCall+'"/>');
			    	} else {
			    		this.websocket.send('<createcall stype="'+sessionType+'" inout="out" uri="999-nyc1'+cleanedJidToCall+'" />');
			    		this._debug("JS =====> LUCL : " + '<createcall stype="'+sessionType+'" inout="out" uri="999-nyc1'+cleanedJidToCall+'" />');
			    	}
		    	break;
			}
//				if(this.key != "" && this.key != undefined) { confId = id+this.key; }
//				else { confId = id; }
//		    	sessionType='conf';
//		    	id='0';
//				this._debug("JS =====> LUCL : " + '<createcall id="'+id+'" inout="out" stype="'+sessionType+'" uri="999-'+confId+'"/>');
//				this.websocket.send('<createcall id="'+id+'" stype="'+sessionType+'" inout="out" uri="999-'+confId+'"/>');
			break;
			
			case 'Explorer':
//				if(this.isConnected === true)  {
//			    	this.sendMessage(this.myId+':<createcall id="'+id+'" stype="'+sessionType+'" inout="out" uri="999-'+confId+'"/>');
//			    	this._debug("JS =====> LUCL : " + this.myId+':<createcall id="'+id+'" stype="'+sessionType+'" inout="out" uri="999-'+confId+'"/>');
//			    }
				if(this.isConnected === true)  {
					switch(this.type) {
						case "poc":
				    		uri = id;
				    		var cleanedUri = this.filterUnicode(uri);
				    		key = this.key;
				    		var cleanedkey = this.filterUnicode(key);
				    		displayNameToCall = displayname;
				    		
				    		var cleanedDisplayNameToCall = this.filterUnicode(displayNameToCall);
					    	sessionType='conf';
					    	this.sendMessage(this.myId+':<createcall stype="'+sessionType+'" inout="out" uri="999-nyc1'+cleanedUri+'" displayname="'+cleanedDisplayNameToCall+'" key="'+cleanedkey+'"  />');
					    	this._debug("JS =====> LUCL : " + this.myId+':<createcall stype="'+sessionType+'" inout="out" uri="999-nyc1'+cleanedUri+'" displayname="'+cleanedDisplayNameToCall+'" key="'+cleanedkey+'"  />');
					    break;
					    
				    	case "weemo":
				    		jidToCall = id;
				    		var cleanedJidToCall = this.filterUnicode(jidToCall);
					    	sessionType='conf';
					    	this.sendMessage(this.myId+':<createcall stype="'+sessionType+'" inout="out" uri="999-nyc1'+cleanedJidToCall+'" />');
					    	this._debug("JS =====> LUCL : " + this.myId+':<createcall stype="'+sessionType+'" inout="out" uri="999-nyc1'+cleanedJidToCall+'"/>');
				    	break;
					}
					break;
				}
		}
	    
	},
authentication: function() {
// replace @ in username
var uriEscaped = this.username;
switch(this.browser) {
case "Firefox":
case "Safari":
case 'Chrome' :
if(this.websocket) {
if(this.displayName != "") this.websocket.send('<authentication token="'+this.uri+'" username="'+uriEscaped+'" domain="'+this.key+'" displayname="'+this.displayName+'"></authentication>');
else this.websocket.send('<authentication token="'+this.uri+'" username="'+uriEscaped+'" domain="'+this.key+'"></authentication>');
this._debug("JS =====> LUCL : " + '<authentication token="'+this.uri+'" username="'+uriEscaped+'" domain="'+this.key+'" displayname="'+this.displayName+'"></authentication>');

}
else {
this._debug("Websocket is null : Authentication");
}
break;


case 'Explorer':
if(this.isConnected === true) {
this.sendMessage(this.myId+':<authentication token="'+this.uri+'" username="'+uriEscaped+'" domain="'+this.key+'" displayname="'+this.displayName+'"></authentication>');
this._debug("JS =====> LUCL : " + this.myId+':<authentication token="'+this.uri+'" username="'+uriEscaped+'" domain="'+this.key+'" displayname="'+this.displayName+'"></authentication>');
}
break;

}
},
authentication_wvc: function() {
// replace @ in uri
var uriEscaped = this.uri;
var uriCleaned = this.filterUnicode(uriEscaped);
var cleanedDisplayName = this.filterUnicode(this.displayName);
var keyEscaped = this.key;
var cleanedKey = this.filterUnicode(keyEscaped);

this.username = this.uri+"-"+this.key;
switch(this.browser) {
case "Firefox":
case "Safari":
case 'Chrome' :
if(this.websocket) {
this.websocket.send('<authentication_wvc uri="'+uriCleaned+'" key="'+cleanedKey+'" displayname="'+cleanedDisplayName+'"></authentication_wvc>');
this._debug("JS =====> LUCL : " + '<authentication_wvc uri="'+uriCleaned+'" key="'+cleanedKey+'" displayname="'+cleanedDisplayName+'"></authentication_wvc>');
}
else {
this._debug("Websocket is null : Authentication_wvc");
}
break;


case 'Explorer':
if(this.isConnected === true) {
this.sendMessage(this.myId+':<authentication_wvc uri="'+uriCleaned+'" key="'+cleanedKey+'" displayname="'+cleanedDisplayName+'"></authentication_wvc>');
this._debug("JS =====> LUCL : " + this.myId+':<authentication_wvc uri="'+uriCleaned+'" key="'+cleanedKey+'" displayname="'+cleanedDisplayName+'"></authentication_wvc>');
}
break;
}

},
controllCall: function(id, action, command) {
switch(this.browser){
case 'Explorer':
this.sendMessage(this.myId+':<controlcall id="'+id+'"><'+action+'>'+command+'</'+action+'></controlcall>');
this._debug("JS =====> LUCL : " + this.myId+':<controlcall id="'+id+'"><'+action+'>'+command+'</'+action+'></controlcall>');
break;

case "Chrome":
case "Safari":
case "Firefox":
if(this.websocket) {
this.websocket.send('<controlcall id="'+id+'"><'+action+'>'+command+'</'+action+'></controlcall>');
this._debug("JS =====> LUCL : " + '<controlcall id="'+id+'"><'+action+'>'+command+'</'+action+'></controlcall>');
}
else {
this._debug("Websocket is null : controllCall");
}
break;
}	

},
disconnect:function() {
if(this.browser == "Explorer") {
this.sendMessage(this.myId+':<disconnect/>');
this._debug(this.myId+':<disconnect/>');
} else {
if(this.websocket) {
this.websocket.send('<disconnect/>');
//this._debug('<disconnect/>');
this.websocket.close();
}
}
},
polling: function() {
if(this.browser == "Explorer") {
var self = this;
$.ajax(this.longpollUri, {
data: {command:this.myId+":<ack/>"},
dataType: "jsonp",
timeout: 16000,
success: function(data) {
if(data != "" && data != undefined) {
if(data.x != "<disconnect/>") {
var pos = self.strpos(data.x, ":");
if(pos !== false) {
data.x = data.x.substring(pos+1);
}
self.handleData(data.x);

if(data.x != self.myId+":<disconnect/>" && self.stateSM != "NOT_CONNECTED") {
self.timer = setTimeout("weemoVideoCall.polling()", 1);
self.isConnected = true;
}
         if(data.x == "<disconnect/>") { self.attemptToConnect = 0; }
} else {
this.timer = setTimeout("weemoVideoCall.lucl_sm('disconnect')", 1000);
}
}	
},
error: function (data, textStatus, errorThrown) {
self.lucl_sm('close');
}
});
}
},
uniqid: function() {
var newDate = new Date;
return (newDate.getTime()%(2147483648-1));
},
updateSession: function(random, timestamp) {

},
urlencode: function(str) {
str = (str + '').toString();

// Tilde should be allowed unescaped in future versions of PHP (as reflected below), but if you want to reflect current
// PHP behavior, you would need to add ".replace(/~/g, '%7E');" to the following.
return encodeURIComponent(str).replace(/!/g, '%21').replace(/'/g, '%27').replace(/\(/g, '%28').
replace(/\)/g, '%29').replace(/\*/g, '%2A');
},
strpos: function(haystack, needle, offset) {
var i = (haystack + '').indexOf(needle, (offset || 0));
return i === -1 ? false : i;
},
trim: function(str, charlist) {
// Strips whitespace from the beginning and end of a string
//
// version: 1109.2015
// discuss at: http://phpjs.org/functions/trim // + original by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
// + improved by: mdsjack (http://www.mdsjack.bo.it)
// + improved by: Alexander Ermolaev (http://snippets.dzone.com/user/AlexanderErmolaev)
// + input by: Erkekjetter
// + improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net) // + input by: DxGx
// + improved by: Steven Levithan (http://blog.stevenlevithan.com)
// + tweaked by: Jack
// + bugfixed by: Onno Marsman
// * example 1: trim(' Kevin van Zonneveld '); // * returns 1: 'Kevin van Zonneveld'
// * example 2: trim('Hello World', 'Hdle');
// * returns 2: 'o Wor'
// * example 3: trim(16, 1);
// * returns 3: 6 var whitespace, l = 0,
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
},
sendMessage: function(val, type) {
var self = this;
var message = new String();

if(val != "" && val != undefined) { message = val; }

this.jqXHR = $.ajax(this.longpollUri, {
timeout: 5000,
beforeSend: function() { },
data: {command:message },
dataType: "jsonp",
success: function(data) {
data = self.trim(data.x);
var pos = self.strpos(data, ":");
if(pos !== false) {
data = data.substring(pos+1);
}

if(data != "" && data != undefined) {
var pos = self.strpos(data, ":");
if(pos !== false) {
data = data.substring(pos+1);
}

try {
if(data != "<disconnect/>") {
xmlDoc = $.parseXML(data);
self.handleData(data);

if(val == self.myId + ":<connect/>" && data != "<disconnect/>" ) {
self._debug('POLLING LAUNCH BY <CONNECT />');
self.polling();
self.isConnected = true;
}
if(data == "<disconnect/>") { self.attemptToConnect = 0; }
} else {
this.timer = setTimeout("weemoVideoCall.lucl_sm('disconnect')", 1000);
}
}
catch(err) {

}
}
else {
if(val == self.myId + ":<connect/>") {
     self._debug('POLLING LAUNCH BY <CONNECT />');
     self.polling();
     self.isConnected = true;
     }
}
},
error: function (data) {
if(val == self.myId+":<connect/>") {
this.timer = setTimeout("weemoVideoCall.lucl_sm('close')", 5000);
}
}
});
},
str_replace: function(search, replace, subject, count) {
// Replaces all occurrences of search in haystack with replace
//
// version: 1109.2015
// discuss at: http://phpjs.org/functions/str_replace // + original by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
// + improved by: Gabriel Paderni
// + improved by: Philip Peterson
// + improved by: Simon Willison (http://simonwillison.net)
// + revised by: Jonas Raoni Soares Silva (http://www.jsfromhell.com) // + bugfixed by: Anton Ongson
// + input by: Onno Marsman
// + improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
// + tweaked by: Onno Marsman
// + input by: Brett Zamir (http://brett-zamir.me) // + bugfixed by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
// + input by: Oleg Eremeev
// + improved by: Brett Zamir (http://brett-zamir.me)
// + bugfixed by: Oleg Eremeev
// % note 1: The count parameter must be passed as a string in order // % note 1: to find a global variable in which the result will be given
// * example 1: str_replace(' ', '.', 'Kevin van Zonneveld');
// * returns 1: 'Kevin.van.Zonneveld'
// * example 2: str_replace(['{name}', 'l'], ['hello', 'm'], '{name}, lars');
// * returns 2: 'hemmo, mars' var i = 0,
j = 0,
temp = '',
repl = '',
sl = 0, fl = 0,
f = [].concat(search),
r = [].concat(replace),
s = subject,
ra = Object.prototype.toString.call(r) === '[object Array]', sa = Object.prototype.toString.call(s) === '[object Array]';
s = [].concat(s);
if (count) {
this.window[count] = 0;
}
for (i = 0, sl = s.length; i < sl; i++) {
if (s[i] === '') {
continue;
} for (j = 0, fl = f.length; j < fl; j++) {
temp = s[i] + '';
repl = ra ? (r[j] !== undefined ? r[j] : '') : r[0];
s[i] = (temp).split(f[j]).join(repl);
if (count && s[i] !== temp) { this.window[count] += (temp.length - s[i].length) / f[j].length;
}
}
}
return sa ? s : s[0];
}
};

window.onbeforeunload = function() {
if(weemoVideoCall != null && weemoVideoCall != undefined && weemoVideoCall != "") weemoVideoCall.lucl_sm('disconnect');
};