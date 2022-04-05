import {chatConstants} from './chatConstants.js';
import * as chatWebStorage from './chatWebStorage';
import * as chatWebSocket from './chatWebSocket';
import * as desktopNotification from './desktopNotification';


const DEFAULT_HTTP_PORT = 80;
const REATTEMPT_INIT_PERIOD = 1000;
const MAX_UNREAD_NUMBER = 99;

export function getUserAvatar(user) {
  return `${chatConstants.SOCIAL_USER_API}${user}/avatar`;
}

export function getSpaceAvatar(space) {
  return `${chatConstants.SOCIAL_SPACE_API}${space}/avatar`;
}

export function getUserStatus(userSettings, user) {
  return fetch(`${chatConstants.CHAT_SERVER_API}getStatus?user=${userSettings.username}&targetUser=${user}`, {
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
  if (!withDetails) {
    withDetails = '';
  }
  return fetch(`${chatConstants.CHAT_SERVER_API}notification?user=${userSettings.username}&withDetails=${withDetails}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function initChatSettings(username, isMiniChat, userSettingsLoadedCallback, chatRoomsLoadedCallback, isChatDrawer) {
  if (!eXo) { eXo = {}; }
  if (!eXo.chat) { eXo.chat = {}; }
  if (!eXo.chat.userSettings) { eXo.chat.userSettings = {}; }

  document.addEventListener(chatConstants.EVENT_USER_SETTINGS_LOADED, (e) => {
    const settings = e.detail;
    if (isMiniChat) {
      //mini chat only need fetching notifications
      getNotReadMessages(settings).then(notifications => chatRoomsLoadedCallback(notifications));
    } else {
      // Get the selected room type filter
      let roomTypeFilter = '';
      if (!isChatDrawer) {
        roomTypeFilter = chatWebStorage.getStoredParam(chatConstants.STORED_PARAM_TYPE_FILTER, chatConstants.TYPE_FILTER_DEFAULT);
      }
      // fetch online users then fetch chat rooms
      getOnlineUsers().then(users => getUserChatRooms(settings, users, '', roomTypeFilter)).then(data => chatRoomsLoadedCallback(data));
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
  if (!eXo) { eXo = {}; }
  if (!eXo.chat) { eXo.chat = {}; }
  if (!eXo.chat.userSettings) { eXo.chat.userSettings = {}; }
  try {
    eXo.chat.userSettings = userSettings;
    chatWebSocket.initSettings(userSettings);
    eXo.chat.userSettings.chatPage = getBaseURL() + eXo.chat.userSettings.chatPage;
    userSettingsLoadedCallback(userSettings);
    
    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_USER_SETTINGS_LOADED, {'detail': userSettings}));
  } catch (e) {
    console.error('Error initializing chat settings', e);
    setTimeout(() => initSettings(status, userSettings, userSettingsLoadedCallback), REATTEMPT_INIT_PERIOD);
  }
}

export function updateTotalUnread(totalUnreadMsg) {
  const pageTitle = document.title.replace(/\(\+?\d{1,2}\)/,'');
  if (totalUnreadMsg && totalUnreadMsg > 0 && totalUnreadMsg <= MAX_UNREAD_NUMBER) {
    document.title = pageTitle.concat(`(${totalUnreadMsg})`);
  } else if (totalUnreadMsg > MAX_UNREAD_NUMBER) {
    document.title = pageTitle.concat('(+99)');
  } else {
    document.title = pageTitle;
  }
}

export function setProfileStatus(event) {
  const contactChanged = event.detail;
  const status = contactChanged.data ? contactChanged.data.status : contactChanged.status;
  const username = contactChanged.sender;
  if (username && status) {
    const $profileMenuStatusBtn = $(`.uiProfileMenu .profileMenuNavHeader [data-userid='${username}'] i`);
    if ($profileMenuStatusBtn.length) {
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

export function sendMentionNotification(roomId, roomName, mentionedUsers) {
  const sender = eXo.chat.userSettings.username;
  const senderFullName = eXo.chat.userSettings.fullName;
  const MentionModel = {
    roomId,
    roomName,
    mentionedUsers,
    sender,
    senderFullName
  };
  return fetch(`${chatConstants.PORTAL}/${chatConstants.PORTAL_REST}${chatConstants.CHAT_API}mentionNotifications`, {
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include',
    method: 'POST',
    body: JSON.stringify(MentionModel)
  }).then((data) => {
    return data.json();
  });
}

export function sendMessageReceivedNotification(roomId, roomName, message, receivers){
  const sender = eXo.chat.userSettings.username;
  const senderFullName = eXo.chat.userSettings.fullName;
  const MessageReceived = {
    roomId,
    roomName,
    message,
    sender,
    senderFullName,
    receivers
  };
  return fetch(`${chatConstants.PORTAL}/${chatConstants.PORTAL_REST}${chatConstants.CHAT_API}messageReceivedNotification`, {
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include',
    method: 'POST',
    body: JSON.stringify(MessageReceived)
  }).then((data) => {
    return data;
  });

}

export function getUserState(user) {
  return fetch(`${chatConstants.PORTAL}/${chatConstants.PORTAL_REST}${chatConstants.CHAT_API}getUserState?user=${user}`, {credentials: 'include'})
    .then(resp => resp.json());
}

export function updateUser(userSettings, targetUser, isDeleted, isEnabled, isExternal) {
  return fetch(`${chatConstants.CHAT_SERVER_API}updateUser?user=${userSettings.username}&targetUser=${targetUser}&isDeleted=${isDeleted}&isEnabled=${isEnabled}&isExternal=${isExternal}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.text());
}

export function setExternal(userSettings, targetUser, isExternal) {
  return fetch(`${chatConstants.CHAT_SERVER_API}setExternal?user=${userSettings.username}&targetUser=${targetUser}&isExternal=${isExternal}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.text());
}

export function toggleFavorite(room, user, favorite) {
  if ((!room || !room.trim().length) && user && user.trim().length) {
    return getRoomId(eXo.chat.userSettings, user, 'username').then((roomId) => {
      room = roomId;
      return fetch(`${chatConstants.CHAT_SERVER_API}toggleFavorite?user=${eXo.chat.userSettings.username}&targetUser=${room}&favorite=${favorite}`, {
        headers: {
          'Authorization': `Bearer ${eXo.chat.userSettings.token}`
        }}).then(resp =>  resp.text());
    });
  } else {
    return fetch(`${chatConstants.CHAT_SERVER_API}toggleFavorite?user=${eXo.chat.userSettings.username}&targetUser=${room}&favorite=${favorite}`, {
      headers: {
        'Authorization': `Bearer ${eXo.chat.userSettings.token}`
      }}).then(resp =>  resp.text());
  }
}

export function getReceiversForMessagePushNotif(userSettings,users,roomId){
  return fetch(`${chatConstants.CHAT_SERVER_API}filterOutSilentUsers?roomUsers=${users}&roomId=${roomId}&user=${userSettings.username}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}
export function getChatRooms(userSettings, onlineUsers, filter, limit) {
  if (!limit) {
    limit = chatConstants.ROOMS_PER_PAGE;
  }
  if (!filter) {
    filter = '';
  }
  return fetch(`${chatConstants.CHAT_SERVER_API}whoIsOnline?user=${userSettings.username}&onlineUsers=${onlineUsers}&filter=${filter}&limit=${limit}&timestamp=${new Date().getTime()}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getUserChatRooms(userSettings, onlineUsers, filter, roomType, offset, limit) {
  if (!limit) {
    limit = chatConstants.ROOMS_PER_PAGE;
  }
  if (!offset) {
    offset = chatConstants.DEFAULT_OFFSET;
  }
  if (!filter) {
    filter = '';
  }
  if (!roomType || roomType === 'All') {
    roomType = '';
  }
  return fetch(`${chatConstants.CHAT_SERVER_API}userRooms?user=${userSettings.username}&onlineUsers=${onlineUsers}&filter=${filter}&offset=${offset}&limit=${limit}&roomType=${roomType}&timestamp=${new Date().getTime()}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getRoomParticipants(userSettings, room, onlineUsers, limit, onlineUsersOnly) {
  if (!limit && isNaN(limit)) {
    limit = chatConstants.DEFAULT_USER_LIMIT;
  }
  onlineUsersOnly = onlineUsersOnly && onlineUsersOnly === true;

  if (!onlineUsers) {
    onlineUsers = '';
  }
  return fetch(`${chatConstants.CHAT_SERVER_API}users?user=${userSettings.username}&room=${room.room}&onlineUsers=${onlineUsers}&limit=${limit}&onlineOnly=${onlineUsersOnly}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getRoomParticipantsCount(userSettings, room) {
  return fetch(`${chatConstants.CHAT_SERVER_API}usersCount?user=${userSettings.username}&room=${room.room}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getUsersToMention(userSettings, room, filter) {
  return fetch(`${chatConstants.CHAT_SERVER_API}users?user=${userSettings.username}&room=${room.room}&filter=${filter}&limit=10`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getRoomId(userSettings, targetUser, fieldName) {
  return fetch(`${chatConstants.CHAT_SERVER_API}getRoom?targetUser=${targetUser}&user=${userSettings.username}&type=${fieldName}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.text());
}

export function getRoomDetail(userSettings, room) {
  return fetch(`${chatConstants.CHAT_SERVER_API}getRoom?targetUser=${room}&user=${userSettings.username}&withDetail=true&type=room-id`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getRoomMessages(userSettings, contact, toTimestamp, limit) {
  if (!limit) {
    limit = '';
  }
  if (!toTimestamp) {
    toTimestamp = '';
  }
  return fetch(`${chatConstants.CHAT_SERVER_API}read?user=${userSettings.username}&room=${contact.room}&toTimestamp=${toTimestamp}&limit=${limit}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function setRoomNotificationTrigger(userSettings, room, notifConditionType, notifCondition, time) {
  return fetch(`${chatConstants.CHAT_SERVER_API}setRoomNotificationTrigger?user=${userSettings.username}&room=${room}&notifConditionType=${notifConditionType}&notifCondition=${notifCondition}&time=${time}`, {
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
  return fetch(`${chatConstants.CHAT_SERVER_API}setNotificationSettings?user=${userSettings.username}${preferredNotificationParam}${preferredNotificationTriggerParam}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function setUserPreferredNotification(userSettings, notifManner) {
  return fetch(`${chatConstants.CHAT_SERVER_API}setPreferredNotification?user=${userSettings.username}&notifManner=${notifManner}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function getUserNotificationSettings(userSettings) {
  return fetch(`${chatConstants.CHAT_SERVER_API}getUserDesktopNotificationSettings?user=${userSettings.username}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function loadNotificationSettings(settings) {
  if (settings && settings.userDesktopNotificationSettings) {
    eXo.chat.desktopNotificationSettings = settings.userDesktopNotificationSettings;
    if (eXo.chat.desktopNotificationSettings.preferredNotification) {
      eXo.chat.desktopNotificationSettings.preferredNotification = JSON.parse(eXo.chat.desktopNotificationSettings.preferredNotification);
    } else {
      eXo.chat.desktopNotificationSettings.preferredNotification = [];
    }
    if (eXo.chat.desktopNotificationSettings.preferredNotificationTrigger) {
      eXo.chat.desktopNotificationSettings.preferredNotificationTrigger = JSON.parse(eXo.chat.desktopNotificationSettings.preferredNotificationTrigger);
    } else {
      eXo.chat.desktopNotificationSettings.preferredNotificationTrigger = [];
    }
    if (eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger) {
      eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger = JSON.parse(eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger);
    } else {
      eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger = [];
    }
  }
}

export function getChatUsers(userSettings, filter, limit) {
  if (!limit) {
    limit = chatConstants.DEFAULT_USER_LIMIT;
  }
  return fetch(`${chatConstants.CHAT_SERVER_API}users?user=${userSettings.username}&filter=${filter}&limit=${limit}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.json());
}

export function saveRoom(userSettings, roomName, users, room) {
  const data = {
    teamName: roomName,
    users: users,
    user: userSettings.username,
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

export function updateRoomMeetingStatus(userSettings, room, start, startTime) {
  const data = {
    user: userSettings.username,
    room: room,
    start: start,
    startTime: startTime
  };

  return fetch(`${chatConstants.CHAT_SERVER_API}updateRoomMeetingStatus`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`,
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    method: 'post',
    body: decodeURI($.param(data))
  });
}

export function getUserInfo(userName) {
  return fetch(`${chatConstants.SOCIAL_USER_API}${userName}`,{
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
  });
}

export function getRoomParticipantsToSuggest(usersList) {
  return fetch(`${chatConstants.PORTAL}/${chatConstants.PORTAL_REST}${chatConstants.CHAT_API}getRoomParticipantsToSuggest`, {
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include',
    method: 'POST',
    body: JSON.stringify(usersList),
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
    else {
      throw new Error ('Error when loading user list');
    }
  });
}

export function getModalParticipantsToSuggest(usersList) {
  return fetch(`${chatConstants.PORTAL}/${chatConstants.PORTAL_REST}${chatConstants.CHAT_API}getModalParticipantsToSuggest`, {
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include',
    method: 'POST',
    body: JSON.stringify(usersList),
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
    else {
      throw new Error ('Error when loading user list');
    }
  });
}


export function getUserProfileLink(user) {
  return `${chatConstants.PORTAL}/${chatConstants.PORTAL_NAME}/${chatConstants.PROFILE_PAGE_NAME}/${user}`;
}

export function getSpaceProfileLink(groupId,prettyName) {
  return `${chatConstants.PORTAL}${chatConstants.PROFILE_SPACE_LINK}${groupId.replaceAll('/',':')}/${prettyName}`;
}

export function getSpaceByPrettyName(prettyName) {
  return fetch(`${chatConstants.SOCIAL_SPACE_API}${chatConstants.BY_PRETTY_NAME}${prettyName}`, {
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
    else {
      throw new Error ('Error when loading space');
    }
  });
}

export function sendMeetingNotes(userSettings, room, fromTimestamp, toTimestamp) {
  const data = {
    user: userSettings.username,
    room: room,
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

export function updateRoomEnabled(userSettings, spaceId, enabled) {
  const data = {
    user: userSettings.username,
    spaceId: spaceId,
    enabled: enabled,
  };

  return fetch('/chatServer/updateRoomEnabled', {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`,
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    method: 'post',
    body: decodeURI($.param(data))
  });
}

export function isRoomEnabled(userSettings, spaceId) {
  return fetch(`/chatServer/isRoomEnabled?user=${userSettings.username}&spaceId=${spaceId}`, {
    headers: {
      'Authorization': `Bearer ${userSettings.token}`
    }}).then(resp =>  resp.text());
}
