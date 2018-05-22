<template>
  <div class="uiRightContainerArea">
    <div v-if="contact && Object.keys(contact).length !== 0" id="chats" class="chat-message-list">
      <div v-for="(subMessages, dayDate) in messagesMap" :key="dayDate" class="chat-message-day">
        <div class="day-separator">{{ dayDate }}</div>
        <chat-message-detail v-for="(messageObj, i) in subMessages" :key="messageObj.clientId" :room="contact.room" :message="messageObj" :hide-time="isHideTime(i, subMessages)" :hide-avatar="isHideAvatar(i, subMessages)" @edit-message="editMessage"></chat-message-detail>
      </div>
    </div>
    <chat-message-composer :contact="contact" @exo-chat-message-written="messageWritten"></chat-message-composer>
    <modal v-show="showEditMessageModal" :title="$t('chat.message.editMessage')" modal-class="edit-message-modal" @modal-closed="closeModal">
      <textarea id="editMessageComposerArea" v-model="messageToEdit.msg" name="editMessageComposerArea"></textarea>
      <div class="uiAction uiActionBorder">
        <div class="btn btn-primary" @click="saveMessage">Enregistrer</div>
        <div class="btn" @click="closeModal">Annuler</div>
      </div>
    </modal>
  </div>
</template>

<script>
import ChatMessageDetail from './ChatMessageDetail.vue';
import ChatMessageComposer from './ChatMessageComposer.vue';
import Modal from './Modal.vue';
import * as chatWebStorage from '../chatWebStorage';
import * as chatServices from '../chatServices';
import * as chatTime from '../chatTime';

const MAX_SCROLL_POSITION_FOR_AUTOMATIC_SCROLL = 25;

export default {
  components: {
    'modal': Modal,
    'chat-message-detail': ChatMessageDetail,
    'chat-message-composer': ChatMessageComposer
  },
  data () {
    return {
      messages: [],
      scrollToBottom: true,
      contact: {},
      messageToEdit: {},
      showEditMessageModal: false
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
    document.addEventListener('exo-chat-message-updated', this.messageReceived);
    document.addEventListener('exo-chat-message-deleted', this.messageDeleted);
    document.addEventListener('exo-chat-message-received', this.messageReceived);
    document.addEventListener('exo-chat-message-not-sent', this.messageNotSent);
    document.addEventListener('exo-chat-selected-contact-changed', this.contactChanged);
  },
  destroyed() {
    document.removeEventListener('exo-chat-message-updated', this.messageReceived);
    document.removeEventListener('exo-chat-message-deleted', this.messageDeleted);
    document.removeEventListener('exo-chat-message-received', this.messageReceived);
    document.removeEventListener('exo-chat-message-not-sent', this.messageNotSent);
    document.removeEventListener('exo-chat-selected-contact-changed', this.contactChanged);
  },
  methods: {
    messageWritten(message) {
      chatWebStorage.storeNotSentMessage(message);
      this.addOrUpdateMessageToList(message);
      this.setScrollToBottom();
      document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : message}));
    },
    messageModified(message) {
      this.addOrUpdateMessageToList(message);
      this.setScrollToBottom();
      message.room = this.contact.room;
      document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : message}));
    },
    messageReceived(e) {
      const messageObj = e.detail;
      const message = messageObj.data;
      this.unifyMessageFormat(messageObj, message);
      chatWebStorage.storeMessageAsSent(message);
      this.addOrUpdateMessageToList(message);
    },
    contactChanged(e) {
      this.contact = e.detail;
      this.messages = [];
      if(this.contact.room) {
        this.retrieveRoomMessages(); 
      } else {
        chatServices.getRoomId(eXo.chat.userSettings, this.contact).then((room) => {
          if(room) {
            this.contact.room = room;
            this.retrieveRoomMessages(); 
          }
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
    isScrollPositionAtEnd() {
      const $chatMessageList = $('.chat-message-list');
      if($chatMessageList && $chatMessageList.length) {
        return $chatMessageList[0].scrollHeight - $chatMessageList.scrollTop() - $chatMessageList.height() < MAX_SCROLL_POSITION_FOR_AUTOMATIC_SCROLL;
      } else {
        return false;
      }
    },
    retrieveRoomMessages() {
      chatServices.getRoomMessages(eXo.chat.userSettings, this.contact).then(data => {
        if (this.contact.room === data.room) {
          // Scroll to bottom once messages list updated
          this.scrollToBottom = true;

          const roomNotSentMessages = chatWebStorage.getRoomNotSentMessages(eXo.chat.userSettings.username, this.contact.room);
          this.messages = data.messages.concat(roomNotSentMessages);
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
    addOrUpdateMessageToList(message) {
      if(!message || !message.room || message.room !== this.contact.room || !message.clientId && !message.msgId) {
        return;
      }
      if(this.isScrollPositionAtEnd()) {
        this.setScrollToBottom();
      }

      if (message.clientId) {
        this.messages = this.messages.filter(messageObj => messageObj.clientId !== message.clientId);
        this.messages.push(message);
      } else if (message.type === 'EDITED') {
        const messageModified = this.messages.find(messageObj => messageObj.msgId === message.msgId);
        if (messageModified) {
          messageModified.type = message.type;
          messageModified.msg = message.msg;
        }
      } else if (message.type === 'DELETED') {
        const messageDeleted = this.messages.find(messageObj => messageObj.msgId === message.msgId);
        if (messageDeleted) {
          messageDeleted.type = message.type;
          messageDeleted.msg = message.msg;
          messageDeleted.isDeleted = message.isDeleted;
        }
      }
    },
    messageDeleted(e) {
      const messageObj = e.detail;
      const message = messageObj.data;
      this.unifyMessageFormat(messageObj, message);
      this.addOrUpdateMessageToList(message);
    },
    unifyMessageFormat(messageObj, message) {
      if(!message.room && messageObj.room) {
        message.room = messageObj.room;
      }
      if(!message.user && (messageObj.user || messageObj.sender)) {
        message.user = messageObj.user ? messageObj.user : messageObj.sender;
      }
    },
    editMessage(message) {
      this.messageToEdit = JSON.parse(JSON.stringify(message));
      this.showEditMessageModal = true;
    },
    saveMessage() {
      this.messageModified(this.messageToEdit);
      this.showEditMessageModal = false;
    },
    closeModal() {
      this.showEditMessageModal = false;
    }
  }
};
</script>
