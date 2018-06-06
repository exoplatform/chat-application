<template>
  <div v-if="contact && Object.keys(contact).length !== 0" :class="{'is-apps-closed': appsClosed}" class="chat-message">
    <div v-if="!miniChat" class="apps-container">
      <div v-for="app in getApplications" :key="app.key" class="apps-item" @click="openAppModal(app)">
        <div class="apps-item-icon"><i :class="app.iconClass"></i></div>
        <div class="apps-item-label">{{ $t(app.labelKey) }}</div>
      </div>
    </div>
    <div class="composer-container">
      <div class="composer-box">
        <div v-if="!miniChat" class="composer-action">
          <div v-exo-tooltip.top="$t('exoplatform.chat.emoji.tip')" class="action-emoji">
            <i class="uiIconChatSmile" @click.prevent.stop="showEmojiPanel = !showEmojiPanel"></i>
            <div v-show="showEmojiPanel" class="composer-emoji-panel popover top">
              <div class="arrow"></div>
              <span v-for="emoji in getEmoticons" :key="emoji.keys[0]" :class="emoji.class" class="chat-emoticon" @click="selectEmoji(emoji)"></span>
            </div>
          </div>
          <div v-exo-tooltip.top="$t('exoplatform.chat.collaborative.actions.tip')" class="action-apps" @click="appsClosed = !appsClosed"><i class="uiIconPlusCircled"></i></div>
        </div>
        <input v-if="miniChat" id="messageComposerArea" ref="messageComposerArea" name="messageComposerArea" type="text" autofocus @keydown.enter="preventDefault" @keypress.enter="preventDefault" @keyup.enter="sendMessageWithKey" />
        <textarea v-else id="messageComposerArea" ref="messageComposerArea" name="messageComposerArea" autofocus @keydown.enter="preventDefault" @keypress.enter="preventDefault" @keyup.enter="sendMessageWithKey" @keyup.up="editLastMessage"></textarea>
        <div v-exo-tooltip.top="$t('exoplatform.chat.moreActions')" v-if="!miniChat" class="composer-action">
          <div class="action-send" @click="sendMessage">
            <i class="uiIconSend"></i>
          </div>
        </div>
      </div>
    </div>
    <apps-modal v-if="appsModal.isOpned" :app="appsModal.app" :title="appsModal.title" :contact="contact" :room-id="contact.room" @modal-closed="appsModal.isOpned = false"></apps-modal>
  </div>
</template>

<script>
import ComposerAppsModal from './modal/ComposerAppsModal.vue';
import {DEFAULT_COMPOSER_APPS} from '../appsMenu';

const ENTER_CODE_KEY = 13;
export default {
  components: {
    'apps-modal': ComposerAppsModal
  },
  props: {
    miniChat: {
      type: Boolean,
      default: false
    },
    contact: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data() {
    return {
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
    this.$nextTick(() => $('#messageComposerArea').focus());
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
      $composer.insertAtCaret(emojiKey);
      this.closeEmojiPanel();
    },
    preventDefault(event) {
      if (event.keyCode === ENTER_CODE_KEY && !event.shiftKey && !event.ctrlKey && !event.altKey) {
        event.stopPropagation();
        event.preventDefault();
      }
    },
    sendMessage() {
      const newMessage = this.$refs.messageComposerArea.value;
      if(!newMessage || !newMessage.trim()) {
        return;
      }
      const message = {
        message : newMessage.trim(),
        room : this.contact.room,
        clientId: new Date().getTime().toString(),
        timestamp: Date.now(),
        user: eXo.chat.userSettings.username
      };
      let found = false;
      if(!this.miniChat) {
        this.getApplications.forEach(application => {
          if(application.shortcutMatches && application.shortcutMatches(newMessage)) {
            if (application.shortcutCallback) {
              found = true;
              application.shortcutCallback(newMessage, this.contact);
            } else if(application.shortcutTriggeredEvent) {
              found = true;
              document.dispatchEvent(new CustomEvent(application.shortcutTriggeredEvent, {detail: {msg: newMessage, contact : this.contact}}));
            }
          }
        });
      }
      if (!found) {
        this.$emit('exo-chat-message-written', message);
      }
      this.$refs.messageComposerArea.value = '';
    },
    sendMessageWithKey(event) {
      if (event.keyCode === ENTER_CODE_KEY && !event.shiftKey && !event.ctrlKey && !event.altKey) {
        this.sendMessage();
      }
      if (event.keyCode === ENTER_CODE_KEY && (event.ctrlKey || event.altKey)) {
        $(this.$refs.messageComposerArea).insertAtCaret('\n');
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
    },
    openAppModal(app) {
      this.appsClosed = true;
      this.appsModal.app = app;
      this.appsModal.title = this.$t(app.labelKey);
      this.appsModal.isOpned = true;
    },
    editLastMessage() {
      const newMessage = this.$refs.messageComposerArea.value;

      if (!newMessage || !newMessage.trim().length) {
        this.$refs.messageComposerArea.value = '';
        document.dispatchEvent(new CustomEvent('exo-chat-message-edit-last'));
      }
    }
  }
};
</script>
