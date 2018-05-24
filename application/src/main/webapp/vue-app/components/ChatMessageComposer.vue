<template>
  <div v-if="contact && Object.keys(contact).length !== 0" :class="{'is-apps-closed': appsClosed}" class="chat-message">
    <div class="apps-container">
      <div v-for="app in getApplications" :key="app.key" class="apps-item" @click="openAppModal(app)">
        <div class="apps-item-icon"><i :class="app.class"></i></div>
        <div class="apps-item-label">{{ app.label }}</div>
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
        <textarea id="messageComposerArea" v-model="newMessage" name="messageComposerArea" autofocus @keydown.enter="preventDefault" @keypress.enter="preventDefault" @keyup.enter="sendMessage" @keyup.up="editLastMessage"></textarea>
        <div class="composer-action">
          <div class="action-send">
            <i class="uiIconSend"></i>
          </div>
        </div>
      </div>
    </div>
    <apps-modal v-if="appsModal.isOpned" :app-key="appsModal.appKey" :title="appsModal.title" :contact="contact" :room-id="contact.room" @modal-closed="appsModal.isOpned = false"></apps-modal>
  </div>
</template>

<script>
import ComposerAppsModal from './ComposerAppsModal.vue';

const DEFAULT_COMPOSER_APPS = [
  {
    key: 'event',
    label: 'Add Event',
    class: 'uiIconChatCreateEvent'
  }, 
  {
    key: 'task',
    label: 'Assign task',
    class: 'uiIconChatCreateTask'
  }, 
  {
    key: 'link',
    label: 'Share link',
    class: 'uiIconChatLink'
  },
  {
    key: 'file',
    label: 'Upload file',
    class: 'uiIconChatUpload'
  },
  {
    key: 'question',
    label: 'Ask question',
    class: 'uiIconChatQuestion'
  },
  {
    key: 'raise-hand',
    label: 'Raise hand',
    class: 'uiIconChatRaiseHand'
  }
];

export default {
  components: {
    'apps-modal': ComposerAppsModal
  },
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
      appsModal: {
        appKey: '',
        title: '',
        isOpned: false
      },
      appsClosed: true
    };
  },
  computed: {
    getApplications() {
      if(eXo && eXo.chat && eXo.chat.room && eXo.chat.room.extraApplications) {
        return DEFAULT_COMPOSER_APPS.concat(eXo.chat.room.extraApplications);
      } else {
        return DEFAULT_COMPOSER_APPS;
      }
    }
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
        if(!this.newMessage || !this.newMessage.trim()) {
          return;
        }
        const message = {
          message : this.newMessage.trim(),
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
    },
    openAppModal(app) {
      this.appsClosed = true;
      this.appsModal.appKey = app.key;
      this.appsModal.title = app.label;
      this.appsModal.isOpned = true;
    },
    editLastMessage() {
      if (!this.newMessage || !this.newMessage.trim().length) {
        document.dispatchEvent(new CustomEvent('exo-chat-message-edit-last'));
      }
    }
  }
};
</script>
