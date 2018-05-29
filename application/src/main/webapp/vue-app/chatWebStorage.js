export const LAST_SELECTED_ROOM_PARAM = 'lastSelectedRoom';

const STORED_NOT_SENT_MESSAGES = 'roomNotSentMessages';

const NB_MILLISECONDS_PERD_SECOND = 1000;
const DEFAULT_EXPIRATION_PERIOD = 300;

const MAX_RESEND_MESSAGE_ATTEMPT = 2;

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
  if (expire && expire > -1) {
    expire = expire ? expire : DEFAULT_EXPIRATION_PERIOD;
    localStorage.setItem(`${key}TS`, Math.round(new Date() / NB_MILLISECONDS_PERD_SECOND) + expire);
  }
  localStorage.setItem(key, value);
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
  if (messageToStore.user !== eXo.chat.userSettings.username || !messageToStore.clientId || !messageToStore.room) {
    return;
  }
  const notSentMessages = getNotSentMessages(messageToStore.user);
  const foundMessage = notSentMessages[messageToStore.clientId];
  if (!foundMessage) {
    messageToStore.notSent = true;
    notSentMessages[messageToStore.clientId] = messageToStore;
    setStoredParam(`${STORED_NOT_SENT_MESSAGES}-${messageToStore.user}`, JSON.stringify(notSentMessages));
  }
}

export function storeMessageAsSent(messageToStore) {
  if (messageToStore.user !== eXo.chat.userSettings.username || !messageToStore.clientId) {
    return false;
  }
  messageToStore.notSent = false;
  return deleteFromStore(messageToStore);
}

export function deleteFromStore(messageToStore) {
  const notSentMessages = getNotSentMessages(messageToStore.user);

  const foundMessage = notSentMessages[messageToStore.clientId];
  if (foundMessage) {
    delete notSentMessages[messageToStore.clientId];
    setStoredParam(`${STORED_NOT_SENT_MESSAGES}-${messageToStore.user}`, JSON.stringify(notSentMessages));
    return true;
  }
  return false;
}

export function sendFailedMessages() {
  if(!window.messagesSending) {
    window.messagesSending = true;
    try {
      const notSentMessages = getNotSentMessages(eXo.chat.userSettings.username);
      if(notSentMessages && Object.keys(notSentMessages).length) {
        Object.keys(notSentMessages).forEach(clienId => {
          const messageToResend = notSentMessages[clienId];
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
      window.messagesSending = false;
    } catch(e) {
      window.messagesSending = false;
    }
  }
}
