<template>
  <div id="message-scroll" class="uiRightContainerArea message-list">
    <div
      v-if="contact && Object.keys(contact).length !== 0"
      id="chats"
      ref="messagesListContainer"
      :class="{'chat-no-conversation muted': (!messages || !messages.length) && !newMessagesLoading}"
      class="chat-message-list"
      @wheel="loadMoreMessages();checkResetUnreadMessage()"
      @scroll="loadMoreMessages();checkResetUnreadMessage()">
      <div v-show="newMessagesLoading" class="center">
        <img
          src="/chat/img/sync.gif"
          width="64px"
          class="chatLoading">
      </div>
      <div
        v-for="(subMessages, dayDate) in messagesMap"
        :key="dayDate"
        class="chat-message-day">
        <div class="day-separator"><span>{{ dayDate }}</span></div>
        <exo-chat-message-detail
          v-for="messageObj in subMessages"
          :key="messageObj"
          :highlight="searchKeyword"
          :room="contact.room"
          :room-fullname="contact.fullName"
          :message="messageObj"
          :hide-time="messageObj.hideTime"
          :hide-avatar="messageObj.hideAvatar"
          :mini-chat="miniChat"
          :download-document-enabled="downloadDocumentEnabled"
          @edit-message="editMessage" />
      </div>
      <span v-show="!newMessagesLoading && (!messages || !messages.length)" class="text">{{ $t('exoplatform.chat.no.messages') }}</span>
    </div>
    <exo-chat-message-composer
      :composers-applications="composerApplications"
      :contact="contact"
      :mini-chat="miniChat"
      :user-settings="userSettings"
      @message-written="messageWritten" />
    <exo-chat-modal
      v-if="!miniChat"
      v-show="showEditMessageModal"
      :title="$t('exoplatform.chat.msg.edit')"
      modal-class="edit-message-modal"
      @modal-closed="closeModal">
      <exo-content-editable
        v-if="showEditMessageModal"
        id="editMessageComposerArea"
        ref="editMessageComposerArea"
        v-model="messageToEdit.msg"
        name="editMessageComposerArea"
        autofocus
        @keydown.enter="preventDefault"
        @keypress.enter="preventDefault"
        @keyup.enter="saveEditMessage" />
      <div class="uiAction uiActionBorder">
        <div class="btn btn-primary" @click="saveEditMessage(false)">{{ $t('exoplatform.chat.save') }}</div>
        <div class="btn" @click="closeModal">{{ $t('exoplatform.chat.cancel') }}</div>
      </div>
    </exo-chat-modal>
  </div>
</template>

<script>
import * as chatWebSocket from '../chatWebSocket';
import * as chatWebStorage from '../chatWebStorage';
import * as chatServices from '../chatServices';
import * as chatTime from '../chatTime';
import {chatConstants} from '../chatConstants';

