<template>
  <div class="contactListContainer">
    <div class="contactFilter">
      <i class="uiIconSearchLight"></i>
      <input v-model="searchTerm" type="text" placeholder="Filter people, spaces...">
    </div>
    <div class="listHeader">
      <dropdown-select>
        <span slot="toggle">{{ sortFilter }}</span>
        <i slot="toggle" class="uiIconArrowDownMini"></i>
        <li v-for="filter in sortByDate" slot="menu" :key="filter" @click="selectSortFilter(filter)"><a href="#">{{ filter }}</a></li>
      </dropdown-select>
      <dropdown-select>
        <span slot="toggle">{{ typeFilter }}</span>
        <i slot="toggle" class="uiIconArrowDownMini"></i>
        <li v-for="filter in filterByType" slot="menu" :key="filter" @click="selectTypeFilter(filter)"><a href="#">{{ filter }}</a></li>
      </dropdown-select>
      <div class="add-room-action" @click="openCreateRoomModal">
        <i class="uiIconSimplePlus"></i>
      </div>
    </div>
    <div class="contactList">
      <div v-for="contact in filteredContacts" :key="contact.user" :class="{selected: selected && contact && selected.user == contact.user, hasUnreadMessages: contact.unreadTotal > 0}" class="contact-list-item isList" @click="selectContact(contact)">
        <chat-contact :list="true" :type="contact.type" :user-name="contact.user" :name="contact.fullName" :status="contact.status"></chat-contact>
        <div v-if="contact.unreadTotal > 0" class="unreadMessages">{{ contact.unreadTotal }}</div>
        <div :class="{'is-fav': contact.isFavorite}" class="uiIcon favorite" @click.stop="toggleFavorite(contact)"></div>
      </div>
      <div v-show="isSearchingContact" class="contact-list-item isList">
        <div class="seeMoreContacts">
          Loading ...
        </div>
      </div>
      <div v-show="hasMoreContacts" class="contact-list-item isList" @click="loadMore()">
        <div class="seeMoreContacts">
          <a href="#"><u>See more</u></a>
          <i class="uiIconArrowDownMini"></i>
        </div>
      </div>
    </div>
    <room-form-modal :show="createRoomModal" :selected="newRoom" @room-saved="roomSaved" @modal-closed="closeModal"></room-form-modal>
  </div>
</template>

<script>
import * as chatServices from '../chatServices';
import * as chatWebStorage from '../chatWebStorage';
import * as chatWebSocket from '../chatWebSocket';

import ChatContact from './ChatContact.vue';
import DropdownSelect from './DropdownSelect.vue';
import RoomFormModal from './modal/RoomFormModal.vue';

const TYPE_FILTER_PARAM = 'exo.chat.type.filter';
const TYPE_FILTER_DEFAULT = 'All';
const SORT_FILTER_PARAM = 'exo.chat.sort.filter';
const SORT_FILTER_DEFAULT = 'Recent';
const CONTACTS_PER_PAGE = 20;

