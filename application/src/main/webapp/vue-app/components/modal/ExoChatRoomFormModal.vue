<template>
  <div>
    <exo-chat-modal
      v-show="show"
      :title="title"
      :display-close="true"
      modal-class="create-room-modal"
      @modal-closed="closeModal">
      <meta charset="utf-8">
      <div class="add-room-form">
        <label>{{ $t('exoplatform.chat.team.name') }}</label>
        <input
          v-model="fullName"
          class="add-room-name"
          type="text">
        <label>{{ $t('exoplatform.chat.team.people') }}</label>
        <exo-identity-suggester
          ref="invitedPeopleAutoComplete"
          v-model="mappedParticipants"
          :search-options="{}"
          :key="mappedParticipants"
          name="invitePeople"
          multiple
          include-users />
        <span class="team-add-user-label">{{ $t('exoplatform.chat.team.help') }}</span>
      </div>
      <div class="uiAction uiActionBorder">
        <button
          :disabled="disableSave"
          class="btn btn-primary"
          @click="saveRoom">
          {{ $t('exoplatform.chat.save') }}
        </button>
        <button class="btn" @click="closeModal">{{ $t('exoplatform.chat.cancel') }}</button>
      </div>
    </exo-chat-modal>
    <exo-chat-modal
      v-show="showErrorModal"
      :title="errorModalTitle"
      :display-close="true"
      @modal-closed="showErrorModal = false">
      <ul class="singleMessage popupMessage resizable">
        <li><span class="errorIcon">{{ errorModalMessage }}</span></li>
      </ul>
      <div class="uiAction uiActionBorder">
        <button class="btn btn-primary" @click="showErrorModal = false">{{ $t('exoplatform.chat.close') }}</button>
      </div>
    </exo-chat-modal>
  </div>
</template>

<script>
import * as chatServices from '../../chatServices';

export default {
  props: {
    selected: {
      type: Object,
      default: function() {
        return {};
      }
    },
    show: {
      type: Boolean,
      default: function() {
        return false;
      }
    }
  },
  data() {
    return {
      participants: [],
      fullName: '',
      showErrorModal: false,
      errorModalTitle: '',
      errorModalMessage: ''
    };
  },
  computed: {
    title() {
      const key = !this.selected || !this.selected.room ? 'exoplatform.chat.team.add.title' : 'exoplatform.chat.team.edit';
      return this.$t(key);
    },
    validNewRoomName() {
      return this.fullName && this.fullName.trim().length;
    },
    disableSave() {
      return !this.validNewRoomName;
    },
    mappedParticipants() {
      return this.participants.map(participant => {
        return {
          'id': participant.name,
          'profile': {
            fullName: participant.fullname,
            external: participant.isExternal,
            avatarUrl: chatServices.getUserAvatar(participant.name)
          },
          'remoteId': participant.name
        };
      });
    }
  },
  watch: {
    show(newValue) {
      if (this.selected && newValue) {
        this.fullName = this.selected.fullName;
        this.participants = this.selected.participants ? this.mapParticipants(this.selected.participants) : [];
      }
    }
  },
  created() {
    if (this.selected) {
      this.fullName = this.selected.fullName;
      this.participants = this.selected.participants ? this.mapParticipants(this.selected.participants) : [];
    }
  },
  methods: {
    saveRoom() {
      if (this.fullName) {
        let users = this.mappedParticipants.map(user => user.remoteId || user.name);
        if (users.indexOf(eXo.chat.userSettings.username) < 0) {
          users.unshift(eXo.chat.userSettings.username);
        }
        users = users.join(',');
        chatServices.saveRoom(eXo.chat.userSettings,  this.fullName, users, this.selected.room)
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
              this.$emit('room-saved', response.room);
              this.closeModal();
            } else if (response === 'roomAlreadyExists.creator') {
              this.errorModalTitle = this.$t('exoplatform.chat.ErrorRoomCreationTitle');
              this.errorModalMessage = this.$t('exoplatform.chat.CreatorErrorRoomCreationMessage');
              this.showErrorModal = true;
            } else if (response === 'roomAlreadyExists.notCreator') {
              this.errorModalTitle = this.$t('exoplatform.chat.ErrorRoomCreationTitle');
              this.errorModalMessage = this.$t('exoplatform.chat.NotCreatorErrorRoomCreationMessage');
              this.showErrorModal = true;
            }
          });
      }
    },
    mapParticipants(participants) {
      return participants.map(participant => {
        return {
          'id': participant.name,
          'profile': {
            fullName: participant.fullname,
            external: participant.isExternal,
            avatarUrl: chatServices.getUserAvatar(participant.name)
          },
          'remoteId': participant.name
        };
      });
    },
    closeModal() {
      this.$emit('modal-closed');
    }
  }
};
</script>
