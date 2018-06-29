import { shallow } from 'vue-test-utils';
import MiniChatApp from '../../main/webapp/vue-app/components/MiniChatApp';
import MiniChatNotifList from '../../main/webapp/vue-app/components/MiniChatNotifList';
import MiniChatRoom from '../../main/webapp/vue-app/components/MiniChatRoom';
import ChatMessageList from '../../main/webapp/vue-app/components/ChatMessageList';
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

function getComponent() {
  return shallow(MiniChatApp, {
    propsData: {
      room: '54654fa654654fa65f6af4654654f4f4f6546'
    },
    mocks: {
      $t: (key, params) => {
        return `${key} params: ${params ? JSON.stringify(params) :''}`;
      },
      $constants : chatConstants,
      mq: 'desktop'
    }
  });
}

describe('MiniChatApp.test.js', () => {

  it('test display mini chat sub components', () => {
    const cmp = getComponent();
    expect(cmp.findAll(MiniChatNotifList)).toHaveLength(1);
    expect(cmp.findAll(MiniChatRoom)).toHaveLength(0);
  });

  it('test connection', () => {
    const cmp = getComponent();
    cmp.vm.connectionEstablished();
    expect(cmp.vm.connected).toBeTruthy();

    cmp.vm.changeUserStatusToOffline();
    expect(cmp.vm.connected).toBeFalsy();
  });

  it('test room detail load', () => {
    const cmp = getComponent();
    global.fetch = jest.fn().mockImplementation((url) => {
      if(url && url.indexOf('type=u') >= 0) {
        cmp.vm.room = 'eaea96e9ae5aea8e1ae8a1ea8e6eaea';
      }
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
    cmp.vm.openRoomInMiniChat({detail: {
      name: 'Test User',
      type: 'u'
    }});
    cmp.update();
    expect(cmp.findAll(MiniChatNotifList)).toHaveLength(1);
    expect(cmp.findAll(MiniChatRoom)).toHaveLength(1);
  });

  it('test total unread update', () => {
    const cmp = getComponent();
    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_GLOBAL_UNREAD_COUNT_UPDATED, {detail: {data: {
      totalUnreadMsg: 2
    }}}));
    cmp.update();
    expect(cmp.vm.totalUnreadMsg).toBe(2);
    expect(cmp.find('.notif-total').text()).toBe('2');
  });

  it('test user status', () => {
    const cmp = getComponent();
    cmp.vm.initSettings(eXo.chat.userSettings);
    eXo.chat.userSettings.status = 'invisible';
    expect(cmp.vm.userSettings).toEqual(eXo.chat.userSettings);
    expect(cmp.vm.status).toBe('offline');
    expect(cmp.vm.statusClass).toBe('user-offline');

    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_USER_STATUS_CHANGED, {detail: {data: {
      sender: 'root',
      status: 'available'
    }}}));
    cmp.update();
    expect(cmp.vm.status).toBe('available');
    expect(cmp.vm.statusClass).toBe('user-offline');
    cmp.vm.connectionEstablished();
    cmp.update();
    expect(cmp.vm.statusClass).toBe('user-available');
    expect(cmp.findAll('.dropdown-toggle.user-available')).toHaveLength(1);

    cmp.vm.status = 'invisible';
    cmp.update();
    expect(cmp.vm.statusClass).toBe('user-offline');
  });
});