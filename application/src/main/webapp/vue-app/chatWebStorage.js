import {chatConstants} from './chatConstants';

const DEFAULT_EXPIRATION_PERIOD = 300;
const RESEND_MESSAGE_PERIOD = 5000;

export function getStoredParam(key, defaultValue) {
  let ts  = localStorage.getItem(`${key}TS`);
  const val = localStorage.getItem(key);
  if (!ts) {
    ts=-1;
  }
  const now = Math.round(new Date() / chatConstants.NB_MILLISECONDS_PERD_SECOND);
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
  if (value && value.length) {
    if (expire && expire > -1) {
      expire = expire ? expire : DEFAULT_EXPIRATION_PERIOD;
      localStorage.setItem(`${key}TS`, Math.round(new Date() / chatConstants.NB_MILLISECONDS_PERD_SECOND) + expire);
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
    const notSentMessages = getSortedNotSentMessages(username);
    if (notSentMessages && notSentMessages.length) {
      notSentMessages.forEach(notSenMessage => {
        if (notSenMessage && notSenMessage.room === room) {
          roomNotSentMessages.push(notSenMessage);
        }
      });
    }
  }
  return roomNotSentMessages;
}

export function getNotSentMessages(username) {
  let notSentMessages = getStoredParam(`${chatConstants.STORED_PARAM_PENDING_MESSAGES}-${username}`);
  if (notSentMessages) {
    notSentMessages = JSON.parse(notSentMessages);
    if (Array.isArray(notSentMessages)) {
      notSentMessages = {};
      localStorage.removeItem(`${chatConstants.STORED_PARAM_PENDING_MESSAGES}-${username}`);
    } else {
      return notSentMessages;
    }
  }
  return {};
}

function getSortedNotSentMessages(username) {
  const notSentMessages = getNotSentMessages(username);
  if (notSentMessages) {
    const notSentMessagesKeys = Object.keys(notSentMessages);
    const notSentMessagesArray = [];
    if (notSentMessagesKeys.length > 1) {
      notSentMessagesKeys.forEach(key => notSentMessagesArray.push(notSentMessages[key]));
      notSentMessagesArray.sort((a, b) => a.timestamp - b.timestamp);
    }
    return notSentMessagesArray;
  }
  return [];
}

export function storeNotSentMessage(messageToStore) {
  if (!messageToStore) {
    return;
  }
  const user = messageToStore.sender ? messageToStore.sender : messageToStore.data && messageToStore.data.sender ? messageToStore.data.sender : messageToStore.user ? messageToStore.user : messageToStore.data ? messageToStore.data.user : null;
  const clientId = messageToStore.clientId ? messageToStore.clientId : messageToStore.data ? messageToStore.data.clientId : null;
  const room = messageToStore.room ? messageToStore.room : messageToStore.data ? messageToStore.data.room : null;
  if (user !== eXo.chat.userSettings.username || !room || !clientId) {
    return;
  }
  const notSentMessages = getNotSentMessages(user);
  const foundMessage = notSentMessages && notSentMessages[clientId];
  if (!foundMessage) {
    messageToStore.notSent = true;
    notSentMessages[clientId] = messageToStore;
    setStoredParam(`${chatConstants.STORED_PARAM_PENDING_MESSAGES}-${user}`, JSON.stringify(notSentMessages));
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
    document.dispatchEvent(
      new CustomEvent(chatConstants.EVENT_MESSAGE_DELETED, {
        detail: {data: notSentMessages[clientId]}
      })
    );
    delete notSentMessages[clientId];
    setStoredParam(`${chatConstants.STORED_PARAM_PENDING_MESSAGES}-${user}`, JSON.stringify(notSentMessages));

    return true;
  }
  return false;
}

export function sendFailedMessage(user, message) {
  if (!getStoredParam(chatConstants.STORED_PARAM_MESSAGES_SENDING)) {
    setStoredParam(chatConstants.STORED_PARAM_MESSAGES_SENDING, 'true', RESEND_MESSAGE_PERIOD / chatConstants.NB_MILLISECONDS_PERD_SECOND);
    try {
      const notSentMessages = getNotSentMessages(user);
      if (notSentMessages && notSentMessages[message.clientId]) {
        document.dispatchEvent(new CustomEvent(chatConstants.ACTION_MESSAGE_SEND, {'detail': notSentMessages[message.clientId]}));
      }
      setStoredParam(chatConstants.STORED_PARAM_MESSAGES_SENDING);
    } catch (e) {
      setStoredParam(chatConstants.STORED_PARAM_MESSAGES_SENDING);
    }
  }
}

export function registreEventListener(){
  document.addEventListener(chatConstants.EVENT_MESSAGE_SENT, (e) => {
    const message = e.detail;
    storeMessageAsSent(message);
  });
  
  document.addEventListener(chatConstants.RESEND_FAILED_MESSAGE, (e) => {
    sendFailedMessage(e.detail.user, e.detail.message);
  });
  document.addEventListener(chatConstants.DELETE_FAILED_MESSAGE, (e) => {
    deleteFromStore(e.detail.user, e.detail.clientId);
  });
}

export function unregistreEventListener(){
  document.removeEventListener(chatConstants.EVENT_MESSAGE_SENT, (e) => {
    const message = e.detail;
    storeMessageAsSent(message);
  });
  
  document.removeEventListener(chatConstants.RESEND_FAILED_MESSAGE, (e) => {
    sendFailedMessage(e.detail.user, e.detail.message);
  });
  document.removeEventListener(chatConstants.DELETE_FAILED_MESSAGE, (e) => {
    deleteFromStore(e.detail.user, e.detail.clientId);
  });
}
