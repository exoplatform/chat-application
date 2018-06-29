<template>
  <div class="contactListContainer">
    <div v-show="mq !== 'mobile' || contactSearchMobile" class="contactFilter">
      <i v-if="mq !== 'mobile'" class="uiIconSearchLight"></i>
      <input ref="contactSearch" v-model="searchTerm" :placeholder="$t('exoplatform.chat.contact.search.placeholder')" type="text">
      <div v-if="mq === 'mobile'" class="contact-search-close" @click="closeContactSearch"><i class="uiIconClose"></i></div>
    </div>
    <div class="listHeader">
      <div v-if="mq === 'mobile'" class="hamburger-menu" @click="$emit('open-side-menu')"><i class="uiIconMenu"></i></div>
      <dropdown-select v-if="mq !== 'mobile'">
        <span slot="toggle">{{ sortByDate[sortFilter] }}</span>
        <i slot="toggle" class="uiIconArrowDownMini"></i>
        <div slot="menu" class="dropdown-category">{{ $t('exoplatform.chat.contact.filter.sort') }}</div>
        <li v-for="(label, filter) in sortByDate" slot="menu" :key="filter" @click="selectSortFilter(filter)"><a href="#"><i :class="{'not-filter': sortFilter !== filter}" class="uiIconTick"></i>{{ label }}</a></li>
        <div slot="menu" class="dropdown-category">{{ $t('exoplatform.chat.contact.filter.actions') }}</div>
        <li slot="menu" @click="markAllAsRead"><a href="#"><i class="uiIconTick not-filter"></i>{{ $t('exoplatform.chat.contact.mark.read') }}</a></li>
      </dropdown-select>
      <dropdown-select v-if="mq !== 'mobile'">
        <span slot="toggle">{{ filterByType[typeFilter] }}</span>
        <i slot="toggle" class="uiIconArrowDownMini"></i>
        <li v-for="(label, filter) in filterByType" slot="menu" :key="filter" @click="selectTypeFilter(filter)"><a href="#"><i :class="{'not-filter': typeFilter !== filter}" class="uiIconTick"></i>{{ label }}</a></li>
      </dropdown-select>
      <div class="room-actions">
        <div v-if="mq === 'mobile'" class="filter-action" @click="filterMenuClosed = false">
          <i class="uiIconFilter"></i>
        </div>
        <div v-exo-tooltip.top="$t('exoplatform.chat.create.team')" class="add-room-action" @click="openCreateRoomModal">
          <i class="uiIconSimplePlus"></i>
        </div>
        <div v-if="mq === 'mobile'" @click="selectContactSearch">
          <i class="uiIconSearch"></i>
        </div>
      </div>
    </div>
    <div id="chat-users" class="contactList isList">
      <div v-hold-tap="openContactActions" v-for="contact in filteredContacts" :key="contact.user" :title="contact.fullName" :class="{selected: mq !== 'mobile' && selected && contact && selected.user === contact.user, currentContactMenu: mq === 'mobile' && contactMenu && contactMenu.user === contact.user, hasUnreadMessages: contact.unreadTotal > 0, 'has-not-sent-messages' : contact.hasNotSentMessages}" class="contact-list-item contact-list-room-item" @click="selectContact(contact)">
        <chat-contact :list="true" :type="contact.type" :user-name="contact.user" :name="contact.fullName" :status="contact.status" :last-message="getLastMessage(contact.lastMessage, contact.type)">
          <div v-if="mq === 'mobile'" :class="{'is-fav': contact.isFavorite}" class="uiIcon favorite"></div>
          <div v-if="mq === 'mobile'" class="last-message-time">{{ getLastMessageTime(contact.lastMessage) }}</div>
        </chat-contact>
        <div v-if="contact.unreadTotal > 0" class="unreadMessages">{{ contact.unreadTotal }}</div>
        <i v-exo-tooltip.top.body="$t('exoplatform.chat.msg.notDelivered')" class="uiIconNotification"></i>
        <div v-exo-tooltip.top.body="favoriteTooltip(contact)" v-if="mq !== 'mobile'" :class="{'is-fav': contact.isFavorite}" class="uiIcon favorite" @click.stop="toggleFavorite(contact)"></div>
      </div>
      <div v-show="isSearchingContact" class="contact-list-item isList">
        <div class="seeMoreContacts">
          {{ $t('exoplatform.chat.loading') }}
        </div>
      </div>
      <div v-show="hasMoreContacts" class="contact-list-item isList" @click="loadMore()">
        <div class="seeMoreContacts">
          <a href="#"><u>{{ $t('exoplatform.chat.seeMore') }}</u></a>
          <i class="uiIconArrowDownMini"></i>
        </div>
      </div>
      <div v-if="mq == 'mobile' && contactMenu !== null" v-show="!contactMenuClosed" class="uiPopupWrapper modal-mask" @click.prevent.stop="closeContactActions">
        <ul class="mobile-options">
          <li><a href="#" @click.prevent="toggleFavorite(contactMenu)">{{ contactMenu.isFavorite === false ? $t('exoplatform.chat.add.favorites') : $t('exoplatform.chat.remove.favorites') }}</a></li>
          <li v-show="contactMenu.type != 't'" @click.stop><a :href="getProfileLink()">{{ $t('exoplatform.chat.contact.profile') }}</a></li>
        </ul>
      </div>
      <div v-if="mq == 'mobile'" v-show="!filterMenuClosed" class="uiPopupWrapper modal-mask">
        <ul class="mobile-options filter-options">
          <li class="options-category">
            <i class="uiIconClose" @click="cancelFilterMobile"></i>
            <span><i class="uiIconFilter"></i>{{ $t('exoplatform.chat.contact.filter') }}</span>
            <div @click="saveFilterMobile">{{ $t('exoplatform.chat.contact.filter.save') }}</div>
          </li>
          <li class="options-category">{{ $t('exoplatform.chat.contact.filter.sort') }}</li>
          <li v-for="(label, filter) in filterByType" :key="filter" @click="typeFilterMobile = filter"><a href="#"><i :class="{'not-filter': typeFilterMobile !== filter}" class="uiIconTick"></i>{{ label }}</a></li>
          <li class="options-category">{{ $t('exoplatform.chat.contact.filter.by') }}</li>
          <li v-for="(label, filter) in sortByDate" slot="menu" :key="filter" @click="sortFilterMobile = filter"><a href="#"><i :class="{'not-filter': sortFilterMobile !== filter}" class="uiIconTick"></i>{{ label }}</a></li>
          <li @click="allAsReadFilterMobile = !allAsReadFilterMobile"><a href="#"><i :class="{'not-filter': !allAsReadFilterMobile}" class="uiIconTick"></i>{{ $t('exoplatform.chat.contact.mark.read') }}</a></li>
        </ul>
      </div>
    </div>
    <room-form-modal :show="createRoomModal" :selected="newRoom" @room-saved="roomSaved" @modal-closed="closeModal"></room-form-modal>
  </div>
