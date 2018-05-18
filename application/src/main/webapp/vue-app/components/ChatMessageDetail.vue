<template>
  <div :class="{'chat-message-not-sent': message.notSent, 'is-same-contact': hideAvatar, 'is-current-user': isCurrentUser}" class="chat-message-box">
    <div class="chat-sender-avatar">
      <div v-if="!hideAvatar && !isCurrentUser" :style="`backgroundImage: url(${contactAvatar}`" class="chat-contact-avatar"></div>
    </div>
    <div class="chat-message-bubble">
      <div v-if="!hideAvatar && !isCurrentUser" class="sender-name">{{ message.fullname }} :</div>
      <div class="message-content" v-html="message.msg"></div>
    </div>
    <div class="chat-message-action">
      <div v-if="!hideTime" class="message-time">{{ dateString }}</div>
    </div>
  </div>
</template>

<script>
import * as chatTime from '../chatTime';
import { getUserAvatar } from '../chatServices';

export default {
  props: {
    message: {
      type: Object,
      default: function() {
        return {
          fullname: null,
          isSystem: false,
          msg: null,
          msgId: null,
          options: null,
          timestamp: 0,
          type: null,
          user: null
        };
      }
    },
    hideAvatar : {
      type: Boolean,
      default: false
    },
    hideTime : {
      type: Boolean,
      default: false
    }
  },
  data : function() {
    return {
      isCurrentUser: eXo.chat.userSettings.username === this.message.user
    };
  },
  computed: {
    dateString() {
      return chatTime.getTimeString(this.message.timestamp);
    },
    contactAvatar() {
      return getUserAvatar(this.message.user);
    },

  }
};
</script>
