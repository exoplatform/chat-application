<template>
  <div v-if="contact && Object.keys(contact).length !== 0 && contact.type != 'u'" class="uiRoomUsersContainerArea">
    <div :class="{collapsed: isCollapsed}" class="room-participants">
      <div class="room-users-collapse-btn" @click="toggleCollapsed">
        <i class="uiIcon"></i>
      </div>
      <div class="room-participants-header no-user-selection">
        <div v-show="!isCollapsed" class="room-participants-title">
          {{ $t("chat.rooms.participants") }}
          <span v-show="participants.length > 0" class="nb-participants">({{ participants.length }})</span>
        </div>
        <dropdown-select v-show="!isCollapsed" position="right">
          <span slot="toggle">{{ participantFilter }}</span>
          <i slot="toggle" class="uiIconArrowDownMini"></i>
          <li v-for="filter in filterByStatus" slot="menu" :key="filter" @click="selectparticipantFilter(filter)"><a href="#">{{ filter }}</a></li>
        </dropdown-select>
      </div>
      <div class="room-participants-list isList">
        <chat-contact v-for="contact in participants" :key="contact.name" :list="true" :user-name="contact.name" :name="contact.fullname" :status="contact.status" type="u"></chat-contact>
      </div>
    </div>
  </div>
</template>

<script>
import ChatContact from './ChatContact.vue';
import DropdownSelect from './DropdownSelect.vue';
import * as chatServices from '../chatServices';

export default {
  components: {ChatContact, DropdownSelect},
  props: {
    userSettings: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data : function() {
    return {
      isCollapsed: true,
      filterByStatus: ['All', 'Online'],
      participantFilter: 'All',
      contact: null,
      participants: []
    };
  },
  created() {
    document.addEventListener('exo-chat-selected-contact-changed', this.contactChanged);
    document.addEventListener('exo-chat-status-changed', this.contactStatusChanged);
  },
  destroyed() {
    document.removeEventListener('exo-chat-selected-contact-changed', this.contactChanged);
    document.removeEventListener('exo-chat-status-changed', this.contactStatusChanged);
  },
  methods: {
    toggleCollapsed() {
      this.isCollapsed = !this.isCollapsed;
    },
    selectparticipantFilter(filter) {
      this.participantFilter = filter;
    },
    contactChanged(e) {
      const contact = e.detail;
      this.contact = contact;
      if (contact.type === 'u') {
        this.participants = [];
      } else {
        chatServices.getRoomParticipants(this.userSettings, contact).then( data => {
          this.participants = data.users;
        });
      }
    },
    contactStatusChanged(e) {
      const contact = e.detail;
      this.participants.forEach((participant) => {
        if(participant.name === contact.name) {
          participant.status = contact.status;
        }
      });
    }
  }
};
</script>
