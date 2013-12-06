var notID=0;

/*
chrome.notifications.create("test", {type : "basic",title: "Basic Notification",message: "Short message part"}, function(){});
*/

function notify(msg) {
  msg.content = msg.content.replace(/<br\/>/g, " ");
	var options = {
		type : "basic",
		title: "eXo Chat: "+msg.from,
		message: msg.content,
		expandedMessage: "Longer part of the message"
	};
	options.priority = 0;
	//options.iconUrl = chrome.runtime.getURL("/images/inbox-64x64.png");

  var xhr = new XMLHttpRequest();
  xhr.open("GET", "http://demo.exoplatform.net/rest/jcr/repository/social/production/soc:providers/soc:organization/soc:"+msg.from+"/soc:profile/soc:avatar");
  xhr.responseType = "blob";
  xhr.onload = function(){
    var blob = this.response;
    options.iconUrl = window.URL.createObjectURL(blob);
    chrome.notifications.create("id"+notID++, options, notifCallback);
  };
  xhr.send(null);

}

function notifCallback(notID) {
	console.log("Succesfully created " + notID + " notification");		
}

onload = function() {
	var $ = function(sel) {
		return document.querySelector(sel);
	};

	var wv1 = $('#wv1');

  function initWebChat() {
    var domainView = $("#domainView");
    var chatView = $("#chatView");

    chrome.storage.local.get("domain", function(val){
      var url = val.domain;
      if (typeof url === "undefined") {
        chatView.style.display = "none";
        domainView.style.display = "block";
      } else {
        chrome.app.window.current().setBounds({width: 870, height: 670});
        wv1.setAttribute("src", url+"/portal/intranet/chat");
        domainView.style.display = "none";
        chatView.style.display = "block";
      }
    });

  }

  function saveDomainBtnClick() {
    var domainText = $("#domainText");
    chrome.storage.local.set({"domain": domainText.value});
    initWebChat();
  }


  function sendInitialMessage(e) {
		// only send the message if the page was loaded from googledrive hosting
    chrome.storage.local.get("domain", function(val){
      var url = val.domain;
      e.target.contentWindow.postMessage("fromChromeApp", url);
    });
	}


	// Event handlers for the various notification events
	function notificationClosed(notID, bByUser) {
		console.log("Notification '" + notID + "' was closed" + (bByUser ? " by the user" : ""));
	}

	function notificationClicked(notID) {
    chrome.app.window.current().hide();
    chrome.app.window.current().show();
    console.log("Notification '" + notID + "' was clicked");
	}

	function notificationBtnClick(notID, iBtn) {
		console.log("Notification '" + notID + "' had button " + iBtn + " clicked");
	}

	//notify();

	window.addEventListener('message', function(e) {
		notify(e.data);
		console.log("received message", e);
	});

	wv1.addEventListener('loadstop', sendInitialMessage);

  $("#saveDomainBtn").addEventListener("click", saveDomainBtnClick);

	// set up the event listeners
	chrome.notifications.onClosed.addListener(notificationClosed);
	chrome.notifications.onClicked.addListener(notificationClicked);
	chrome.notifications.onButtonClicked.addListener(notificationBtnClick);

  initWebChat();

};