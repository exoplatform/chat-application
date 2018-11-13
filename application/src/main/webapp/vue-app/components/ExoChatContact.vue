<template>
  <div class="chat-contact">
    <div :style="`backgroundImage: url(${contactAvatar})`" :class="statusStyle" class="chat-contact-avatar">
      <a v-if="!list && type!=='t'" :href="getProfileLink()" class="chat-contact-link"></a>
      <i v-if="list && type=='u'" class="uiIconStatus"></i>
    </div>
    <div class="contactDetail">
      <div class="contactLabel">
        <span v-html="escapedName" />
        <slot></slot>
      </div>
      <div v-if="type =='u' && !list && !isCurrentUser" :class="statusStyle" class="user-status">
        <i class="uiIconStatus"></i>
        <span class="user-status">{{ getStatus }}</span>
      </div>
      <exo-dropdown-select v-if="type =='u' && !list && isCurrentUser" toggler-class="user-status" class="status-dropdown">
        <div slot="toggle" :class="statusStyle">
          <i class="uiIconStatus"></i>
          <span class="user-status">{{ getStatus }}</span>
        </div>
        <li v-for="(value, key) in statusMap" v-if="key !== 'offline'" slot="menu" :class="`user-${key}`" :key="key" @click="setStatus(key)"><a href="#"><span><i class="uiIconStatus"></i></span>{{ value }}</a></li>
      </exo-dropdown-select>
      <div v-if="type !='u' && !list && nbMembers > 0" class="room-number-members">
        {{ nbMembers }} {{ $t('exoplatform.chat.members') }}
      </div>
      <div v-if="mq === 'mobile' && list && lastMessage" class="last-message" v-html="lastMessage"></div>
    </div>
  </div>
</template>

<script>
import { getUserAvatar, getSpaceAvatar, getUserProfileLink, getSpaceProfileLink, escapeHtml } from '../chatServices';
import {chatConstants} from '../chatConstants';

export default {
  props: {
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
    }
  },
  data : function() {
    return {
      isOnline : true,
      statusMap : {
        available: this.$t('exoplatform.chat.available'),
        away: this.$t('exoplatform.chat.away'),
        donotdisturb: this.$t('exoplatform.chat.donotdisturb'),
        invisible: this.$t('exoplatform.chat.invisible'),
        offline: this.$t('exoplatform.chat.button.offline')
      }
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
      if (!this.isOnline || this.status === 'invisible' && !this.isCurrentUser) {
        return this.statusMap.offline;
      } else {
        return this.statusMap[this.status];
      }
    },
    contactAvatar() {
      if (this.type === 'u') {
        return getUserAvatar(this.userName);
      } else if (this.type === 's') {
        return getSpaceAvatar(this.name);
      } else {
        return chatConstants.DEFAULT_ROOM_AVATAR;
      }
    },
    escapedName() {
      return escapeHtml(this.name);
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
    },
    getProfileLink() {
      if (this.type === 'u') {
        return getUserProfileLink(this.userName);
      } else if (this.type === 's') {
        return getSpaceProfileLink(this.name);
      }
      return '#';
    }
  }
};
</script>