<template>
  <div v-if="contact && Object.keys(contact).length !== 0" id="chats" class="chat-message-list">
    <div v-for="(subMessages, dayDate) in messagesMap" :key="dayDate" class="chat-message-day">
      <div class="day-separator">{{ dayDate }}</div>
      <chat-message-detail v-for="(messageObj, i) in subMessages" :key="messageObj.clientId" :message="messageObj" :hide-time="isHideTime(i, subMessages)" :hide-avatar="isHideAvatar(i, subMessages)"></chat-message-detail>
    </div>
  </div>
</template>

<script>
import ChatMessageDetail from './ChatMessageDetail.vue';
import * as chatServices from '../chatServices';
import * as chatTime from '../chatTime';

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
  computed: {
    messagesMap() {
      const days = this.messages.map((message) => chatTime.getDayDate(message.timestamp).toString() ).reduce(function(result, current){
        return result.indexOf(current) === -1 ? result.concat(current) : result;
      }, []);
      const messagesMap = {};
      days.forEach(element => {
        messagesMap[element] = this.messages.filter((message) => chatTime.getDayDate(message.timestamp) === element);
      });
      console.log(messagesMap);
      return messagesMap;
    }
  },
  updated() {
    this.scrollToEnd();
  },
  created() {
    document.addEventListener('exo-chat-message-received', this.messageReceived);
    document.addEventListener('exo-chat-selected-contact-changed', this.contactChanged);
    document.addEventListener('exo-chat-messages-scrollToEnd', this.scrollToEnd);
    document.addEventListener('exo-chat-message-tosend', this.messageReceived);
    document.addEventListener('exo-chat-message-tosend', this.setScrollToBottom);
    document.addEventListener('exo-chat-message-not-sent', this.messageNotSent);
  },
  destroyed() {
    document.removeEventListener('exo-chat-message-received', this.messageReceived);
    document.removeEventListener('exo-chat-selected-contact-changed', this.contactChanged);
    document.removeEventListener('exo-chat-messages-scrollToEnd', this.scrollToEnd);
    document.removeEventListener('exo-chat-message-tosend', this.messageSent);
    document.removeEventListener('exo-chat-message-tosend', this.setScrollToBottom);
    document.removeEventListener('exo-chat-message-not-sent', this.messageNotSent);
  },
  methods: {
    messageSent(e) {
      const messageObj = e.detail;
      if (messageObj && (!this.contact || this.contact.room === messageObj.room) && messageObj && messageObj.data && messageObj.data.msgId) {
        const foundMessage = this.findMessage('clientId', messageObj.data.clientId);
        if (foundMessage) {
          foundMessage.notSent = false;
        } else {
          this.messages.push(messageObj.data);
        }
      }
    },
    messageReceived(e) {
      const messageObj = e.detail;
      if (messageObj && (!this.contact || this.contact.room === messageObj.room) && messageObj && messageObj.data && messageObj.data.msgId) {
        this.messages.push(messageObj.data);
      }
    },
    contactChanged(e) {
      this.contact = e.detail;
      if(this.contact.room) {
        this.retrieveRoomMessages(); 
      } else {
        chatServices.getRoomId(this.userSettings, this.contact).then((room) => {
          this.contact.room = room;
          this.retrieveRoomMessages(); 
        });
      }
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
    },
    retrieveRoomMessages() {
      chatServices.getRoomMessages(this.userSettings, this.contact).then(data => {
        if (this.contact.room === data.room) {
          // Scroll to bottom once messages list updated
          this.scrollToBottom = true;

          this.messages = data.messages;
          this.messages.sort((a, b) => {
            return a.timestamp - b.timestamp;
          });
        }
      });
    },
    messageNotSent(e) {
      const notSentMessage = e.detail;
      if (notSentMessage && notSentMessage.clientId) {
        const foundMessage = this.findMessage('clientId', notSentMessage.clientId);
        if (foundMessage) {
          foundMessage.notSent = true;
        } else {
          notSentMessage.notSent = true;
          this.messages.push(notSentMessage);
        }
      }
    },
    findMessage(field, msgId) {
      return this.messages.find(message => {return message[field] === msgId;});
    },
    getPrevMessage(i, messages) {
      return i <= 0 && messages.length >= i ? null : messages[i-1];
    },
    isHideTime(i, messages) {
      const prevMsg = this.getPrevMessage(i, messages);
      if (prevMsg === null) {
        return false;
      } else {
        return chatTime.getTimeString(prevMsg.timestamp) === chatTime.getTimeString(messages[i].timestamp) ? true : false;
      }
    },
    isHideAvatar(i, messages) {
      const prevMsg = this.getPrevMessage(i, messages);
      if (prevMsg === null) {
        return false;
      } else {
        return prevMsg.user === messages[i].user ? true : false;
      }
    }
  }
};
</script>
