<template>
  <div :class="statusStyle" class="chat-contact">
    <div :style="`backgroundImage: url(${contactAvatar}`" class="chat-contact-avatar">
      <i v-if="list && type=='u'" class="uiIconStatus"></i>
    </div>
    <div class="contactDetail">
      <div class="contactLabel">
        {{ name }}
        <slot></slot>
      </div>
      <div v-if="type =='u' && !list && !isCurrentUser" class="user-status">
        <i class="uiIconStatus"></i><span>{{ getStatus }}</span>
      </div>
      <dropdown-select v-if="type =='u' && !list && isCurrentUser" toggler-class="user-status" class="status-dropdown">
        <i slot="toggle" class="uiIconStatus"></i>
        <span slot="toggle">{{ getStatus }}</span>
        <li v-for="(value, key) in statusMap" v-if="key !== 'offline'" slot="menu" :class="`user-${key}`" :key="key" @click="setStatus(key)"><a href="#"><span><i class="uiIconStatus"></i></span>{{ value }}</a></li>
      </dropdown-select>
      <div v-if="type !='u' && !list && nbMembers > 0" class="room-number-members">
        {{ nbMembers }} {{ $t('exoplatform.chat.members') }}
      </div>
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
    }
  },
  data : function() {
    return {
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
      if (this.status === 'invisible' && !this.isCurrentUser) {
        return 'user-offline';
      } else {
        return `user-${this.status}`;
      }
    },
    getStatus() {
      if (this.status === 'invisible' && !this.isCurrentUser) {
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
  methods: {
    setStatus(status) {
      this.$emit('exo-chat-status-changed', status);
    }
  }
};
</script>