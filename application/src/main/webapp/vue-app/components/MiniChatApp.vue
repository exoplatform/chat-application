<template>
  <div id="chatApplicationNotification" :class="connected ? 'online' : 'offline'">
    <div class="dropdown uiDropdownWithIcon status-dropdown pull-right">
      <a :class="statusClass" data-toggle="dropdown" class="dropdown-toggle" @click="loadMessages">
        <div class="uiIconStatus uiNotifChatIcon">
          <span :class="totalUnreadMsg > 0 ? '' : 'hidden'" class="notif-total badgeDefault badgePrimary mini">{{ totalUnreadMsg }}</span>
        </div>
      </a>
      <chat-notif-list ref="menuItems" :total-unread-msg="totalUnreadMsg" @set-status="setStatus($event)" @select-room="room = $event"></chat-notif-list>
    </div>
    <div style="display: none;">
      <audio id="chat-audio-notif" controls>
        <source src="/chat/audio/notif.wav">
        <source src="/chat/audio/notif.mp3">
        <source src="/chat/audio/notif.ogg">
      </audio>
    </div>
    <chat-room v-if="room" ref="chatRoom" :room="room" @close="room = null"></chat-room>
  </div>
</template>

<script>
import * as chatServices from '../chatServices';
import * as chatWebSocket from '../chatWebSocket';
import {initTiptip} from '../tiptip';
import {chatData} from '../chatData';

import MiniChatNotifList from './MiniChatNotifList.vue';
import ChatMessageList from './ChatMessageList.vue';
import MiniChatRoom from './MiniChatRoom.vue';

export default {
  components: {
    'chat-notif-list': MiniChatNotifList,
    'chat-message-list': ChatMessageList,
    'chat-room': MiniChatRoom
  },
  props: {
    statusMap: {
      type: Object,
      default: function() {
        return {
          available: this.$t('exoplatform.chat.available'),
          away: this.$t('exoplatform.chat.away'),
          donotdistrub: this.$t('exoplatform.chat.donotdisturb'),
          invisible: this.$t('exoplatform.chat.invisible'),
          offline: this.$t('exoplatform.chat.button.offline')
        };
      }
    }
  },
  data() {
    return {
      status: 'offline',
      room: null,
      userSettings: {},
      totalUnreadMsg: 0,
      connected: false
    };
  },
  computed: {
    statusClass() {
      if (!this.connected || this.status === 'invisible') {
        return 'user-offline';
      } else {
        return `user-${this.status}`;
      }
    }
  },
  created() {
    initTiptip();
    chatServices.initChatSettings(this.userSettings.username,
      userSettings => this.initSettings(userSettings),
      data => {
        const totalUnreadMsg = Math.abs(data.unreadOffline) + Math.abs(data.unreadOnline) + Math.abs(data.unreadSpaces) + Math.abs(data.unreadTeams);
        if(totalUnreadMsg >= 0) {
          this.totalUnreadMsg = totalUnreadMsg;
        }
      }
    );
    document.addEventListener('exo-chat-logout-sent', () => {
      if (!chatWebSocket.isConnected()) {
        this.changeUserStatusToOffline();
      }
    });
    document.addEventListener('exo-chat-room-open', (e) => {
      const roomName = e.detail ? e.detail.name : null;
      const roomType = e.detail ? e.detail.type : null;
      if(roomName && roomName.trim().length) {
        chatServices.getRoomId(this.userSettings, roomName, roomType).then(rommId => {
          this.room = rommId;
        });
      }
    });
    document.addEventListener('exo-chat-disconnected', this.changeUserStatusToOffline);
    document.addEventListener('exo-chat-connected', this.connectionEstablished);
    document.addEventListener('exo-chat-reconnected', this.connectionEstablished);
    document.addEventListener('exo-chat-user-status-changed', (e) => {
      const contactChanged = e.detail;
      if (this.userSettings.username === contactChanged.sender) {
        this.userSettings.status = contactChanged.status ? contactChanged.status : contactChanged.data ? contactChanged.data.status : null;
        this.userSettings.originalStatus = this.userSettings.status;
        this.status = this.userSettings.status;
      }
    });
    document.addEventListener('exo-chat-notification-count-updated', (e) => {
      const totalUnreadMsg = e.detail ? e.detail.data.totalUnreadMsg : e.totalUnreadMsg;
      if(totalUnreadMsg >= 0) {
        this.totalUnreadMsg = totalUnreadMsg;
      }
    });

    if (chatWebSocket.isConnected()) {
      this.connectionEstablished();
    } else {
      this.changeUserStatusToOffline();
    }
  },
  destroyed() {
    document.removeEventListener('exo-chat-disconnected', this.changeUserStatusToOffline);
    document.removeEventListener('exo-chat-connected', this.connectionEstablished);
    document.removeEventListener('exo-chat-reconnected', this.connectionEstablished);
    document.removeEventListener('exo-chat-room-updated', this.roomUpdated);
    // TODO remove added listeners
  },
  methods: {
    initSettings(userSettings) {
      this.userSettings = userSettings;
      // Trigger that the new status has been loaded
      this.setStatus(this.userSettings.status);
    },
    setStatus(status) {
      const thiss = this;
      if (chatWebSocket && chatWebSocket.isConnected()) {
        chatWebSocket.setStatus(status, newStatus => {
          this.userSettings.status = newStatus;
          this.status = newStatus;
        }, () => {
          setTimeout(() => thiss.setStatus(status), chatData.REATTEMPT_PERIOD);
        });
      } else {
        setTimeout(() => thiss.setStatus(status), chatData.REATTEMPT_PERIOD);
      }
    },
    connectionEstablished() {
      eXo.chat.isOnline = true;
      this.connected = true;
    },
    changeUserStatusToOffline() {
      eXo.chat.isOnline = false;
      this.connected = false;
    },
    loadMessages() {
      this.$refs.menuItems.refreshMessages();
    }
  }
};
</script>
