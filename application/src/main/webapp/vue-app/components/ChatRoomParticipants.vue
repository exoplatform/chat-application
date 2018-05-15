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
        <dropdown-select v-show="!isCollapsed" position="right">
          <span slot="toggle">{{ participantFilter }}</span>
          <i slot="toggle" class="uiIconArrowDownMini"></i>
          <li v-for="filter in filterByStatus" slot="menu" :key="filter" @click="selectparticipantFilter(filter)"><a href="#">{{ filter }}</a></li>
        </dropdown-select>
      </div>
    </div>
    <div class="room-participants-list isList">
      <chat-contact v-for="contact in participants" :key="contact.name" :list="true" :user-name="contact.name" :name="contact.fullname" :status="contact.status" type="u"></chat-contact>
    </div>
  </div>
</template>

<script>
import ChatContact from './ChatContact.vue';
import DropdownSelect from './DropdownSelect.vue';
export default {
  components: {ChatContact, DropdownSelect},
  props: {
    participants: {
      type: Array,
      default: function() { return [];}
    }
  },
  data : function() {
    return {
      isCollapsed: true,
      filterByStatus: ['All', 'Online'],
      participantFilter: 'All'
    };
  },
  methods: {
    toggleCollapsed() {
      this.isCollapsed = !this.isCollapsed;
    },
    selectparticipantFilter(filter) {
      this.participantFilter = filter;
    }
  }
};
</script>
