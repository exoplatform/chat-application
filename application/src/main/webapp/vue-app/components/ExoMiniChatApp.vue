<template>
  <div id="chatApplicationNotification" :class="connected ? 'online' : 'offline'">
    <div class="dropdown uiDropdownWithIcon status-dropdown pull-right">
      <a :class="statusClass" data-toggle="dropdown" class="dropdown-toggle" @click="loadMessages">
        <div class="uiIconStatus uiNotifChatIcon">
          <span :class="canShowOnSiteNotif() && totalUnreadMsg > 0 ? '' : 'hidden'" class="notif-total badgeDefault badgePrimary mini">{{ totalUnreadMsg }}</span>
        </div>
      </a>
      <exo-chat-notif-list ref="menuItems" :onsite-notif="canShowOnSiteNotif()" :total-unread-msg="totalUnreadMsg" @set-status="setStatus($event)" @select-room="room = $event"></exo-chat-notif-list>
    </div>
    <div style="display: none;">
      <audio id="chat-audio-notif" controls>
        <source src="/chat/audio/notif.wav">
        <source src="/chat/audio/notif.mp3">
        <source src="/chat/audio/notif.ogg">
      </audio>
    </div>
    <exo-chat-room v-if="room" ref="chatRoom" :room="room" @close="close()"></exo-chat-room>
  </div>
</template>

<script>
import {chatConstants} from '../chatConstants';
import * as chatServices from '../chatServices';
import * as chatWebSocket from '../chatWebSocket';
import * as desktopNotification from '../desktopNotification';

export default {
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
    chatServices.initChatSettings(this.userSettings.username, true,
      userSettings => this.initSettings(userSettings),
      data => {
        const totalUnreadMsg = Math.abs(data.total);
        if(totalUnreadMsg >= 0) {
          this.totalUnreadMsg = totalUnreadMsg;
        }
        this.room = localStorage.getItem(`${chatConstants.STORED_PARAM_OPENED_MINI_CHAT_ROOM}-${this.userSettings.username}`);
      }
    );

    document.addEventListener(this.$constants.EVENT_DISCONNECTED, this.changeUserStatusToOffline);
    document.addEventListener(this.$constants.EVENT_CONNECTED, this.connectionEstablished);
    document.addEventListener(this.$constants.EVENT_RECONNECTED, this.connectionEstablished);
    document.addEventListener(this.$constants.EVENT_LOGGED_OUT, this.userLoggedout);
    document.addEventListener(this.$constants.EVENT_USER_STATUS_CHANGED, this.userStatusChanged);
    document.addEventListener(this.$constants.EVENT_GLOBAL_UNREAD_COUNT_UPDATED, this.totalUnreadMessagesUpdated);
    document.addEventListener(this.$constants.ACTION_ROOM_OPEN_CHAT, this.openRoomInMiniChat);

    if (chatWebSocket.isConnected()) {
      this.connectionEstablished();
    } else {
      this.changeUserStatusToOffline();
    }
  },
  destroyed() {
    document.removeEventListener(this.$constants.EVENT_DISCONNECTED, this.changeUserStatusToOffline);
    document.removeEventListener(this.$constants.EVENT_CONNECTED, this.connectionEstablished);
    document.removeEventListener(this.$constants.EVENT_RECONNECTED, this.connectionEstablished);
    document.removeEventListener(this.$constants.EVENT_LOGGED_OUT, this.userLoggedout);
    document.removeEventListener(this.$constants.EVENT_USER_STATUS_CHANGED, this.userStatusChanged);
    document.removeEventListener(this.$constants.EVENT_GLOBAL_UNREAD_COUNT_UPDATED, this.totalUnreadMessagesUpdated);
    document.removeEventListener(this.$constants.ACTION_ROOM_OPEN_CHAT, this.openRoomInMiniChat);
  },
  methods: {
    initSettings(userSettings) {
      this.userSettings = userSettings;
      // Trigger that the new status has been loaded
      this.setStatus(this.userSettings.status);

      this.userSettings.statusLabels = {
        available: this.$t('exoplatform.chat.available'),
        away: this.$t('exoplatform.chat.away'),
        donotdisturb: this.$t('exoplatform.chat.donotdisturb'),
        invisible: this.$t('exoplatform.chat.button.offline'),
        offline: this.$t('exoplatform.chat.button.offline')
      };
    },
    userLoggedout() {
      if (!chatWebSocket.isConnected()) {
        this.changeUserStatusToOffline();
      }
    },
    openRoomInMiniChat(e) {
      const roomName = e.detail ? e.detail.name : null;
      const roomType = e.detail ? e.detail.type : null;
      if(roomName && roomName.trim().length) {
        chatServices.getRoomId(this.userSettings, roomName, roomType).then(rommId => {
          this.room = rommId;
        });
      }
      const tiptip = document.getElementById('tiptip_holder');
      if (tiptip) {
        tiptip.style.display = 'none';
      }
    },
    userStatusChanged(e) {
      const contactChanged = e.detail;
      if (this.userSettings.username === contactChanged.sender) {
        this.userSettings.status = contactChanged.status ? contactChanged.status : contactChanged.data ? contactChanged.data.status : null;
        this.userSettings.originalStatus = this.userSettings.status;
        this.status = this.userSettings.status;
      }
    },
    totalUnreadMessagesUpdated(e) {
      const totalUnreadMsg = e.detail ? e.detail.data.totalUnreadMsg : e.totalUnreadMsg;
      if(totalUnreadMsg >= 0) {
        this.totalUnreadMsg = totalUnreadMsg;
      }
    },
    setStatus(status) {
      chatServices.setUserStatus(this.userSettings, status, newStatus => {
        this.userSettings.status = newStatus;
        this.status = newStatus;
      });
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
    },
    canShowOnSiteNotif() {
      return desktopNotification.canShowOnSiteNotif();
    },
    close() {
      this.room = null;
      localStorage.removeItem(`${chatConstants.STORED_PARAM_OPENED_MINI_CHAT_ROOM}-${this.userSettings.username}`);
    }
  }
};
</script>
