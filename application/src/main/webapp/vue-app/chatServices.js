import {chatConstants} from './chatConstants.js';
import * as chatWebStorage from './chatWebStorage';
import * as chatWebSocket from './chatWebSocket';
import * as desktopNotification from './desktopNotification';
import {initTiptip} from './tiptip';

const DEFAULT_USERS_ROOMS_TO_LOAD = 30;
const DEFAULT_USER_LIMIT = 20;
const DEFAULT_HTTP_PORT = 80;
const REATTEMPT_INIT_PERIOD = 2000;

export function getUserStatus(userSettings, user) {
  return fetch(`${chatConstants.CHAT_SERVER_API}getStatus?user=${userSettings.username}&targetUser=${user}&dbName=${userSettings.dbName}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.text());
}

export function setUserStatus(userSettings, status, callback) {
  if (chatWebSocket && chatWebSocket.isConnected()) {
    chatWebSocket.setStatus(status, () => {
      fetch(`${chatConstants.USER_STATE_API}${userSettings.username}?status=${status}`, {
        credentials: 'include',
        method: 'PUT'
      }).then(() => {
        if (typeof callback === 'function') {
          callback(status);
        }
      });
    }, () => setTimeout(() => setUserStatus(userSettings, status, callback), chatConstants.REATTEMPT_PERIOD));
  } else {
    setTimeout(() => setUserStatus(userSettings, status, callback), chatConstants.REATTEMPT_PERIOD);
  }
}

export function getNotReadMessages(userSettings, withDetails) {
  return fetch(`${chatConstants.CHAT_SERVER_API}notification?user=${userSettings.username}&dbName=${userSettings.dbName}&withDetails=${withDetails}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function initChatSettings(username, isMiniChat, userSettingsLoadedCallback, chatRoomsLoadedCallback) {
  if (!eXo) { eXo = {}; }
  if (!eXo.chat) { eXo.chat = {}; }
  if (!eXo.chat.userSettings) { eXo.chat.userSettings = {}; }
  initTiptip();

  document.addEventListener(chatConstants.EVENT_USER_SETTINGS_LOADED, (e) => {
    const settings = e.detail;
    if (isMiniChat) {
      //mini chat only need fetching notifications
      getNotReadMessages(settings).then(notifications => chatRoomsLoadedCallback(notifications));
    } else {
      // fetch online users then fetch chat rooms
      getOnlineUsers().then(users => getChatRooms(settings, users)).then(data => chatRoomsLoadedCallback(data));
    }

    document.addEventListener(chatConstants.EVENT_ROOM_SELECTION_CHANGED, (e) => {
      const selectedContact = e.detail;
      if (selectedContact && selectedContact.room) {
        chatWebStorage.setStoredParam(chatConstants.STORED_PARAM_LAST_SELECTED_ROOM, selectedContact.room);
      }
    });

    getUserNotificationSettings(e.detail).then(settings => {
      loadNotificationSettings(settings);
      desktopNotification.initDesktopNotifications();
    });
  });

  getUserSettings(username).then(userSettings => {
    eXo.chat.userSettings = userSettings;
    initSettings(username, userSettings, userSettingsLoadedCallback);
  });

  document.addEventListener(chatConstants.EVENT_USER_STATUS_CHANGED, setProfileStatus);
}

export function initSettings(username, userSettings, userSettingsLoadedCallback) {
  try {
    eXo.chat.userSettings = userSettings;
    chatWebSocket.initSettings(userSettings);
    eXo.chat.userSettings.chatPage = getBaseURL() + eXo.chat.userSettings.chatPage;
    userSettingsLoadedCallback(userSettings);
    
    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_USER_SETTINGS_LOADED, {'detail' : userSettings}));
  } catch(e) {
    setTimeout(() => initSettings(status, userSettings, userSettingsLoadedCallback), REATTEMPT_INIT_PERIOD);
  }
}

export function updateTotalUnread(totalUnreadMsg) {
  if (totalUnreadMsg && totalUnreadMsg > 0) {
    document.title = `Chat (${totalUnreadMsg})`;
  } else {
    document.title = 'Chat';
  }
}

