<template>
  <modal v-show="show" :title="title" modal-class="create-room-modal" @modal-closed="closeModal">
    <meta charset="utf-8">

    <div class="add-room-form">
      <label>{{ $t('exoplatform.chat.team.name') }}</label>
      <input v-model="fullName" type="text">
      <label>{{ $t('exoplatform.chat.team.people') }}</label>
      <input id="add-room-suggestor" type="text">
      <div class="room-suggest-list">
        <div v-for="participant in otherParticiants" :key="participant.name" class="uiMention">
          {{ participant.fullname }}
          <span @click="removeSuggest(participant)"><i class="uiIconClose"></i></span>
        </div>
      </div>
      <span class="team-add-user-label">{{ $t('exoplatform.chat.team.help') }}</span>
    </div>
    <div class="uiAction uiActionBorder">
      <button :disabled="disableSave" class="btn btn-primary" @click="saveRoom">{{ $t('exoplatform.chat.save') }}</button>
      <button class="btn" @click="closeModal">{{ $t('exoplatform.chat.cancel') }}</button>
    </div>
  </modal>
</template>

<script>
import * as chatServices from '../../chatServices';
import Modal from './Modal.vue';

export default {
  components: {Modal},
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
      fullName: ''
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
      if(newValue) {
        this.fullName = this.selected.fullName;
        this.participants = this.selected.participants ? this.selected.participants : [];
        this.initSuggester();
      }
    }
  },
  methods: {
    initSuggester() {
      const component = this;
      const $roomFormSuggestor = $('#add-room-suggestor');
      if(!$roomFormSuggestor[0].selectize) {
        //init suggester
        $roomFormSuggestor.suggester({
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
          renderItem(item) {
            return `<span class="hidden" data-value="${item.value}"></span>`;
          },
          onItemAdd(item) {
            component.addSuggestedItem(item);
          },
          sortField: [{field: 'order'}, {field: '$score'}],
          providers: {
            'exo:chatuser': function (query, callback) {
              if (!query.length) {
                return callback(); 
              }
              chatServices.getChatUsers(eXo.chat.userSettings, query).then(data => {
                if(data && data.users) {
                  callback(data.users.filter(user => user.name !== eXo.chat.userSettings.username));
                }
              });
            }
          }
        });
      } else {
        //clear suggester
        $roomFormSuggestor.suggester('setValue', '');
        $roomFormSuggestor[0].selectize.clear(true);
        $roomFormSuggestor[0].selectize.renderCache['item'] = {};
      }

      if(this.participants) {
        this.participants.forEach(participant => {
          if(participant.name !== eXo.chat.userSettings.username) {
            $roomFormSuggestor[0].selectize.addOption(participant);
            $roomFormSuggestor[0].selectize.addItem(participant.name);
          }
        });
      }
    },
    addSuggestedItem(item) {
      const selectize = $('#add-room-suggestor')[0].selectize;
      item = selectize.options[item];
      if(!this.participants.find(participant => participant.name === item.name)) {
        this.participants.push(item);
      }
    },
    removeSuggest(deletedParticipant) {
      const $suggesterInput = $('#add-room-suggestor');
      const selectize = $suggesterInput[0].selectize;
      if(deletedParticipant && deletedParticipant.name) {
        selectize.removeItem(deletedParticipant.name, true);
        const indexOfItemToRemove = selectize.items.indexOf(deletedParticipant.name);
        if(indexOfItemToRemove >= 0) {
          selectize.items.splice(indexOfItemToRemove, 1);
        }
      }
      // remove suggest for participants list
      this.participants = this.participants.filter(participant => deletedParticipant.name !== participant.name);
    },
    saveRoom() {
      if (this.fullName) {
        let users = this.participants.map(user => user.name);
        if(users.indexOf(eXo.chat.userSettings.username) < 0) {
          users.unshift(eXo.chat.userSettings.username);
        }
        users = users.join(',');
        chatServices.saveRoom(eXo.chat.userSettings,  this.fullName, users, this.selected.room).then((roomDetails) => {
          this.$emit('room-saved', roomDetails.room);
        });
        this.closeModal();
      }
    },
    closeModal() {
      this.$emit('modal-closed');
    }
  }
};
</script>