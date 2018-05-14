//import {chatData} from './chatData.js';
import { cometD } from '../js/lib/chatCometd3.js';

function getUser(userName) {
  return fetch(`/portal/rest/v1/social/users/${userName}`, {credentials: 'include'})
    .then(resp => resp.json());
}

function getUserStatus(userName) {
  return fetch(`/portal/rest/chat/api/1.0/user/onlineStatus?users=${userName}`, {credentials: 'include'})
    .then(resp => resp.json());
}

function getUserSettings() {
  return fetch('/portal/rest/chat/api/1.0/user/settings', {credentials: 'include'})
    .then(resp => resp.json());
}

function getChatRooms(userSettings, onlineUsers) {
  return fetch('/chatServer/whoIsOnline?user=' + userSettings.username + '&onlineUsers=' + onlineUsers.join(',') + '&dbName=' + userSettings.dbName + '&timestamp=' + new Date().getTime(), {
    headers: {
      'Authorization': 'Bearer ' + userSettings.token
    }}).then(resp =>  resp.json());
}

function initServerChannel() {
  alert(cometD);
}

export { getUser, getUserStatus, getChatRooms, getUserSettings, initServerChannel };
