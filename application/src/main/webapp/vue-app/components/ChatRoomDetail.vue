<template>
  <div class="room-detail">
    <chat-contact :type="contact.type" :user-name="contact.user" :name="contact.fullName" :status="contact.status" :nb-members="getMembersNumber">
      <div :class="{'is-fav': contact.isFavorite}" class="uiIcon favorite" @click.stop="toggleFavorite(contact)"></div>
    </chat-contact>
    <div :class="{'search-active': showSearchRoom}" class="room-actions-container">
      <div class="room-search">
        <input ref="searchRoom" v-model="searchText" type="text" placeholder="search here" @blur="closeSearchRoom" @keyup.esc="closeSearchRoom">
        <i class="uiIconCloseLight" @click="closeSearchRoom"></i>
      </div>
      <div class="room-action-menu">
        <div class="room-search-btn" @click="openSearchRoom">
          <i class="uiIconSearchLight"></i>    
        </div>
        <div class="room-settings-btn dropdown">
          <i class="uiIconVerticalDots dropdown-toggle"></i>
          <ul class="dropdown-menu pull-right">
            <li><a href="#">Start meeting</a></li>
            <li><a href="#">Notification settings</a></li>
            <li><a href="#">Edit</a></li>
            <li><a href="#">Delete</a></li>
          </ul>
        </div>
      </div>
      
    </div>
  </div>
</template>

<script>
import ChatContact from './ChatContact.vue';
export default {
  components: {
    'chat-contact': ChatContact
  },
  props: {
    contact: {
      type: Object,
      default: function () {
        return {};
      }
    }
  },
  data() {
    return {
      showSearchRoom: false,
      searchText: ''
    };
  },
  computed: {
    getMembersNumber() {
      return /*(this.contact.type != 'u') ? this.contact.participants.length :*/ 0;
    }
  },
  updated() {
  },
  methods: {
    toggleFavorite(contact) {
      contact.isFavorite = !contact.isFavorite;
    },
    openSearchRoom() {
      this.showSearchRoom = true;
      this.$nextTick(() => this.$refs.searchRoom.focus());
    },
    closeSearchRoom(e) {
      if (e.type == 'blur' && this.searchText != '') {
        return;
      }
      this.showSearchRoom = false;
      this.searchText = '';
    }
  }
};
</script>