<template>
  <div v-if="contact && Object.keys(contact).length !== 0 && (contact.isEnabledUser === 'true' || contact.isEnabledUser === 'null')" :class="{'is-apps-closed': appsClosed}" class="chat-message-composer">
    <div v-if="!miniChat" class="apps-container">
      <div v-for="app in composerApplications" :key="app.key" class="apps-item" @click="openAppModal(app)">
        <div class="apps-item-icon"><i :class="app.iconClass"></i></div>
        <div v-if="mq==='desktop'" class="apps-item-label">{{ $t(app.labelKey) }}</div>
      </div>
    </div>
    <div class="composer-container">
      <div class="composer-box">
        <div v-if="!miniChat" class="composer-action">
          <div class="action-emoji">
            <i v-exo-tooltip.top="$t('exoplatform.chat.emoji.tip')" class="uiIconChatSmile" @click.prevent.stop="showEmojiPanel = !showEmojiPanel"></i>
            <div v-show="showEmojiPanel" class="composer-emoji-panel popover top">
              <div class="arrow"></div>
              <span v-for="emoji in getEmoticons" :key="emoji.keys[0]" :class="emoji.class" class="chat-emoticon" @click="selectEmoji(emoji)"></span>
            </div>
          </div>
          <div v-exo-tooltip.top="$t('exoplatform.chat.collaborative.actions.tip')" class="action-apps" @click="appsClosed = !appsClosed"><i class="uiIconPlusCircled"></i></div>
        </div>
        <input v-if="miniChat" id="messageComposerArea" ref="messageComposerArea" name="messageComposerArea" type="text" autofocus @keydown.enter="preventDefault" @keypress.enter="preventDefault" @keyup.enter="sendMessageWithKey" />
        <textarea v-else id="messageComposerArea" ref="messageComposerArea" name="messageComposerArea" @paste="onPaste" @keydown.enter="preventDefault" @keypress.enter="preventDefault" @keyup.enter="sendMessageWithKey" @keyup.up="editLastMessage" @keyup="resizeTextarea($event)"></textarea>
        <div v-exo-tooltip.top="$t('exoplatform.chat.send')" v-if="!miniChat" class="composer-action">
          <div class="action-send" @click="sendMessage">
            <i class="uiIconSend"></i>
          </div>
        </div>
      </div>
    </div>
    <exo-chat-apps-modal v-if="appsModal.isOpned" :app="appsModal.app" :title="appsModal.title" :contact="contact" :room-id="contact.room" @modal-closed="appsModal.isOpned = false"></exo-chat-apps-modal>
  </div>
</template>

<script>
import * as chatServices from '../chatServices';
import {composerApplications, EMOTICONS} from '../extension';
import {chatConstants} from '../chatConstants';

export default {
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
    },
    userSettings: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data() {
    return {
      appsModal: {
        title: '',
        isOpned: false
      },
      appsClosed: true,
      composerApplications: [],
      showEmojiPanel: false
    };
  },
  computed: {
    getEmoticons() {
      if(eXo && eXo.chat && eXo.chat.room && eXo.chat.room.extraEmoticons) {
        return EMOTICONS.concat(eXo.chat.room.extraEmoticons);
      } else if(EMOTICONS) {
        return EMOTICONS;
      } else {
        return [];
      }
    },
  },
  watch: {
    userSettings() {
      this.composerApplications = composerApplications;
    }
  },
  updated() {
    if (this.contact) {
      if (this.mq === 'desktop' && (this.contact.isEnabledUser === 'true' || this.contact.isEnabledUser === 'null')) { // set autofocus only for enabled contact on desktop
        this.$nextTick(() => {
          this.$refs.messageComposerArea.focus();

          this.composerApplications.forEach(application => {
            if (application.mount) {
              application.mount($, chatServices);
            }
          });
        });
      }
    }
  },
  created() {
    document.addEventListener('keyup', this.closeApps);
    document.addEventListener('click', this.closeEmojiPanel);
    document.addEventListener(chatConstants.ACTION_MESSAGE_SEND, this.putFocusOnComposer);
    document.addEventListener(chatConstants.ACTION_MESSAGE_DELETE, this.putFocusOnComposer);
    document.addEventListener(chatConstants.ACTION_MESSAGE_QUOTE, this.quoteMessage);
  },
  mounted() {
    this.composerApplications = composerApplications;
  },
  destroyed() {
    document.removeEventListener('keyup', this.closeApps);
    document.removeEventListener('click', this.closeEmojiPanel);
    document.removeEventListener(chatConstants.ACTION_MESSAGE_SEND, this.putFocusOnComposer);
    document.removeEventListener(chatConstants.ACTION_MESSAGE_DELETE, this.putFocusOnComposer);
    document.removeEventListener(chatConstants.ACTION_MESSAGE_QUOTE, this.quoteMessage);
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
      emojiKey = ` ${emojiKey} `;
      $composer.insertAtCaret(emojiKey);
      this.closeEmojiPanel();
    },
    preventDefault(event) {
      if (event.keyCode === chatConstants.ENTER_CODE_KEY) {
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
        this.composerApplications.forEach(application => {
          if(application.shortcutMatches && application.shortcutMatches(newMessage)) {
            if (application.shortcutCallback) {
              found = true;
              application.shortcutCallback(chatServices, $, newMessage, this.contact);
            } else if(application.shortcutTriggeredEvent) {
              found = true;
              document.dispatchEvent(new CustomEvent(application.shortcutTriggeredEvent, {detail: {msg: newMessage, contact : this.contact}}));
            }
          }
        });
      }
      if (!found) {
        this.$emit('message-written', message);
      }
      this.$refs.messageComposerArea.value = '';
    },
    sendMessageWithKey(event) {
      if (event && event.keyCode === chatConstants.ENTER_CODE_KEY) {
        if (event.ctrlKey || event.altKey || event.shiftKey) {
          $(this.$refs.messageComposerArea).insertAtCaret('\n');
        } else {
          this.sendMessage();
        }
      }
    },
    quoteMessage(e) {
      const quotedMessage = e.detail;
      const composer = $(this.$refs.messageComposerArea);
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
        document.dispatchEvent(new CustomEvent(chatConstants.ACTION_MESSAGE_EDIT_LAST));
      }
    },
    putFocusOnComposer() {
      this.$refs.messageComposerArea.focus();
    },
    resizeTextarea(e) {
      if (this.mq !== 'mobile') {return;}
      const BORDER_SIZE = 2;
      const INITIAL_HEIGHT = '40px';
      const elem = e.target;
      elem.style.height = INITIAL_HEIGHT;
      elem.style.height = `${elem.scrollHeight + BORDER_SIZE}px`;
      elem.scrollTop = elem.scrollHeight;
    },
    onPaste(e) {
      //paste images
      const clipboardData = e.clipboardData || window.clipboardData;
      const html = clipboardData.getData('text/html') || '';
      const parsed = new DOMParser().parseFromString(html, 'text/html');
      const img = parsed.querySelector('img');
      if (img !== null) {
        const url = img.src;
        this.$refs.messageComposerArea.value = url;
        this.sendMessage();
      }
    },
  }
};
</script>
