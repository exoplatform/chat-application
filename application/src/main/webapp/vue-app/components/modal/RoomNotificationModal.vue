<template>
  <modal v-show="show" :title="title" modal-class="room-notification-modal" @modal-closed="closeModal">
    <div id="room-config" style="display: inline-block;">
      <div class="row">
        <div class="offset1 chat-room-config">
          <input v-model="selectedOption" type="radio" name="optionsRoomNotificationNormal" value="normal">
          <span class="label-head" for="optionsRoomNotificationNormal" @click="selectedOption = 'normal'"> {{ $t('exoplatform.chat.desktopNotif.local.normal.label') }} </span> <br>
          <span class="label-text local"> {{ $t('exoplatform.chat.desktopNotif.local.normal') }} </span><br>

          <input v-model="selectedOption" type="radio" name="optionsRoomNotificationSilence" value="silence">
          <span class="label-head" for="optionsRoomNotificationSilence" @click="selectedOption = 'silence'"> {{ $t('exoplatform.chat.desktopNotif.local.silence.label') }} </span><br>
          <span class="label-text local">{{ $t('exoplatform.chat.desktopNotif.local.silence') }}</span><br>

          <input v-model="selectedOption" type="radio" name="optionsRoomNotificationKeywords" value="keywords">
          <span class="label-head" for="optionsRoomNotificationKeywords" @click="selectedOption = 'keywords'"> {{ $t('exoplatform.chat.desktopNotif.local.alerton.label') }} :</span><br>

          <input v-model="keywords" :disabled="disableAdvancedFilter" :placeholder="$t('exoplatform.chat.desktopNotif.local.alerton.placeholder')" class="radio-input-text" type="text" name="keyWord"><br>
          <span class="label-text local">{{ $t('exoplatform.chat.desktopNotif.local.alerton') }}</span><br><br>
        </div>
      </div>
      <div class="row center">
        <div class="btn btn-primary" @click="saveSettings">{{ $t('exoplatform.chat.save') }}</div>
        <div class="btn" @click="closeModal">{{ $t('exoplatform.chat.cancel') }}</div>
      </div>
    </div>
  </modal>
</template>

<script>
import Modal from './Modal.vue';
import * as chatServices from '../../chatServices';

export default {
  components: {Modal},
  props: {
    show: {
      type: Boolean,
      default: function () {
        return false;
      }
    },
    room: {
      type: String,
      default: function () {
        return '';
      }
    },
    roomName: {
      type: String,
      default: function () {
        return '';
      }
    }
  },
  data() {
    return {
      selectedOption: 'normal',
      keywords: ''
    };
  },
  computed: {
    disableAdvancedFilter() {
      return this.selectedOption !== 'keywords';
    },
    title() {
      return this.$t('exoplatform.chat.team.notifications', {0: this.roomName});
    }
  },
  created() {
    if(eXo.chat.desktopNotificationSettings && eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger && eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger[this.room]) {
      this.selectedOption = eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger[this.room].notifCond;
      this.keywords = eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger[this.room].keywords;
    }
  },
  methods: {
    closeModal() {
      // Emit the click event of close icon
      this.$emit('modal-closed');
    },
    saveSettings() {
      chatServices.setRoomNotificationTrigger(eXo.chat.userSettings, this.room, this.selectedOption, this.selectedOption === 'keywords' ? this.keywords : '', new Date().getTime().toString()).then(settings => {
        chatServices.loadNotificationSettings(settings);
      });
      this.closeModal();
    }
  }
};
</script>
