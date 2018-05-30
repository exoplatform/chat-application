export const LAST_SELECTED_ROOM_PARAM = 'lastSelectedRoom';

const STORED_NOT_SENT_MESSAGES = 'roomNotSentMessages';

const NB_MILLISECONDS_PERD_SECOND = 1000;
const DEFAULT_EXPIRATION_PERIOD = 300;

const MAX_RESEND_MESSAGE_ATTEMPT = 2;

const RESEND_MESSAGE_PERIOD = 5000;

let resendIntervalID;

export function getStoredParam(key, defaultValue) {
  let ts  = localStorage.getItem(`${key}TS`);
  const val = localStorage.getItem(key);
  if (!ts) {
    ts=-1;
  }
  const now = Math.round(new Date() / NB_MILLISECONDS_PERD_SECOND);
  if (val && (now < ts || ts === -1 )) {
    return val;
  } else {
    localStorage.removeItem(key);
    localStorage.removeItem(`${key}TS`);
    localStorage.removeItem(key);
  }
  return defaultValue;
}

export function setStoredParam(key, value, expire) {
  if(value && value.length) {
    if (expire && expire > -1) {
      expire = expire ? expire : DEFAULT_EXPIRATION_PERIOD;
      localStorage.setItem(`${key}TS`, Math.round(new Date() / NB_MILLISECONDS_PERD_SECOND) + expire);
    }
    localStorage.setItem(key, value);
  } else {
    localStorage.removeItem(key);
    localStorage.removeItem(`${key}TS`);
  }
}

export function getRoomNotSentMessages(username, room) {
  const roomNotSentMessages = [];
  if (room && username) {
    const notSentMessages = getNotSentMessages(username);
    if(notSentMessages && Object.keys(notSentMessages).length) {
      Object.keys(notSentMessages).forEach(clientId => {
        const notSenMessage = notSentMessages[clientId];
        if (notSenMessage && notSenMessage.room === room) {
          roomNotSentMessages.push(notSenMessage);
        }
      });
    }
  }
  return roomNotSentMessages;
}

export function getNotSentMessages(username) {
  let notSentMessages = getStoredParam(`${STORED_NOT_SENT_MESSAGES}-${username}`);
  if(notSentMessages) {
    notSentMessages = JSON.parse(notSentMessages);
    if(Array.isArray(notSentMessages)) {
      notSentMessages = {};
      localStorage.removeItem(`${STORED_NOT_SENT_MESSAGES}-${username}`);
    }
    return notSentMessages;
  } else {
    return {};
  }
}

export function storeNotSentMessage(messageToStore) {
  if(!messageToStore) {
    return;
  }
  const user = messageToStore.sender ? messageToStore.sender : messageToStore.data && messageToStore.data.sender ? messageToStore.data.sender : messageToStore.user ? messageToStore.user : messageToStore.data ? messageToStore.data.user : null;
  const clientId = messageToStore.clientId ? messageToStore.clientId : messageToStore.data ? messageToStore.data.clientId : null;
  const room = messageToStore.room ? messageToStore.room : messageToStore.data ? messageToStore.data.room : null;
  if (user !== eXo.chat.userSettings.username || !room || !clientId) {
    return;
  }
  const notSentMessages = getNotSentMessages(user);
  const foundMessage = notSentMessages[clientId];
  if (!foundMessage) {
    messageToStore.notSent = true;
    notSentMessages[clientId] = messageToStore;
    setStoredParam(`${STORED_NOT_SENT_MESSAGES}-${user}`, JSON.stringify(notSentMessages));
  }
}

export function storeMessageAsSent(messageToStore) {
  const user = messageToStore.sender ? messageToStore.sender : messageToStore.data && messageToStore.data.sender ? messageToStore.data.sender : messageToStore.user ? messageToStore.user : messageToStore.data ? messageToStore.data.user : null;
  const clientId = messageToStore.clientId ? messageToStore.clientId : messageToStore.data ? messageToStore.data.clientId : null;
  const room = messageToStore.room ? messageToStore.room : messageToStore.data ? messageToStore.data.room : null;
  if (user !== eXo.chat.userSettings.username || !room || !clientId) {
    return false;
  }
  messageToStore.notSent = false;
  return deleteFromStore(user, clientId);
}

export function deleteFromStore(user, clientId) {
  const notSentMessages = getNotSentMessages(user);
  if (notSentMessages && notSentMessages[clientId]) {
    delete notSentMessages[clientId];
    setStoredParam(`${STORED_NOT_SENT_MESSAGES}-${user}`, JSON.stringify(notSentMessages));
    return true;
  }
  return false;
}

export function sendFailedMessages() {
  if(!getStoredParam('messagesSending')) {
    setStoredParam('messagesSending', 'true', RESEND_MESSAGE_PERIOD / NB_MILLISECONDS_PERD_SECOND);
    try {
      const notSentMessages = getNotSentMessages(eXo.chat.userSettings.username);
      if(notSentMessages && Object.keys(notSentMessages).length) {
        Object.keys(notSentMessages).forEach(clientId => {
          const messageToResend = notSentMessages[clientId];
          if(messageToResend) {
            if (eXo.chat.isOnline) {
              if(messageToResend.attemptCount > MAX_RESEND_MESSAGE_ATTEMPT) {
                // Give up retrying send message when the user is online
                // but the message sending always fails
                storeMessageAsSent(messageToResend);
              } else {
                messageToResend.attemptCount = messageToResend.attemptCount ? messageToResend.attemptCount + 1 : 1;
              }
            }
            document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : messageToResend}));
          }
        });
      }
      setStoredParam('messagesSending');
    } catch(e) {
      setStoredParam('messagesSending');
    }
  }
}

document.addEventListener('exo-chat-message-sent-ack', (e) => {
  const message = e.detail;
  storeMessageAsSent(message);
});

document.addEventListener('exo-chat-connected', () => {
  if (resendIntervalID) {
    window.clearInterval(resendIntervalID);
  }
  resendIntervalID = window.setInterval(sendFailedMessages, RESEND_MESSAGE_PERIOD);
});
