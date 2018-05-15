<template>
  <div class="chat-message-detail"> {{ dateString }} - {{ message.fullname }} - {{ message.msg }}</div>
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
    }
  }
};
</script>
