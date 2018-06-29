import { shallow } from 'vue-test-utils';
import MiniChatNotifList from '../../main/webapp/vue-app/components/MiniChatNotifList';
import MiniChatNotifDetail from '../../main/webapp/vue-app/components/MiniChatNotifDetail';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';

global.fetch = jest.fn().mockImplementation(() => {
  var p = new Promise((resolve) => {
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

const messages = [
  getMessage("Test message 1", "testuser1", Date.UTC(2000, 11, 10, 3, 0, 0), 'eb74205830cf97546269bbdc5d439b29ddd1735b'),
  getMessage("Test message 2", "testuser1", Date.UTC(2000, 11, 10, 3, 0, 0), 'eb74205830cf97546269bbdc5d439b2fffee735b'),
  getMessage("Test message 3", "testuser1", Date.UTC(2000, 11, 11, 3, 0, 0), 'eb74205830cf97546269bbdc5d439b29ddd1735b'),
  getMessage("Test message 4", "testuser2", Date.UTC(2000, 11, 11, 3, 0, 2), 'eb74205830cf97546269bbdc5d439b2fffee735b'),
  getMessage("Test message 5", "testuser2", Date.UTC(2000, 11, 11, 3, 0, 4), 'eb74205830cf97546269bbdc5d439b29ddd1735b'),
  getMessage("Test message 6", "testuser1", Date.UTC(2000, 11, 11, 3, 0, 5), 'eb74205830cf97546269bbdc5d439b2fffee735b'),
  getMessage("Test message 7", "testuser1", Date.UTC(2000, 11, 11, 3, 0, 6), 'eb74205830cf97546269bbdc5d439b29ddd1735b'),
  getMessage("Test message 8", "testuser2", Date.UTC(2000, 11, 11, 3, 0, 7), 'eb74205830cf97546269bbdc5d439b2fffee735b')
];


function getMessage(msg, username, timestamp, room) {
  return {
    msg: msg,
    isSystem: false,
    msgId: Math.ceil(Math.random() * 10000000),
    clientId: Math.ceil(Math.random() * 10000000),
    room: room,
    categoryId: room,
    fullname: username,
    user: username,
    timestamp: timestamp
  }
}

function getComponent() {
  const comp = shallow(MiniChatNotifList, {
    propsData: {
      totalUnreadMsg: 2
    },
    mocks: {
      $t: (key, params) => {
        return `${key} params: ${params ? JSON.stringify(params) :''}`;
      },
      $constants : chatConstants,
      mq: 'desktop'
    }
  });
  comp.setData({messagesList: messages});
  comp.update();
  return comp;
}

describe('MiniChatNotifList.test.js', () => {

  it('test displayed messages', () => {
    const cmp = getComponent();
    expect(Object.keys(cmp.vm.messagesFiltered)).toHaveLength(2);
    expect(cmp.findAll(MiniChatNotifDetail)).toHaveLength(2);
    expect(Object.keys(cmp.vm.messagesFiltered)).toEqual(['eb74205830cf97546269bbdc5d439b29ddd1735b', 'eb74205830cf97546269bbdc5d439b2fffee735b']);
    expect(cmp.vm.messagesFiltered['eb74205830cf97546269bbdc5d439b29ddd1735b'][0].msg).toBe('Test message 7');
    expect(cmp.vm.messagesFiltered['eb74205830cf97546269bbdc5d439b2fffee735b'][0].msg).toBe('Test message 8');
  });

  it('test display refresh messages container', () => {
    const cmp = getComponent();
    expect(cmp.vm.isRetrievingMessagges).toBeFalsy();
    cmp.vm.refreshMessages();
    expect(cmp.vm.isRetrievingMessagges).toBeTruthy();
  });
  
  it('test user status', () => {
    const cmp = getComponent();
    expect(cmp.findAll('.uiIconStatus')).toHaveLength(Object.keys(cmp.vm.statusMap).length - 1);
  });

});