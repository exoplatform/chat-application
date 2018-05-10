<template>
  <div class="room-detail">
    <chat-contact :type="room.type" :avatar="room.avatar" :name="room.fullName" :status="room.status" :nbMembers="getMembersNumber">
      <div class="uiIcon favorite" :class="{'is-fav': room.isFavorite}" @click.stop="toggleFavorite(room)"></div>
    </chat-contact>
    <div class="room-actions-container" :class="{'search-active': showSearchRoom}">
      <div class="room-search">
        <input type="text" placeholder="search here" ref="searchRoom" v-model="searchText" @blur="closeSearchRoom" @keyup.esc="closeSearchRoom">
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
import ChatContact from './ChatContact.vue'
export default {
  components: {
    "chat-contact": ChatContact
  },
  props: ['room'],
  data() {
    return {
      showSearchRoom: false,
      searchText: ''
    }
  },
  computed: {
    getMembersNumber() {
      return (this.room.type != "u") ? this.room.participants.length : false
    }
  },
  methods: {
    toggleFavorite(room) {
      room.isFavorite = !room.isFavorite;
    },
    openSearchRoom(e) {
      this.showSearchRoom = true;
      this.$nextTick(() => this.$refs.searchRoom.focus())
    },
    closeSearchRoom(e) {
      if (e.type == 'blur' && this.searchText != '') {
        return
      }
      this.showSearchRoom = false;
      this.searchText = '';
    }
  }
}
</script>