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
        <input id="add-room-suggestor" type="text">
        <div v-show="otherParticiants && otherParticiants.length > 0" class="room-suggest-list">
          <div
            v-for="participant in otherParticiants"
            :key="participant.name"
            class="uiMention">
            {{ participant.fullname }}
            <span @click="removeSuggest(participant)"><i class="uiIconClose"></i></span>
          </div>
        </div>
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
    otherParticiants() {
      return this.participants.filter(participant => participant.name !== eXo.chat.userSettings.username);
    },
    disableSave() {
      return !this.fullName || !this.fullName.trim().length;
    }
  },
  watch: {
    show(newValue) {
      if (this.selected && newValue) {
        this.fullName = this.selected.fullName;
        this.participants = this.selected.participants ? this.selected.participants : [];
        this.initSuggester();
      }
    }
  },
  created() {
    if (this.selected) {
      this.fullName = this.selected.fullName;
      this.participants = this.selected.participants ? this.selected.participants : [];
      this.initSuggester();
    }
  },
  methods: {
    initSuggester() {
      const $roomFormSuggestor = $('#add-room-suggestor');
      if ($roomFormSuggestor && $roomFormSuggestor.length && $roomFormSuggestor.suggester) {
        if (!$roomFormSuggestor[0].selectize) {
          const component = this;
          const suggesterData = {
            type: 'tag',
            create: false,
            createOnBlur: false,
            highlight: false,
            openOnFocus: false,
            sourceProviders: ['exo:chatuser'],
            valueField: 'name',
            labelField: 'fullname',
            searchField: ['fullname', 'name'],
            closeAfterSelect: true,
            dropdownParent: 'body',
            hideSelected: true,
            renderMenuItem (item, escape) {
              return component.renderMenuItem(item, escape);
            },
            renderItem(item) {
              return `<span class="hidden" data-value="${item.value}"></span>`;
            },
            onItemAdd(item) {
              component.addSuggestedItem(item);
            },
            sortField: [{field: 'order'}, {field: '$score'}],
            providers: {
              'exo:chatuser': component.findUsers
            }
          };
          //init suggester
          $roomFormSuggestor.suggester(suggesterData);
        } else {
          //clear suggester
          $roomFormSuggestor.suggester('setValue', '');
          $roomFormSuggestor[0].selectize.clear(true);
          $roomFormSuggestor[0].selectize.renderCache['item'] = {};
        }

        if (this.otherParticiants && this.otherParticiants.length) {
          this.otherParticiants.forEach(participant => {
            $roomFormSuggestor[0].selectize.addOption(participant);
            $roomFormSuggestor[0].selectize.addItem(participant.isExternal === 'true' ? `${participant.name} (this.$t('exoplatform.chat.external'))` : participant.name);
          });
        }
      }
    },
    findUsers (query, callback) {
      if (!query.length) {
        return callback(); 
      }
      chatServices.getChatUsers(eXo.chat.userSettings, query).then(data => {
        if (data && data.users) {
          chatServices.getRoomParticipantsToSuggest(data.users).then(users => {
            users.forEach(user => {
              if (user.isEnabled === 'null') {
                chatServices.getUserState(user.name).then(userState => {
                  chatServices.updateUser(eXo.chat.userSettings, user.name, userState.isDeleted, userState.isEnabled, userState.isExternal);
                  user.isEnabled = userState.isEnabled;
                  user.isDeleted = userState.isDeleted;
                  user.isExternal = userState.isExternal;
                });
              }
            });
            callback(users.filter(user => user.name !== eXo.chat.userSettings.username));
          });
        }
      });
    },
    renderMenuItem (item, escape) {
      const avatar = chatServices.getUserAvatar(item.name);
      const defaultAvatar = '/chat/img/room-default.jpg';
      return `
        <div class="avatarMini">
          <img src="${avatar}" onerror="this.src='${defaultAvatar}'">
        </div>
        <div class="user-name">${escape(item.fullname)} (${item.name})</div>
        <div class="user-status"><i class="chat-status-${item.status}"></i></div>
      `;
    },
    addSuggestedItem(item) {
      if ($('#add-room-suggestor') && $('#add-room-suggestor').length && $('#add-room-suggestor')[0].selectize) {
        const selectize = $('#add-room-suggestor')[0].selectize;
        item = selectize.options[item];
      }
      if (!this.participants.find(participant => participant.name === item.name)) {
        this.participants.push(item);
      }
    },
    removeSuggest(deletedParticipant) {
      const $suggesterInput = $('#add-room-suggestor');
      if ($suggesterInput && $suggesterInput.length && $suggesterInput[0].selectize) {
        const selectize = $suggesterInput[0].selectize;
        if (deletedParticipant && deletedParticipant.name) {
          selectize.removeItem(deletedParticipant.name, true);
          const indexOfItemToRemove = selectize.items.indexOf(deletedParticipant.name);
          if (indexOfItemToRemove >= 0) {
            selectize.items.splice(indexOfItemToRemove, 1);
          }
        }
      }
      // remove suggest for participants list
      this.participants = this.participants.filter(participant => deletedParticipant.name !== participant.name);
    },
    saveRoom() {
      if (this.fullName) {
        let users = this.participants.map(user => user.name);
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
    closeModal() {
      this.$emit('modal-closed');
    }
  }
};
</script>
