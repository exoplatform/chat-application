import { shallow } from 'vue-test-utils';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';

import ExoChatContactList from '../../main/webapp/vue-app/components/ExoChatContactList';
import ExoChatContact from '../../main/webapp/vue-app/components/ExoChatContact';
import ExoChatApp from '../../main/webapp/vue-app/components/ExoChatApp';
import ExoChatModal from '../../main/webapp/vue-app/components/modal/ExoChatModal';
import ExoChatGlobalNotificationModal from '../../main/webapp/vue-app/components/modal/ExoChatGlobalNotificationModal';

describe('ExoChatApp.test.js', () => {
  let app;
  const userSettings = {
    username: 'root',
    fullName: 'Root Root',
    isOnline: true,
    status: 'available'
  };
  const roomsData = {
    unreadOffline: '0',
    unreadOnline: '0',
    unreadSpaces: '0',
    unreadTeams: '0',
    rooms: [
      {
        fullName: 'room1',
        user: 'team-a11192fa4a461dc023ac8b6d1cd85951a385d418',
        room: 'a11192fa4a461dc023ac8b6d1cd85951a385d418'
      },
      {
        fullName: 'room2',
        user: 'team-a11192fa4a461dc023ac8b6d1cd85951a385d419',
        room: 'a11192fa4a461dc023ac8b6d1cd85951a385d419'
      }
    ]
  };

  beforeEach(() => {
    app = shallow(ExoChatApp, {
      stubs: {
        'exo-chat-modal': ExoChatModal,
        'exo-chat-contact': ExoChatContact,
        'exo-chat-contact-list': ExoChatContactList,
        'exo-chat-global-notification-modal': ExoChatGlobalNotificationModal
      },
      mocks: {
        $t: () => {},
        $constants : chatConstants
      },
      attachToDocument: true
    });
    
  });

  it('chat-application has offline class and change to online when chat server connected', () => {
    expect(app.find('#chat-application').classes()).toContain('offline');
    expect(app.find('#chat-application').classes()).not.toContain('online');
    app.trigger(chatConstants.EVENT_CONNECTED);
    expect(app.find('#chat-application').classes()).toContain('online');
    expect(app.find('#chat-application').classes()).not.toContain('offline');
  });

  it('contact label contain user full name', () => {
    expect(app.find('.contactDetail .contactLabel span').text()).toBe('');
    app.vm.initSettings(userSettings);
    app.update();
    expect(app.find('.contactDetail .contactLabel span').text()).toBe(userSettings.fullName);
  });

  it('updated room must be at updated on contact list', () => {
    app.vm.initChatRooms(roomsData);
    expect(app.vm.contactList[0].fullName).toEqual('room1');
    roomsData.rooms[0].fullName = 'room3';
    app.vm.roomUpdated({
      detail: {
        data : roomsData.rooms[0]
      }
    });
    expect(app.vm.contactList[0].fullName).toEqual('room3');
  });

  it('init rooms data', () => {
    app.vm.initChatRooms(roomsData);
    expect(app.vm.contactList[0]).toEqual(roomsData.rooms[0]);
  });

  it('logout modal should be displayed when logged out', () => {
    expect(app.find('.logout-popup').element.style.display).toBe('none');
    app.vm.loggedout = true;
    app.update();
    expect(app.find('.logout-popup').element.style.display).not.toBe('none');
  });

  it('selected contact should much the setted contact', () => {
    app.vm.setSelectedContact(roomsData.rooms[0]);
    expect(app.vm.selectedContact.name).toBe(roomsData.rooms[0].name);
  });

  it('settings modal should be hidden and opned on click on settings icon', () => {
    expect(app.find('#chatPreferences').element.style.display).toBe('none');
    app.find('.chat-user-settings').trigger('click');
    expect(app.find('#chatPreferences').element.style.display).not.toBe('none');
  });

  it('side menu should exist only on mobile', () => {
    expect(app.find('.chat-side-menu').exists()).toBe(false);
    app.vm.mq = 'mobile';
    app.update();
    expect(app.find('.chat-side-menu').exists()).toBe(true);
  });

  it('add show-sideMenu when open side menu event', () => {
    expect(app.vm.sideMenuArea).toBe(false);
    expect(app.find('#chat-application').classes()).not.toContain('show-sideMenu');
    app.vm.sideMenuArea = true;
    app.update();
    expect(app.find('#chat-application').classes()).toContain('show-sideMenu');
  });

  it('add show-conversation class when select contact on mobile', () => {
    expect(app.find('#chat-application').classes()).not.toContain('show-conversation');
    app.vm.mq = 'mobile';
    app.vm.conversationArea = true;
    app.update();
    expect(app.find('#chat-application').classes()).toContain('show-conversation');
  });
  
});
