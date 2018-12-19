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
    timestamp: timestamp,
    options: {}
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
    },
    stubs: {
      'exo-chat-message-detail': ExoChatMessageDetail
    }
  });
  return component;
}

describe('ExoChatMessageList.test.js', () => {
  const cmp = getMessageListDetail();

  const dates = [
    new Date(Date.UTC(2000, 11, 10)).toLocaleDateString(eXo.env.portal.language),
    new Date(Date.UTC(2000, 11, 11)).toLocaleDateString(eXo.env.portal.language),
    new Date(Date.UTC(2000, 11, 12)).toLocaleDateString(eXo.env.portal.language),
    new Date(Date.UTC(2000, 11, 13)).toLocaleDateString(eXo.env.portal.language),
    new Date(Date.UTC(2000, 11, 14)).toLocaleDateString(eXo.env.portal.language)
  ];

  const messages = [
    getMessage('Test message 1', 'testuser1', Date.UTC(2000, 11, 10, 3, 0, 0)),
    getMessage('Test message 2', 'testuser1', Date.UTC(2000, 11, 10, 3, 0, 0)),
    getMessage('Test message 3', 'testuser1', Date.UTC(2000, 11, 11, 3, 0, 0)),
    getMessage('Test message 4', 'testuser2', Date.UTC(2000, 11, 11, 3, 0, 2)),
    getMessage('Test message 5', 'testuser2', Date.UTC(2000, 11, 11, 3, 0, 4)),
    getMessage('Test message 6', 'testuser1', Date.UTC(2000, 11, 11, 3, 0, 5)),
    getMessage('Test message 7', 'testuser1', Date.UTC(2000, 11, 11, 3, 0, 6)),
    getMessage('Test message 8', 'testuser2', Date.UTC(2000, 11, 11, 3, 0, 7))
  ];

  it('test no messages container', () => {
    expect(cmp.html()).toContain('exoplatform.chat.no.messages');
  });

  cmp.setData({
    messages: messages,
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

  it('message list test', () => {
    expect(cmp.vm.messagesMap).not.toBeUndefined();
    expect(cmp.findAll('.chat-message-box')).toHaveLength(8);
    expect(Object.keys(cmp.vm.messagesMap)).toEqual([dates[0], dates[1]]);
    expect(cmp.findAll('.day-separator')).toHaveLength(2);
    expect(cmp.vm.messagesMap[dates[0]]).toHaveLength(2);
    expect(cmp.vm.messagesMap[dates[1]]).toHaveLength(6);
    
    expect(cmp.vm.isHideTime(null, messages[0])).toBeFalsy();
    expect(cmp.vm.isHideTime(messages[0], messages[1])).toBeTruthy();
    expect(cmp.vm.isHideTime(messages[1], messages[2])).toBeFalsy();
    expect(cmp.vm.isHideTime(messages[2], messages[3])).toBeTruthy();
    expect(cmp.vm.isHideTime(messages[3], messages[4])).toBeTruthy();
    expect(cmp.vm.isHideTime(messages[4], messages[5])).toBeTruthy();
    expect(cmp.vm.isHideTime(messages[5], messages[6])).toBeTruthy();
    expect(cmp.vm.isHideTime(messages[6], messages[7])).toBeTruthy();

    expect(cmp.vm.isHideAvatar(null, messages[0])).toBeFalsy();
    expect(cmp.vm.isHideAvatar(messages[0], messages[1])).toBeTruthy();
    expect(cmp.vm.isHideAvatar(messages[1], messages[2])).toBeFalsy();
    expect(cmp.vm.isHideAvatar(messages[2], messages[3])).toBeFalsy();
    expect(cmp.vm.isHideAvatar(messages[3], messages[4])).toBeTruthy();
    expect(cmp.vm.isHideAvatar(messages[4], messages[5])).toBeFalsy();
    expect(cmp.vm.isHideAvatar(messages[5], messages[6])).toBeTruthy();
    expect(cmp.vm.isHideAvatar(messages[6], messages[7])).toBeFalsy();
  });

  it('test new message added', () => {
    cmp.vm.messageWritten(getMessage('Test message 4', 'testuser1', Date.UTC(2000, 11, 12, 3, 0, 10)));
    cmp.update();
    expect(cmp.findAll('.day-separator')).toHaveLength(3);
    expect(Object.keys(cmp.vm.messagesMap)).toEqual([dates[0],dates[1],dates[2]]);
    expect(cmp.vm.messagesMap[dates[2]]).toHaveLength(1);
  });

  it('test message with special characters edited', () => {
    const newMessage = getMessage('Test &#92 &#38 &lt; &gt; &quot; &#92 &#38 &lt; &gt; &quot;', 'testuser1', Date.UTC(2000, 11, 12, 3, 0, 20));
    cmp.vm.editMessage(newMessage);
    cmp.update();
    expect(cmp.vm.messageToEdit.msg).toBe('Test \\ & < > " \\ & < > "');
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
    expect(cmp.vm.messageToEdit.clientId).toEqual(newMessage.clientId);
    expect(cmp.vm.messageToEdit.fullname).toEqual(newMessage.fullname);
    expect(cmp.vm.messageToEdit.msg).toEqual(newMessage.msg);
    expect(cmp.vm.messageToEdit.msgId).toEqual(newMessage.msgId);
    expect(cmp.vm.messageToEdit.room).toEqual(newMessage.room);
    expect(cmp.vm.messageToEdit.timestamp).toEqual(newMessage.timestamp);
    expect(cmp.vm.messageToEdit.user).toEqual(newMessage.user);
  });

  it('test save editing message', () => {
    const messageModifiedSpy = jest.spyOn(cmp.vm, 'messageModified');
    cmp.vm.saveEditMessage();
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