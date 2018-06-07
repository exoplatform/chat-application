<template>
  <div class="chat-contact">
    <div :style="`backgroundImage: url(${contactAvatar})`" :class="statusStyle" class="chat-contact-avatar">
      <i v-if="list && type=='u'" class="uiIconStatus"></i>
    </div>
    <div class="contactDetail">
      <div :class="statusStyle" class="contactLabel">
        <span v-html="name" />
        <slot></slot>
      </div>
      <div v-if="type =='u' && !list && !isCurrentUser" :class="statusStyle" class="user-status">
        <i class="uiIconStatus"></i>
        <span class="user-status">{{ getStatus }}</span>
      </div>
      <dropdown-select v-if="type =='u' && !list && isCurrentUser" toggler-class="user-status" class="status-dropdown">
        <div slot="toggle" :class="statusStyle">
          <i class="uiIconStatus"></i>
          <span class="user-status">{{ getStatus }}</span>
        </div>
        <li v-for="(value, key) in statusMap" v-if="key !== 'offline'" slot="menu" :class="`user-${key}`" :key="key" @click="setStatus(key)"><a href="#"><span><i class="uiIconStatus"></i></span>{{ value }}</a></li>
      </dropdown-select>
      <div v-if="type !='u' && !list && nbMembers > 0" class="room-number-members">
        {{ nbMembers }} {{ $t('exoplatform.chat.members') }}
      </div>
      <div v-if="mq === 'mobile' && list && lastMessage" class="last-message">{{ lastMessage }}</div>
    </div>
  </div>
</template>

<script>
import { getUserAvatar, getSpaceAvatar } from '../chatServices';
import DropdownSelect from './DropdownSelect.vue';
export default {
  components: {DropdownSelect},
  props: {
    name: {
      type: String,
      default: ''
    },
    userName: {
      type: String,
      default: ''
    },
    status: {
      type: String,
      default: ''
    },
    list: {
      type: Boolean,
      default: false
    },
    type: {
      type: String,
      default: '',
      required: true
    },
    nbMembers: {
      type: Number,
      default: 0
    },
    isCurrentUser: {
      type: Boolean,
      default: false
    },
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
        donotdistrub: this.$t('exoplatform.chat.donotdisturb'),
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
        return getSpaceAvatar(encodeURI(this.name));
      } else {
        return '/chat/img/room-default.jpg'; // TODO add room default avatar
      }
    }
  },
  created() {
    document.addEventListener(this.$constants.EVENT_DISCONNECTED, this.setOffline);
    document.addEventListener(this.$constants.EVENT_CONNECTED, this.setOnline);
    document.addEventListener(this.$constants.EVENT_RECONNECTED, this.setOnline);
  },
  destroyed() {
    document.removeEventListener(this.$constants.EVENT_DISCONNECTED, this.setOffline);
    document.removeEventListener(this.$constants.EVENT_CONNECTED, this.setOnline);
    document.removeEventListener(this.$constants.EVENT_RECONNECTED, this.setOnline);
  },
  methods: {
    setStatus(status) {
      this.$emit('exo-chat-status-changed', status);
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