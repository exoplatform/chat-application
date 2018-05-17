const NB_MILLISECONDS_PERD_SECOND = 1000;
const DEFAULT_EXPIRATION_PERIOD = 300;

export function getStoredParam(key, defaultValue) {
  let ts  = localStorage.getItem(`${key}TS`);
  const val = localStorage.getItem(key);
  if (!ts) {
    ts=-1;
  }

  const now = Math.round(new Date() / NB_MILLISECONDS_PERD_SECOND);

  if (val !== null && val !== null && (now<ts || ts===-1 )) {
    return val;
  }

  return defaultValue;
}

export function setStoredParam(key, value, expire) {
  expire = expire ? expire : DEFAULT_EXPIRATION_PERIOD;
  localStorage.setItem(`${key}TS`, Math.round(new Date() / NB_MILLISECONDS_PERD_SECOND) + expire);
  localStorage.setItem(key, value);
}
