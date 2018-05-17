const preferredNotification = [];
const preferredNotificationTrigger = [];
const preferredRoomNotificationTrigger = {};

export const ROOM_NOTIF_TRIGGER_NORMAL = 'normal';
export const ROOM_NOTIF_TRIGGER_SILENCE = 'silence';
export const ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD = 'keywords';
export const ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD_VALUE = 'room-notif-trigger-when-key-word-value';
export const ROOM_ON_SITE = 'on-site';
export const ROOM_DESKTOP = 'desktop';
export const ROOM_BIP = 'bip';

export function containKeyWord(message, keywords) {
  message = message.toLowerCase();
  const keyW = keywords.split(':')[1];
  if (keyW === '') {
    return false;
  }
  const keys = keyW.split(',');
  for (let i = 0; i < keys.length; i++) {
    if (message.includes(keys[i].trim().toLowerCase())) {
      return true;
    }
  }
  return false;
}

export function getPreferredNotification() {
  return preferredNotification;
}

export function setPreferredNotification(prefNotifs) {
  if (!(prefNotifs instanceof Array)) { // always force data to be wrapped into an array
    prefNotifs = [prefNotifs];
  }
  prefNotifs.forEach(function (prefNotif) {
    const index = preferredNotification.indexOf(prefNotif);
    if (index === -1) {
      preferredNotification.push(prefNotif);
    } else { //if a preferred notification is already set then remove it
      preferredNotification.splice(index, 1);
    }
  });
}

export function getPreferredNotificationTrigger() {
  return preferredNotificationTrigger;
}

export function setPreferredNotificationTrigger(prefNotifTrigger) {
  if (!(prefNotifTrigger instanceof Array)) { // always force data to be wrapped into an array
    prefNotifTrigger = [prefNotifTrigger];
  }
  prefNotifTrigger.forEach(function (prefNotif) {
    const index = preferredNotificationTrigger.indexOf(prefNotif);
    if (index === -1) {
      preferredNotificationTrigger.push(prefNotif);
    } else { //if a preferred notification is already set then remove it
      preferredNotificationTrigger.splice(index, 1);
    }
  });
}

export function getRoomPreferredNotificationTrigger() {
  return preferredRoomNotificationTrigger;
}

export function setRoomPreferredNotificationTrigger(roomId, value) {
  preferredRoomNotificationTrigger[roomId] = value;
}

export function setRoomPreferredNotificationTriggerSettings(settings) {
  for (const roomId in settings) {
    const notifData = settings[roomId];
    let notifCond = notifData['notifCond'];
    if (this.ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD === notifCond) {
      notifCond += `:${  notifData[notifCond]}`;
    }
    this.setRoomPreferredNotificationTrigger(roomId, notifCond);
  }
}

export function setPreferredNotificationSettings(settings, overrideSettin) { //this is always called on the reload of the page
  if (settings.preferredNotification && overrideSettin) {
    this.setPreferredNotification(JSON.parse(settings.preferredNotification));
  }
  if (settings.preferredNotificationTrigger && overrideSettin) {
    this.setPreferredNotificationTrigger(JSON.parse(settings.preferredNotificationTrigger));
  }
  if (settings.preferredRoomNotificationTrigger) {
    this.setRoomPreferredNotificationTriggerSettings(JSON.parse(settings.preferredRoomNotificationTrigger));
  }
}

export function canBypassDonotDistrub() {
  return preferredNotificationTrigger.indexOf('notify-even-not-distrub') === -1;
}

export function canPlaySound() {
  return preferredNotification.indexOf(this.ROOM_BIP) !== -1;
}

export function canShowDesktopNotif() {
  return preferredNotification.indexOf(this.ROOM_DESKTOP) !== -1;
}

export function canShowOnSiteNotif() {
  return preferredNotification.indexOf(this.ROOM_ON_SITE) !== -1;
}

export function canBypassRoomNotif(msgObj) {
  const message = msgObj.content;
  const sourceRoom = msgObj.categoryId;
  return !preferredRoomNotificationTrigger[sourceRoom] || // Not specified yet
      preferredRoomNotificationTrigger[sourceRoom].startsWith(this.ROOM_NOTIF_TRIGGER_NORMAL) || // Normal condition
      preferredRoomNotificationTrigger[sourceRoom].startsWith(this.ROOM_NOTIF_TRIGGER_WHEN_KEY_WORD) &&
      this.containKeyWord(message, preferredRoomNotificationTrigger[sourceRoom]); // Containing keywords
}

export function highlightMessage(msgObject) {
  let highlightedMsg = msgObject.content;

  if (msgObject.options) {
    switch (msgObject.options.type) {
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