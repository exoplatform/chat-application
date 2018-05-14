<template>
  <div class="room-participants" :class="{collapsed: isCollapsed}">
    <div class="room-users-collapse-btn" @click="toggleCollapsed">
      <i class="uiIcon"></i>
    </div>
    <div class="room-participants-header no-user-selection">
      <div class="room-participants-title" v-show="!isCollapsed">
        {{ $t("chat.rooms.participants") }}
        <span class="nb-participants" v-show="participants.length > 0">({{participants.length}})</span>
      </div>
      <div class="room-participants-filter">
        <div class="actionIcon" v-show="isCollapsed">
          <i class="uiIconChatMember uiIconChatLightGray"></i>
        </div>
        <div class="dropdown dropdown-filter" v-show="!isCollapsed">
          <span class="dropdown-toggle" data-toggle="dropdown">
            {{filter}}
            <i class="uiIconArrowDownMini"></i>
          </span>
          <ul class="dropdown-menu pull-right">
            <li><a href="#">All</a></li>
            <li><a href="#">Online</a></li>
          </ul>
        </div>
      </div>
    </div>
    <div class="room-participants-list isList">
      <chat-contact v-for="contact in participants" :key="contact.user" :list="true" :type="contact.type" :avatar="getContactAvatar(contact.user)" :name="contact.fullName" :status="contact.status"></chat-contact>
    </div>
  </div>
</template>

<script>
import {chatData} from '../chatData'
import ChatContact from './ChatContact.vue'
export default {
  components: {ChatContact},
  props: ['participants'],
  data : function() {
    return {
      isCollapsed: true,
      filter: "All"
    }
  },
  methods: {
    toggleCollapsed() {
      this.isCollapsed = !this.isCollapsed;
    },
    getContactAvatar(user) {
      return chatData.socialUserAPI + user + '/avatar'
    }
  },
  mounted() {
    console.log('mounted', this.participants);
  }
}
</script>