export default {
  props: {
    composersApplications: {
      type: Object,
      default: function () {
        return {};
      }
    },
    miniChat: {
      type: Boolean,
      default: false
    },
    isOpenedContact: {
      type: Boolean,
      default: false
    },
    isOpenedContactApps: {
      type: Boolean,
      default: false
    },
    minimized: {
      type: Boolean,
      default: false
    },
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
      contact: {},
      messageToEdit: {},
      showEditMessageModal: false,
      totalMessagesToLoad: 0,
      searchKeyword: '',
      windowFocused: true,
      newMessagesLoading: false,
      downloadDocumentEnabled: false,
      composerApplications: [],
    };
  },
  mounted () {
    this.composerApplications = this.composersApplications;
  },
  computed: {
    messagesMap() {
      const messagesMap = {};
      let previousMessage = null;
      this.messages.forEach(message => {
        const messageDay = chatTime.getDayDateString(message.timestamp);
        if (!messagesMap[messageDay]) {
          messagesMap[messageDay] = [];
        }

        message.hideTime = this.isHideTime(previousMessage, message);
        message.hideAvatar = this.isHideAvatar(previousMessage, message);

        messagesMap[messageDay].push(message);
        previousMessage = message;
      });


      return messagesMap;
    },
    hasMoreMessages() {
      return this.totalMessagesToLoad <= this.messages.length;
    }
  },
  updated() {
    this.scrollToEnd();
    this.composerApplications = this.composersApplications;
  },
  created() {
    document.addEventListener(chatConstants.EVENT_MESSAGE_UPDATED, this.messageReceived);
    document.addEventListener(chatConstants.EVENT_MESSAGE_DELETED, this.messageDeleted);
    document.addEventListener(chatConstants.EVENT_MESSAGE_RECEIVED, this.messageReceived);
    document.addEventListener(chatConstants.EVENT_MESSAGE_READ, this.messageSent);
    document.addEventListener(chatConstants.EVENT_ROOM_SELECTION_CHANGED, this.contactChanged);
    document.addEventListener(chatConstants.ACTION_MESSAGE_EDIT_LAST, this.editLastMessage);
    document.addEventListener(chatConstants.ACTION_MESSAGE_SEARCH, this.searchMessage);

    $(window).focus(this.chatFocused);
    $(window).blur(this.chatFocused);
    this.$transferRulesService?.getTransfertRulesDownloadDocumentStatus().then(enabled => {
      this.downloadDocumentEnabled = enabled;
    });
  },
  destroyed() {
    document.removeEventListener(chatConstants.EVENT_MESSAGE_UPDATED, this.messageReceived);
    document.removeEventListener(chatConstants.EVENT_MESSAGE_DELETED, this.messageDeleted);
    document.removeEventListener(chatConstants.EVENT_MESSAGE_RECEIVED, this.messageReceived);
    document.removeEventListener(chatConstants.EVENT_MESSAGE_READ, this.messageSent);
    document.removeEventListener(chatConstants.EVENT_ROOM_SELECTION_CHANGED, this.contactChanged);
    document.removeEventListener(chatConstants.ACTION_MESSAGE_EDIT_LAST, this.editLastMessage);
    document.removeEventListener(chatConstants.ACTION_MESSAGE_SEARCH, this.searchMessage);
  },
  methods: {
    messageWritten(message) {
      chatWebStorage.storeNotSentMessage(message);
      this.addOrUpdateMessageToList(message);
      this.setScrollToBottom();
      document.dispatchEvent(new CustomEvent(chatConstants.ACTION_MESSAGE_SEND, {'detail': message}));
    },
    messageModified(message) {
      this.addOrUpdateMessageToList(message);
      this.setScrollToBottom();
      message.room = this.contact.room;
      document.dispatchEvent(new CustomEvent(chatConstants.ACTION_MESSAGE_SEND, {'detail': message}));
    },
    messageReceived(e) {
      const messageObj = e.detail;
      const message = messageObj.data;
      this.unifyMessageFormat(messageObj, message);
      this.addOrUpdateMessageToList(message);
    },
    messageSent(e) {
      const messageObj = e.detail;
      const message = messageObj.data;
      if (message) {
        this.setScrollToBottom();
        this.addOrUpdateMessageToList(message);
      }
    },
    chatFocused(e) {
      this.windowFocused = e.type === 'focus';
    },
    contactChanged(e) {
      this.messages = [];
      this.totalMessagesToLoad = 0;
      this.newMessagesLoading = false;

      this.contact = e.detail;
      if (this.contact) {
        if (this.contact.room) {
          this.retrieveRoomMessages();
        } else if (this.contact.user) {
          chatServices.getRoomId(this.userSettings, this.contact.user, 'username').then((room) => {
            if (room) {
              this.contact.room = room;
              this.retrieveRoomMessages();
            }
          });
        }
      }
    },
    setScrollToBottom: function() {
      this.scrollToBottom = true;
    },
    scrollToEnd: function(e) {
      // If triggered using an event or explicitly asked to scroll to bottom
      if (e || this.scrollToBottom) {
        const messagesListContainer = $(this.$refs.messagesListContainer);
        messagesListContainer.scrollTop(messagesListContainer.prop('scrollHeight'));
        if (!e) {
          this.scrollToBottom = false;
        }
      }
    },
    isScrollPositionAtEnd() {
      const messagesListContainer = $(this.$refs.messagesListContainer);
      if (messagesListContainer && messagesListContainer.length) {
        return messagesListContainer[0].scrollHeight - messagesListContainer.scrollTop() - messagesListContainer.height() < chatConstants.MAX_SCROLL_POSITION_FOR_AUTOMATIC_SCROLL;
      } else {
        return false;
      }
    },
    loadMoreMessages() {
      if (this.newMessagesLoading || $(this.$refs.messagesListContainer).scrollTop() > 0 || !this.hasMoreMessages) {
        return;
      }
      this.retrieveRoomMessages(true);
    },
    retrieveRoomMessages(avoidScrollingDown) {
      if (!this.contact || !this.contact.room || !this.contact.room.trim().length) {
        this.messages = [];
        return;
      }
      if (this.newMessagesLoading) {
        return;
      }
      let toTimestamp;
      if (!this.messages || !this.messages.length) {
        toTimestamp = '';
        this.totalMessagesToLoad = 0;
      } else {
        toTimestamp = this.messages[0].timestamp;
      }
      const limit = chatConstants.MESSAGES_PER_PAGE;
      this.newMessagesLoading = true;
      return chatServices.getRoomMessages(this.userSettings, this.contact, toTimestamp, limit).then(data => {
        if (this.contact.room === data.room) {
          // Mark room messages as read
          document.dispatchEvent(new CustomEvent(chatConstants.ACTION_ROOM_SET_READ, {detail: this.contact.room}));

          // Scroll to bottom once messages list updated
          this.scrollToBottom = !avoidScrollingDown;

          const roomNotSentMessages = chatWebStorage.getRoomNotSentMessages(this.userSettings.username, this.contact.room);
          const regexRest = new RegExp('^/rest/');
          data.messages.concat(roomNotSentMessages).forEach((message) => {
            if (!this.messages.find(displayedMessage => displayedMessage.msgId && displayedMessage.msgId === message.msgId || displayedMessage.clientId && displayedMessage.clientId === message.clientId)) {
              if (message.options && message.options.type && message.options.type === chatConstants.FILE_MESSAGE) {
                message.options.restPath = message.options.restPath.replace(regexRest, '/portal/rest/');
                message.options.thumbnailUrl = message.options.thumbnailUrl.replace(regexRest, '/portal/rest/');
                message.options.thumbnailURL = message.options.thumbnailURL.replace(regexRest, '/portal/rest/');
                message.options.downloadLink = message.options.downloadLink.replace(regexRest, '/portal/rest/');
              }
              this.messages.unshift(message);
            }
          });
          this.messages.sort((a, b) => {
            return a.timestamp - b.timestamp;
          });
          this.totalMessagesToLoad += limit;
        }
        this.newMessagesLoading = false;
      }).catch((error) => {this.newMessagesLoading = false; console.error(error);});
    },
    findMessage(field, value) {
      return this.messages.find(message => {return message[field] === value;});
    },
    getPrevMessage(i, messages) {
      return i <= 0 && messages.length >= i ? null : messages[i-1];
    },
    isHideTime(previousMessage, message) {
      if (previousMessage === null || this.mq === 'mobile') {
        return false;
      } else {
        return !message.timestamp || chatTime.isSameMinute(previousMessage.timestamp, message.timestamp);
      }
    },
    isHideAvatar(previousMessage, message) {
      if (previousMessage === null) {
        return false;
      } else {
        return previousMessage.user === message.user
                && previousMessage.timestamp && message.timestamp
                && chatTime.isSameDay(previousMessage.timestamp, message.timestamp);
      }
    },
    addOrUpdateMessageToList(message) {
      if (!message || !message.room || !this.contact.room || message.room !== this.contact.room || !message.clientId && !message.msgId) {
        return;
      }

      if (!this.ap && !this.isOpenedContact) {
        chatWebSocket.setRoomMessagesAsRead(this.contact.room);
      }

      if (this.ap && !this.isOpenedContactApps) {
        chatWebSocket.setRoomMessagesAsRead(this.contact.room);
      }

      if (this.isScrollPositionAtEnd()) {
        this.setScrollToBottom();
      }

      const index = this.messages.findIndex(messageObj => messageObj.clientId && messageObj.clientId === message.clientId || messageObj.msgId && messageObj.msgId === message.msgId);
      if (index > -1) {
        if (!message.fullname) {
          message.fullname = this.messages[index].fullname;
        }
        if (this.messages[index].notSent) {
          //remove the old failed message to not push the new succeed one instead of it in the list.
          this.messages.splice(index, 1);
          this.messages.push(message);
        } else {
          this.messages.splice(index, 1, message);
        }
      } else {
        this.messages.push(message);
      }
    },
    messageDeleted(e) {
      const messageObj = e.detail;
      const message = messageObj.data;
      if (!message.notSent) {
        this.unifyMessageFormat(messageObj, message);
        this.addOrUpdateMessageToList(message);
      } else {
        const msgIndex = this.messages.findIndex(msg => msg.clientId === message.clientId && !msg.msgId);
        if (msgIndex > 0) {
          this.messages.splice(msgIndex, 1);
        }
      }
    },
    unifyMessageFormat(messageObj, message) {
      if (!message.room && messageObj.room) {
        message.room = messageObj.room;
      }
      if (!message.user && (messageObj.user || messageObj.sender)) {
        message.user = messageObj.user ? messageObj.user : messageObj.sender;
      }
    },
    editLastMessage() {
      if (!this.messages || !this.messages.length) {
        return;
      }
      let lastMessage = null;
      let index = this.messages.length -1;
      while (!lastMessage && index >= 0) {
        const message = this.messages[index];
        if (message && !message.isDeleted && message.type !== chatConstants.DELETED_MESSAGE && !message.isSystem && message.user === this.userSettings.username) {
          lastMessage = message;
        }
        index--;
      }
      if (lastMessage) {
        this.editMessage(lastMessage);
      }
    },
    editMessage(message) {
      this.messageToEdit = JSON.parse(JSON.stringify(message));
      this.messageToEdit.msg = this.messageToEdit.msg || this.messageToEdit.message || '';
      this.messageToEdit.msg = this.messageToEdit.msg
        .replace(/&#92/g, '\\')
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')
        .replace(/&quot;/g, '"')
        .replace(/<br( *)\/?>/g, '\n')
        .replace(/&#38/g, '&');
      this.showEditMessageModal = true;
      this.$nextTick(() => {
        if (this.$refs.editMessageComposerArea) {
          document.execCommand('selectAll', false, null);
          document.getSelection().collapseToEnd();
        }
      });
    },
    saveEditMessage(event) {
      if (!event || event.keyCode === chatConstants.ENTER_CODE_KEY) {
        if (event && (event.ctrlKey || event.altKey || event.shiftKey)) {
          const composerArea = $(this.$refs.editMessageComposerArea);
          composerArea.insertAtCaret('\n');
          // make sure the vue model is updated after update via jquery
          this.messageToEdit.msg = composerArea.val();
        } else {
          this.messageToEdit.msg = this.messageToEdit.msg.trim();
          this.messageToEdit.msg = this.messageToEdit.msg.replace(/^(\s+<br( \/)?>)*|(<br( \/)?>\s)*$/gm, '');
          this.messageModified(this.messageToEdit);
          this.showEditMessageModal = false;
        }
      }
    },
    closeModal() {
      this.showEditMessageModal = false;
    },
    searchMessage(e) {
      this.searchKeyword = e.detail.trim();
    },
    preventDefault(event) {
      if (event.keyCode === chatConstants.ENTER_CODE_KEY) {
        event.stopPropagation();
        event.preventDefault();
      }
    },
    checkResetUnreadMessage() {
      if (this.isScrollPositionAtEnd()) {
        this.contact.unreadTotal = 0;
      }
    }
  }
};
</script>
