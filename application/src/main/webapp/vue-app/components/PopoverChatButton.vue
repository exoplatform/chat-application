<template>    
  <v-tooltip bottom>
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
      {{ $t('exoplatform.chat') }}
    </span>
  </v-tooltip>
</template>
<script>
import {chatConstants} from '../chatConstants';
export default {
  props: {
    identity: {
      type: Object,
      default: null,
    }
  },
  computed: {
    identityId() {
      return this.identity && this.identity.id;
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
    }
  }

};
</script>
