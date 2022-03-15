<template>
  <div class="chat-contact">
    <div
      :style="`backgroundImage: url(${avatarUrl})`"
      :class="statusStyle"
      class="chat-contact-avatar">
      <a
        v-if="!list && type!=='t' && (isEnabled || isEnabled === null)"
        :href="contactUrl"
        class="chat-contact-link"></a>
      <i v-if="list && type=='u' && (isEnabled || isEnabled === null)" class="uiIconStatus"></i>
    </div>
    <div class="contactDetail">
      <div :class="isActive" class="contactLabel">
        <span v-sanitized-html="escapedName"></span>
        <span v-if="isExternal" class="externalTagClass">{{ externalTag }}</span>
        <slot></slot>
      </div>
      <div
        v-if="type =='u' && !list && !isCurrentUser"
        :class="statusStyle"
        class="user-status">
        <i v-if="isEnabled || isEnabled === null" class="uiIconStatus"></i>
        <span class="user-status">{{ getStatus }}</span>
      </div>
      <exo-dropdown-select
        v-if="type =='u' && !list && isCurrentUser"
        toggler-class="user-status"
        class="status-dropdown">
        <div slot="toggle" :class="statusStyle">
          <div class="statusMargin">
            <i class="uiIconStatus"></i>
          </div>
          <div v-if="!ap" class="chat-title">Chat</div>
          <span v-if="ap" class="user-status">{{ getStatus }}</span>
          <div v-if="chatDrawerContact" class="chat-status-toggle">
            <a v-if="!list && type!=='t' && (isEnabled || isEnabled === null)" class="chat-contact-link"></a>
          </div>
        </div>
        <li
          v-for="(value, key) in activeStatusMap"
          slot="menu"
          :class="`user-${key}`"
          :key="key"
          @click="setStatus(key)">
          <a href="#" class="status-link"><span><i class="uiIconStatus"></i></span>{{ value }}</a>
        </li>
      </exo-dropdown-select>
      <div v-if="type !='u' && !list && nbMembers > 0" class="room-number-members">
        {{ nbMembers }} {{ $t('exoplatform.chat.members') }}
      </div>
      <div
        v-sanitized-html="lastMessage"
        v-if="mq === 'mobile' && list && lastMessage || chatDrawerContact"
        :class="chatDrawerContact ? 'lastMessageDrawer last-message' : 'last-message' "></div>
    </div>
    <div
      v-if="!isRoomSilent && unreadTotal > 0 && unreadTotal <= maxShowUnread"
      :class="['unreadMessages', {'mt-10':mq==='mobile'}]">
      {{ unreadTotal }}
    </div>
    <div
      v-if="!isRoomSilent && unreadTotal > maxShowUnread"
      :class="['unreadMessages', 'maxUnread', {'mt-10':mq==='mobile'}]">
      +{{ maxShowUnread }}
    </div>
    <div>
      <v-icon
        color="red darken-2"
        dense
        class="ml-2 mb-1"
        v-if="isRoomSilent">
        mdi-volume-off
      </v-icon>
    </div>
  </div>
</template>

<script>
import {
  getUserProfileLink,
  getSpaceProfileLink,
  escapeHtml,
  getSpaceAvatar,
  getUserAvatar
} from '../chatServices';
import {chatConstants} from '../chatConstants';

