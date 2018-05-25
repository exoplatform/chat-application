import { cCometD } from '../js/lib/chatCometd3.js';

const DEFAULT_TIME_TO_SUBSCRIBE = 200;

export function initCometD() {
  window.chatNotification = {
    initSettings : function (settings) {
      this.token = settings.token;
      this.username = settings.username;
      this.sessionId = settings.sessionId;
      this.dbName = settings.dbName;
      this.spaceId = settings.spaceId;
      this.cometdToken = settings.cometdToken;
      this.standalone = settings.standalone === 'true';
      this.wsEndpoint = settings.wsEndpoint;

      if (!this.cCometD) {
        this.cCometD = this.standalone ? cCometD.getInstance('chat') : cCometD;
    
        if (!this.cCometD.isConfigured) {
          this.cCometD.configure({
            url: window.chatNotification.wsEndpoint,
            'exoId': window.chatNotification.username,
            'exoToken': window.chatNotification.cometdToken
          });
        }
        this.initChatCometdHandshake();
        this.initChatConnectionListener();
        this.initChatCometd();
        this.cCometD.handshake();
        this.setStatus(settings.status);
      }
    },
    initChatConnectionListener : function () {
      this.connected = false;
      this.cCometD.addListener('/meta/connect', function (message) {
        if (window.chatNotification.cCometD.isDisconnected()) {
          window.chatNotification.connected = false;
          return;
        }

        const wasConnected = window.chatNotification.connected;
        window.chatNotification.connected = message.successful === true;
        if(window.chatNotification.connected) {
          if (wasConnected) {
            document.dispatchEvent(new CustomEvent('exo-chat-reconnected'));
          } else {
            document.dispatchEvent(new CustomEvent('exo-chat-connected'));
          }
        } else  if(wasConnected) {
          document.dispatchEvent(new CustomEvent('exo-chat-disconnected'));
        }
      });
    },
    initChatCometdHandshake  : function () {
      this.cCometD.addListener('/meta/handshake', function (handshake) {
        if (handshake.successful) {
          window.chatNotification.initChatCometd();
        } else if (window.chatNotification.connected === false) {
          // Reload the page when re-handshake denied.
          $.ajax({
            url: '/portal/rest/chat/api/1.0/user/cometdToken',
            success: function (data) {
              window.chatNotification.cometdToken = data;
              window.chatNotification.cCometD.isConfigured = false;
              window.chatNotification.initChatCometd();
            },
            error: function () {
              window.location.reload(true);
            }
          });
        }
      });
    },
    initChatCometd  : function () {
      setTimeout(() => 
        this.cCometD.subscribe('/service/chat', null, function (event) {
          let message = event.data;
          if (typeof message !== 'object') {
            message = JSON.parse(message);
          }
          document.dispatchEvent(new CustomEvent(`exo-chat-${message.event}`, {'detail' : message}));
        }), DEFAULT_TIME_TO_SUBSCRIBE);
    },
    setStatus : function (status, callback) {
      if (status) {
        this.cCometD.publish('/service/chat', JSON.stringify({
          'event': 'user-status-changed',
          'sender': this.username,
          'room': this.username,
          'dbName': this.dbName,
          'token': this.token,
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
    },
    isConnected: function() {
      return this.cCometD && !this.cCometD.isDisconnected();
    },
    leaveRoom : function (room, callback) {
      const content = JSON.stringify({
        'event': 'room-member-leave',
        'sender': this.username,
        'token': this.token,
        'dbName': this.dbName,
        'room': room
      });
      this.cCometD.publish('/service/chat', content, function(publishAck) {
        if (publishAck && publishAck.successful && callback) {
          callback();
        }
      });
    },
    deleteRoom : function (room, callback) {
      const content = JSON.stringify({
        'event': 'room-deleted',
        'sender': this.username,
        'token': this.token,
        'dbName': this.dbName,
        'room': room
      });
      this.cCometD.publish('/service/chat', content, function(publishAck) {
        if (publishAck && publishAck.successful && callback) {
          callback();
        }
      });
    },
    sendMessage : function (messageObj, callback) {
      const data = {
        'clientId': messageObj.clientId,
        'timestamp': messageObj.timestamp ? messageObj.timestamp : Date.now(),
        'msg': messageObj.message ? messageObj.message : messageObj.msg,
        'msgId': messageObj.msgId,
        'room': messageObj.room,
        'options': messageObj.options ? messageObj.options : {},
        'isSystem': messageObj.isSystemMessage || messageObj.isSystem,
        'user': this.username,
        'fullname': this.fullname
      };

      if (!this.isConnected()) {
        document.dispatchEvent(new CustomEvent('exo-chat-message-not-sent', {'detail' : data}));
        return;
      }

      const content = {
        'event': messageObj.msgId ? 'message-updated' : 'message-sent',
        'room': messageObj.room,
        'sender': this.username,
        'token': this.token,
        'dbName': this.dbName,
        'data': data
      };

      try {
        this.cCometD.publish('/service/chat', JSON.stringify(content), function(publishAck) {
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
    },
    deleteMessage: function (messageObj, callback) {
      const content = {
        'event': 'message-deleted',
        'sender': this.username,
        'token': this.token,
        'dbName': this.dbName,
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
    },
    setRoomMessagesAsRead: function(room, callback) {
      const data = JSON.stringify({
        'event': 'message-read',
        'room': room,
        'sender': this.username,
        'dbName': this.dbName,
        'token': this.token
      });

      cCometD.publish('/service/chat', data, function(publishAck) {
        if (publishAck.successful) {
          if (typeof callback === 'function') {
            callback();
          }
        }
      });
    }
  };

  document.addEventListener('exo-chat-logout-sent', () => {
    window.chatNotification.cCometD.disconnect();
  });
  
  document.addEventListener('exo-chat-message-tosend', (e) => {
    window.chatNotification.sendMessage(e.detail);
  });
  
  document.addEventListener('exo-chat-message-todelete', (e) => {
    window.chatNotification.deleteMessage(e.detail);
  });

  document.addEventListener('exo-chat-settings-loaded', (e) => {
    window.chatNotification.initSettings(e.detail);
  });
}