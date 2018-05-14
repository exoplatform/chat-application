import {chatData} from './chatData.js';
import { cometD } from '../js/lib/chatCometd3.js';

export function getUser(userName) {
  return fetch(`/portal/rest/v1/social/users/${userName}`, {credentials: 'include'})
    .then(resp => resp.json());
}

export function getUserStatus(userName) {
  return fetch(`/portal/rest/chat/api/1.0/user/onlineStatus?users=${userName}`, {credentials: 'include'})
    .then(resp => resp.json());
}

export function getUserSettings() {
  return fetch('/portal/rest/chat/api/1.0/user/settings', {credentials: 'include'})
    .then(resp => resp.json());
}

export function getOnlineUsers() {
  return fetch(`${chatData.chatAPI}onlineUsers`, {credentials: 'include'})
    .then(resp => resp.text());
}

export function getChatRooms(userSettings, onlineUsers) {
  return fetch(`${chatData.chatServerAPI}whoIsOnline?user=${userSettings.username}&onlineUsers=${onlineUsers}&dbName=${userSettings.dbName}&timestamp=${new Date().getTime()}`, {
    headers: {
      'Authorization': 'Bearer ' + userSettings.token
    }}).then(resp =>  resp.json());
}

export function initServerChannel() {
  alert(cometD);
}

export function getRoomParticipants(userSettings, room) {
  return fetch(`${chatData.chatServerAPI}users?user=${userSettings.username}&dbName=${userSettings.dbName}&room=${room.room}`, {
    headers: {
      'Authorization': 'Bearer ' + userSettings.token
    }}).then(resp =>  resp.json());
}
