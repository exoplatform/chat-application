//import {chatData} from './chatData.js';

function getUser(userName) {
  return fetch(`/rest/v1/social/users/${userName}`, {credentials: 'include'})
    .then(resp => resp.json());
}

function getUserStatus(userName) {
  return fetch(`/rest/chat/api/1.0/user/onlineStatus?users=${userName}`, {credentials: 'include'})
    .then(resp => resp.json());
}

function getChatRooms() {
  return fetch('/chatServer/whoIsOnline').then(resp =>
    resp.json()
  );
}

export { getUser, getUserStatus, getChatRooms };