<template>
  <exo-drawer
    ref="QuickCreateDiscussionDrawer"
    id="QuickCreateDiscussionDrawer"
    right
    @closed="close">
    <template slot="title">
      <span class="PopupTitle popupTitle"> <v-icon left @click="close">mdi-arrow-left</v-icon>{{ $t('exoplatform.chat.quick.create.discussion') }}</span>
    </template>
    <template slot="content">
      <v-form ref="Quicksuggester" class="pa-2 ms-2 mt-4">
        <div class="d-flex flex-column flex-grow-1">
          <div class="d-flex flex-column mb-2">
            <label class="d-flex flex-row font-weight-bold my-2">{{ $t('exoplatform.chat.quick.create.discussion.team.people') }}</label>
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
                  <span><v-icon small>info</v-icon></span>{{ $t('exoplatform.chat.quick.create.discussion.info') }}
                </p>
              </v-flex>
            </div>
          </div>
          <div v-if="displayQuickDiscussionInputName" class="d-flex flex-column mb-2">
            <label class="d-flex flex-row font-weight-bold my-2">{{ $t('exoplatform.chat.team.name') }}</label>
            <div class="d-flex flex-row">
              <input
                :placeholder="$t('exoplatform.chat.quick.create.discussion.placeholder')"
                type="text"
                class="input-block-level ignore-vuetify-classes my-3"
                v-model="fullName">
            </div>
          </div>
        </div>
      </v-form>
    </template>
  </exo-drawer>
</template>


<script>

import {chatConstants} from '../../chatConstants';
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
  }
};
</script>




