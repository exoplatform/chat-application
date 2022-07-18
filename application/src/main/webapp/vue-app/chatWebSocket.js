import {chatConstants} from './chatConstants';

const cometDSettings = {
  connected: false
};

export function initSettings(settings) {
  cometDSettings.token = settings.token;
  cometDSettings.username = settings.username;
  cometDSettings.sessionId = settings.sessionId;
  cometDSettings.spaceId = settings.spaceId;
  cometDSettings.fullname = settings.fullName;
  cometDSettings.cometdToken = settings.cometdToken;
  cometDSettings.standalone = settings.standalone === 'true' || settings.standalone === true;
  const loc = window.location;
  const port = loc.port ? `:${loc.port}` : '';
  cometDSettings.wsEndpoint = settings.wsEndpoint.indexOf('://') > -1 ?
    settings.wsEndpoint :
    `${loc.protocol}//${loc.hostname}${port}${settings.wsEndpoint}`;

  if (!cometDSettings.cCometD) {
    cometDSettings.cCometD = cometDSettings.standalone ? cCometD.getInstance('chat') : cCometD;

    if (!cometDSettings.cCometD.isConfigured) {
      const wsConfig = {
        url: cometDSettings.wsEndpoint,
        'exoId': cometDSettings.username,
        'exoToken': cometDSettings.cometdToken
      };
      cometDSettings.cCometD.configure(wsConfig);
    }
  }
  if (!cometDSettings.chatSubscription) {
    initChatCometdHandshake();
    initChatConnectionListener();
    initChatCometd();
    setStatus(settings.status);
  }
}
export function initChatConnectionListener() {
  cometDSettings.connected = false;
  cometDSettings.cCometD.addListener('/meta/connect', function (message) {
    if (cometDSettings.cCometD.isDisconnected()) {
      eXo.chat.isOnline = false;
      cometDSettings.connected = false;
      return;
    }

    const wasConnected = cometDSettings.connected;
    eXo.chat.isOnline = cometDSettings.connected = message.successful === true;
    if (cometDSettings.connected) {
      if (wasConnected) {
        document.dispatchEvent(new CustomEvent(chatConstants.EVENT_RECONNECTED));
      } else {
        document.dispatchEvent(new CustomEvent(chatConstants.EVENT_CONNECTED));
      }
    } else  if (wasConnected) {
      document.dispatchEvent(new CustomEvent(chatConstants.EVENT_DISCONNECTED));
    }
  });
  cometDSettings.cCometD.addListener('/meta/disconnect', function(message) {
    if (message.successful) {
      document.dispatchEvent(new CustomEvent(chatConstants.EVENT_DISCONNECTED));
    }
  });
}
export function initChatCometdHandshake() {
  cometDSettings.cCometD.addListener('/meta/handshake', function (handshake) {
    if (handshake.successful) {
      initChatCometd();
    } else if (cometDSettings.connected === false) {
      renewToken();
    }
  });
}

function initChatCometd() {
  if (cometDSettings.chatSubscription) {
    cometDSettings.cCometD.resubscribe(cometDSettings.chatSubscription);
  } else {
    cometDSettings.chatSubscription = cometDSettings.cCometD.subscribe('/service/chat', null, function (event) {
      let message = event.data;
      if (typeof message !== 'object') {
        message = JSON.parse(message);
      }
      document.dispatchEvent(new CustomEvent(`exo-chat-${message.event}`, {'detail': message}));
    }, null, function(subscribeReply) {
      if (subscribeReply.successful) {
        cometDSettings.chatSubscription = subscribeReply;
        document.dispatchEvent(new CustomEvent(chatConstants.EVENT_CONNECTED));
      }
    });
  }
}

