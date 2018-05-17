<template>
  <div :class="statusStyle" class="chat-contact" @click="setStatus(status)">
    <div :style="`backgroundImage: url(${contactAvatar}`" :class="`user-${status}`" class="chat-contact-avatar">
      <i v-if="list && type=='u'" class="uiIconStatus"></i>
    </div>
    <div class="contactDetail">
      <div class="contactLabel">
        {{ name }}
        <slot></slot>
      </div>
      <div v-if="type =='u' && !list" :class="`user-${status}`" class="user-status">
        <i class="uiIconStatus"></i>
        {{ status }}
      </div>
      <div v-if="type !='u' && !list && nbMembers > 0" class="room-number-members">
        {{ nbMembers }} members
      </div>
    </div>
  </div>
</template>

<script>
import { getUserAvatar, getSpaceAvatar } from '../chatServices';
export default {
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
    }
  },
  data : function() {
    return {};
  },
  computed: {
    statusStyle: function() {
      return this.status === 'online' ? 'user-available' : 'user-invisible';
    },
    contactAvatar() {
      if (this.type === 'u') {
        return getUserAvatar(this.userName);
      } else if (this.type === 's') {
        return getSpaceAvatar(encodeURI(this.name));
      } else {
        return '/chat/img/user-default.jpg'; // TODO add room default avatar
      }
    }
  },
  methods: {
    setStatus(status) {
      // TODO, TO REMOVE !!!!
      status = status === 'available' ? 'away' : 'available';

      this.$emit('exo-chat-status-changed', status);
    }
  }
};
</script>