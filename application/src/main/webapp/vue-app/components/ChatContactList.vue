<template>
  <div class="contactListContainer">
    <div class="contactFilter">
      <i class="uiIconSearchLight"></i>
      <input type="text" placeholder="Filter people, spaces...">
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
      <div v-for="contact in filteredContacts" :key="contact.user" :class="{selected: selected && contact && selected.user == contact.user}" class="contact-list-item isList" @click="selectContact(contact)">
        <chat-contact :list="true" :type="contact.type" :user-name="contact.user" :name="contact.fullName" :status="contact.status"></chat-contact>
        <div v-if="contact.unreadTotal > 0" class="unreadMessages">{{ contact.unreadTotal }}</div>
        <div :class="{'is-fav': contact.isFavorite}" class="uiIcon favorite" @click.stop="toggleFavorite(contact)"></div>
      </div>
    </div>
    <modal v-show="createRoomModal" :title="title" modal-class="create-room-modal" @modal-closed="closeNewRoomModal">
      <div class="add-room-form">
        <label>Quel est le nom de votre salon?</label>
        <input v-model="newRoom.name" type="text">
        <label>Ajouter des personnes à votre salon:</label>
        <input id="add-room-suggestor" type="text">
        <div v-show="newRoom.participants.length > 0" class="room-suggest-list">
          <div v-for="(participant, index) in newRoom.participants" :key="participant.name" class="uiMention">
            {{ participant.fullname }}
            <span @click="removeSuggest(index)"><i class="uiIconClose"></i></span>
          </div>
        </div>
        <span class="team-add-user-label">Ex: "ro" or "Ro Be" pour trouver Robert Beranot</span>
      </div>
      <div class="uiAction uiActionBorder">
        <a href="#" class="btn btn-primary" @click="saveRoom">Enregistrer</a>
        <a href="#" class="btn" @click="closeNewRoomModal">Annuler</a>
      </div>
    </modal>
  </div>
</template>

<script>
import * as chatServices from '../chatServices';
import * as chatWebStorage from '../chatWebStorage';

import ChatContact from './ChatContact.vue';
import DropdownSelect from './DropdownSelect.vue';
import Modal from './Modal.vue';
import initSuggester from '../chatSuggester';

const TYPE_FILTER_PARAM = 'exo.chat.type.filter';
const TYPE_FILTER_DEFAULT = 'All';
const SORT_FILTER_PARAM = 'exo.chat.sort.filter';
const SORT_FILTER_DEFAULT = 'Recent';

