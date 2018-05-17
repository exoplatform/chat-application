<template>
  <div :class="sentClass" class="chat-message-detail"> {{ dateString }} - {{ message.fullname }} - <span v-html="message.msg"></span></div>
  <!-- TODO: use notSent attribute from message object -->
</template>

<script>
import * as chatTime from '../chatTime';

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
    }
  },
  computed: {
    dateString() {
      return chatTime.getTimeString(this.message.timestamp);
    },
    sentClass() {
      return this.message.notSent ? 'chat-message-not-sent' : '';
    }
  }
};
</script>
