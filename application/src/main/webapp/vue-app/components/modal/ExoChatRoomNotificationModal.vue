<template>
  <exo-modal v-show="show" :title="title" modal-class="room-notification-modal" @modal-closed="closeModal">
    <div class="chat-room-config">
      <span class="uiRadio">
        <input v-model="selectedOption" type="radio" value="normal">
        <span @click="selectedOption = 'normal'"> {{ $t('exoplatform.chat.desktopNotif.local.normal.label') }}</span>
      </span>
      <em class="notif-description"> {{ $t('exoplatform.chat.desktopNotif.local.normal') }}</em>

      <span class="uiRadio">
        <input v-model="selectedOption" type="radio" value="silence">
        <span @click="selectedOption = 'silence'"> {{ $t('exoplatform.chat.desktopNotif.local.silence.label') }}</span>
      </span>
      <em class="notif-description">{{ $t('exoplatform.chat.desktopNotif.local.silence') }}</em>

      <span class="uiRadio">
        <input v-model="selectedOption" type="radio" value="keywords">
        <span @click="selectedOption = 'keywords'"> {{ $t('exoplatform.chat.desktopNotif.local.alerton.label') }} :</span>
      </span>
      <input v-model="keywords" :disabled="disableAdvancedFilter" :placeholder="$t('exoplatform.chat.desktopNotif.local.alerton.placeholder')" class="notif-keyword" type="text">
      <span class="notif-description">{{ $t('exoplatform.chat.desktopNotif.local.alerton') }}</span>
    </div>
    <div class="uiAction uiActionBorder">
      <div class="btn btn-primary" @click="saveSettings">{{ $t('exoplatform.chat.user.popup.confirm') }}</div>
      <div class="btn" @click="closeModal">{{ $t('exoplatform.chat.cancel') }}</div>
    </div>
  </exo-modal>
</template>

<script>
import * as chatServices from '../../chatServices';

export default {
  props: {
    show: {
      type: Boolean,
      default() {
        return false;
      }
    },
    room: {
      type: String,
      default() {
        return '';
      }
    },
    roomName: {
      type: String,
      default() {
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
  watch: {
    show(){
      if(this.show){
        this.getPreferredNotification();
      }
    }
  },
  created() {
    this.getPreferredNotification();
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
    },
    getPreferredNotification() {
      if(eXo.chat.desktopNotificationSettings && eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger && eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger[this.room]) {
        this.selectedOption = eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger[this.room].notifCond;
        this.keywords = eXo.chat.desktopNotificationSettings.preferredRoomNotificationTrigger[this.room].keywords;
      } else {
        this.selectedOption = 'normal';
        this.keywords = '';
      }
    }
  }
};
</script>
