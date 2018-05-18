import { cCometD } from '../js/lib/chatCometd3.js';
import * as desktopNotification from './desktopNotification.js';

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
          this.initChatCometd();
          this.initChatCometdHandshake();
          this.initChatConnectionListener();
        }
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
        if (window.chatNotification.connected === false && handshake.successful === false) {
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
      this.cCometD.subscribe('/service/chat', null, function (event) {
        let message = event.data;
        if (typeof message !== 'object') {
          message = JSON.parse(message);
        }

        // TODO combine all those to one single statement
        if (message.event === 'logout-sent') {
          window.chatNotification.cCometD.disconnect();
          document.dispatchEvent(new CustomEvent('exo-chat-logout-sent', {'detail' : message}));
        } else if (message.event === 'user-status-changed') {
          document.dispatchEvent(new CustomEvent('exo-chat-user-status-changed', {'detail' : message}));
        } else if (message.event === 'notification-count-updated') {
          document.dispatchEvent(new CustomEvent('exo-chat-notification-count-updated', {'detail' : message}));
        } else if (message.event === 'room-member-joined') {
          document.dispatchEvent(new CustomEvent('exo-chat-room-member-joined', {'detail' : message}));
        } else if (message.event === 'room-member-left') {
          document.dispatchEvent(new CustomEvent('exo-chat-room-member-left', {'detail' : message}));
        } else if (message.event === 'room-updated') {
          document.dispatchEvent(new CustomEvent('exo-chat-room-updated', {'detail' : message}));
        } else if (message.event === 'room-deleted') {
          document.dispatchEvent(new CustomEvent('exo-chat-room-deleted', {'detail' : message}));
        } else if (message.event === 'room-settings-updated') {
          const settings = message.data.settings;
          const val = `${settings.notifConditionType}:${settings.notifCondition}`;
          desktopNotification.setRoomPreferredNotificationTrigger(message.room, val);

          document.dispatchEvent(new CustomEvent('exo-chat-room-settings-updated', {'detail' : message}));
        } else if (message.event === 'message-sent') {
          document.dispatchEvent(new CustomEvent('exo-chat-message-received', {'detail' : message}));
        } else if (message.event === 'message-read') {
          document.dispatchEvent(new CustomEvent('exo-chat-message-read', {'detail' : message}));
        } else if (message.event === 'message-updated') {
          document.dispatchEvent(new CustomEvent('exo-chat-message-updated', {'detail' : message}));
        } else if (message.event === 'message-deleted') {
          document.dispatchEvent(new CustomEvent('exo-chat-message-deleted', {'detail' : message}));
        } else if (message.event === 'favorite-added') {
          document.dispatchEvent(new CustomEvent('exo-chat-favorite-added', {'detail' : message}));
        } else if (message.event === 'favorite-removed') {
          document.dispatchEvent(new CustomEvent('exo-chat-favorite-removed', {'detail' : message}));
        }
      });
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
    sendMessage : function (messageObj, callback) {
      const data = {
        'clientId': new Date().getTime().toString(),
        'timestamp': Date.now(),
        'msg': messageObj.message,
        'room': messageObj.room,
        'options': messageObj.options ? messageObj.options : {},
        'isSystem': messageObj.isSystemMessage != null && messageObj.isSystemMessage,
        'user': this.username,
        'fullname': this.fullname
      };

      const content = JSON.stringify({
        'event': 'message-sent',
        'sender': this.username,
        'token': this.token,
        'dbName': this.dbName,
        'data': data
      });

      if (!this.isConnected()) {
        document.dispatchEvent(new CustomEvent('exo-chat-message-not-sent', {'detail' : data}));
        return;
      }
      try {
        this.cCometD.publish('/service/chat', content, function(publishAck) {
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
  };

  document.addEventListener('exo-chat-message-tosend', (e) => {
    window.chatNotification.sendMessage(e.detail);
  });

  document.addEventListener('exo-chat-settings-loaded', (e) => {
    window.chatNotification.initSettings(e.detail);
  });

  document.addEventListener('exo-chat-message-not-sent', () => {
    // TODO store on localstorage to reattempt sending it once connected again
  });
}