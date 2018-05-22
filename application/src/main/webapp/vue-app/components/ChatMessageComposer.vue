<template>
  <div v-if="contact && Object.keys(contact).length !== 0" :class="{'is-apps-closed': appsClosed}" class="chat-message">
    <div class="apps-container">
      <div class="apps-item">
        <div class="apps-item-icon"><i class="uiIconChatCreateEvent"></i></div>
        <div class="apps-item-label">Add Event</div>
      </div>
      <div class="apps-item">
        <div class="apps-item-icon"><i class="uiIconChatCreateTask"></i></div>
        <div class="apps-item-label">Assign Task</div>
      </div>
      <div class="apps-item">
        <div class="apps-item-icon"><i class="uiIconChatLink"></i></div>
        <div class="apps-item-label">Share Link</div>
      </div>
      <div class="apps-item">
        <div class="apps-item-icon"><i class="uiIconChatUpload"></i></div>
        <div class="apps-item-label">Upload File</div>
      </div>
      <div class="apps-item">
        <div class="apps-item-icon"><i class="uiIconChatQuestion"></i></div>
        <div class="apps-item-label">Ask Question</div>
      </div>
      <div class="apps-item">
        <div class="apps-item-icon"><i class="uiIconChatRaiseHand"></i></div>
        <div class="apps-item-label">Raise Hand</div>
      </div>
    </div>
    <div class="composer-container">
      <div class="composer-box">
        <div class="composer-action">
          <div class="action-emoji">
            <i class="uiIconChatSmile"></i>
          </div>
          <div class="action-apps" @click="appsClosed = !appsClosed">+</div>
        </div>
        <textarea id="messageComposerArea" v-model="newMessage" name="messageComposerArea" @keydown.enter="preventDefault" @keypress.enter="preventDefault" @keyup.enter="sendMessage"></textarea>
        <div class="composer-action">
          <div class="action-send">
            <i class="uiIconSend"></i>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    contact: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data() {
    return {
      newMessage: '',
      appsClosed: true
    };
  },
  created() {
    document.addEventListener('keyup', this.closeApps);
    document.addEventListener('exo-chat-message-acton-quote', this.quoteMessage);
  },
  destroyed() {
    document.removeEventListener('keyup', this.closeApps);
    document.removeEventListener('exo-chat-message-acton-quote', this.quoteMessage);
  },
  methods: {
    closeApps(e) {
      const ESC_KEY = 27;
      if (e.keyCode === ESC_KEY) {
        this.appsClosed = true;
      }
    },
    preventDefault(event) {
      const enterKeyCode = 13;
      if (event.keyCode === enterKeyCode && !event.shiftKey && !event.ctrlKey && !event.altKey) {
        event.stopPropagation();
        event.preventDefault();
      }
    },
    sendMessage(event) {
      const enterKeyCode = 13;
      if (event.keyCode === enterKeyCode && !event.shiftKey && !event.ctrlKey && !event.altKey) {
        const message = {
          message : this.newMessage,
          room : this.contact.room,
          clientId: new Date().getTime().toString(),
          timestamp: Date.now(),
          user: eXo.chat.userSettings.username
        };
        this.$emit('exo-chat-message-written', message);
        this.newMessage = '';
      }
    },
    quoteMessage(e) {
      const quotedMessage = e.detail;
      if(!quotedMessage) {
        return;
      }
      let messageToSend = quotedMessage.msg ? quotedMessage.msg : quotedMessage.message;
      if(!messageToSend) {
        return;
      }
      messageToSend = messageToSend.replace(/<br\/>/g, '\n');
      messageToSend = $('<div />').html(messageToSend).text();
      messageToSend = `[quote=${quotedMessage.fullname}] ${messageToSend} [/quote]`;
      $('#messageComposerArea').insertAtCaret(messageToSend);

      // The text insertion doesn't trigger Vue for modified field
      this.newMessage = $('#messageComposerArea').val();
    }
  }
};
</script>
