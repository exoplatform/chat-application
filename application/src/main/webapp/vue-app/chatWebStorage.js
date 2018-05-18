export const LAST_SELECTED_ROOM_PARAM = 'lastSelectedRoom';

const NB_MILLISECONDS_PERD_SECOND = 1000;
const DEFAULT_EXPIRATION_PERIOD = 300;

export function getStoredParam(key, defaultValue) {
  let ts  = localStorage.getItem(`${key}TS`);
  const val = localStorage.getItem(key);
  if (!ts) {
    ts=-1;
  }

  const now = Math.round(new Date() / NB_MILLISECONDS_PERD_SECOND);

  if (val && (now<ts || ts===-1 )) {
    return val;
  }

  return defaultValue;
}

export function setStoredParam(key, value, expire) {
  if (expire > -1) {
    expire = expire ? expire : DEFAULT_EXPIRATION_PERIOD;
    localStorage.setItem(`${key}TS`, Math.round(new Date() / NB_MILLISECONDS_PERD_SECOND) + expire);
  }
  localStorage.setItem(key, value);
}

document.addEventListener('exo-chat-selected-contact-changed', (e) => {
  const selectedContact = e.detail;
  if (selectedContact && selectedContact.room) {
    setStoredParam('lastSelectedRoom', selectedContact.room);
  }
});