export default {
  components: {ChatContact, DropdownSelect, Modal},
  props: {
    contacts: {
      type: Array,
      default: function() { return [];}
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
    title() {
      const key = !this.newRoom.name || !this.newRoom.name.length ? 'chat.rooms.new' : 'chat.rooms.edit';
      return this.$t(key);
    },
    filteredContacts: function() {
      let sortedContacts = this.contacts;
      if(this.typeFilter !== 'All') {
        sortedContacts = this.contacts.filter(contact =>
          this.typeFilter === 'People' && contact.type === 'u'
          || this.typeFilter === 'Rooms' && contact.type === 't'
          || this.typeFilter === 'Spaces' && contact.type === 's'
          || this.typeFilter === 'Favorites' && contact.isFavorite
        );
      }
      if (this.sortFilter === 'Unread') {
        sortedContacts.sort(function(a, b){return b.unreadTotal - a.unreadTotal;});
      } else {
        sortedContacts.sort(function(a, b){return b.timestamp - a.timestamp;});
      }
      return sortedContacts;
    }
  },
  created() {
    document.addEventListener('exo-chat-room-member-left', this.leftRoom);
    document.addEventListener('exo-chat-room-deleted', this.leftRoom);
    document.addEventListener('exo-chat-room-member-joined', this.joinedToNewRoom);
    document.addEventListener('exo-chat-room-favorite-added', this.favoriteAdded);
    document.addEventListener('exo-chat-room-favorite-removed', this.favoriteRemoved);
    document.addEventListener('exo-chat-message-received', this.notificationCountUpdated);
    document.addEventListener('exo-chat-user-status-changed', this.contactStatusChanged);
    document.addEventListener('exo-chat-message-read', this.markRoomMessagesRead);
    document.addEventListener('exo-chat-setting-editRoom', this.editRoom);
    document.addEventListener('exo-chat-setting-leaveRoom', this.leaveRoom);
    this.typeFilter = chatWebStorage.getStoredParam(TYPE_FILTER_PARAM, TYPE_FILTER_DEFAULT);
    this.sortFilter = chatWebStorage.getStoredParam(SORT_FILTER_PARAM, SORT_FILTER_DEFAULT);
  },
  destroyed() {
    document.removeEventListener('exo-chat-room-member-left', this.leftRoom);
    document.removeEventListener('exo-chat-room-deleted', this.leftRoom);
    document.removeEventListener('exo-chat-room-member-joined', this.joinedToNewRoom);
    document.removeEventListener('exo-chat-room-favorite-added', this.favoriteAdded);
    document.removeEventListener('exo-chat-room-favorite-removed', this.favoriteRemoved);
    document.removeEventListener('exo-chat-message-received', this.notificationCountUpdated);
    document.removeEventListener('exo-chat-user-status-changed', this.contactStatusChanged);
    document.removeEventListener('exo-chat-message-read', this.markRoomMessagesRead);
    document.removeEventListener('exo-chat-setting-editRoom', this.editRoom);
    document.removeEventListener('exo-chat-setting-leaveRoom', this.leaveRoom);
  },
  methods: {
    selectContact(contact) {
      this.$emit('exo-chat-contact-selected', contact);
      contact.unreadTotal = 0;
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
      this.createRoomModal = true;
      initSuggester('#add-room-suggestor', eXo.chat.userSettings, this);
    },
    notificationCountUpdated(event) {
      const room = event.detail.room;
      this.contacts.forEach(contact => {
        if (contact.room === room) {
          contact.timestamp = event.detail.ts;
          if (this.selected.room !== room) {
            contact.unreadTotal ++;
          }
        }
      });
    },
    removeSuggest(i) {
      const suggest = this.newRoom.participants[i].name;
      const suggesterInput = $('#add-room-suggestor');
      // cast suugester value to array
      let suggesterValue = suggesterInput.suggester('getValue').split(',');
      // remove suggest from array
      suggesterValue = suggesterValue.filter(value => value !== suggest);
      // set new value as string
      suggesterInput.suggester('setValue', suggesterValue.join(','));
      // clear render cache
      suggesterInput[0].selectize.renderCache['item'] = {};
      // remove suggest for participants list
      this.newRoom.participants.splice(i,1);
    },
    saveRoom() {
      if (this.newRoom.name) {
        let users = this.newRoom.participants.map(user => user.name);
        users.unshift(eXo.chat.userSettings.username);
        users = users.join(',');
        chatServices.saveRoom(eXo.chat.userSettings,  this.newRoom.name, users, this.newRoom.room).then(() => {
          this.closeNewRoomModal();
        });
      }
    },
    editRoom() {
      chatServices.getRoomParticipants(eXo.chat.userSettings, this.selected).then(data => {
        this.selected.participants = data.users;
        this.newRoom.name = this.selected.fullName;
        this.newRoom.room = this.selected.room;
        this.openCreateRoomModal();
      });
    },
    leaveRoom() {
      if(this.selected && this.selected.type === 't') {
        window.chatNotification.leaveRoom(this.selected.room);
      }
    },
    closeNewRoomModal() {
      // reset newRoom
      this.createRoomModal = false;
      this.newRoom.name = '';
      this.newRoom.room = '';
      this.newRoom.participants = [];
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
            this.selectContact(this.filteredContacts()[0]);
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
    }
  }
};
</script>