import { shallow } from 'vue-test-utils';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';

import ExoChatMessageList from '../../main/webapp/vue-app/components/ExoChatMessageList';
import ExoChatMessageDetail from '../../main/webapp/vue-app/components/ExoChatMessageDetail';

global.fetch = jest.fn().mockImplementation(() => {
  const p = new Promise((resolve) => {
    resolve({
      text: function() {
        return '';
      },
      json: function() {
        return {};
      }
    });
  });
  return p;
});

function getMessage(msg, username, timestamp) {
  return {
    msg: msg,
    isSystem: false,
    msgId: Math.ceil(Math.random() * 10000000),
    clientId: Math.ceil(Math.random() * 10000000),
    room:'eb74205830cf97546269bbdc5d439b29ddd1735b',
    fullname: username,
    user: username,
    timestamp: timestamp
  };
}

function getMessageListDetail() {
  const component = shallow(ExoChatMessageList, {
    propsData: {
      miniChat : false
    },
    mocks: {
      $t: (key, params) => {
        return `${key} params: ${params ? JSON.stringify(params) :''}`;
      },
      $constants : chatConstants,
      mq: 'desktop'
    }
  });
  return component;
}

describe('ExoChatMessageList.test.js', () => {
  const cmp = getMessageListDetail();

  it('test no messages container', () => {
    expect(cmp.html()).toContain('exoplatform.chat.no.messages');
  });

  cmp.setData({
    messages: [
      getMessage('Test message 1', 'testuser1', Date.UTC(2000, 11, 10, 3, 0, 0)),
      getMessage('Test message 2', 'testuser1', Date.UTC(2000, 11, 10, 3, 0, 0)),
      getMessage('Test message 3', 'testuser1', Date.UTC(2000, 11, 11, 3, 0, 0)),
      getMessage('Test message 2', 'testuser2', Date.UTC(2000, 11, 11, 3, 0, 2)),
      getMessage('Test message 2', 'testuser2', Date.UTC(2000, 11, 11, 3, 0, 4)),
      getMessage('Test message 2', 'testuser1', Date.UTC(2000, 11, 11, 3, 0, 5)),
      getMessage('Test message 2', 'testuser1', Date.UTC(2000, 11, 11, 3, 0, 6)),
      getMessage('Test message 2', 'testuser2', Date.UTC(2000, 11, 11, 3, 0, 7))
    ],
    contact: {
      'fullName':'Test User',
      'unreadTotal':0,
      'isActive':'true',
      'type':'u',
      'user':'testuser',
      'room':'eb74205830cf97546269bbdc5d439b29ddd1735b',
      'status':'invisible',
      'timestamp':1528455913624,
      'isFavorite':false
    }
  });
  cmp.update();

  const dates = [
    new Date(Date.UTC(2000, 11, 10)).toLocaleDateString(eXo.env.portal.language),
    new Date(Date.UTC(2000, 11, 11)).toLocaleDateString(eXo.env.portal.language),
    new Date(Date.UTC(2000, 11, 12)).toLocaleDateString(eXo.env.portal.language),
    new Date(Date.UTC(2000, 11, 13)).toLocaleDateString(eXo.env.portal.language),
    new Date(Date.UTC(2000, 11, 14)).toLocaleDateString(eXo.env.portal.language)
  ];
  it('message list test', () => {
    expect(cmp.vm.messagesMap).not.toBeUndefined();
    expect(cmp.findAll(ExoChatMessageDetail)).toHaveLength(8);
    expect(Object.keys(cmp.vm.messagesMap)).toEqual([dates[0], dates[1]]);
    expect(cmp.findAll('.day-separator')).toHaveLength(2);
    expect(cmp.vm.messagesMap[dates[0]]).toHaveLength(2);
    expect(cmp.vm.messagesMap[dates[1]]).toHaveLength(6);
    expect(cmp.findAll(ExoChatMessageDetail).at(0).vm.hideTime).toBeFalsy();
    expect(cmp.findAll(ExoChatMessageDetail).at(1).vm.hideTime).toBeTruthy();
    expect(cmp.findAll(ExoChatMessageDetail).at(2).vm.hideTime).toBeFalsy();
    expect(cmp.findAll(ExoChatMessageDetail).at(3).vm.hideTime).toBeFalsy();
    expect(cmp.findAll(ExoChatMessageDetail).at(4).vm.hideTime).toBeFalsy();
    expect(cmp.findAll(ExoChatMessageDetail).at(5).vm.hideTime).toBeFalsy();
    expect(cmp.findAll(ExoChatMessageDetail).at(6).vm.hideTime).toBeFalsy();
    expect(cmp.findAll(ExoChatMessageDetail).at(7).vm.hideTime).toBeFalsy();

    expect(cmp.findAll(ExoChatMessageDetail).at(0).vm.hideAvatar).toBeFalsy();
    expect(cmp.findAll(ExoChatMessageDetail).at(1).vm.hideAvatar).toBeTruthy();
    expect(cmp.findAll(ExoChatMessageDetail).at(2).vm.hideAvatar).toBeFalsy();
    expect(cmp.findAll(ExoChatMessageDetail).at(3).vm.hideAvatar).toBeFalsy();
    expect(cmp.findAll(ExoChatMessageDetail).at(4).vm.hideAvatar).toBeTruthy();
    expect(cmp.findAll(ExoChatMessageDetail).at(5).vm.hideAvatar).toBeFalsy();
    expect(cmp.findAll(ExoChatMessageDetail).at(6).vm.hideAvatar).toBeTruthy();
    expect(cmp.findAll(ExoChatMessageDetail).at(7).vm.hideAvatar).toBeFalsy();
  });

  it('test new message added', () => {
    cmp.vm.messageWritten(getMessage('Test message 4', 'testuser1', Date.UTC(2000, 11, 12, 3, 0, 10)));
    cmp.update();
    expect(cmp.findAll('.day-separator')).toHaveLength(3);
    expect(Object.keys(cmp.vm.messagesMap)).toEqual([dates[0],dates[1],dates[2]]);
    expect(cmp.vm.messagesMap[dates[2]]).toHaveLength(1);
  });

  it('test message modified', () => {
    const newMessage = getMessage('Test message 4', 'testuser1', Date.UTC(2000, 11, 12, 3, 0, 20));
    cmp.vm.messageWritten(newMessage);
    cmp.update();
    newMessage.msg = 'Message modified';
    cmp.vm.messageModified(newMessage);
    expect(cmp.findAll('.day-separator')).toHaveLength(3);
    expect(cmp.vm.messages).toHaveLength(10);
    expect(cmp.vm.messagesMap[dates[2]]).toHaveLength(2);
    expect(cmp.vm.messagesMap[dates[2]][1].msg).toBe('Message modified');
  });

  it('test message received', () => {
    const newMessage = getMessage('Test message received', 'testuser2', Date.UTC(2000, 11, 13, 3, 0, 20));
    cmp.vm.messageReceived({detail: {data: newMessage}});
    cmp.update();
    expect(cmp.findAll('.day-separator')).toHaveLength(4);
    expect(cmp.vm.messages).toHaveLength(11);
    expect(cmp.vm.messagesMap[dates[3]]).toHaveLength(1);
    expect(cmp.vm.messagesMap[dates[3]][0].msg).toBe('Test message received');
  });

  it('test message sent', () => {
    const newMessage = getMessage('Test message sent', 'testuser2', Date.UTC(2000, 11, 14, 3, 0, 20));
    cmp.vm.messageSent({detail: {data: newMessage}});
    cmp.update();
    expect(cmp.findAll('.day-separator')).toHaveLength(5);
    expect(cmp.vm.messages).toHaveLength(12);
    expect(cmp.vm.messagesMap[dates[4]]).toHaveLength(1);
    expect(cmp.vm.messagesMap[dates[4]][0].msg).toBe('Test message sent');
  });

  it('test delete message', () => {
    const deletedMessage = cmp.vm.messages[11];
    deletedMessage.type = chatConstants.DELETE_MESSAGE;
    cmp.vm.messageDeleted({detail: {data: deletedMessage}});
    cmp.update();
    expect(cmp.findAll('.day-separator')).toHaveLength(5);
    expect(cmp.vm.messages).toHaveLength(12);
    expect(cmp.vm.messagesMap[dates[4]][0].type).toBe(chatConstants.DELETE_MESSAGE);
  });

  it('test edit last message', () => {
    const newMessage = getMessage('Test message to edit', 'root', Date.UTC(2000, 11, 15, 3, 0, 0));
    cmp.vm.messageWritten(newMessage);
    const editMessageSpy = jest.spyOn(cmp.vm, 'editMessage');
    cmp.vm.editLastMessage();
    cmp.update();

    expect(editMessageSpy).toBeCalledWith(newMessage);
    expect(cmp.vm.showEditMessageModal).toBeTruthy();
    expect(cmp.vm.messageToEdit).toEqual(newMessage);
  });

  it('test save editing message', () => {
    const messageModifiedSpy = jest.spyOn(cmp.vm, 'messageModified');
    cmp.vm.saveMessage();
    cmp.update();

    expect(messageModifiedSpy).toBeCalledWith(cmp.vm.messageToEdit);
    expect(cmp.vm.showEditMessageModal).toBeFalsy();
  });

  it('test contact changed', () => {
    cmp.vm.contactChanged({detail: {room: 'new-room-id'}});
    cmp.update();
    expect(cmp.findAll('.day-separator')).toHaveLength(0);
    expect(cmp.vm.messages).toHaveLength(0);
    expect(cmp.vm.newMessagesLoading).toBeTruthy();
  });

});