export default {
  props: {
    /** Contact pretty name */
    prettyName: {
      type: String,
      default: ''
    },
    /** Group ID in case of a space */
    groupId: {
      type: String,
      default: ''
    },
    /** Contact display name */
    name: {
      type: String,
      default: ''
    },
    /** Contact id */
    userName: {
      type: String,
      default: ''
    },
    /** Contact status: offline, invisible, available, away, donotdisturb*/
    status: {
      type: String,
      default: ''
    },
    /** For list contact or simple contact */
    list: {
      type: Boolean,
      default: false
    },
    /** For contact status: (Disabled or Deleted) */
    isEnabled: {
      type: Boolean,
      default: true
    },
    isExternal: {
      type: Boolean,
      default: false
    },
    /** Contact type
     * u: user
     * t: room
     * s: space
     */
    type: {
      type: String,
      default: '',
      required: true
    },
    /**
     * Number of room or space members
     */
    nbMembers: {
      type: Number,
      default: 0
    },
    /** the Current user have status dropdown */
    isCurrentUser: {
      type: Boolean,
      default: false
    },
    /** last reveiced message for chat Contact used only on mobile display */
    lastMessage: {
      type: String,
      default: ''
    },
    chatDrawerContact: {
      type: Boolean,
      default: false
    },
    contactRoomId: {
      type: String,
      default: null
    },
    unreadTotal: {
      type: Number,
      default: null
    },
    isRoomSilent: {
      type: Boolean,
      default: false
    },
  },
  data: function() {
    return {
      isOnline: true,
      statusMap: {
        available: this.$t('exoplatform.chat.available'),
        away: this.$t('exoplatform.chat.away'),
        inactive: this.$t('exoplatform.chat.inactive'),
        donotdisturb: this.$t('exoplatform.chat.donotdisturb'),
        invisible: this.$t('exoplatform.chat.invisible'),
        offline: this.$t('exoplatform.chat.button.offline')
      },
      activeStatusMap: {
        available: this.$t('exoplatform.chat.available'),
        away: this.$t('exoplatform.chat.away'),
        donotdisturb: this.$t('exoplatform.chat.donotdisturb'),
        invisible: this.$t('exoplatform.chat.invisible'),
      },
      maxShowUnread: 99
    };
  },
  computed: {
    statusStyle: function() {
      if (!this.isOnline || this.status === 'invisible' && !this.isCurrentUser) {
        return 'user-offline';
      } else {
        return `user-${this.status}`;
      }
    },
    getStatus() {
      if (!this.isEnabled) {
        return this.statusMap.inactive;
      }
      if (!this.isOnline || this.status === 'invisible' && !this.isCurrentUser) {
        return this.statusMap.offline;
      } else {
        return this.statusMap[this.status];
      }
    },
    escapedName() {
      const name = escapeHtml(this.name);
      if (!this.isEnabled && this.list === true) {
        return name.concat(' ').concat('(').concat(this.statusMap.inactive).concat(')');
      } else {
        return name;
      }
    },
    externalTag() {
      return  `(${this.$t('exoplatform.chat.external')})`;
    },
    isActive() {
      return this.type === 'u' && !this.isEnabled ? 'inactive' : 'active';
    },
    contactUrl() {
      if (this.type === 'u') {
        return getUserProfileLink(this.userName);
      } else if (this.type === 's' && this.groupId) {
        return getSpaceProfileLink(this.groupId, this.prettyName);
      }
      return '#';
    },
    avatarUrl() {
      if (this.type === 'u') {
        return  getUserAvatar(this.userName);
      } else if (this.type === 's') {
        return getSpaceAvatar(this.prettyName);
      } else {
        return chatConstants.DEFAULT_ROOM_AVATAR;
      }
    }
  },
  created() {
    document.addEventListener(chatConstants.EVENT_DISCONNECTED, this.setOffline);
    document.addEventListener(chatConstants.EVENT_CONNECTED, this.setOnline);
    document.addEventListener(chatConstants.EVENT_RECONNECTED, this.setOnline);
  },
  destroyed() {
    document.removeEventListener(chatConstants.EVENT_DISCONNECTED, this.setOffline);
    document.removeEventListener(chatConstants.EVENT_CONNECTED, this.setOnline);
    document.removeEventListener(chatConstants.EVENT_RECONNECTED, this.setOnline);
  },
  methods: {
    setStatus(status) {
      this.$emit('status-changed', status);
    },
    setOnline() {
      this.isOnline = true;
    },
    setOffline() {
      this.isOnline = false;
    }
  }
};
</script>