export default {
  components: {ChatContact, DropdownSelect, RoomFormModal},
  props: {
    contacts: {
      type: Array,
      default: function() { return [];}
    },
    isSearchingContact: {
      type: Boolean,
      default: function() { return false;}
    },
    selected: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data : function() {
    return {
      sortByDate: ['Recent','Unread'], // TODO add to locale
      sortFilter: SORT_FILTER_DEFAULT,
      filterByType: ['All','People','Rooms','Spaces','Favorites'],
      typeFilter: TYPE_FILTER_DEFAULT,
      createRoomModal: false,
      searchTerm: '',
      totalEntriesToLoad: CONTACTS_PER_PAGE,
      newRoom: {
        name: '',
        participants: []
      }
    };
  },
  computed: {
    statusStyle() {
      return this.contactStatus === 'inline' ? 'user-available' : 'user-invisible';
    },
    usersCount() {
      return this.contacts.filter(contact => contact.type === 'u').length;
    },
    roomsCount() {
      return this.contacts.filter(contact => contact.type === 't').length;
    },
    spacesCount() {
      return this.contacts.filter(contact => contact.type === 's').length;
    },
    favoritesCount() {
      return this.contacts.filter(contact => contact.isFavorite).length;
    },
    filteredContacts: function() {
      let sortedContacts = this.contacts.slice(0);
      if(this.typeFilter !== 'All') {
        sortedContacts = sortedContacts.filter(contact =>
          this.typeFilter === 'People' && contact.type === 'u'
          || this.typeFilter === 'Rooms' && contact.type === 't'
          || this.typeFilter === 'Spaces' && contact.type === 's'
          || this.typeFilter === 'Favorites' && contact.isFavorite
        );
      }
      if (this.searchTerm && this.searchTerm.trim().length) {
        sortedContacts = sortedContacts.filter(contact => contact.fullName.toLowerCase().indexOf(this.searchTerm.toLowerCase()) >= 0);
      }
      if (this.sortFilter === 'Unread') {
        sortedContacts.sort(function(a, b){
          const unreadTotal = b.unreadTotal - a.unreadTotal;
          if (unreadTotal === 0) {
            return b.timestamp - a.timestamp;
          }
          return unreadTotal;
        });
      } else {
        sortedContacts.sort(function(a, b){
          return b.timestamp - a.timestamp;
        });
      }
      return sortedContacts.slice(0, this.totalEntriesToLoad);
    },
    hasMoreContacts() {
      if(this.searchTerm.trim().length) {
        return false;
      }
      // All Rooms and spaces are loaded with the first call, only users are paginated 
      switch (this.typeFilter) {
      case 'People':
        return this.usersCount >= this.totalEntriesToLoad;
      case 'Rooms':
        return this.roomsCount > this.totalEntriesToLoad;
      case 'Spaces':
        return this.spacesCount > this.totalEntriesToLoad;
      case 'Favorites':
        return this.favoritesCount > this.totalEntriesToLoad;
      default:
        return this.contacts.length > this.totalEntriesToLoad || this.usersCount >= this.totalEntriesToLoad;
      }
    }
  },
  watch: {
    searchTerm(value) {
      this.$emit('search-contact', value);
    }
  },
  created() {
    document.addEventListener('exo-chat-room-member-left', this.leftRoom);
    document.addEventListener('exo-chat-room-deleted', this.leftRoom);
    document.addEventListener('exo-chat-room-member-joined', this.joinedToNewRoom);
    document.addEventListener('exo-chat-room-favorite-added', this.favoriteAdded);
    document.addEventListener('exo-chat-room-favorite-removed', this.favoriteRemoved);
    document.addEventListener('exo-chat-message-sent', this.messageReceived);
    document.addEventListener('exo-chat-message-sent', this.notificationCountUpdated);
    document.addEventListener('exo-chat-user-status-changed', this.contactStatusChanged);
    document.addEventListener('exo-chat-message-read', this.markRoomMessagesRead);
    document.addEventListener('exo-chat-setting-editRoom', this.editRoom);
    document.addEventListener('exo-chat-setting-leaveRoom', this.leaveRoom);
    document.addEventListener('exo-chat-setting-deleteRoom', this.deleteRoom);
    document.addEventListener('exo-chat-select-room', this.selectContact);
    document.addEventListener('exo-chat-selected-contact-changed', this.contactChanged);
    this.typeFilter = chatWebStorage.getStoredParam(TYPE_FILTER_PARAM, TYPE_FILTER_DEFAULT);
    this.sortFilter = chatWebStorage.getStoredParam(SORT_FILTER_PARAM, SORT_FILTER_DEFAULT);
  },
  destroyed() {
    document.removeEventListener('exo-chat-room-member-left', this.leftRoom);
    document.removeEventListener('exo-chat-room-deleted', this.leftRoom);
    document.removeEventListener('exo-chat-room-member-joined', this.joinedToNewRoom);
    document.removeEventListener('exo-chat-room-favorite-added', this.favoriteAdded);
    document.removeEventListener('exo-chat-room-favorite-removed', this.favoriteRemoved);
    document.removeEventListener('exo-chat-message-sent', this.messageReceived);
    document.removeEventListener('exo-chat-user-status-changed', this.contactStatusChanged);
    document.removeEventListener('exo-chat-message-read', this.markRoomMessagesRead);
    document.removeEventListener('exo-chat-setting-editRoom', this.editRoom);
    document.removeEventListener('exo-chat-setting-leaveRoom', this.leaveRoom);
    document.removeEventListener('exo-chat-setting-deleteRoom', this.deleteRoom);
    document.removeEventListener('exo-chat-select-room', this.selectContact);
    document.removeEventListener('exo-chat-selected-contact-changed', this.contactChanged);
  },
  methods: {
    selectContact(contact) {
      if(!contact) {
        contact = {};
      }
      if(contact.detail) {
        contact = contact.detail;
      }
      this.$emit('exo-chat-contact-selected', contact);
    },
    contactChanged(e) {
      let selectedContact = e.detail;
      if(this.filteredContacts.length > 0 && !this.filteredContacts.find(contact => contact.room === selectedContact.room)) {
        // Select different contact if the contact is not visible
        selectedContact = this.filteredContacts[0];
        this.$emit('exo-chat-contact-selected', selectedContact);
      }
      selectedContact.unreadTotal = 0;
      chatWebSocket.setRoomMessagesAsRead(selectedContact.room);
    },
    toggleFavorite(contact) {
      chatServices.toggleFavorite(contact.room, !contact.isFavorite).then(contact.isFavorite = !contact.isFavorite);
    },
    selectSortFilter(filter) {
      this.sortFilter = filter;
      chatWebStorage.setStoredParam(SORT_FILTER_PARAM, this.sortFilter);
    },
    selectTypeFilter(filter) {
      this.typeFilter = filter;
      chatWebStorage.setStoredParam(TYPE_FILTER_PARAM, this.typeFilter);
    },
    openCreateRoomModal() {
      this.newRoom = {};
      this.createRoomModal = true;
    },
    messageReceived(event) {
      const room = event.detail.room;
      let foundContact;
      this.contacts.forEach(contact => {
        if (contact.room === room) {
          foundContact = contact;
          contact.timestamp = event.detail.ts;
          if (this.selected.room !== room) {
            contact.unreadTotal ++;
          }
        }
      });

      if(!foundContact) {
        chatServices.getRoomDetail(eXo.chat.userSettings, room).then((contact) => {
          this.contacts.unshift(contact);
        });
      }
    },
    roomSaved(room) {
      this.createRoomModal = false;
      this.selectContact(room);
    },
    editRoom() {
      chatServices.getRoomParticipants(eXo.chat.userSettings, this.selected).then(data => {
        this.selected.participants = data.users;
        this.newRoom = JSON.parse(JSON.stringify(this.selected));
        this.createRoomModal = true;
      });
    },
    leaveRoom() {
      if(this.selected && this.selected.type === 't') {
        chatWebSocket.leaveRoom(this.selected.room);
      }
    },
    deleteRoom() {
      if(this.selected && this.selected.type === 't') {
        chatWebSocket.deleteRoom(this.selected.room);
      }
    },
    closeModal() {
      this.createRoomModal = false;
      this.newRoom = {};
    },
    markRoomMessagesRead(message) {
      const contactToUpdate = this.findContact(message.room);
      if(contactToUpdate) {
        contactToUpdate.unreadTotal = 0;
      }
    },
    contactStatusChanged(event) {
      const contactChanged = event.detail;
      const foundContact = this.findContact(contactChanged.sender, 'user');
      if (foundContact) {
        foundContact.status = contactChanged.data.status;
      }
    },
    leftRoom(message) {
      message = message.detail ? message.detail: message;
      const roomLeft = message.data ? message.data.room : message.room;
      const roomIndex = this.contacts.findIndex(contact => contact.room === roomLeft);
      if (roomIndex >= 0) {
        this.contacts.splice(roomIndex, 1);
        if(this.selected && this.selected.room === roomLeft) {
          if(!this.contacts || this.contacts.length === 0) {
            this.selected = null;
          } else {
            if(this.filteredContacts && this.filteredContacts.length) {
              this.selectContact(this.filteredContacts[0]);
            } else {
              this.selectContact();
            }
          }
        }
      }
    },
    joinedToNewRoom(e) {
      if (!this.findContact(e.detail.room)) {
        this.contacts.push(e.detail.data);
      }
    },
    favoriteAdded(message) {
      const contactToUpdate = this.findContact(message.room);
      if(contactToUpdate) {
        contactToUpdate.isFavorite = true;
      }
    },
    favoriteRemoved(message) {
      const contactToUpdate = this.findContact(message.room);
      if(contactToUpdate) {
        contactToUpdate.isFavorite = false;
      }
    },
    findContact(value, field) {
      if (!field)  {
        field = 'room';
      }
      return this.contacts.find(contact => contact[field] === value);
    },
    loadMore() {
      this.totalEntriesToLoad += CONTACTS_PER_PAGE;
      this.$emit('load-more-contacts', this.totalEntriesToLoad);
    }
  }
};
</script>