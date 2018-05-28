import { cCometD } from '../js/lib/chatCometd3.js';

const DEFAULT_TIME_TO_SUBSCRIBE = 200;

const cometDSettings = {
  connected: false
};

export function initSettings(settings) {
  cometDSettings.token = settings.token;
  cometDSettings.username = settings.username;
  cometDSettings.sessionId = settings.sessionId;
  cometDSettings.dbName = settings.dbName;
  cometDSettings.spaceId = settings.spaceId;
  cometDSettings.cometdToken = settings.cometdToken;
  cometDSettings.standalone = settings.standalone === 'true' || settings.standalone === true;
  cometDSettings.wsEndpoint = settings.wsEndpoint;
  if (!cometDSettings.cCometD) {
    cometDSettings.cCometD = cometDSettings.standalone ? cCometD.getInstance('chat') : cCometD;

    if (!cometDSettings.cCometD.isConfigured) {
      cometDSettings.cCometD.configure({
        url: cometDSettings.wsEndpoint,
        'exoId': cometDSettings.username,
        'exoToken': cometDSettings.cometdToken
      });
    }
    initChatCometdHandshake();
    initChatConnectionListener();
    initChatCometd();
    cometDSettings.cCometD.handshake();
    setStatus(settings.status);
  }
}
export function initChatConnectionListener() {
  cometDSettings.connected = false;
  cometDSettings.cCometD.addListener('/meta/connect', function (message) {
    if (cometDSettings.cCometD.isDisconnected()) {
      cometDSettings.connected = false;
      return;
    }

    const wasConnected = cometDSettings.connected;
    cometDSettings.connected = message.successful === true;
    if(cometDSettings.connected) {
      if (wasConnected) {
        document.dispatchEvent(new CustomEvent('exo-chat-reconnected'));
      } else {
        document.dispatchEvent(new CustomEvent('exo-chat-connected'));
      }
    } else  if(wasConnected) {
      document.dispatchEvent(new CustomEvent('exo-chat-disconnected'));
    }
  });
}
export function initChatCometdHandshake() {
  cometDSettings.cCometD.addListener('/meta/handshake', function (handshake) {
    if (handshake.successful) {
      initChatCometd();
    } else if (cometDSettings.connected === false) {
      // Reload the page when re-handshake denied.
      $.ajax({
        url: '/portal/rest/chat/api/1.0/user/cometdToken',
        success: function (data) {
          cometDSettings.cometdToken = data;
          cometDSettings.cCometD.isConfigured = false;
          initChatCometd();
        },
        error: function () {
          window.location.reload(true);
        }
      });
    }
  });
}

export function initChatCometd() {
  setTimeout(() => 
    cometDSettings.cCometD.subscribe('/service/chat', null, function (event) {
      let message = event.data;
      if (typeof message !== 'object') {
        message = JSON.parse(message);
      }
      document.dispatchEvent(new CustomEvent(`exo-chat-${message.event}`, {'detail' : message}));
    }), DEFAULT_TIME_TO_SUBSCRIBE);
}

export function setStatus(status, callback) {
  if (status) {
    cometDSettings.cCometD.publish('/service/chat', JSON.stringify({
      'event': 'user-status-changed',
      'sender': cometDSettings.username,
      'room': cometDSettings.username,
      'dbName': cometDSettings.dbName,
      'token': cometDSettings.token,
      'data': {
        'status': status
      }
    }), function (publishAck) {
      if (publishAck.successful) {
        if (typeof callback === 'function') {
          callback(status);
        }
      }
    });
  }
}
export function isConnected() {
  return cometDSettings.cCometD && !cometDSettings.cCometD.isDisconnected();
}
export function leaveRoom(room, callback) {
  const content = JSON.stringify({
    'event': 'room-member-leave',
    'sender': cometDSettings.username,
    'token': cometDSettings.token,
    'dbName': cometDSettings.dbName,
    'room': room
  });
  cometDSettings.cCometD.publish('/service/chat', content, function(publishAck) {
    if (publishAck && publishAck.successful && callback) {
      callback();
    }
  });
}
export function deleteRoom(room, callback) {
  const content = JSON.stringify({
    'event': 'room-deleted',
    'sender': cometDSettings.username,
    'token': cometDSettings.token,
    'dbName': cometDSettings.dbName,
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

  if (!isConnected()) {
    document.dispatchEvent(new CustomEvent('exo-chat-message-not-sent', {'detail' : data}));
    return;
  }

  const content = {
    'event': messageObj.msgId ? 'message-updated' : 'message-sent',
    'room': messageObj.room,
    'sender': cometDSettings.username,
    'token': cometDSettings.token,
    'dbName': cometDSettings.dbName,
    'data': data
  };

  try {
    cometDSettings.cCometD.publish('/service/chat', JSON.stringify(content), function(publishAck) {
      if (!publishAck || !publishAck.successful) {
        document.dispatchEvent(new CustomEvent('exo-chat-message-not-sent', {'detail' : data}));
      } else {
        if (callback) {
          callback(data);
        }
      }
    });
  } catch (e) {
    document.dispatchEvent(new CustomEvent('exo-chat-message-not-sent', {'detail' : data}));
  }
}
export function deleteMessage(messageObj, callback) {
  const content = {
    'event': 'message-deleted',
    'sender': cometDSettings.username,
    'token': cometDSettings.token,
    'dbName': cometDSettings.dbName,
    'room': messageObj.room,
    'data': {
      'msgId': messageObj.msgId
    }
  };
  cCometD.publish('/service/chat', JSON.stringify(content), function (publishAck) {
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
    'room': room,
    'sender': cometDSettings.username,
    'dbName': cometDSettings.dbName,
    'token': cometDSettings.token
  });

  cCometD.publish('/service/chat', data, function(publishAck) {
    if (publishAck.successful) {
      if (typeof callback === 'function') {
        callback();
      }
    }
  });
}

document.addEventListener('exo-chat-logout-sent', () => {
  cometDSettings.cCometD.disconnect();
});

document.addEventListener('exo-chat-message-tosend', (e) => {
  sendMessage(e.detail);
});

document.addEventListener('exo-chat-message-todelete', (e) => {
  deleteMessage(e.detail);
});

document.addEventListener('exo-chat-settings-loaded', (e) => {
  initSettings(e.detail);
});
