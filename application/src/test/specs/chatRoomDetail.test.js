import { shallow } from 'vue-test-utils';
import ChatContact from '../../main/webapp/vue-app/components/ChatContact';
import ChatRoomDetail from '../../main/webapp/vue-app/components/ChatRoomDetail';
import DropdownSelect from '../../main/webapp/vue-app/components/DropdownSelect';
import RoomNotificationModal from '../../main/webapp/vue-app/components/modal/RoomNotificationModal';
import Modal from '../../main/webapp/vue-app/components/modal/Modal';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';

describe('ChatRoomDetail.test.js', () => {
  let roomDetail;
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

  beforeEach(() => {
    roomDetail = shallow(ChatRoomDetail, {
      propsData: {
        contact : room
      },
      stubs: {
        'chat-contact': ChatContact,
        'modal': Modal,
        'Room-notification-modal': RoomNotificationModal
      },
      mocks: {
        $t: () => {},
        $constants : chatConstants
      },
      attachToDocument: true
    });
  });


  it('Room detail contain contact', () => {
    expect(roomDetail.contains(ChatContact)).toBe(true);
  });

  it('contact has is-fav class when room is favorite', () => {
    const contact = roomDetail.find(ChatContact);
    expect(contact.find('.favorite').classes()).toContain('is-fav');
  });

  it('open room notification modal', () => {
    const vm = roomDetail.vm;
    // modal must be closed: openNotificationSettings = false
    expect(vm.openNotificationSettings).toBe(false);
    // trigger open room setting event
    roomDetail.trigger(vm.$constants.ACTION_ROOM_OPEN_SETTINGS);
    // modal must be opned: openNotificationSettings = true
    expect(vm.openNotificationSettings).toBe(true);
  });

  it('open search area when click on loop icon', () => {
    const searchButton = roomDetail.find('.room-search-btn');
    const vm = roomDetail.vm;
    // check search icon exist
    expect(searchButton.exists()).toBe(true);
    // search input must be hidden
    expect(vm.showSearchRoom).toBe(false);
    // trigger search icon click
    searchButton.trigger('click');
    // search input must be visible
    expect(vm.showSearchRoom).toBe(true);
    expect(roomDetail.find('.room-actions-container').classes()).toContain('search-active');
  });

  it('close search area when click on close icon', () => {
    const closeButton = roomDetail.find('.room-search i');
    const vm = roomDetail.vm;
    vm.openSearchRoom();
    expect(vm.showSearchRoom).toBe(true);
    closeButton.trigger('click');
    expect(vm.showSearchRoom).toBe(false);
  });

  it('close search area when click on blur', () => {
    const searchInput = roomDetail.find('.room-search input');
    const vm = roomDetail.vm;
    vm.openSearchRoom();
    expect(vm.showSearchRoom).toBe(true);
    searchInput.trigger('blur');
    expect(vm.showSearchRoom).toBe(false);
  });

  it('close search area must still displayed on blur when the seach field is filled', () => {
    const searchInput = roomDetail.find('.room-search input');
    const vm = roomDetail.vm;
    vm.openSearchRoom();
    expect(vm.showSearchRoom).toBe(true);
    // fill the search input with text
    vm.searchText = 'test';
    searchInput.trigger('blur');
    expect(vm.showSearchRoom).toBe(true);
  });

  it('close search area when click on esc key press', () => {
    const searchInput = roomDetail.find('.room-search input');
    const vm = roomDetail.vm;
    vm.openSearchRoom();
    expect(vm.showSearchRoom).toBe(true);
    searchInput.trigger('keyup.esc');
    expect(vm.showSearchRoom).toBe(false);
  });

  it('Room detail contain action menu only for rooms and spaces', () => {
    // action menu should be displayed for rooms
    expect(roomDetail.contains(DropdownSelect)).toBe(true);
    // action menu should not be displayed for users
    roomDetail.setProps({ contact: user });
    expect(roomDetail.contains(DropdownSelect)).toBe(false);
    // action menu should be displayed for spaces
    roomDetail.setProps({ contact: space });
    expect(roomDetail.contains(DropdownSelect)).toBe(true);
  });


  
});
