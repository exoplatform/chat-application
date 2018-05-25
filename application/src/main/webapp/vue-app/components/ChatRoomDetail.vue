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
        <dropdown-select v-if="displayMenu" class="room-settings-dropdown" position="right">
          <i slot="toggle" class="uiIconVerticalDots"></i>
          <li v-for="settingAction in settingActions" v-if="displayItem(settingAction)" slot="menu" :class="`room-setting-action-${settingAction.key}`" :key="settingAction.key" @click="executeAction(settingAction.key)">
            <a href="#">
              <i :class="settingAction.class" class="uiIconRoomSetting"></i>
              {{ $t(`chat.rooms.action.${settingAction.key}`) }}
            </a>
          </li>
        </dropdown-select>
      </div>
    </div>
    <room-notification-modal :room="contact.room" :room-name="contact.fullName" :show="openNotificationSettings" @modal-closed="closeNotificationSettingsModal"></room-notification-modal>
  </div>
</template>

<script>
import ChatContact from './ChatContact.vue';
import DropdownSelect from './DropdownSelect.vue';
import RoomNotificationModal from './modal/RoomNotificationModal.vue';
import * as chatServices from '../chatServices';
import * as chatWebStorage from '../chatWebStorage';

const DEFAULT_ROOM_ACTIONS = [{
  key: 'startMeeting',
  class: 'uiIconChatRecordStart',
  enabled: (comp) => {
    return !comp.meetingStarted;
  }
}, {
  key: 'stopMeeting',
  class: 'uiIconChatRecordStop',
  enabled: (comp) => {
    return comp.meetingStarted;
  }
} , {
  key: 'notificationSettings',
  class: 'uiIconPLFNotifications'
} , {
  key: 'editRoom',
  type: 't',
  class: 'uiIconEditInfo',
  enabled: (comp) => {
    return comp.isAdmin;
  }
} , {
  key: 'deleteRoom',
  type: 't',
  class: 'uiIconDelete',
  enabled: (comp) => {
    return comp.isAdmin;
  }
} , {
  key: 'leaveRoom',
  type: 't',
  class: 'uiIconDelete',
  enabled: (comp) => {
    return !comp.isAdmin;
  }
}];

export default {
  components: {
    'chat-contact': ChatContact,
    'dropdown-select': DropdownSelect,
    'room-notification-modal': RoomNotificationModal
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
      searchText: '',
      meetingStarted: false,
      openNotificationSettings: false
    };
  },
  computed: {
    getMembersNumber() {
      return /*(this.contact.type != 'u') ? this.contact.participants.length :*/ 0;
    },
    settingActions() {
      if(eXo && eXo.chat && eXo.chat.room && eXo.chat.room.extraActions) {
        return DEFAULT_ROOM_ACTIONS.concat(eXo.chat.room.extraActions);
      } else {
        return DEFAULT_ROOM_ACTIONS;
      }
    },
    isAdmin() {
      return this.contact.admins && this.contact.admins.indexOf(eXo.chat.userSettings.username) >= 0;
    },
    displayMenu() {
      return this.contact.type === 's' || this.contact.type === 't';
    }
  },
  watch: {
    searchText(value) {
      document.dispatchEvent(new CustomEvent('exo-chat-message-search', {detail: value}));
    }
  },
  created() {
    document.addEventListener('exo-chat-setting-startMeeting', this.startMeeting);
    document.addEventListener('exo-chat-setting-stopMeeting', this.stopMeeting);
    document.addEventListener('exo-chat-setting-notificationSettings', this.openNotificationSettingsModal);
    this.meetingStarted = chatWebStorage.getStoredParam(`meetingStarted-${this.contact.room}`);
  },
  updated() {
    this.meetingStarted = chatWebStorage.getStoredParam(`meetingStarted-${this.contact.room}`);
  },
  destroyed() {
    document.removeEventListener('exo-chat-setting-startMeeting', this.startMeeting);
    document.removeEventListener('exo-chat-setting-stopMeeting', this.stopMeeting);
    document.removeEventListener('exo-chat-setting-notificationSettings', this.openNotificationSettingsModal);
  },
  methods: {
    toggleFavorite(contact) {
      chatServices.toggleFavorite(contact.room, !contact.isFavorite).then(contact.isFavorite = !contact.isFavorite);
    },
    openSearchRoom() {
      this.showSearchRoom = true;
      this.$nextTick(() => this.$refs.searchRoom.focus());
    },
    closeSearchRoom(e) {
      if (e.type === 'blur' && this.searchText !== '') {
        return;
      }
      this.showSearchRoom = false;
      this.searchText = '';
    },
    openNotificationSettingsModal() {
      this.openNotificationSettings = true;
    },
    closeNotificationSettingsModal() {
      this.openNotificationSettings = false;
    },
    executeAction(actionName) {
      document.dispatchEvent(new CustomEvent(`exo-chat-setting-${actionName}`, {'detail': this.contact}));
    },
    startMeeting() {
      const room = this.contact.room;
      chatWebStorage.setStoredParam(`meetingStarted-${room}`, new Date().getTime().toString());
      this.meetingStarted = true;
      this.sendMeetingMessage(true);
    },
    stopMeeting() {
      const room = this.contact.room;
      const fromTimestamp = chatWebStorage.getStoredParam(`meetingStarted-${room}`);
      if (fromTimestamp) {
        chatWebStorage.setStoredParam(`meetingStarted-${room}`, '');
        this.meetingStarted = false;
        this.sendMeetingMessage(false, fromTimestamp);
      }
    },
    sendMeetingMessage(startMeeting, fromTimestamp) {
      const msgType = startMeeting ? 'type-meeting-start' : 'type-meeting-stop';
      const message = {
        message : this.newMessage,
        room : this.contact.room,
        clientId: new Date().getTime().toString(),
        timestamp: Date.now(),
        user: eXo.chat.userSettings.username,
        isSystem: true,
        options: {
          type: msgType,
          fromUser: eXo.chat.userSettings.username,
          fromFullname: eXo.chat.userSettings.fullName,
          fromTimestamp: fromTimestamp
        }
      };
      document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : message}));
    },
    displayItem(settingAction) {
      return (!settingAction.enabled || settingAction.enabled(this)) && (!settingAction.type || settingAction.type === this.contact.type);
    }
  }
};
</script>