<template>    
  <v-tooltip v-if="spaceChatEnabled" bottom>
    <template #activator="{ on, attrs }">
      <div
        v-bind="attrs"
        v-on="on">
        <v-btn
          :ripple="false"
          icon
          color="primary"
          @click="openChatDrawer($event)">
          <v-icon size="18">fas fa-comments</v-icon>
        </v-btn>
      </div>
    </template>
    <span>
      {{ $t('exoplatform.chat.send.message') }}
    </span>
  </v-tooltip>
</template>
<script>
import {chatConstants} from '../chatConstants';
import * as chatServices from '../chatServices';
export default {
  props: {
    identityType: {
      type: String,
      default: '',
    },
    identityId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      spaceChatEnabled: true,
    };
  },
  created() {
    if ( this.identityType === 'space' ) {
      this.spaceChatEnabled = false;
      chatServices.getUserSettings().then(userSettings => {
        this.userSettings = userSettings;
        this.$spaceService.isSpaceMember(this.identityId, this.userSettings.username).then(data => {
          if (data.isMember === 'true') {
            chatServices.isRoomEnabled(this.userSettings, this.identityId).then(value => {
              this.spaceChatEnabled = value === 'true';
            });
          }
        });
      });
    }
  },
  methods: {
    openChatDrawer(event) {
      event.preventDefault();
      event.stopPropagation();
      const chatType =  this.identityType === 'space' ? 'space-id' : 'username';
      const chatRoomName = this.identityId;

      document.dispatchEvent(
        new CustomEvent(chatConstants.ACTION_ROOM_OPEN_CHAT, { detail: {
          name: chatRoomName,
          type: chatType,
        }}));
    },
  }
};
</script>
