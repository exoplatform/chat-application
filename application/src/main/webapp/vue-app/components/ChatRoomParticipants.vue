<template>
  <div :class="{collapsed: isCollapsed}" class="room-participants">
    <div class="room-users-collapse-btn" @click="toggleCollapsed">
      <i class="uiIcon"></i>
    </div>
    <div class="room-participants-header no-user-selection">
      <div v-show="!isCollapsed" class="room-participants-title">
        {{ $t("chat.rooms.participants") }}
        <span v-show="participants.length > 0" class="nb-participants">({{ participants.length }})</span>
      </div>
      <div class="room-participants-filter">
        <div v-show="isCollapsed" class="actionIcon">
          <i class="uiIconChatMember uiIconChatLightGray"></i>
        </div>
        <div v-show="!isCollapsed" class="dropdown dropdown-filter">
          <span class="dropdown-toggle" data-toggle="dropdown">
            {{ filter }}
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
      <chat-contact v-for="contact in participants" :key="contact.name" :list="true" :user-name="contact.name" :name="contact.fullname" :status="contact.status" type="u"></chat-contact>
    </div>
  </div>
</template>

<script>
import {chatData} from '../chatData';
import ChatContact from './ChatContact.vue';
export default {
  components: {ChatContact},
  props: {
    participants: {
      type: Array,
      default: function() { return [];}
    }
  },
  data : function() {
    return {
      isCollapsed: true,
      filter: 'All'
    };
  },
  methods: {
    toggleCollapsed() {
      this.isCollapsed = !this.isCollapsed;
    },
    getContactAvatar(user) {
      return `${chatData.socialUserAPI}${user}/avatar`;
    }
  }
};
</script>