export function setStatus(status, callback, errorCallback) {
  if (status && isConnected()) {
    cometDSettings.cCometD.publish('/service/chat', JSON.stringify({
      'event': 'user-status-changed',
      'sender': cometDSettings.username,
      'room': cometDSettings.username,
      'token': cometDSettings.token,
      'data': {
        'status': status
      }
    }), function (publishAck) {
      if (publishAck.successful) {
        if (typeof callback === 'function') {
          if (document.readyState === 'complete') {
            callback(status);
          } else {
            errorCallback(status);
          }
        }
      } else if (errorCallback && typeof errorCallback === 'function') {
        errorCallback();
      }
    });
  } else if (errorCallback && typeof errorCallback === 'function') {
    errorCallback();
  }
}
export function isConnected() {
  return cometDSettings.cCometD && !cometDSettings.cCometD.isDisconnected() && cometDSettings.chatSubscription;
}
export function leaveRoom(room, callback) {
  if (!cometDSettings.cCometD) {
    return;
  }
  const content = JSON.stringify({
    'event': 'room-member-leave-requested',
    'clientId': new Date().getTime().toString(),
    'sender': cometDSettings.username,
    'token': cometDSettings.token,
    'room': room,
    'options': {
      'fullName': cometDSettings.fullname
    }
  });
  cometDSettings.cCometD.publish('/service/chat', content, function(publishAck) {
    if (publishAck && publishAck.successful && callback) {
      callback();
    }
  });
}
export function deleteRoom(room, callback) {
  if (!cometDSettings.cCometD) {
    return;
  }
  const content = JSON.stringify({
    'event': 'room-deleted',
    'sender': cometDSettings.username,
    'token': cometDSettings.token,
    'room': room
  });
  cometDSettings.cCometD.publish('/service/chat', content, function(publishAck) {
    if (publishAck && publishAck.successful && callback) {
      callback();
    }
  });
}
export function sendMessage(messageObj, callback) {
  const data = {
    'clientId': messageObj.clientId,
    'timestamp': messageObj.timestamp ? messageObj.timestamp : Date.now(),
    'msg': messageObj.message ? messageObj.message : messageObj.msg,
    'msgId': messageObj.msgId,
    'room': messageObj.room,
    'options': messageObj.options ? messageObj.options : {},
    'isSystem': messageObj.isSystemMessage || messageObj.isSystem,
    'user': cometDSettings.username,
    'fullname': cometDSettings.fullname
  };

  if (!isConnected() || !navigator.onLine) {
    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_MESSAGE_NOT_SENT, {'detail': data}));
    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_DISCONNECTED));
    return;
  }

  const content = {
    'event': messageObj.msgId ? 'message-updated' : 'message-sent',
    'room': messageObj.room,
    'sender': cometDSettings.username,
    'token': cometDSettings.token,
    'data': data
  };

  try {
    cometDSettings.cCometD.publish('/service/chat', JSON.stringify(content), function(publishAck) {
      if (!publishAck || !publishAck.successful) {
        document.dispatchEvent(new CustomEvent(chatConstants.EVENT_MESSAGE_NOT_SENT, {'detail': data}));
      } else {
        document.dispatchEvent(new CustomEvent(chatConstants.EVENT_MESSAGE_SENT, {detail: content}));
        if (callback) {
          callback(data);
        }
      }
    });
  } catch (e) {
    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_MESSAGE_NOT_SENT, {'detail': data}));
  }
}
export function deleteMessage(messageObj, callback) {
  if (!cometDSettings.cCometD) {
    return;
  }
  const content = {
    'event': 'message-deleted',
    'sender': cometDSettings.username,
    'token': cometDSettings.token,
    'room': messageObj.room,
    'data': {
      'msgId': messageObj.msgId
    }
  };
  cometDSettings.cCometD.publish('/service/chat', JSON.stringify(content), function (publishAck) {
    if (publishAck.successful) {
      if (typeof callback === 'function') {
        callback();
      }
    }
  });
}
export function setRoomMessagesAsRead(room, callback) {
  const data = JSON.stringify({
    'event': 'message-read',
    'sender': cometDSettings.username,
    'token': cometDSettings.token,
    'room': room
  });

  if (cometDSettings.cCometD) {
    cometDSettings.cCometD.publish('/service/chat', data, function(publishAck) {
      if (publishAck.successful) {
        if (typeof callback === 'function') {
          callback();
        }
      }
    });
  }
}

function renewToken() {
  $.ajax({
    url: '/portal/rest/chat/api/1.0/user/cometdToken',
    success: function (data) {
      cometDSettings.cometdToken = data;
      cometDSettings.cCometD.isConfigured = false;

      const wsConfig = {
        url: cometDSettings.wsEndpoint,
        'exoId': cometDSettings.username,
        'exoToken': cometDSettings.cometdToken
      };
      cometDSettings.cCometD.configure(wsConfig);
      initChatCometd();
    },
    error: e => console.error('Error while retrieving cometd token from server', e)
  });
}
export function extensionAddEventListener(){
  document.addEventListener(chatConstants.EVENT_LOGGED_OUT, (e) => {
    const message = e.detail;

    if (message && message.data && message.data.sessionId === cometDSettings.sessionId) {
      cometDSettings.cCometD.disconnect();
      cometDSettings.cCometD.explicitlyDisconnected = true;
    }
  });

  document.addEventListener(chatConstants.ACTION_MESSAGE_SEND, (e) => {
    const message = e.detail;
    sendMessage(message);
  });

  document.addEventListener(chatConstants.ACTION_MESSAGE_DELETE, (e) => {
    deleteMessage(e.detail);
  });

  document.addEventListener(chatConstants.ACTION_ROOM_SET_READ, (e) => {
    if (e.detail && e.detail.trim()) {
      setRoomMessagesAsRead(e.detail);
    }
  });
}

export function extensionRemoveEventListener(){
  document.removeEventListener(chatConstants.EVENT_LOGGED_OUT, (e) => {
    const message = e.detail;

    if (message && message.data && message.data.sessionId === cometDSettings.sessionId) {
      cometDSettings.cCometD.disconnect();
      cometDSettings.cCometD.explicitlyDisconnected = true;
    }
  });

  document.removeEventListener(chatConstants.ACTION_MESSAGE_SEND, (e) => {
    const message = e.detail;
    sendMessage(message);
  });

  document.removeEventListener(chatConstants.ACTION_MESSAGE_DELETE, (e) => {
    deleteMessage(e.detail);
  });

  document.removeEventListener(chatConstants.ACTION_ROOM_SET_READ, (e) => {
    if (e.detail && e.detail.trim()) {
      setRoomMessagesAsRead(e.detail);
    }
  });
}
