import {chatConstants} from './chatConstants.js';

const ROOM_NOTIF_TRIGGER_NORMAL = 'normal';
const ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD = 'keywords';
const ROOM_ON_SITE = 'on-site';
const ROOM_DESKTOP = 'desktop';
const ROOM_BIP = 'bip';

function containKeyWord(message, keywords) {
  message = message.toLowerCase();
  if (!keywords || !keywords.trim()) {
    return false;
  }
  const keys = keywords.split(/[\s,]+/);
  for (let i = 0; i < keys.length; i++) {
    if (message.includes(keys[i].trim().toLowerCase())) {
      return true;
    }
  }
  return false;
}

function canNotifySwitchStatus() {
  return canBypassDonotDisturb() || canBypassStatusCondition();
}

function canBypassStatusCondition() {
  return eXo.chat.userSettings.status !== 'donotdisturb' && eXo.chat.userSettings.status !== 'offline';
}

function canBypassDonotDisturb() {
  return eXo.chat.userSettings.status !== 'donotdisturb' || eXo.chat.desktopNotificationSettings.preferredNotificationTrigger.indexOf('notify-even-not-disturb') !== -1;
}

function canPlaySound() {
  return eXo.chat.desktopNotificationSettings.preferredNotification.indexOf(ROOM_BIP) !== -1;
}

function canShowDesktopNotif() {
  return eXo.chat.desktopNotificationSettings.preferredNotification.indexOf(ROOM_DESKTOP) !== -1;
}

export function canShowOnSiteNotif() {
  return eXo.chat.desktopNotificationSettings.preferredNotification.indexOf(ROOM_ON_SITE) !== -1;
}

function canBypassRoomNotif(msgObj) {
  const message = msgObj.content;
  const sourceRoom = msgObj.room;
  return !eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger[sourceRoom] || // Not specified yet
      !eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger[sourceRoom].notifCond ||
      eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger[sourceRoom].notifCond.startsWith(ROOM_NOTIF_TRIGGER_NORMAL) || // Normal condition
      eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger[sourceRoom].notifCond.startsWith(ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD) &&
      containKeyWord(message, eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger[sourceRoom].keywords); // Containing keywords
}

function highlightMessage(msgObject) {
  let highlightedMsg = msgObject.content;

  if (msgObject.options) {
    switch (msgObject.options.type) {
    case 'type-event':
      highlightedMsg = msgObject.options.summary;
      break;
    case 'type-hand':
      highlightedMsg = `raises hand: ${msgObject.content}`;
      break;
    case 'type-link':
      highlightedMsg = msgObject.options.link;
      break;
    case 'type-meeting-start':
      highlightedMsg = 'Start Meeting';
      break;
    case 'type-meeting-stop':
      highlightedMsg = 'End Meeting';
      break;
    }
  }

  return highlightedMsg;
}

function unifyMessageFormat(messageObj, message) {
  if(!message.room && messageObj.room) {
    message.room = messageObj.room;
  }
  if(!message.user && (messageObj.user || messageObj.sender)) {
    message.user = messageObj.user ? messageObj.user : messageObj.sender;
  }
}

function notify(e) {
  const messageObj = e.detail;
  const message = messageObj.data;
  unifyMessageFormat(messageObj, message);
  if (message.user === eXo.chat.userSettings.username) {
    return;
  }

  // A tip that helps making a tiny delay in execution of block code in the function,
  // to avoid concurrency issue in condition checking.
  setTimeout(function () {
    // Check if the message has been notified by other tab
    if (localStorage.getItem(`lastNotify-${message.room}`) === message.msgId) {
      return;
    }
    localStorage.setItem(`lastNotify-${message.room}`, message.msgId);

    const notification = {
      roomType: message.roomType,
      options: message.options,
      roomDisplayName: message.roomDisplayName,
      content: message.msg,
      room: message.room,
      from: message.user
    };

    if (canNotifySwitchStatus() && canBypassRoomNotif(notification)) {
      if (canPlaySound()) {
        $('#chat-audio-notif')[0].play();
      }
      if (canShowDesktopNotif()) {
        showDesktopNotif(eXo.chat.userSettings.chatPage, notification);
      }
    }
  });
}

function showDesktopNotif(path, msg) {
  let displayMsg = highlightMessage(msg);
  displayMsg = $('<div />').html(displayMsg).text();

  if (Notification.permission !== 'granted') {
    Notification.requestPermission();
  }

  if (!Notification) {
    return;
  }

  if (Notification.permission !== 'granted') {
    Notification.requestPermission();
  } else {
    const isFirefox = typeof InstallTrigger !== 'undefined';
    const isLinux = navigator.platform.indexOf('Linux') >= 0;

    const clickHandler = function (notif) {
      notif.onclick = function () {
        document.dispatchEvent(new CustomEvent(chatConstants.ACTION_ROOM_SELECT, {'detail' : msg.room}));
        window.focus();
        notif.close();
      };
    };

    let notification = null;
    // check if we're running Firefox on Linux then disable the Icons
    // bug firefox on Linux : https://bugzilla.mozilla.org/show_bug.cgi?id=1295974
    if (isLinux && isFirefox) {
      notification = new Notification(msg.roomDisplayName, {
        body: displayMsg
      });
      clickHandler(notification);

    } else {
      let avatarUrl = null;
      if (msg.roomType === 'u') {
        avatarUrl = `${chatConstants.socialUserAPI}${msg.from}/avatar`;
      } else {
        avatarUrl = `${chatConstants.socialSpaceAPI}${msg.roomDisplayName}/avatar`;
      }
      notification = new Notification(msg.roomDisplayName, {
        icon: avatarUrl,
        body: displayMsg
      });
      clickHandler(notification);
    }
  }
}

export function initDesktopNotifications() {
  document.addEventListener(chatConstants.EVENT_MESSAGE_RECEIVED, notify);
  document.addEventListener(chatConstants.EVENT_MESSAGE_UPDATED, notify);
}