export function setProfileStatus(event) {
  const contactChanged = event.detail;
  const status = contactChanged.data ? contactChanged.data.status : contactChanged.status;
  const username = contactChanged.sender;
  if (username && status) {
    const $profileMenuStatusBtn = $(`.uiProfileMenu .profileMenuNavHeader [data-userid='${username}'] i`);
    if($profileMenuStatusBtn.length) {
      updateStatusElement($profileMenuStatusBtn, status, eXo.chat.userSettings.statusLabels ? eXo.chat.userSettings.statusLabels[status] : '');
    }
  }
}

export function getUserSettings() {
  return fetch(`${chatConstants.PORTAL}/${chatConstants.PORTAL_REST}${chatConstants.CHAT_API}settings`, {credentials: 'include'})
    .then(resp => resp.json());
}

export function getOnlineUsers() {
  return fetch(`${chatConstants.PORTAL}/${chatConstants.PORTAL_REST}${chatConstants.CHAT_API}onlineUsers`, {credentials: 'include'})
    .then(resp => resp.text());
}

export function toggleFavorite(room, user, favorite) {
  if ((!room || !room.trim().length) && user && user.trim().length) {
    return getRoomId(eXo.chat.userSettings, user, 'username').then((roomId) => {
      room = roomId;
      return fetch(`${chatConstants.CHAT_SERVER_API}toggleFavorite?user=${eXo.chat.userSettings.username}&targetUser=${room}&favorite=${favorite}&dbName=${eXo.chat.userSettings.dbName}`, {
        headers: {
          'Authorization': `Bearer ${eXo.chat.userSettings.token}`
        }}).then(resp =>  resp.text());
    });
  } else {
    return fetch(`${chatConstants.CHAT_SERVER_API}toggleFavorite?user=${eXo.chat.userSettings.username}&targetUser=${room}&favorite=${favorite}&dbName=${eXo.chat.userSettings.dbName}`, {
      headers: {
        'Authorization': `Bearer ${eXo.chat.userSettings.token}`
      }}).then(resp =>  resp.text());
  }
}

