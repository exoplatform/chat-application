<template>
  <div v-if="contact && Object.keys(contact).length !== 0" id="chats" class="chat-message-list">
    <chat-message-detail v-for="messageObj in messages" :key="messageObj.msgId" :message="messageObj"></chat-message-detail>
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
      scrollToBottom: true,
      contact: {}
    };
  },
  updated() {
    this.scrollToEnd();
  },
  created() {
    document.addEventListener('exo-chat-message-received', this.messageReceived);
    document.addEventListener('exo-chat-contact-changed', this.contactChanged);
    document.addEventListener('exo-chat-messages-scrollToEnd', this.scrollToEnd);
    document.addEventListener('exo-chat-message-tosend', this.setScrollToBottom);
  },
  destroyed() {
    document.removeEventListener('exo-chat-message-received', this.messageReceived);
    document.removeEventListener('exo-chat-contact-changed', this.contactChanged);
    document.removeEventListener('exo-chat-messages-scrollToEnd', this.scrollToEnd);
    document.removeEventListener('exo-chat-message-tosend', this.setScrollToBottom);
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
      chatServices.getRoomMessages(this.userSettings, this.contact).then(data => {
        if (this.contact.room === data.room) {
          // Scroll to bottom once messages list updated
          this.scrollToBottom = true;

          this.messages = data.messages;
        }
      });
    },
    setScrollToBottom: function() {
      this.scrollToBottom = true;
    },
    scrollToEnd: function(e) {
      // If triggered using an event or explicitly asked to scroll to bottom
      if (e || this.scrollToBottom) {
        const container = $('.chat-message-list');
        container.scrollTop(container.prop('scrollHeight'));
        if (!e) {
          this.scrollToBottom = false;
        }
      }
    }
  }
};
</script>
