import {chatData} from './chatData.js';
import * as chatNotification from './ChatNotification';
import * as chatWebStorage from './chatWebStorage';
import * as desktopNotification from './desktopNotification';

const DEFAULT_USERS_ROOMS_TO_LOAD = 30;
const DEFAULT_USER_LIMIT = 10;

const RESEND_MESSAGE_PERIOD = 5000;
const DEFAULT_HTTP_PORT = 80;

let resendIntervalID;

export function getUser(userName) {
  return fetch(`/portal/rest/v1/social/users/${userName}`, {credentials: 'include'})
    .then(resp => resp.json());
}

export function getUserStatus(userSettings, user) {
  return fetch(`${chatData.chatServerAPI}getStatus?user=${userSettings.username}&targetUser=${user}&dbName=${userSettings.dbName}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.text());
}

export function initChatSettings(username, chatRoomsLoadedCallback, userSettingsLoadedCallback) {
  chatNotification.initCometD();

  document.addEventListener('exo-chat-settings-loaded', (e) => {
    getOnlineUsers().then(users => { // Fetch online users
      getChatRooms(e.detail, users).then(data => {
        chatRoomsLoadedCallback(data);
        
        const totalUnreadMsg = Math.abs(data.unreadOffline) + Math.abs(data.unreadOnline) + Math.abs(data.unreadSpaces) + Math.abs(data.unreadTeams);
        updateTotalUnread(totalUnreadMsg);

        if (!resendIntervalID) {
          window.clearInterval(resendIntervalID);
        }
        resendIntervalID = window.setInterval(chatWebStorage.sendFailedMessages, RESEND_MESSAGE_PERIOD);
      });
    });

    document.addEventListener('exo-chat-selected-contact-changed', (e) => {
      const selectedContact = e.detail;
      if (selectedContact && selectedContact.room) {
        chatWebStorage.setStoredParam('lastSelectedRoom', selectedContact.room);
      }
    });

    getUserNotificationSettings(e.detail).then(settings => {
      loadNotificationSettings(settings);
      desktopNotification.initDesktopNotifications();
    });

  });

  document.addEventListener('exo-chat-notification-count-updated', (e) => {
    const totalUnreadMsg = e.detail ? e.detail.data.totalUnreadMsg : e.totalUnreadMsg;
    updateTotalUnread(totalUnreadMsg);
  });

  getUserSettings(username).then(userSettings => {
    if (!eXo) { eXo = {}; }
    if (!eXo.chat) { eXo.chat = {}; }
    eXo.chat.userSettings = userSettings;
    eXo.chat.userSettings.chatPage = getBaseURL() + eXo.chat.userSettings.chatPage;
    userSettingsLoadedCallback(userSettings);
    
    document.dispatchEvent(new CustomEvent('exo-chat-settings-loaded', {'detail' : userSettings}));
  });
}

export function updateTotalUnread(totalUnreadMsg) {
  if (totalUnreadMsg && totalUnreadMsg > 0) {
    document.title = `Chat (${totalUnreadMsg})`;
  } else {
    document.title = 'Chat';
  }
}

export function getUserSettings() {
  return fetch('/portal/rest/chat/api/1.0/user/settings', {credentials: 'include'})
    .then(resp => resp.json());
}

export function getOnlineUsers() {
  return fetch(`${chatData.chatAPI}onlineUsers`, {credentials: 'include'})
    .then(resp => resp.text());
}

export function toggleFavorite(room, favorite) {
  return fetch(`${chatData.chatServerAPI}toggleFavorite?user=${eXo.chat.userSettings.username}&targetUser=${room}&favorite=${favorite}&dbName=${eXo.chat.userSettings.dbName}`, {
    headers: {
      'Authorization': `Bearer ${eXo.chat.userSettings.token}`
    }}).then(resp =>  resp.text());
}

export function getChatRooms(userSettings, onlineUsers, filter, limit) {
  if(!limit) {
    limit = DEFAULT_USERS_ROOMS_TO_LOAD;
  }
  if(!filter) {
    filter = '';
  }
  return fetch(`${chatData.chatServerAPI}whoIsOnline?user=${userSettings.username}&onlineUsers=${onlineUsers}&dbName=${userSettings.dbName}&filter=${filter}&limit=${limit}&timestamp=${new Date().getTime()}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getRoomParticipants(userSettings, room) {
  return fetch(`${chatData.chatServerAPI}users?user=${userSettings.username}&dbName=${userSettings.dbName}&room=${room.room}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getRoomId(userSettings, targetUser) {
  return fetch(`${chatData.chatServerAPI}getRoom?targetUser=${targetUser}&user=${userSettings.username}&dbName=${userSettings.dbName}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.text());
}

export function getRoomDetail(userSettings, contact) {
  return fetch(`${chatData.chatServerAPI}getRoom?targetUser=${contact.user}&user=${userSettings.username}&dbName=${userSettings.dbName}&withDetail=true`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getRoomMessages(userSettings, contact, toTimestamp, limit) {
  if (!limit) {
    limit = '';
  }
  if(!toTimestamp) {
    toTimestamp = '';
  }
  return fetch(`${chatData.chatServerAPI}read?user=${userSettings.username}&dbName=${userSettings.dbName}&room=${contact.room}&toTimestamp=${toTimestamp}&limit=${limit}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function setRoomNotificationTrigger(userSettings, room, notifConditionType, notifCondition, time) {
  return fetch(`${chatData.chatServerAPI}setRoomNotificationTrigger?user=${userSettings.username}&dbName=${userSettings.dbName}&room=${room}&notifConditionType=${notifConditionType}&notifCondition=${notifCondition}&time=${time}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function setUserNotificationPreferences(userSettings, preferredNotifications, preferredNotificationTriggers) {
  let preferredNotificationParam = '';
  preferredNotifications.forEach(preferredNotification => {
    preferredNotificationParam += `&notifConditions=${preferredNotification}`;
  });
  let preferredNotificationTriggerParam = '';
  preferredNotificationTriggers.forEach(preferredNotificationTrigger => {
    preferredNotificationTriggerParam += `&notifManners=${preferredNotificationTrigger}`;
  });
  return fetch(`${chatData.chatServerAPI}setNotificationSettings?user=${userSettings.username}&dbName=${userSettings.dbName}${preferredNotificationParam}${preferredNotificationTriggerParam}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function setUserPreferredNotification(userSettings, notifManner) {
  return fetch(`${chatData.chatServerAPI}setPreferredNotification?user=${userSettings.username}&dbName=${userSettings.dbName}&notifManner=${notifManner}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getUserNotificationSettings(userSettings) {
  return fetch(`${chatData.chatServerAPI}getUserDesktopNotificationSettings?user=${userSettings.username}&dbName=${userSettings.dbName}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function loadNotificationSettings(settings) {
  if(settings && settings.userDesktopNotificationSettings) {
    eXo.chat.desktopNotificationSettings = settings.userDesktopNotificationSettings;
    if(eXo.chat.desktopNotificationSettings.preferredNotification) {
      eXo.chat.desktopNotificationSettings.preferredNotification = JSON.parse(eXo.chat.desktopNotificationSettings.preferredNotification);
    } else {
      eXo.chat.desktopNotificationSettings.preferredNotification = [];
    }
    if(eXo.chat.desktopNotificationSettings.preferredNotificationTrigger) {
      eXo.chat.desktopNotificationSettings.preferredNotificationTrigger = JSON.parse(eXo.chat.desktopNotificationSettings.preferredNotificationTrigger);
    } else {
      eXo.chat.desktopNotificationSettings.preferredNotificationTrigger = [];
    }
    if(eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger) {
      eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger = JSON.parse(eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger);
    } else {
      eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger = [];
    }
  }
}

export function getChatUsers(userSettings, filter, limit) {
  if(!limit) {
    limit = DEFAULT_USER_LIMIT;
  }
  return fetch(`${chatData.chatServerAPI}users?user=${userSettings.username}&dbName=${userSettings.dbName}&filter=${filter}&limit=${limit}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function saveRoom(userSettings, roomName, users, room) {
  const data = {
    teamName: roomName,
    users: users,
    user: userSettings.username,
    dbName: userSettings.dbName,
    room: room
  };

  return fetch(`${chatData.chatServerAPI}saveTeamRoom`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`,
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    method: 'post',
    body: $.param(data)
  }).then(resp =>  resp.json());
}

export function getUserAvatar(user) {
  return `${chatData.socialUserAPI}${user}/avatar`;
}

export function getSpaceAvatar(space) {
  return `${chatData.socialSpaceAPI}${space}/avatar`;
}

export function sendMeetingNotes(userSettings, room, fromTimestamp, toTimestamp) {
  const serverBase = getBaseURL();

  const data = {
    user: userSettings.username,
    dbName: userSettings.dbName,
    room: room,
    serverBase: serverBase,
    fromTimestamp: fromTimestamp,
    toTimestamp: toTimestamp
  };

  return fetch(`${chatData.chatServerAPI}sendMeetingNotes`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`,
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    method: 'post',
    body: $.param(data)
  }).then(resp =>  resp.text());
}

export function getMeetingNotes(userSettings, room, fromTimestamp, toTimestamp) {
  const serverBase = getBaseURL();
  const data = {
    user: userSettings.username,
    dbName: userSettings.dbName,
    room: room,
    portalURI: `${eXo.env.portal.context}/${eXo.env.portal.portalName}`,
    serverBase: serverBase,
    fromTimestamp: fromTimestamp,
    toTimestamp: toTimestamp
  };

  return fetch(`${chatData.chatServerAPI}getMeetingNotes`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`,
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    method: 'post',
    body: $.param(data)
  }).then(resp =>  resp.text());
}

export function saveWiki(userSettings, targetFullName, content) {
  if(!content || content === 'ko') {
    return;
  }
  const data = {
    targetFullname: targetFullName,
    content: typeof content === Object ? JSON.stringify(content) : content
  };

  return fetch(`${chatData.chatWikiAPI}saveWiki`, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    credentials: 'include',
    method: 'post',
    body: $.param(data)
  }).then(resp =>  resp.json());
}

export function saveEvent(userSettings, data, target) {
  const users = target.participants.map(user => user.name);
  data.space = target.fullName;
  data.users = target.type === 's' ? '' : users.join(',');

  return fetch(`${chatData.chatCalendarAPI}saveEvent`, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    credentials: 'include',
    method: 'post',
    body: $.param(data)
  });
}

export function saveTask(userSettings, data) {
  return fetch(`${chatData.chatPluginAPI}action`, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    credentials: 'include',
    method: 'post',
    body: $.param(data)
  });
}

export function getBaseURL() {
  const port = !window.location.port || window.location.port === DEFAULT_HTTP_PORT ? '':`:${  window.location.port}`;
  return `${window.location.protocol  }//${  window.location.hostname  }${port}`;
}