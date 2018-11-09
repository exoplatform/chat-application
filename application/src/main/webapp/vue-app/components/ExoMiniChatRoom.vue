<template>
  <div :class="{minimized : minimized}" class="mini-chat">
    <div class="title clearfix">
      <div class="title-right">
        <div class="callButtonContainerMiniWrapper pull-left"></div>
        <a v-exo-tooltip="$t('exoplatform.chat.minimize')" class="uiActionWithLabel btn-mini" href="javaScript:void(0);" @click="minimized = true">
          <i class="uiIconMinimize"></i>
        </a>
        <a v-exo-tooltip="$t('exoplatform.chat.maximize')" class="uiActionWithLabel btn-maxi" href="javaScript:void(0);" @click="minimized = false">
          <i class="uiIconMaximize"></i>
        </a>
        <a v-exo-tooltip="$t('exoplatform.chat.open.chat')" class="uiActionWithLabel btn-open-chat" href="/portal/intranet/chat" target="_chat">
          <i class="uiIconChatPopOut"></i>
        </a>
        <a v-exo-tooltip="$t('exoplatform.chat.close')" class="uiActionWithLabel btn-close" href="javaScript:void(0);" @click="$emit('close')">
          <i class="uiIconClose"></i>
        </a>
      </div>
      <div class="title-left">
        <span :class="{'hidden': !selectedContact.unreadTotal}" class="notify-info badgeDefault badgePrimary mini">{{ selectedContact.unreadTotal }}</span> <span class="fullname">{{ selectedContact.fullName }}</span>
      </div>
    </div>
    <exo-chat-message-list :mini-chat="true" :minimized="minimized"></exo-chat-message-list>
  </div>
</template>

<script>
import {chatConstants} from '../chatConstants';
import * as chatServices from '../chatServices';

export default {
  props: {
    room: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      minimized: false,
      selectedContact: {}
    };
  },
  watch: {
    room() {
      this.refreshSelectedRoom();
    },
    minimized(value) {
      if (!value) {
        this.selectedContact.unreadTotal = 0;
      }
    }
  },
  created() {
    document.addEventListener(this.$constants.EVENT_MESSAGE_RECEIVED, this.messageReceived);
    this.refreshSelectedRoom();
  },
  destroyed() {
    document.removeEventListener(this.$constants.EVENT_MESSAGE_RECEIVED, this.messageReceived);
  },
  methods: {
    messageReceived(e) {
      if (!e || !e.detail) {
        return;
      }
      const message = e.detail;
      const user = message.data ? message.data.user : message.sender;
      const room = message.room;
      if (this.selectedContact && (this.selectedContact.room === room || this.selectedContact.user === user ) && this.minimized) {
        this.selectedContact.unreadTotal ++;
      }
    },
    refreshSelectedRoom() {
      if (this.room) {
        chatServices.getRoomDetail(eXo.chat.userSettings, this.room).then(contact => {
          if (
            contact &&
            contact.user &&
            contact.user.length &&
            contact.user !== 'undefined'
          ) {
            this.selectedContact = contact;
            eXo.chat.selectedContact = contact;
            if (!this.minimized) {
              document.dispatchEvent(new CustomEvent(this.$constants.EVENT_ROOM_SELECTION_CHANGED, {detail: this.selectedContact}));
            }
            localStorage.setItem(`${chatConstants.STORED_PARAM_OPENED_MINI_CHAT_ROOM}-${eXo.chat.userSettings.username}`, this.room);
          }
        });
      } else {
        this.selectedContact = null;
      }
    }
  }
};
</script>