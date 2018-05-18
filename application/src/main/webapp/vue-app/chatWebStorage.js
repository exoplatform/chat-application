export const LAST_SELECTED_ROOM_PARAM = 'lastSelectedRoom';

const STORED_NOT_SENT_MESSAGES = 'roomNotSentMessages';

const NB_MILLISECONDS_PERD_SECOND = 1000;
const DEFAULT_EXPIRATION_PERIOD = 300;

export function getStoredParam(key, defaultValue) {
  let ts  = localStorage.getItem(`${key}TS`);
  const val = localStorage.getItem(key);
  if (!ts) {
    ts=-1;
  }
  const now = Math.round(new Date() / NB_MILLISECONDS_PERD_SECOND);
  if (val && (now < ts || ts === -1 )) {
    return val;
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
  const notSentMessages = getStoredParam(`${STORED_NOT_SENT_MESSAGES}-${username}`);
  if(notSentMessages) {
    return JSON.parse(notSentMessages).filter(message => message.room === room);
  } else {
    return [];
  }
}

export function getNotSentMessages(username) {
  const notSentMessages = getStoredParam(`${STORED_NOT_SENT_MESSAGES}-${username}`);
  if(notSentMessages) {
    return JSON.parse(notSentMessages);
  } else {
    return [];
  }
}

export function storeNotSentMessage(messageToStore) {
  if (messageToStore.user !== eXo.chat.userSettings.username) {
    return;
  }
  const notSentMessages = getNotSentMessages(messageToStore.user);
  const foundMessageIndex = notSentMessages.findIndex(message => message.clientId === messageToStore.clientId);
  if (foundMessageIndex < 0) {
    messageToStore.notSent = true;
    notSentMessages.push(messageToStore);
    setStoredParam(`${STORED_NOT_SENT_MESSAGES}-${messageToStore.user}`, JSON.stringify(notSentMessages));
  }
}

export function storeMessageAsSent(messageToStore) {
  if (messageToStore.user !== eXo.chat.userSettings.username) {
    return false;
  }
  messageToStore.notSent = false;
  const notSentMessages = getNotSentMessages(messageToStore.user);

  const foundMessageIndex = notSentMessages.findIndex(message => message.clientId === messageToStore.clientId);
  if (foundMessageIndex >= 0 && foundMessageIndex < notSentMessages.length) {
    notSentMessages.splice(foundMessageIndex, 1);
    setStoredParam(`${STORED_NOT_SENT_MESSAGES}-${messageToStore.user}`, JSON.stringify(notSentMessages));
    return true;
  }
  return false;
}

export function sendFailedMessages() {
  const notSentMessages = getNotSentMessages(eXo.chat.userSettings.username);
  if(notSentMessages && notSentMessages.length) {
    notSentMessages.forEach(messageToResend => {
      document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : messageToResend}));
    });
  }
}

document.addEventListener('exo-chat-selected-contact-changed', (e) => {
  const selectedContact = e.detail;
  if (selectedContact && selectedContact.room) {
    setStoredParam('lastSelectedRoom', selectedContact.room);
  }
});
