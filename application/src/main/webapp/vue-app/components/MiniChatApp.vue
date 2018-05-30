<template>
  <div id="chatApplicationNotification" :class="connected ? 'online' : 'offline'">
    <div class="dropdown uiDropdownWithIcon status-dropdown pull-right">
      <a :class="statusClass" data-toggle="dropdown" class="dropdown-toggle">
        <div class="uiIconStatus uiNotifChatIcon">
          <span :class="totalUnreadMsg > 0 ? '' : 'hidden'" class="notif-total badgeDefault badgePrimary mini">{{ totalUnreadMsg }}</span>
        </div>
      </a>
      <ul class="dropdown-menu pull-right">
        <li v-for="(value, key) in statusMap" v-if="key !== 'offline'" slot="menu" :class="`user-${key}`" :key="key" @click="setStatus(key)">
          <a href="#">
            <span>
              <i class="uiIconStatus"></i>
            </span>
            {{ value }}
          </a>
        </li>
        <li slot="menu" class="divider">&nbsp;</li>
        <li slot="menu">
          <a href="/portal/intranet/chat" class="notif-chat-open-link" target="_chat">
            <div class="chat-status-open chat-status-icon"></div
            ><span class="chat-label-status">{{ $t('exoplatform.chat.open.chat') }}</span>
          </a>
        </li>
      </ul>
    </div>
    <div style="display: none;">
      <audio id="chat-audio-notif" controls>
        <source src="/chat/audio/notif.wav">
        <source src="/chat/audio/notif.mp3">
        <source src="/chat/audio/notif.ogg">
      </audio>
    </div>
  </div>
</template>

<script>
import * as chatServices from '../chatServices';
import * as chatWebSocket from '../chatWebSocket';

import ChatMessageList from './ChatMessageList.vue';

const REATTEMPT_PERIOD = 1000;

export default {
  components: {
    'chat-message-list': ChatMessageList
  },
  data() {
    return {
      messagesList: [],
      status: 'offline',
      statusMap : {
        available: this.$t('exoplatform.chat.available'),
        away: this.$t('exoplatform.chat.away'),
        donotdistrub: this.$t('exoplatform.chat.donotdisturb'),
        invisible: this.$t('exoplatform.chat.invisible'),
        offline: this.$t('exoplatform.chat.button.offline')
      },
      userSettings: {
        username: typeof eXo !== 'undefined' ? eXo.env.portal.userName : 'root',
        token: null,
        fullName: null,
        status: null,
        isOnline: false,
        cometdToken: null,
        dbName: null,
        sessionId: null,
        serverURL: null,
        standalone: false,
        chatPage: null,
        wsEndpoint: null,
      },
      totalUnreadMsg: 0,
      connected: true
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
    chatServices.initChatSettings(this.userSettings.username,
      userSettings => this.initSettings(userSettings),
      data => {
        const totalUnreadMsg = Math.abs(data.unreadOffline) + Math.abs(data.unreadOnline) + Math.abs(data.unreadSpaces) + Math.abs(data.unreadTeams);
        if(totalUnreadMsg >= 0) {
          this.totalUnreadMsg = totalUnreadMsg;
        }
      });
    document.addEventListener('exo-chat-logout-sent', () => {
      if (!chatWebSocket.isConnected()) {
        this.changeUserStatusToOffline();
      }
    });
    document.addEventListener('exo-chat-disconnected', this.changeUserStatusToOffline);
    document.addEventListener('exo-chat-connected', this.connectionEstablished);
    document.addEventListener('exo-chat-reconnected', this.connectionEstablished);
    document.addEventListener('exo-chat-user-status-changed', (e) => {
      const contactChanged = e.detail;
      if (this.userSettings.username === contactChanged.name) {
        this.userSettings.status = contactChanged.status;
        this.status = contactChanged.status;
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
          setTimeout(() => thiss.setStatus(status), REATTEMPT_PERIOD);
        });
      } else {
        setTimeout(() => thiss.setStatus(status), REATTEMPT_PERIOD);
      }
    },
    connectionEstablished() {
      eXo.chat.isOnline = true;
      this.connected = true;
    },
    changeUserStatusToOffline() {
      eXo.chat.isOnline = false;
      this.connected = false;
    }
  }
};
</script>
