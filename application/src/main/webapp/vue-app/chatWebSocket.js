const cometDSettings = {
  connected: false
};

export function initSettings(settings) {
  cometDSettings.token = settings.token;
  cometDSettings.username = settings.username;
  cometDSettings.sessionId = settings.sessionId;
  cometDSettings.dbName = settings.dbName;
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
  if(!cometDSettings.chatSubscription) {
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
  cometDSettings.cCometD.addListener('/meta/disconnect', function(message) {
    if (message.successful) {
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

function initChatCometd() {
  if(cometDSettings.chatSubscription) {
    cometDSettings.cCometD.resubscribe(cometDSettings.chatSubscription);
  } else {
    cometDSettings.chatSubscription = cometDSettings.cCometD.subscribe('/service/chat', null, function (event) {
      let message = event.data;
      if (typeof message !== 'object') {
        message = JSON.parse(message);
      }
      document.dispatchEvent(new CustomEvent(`exo-chat-${message.event}`, {'detail' : message}));
    }, null, function(subscribeReply) {
      if (subscribeReply.successful) {
        cometDSettings.chatSubscription = subscribeReply;
        document.dispatchEvent(new CustomEvent('exo-chat-connected'));
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
      } else if(errorCallback && typeof errorCallback === 'function') {
        errorCallback();
      }
    });
  } else if(errorCallback && typeof errorCallback === 'function') {
    errorCallback();
  }
}
export function isConnected() {
  return cometDSettings.cCometD && !cometDSettings.cCometD.isDisconnected() && cometDSettings.chatSubscription;
}
export function leaveRoom(room, callback) {
  const content = JSON.stringify({
    'event': 'room-member-leave',
    'clientId': new Date().getTime().toString(),
    'sender': cometDSettings.username,
    'token': cometDSettings.token,
    'dbName': cometDSettings.dbName,
    'room': room,
    'options' : {
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
    document.dispatchEvent(new CustomEvent('exo-chat-disconnected'));
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
