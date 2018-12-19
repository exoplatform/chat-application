export function getDayDate(timestamp) {
  const date = new Date(timestamp);
  date.setHours(0);
  date.setMinutes(0);
  date.setSeconds(0);
  date.setMilliseconds(0);
  return date;
}

export function getDayDateString(timestamp) {
  return getDayDate(timestamp).toLocaleDateString(eXo.env.portal.language);
}

export function getMinuteDate(timestamp) {
  let date = new Date();
  if (timestamp) {
    date = new Date(timestamp);
  }
  date.setSeconds(0);
  date.setMilliseconds(0);
  return date;
}

export function isSameDay(timestamp1, timestamp2) {
  return getDayDate(timestamp1).getTime() === getDayDate(timestamp2).getTime();
}

export function isSameMinute(timestamp1, timestamp2) {
  return getMinuteDate(timestamp1).getTime() === getMinuteDate(timestamp2).getTime();
}

export function getTimeString(timestampServer, displayDate) {
  let date = new Date();
  if (timestampServer) {
    date = new Date(timestampServer);
  }

  let sTime = '';
  let sHours = date.getHours();
  const sMinutes = date.getMinutes();
  const timezone = date.getTimezoneOffset();

  let ampm = '';
  const timeZoneOffsetForAMUse = 60;
  if (timezone > timeZoneOffsetForAMUse) {
    const hoursForPMUse = 12;

    ampm = 'AM';
    if (sHours > hoursForPMUse) {
      ampm = 'PM';
      sHours -= hoursForPMUse;
    } else if (sHours === hoursForPMUse) {
      ampm = 'PM';
    }
  }

  const minNumberWithMoreThanOneDigit = 10;
  if (sHours < minNumberWithMoreThanOneDigit) {
    sTime = '0';
  }
  sTime += `${sHours}:`;
  if (sMinutes < minNumberWithMoreThanOneDigit) {
    sTime += '0';
  }
  sTime += sMinutes;
  if (ampm !== '') {
    sTime += ` ${ampm}`;
  }

  if(displayDate) {
    const sNowDate = new Date().toLocaleDateString();
    const sDate = date.toLocaleDateString();
    if (sNowDate !== sDate) {
      sTime = `${sDate} ${sTime}`;
    }
  }

  return sTime;
}