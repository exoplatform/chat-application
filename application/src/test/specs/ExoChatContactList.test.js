import { shallow } from 'vue-test-utils';
import ExoChatContactList from '../../main/webapp/vue-app/components/ExoChatContactList';
import ExoChatContact from '../../main/webapp/vue-app/components/ExoChatContact';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';

describe('ExoChatContactList.test.js', () => {
  const cmp = shallow(ExoChatContactList, {
    propsData: {
      loadingContacts : false,
      selected: {
        'fullName':'Test User',
        'unreadTotal':0,
        'isActive':'true',
        'type':'u',
        'user':'testuser',
        'room':'eb74205830cf97546269bbdc5d439b29ddd1735b',
        'status':'invisible',
        'timestamp':1528455913624,
        'isFavorite':false
      },
      contacts: [
        {
          'lastMessage':{
            'msg':'Test Message',
            'isSystem':false,
            'options':{},
            'msgId':'5b1a7a4067c9a30c23b5223d',
            'fullname':'Test User 2',
            'type':null,
            'user':'testuser2',
            'timestamp':1528461888213
          },
          'fullName':'Test User 2',
          'unreadTotal':0,
          'isActive':'true',
          'type':'u',
          'user':'testuser2',
          'room':'ca5dc389cc131008d504a44e64dc3d06279a93ae',
          'status':'invisible',
          'timestamp':1528461888215,
          'isFavorite':true
        },
        {
          'fullName':'Test User',
          'unreadTotal':0,
          'isActive':'true',
          'type':'u',
          'user':'testuser',
          'room':'eb74205830cf97546269bbdc5d439b29ddd1735b',
          'status':'invisible',
          'timestamp':1558455913624,
          'isFavorite':false
        },
        {
          'lastMessage':{
            'msg':'',
            'isSystem':true,
            'options':{
              'fullname':'Root Root',
              'type':'type-add-team-user'
            },
            'msgId':'5b311b0a4b8734281e6e80c5',
            'fullname':'Root Root',
            'type':null,
            'user':'root',
            'timestamp':1529944842645
          },
          'fullName':'Team Room',
          'unreadTotal':2,
          'isActive':'true',
          'type':'t',
          'user':'team-be95776cd7c3950f190f0e21ea1e4848fec3874f',
          'room':'be95776cd7c3950f190f0e21ea1e4848fec3874f',
          'admins':[
            'root'
          ],
          'status':'team',
          'timestamp':1529944842647,
          'isFavorite':true
        },
        {
          'lastMessage':{
            'msg':'',
            'isSystem':true,
            'options':{
              'fullname':'Root Root',
              'type':'type-add-team-user'
            },
            'msgId':'5b311b0a4b8734281e6e90f5',
            'fullname':'Root Root',
            'type':null,
            'user':'root',
            'timestamp':1529944842505
          },
          'fullName':'qdqsdsqdqsdqsdsqdsqd',
          'unreadTotal':0,
          'isActive':'true',
          'type':'s',
          'user':'space-be95776cd7c3950f190f0e21ea1e4848fed8995',
          'room':'be95776cd7c3950f190f0e21ea1e4848fed8995',
          'admins':[
            'root'
          ],
          'status':'team',
          'timestamp':1529944842515,
          'isFavorite':true
        }
      ]
    },
    mocks: {
      $t: () => {},
      $constants : chatConstants,
      mq: 'desktop'
    }
  });

  it('4 displayed contacts', () => {
    expect(cmp.findAll(ExoChatContact)).toHaveLength(4);
  });

  it('2 displayed user', () => {
    expect(cmp.vm.usersCount).toBe(2);
  });
  
  it('1 displayed team room', () => {
    expect(cmp.vm.roomsCount).toBe(1);
  });
  
  it('3 displayed favorites', () => {
    expect(cmp.vm.favoritesCount).toBe(3);
  });

  it('1 displayed space', () => {
    expect(cmp.vm.spacesCount).toBe(1);
  });
  
  it('No more contacts are to load', () => {
    expect(cmp.vm.hasMoreContacts).toBe(false);
  });

  it('Search term should display only exact entries', () => {
    cmp.setData({searchTerm : 'Test User'});
    try {
      expect(cmp.findAll(ExoChatContact)).toHaveLength(2);
    } finally {
      cmp.setData({searchTerm : ''});
    }
  });

  it('Test display order', () => {
    cmp.setData({sortFilter : 'Recent'});
    try {
      expect(cmp.vm.filteredContacts[0].user).toBe('testuser');
    } finally {
      cmp.setData({sortByDate : chatConstants.SORT_FILTER_DEFAULT});
    }
    cmp.setData({sortFilter : 'Unread'});
    try {
      expect(cmp.vm.filteredContacts[0].fullName).toBe('Team Room');
    } finally {
      cmp.setData({sortByDate : chatConstants.SORT_FILTER_DEFAULT});
    }
  });

  it('Test display with type filter', () => {
    cmp.setData({typeFilter : 'All'});
    try {
      expect(cmp.vm.filteredContacts).toHaveLength(4);
    } finally {
      cmp.setData({typeFilter : chatConstants.TYPE_FILTER_DEFAULT});
    }
    cmp.setData({typeFilter : 'People'});
    try {
      expect(cmp.vm.filteredContacts).toHaveLength(2);
    } finally {
      cmp.setData({typeFilter : chatConstants.TYPE_FILTER_DEFAULT});
    }
    cmp.setData({typeFilter : 'Rooms'});
    try {
      expect(cmp.vm.filteredContacts).toHaveLength(1);
    } finally {
      cmp.setData({typeFilter : chatConstants.TYPE_FILTER_DEFAULT});
    }
    cmp.setData({typeFilter : 'Spaces'});
    try {
      expect(cmp.vm.filteredContacts).toHaveLength(1);
    } finally {
      cmp.setData({typeFilter : chatConstants.TYPE_FILTER_DEFAULT});
    }
    cmp.setData({typeFilter : 'Favorites'});
    try {
      expect(cmp.vm.filteredContacts).toHaveLength(3);
    } finally {
      cmp.setData({typeFilter : chatConstants.TYPE_FILTER_DEFAULT});
    }
  });

  it('Selected contact to be "Test User"', () => {
    expect(cmp.findAll('.contact-list-room-item.selected')).toHaveLength(1);
    expect(cmp.find('.contact-list-room-item.selected').find(ExoChatContact).props().userName).toBe('testuser');
  });

  it('Room "Team Room" has unread total', () => {
    expect(cmp.findAll('#chat-users .contact-list-room-item .unreadMessages')).toHaveLength(1);
    expect(cmp.find('#chat-users .contact-list-room-item .unreadMessages').text()).toBe('2');
  });

  it('emits load-more-contacts event when calling loadMore method', () => {
    cmp.vm.loadMore();
    expect(cmp.emitted('load-more-contacts')).toHaveLength(1);
  });

  it('Current user joined new room that should be displayed in contact list', () => {
    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_ROOM_MEMBER_JOINED, {detail: {
      room:'be95776cd7c3950f190f0e21ea1e4848fec38aaa',
      data : {
        'fullName':'Team Room added',
        'unreadTotal':1,
        'isActive':'true',
        'type':'t',
        'user':'team-be95776cd7c3950f190f0e21ea1e4848fec38aaa',
        'room':'be95776cd7c3950f190f0e21ea1e4848fec38aaa',
        'admins':[
          'root'
        ],
        'status':'team',
        'timestamp':1529944842800,
        'isFavorite':false
      }
    }}));
    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_ROOM_MEMBER_JOINED, {detail: {
      room:'be95776cd7c3950f190f0e21ea1e4848fec38bbb',
      data : {
        'fullName':'Team Room added 2',
        'unreadTotal':1,
        'isActive':'true',
        'type':'t',
        'user':'team-be95776cd7c3950f190f0e21ea1e4848fec38bbb',
        'room':'be95776cd7c3950f190f0e21ea1e4848fec38bbb',
        'admins':[
          'root'
        ],
        'status':'team',
        'timestamp':1529944842801,
        'isFavorite':false
      }
    }}));
    expect(cmp.vm.filteredContacts).toHaveLength(6);
    expect(cmp.vm.roomsCount).toBe(3);
  });

  it('Current user deleted room that should be deleted from contact list', () => {
    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_ROOM_DELETED, {detail: {
      room:'be95776cd7c3950f190f0e21ea1e4848fec38aaa',
      data : {
        'fullName':'Team Room added',
        'unreadTotal':1,
        'isActive':'true',
        'type':'t',
        'user':'team-be95776cd7c3950f190f0e21ea1e4848fec38aaa',
        'room':'be95776cd7c3950f190f0e21ea1e4848fec38aaa',
        'admins':[
          'root'
        ],
        'status':'team',
        'timestamp':1529944842800,
        'isFavorite':false
      }
    }}));
    expect(cmp.vm.filteredContacts).toHaveLength(5);
    expect(cmp.vm.roomsCount).toBe(2);
  });

  it('Current user left room that should be deleted from contact list', () => {
    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_ROOM_DELETED, {detail: {
      room:'be95776cd7c3950f190f0e21ea1e4848fec38bbb',
      data : {
        'fullName':'Team Room added 2',
        'unreadTotal':1,
        'isActive':'true',
        'type':'t',
        'user':'team-be95776cd7c3950f190f0e21ea1e4848fec38bbb',
        'room':'be95776cd7c3950f190f0e21ea1e4848fec38bbb',
        'admins':[
          'root'
        ],
        'status':'team',
        'timestamp':1529944842801,
        'isFavorite':false
      }
    }}));
    expect(cmp.vm.filteredContacts).toHaveLength(4);
    expect(cmp.vm.roomsCount).toBe(1);
  });

  it('current user add/remove favorites', () => {
    expect(cmp.vm.favoritesCount).toBe(3);

    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_ROOM_FAVORITE_ADDED, {detail: {
      room: 'eb74205830cf97546269bbdc5d439b29ddd1735b'
    }}));

    expect(cmp.vm.favoritesCount).toBe(4);

    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_ROOM_FAVORITE_REMOVED, {detail: {
      room:'eb74205830cf97546269bbdc5d439b29ddd1735b'
    }}));

    expect(cmp.vm.favoritesCount).toBe(3);
  });

  it('Mark room as read', () => {
    let contact = cmp.vm.findContact('be95776cd7c3950f190f0e21ea1e4848fec3874f');
    expect(contact.unreadTotal).toBe(2);

    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_MESSAGE_READ, {detail: {
      room:'be95776cd7c3950f190f0e21ea1e4848fec3874f'
    }}));

    contact = cmp.vm.findContact('be95776cd7c3950f190f0e21ea1e4848fec3874f');
    expect(contact.unreadTotal).toBe(0);
  });

  it('Increment unreadTotal for a room that receives a message', () => {
    let contact = cmp.vm.findContact('be95776cd7c3950f190f0e21ea1e4848fec3874f');
    expect(contact.unreadTotal).toBe(0);

    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_MESSAGE_RECEIVED, {detail: {
      'msg':'Test Message for user',
      'isSystem':false,
      'options':{},
      'msgId':'5b1a7a4067c9a30c23b52654',
      'fullname':'Test User',
      'type':null,
      'user':'testuser',
      'timestamp':1528461888213,
      'room': 'be95776cd7c3950f190f0e21ea1e4848fec3874f'
    }}));

    contact = cmp.vm.findContact('be95776cd7c3950f190f0e21ea1e4848fec3874f');
    expect(contact.unreadTotal).toBe(1);
  });

  it('Mark all rooms as read', () => {
    let contact = cmp.vm.findContact('be95776cd7c3950f190f0e21ea1e4848fec3874f');
    expect(contact.unreadTotal).toBe(1);

    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_MESSAGE_READ));

    contact = cmp.vm.findContact('be95776cd7c3950f190f0e21ea1e4848fec3874f');
    expect(contact.unreadTotal).toBe(0);
  });

  it('Change user status from contact list', () => {
    let contact = cmp.vm.findContactByRoomOrUser(null, 'testuser');
    expect(contact.status).toBe('invisible');

    document.dispatchEvent(new CustomEvent(chatConstants.EVENT_USER_STATUS_CHANGED, {detail: {
      sender:'testuser',
      data:{
        status:'available'
      }
    }}));

    contact = cmp.vm.findContactByRoomOrUser(null, 'testuser');
    expect(contact.status).toBe('available');
  });

  it('emits contact-selected event when selecting a room', () => {
    cmp.vm.selectContact({
      'user':'team-be95776cd7c3950f190f0e21ea1e4848fec3874f',
      'room':'be95776cd7c3950f190f0e21ea1e4848fec3874f'
    });
    expect(cmp.emitted('contact-selected')).toHaveLength(1);
  });

});
