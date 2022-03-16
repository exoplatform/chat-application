<template>    
  <v-tooltip v-if="spaceChatEnabled" bottom>
    <template v-slot:activator="{ on, attrs }">
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
    identity: {
      type: Object,
      default: null,
    }
  },
  data() {
    return {
      spaceChatEnabled: true,
    };
  },
  created() {
    if ( this.identity && this.identity.prettyName ) {
      chatServices.getUserSettings()
        .then(userSettings => {
          this.userSettings = userSettings;
          chatServices.isRoomEnabled(this.userSettings, this.identity.id)
            .then(value => {
              this.spaceChatEnabled = value === 'true';
            });
        });
    }
  },
  methods: {
    openChatDrawer(event) {
      event.preventDefault();
      event.stopPropagation();
      const chatType = this.identity && this.identity.groupId ? 'space-id' : 'username';
      const chatRoomName = this.identity && this.identity.prettyName ? this.identity.id : this.identity.username;

      document.dispatchEvent(
        new CustomEvent(chatConstants.ACTION_ROOM_OPEN_CHAT, { detail: {
          name: chatRoomName,
          type: chatType,
        }}));
    },
  }

};
</script>
