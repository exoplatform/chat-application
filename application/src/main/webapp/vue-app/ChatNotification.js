import { cCometD } from '../js/lib/chatCometd3.js';

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

      this.portalURI = '/portal';
      this.chatPage = '/portal/intranet/chat';
      this.sendMessageURI = '/chatServer/send';

      if (this.cCometD == null) {
        this.cCometD = this.standalone ? cCometD.getInstance('chat') : cCometD;
    
        if (!this.cCometD.isConfigured) {
          this.cCometD.configure({
            url: window.chatNotification.wsEndpoint,
            'exoId': window.chatNotification.username, // current username
            'exoToken': window.chatNotification.cometdToken // unique token for the
          });
          this.initChatCometd();
          this.initChatCometdHandshake();
          this.initChatConnectionListener();
        }
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
        if (!wasConnected && window.chatNotification.connected) {
          document.dispatchEvent(new CustomEvent('exo-chat-connected', {'detail' : window.chatNotification}));
        } else if (wasConnected && !window.chatNotification.connected) {
          document.dispatchEvent(new CustomEvent('exo-chat-disconnected', {'detail' : window.chatNotification}));
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
    
        // Do what you want with the message...
        if (message.event === 'logout-sent') {
          window.chatNotification.cCometD.disconnect();
          document.dispatchEvent(new CustomEvent('exo-chat-logout-sent', {'detail' : message}));
        } else if (message.event === 'user-status-changed') {
          document.dispatchEvent(new CustomEvent('exo-chat-status-changed', {'detail' : message}));
        } else if (message.event === 'message-sent') {
          document.dispatchEvent(new CustomEvent('exo-chat-message-sent', {'detail' : message}));
        } else if (message.event === 'notification-count-updated') {
          document.dispatchEvent(new CustomEvent('exo-chat-notification-count-updated', {'detail' : message}));
        }
      });
    },
    sendFullMessage : function (room, msg, options, isSystemMessage, callback) {
      const thiss = this;
      $.ajax({
        url: thiss.sendMessageURI,
        data: {
          'sender': thiss.username,
          'room': room,
          'dbName': thiss.dbName,
          'message': encodeURIComponent(msg),
          'options': encodeURIComponent(JSON.stringify(options)),
          'timestamp': new Date().getTime(),
          'isSystem': isSystemMessage
        },
        headers: {
          'Authorization': `Bearer ${thiss.token}`
        }
      }, function (err) {
        if (!err) {
          if (callback && typeof callback === 'function') {
            callback();
          }
        }
      });
    }
  };

  document.addEventListener('exo-chat-message-tosend', (e) => {
    window.chatNotification.sendFullMessage(e.detail.room, e.detail.message, {}, false);
  });

  document.addEventListener('exo-chat-settings-loaded', (e) => {
    window.chatNotification.initSettings(e.detail);
  });
}