export function getChatRooms(userSettings, onlineUsers, filter, limit) {
  if(!limit) {
    limit = DEFAULT_USERS_ROOMS_TO_LOAD;
  }
  if(!filter) {
    filter = '';
  }
  return fetch(`${chatConstants.CHAT_SERVER_API}whoIsOnline?user=${userSettings.username}&onlineUsers=${onlineUsers}&dbName=${userSettings.dbName}&filter=${filter}&limit=${limit}&timestamp=${new Date().getTime()}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getRoomParticipants(userSettings, room) {
  return fetch(`${chatConstants.CHAT_SERVER_API}users?user=${userSettings.username}&dbName=${userSettings.dbName}&room=${room.room}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getRoomId(userSettings, targetUser, fieldName) {
  return fetch(`${chatConstants.CHAT_SERVER_API}getRoom?targetUser=${targetUser}&user=${userSettings.username}&dbName=${userSettings.dbName}&type=${fieldName}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.text());
}

export function getRoomDetail(userSettings, room) {
  return fetch(`${chatConstants.CHAT_SERVER_API}getRoom?targetUser=${room}&user=${userSettings.username}&dbName=${userSettings.dbName}&withDetail=true&type=room-id`, {
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
  return fetch(`${chatConstants.CHAT_SERVER_API}read?user=${userSettings.username}&dbName=${userSettings.dbName}&room=${contact.room}&toTimestamp=${toTimestamp}&limit=${limit}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function setRoomNotificationTrigger(userSettings, room, notifConditionType, notifCondition, time) {
  return fetch(`${chatConstants.CHAT_SERVER_API}setRoomNotificationTrigger?user=${userSettings.username}&dbName=${userSettings.dbName}&room=${room}&notifConditionType=${notifConditionType}&notifCondition=${notifCondition}&time=${time}`, {
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
  return fetch(`${chatConstants.CHAT_SERVER_API}setNotificationSettings?user=${userSettings.username}&dbName=${userSettings.dbName}${preferredNotificationParam}${preferredNotificationTriggerParam}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function setUserPreferredNotification(userSettings, notifManner) {
  return fetch(`${chatConstants.CHAT_SERVER_API}setPreferredNotification?user=${userSettings.username}&dbName=${userSettings.dbName}&notifManner=${notifManner}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getUserNotificationSettings(userSettings) {
  return fetch(`${chatConstants.CHAT_SERVER_API}getUserDesktopNotificationSettings?user=${userSettings.username}&dbName=${userSettings.dbName}`, {
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
  return fetch(`${chatConstants.CHAT_SERVER_API}users?user=${userSettings.username}&dbName=${userSettings.dbName}&filter=${filter}&limit=${limit}`, {
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

  return fetch(`${chatConstants.CHAT_SERVER_API}saveTeamRoom`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`,
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    method: 'post',
    body: decodeURI($.param(data))
  });
}

export function getUserAvatar(user) {
  return `${chatConstants.SOCIAL_USER_API}${user}/avatar`;
}

export function getSpaceAvatar(space) {
  // FIXME very ugly, the technical ID should be used here instead
  let spaceId = space.toLowerCase().split(' ').join('_');
  spaceId = encodeSpecialCharacters(spaceId);
  return `${chatConstants.SOCIAL_SPACE_API}${spaceId}/avatar`;
}

export function getUserProfileLink(user) {
  return `${chatConstants.PORTAL}/${chatConstants.PORTAL_NAME}/${chatConstants.PROFILE_PAGE_NAME}/${user}`;
}

export function getSpaceProfileLink(space) {
  // FIXME very ugly, the technical ID should be used here instead
  const spaceId = space.toLowerCase().split(' ').join('_');
  return `${chatConstants.PORTAL}${chatConstants.PROFILE_SPACE_LINK}${spaceId}/${spaceId}`;
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

  return fetch(`${chatConstants.CHAT_SERVER_API}sendMeetingNotes`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`,
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    method: 'post',
    body: decodeURI($.param(data))
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

  return fetch(`${chatConstants.CHAT_SERVER_API}getMeetingNotes`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`,
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    method: 'post',
    body: decodeURI($.param(data))
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

  return fetch(`${chatConstants.PORTAL}/${chatConstants.PORTAL_REST}${chatConstants.CHAT_WIKI_API}saveWiki`, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    credentials: 'include',
    method: 'post',
    body: decodeURI($.param(data))
  }).then(resp =>  resp.json());
}

export function saveEvent(userSettings, data, target) {
  const users = target.participants ? target.participants.map(user => user.name) : [];
  data.space = target.fullName;
  data.users = target.type === 's' ? '' : users.join(',');

  return fetch(`${chatConstants.PORTAL}/${chatConstants.PORTAL_REST}${chatConstants.CHAT_CALENDAR_API}saveEvent`, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    credentials: 'include',
    method: 'post',
    body: decodeURI($.param(data))
  });
}

export function getBaseURL() {
  const port = !window.location.port || window.location.port === DEFAULT_HTTP_PORT ? '':`:${  window.location.port}`;
  return `${window.location.protocol  }//${  window.location.hostname  }${port}`;
}

export function escapeHtml(unsafe) {
  return unsafe
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

export function unescapeHtml(html) {
  return html
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"');
}

export function encodeSpecialCharacters(word) {
  word = encodeURIComponent(word);
  const ASCII = 16;
  return word.replace(/[&<>"']/g, (w) => `%${w.charCodeAt(0).toString(ASCII)}`);
}

function updateStatusElement($profileMenuStatusBtn, status,  statusTitle) {
  $profileMenuStatusBtn.removeClass('uiIconUserAvailable uiIconUserOnline uiIconUserInvisible uiIconUserOffline uiIconUserAway uiIconUserDonotdisturb');
  if (status === 'available') {
    $profileMenuStatusBtn.addClass('uiIconUserAvailable');
  } else if (status === 'away') {
    $profileMenuStatusBtn.addClass('uiIconUserAway');
  } else if (status === 'donotdisturb') {
    $profileMenuStatusBtn.addClass('uiIconUserDonotdisturb');
  } else if (status === 'invisible') {
    $profileMenuStatusBtn.addClass('uiIconUserInvisible');
  }
  $profileMenuStatusBtn.attr('data-original-title', statusTitle);
}
