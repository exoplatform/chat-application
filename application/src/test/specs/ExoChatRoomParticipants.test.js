import { shallow } from 'vue-test-utils';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';

import ExoChatContact from '../../main/webapp/vue-app/components/ExoChatContact';
import ExoChatRoomParticipants from '../../main/webapp/vue-app/components/ExoChatRoomParticipants';

describe('ExoChatRoomParticipants.test.js', () => {
  
  let roomParticipant;
  const room = {
    fullName: 'test room',
    unreadTota: 0,
    isActive: 'true',
    type: 't',
    user: 'team-a11192fa4a461dc023ac8b6d1cd85951a385d418',
    room: 'a11192fa4a461dc023ac8b6d1cd85951a385d418',
    admins: ['root'],
    status: 'team',
    timestamp: 1528897226090,
    isFavorite: true
  };

  const participants = [
    {
      name: 'usera',
      fullname: 'User A',
      status: 'available'
    },
    {
      name: 'userb',
      fullname: 'User B',
      status: 'available'
    },
    {
      name: 'userc',
      fullname: 'User C',
      status: 'offline'
    }
  ];

  const onlineParticipants = [
    {
      name: 'usera',
      fullname: 'User A',
      status: 'available'
    },
    {
      name: 'userb',
      fullname: 'User B',
      status: 'available'
    }
  ];

  const user = {
    fullName: 'John Smith',
    unreadTota: 0,
    isActive: 'true',
    type: 'u',
    user: 'smith',
    room: 'a11192fa4a461dc023ac8b6d1cd85951a385d419',
    status: 'away',
    timestamp: 1528897226090,
    isFavorite: false
  };

  const space = {
    fullName: 'My space',
    unreadTota: 0,
    isActive: 'true',
    type: 's',
    user: 'space-a11192fa4a461dc023ac8b6d1cd85951a385d417',
    room: 'a11192fa4a461dc023ac8b6d1cd85951a385d417',
    status: 'space',
    timestamp: 1528897226090,
    isFavorite: false
  };

  global.fetch = jest.fn().mockImplementation(() => {
    const p = new Promise((resolve) => {
      resolve({
        text: function() { 
          return '';
        },
        json: function() { 
          return {
            users: []
          };
        }
      });
    });
    return p;
  });

  beforeEach(() => {
    roomParticipant = shallow(ExoChatRoomParticipants, {
      stubs: {
        'exo-chat-contact': ExoChatContact
      },
      mocks: {
        $t: () => {},
        $constants : chatConstants
      },
      attachToDocument: true
    });

<<<<<<< HEAD
    roomParticipant.setData({contact: room, participants: participants, participantsCount: participants.length});
=======
    roomParticipant.setData({contact: room, participants: participants, participantsCount: participants.length -1});
>>>>>>> 4c3bd43b... 36886 : Improve loading users in Particpants panel (#386)
  });

  it('room participants should be displayed only for teams and spaces', () => {
    expect(roomParticipant.find('.uiRoomUsersContainerArea').exists()).toBe(true);
    // change selected contact to user
    roomParticipant.setData({contact: user});
    expect(roomParticipant.find('.uiRoomUsersContainerArea').exists()).toBe(false);
    // change selected contact to space
    roomParticipant.setData({contact: space});
    expect(roomParticipant.find('.uiRoomUsersContainerArea').exists()).toBe(true);
  });

  it('room participants should be collapsed', () => {
    expect(roomParticipant.find('.room-participants').classes()).toContain('collapsed');
  });

  it('toggle room participants area when click on arrow icon', () => {
    expect(roomParticipant.find('.room-participants').classes()).toContain('collapsed');
    roomParticipant.find('.room-users-collapse-btn').trigger('click');
    expect(roomParticipant.find('.room-participants').classes()).not.toContain('collapsed');
    roomParticipant.find('.room-users-collapse-btn').trigger('click');
    expect(roomParticipant.find('.room-participants').classes()).toContain('collapsed');
  });

  it('room participants should be extended when exo-chat-setting-showParticipants-requested event triggered', () => {
    roomParticipant.trigger(chatConstants.ACTION_ROOM_SHOW_PARTICIPANTS);
    expect(roomParticipant.find('.room-participants').classes()).not.toContain('collapsed');
  });

  it('room participants filter icon must have all-participants css class when filter is All', () => {
    roomParticipant.vm.participantFilter = 'All';
    roomParticipant.update();
    expect(roomParticipant.find('.room-participants-filter .actionIcon i').classes()).toContain('all-participants');
  });

  it('back-to-conversation event emitted on click on back button on mobile', () => {
    roomParticipant.vm.mq = 'mobile';
    roomParticipant.update();
    roomParticipant.find('.uiIconGoBack').trigger('click');
    expect(roomParticipant.emitted('back-to-conversation')).toBeTruthy();
  });

  it('participants number label should be (3)', () => {
    expect(roomParticipant.find('.nb-participants').text()).toBe('(3)');
  });

  it('3 displayed participants with all filter', () => {
    expect(roomParticipant.findAll('.chat-contact')).toHaveLength(3);
  });

  it('2 displayed participants with online filter', () => {
    // change filter to online, participants and participantsCount should be changed
    roomParticipant.setData({participants: onlineParticipants, participantsCount: onlineParticipants.length});
    roomParticipant.find('.room-participants-filter .actionIcon').trigger('click');
    expect(roomParticipant.findAll('.chat-contact')).toHaveLength(2);
  });

  it('when trigger exo-chat-selected-contact-changed event the contact must be changed', () => {
    roomParticipant.trigger(chatConstants.EVENT_ROOM_SELECTION_CHANGED, {detail: space});
    expect(roomParticipant.vm.contact.name).toEqual(space.name); 
  });

  it('when trigger exo-chat-user-status-changed event the contact status must be changed', () => {
    expect(roomParticipant.vm.participants[0].status).toEqual('available');
    roomParticipant.trigger(chatConstants.EVENT_USER_STATUS_CHANGED, 
      {
        detail: {
          sender:'usera',
          data:{
            status:'away'
          }
        }
      }
    );
    expect(roomParticipant.vm.participants[0].status).toEqual('away');
  });
  
  it('participant should be removed from participant list when he left the room', () => {
    expect(roomParticipant.vm.participants[0].name).toEqual('usera');
    roomParticipant.trigger(chatConstants.EVENT_ROOM_MEMBER_LEFT, 
      {
        detail: {
          sender:'usera',
          data:{
            room: room.room
          }
        }
      }
    );
    expect(roomParticipant.vm.participants[0].name).toEqual('userb');
  });
  
});
