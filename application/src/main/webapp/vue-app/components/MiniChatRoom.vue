<template>
  <div :class="{minimized : minimized}" class="mini-chat">
    <div class="title clearfix">
      <div class="title-right">
        <div class="callButtonContainerMiniWrapper pull-left" style="display: inline-block;"></div>
        <a :title="$t('exoplatform.chat.minimize')" class="uiActionWithLabel btn-mini" href="javaScript:void(0);" data-placement="top" data-toggle="tooltip" @click="minimized = true">
          <i class="uiIconMinimize"></i>
        </a>
        <a :title="$t('exoplatform.chat.maximize')" class="uiActionWithLabel btn-maxi" href="javaScript:void(0);" data-placement="top" data-toggle="tooltip" @click="minimized = false">
          <i class="uiIconMaximize"></i>
        </a>
        <a :title="$t('exoplatform.chat.open.chat')" class="uiActionWithLabel btn-open-chat" href="/portal/intranet/chat" data-placement="top" data-toggle="tooltip" target="_chat">
          <i class="uiIconChatPopOut"></i>
        </a>
        <a :title="$t('exoplatform.chat.close')" class="uiActionWithLabel btn-close" href="javaScript:void(0);" data-placement="top" data-toggle="tooltip" @click="$emit('close')">
          <i class="uiIconClose"></i>
        </a>
      </div>
      <div class="title-left">
        <span class="notify-info badgeDefault badgePrimary mini" style="display: none;">0</span> <span class="fullname">{{ selectedContact.fullName }}</span>
      </div>
    </div>
    <chat-message-list :mini-chat="true"></chat-message-list>
  </div>
</template>

<script>
import * as chatServices from '../chatServices';

import ChatMessageList from './ChatMessageList.vue';

export default {
  components: {
    'chat-message-list': ChatMessageList
  },
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
    }
  },
  created() {
    this.refreshSelectedRoom();
  },
  methods: {
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
            document.dispatchEvent(new CustomEvent(this.$constants.EVENT_ROOM_SELECTION_CHANGED, {detail: this.selectedContact}));
          }
        });
      } else {
        this.selectedContact = null;
      }
    }
  }
};
</script>