</template>

<script>
import * as chatServices from '../chatServices';
import * as chatWebStorage from '../chatWebStorage';
import * as chatWebSocket from '../chatWebSocket';
import * as chatTime from '../chatTime';

import ChatContact from './ChatContact.vue';
import DropdownSelect from './DropdownSelect.vue';
import RoomFormModal from './modal/RoomFormModal.vue';
import {getComposerApplications} from '../extension';

export default {
  components: {ChatContact, DropdownSelect, RoomFormModal},
  props: {
    /**
     * List of contacts objects to display in List.
     * Contact {
     *   fullName: {string} full name of contact
     *   isActive: {string} if the contact is of type user, this will be equals to "true" when the user is enabled
     *   isFavorite: {Boolean} whether is favortie of current user or not
     *   lastMessage: {string} Last message object with current user
     *   room: {string} contact room id
     *   status: {string} if the contact is of type user, this variable determines the user status (away, offline, available...)
     *   timestamp: {number} contact update timestamp
     *   type: {string} contact type, 'u' for user, 't' for team and 's' for space
     *   unreadTotal: {number} unread total number of messages for this contact
     *   user: {string} contact id, if user , username else team-{CONTACT_ID} or space-{CONTACT_ID}
     * }
     */
    contacts: {
      type: Array,
      default: function() { return [];}
    },
    /**
     * whether some additional contacts are currently loading or not
     */
    isSearchingContact: {
      type: Boolean,
      default: function() { return false;}
    },
    /**
    * Select contact object
    * Contact {
      *   fullName: {string} full name of contact
      *   isActive: {string} if the contact is of type user, this will be equals to "true" when the user is enabled
      *   isFavorite: {Boolean} whether is favortie of current user or not
      *   lastMessage: {string} Last message object with current user
      *   room: {string} contact room id
      *   status: {string} if the contact is of type user, this variable determines the user status (away, offline, available...)
      *   timestamp: {number} contact update timestamp
      *   type: {string} contact type, 'u' for user, 't' for team and 's' for space
      *   unreadTotal: {number} unread total number of messages for this contact
      *   user: {string} contact id, if user , username else team-{CONTACT_ID} or space-{CONTACT_ID}
      * }
    **/
    selected: {
      type: Object,
      default: function() {
        return {};
      }
    },
    totalEntriesToLoad: {
      type: Number,
      default: function() {
        return this.$constants.CONTACTS_PER_PAGE;
      }
    }
  },
  data : function() {
    return {
      sortByDate: {
        'Recent': this.$t('exoplatform.chat.contact.recent'),
        'Unread': this.$t('exoplatform.chat.contact.unread'),
      },
      sortFilter: this.$constants.SORT_FILTER_DEFAULT,
      sortFilterMobile: null,
      filterByType: {
        'All': this.$t('exoplatform.chat.contact.all'),
        'People': this.$t('exoplatform.chat.people'),
        'Rooms': this.$t('exoplatform.chat.teams'),
        'Spaces': this.$t('exoplatform.chat.spaces'),
        'Favorites': this.$t('exoplatform.chat.favorites')
      },
      typeFilter: this.$constants.TYPE_FILTER_DEFAULT,
      typeFilterMobile: null,
      allAsReadFilterMobile: false,
      contactSearchMobile: false,
      createRoomModal: false,
      searchTerm: '',
      newRoom: {
        name: '',
        participants: []
      },
      contactMenu: null,
      contactMenuClosed: true,
      filterMenuClosed: true
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
      let sortedContacts = this.contacts.slice(0).filter(contact => (contact.room || contact.user) && contact.fullName);
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
    document.addEventListener(this.$constants.EVENT_ROOM_MEMBER_LEFT, this.leftRoom);
    document.addEventListener(this.$constants.EVENT_ROOM_DELETED, this.leftRoom);
    document.addEventListener(this.$constants.EVENT_ROOM_MEMBER_JOINED, this.joinedToNewRoom);
    document.addEventListener(this.$constants.EVENT_ROOM_FAVORITE_ADDED, this.favoriteAdded);
    document.addEventListener(this.$constants.EVENT_ROOM_FAVORITE_REMOVED, this.favoriteRemoved);
    document.addEventListener(this.$constants.EVENT_MESSAGE_RECEIVED, this.messageReceived);
    document.addEventListener(this.$constants.EVENT_USER_STATUS_CHANGED, this.contactStatusChanged);
    document.addEventListener(this.$constants.EVENT_MESSAGE_READ, this.markRoomMessagesRead);
    document.addEventListener(this.$constants.ACTION_ROOM_EDIT, this.editRoom);
    document.addEventListener(this.$constants.ACTION_ROOM_LEAVE, this.leaveRoom);
    document.addEventListener(this.$constants.ACTION_ROOM_DELETE, this.deleteRoom);
    document.addEventListener(this.$constants.ACTION_ROOM_SELECT, this.selectContact);
    document.addEventListener(this.$constants.EVENT_ROOM_SELECTION_CHANGED, this.contactChanged);
    document.addEventListener(this.$constants.EVENT_MESSAGE_NOT_SENT, this.messageNotSent);
    this.typeFilter = chatWebStorage.getStoredParam(this.$constants.TYPE_FILTER_PARAM, this.$constants.TYPE_FILTER_DEFAULT);
    this.sortFilter = chatWebStorage.getStoredParam(this.$constants.SORT_FILTER_PARAM, this.$constants.SORT_FILTER_DEFAULT);
    this.initFilterMobile();
  },
  destroyed() {
    document.removeEventListener(this.$constants.EVENT_ROOM_MEMBER_LEFT, this.leftRoom);
    document.removeEventListener(this.$constants.EVENT_ROOM_DELETED, this.leftRoom);
    document.removeEventListener(this.$constants.EVENT_ROOM_MEMBER_JOINED, this.joinedToNewRoom);
    document.removeEventListener(this.$constants.EVENT_ROOM_FAVORITE_ADDED, this.favoriteAdded);
    document.removeEventListener(this.$constants.EVENT_ROOM_FAVORITE_REMOVED, this.favoriteRemoved);
    document.removeEventListener(this.$constants.EVENT_MESSAGE_RECEIVED, this.messageReceived);
    document.removeEventListener(this.$constants.EVENT_USER_STATUS_CHANGED, this.contactStatusChanged);
    document.removeEventListener(this.$constants.EVENT_MESSAGE_READ, this.markRoomMessagesRead);
    document.removeEventListener(this.$constants.ACTION_ROOM_EDIT, this.editRoom);
    document.removeEventListener(this.$constants.ACTION_ROOM_LEAVE, this.leaveRoom);
    document.removeEventListener(this.$constants.ACTION_ROOM_DELETE, this.deleteRoom);
    document.removeEventListener(this.$constants.ACTION_ROOM_SELECT, this.selectContact);
    document.removeEventListener(this.$constants.EVENT_ROOM_SELECTION_CHANGED, this.contactChanged);
    document.removeEventListener(this.$constants.EVENT_MESSAGE_NOT_SENT, this.messageNotSent);
  },
  methods: {
    selectContact(contact) {
      if(!contact) {
        contact = {};
      }
      if(contact.detail) {
        contact = contact.detail;
      }
      if(!contact && !contact.room && contact.user) {
        contact = contact.user;
      }
      eXo.chat.selectedContact = contact;
      this.$emit('contact-selected', contact);
    },
    contactChanged(e) {
      let selectedContact = e.detail;
      if(this.filteredContacts.length > 0 && !this.filteredContacts.find(contact => contact.room === selectedContact.room || contact.user && contact.user.trim().length && contact.user === selectedContact.user)) {
        // Select different contact if the contact is not visible
        selectedContact = this.filteredContacts[0];
        this.$emit('contact-selected', selectedContact);
      }
      if (selectedContact.room) {
        chatWebSocket.setRoomMessagesAsRead(selectedContact.room);
      }
    },
    markAllAsRead() {
      chatWebSocket.setRoomMessagesAsRead();
    },
    toggleFavorite(contact) {
      chatServices.toggleFavorite(contact.room, contact.user, !contact.isFavorite).then(contact.isFavorite = !contact.isFavorite);
    },
    selectSortFilter(filter) {
      this.sortFilter = filter;
      chatWebStorage.setStoredParam(this.$constants.SORT_FILTER_PARAM, this.sortFilter);
    },
    selectTypeFilter(filter) {
      this.typeFilter = filter;
      chatWebStorage.setStoredParam(this.$constants.TYPE_FILTER_PARAM, this.typeFilter);
    },
    initFilterMobile() {
      this.typeFilterMobile = this.typeFilter;
      this.sortFilterMobile = this.sortFilter;
    },
    saveFilterMobile() {
      this.filterMenuClosed = true;
      this.selectTypeFilter(this.typeFilterMobile);
      this.selectSortFilter(this.sortFilterMobile);
      if (this.allAsReadFilterMobile) {
        this.markAllAsRead();
      }
      this.allAsReadFilterMobile = false;
    },
    cancelFilterMobile() {
      this.filterMenuClosed = true;
      this.allAsReadFilterMobile = false;
      this.initFilterMobile();
    },
    selectContactSearch() {
      this.contactSearchMobile = true;
      this.$nextTick(() => this.$refs.contactSearch.focus());
    },
    closeContactSearch() {
      this.contactSearchMobile = false;
      this.searchTerm = '';
    },
    openCreateRoomModal() {
      this.newRoom = {};
      this.createRoomModal = true;
    },
    messageNotSent(e) {
      const foundContact = this.findContact(e.detail.room);
      if (foundContact) {
        foundContact.hasNotSentMessages = true;
        this.$forceUpdate();
      }
    },
    messageReceived(event) {
      const message = event.detail;
      const room = message.room;
      if(!room) {
        return;
      }
      const foundContact = this.findContactByRoomOrUser(room, message.data ? message.data.user : message.sender);
      if(foundContact) {
        foundContact.timestamp = message.ts;
        if (this.selected.room !== foundContact.room) {
          foundContact.unreadTotal ++;
        }
      } else {
        chatServices.getRoomDetail(eXo.chat.userSettings, room).then((contact) => {
          if(contact && contact.user && contact.user.length && contact.user !== 'undefined') {
            contact.unreadTotal = 1;
            this.contacts.unshift(contact);
          }
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
    markRoomMessagesRead(e) {
      const message = e.detail;
      if (message && message.room) {
        const contactToUpdate = this.findContact(message.room);
        if(contactToUpdate) {
          contactToUpdate.unreadTotal = 0;
          contactToUpdate.hasNotSentMessages = false;
        }
      } else {
        this.contacts.forEach(contact => {
          contact.unreadTotal = 0;
        });
        this.$emit('refresh-contacts', true);
        this.$forceUpdate();
      }
    },
    leftRoom(e) {
      const message = e.detail ? e.detail: e;
      const sender = message.data && message.data.sender ? message.data.sender : message.sender;
      if(message.event === 'room-member-left' && sender !== eXo.chat.userSettings.username) {
        return;
      }
      const roomLeft = message.data && message.data.room ? message.data.room : message.room;
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
    favoriteAdded(event) {
      const room = event.room ? event.room : event.detail && event.detail.room ? event.detail.room : null;
      const contactToUpdate = this.findContact(room);
      if(contactToUpdate) {
        contactToUpdate.isFavorite = true;
      }
    },
    favoriteRemoved(event) {
      const room = event.room ? event.room : event.detail && event.detail.room ? event.detail.room : null;
      const contactToUpdate = this.findContact(room);
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
    findContactByRoomOrUser(room, targetUser) {
      let foundContact = null;
      if (room && room.trim().length)  {
        foundContact = this.findContact(room, 'room');
      }
      if (!foundContact && targetUser && targetUser.trim().length) {
        foundContact = this.findContact(targetUser, 'user');
      }
      return foundContact;
    },
    loadMore() {
      this.totalEntriesToLoad += this.$constants.CONTACTS_PER_PAGE;
      this.$emit('load-more-contacts', this.totalEntriesToLoad);
    },
    favoriteTooltip(contact) {
      return contact.isFavorite === true ? this.$t('exoplatform.chat.remove.favorites') : this.$t('exoplatform.chat.add.favorites');
    },
    getLastMessage(message, contactType) {
      if (message) {
        if (!message.isSystem) {
          const filteredMessage = this.filterLastMessage(message.msg);
          return contactType === 'u' ? message.msg : `${message.fullname}: ${filteredMessage}`;
        } else {
          const apps = getComposerApplications();
          let systemMsg = '';
          apps.forEach(element => {
            if (message.options.type === element.type) {
              systemMsg = `<span><i class="${element.iconClass || ''}"></i>${this.$t(element.nameKey || element.labelKey || '')}</span>`;
            }
          });
          return systemMsg;
        }
      }
      return '';
    },
    getLastMessageTime(message) {
      if (message && message.timestamp) {
        const timestamp = message.timestamp;
        if (chatTime.getDayDate(timestamp) === chatTime.getDayDate(new Date())) {
          return chatTime.getTimeString(timestamp);
        } else {
          return chatTime.getDayDate(timestamp);
        }
      }
      return '';
    },
    filterLastMessage(msg) {
      if (msg === this.$constants.DELETED_MESSAGE) {
        return this.$t('exoplatform.chat.deleted');
      }
      // replace line breaks with an ellipsis
      if (msg.indexOf('<br/>') >= 0) {
        return msg.replace(msg.slice(msg.indexOf('<br/>')),'...');
      }
      return msg;
    },
    contactStatusChanged(e) {
      const contactChanged = e.detail;

      if (contactChanged.data && contactChanged.data.status && contactChanged.sender && contactChanged.sender.trim().length && eXo.chat.userSettings.username !== contactChanged.sender) {
        const foundContact = this.findContactByRoomOrUser(null, contactChanged.sender);
        if (foundContact) {
          foundContact.status = contactChanged.data.status;
        }
      }
    },
    openContactActions(user) {
      const contact = this.contacts.find(contact => contact['user'] === user);
      this.contactMenuClosed = false;
      this.contactMenu = contact;
    },
    closeContactActions() {
      this.contactMenuClosed = true;
      this.contactMenu = null;
    },
    getProfileLink() {
      if (this.contactMenu.type === 'u') {
        return chatServices.getUserProfileLink(this.contactMenu.user);
      } else if (this.contactMenu.type === 's') {
        return chatServices.getSpaceProfileLink(this.contactMenu.fullName);
      }
      return '#';
    }
  }
};
</script>