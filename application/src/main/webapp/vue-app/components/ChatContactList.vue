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
      <div v-for="contact in filteredContacts" :key="contact.user" :class="{selected: selected.user == contact.user}" class="contact-list-item isList" @click="selectContact(contact)">
        <chat-contact :list="true" :type="contact.type" :user-name="contact.user" :name="contact.fullName" :status="contact.status"></chat-contact>
        <div v-show="contact.unreadTotal > 0" class="unreadMessages">{{ contact.unreadTotal }}</div>
        <div :class="{'is-fav': contact.isFavorite}" class="uiIcon favorite" @click.stop="toggleFavorite(contact)"></div>
      </div>
    </div>
    <modal v-show="createRoomModal" title="Add new room" modal-class="create-room-modal" @modal-closed="createRoomModal = false">
      <div class="add-room-form">
        <label>Quel est le nom de votre salon?</label>
        <input type="text">
        <label>Ajouter des personnes à votre salon:</label>
        <input id="add-room-suggestor" type="text">
        
        <span class="team-add-user-label">Ex: "ro" or "Ro Be" pour trouver Robert Beranot</span>
      </div>
      <div class="uiAction uiActionBorder">
        <a href="#" class="btn btn-primary">Enregistrer</a>
        <a href="#" class="btn" @click="createRoomModal = false">Annuler</a>
      </div>
    </modal>
  </div>
</template>

<script>
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
      createRoomModal: false
    };
  },
  computed: {
    statusStyle: function() {
      return this.contactStatus === 'inline' ? 'user-available' : 'user-invisible';
    },
    filteredContacts: function() {
      if(this.typeFilter === 'All') {
        return this.contacts;
      } else {
        return this.contacts.filter(contact =>
          this.typeFilter === 'People' && contact.type === 'u'
          || this.typeFilter === 'Rooms' && contact.type === 't'
          || this.typeFilter === 'Spaces' && contact.type === 's'
          || this.typeFilter === 'Favorites' && contact.isFavorite
        );
      }
    }
  },
  created() {
    document.addEventListener('exo-chat-message-received', this.notificationCountUpdated);
  },
  destroyed() {
    document.removeEventListener('exo-chat-message-received', this.notificationCountUpdated);
  },
  methods: {
    selectContact(contact) {
      this.$emit('exo-chat-contact-selected', contact);
      //let room = this.contacts.findIndex(c => c.user == contact.user);
      //this.contacts[room].unreadTotal = 0;
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
      initSuggester('#add-room-suggestor');
    },
    notificationCountUpdated(event) {
      const room = event.detail.room;
      this.contacts.forEach(contact => {
        if (this.selected.room !== room && contact.room === room) {
          contact.unreadTotal ++;
        }
      });
    }
  }
};
</script>