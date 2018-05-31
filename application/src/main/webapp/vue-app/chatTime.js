export function getDayDate(timestampServer) {
  const date = new Date(timestampServer);
  date.setHours(0);
  date.setMinutes(0);
  date.setSeconds(0);
  date.setMilliseconds(0);
  return date.toLocaleDateString();
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