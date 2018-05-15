<template>
  <div class="chat-message-detail"> {{ message.user }} - {{ message.fullname }} - {{ message.msg }}</div>
</template>

<script>
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
  }
};
</script>
