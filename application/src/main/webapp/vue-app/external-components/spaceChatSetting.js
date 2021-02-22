import * as chatServices from '../chatServices';

export const template = `
        <v-card id="FromChat" class="border-radius" flat>
          <v-list>
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title class="title text-color">
                  {{ $t('exoplatform.chat.app.title') }}
                </v-list-item-title>
                <v-list-item-subtitle>
                  {{ $t('exoplatform.chat.spaceSettings.external.component.description') }}
                </v-list-item-subtitle>
              </v-list-item-content>
              <v-list-item-action>
                <v-switch v-model="spaceChatEnabled" @change="enableDisableChat"></v-switch>
              </v-list-item-action>
            </v-list-item>
          </v-list>
        </v-card>
      `;
export const data = {
  spaceChatEnabled: false,
};
export const props = {
};
export const created = function() {
  //check if space's chat is enabled
  chatServices.getUserSettings()
    .then(userSettings => {
      this.userSettings = userSettings;
      chatServices.isRoomEnabled(this.userSettings, eXo.env.portal.spaceId)
        .then(value => {
          this.spaceChatEnabled = value === 'true';
        });
    });
};
export const methods = {
  enableDisableChat() {
    chatServices.updateRoomEnabled(this.userSettings, eXo.env.portal.spaceId, this.spaceChatEnabled);
  },
};