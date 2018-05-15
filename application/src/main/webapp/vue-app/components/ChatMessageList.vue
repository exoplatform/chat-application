<template>
  <div id="chats" class="chat-message-list">
    <chat-message-detail v-for="messageObj in messages" :key="messageObj.msgId" :message="messageObj">
    </chat-message-detail>
  </div>
</template>

<script>
import ChatMessageDetail from './ChatMessageDetail.vue';
import * as chatServices from '../chatServices';

export default {
  components: {'chat-message-detail': ChatMessageDetail},
  props: {
    userSettings: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data () {
    return {
      messages: [],
      contact: {}
    };
  },
  created() {
    document.addEventListener('exo-chat-message-received', this.messageReceived);
    document.addEventListener('exo-chat-contact-changed', this.contactChanged);
  },
  destroyed() {
    document.removeEventListener('exo-chat-message-received', this.messageReceived);
    document.addEventListener('exo-chat-contact-changed', this.contactChanged);
  },
  methods: {
    messageReceived(e) {
      const messageObj = e.detail;
      if (messageObj && (!this.contact || this.contact.room === messageObj.room) && messageObj && messageObj.data && messageObj.data.msgId) {
        this.messages.push(messageObj.data);
      }
    },
    contactChanged(e) {
      this.contact = e.detail;
      const thiss = this;
      chatServices.getRoomMessages(this.userSettings, this.contact).then(data => {
        if (thiss.contact.room === data.room) {
          thiss.messages = data.messages;
        }
      });
    }
  }
};
</script>
