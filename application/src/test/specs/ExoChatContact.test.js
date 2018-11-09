import { shallow } from 'vue-test-utils';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';

import ExoChatContact from '../../main/webapp/vue-app/components/ExoChatContact';
import ExoDropdownSelect from '../../main/webapp/vue-app/components/ExoDropdownSelect';

describe('ExoChatContact.test.js', () => {
  let currentUser;
  let contactionList;
  let selectedRoom;

  beforeEach(() => {
    currentUser = shallow(ExoChatContact, {
      propsData: {
        name : 'Root Root',
        userName: 'root',
        status: 'away',
        list: false,
        type: 'u',
        isCurrentUser: true
      },
      mocks: {
        $t: () => {},
        $constants : chatConstants,
      },
      stubs: {
        'exo-dropdown-select': ExoDropdownSelect
      },
      attachToDocument: true
    });
    contactionList = shallow(ExoChatContact, {
      propsData: {
        name : 'John Smith',
        userName: 'smith',
        status: 'donotdisturb',
        list: true,
        type: 'u'
      },
      mocks: {
        $t: () => {},
        $constants : chatConstants
      }
    });
    selectedRoom = shallow(ExoChatContact, {
      propsData: {
        name : 'Test Room',
        userName: 'team-60e864536eeee1fa0dfef6e9ad0a753c7d7924b6',
        type: 't',
        nbMembers: 10
      },
      mocks: {
        $t: () => {},
        $constants : chatConstants
      }
    });
  });

  it('equals current user name to root', () => {
    expect(currentUser.findAll('.contactLabel')).toHaveLength(1);
    expect(currentUser.find('.contactLabel').text()).toBe('Root Root');
  });

  it('current user has status choice dropdown', () => {
    expect(currentUser.contains('.status-dropdown')).toBe(true);
  });

  it('contactionList has user-donotdisturb css class', () => {
    expect(contactionList.find('.chat-contact-avatar').classes()).toContain('user-donotdisturb');
  });

  it('selected room has number members', () => {
    expect(selectedRoom.contains('.room-number-members')).toBe(true);
  });

  it('emits exo-chat-status-changed event when calling setStatus method', () => {
    currentUser.vm.setStatus('invisible');
    expect(currentUser.emitted()['status-changed'][0]).toEqual(['invisible']);
  });

  it('set isOnLine to false when disconnect event triggred', () => {
    currentUser.trigger(currentUser.vm.$constants.EVENT_DISCONNECTED);
    expect(currentUser.vm.isOnline).toEqual(false);
  });

  it('set isOnLine to true when reconnect event triggred', () => {
    currentUser.trigger(currentUser.vm.$constants.EVENT_RECONNECTED);
    expect(currentUser.vm.isOnline).toEqual(true);
  });


  
});
