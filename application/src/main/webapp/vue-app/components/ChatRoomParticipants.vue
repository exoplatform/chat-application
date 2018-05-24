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
          <li v-for="filter in filterByStatus" slot="menu" :key="filter" @click="selectParticipantFilter(filter)"><a href="#">{{ filter }}</a></li>
        </dropdown-select>
      </div>
      <div class="room-participants-list isList">
        <chat-contact v-for="contact in filteredParticipant" :key="contact.name" :list="true" :user-name="contact.name" :name="contact.fullname" :status="contact.status" type="u"></chat-contact>
      </div>
    </div>
  </div>
</template>

<script>
import ChatContact from './ChatContact.vue';
import DropdownSelect from './DropdownSelect.vue';
import * as chatServices from '../chatServices';
import * as chatWebStorage from '../chatWebStorage';

const STATUS_FILTER_PARAM = 'exo.chat.room.participant.filter';
const STATUS_FILTER_DEFAULT = 'All';

export default {
  components: {ChatContact, DropdownSelect},
  data : function() {
    return {
      isCollapsed: true,
      filterByStatus: ['All', 'Online'],
      participantFilter: STATUS_FILTER_DEFAULT,
      contact: null,
      participants: []
    };
  },
  computed: {
    filteredParticipant() {
      return this.participants.filter(participant => {
        return this.participantFilter === 'All' ||  ['available','busy','absent'].indexOf(participant.status) > -1;
      });
    }
  },
  created() {
    document.addEventListener('exo-chat-selected-contact-changed', this.contactChanged);
    document.addEventListener('exo-chat-user-status-changed', this.contactStatusChanged);
    this.participantFilter = chatWebStorage.getStoredParam(STATUS_FILTER_PARAM, STATUS_FILTER_DEFAULT);
  },
  destroyed() {
    document.removeEventListener('exo-chat-selected-contact-changed', this.contactChanged);
    document.removeEventListener('exo-chat-user-status-changed', this.contactStatusChanged);
  },
  methods: {
    toggleCollapsed() {
      this.isCollapsed = !this.isCollapsed;
    },
    selectParticipantFilter(filter) {
      chatWebStorage.setStoredParam(STATUS_FILTER_PARAM, filter);
      this.participantFilter = filter;
    },
    contactChanged(e) {
      const contact = e.detail;
      this.contact = contact;
      this.participants = [];
      if (contact.type && contact.type !== 'u') {
        chatServices.getOnlineUsers().then(users => {
          chatServices.getRoomParticipants(eXo.chat.userSettings, contact).then( data => {
            this.$emit('exo-chat-particpants-loaded', data.users);
            this.participants = data.users.reduce((prev, curr) => {
              // if user is not online, set its status as offline
              if(users.indexOf(curr.name) < 0) {
                curr.status = 'offline';
              }
              return curr.name === eXo.chat.userSettings.username ? prev: [...prev, curr];
            }, []);
          });
        });
      }
    },
    contactStatusChanged(e) {
      const contact = e.detail;
      const participantToChange = this.participants.find(participant => participant.name === contact.sender);
      if (participantToChange) {
        participantToChange.status = contact.data.status;
      }
    }
  }
};
</script>
