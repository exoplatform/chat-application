import {chatData} from './chatData.js';
import * as chatNotification from './ChatNotification';

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
      });
    });
  });

  document.addEventListener('exo-chat-notification-count-updated', (e) => {
    const totalUnreadMsg = e.detail ? e.detail.data.totalUnreadMsg : e.totalUnreadMsg;
    updateTotalUnread(totalUnreadMsg);
  });

  getUserSettings(username).then(userSettings => {
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

export function getChatRooms(userSettings, onlineUsers) {
  return fetch(`${chatData.chatServerAPI}whoIsOnline?user=${userSettings.username}&onlineUsers=${onlineUsers}&dbName=${userSettings.dbName}&timestamp=${new Date().getTime()}`, {
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

export function getRoomCreator(userSettings, room) {
  return fetch(`${chatData.chatServerAPI}getCreator?user=${userSettings.username}&dbName=${userSettings.dbName}&room=${room.room}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getRoomId(userSettings, contact) {
  return fetch(`${chatData.chatServerAPI}getRoom?targetUser=${contact.user}&user=${userSettings.username}&dbName=${userSettings.dbName}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.text());
}

export function getRoomMessages(userSettings, contact) {
  return fetch(`${chatData.chatServerAPI}read?user=${userSettings.username}&dbName=${userSettings.dbName}&room=${contact.room}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getUserAvatar(user) {
  return `${chatData.socialUserAPI}${user}/avatar`;
}

export function getSpaceAvatar(space) {
  return `${chatData.socialSpaceAPI}${space}/avatar`;
}