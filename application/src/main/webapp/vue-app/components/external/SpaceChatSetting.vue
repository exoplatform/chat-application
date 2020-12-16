<template>
  <v-card id="FromChat" class="border-radius" flat>
    <v-list>
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title class="title text-color">
            {{ $t('SpaceSettings.Chat') }}
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ 'Enable space chat' }}
          </v-list-item-subtitle>
        </v-list-item-content>
        <v-list-item-action>
          <v-switch v-model="spaceChatEnabled" @change="enableDisableChat"></v-switch>
        </v-list-item-action>
      </v-list-item>
    </v-list>
  </v-card>
</template>

<script>
import * as chatServices from '../../chatServices';

export default {
  props: {
    spaceId: {
      type: String,
      default: ''
    }
  },
  data: function() {
    return {
      spaceChatEnabled: false,
    };
  },
  created() {
    //check if space's chat is enabled
    chatServices.getUserSettings()
      .then(userSettings => {
        this.userSettings = userSettings;
        chatServices.isRoomEnabled(this.userSettings, this.spaceId)
          .then(value => {
            this.spaceChatEnabled = value === 'true';
          });
      });
  },
  methods: {
    enableDisableChat() {
      chatServices.updateRoomEnabled(this.userSettings, this.spaceId, this.spaceChatEnabled);
    },
  }
};
</script>