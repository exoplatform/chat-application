<template>
  <div class="contactListContainer">
    <div class="contactFilter">
      <i class="uiIconSearchLight"></i>
      <input type="text" placeholder="Filter people, spaces...">
    </div>
    <div class="listHeader">
      <div class="dropdown">
        <span class="dropdown-toggle" data-toggle="dropdown">
          
          <i class="uiIconArrowDownMini"></i>
        </span>
        <ul class="dropdown-menu pull-right">
          <li><a href="#">All</a></li>
          <li><a href="#">Online</a></li>
        </ul>
      </div>
    </div>
    <div class="contactList">
      <div v-for="contact in contacts" :key="contact.user" :class="{selected: selected.user == contact.user}" class="contact-list-item isList" @click="selectContact(contact)">
        <chat-contact :list="true" :type="contact.type" :avatar="getContactAvatar(contact.user)" :name="contact.fullName" :status="contact.status"></chat-contact>
        <div v-show="contact.unreadTotal > 0" class="unreadMessages">{{ contact.unreadTotal }}</div>
        <div :class="{'is-fav': contact.isFavorite}" class="uiIcon favorite" @click.stop="toggleFavorite(contact)"></div>
      </div>
    </div>
  </div>
</template>

<script>
import {chatData} from '../chatData';
import ChatContact from './ChatContact.vue';
export default {
  components: {ChatContact},
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
    return {};
  },
  computed: {
    statusStyle: function() {
      return (this.contactStatus == 'inline') ? 'user-available' : 'user-invisible';
    }
  },
  methods: {
    selectContact(contact) {
      this.$emit('exo-chat-contact-selected', contact);
      //let room = this.contacts.findIndex(c => c.user == contact.user);
      //this.contacts[room].unreadTotal = 0;
      contact.unreadTotal = 0;
    },
    getContactAvatar(user) {
      return chatData.socialUserAPI + user + '/avatar';
    },
    toggleFavorite(contact) {
      contact.isFavorite = !contact.isFavorite;
    }
  }
};
</script>