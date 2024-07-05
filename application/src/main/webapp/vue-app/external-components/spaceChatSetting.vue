<template>
  <v-app>
    <template v-if="displayed">
      <v-card
        id="chatSpaceSetting"
        class="card-border-radius"
        flat
        v-if="displayed">
        <v-list>
          <v-list-item>
            <v-list-item-content>
              <v-list-item-title class="text-title">
                  {{ $t('exoplatform.chat.spaceSettings.title') }}
              </v-list-item-title>
              <v-list-item-title class="pt-2">
                {{ $t('exoplatform.chat.spaceSettings.external.component.title') }}
              </v-list-item-title>
              <v-list-item-subtitle>
                {{ $t('exoplatform.chat.spaceSettings.external.component.description') }}
              </v-list-item-subtitle>
            </v-list-item-content>
            <v-list-item-action>
              <v-switch v-model="spaceChatEnabled" @change="enableDisableChat" class="pt-5"
              :aria-label="this.$t(`exoplatform.chat.spaceSettings.switch.label.${this.switchAriaLabel}`)" />
            </v-list-item-action>
          </v-list-item>
        </v-list>
      </v-card>
    </template>
  </v-app>
</template>
<script>
import * as chatServices from '../chatServices';
export default {
  data: () => ({
    id: `ChatApp${parseInt(Math.random() * 10000)}`,
    spaceChatEnabled: false,
    displayed: true,
  }),
  created() {
    //check if space's chat is enabled
    chatServices.getUserSettings()
      .then(userSettings => {
        this.userSettings = userSettings;
        chatServices.isRoomEnabled(this.userSettings, eXo.env.portal.spaceId)
          .then(value => {
            this.spaceChatEnabled = value === 'true';
          });
      });
    document.addEventListener('hideSettingsApps', (event) => {
      if (event && event.detail && this.id !== event.detail) {
        this.displayed = false;
      }
    });
    document.addEventListener('showSettingsApps', () => this.displayed = true);
  },
  computed: {
    switchAriaLabel() {
      return this.spaceChatEnabled && 'disable' || 'enable';
    },
  },
  methods: {
    enableDisableChat() {
      chatServices.updateRoomEnabled(this.userSettings, eXo.env.portal.spaceId, this.spaceChatEnabled);
    },
  }
};
</script>
