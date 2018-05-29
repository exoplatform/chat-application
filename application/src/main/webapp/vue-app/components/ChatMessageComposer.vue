<template>
  <div v-if="contact && Object.keys(contact).length !== 0" :class="{'is-apps-closed': appsClosed}" class="chat-message">
    <div class="apps-container">
      <div v-for="app in getApplications" :key="app.key" class="apps-item" @click="openAppModal(app)">
        <div class="apps-item-icon"><i :class="app.class"></i></div>
        <div class="apps-item-label">{{ $t(app.labelKey) }}</div>
      </div>
    </div>
    <div class="composer-container">
      <div class="composer-box">
        <div class="composer-action">
          <div class="action-emoji">
            <i class="uiIconChatSmile" @click.prevent.stop="showEmojiPanel = !showEmojiPanel"></i>
            <div v-show="showEmojiPanel" class="composer-emoji-panel popover top">
              <div class="arrow"></div>
              <span v-for="emoji in getEmoticons" :key="emoji.keys[0]" :class="emoji.class" class="chat-emoticon" @click="selectEmoji(emoji)"></span>
            </div>
          </div>
          <div class="action-apps" @click="appsClosed = !appsClosed">+</div>
        </div>
        <textarea id="messageComposerArea" v-model="newMessage" name="messageComposerArea" autofocus @keydown.enter="preventDefault" @keypress.enter="preventDefault" @keyup.enter="sendMessageWithKey" @keyup.up="editLastMessage"></textarea>
        <div class="composer-action">
          <div class="action-send" @click="sendMessage">
            <i class="uiIconSend"></i>
          </div>
        </div>
      </div>
    </div>
    <apps-modal v-if="appsModal.isOpned" :app-key="appsModal.appKey" :title="appsModal.title" :contact="contact" :room-id="contact.room" @modal-closed="appsModal.isOpned = false"></apps-modal>
  </div>
</template>

<script>
import ComposerAppsModal from './modal/ComposerAppsModal.vue';
import * as chatServices from '../chatServices';

const ENTER_CODE_KEY = 13;
const DEFAULT_COMPOSER_APPS = [
  {
    key: 'event',
    labelKey: 'exoplatform.chat.add.event',
    class: 'uiIconChatCreateEvent'
  }, 
  {
    key: 'task',
    shortcutMatches(msg) {
      return /\s*\+\+\S+/.test(msg);
    },
    shortcutCallback(msg, contact) {
      const message = {
        msg : '',
        room : contact.room,
        clientId: new Date().getTime().toString(),
        user: eXo.chat.userSettings.username,
        isSystem: true,
        options: {
          fromUser: eXo.chat.userSettings.username,
          fromFullname: eXo.chat.userSettings.fullName
        }
      };
      message.options.type = 'type-task';
      const isSpace = contact.user.indexOf('space-') === 0;
      const isTeam = contact.user.indexOf('team-') === 0;
      const data = {
        'extension_action' : 'createTaskInline',
        'text' : msg,
        'roomName' : contact.fullName,
        'isSpace' : isSpace,
        'isTeam': isTeam,
        'participants': isSpace || isTeam ? contact.participants.join(',') : contact.user
      };
      chatServices.saveTask(eXo.chat.userSettings, data).then((response) => response.json()).then(data => {
        const url = data.url ? data.url : data.length && data.length === 1 && data[0].url ? data[0].url : '';
        const title = data.title ? data.title : data.length && data.length === 1 && data[0].title ? data[0].title : '';
        message.options.url = url;
        message.options.task = title;

        document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : message}));
      });
    },
    labelKey: 'exoplatform.chat.assign.task',
    class: 'uiIconChatCreateTask'
  }, 
  {
    key: 'link',
    labelKey: 'exoplatform.chat.share.link',
    class: 'uiIconChatLink'
  },
  {
    key: 'file',
    labelKey: 'exoplatform.chat.upload.file',
    class: 'uiIconChatUpload'
  },
  {
    key: 'question',
    labelKey: 'exoplatform.chat.ask.question',
    class: 'uiIconChatQuestion'
  },
  {
    key: 'raise-hand',
    labelKey: 'exoplatform.chat.raise.hand',
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
      appsClosed: true,
      showEmojiPanel: false
    };
  },
  computed: {
    getApplications() {
      if(eXo && eXo.chat && eXo.chat.room && eXo.chat.room.extraApplications) {
        return DEFAULT_COMPOSER_APPS.concat(eXo.chat.room.extraApplications);
      } else {
        return DEFAULT_COMPOSER_APPS;
      }
    },
    getEmoticons() {
      if(eXo && eXo.chat && eXo.chat.room && eXo.chat.room.extraEmoticons) {
        return this.EMOTICONS.concat(eXo.chat.room.extraEmoticons);
      } else {
        return this.EMOTICONS;
      } 
    }
  },
  created() {
    document.addEventListener('keyup', this.closeApps);
    document.addEventListener('click', this.closeEmojiPanel);
    document.addEventListener('exo-chat-message-acton-quote', this.quoteMessage);
    $(this.$refs.messageComposerArea).focus();
  },
  destroyed() {
    document.removeEventListener('keyup', this.closeApps);
    document.removeEventListener('click', this.closeEmojiPanel);
    document.removeEventListener('exo-chat-message-acton-quote', this.quoteMessage);
  },
  methods: {
    closeApps(e) {
      const ESC_KEY = 27;
      if (e.keyCode === ESC_KEY) {
        this.appsClosed = true;
      }
    },
    closeEmojiPanel() {
      this.showEmojiPanel = false;
    },
    selectEmoji(emoji) {
      let emojiKey = emoji.keys[0];
      const $composer = $(this.$refs.messageComposerArea);
      emojiKey = $composer.text().split(/\s/).slice(-1)[0] === '' ? emojiKey : ` ${emojiKey}`;
      $composer.insertAtCursor(emojiKey);
      this.closeEmojiPanel();
    },
    preventDefault(event) {
      if (event.keyCode === ENTER_CODE_KEY && !event.shiftKey && !event.ctrlKey && !event.altKey) {
        event.stopPropagation();
        event.preventDefault();
      }
    },
    sendMessage() {
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
      let found = false;
      this.getApplications.forEach(application => {
        if(application.shortcutMatches && application.shortcutMatches(this.newMessage)) {
          if (application.shortcutCallback) {
            found = true;
            application.shortcutCallback(this.newMessage, this.contact);
          } else if(application.shortcutTriggeredEvent) {
            found = true;
            document.dispatchEvent(new CustomEvent(application.shortcutTriggeredEvent, {detail: {msg: this.newMessage, contact : this.contact}}));
          }
        }
      });
      if (!found) {
        this.$emit('exo-chat-message-written', message);
      }
      this.newMessage = '';
    },
    sendMessageWithKey(event) {
      if (event.keyCode === ENTER_CODE_KEY && !event.shiftKey && !event.ctrlKey && !event.altKey) {
        this.sendMessage();
      }
    },
    quoteMessage(e) {
      const quotedMessage = e.detail;
      const composer = $('#messageComposerArea');
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
      composer.insertAtCaret(messageToSend);
      // The text insertion doesn't trigger Vue for modified field
      this.newMessage = composer.val();
    },
    openAppModal(app) {
      this.appsClosed = true;
      this.appsModal.appKey = app.key;
      this.appsModal.title = this.$t(app.labelKey);
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
