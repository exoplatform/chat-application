<template>
  <exo-drawer
    ref="QuickCreateDiscussionDrawer"
    id="QuickCreateDiscussionDrawer"
    right
    @closed="close">
    <template slot="title">
      <span class="PopupTitle"> <v-icon left @click="close">mdi-arrow-left</v-icon>{{ $t('exoplatform.chat.quick.create.discussion') }}</span>
    </template>
    <template slot="content">
      <v-form ref="Quicksuggester" class="pa-2 ms-2 mt-4">
        <div class="d-flex flex-column flex-grow-1">
          <div class="d-flex flex-column mb-2">
            <label class="d-flex flex-row font-weight-bold my-2">{{ $t('exoplatform.chat.quick.create.discussion.add.people') }}</label>
            <div class="d-flex flex-row">
              <v-flex class="user-suggester text-truncate">
                <exo-identity-suggester
                  ref="invitedPeopleAutoCompleteToRoom"
                  v-model="participants"
                  :search-options="{}"
                  :labels="suggesterLabels"
                  include-users />
                <div v-if="participantItem" class="identitySuggester no-border mt-0">
                  <exo-chat-quick-discussion-participant-item
                    v-for="item in participantItem"
                    :key="item.identity.id"
                    :attendee="item"
                    @remove-attendee="removeAttendee" />
                </div>
                <p class="caption font-weight-light ps-1 muted font-italic">
                  <span class="mr-2"><v-icon small>info</v-icon></span>{{ $t('exoplatform.chat.quick.create.discussion.info') }}
                </p>
              </v-flex>
            </div>
          </div>
          <div v-if="displayQuickDiscussionInputName" class="d-flex flex-column mb-2">
            <label class="d-flex flex-row font-weight-bold my-2">{{ $t('exoplatform.chat.team.name') }}</label>
            <div class="d-flex flex-row">
              <input
                :placeholder="$t('exoplatform.chat.quick.create.discussion.room.placeholder')"
                type="text"
                class="input-block-level ignore-vuetify-classes my-3"
                v-model="fullName">
            </div>
          </div>
        </div>
      </v-form>
    </template>
    <template slot="footer">
      <div class="d-flex my-2 flex-row justify-end">
        <button class="mx-5 px-8 btn" @click="close()">{{ $t('exoplatform.chat.cancel') }}</button>
        <button
          :disabled="disabledSaveButton"
          class="btn btn-primary"
          @click="saveRoom">
          {{ $t('exoplatform.chat.save') }}
        </button>
      </div>
    </template>
  </exo-drawer>
</template>


<script>

import {chatConstants} from '../../chatConstants';
import * as chatServices from '../../chatServices';

export default {
  name: 'ExoChatDrawer',
  components: {},

  data() {
    return {
      participants: [],
      participantItem: [],
      fullName: '',
    };
  },
  computed: {
    validNewRoomName() {
      return this.fullName && this.fullName.trim().length;
    },
    suggesterLabels() {
      return {
        placeholder: this.$t('exoplatform.chat.team.help'),
      };
    },
    displayQuickDiscussionInputName() {
      return this.participantItem !== null && this.participantItem.length > 1 ? true : false;
    },
    disabledSaveButton(){
      return this.participantItem && this.participantItem.length === 1 && !this.validNewRoomName || 
        this.participantItem && this.participantItem.length > 1 && this.validNewRoomName ? false : true ;
    }
  },
  watch: {
    participants() {
      if (!this.participants) {
        this.$nextTick(this.$refs.invitedPeopleAutoCompleteToRoom.$refs.selectAutoComplete.deleteCurrentItem);
        return;
      }
      if (!this.participantItem) {
        this.participantItem = [];
      }
      const found = this.participantItem?.find(item => {
        return item.identity.remoteId === this.participants.remoteId
            && item.identity.providerId === this.participants.providerId;
      });
      if (!found) {
        this.participantItem.push({
          identity: this.participants,
        });
      }
      this.participants = null;
    },
  },
  created() {
    this.$root.$on(chatConstants.ACTION_CHAT_OPEN_QUICK_CREATE_DISCUSSION_DRAWER, this.openDrawer);
  },

  methods: {
    openDrawer() {
      this.$refs.QuickCreateDiscussionDrawer.open();
    },
    close(){
      this.participants = null;
      this.participantItem = [];
      this.fullName = '' ;
      this.$refs.QuickCreateDiscussionDrawer.close();
    },
    removeAttendee(attendee) {
      const index = this.participantItem.findIndex(addedAttendee => {
        return attendee.identity.remoteId === addedAttendee.identity.remoteId
            && attendee.identity.providerId === addedAttendee.identity.providerId;
      });
      if (index >= 0) {
        this.participantItem.splice(index, 1);
      }
    },
    saveQuickChatDiscussion() {
      if (this.fullName) {
        let users = this.participantItem.map(user => user.identity.remoteId || user.identity.name);
        if (users.indexOf(eXo.chat.userSettings.username) < 0) {
          users.unshift(eXo.chat.userSettings.username);
        }
        users = users.join(',');
        chatServices.saveRoom(eXo.chat.userSettings,  this.fullName, users, null)
          .then(resp => {
            const HTTP_OK_CODE = 200;
            if (resp.status === HTTP_OK_CODE) {
              return resp.json();
            } else {
              return resp.text();
            }
          })
          .then((response) => {
            if (response && response.room) {
              this.$root.$emit('quick-chat-discussion-saved', response.room);
              this.close();
              this.displayAlert(this.$t('exoplatform.chat.quick.create.discussion.success.notification'));
            } else if (response === 'roomAlreadyExists.creator') {
              this.displayAlert(this.$t('exoplatform.chat.CreatorErrorRoomCreationMessage'),'error');
            } else if (response === 'roomAlreadyExists.notCreator') {
              this.displayAlert(this.$t('exoplatform.chat.NotCreatorErrorRoomCreationMessage'),'error');
            }
          });
      }
    },
    displayAlert(message, type) {
      document.dispatchEvent(new CustomEvent('notification-alert', {detail: {
        message,
        type: type || 'success',
      }}));
    },
    openOneToOneQuickDiscussion(){
      const remoteId = this.participantItem[0].identity.remoteId ;
      this.close();
      document.dispatchEvent(new CustomEvent(chatConstants.ACTION_ROOM_OPEN_CHAT, {detail: {name: remoteId, type: 'username'}}));
      this.displayAlert(this.$t('exoplatform.chat.quick.create.discussion.success.notification'));
    },
    saveRoom(){
      if (this.validNewRoomName){
        this.saveQuickChatDiscussion();
      } else {this.openOneToOneQuickDiscussion();}
    }
  }
};
</script>
