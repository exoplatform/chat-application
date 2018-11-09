import { shallow } from 'vue-test-utils';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';

import ExoMiniChatApp from '../../main/webapp/vue-app/components/ExoMiniChatApp';
import ExoMiniChatNotifList from '../../main/webapp/vue-app/components/ExoMiniChatNotifList';
import ExoMiniChatRoom from '../../main/webapp/vue-app/components/ExoMiniChatRoom';

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

function getComponent() {
  return shallow(ExoMiniChatApp, {
    propsData: {
      room: '54654fa654654fa65f6af4654654f4f4f6546'
    },
    stubs: {
      'exo-chat-notif-list': ExoMiniChatNotifList,
      'exo-chat-room': ExoMiniChatRoom
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

describe('ExoMiniChatApp.test.js', () => {

  it('test display mini chat sub components', () => {
    const cmp = getComponent();
    expect(cmp.findAll('#chatApplicationNotification .dropdown')).toHaveLength(1);
    expect(cmp.findAll('.mini-chat')).toHaveLength(0);
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
    cmp.vm.openRoomInMiniChat({detail: {
      name: 'Test User',
      type: 'u'
    }});
    cmp.update();
    expect(cmp.findAll('#chatApplicationNotification .dropdown')).toHaveLength(1);
    expect(cmp.findAll('.mini-chat')).toHaveLength(1);
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