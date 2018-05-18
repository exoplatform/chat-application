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
    <modal v-show="createRoomModal" title="Add new room" modal-class="create-room-modal" @modal-closed="createRoomModal = false">
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
        <a href="#" class="btn btn-primary" @click="saveNewRoom">Enregistrer</a>
        <a href="#" class="btn" @click="createRoomModal = false">Annuler</a>
      </div>
    </modal>
  </div>
</template>

<script>
import * as chatServices from '../chatServices';
import ChatContact from './ChatContact.vue';
import DropdownSelect from './DropdownSelect.vue';
import Modal from './Modal.vue';
import initSuggester from '../chatSuggester';
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
      sortFilter: 'Recent',
      filterByType: ['All','People','Rooms','Spaces','Favorites'],
      typeFilter: 'All',
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
      sortedContacts.sort(function(a, b){return b.timestamp - a.timestamp;});
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
  },
  methods: {
    selectContact(contact) {
      this.$emit('exo-chat-contact-selected', contact);
      contact.unreadTotal = 0;
    },
    toggleFavorite(contact) {
      contact.isFavorite = !contact.isFavorite;
    },
    selectSortFilter(filter) {
      this.sortFilter = filter;
    },
    selectTypeFilter(filter) {
      this.typeFilter = filter;
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
      this.newRoom.participants.splice(i,1);
    },
    saveNewRoom() {
      if (this.newRoom.name) {
        let users = this.newRoom.participants.map(user => user.name);
        users.unshift(eXo.chat.userSettings.username);
        users = users.join(',');
        chatServices.saveRoom(eXo.chat.userSettings,  this.newRoom.name, users).then(() => {
          // reset newRoom
          this.newRoom.name = '';
          this.newRoom.participants = [];
          this.createRoomModal = false;
        });
      }
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
      const roomLeft = message.data ? message.data.room : message.room;
      const roomIndex = this.contacts.findIndex(contact => contact.room === roomLeft);
      if (roomIndex >= 0) {
        this.contacts.splice(roomIndex, 1);
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