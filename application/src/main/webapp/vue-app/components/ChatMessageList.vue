<template>
  <div class="uiRightContainerArea">
    <div v-if="contact && Object.keys(contact).length !== 0" id="chats" class="chat-message-list">
      <div v-for="(subMessages, dayDate) in messagesMap" :key="dayDate" class="chat-message-day">
        <div class="day-separator">{{ dayDate }}</div>
        <chat-message-detail v-for="(messageObj, i) in subMessages" :key="messageObj.clientId" :message="messageObj" :hide-time="isHideTime(i, subMessages)" :hide-avatar="isHideAvatar(i, subMessages)"></chat-message-detail>
      </div>
    </div>
    <chat-message-composer :contact="contact" @exo-chat-message-written="messageWritten"></chat-message-composer>
  </div>
</template>

<script>
import ChatMessageDetail from './ChatMessageDetail.vue';
import * as chatServices from '../chatServices';
import * as chatTime from '../chatTime';
import ChatMessageComposer from './ChatMessageComposer.vue';

export default {
  components: {
    'chat-message-detail': ChatMessageDetail,
    'chat-message-composer': ChatMessageComposer
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
      return messagesMap;
    }
  },
  updated() {
    this.scrollToEnd();
  },
  created() {
    document.addEventListener('exo-chat-message-received', this.messageReceived);
    document.addEventListener('exo-chat-message-not-sent', this.messageNotSent);
    document.addEventListener('exo-chat-selected-contact-changed', this.contactChanged);
  },
  destroyed() {
    document.removeEventListener('exo-chat-message-received', this.messageReceived);
    document.removeEventListener('exo-chat-message-not-sent', this.messageNotSent);
    document.removeEventListener('exo-chat-selected-contact-changed', this.contactChanged);
  },
  methods: {
    messageWritten(message) {
      message.notSent = true;
      this.addOrUpdateMessage(message);
      this.setScrollToBottom();
    },
    messageReceived(e) {
      const messageObj = e.detail;
      messageObj.notSent = false;
      this.addOrUpdateMessage(messageObj.data);
    },
    contactChanged(e) {
      this.contact = e.detail;
      if(this.contact.room) {
        this.retrieveRoomMessages(); 
      } else {
        chatServices.getRoomId(eXo.chat.userSettings, this.contact).then((room) => {
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
      chatServices.getRoomMessages(eXo.chat.userSettings, this.contact).then(data => {
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
    },
    addOrUpdateMessage(message) {
      if(!message || !message.room || !message.clientId || message.room !== this.contact.room) {
        return;
      }
      const foundMessageIndex = this.messages.findIndex(messageObj => messageObj.clientId === message.clientId);
      if (foundMessageIndex >= 0) {
        this.messages.splice(foundMessageIndex, 1, message);
      } else {
        this.messages.push(message);
      }
    }
  }
};
</script>
