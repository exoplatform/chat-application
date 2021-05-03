<template>
  <exo-chat-modal
    v-show="show"
    id="chatPreferences"
    :title="$t('exoplatform.chat.settings.button.tip')"
    modal-class="chatPreferences"
    @modal-closed="closeModal">
    <section>
      <h4>{{ $t('exoplatform.chat.desktopNotif.global.notifications') }}</h4>
      <div class="notification-item">
        <input
          id="notifyDonotdisturb"
          ref="notifyDonotdisturb"
          v-model="chatPreferences.notifyDonotdisturb"
          type="checkbox">
        <div class="notification-description">
          <b> {{ $t('exoplatform.chat.desktopNotif.global.donotdist') }} </b>
          <em> {{ $t('exoplatform.chat.desktopNotif.global.donotdist.description') }} </em>
        </div>
      </div>
    </section>
    <section>
      <h4>{{ $t('exoplatform.chat.desktopNotif.global.notifyme') }}</h4>
      <div class="notification-item">
        <input
          id="notifyDesktop"
          ref="notifyDesktop"
          v-model="chatPreferences.notifyDesktop"
          type="checkbox">
        <div class="notification-description">
          <b>{{ $t('exoplatform.chat.desktopNotif.global.desktop') }}</b>
          <em>{{ $t('exoplatform.chat.desktopNotif.global.desktop.description') }}</em>
        </div>
      </div>
      <div class="notification-item">
        <input
          id="notifyOnSite"
          ref="notifyOnSite"
          v-model="chatPreferences.notifyOnSite"
          type="checkbox">
        <div class="notification-description">
          <b>{{ $t('exoplatform.chat.desktopNotif.global.onsite') }}</b>
          <em>{{ $t('exoplatform.chat.desktopNotif.global.onsite.description') }}</em>
        </div>
      </div>
      <div class="notification-item">
        <input
          id="notifyBip"
          ref="notifyBip"
          v-model="chatPreferences.notifyBip"
          type="checkbox">
        <div class="notification-description">
          <b>{{ $t('exoplatform.chat.desktopNotif.global.beep') }}</b>
          <em>{{ $t('exoplatform.chat.desktopNotif.global.beep.description') }}</em>
        </div>
      </div>
    </section>
    <div class="uiAction uiActionBorder">
      <div class="btn btn-primary" @click="saveNotificationSettings"> {{ $t('exoplatform.chat.user.popup.confirm') }} </div>
      <div class="btn" @click="closeModal"> {{ $t('exoplatform.chat.cancel') }} </div>
    </div>
  </exo-chat-modal>
</template>

<script>
import * as chatServices from '../../chatServices';
import {chatConstants} from '../../chatConstants';

export default {
  props: {
    show: {
      type: Boolean,
      default() {
        return false;
      }
    }
  },
  data() {
    return {
      chatPreferences: {
        notifyDonotdisturb: false,
        notifyDesktop: true,
        notifyOnSite: true,
        notifyBip: true
      },
      originalChatPreferences: {
        notifyDonotdisturb: false,
        notifyDesktop: true,
        notifyOnSite: true,
        notifyBip: true
      },
      checkboxesEnhanced: false
    };
  },
  watch: {
    show() {
      if (this.show) {
        if (eXo && eXo.chat && eXo.chat.desktopNotificationSettings) {
          const notifSettings = eXo.chat.desktopNotificationSettings;
          this.$refs.notifyDonotdisturb.checked = this.chatPreferences.notifyDonotdisturb = notifSettings.preferredNotificationTrigger.indexOf(chatConstants.NOT_DISTURB_NOTIF) < 0 ? false : true;
          this.$refs.notifyOnSite.checked = this.chatPreferences.notifyOnSite = notifSettings.preferredNotification.indexOf(chatConstants.ON_SITE_NOTIF) < 0 ? false : true;
          this.$refs.notifyDesktop.checked = this.chatPreferences.notifyDesktop = notifSettings.preferredNotification.indexOf(chatConstants.DESKTOP_NOTIF) < 0 ? false : true;
          this.$refs.notifyBip.checked = this.chatPreferences.notifyBip = notifSettings.preferredNotification.indexOf(chatConstants.BIP_NOTIF) < 0 ? false : true;
        }
        this.originalChatPreferences = JSON.parse(JSON.stringify(this.chatPreferences));

        if (!this.checkboxesEnhanced) {
          if ($.iphoneStyle) {
            $('#chatPreferences :checkbox').iphoneStyle({
              disabledClass: 'switchBtnDisabled',
              containerClass: 'uiSwitchBtn',
              labelOnClass: 'switchBtnLabelOn',
              labelOffClass: 'switchBtnLabelOff',
              handleClass: 'switchBtnHandle',
            });
          }
          this.checkboxesEnhanced = true;
        }
      }
    }
  },
  methods: {
    saveNotificationSettings() {
      const userSettings = eXo.chat.userSettings;
      const preferredNotificationTrigger = [];
      const preferredNotification = [];

      this.chatPreferences.notifyDonotdisturb = this.$refs.notifyDonotdisturb.checked;
      this.chatPreferences.notifyDesktop = this.$refs.notifyDesktop.checked;
      this.chatPreferences.notifyOnSite = this.$refs.notifyOnSite.checked;
      this.chatPreferences.notifyBip = this.$refs.notifyBip.checked;

      if (this.originalChatPreferences.notifyDonotdisturb !== this.chatPreferences.notifyDonotdisturb) {
        preferredNotificationTrigger.push(chatConstants.NOT_DISTURB_NOTIF);
      }
      if (this.originalChatPreferences.notifyDesktop !== this.chatPreferences.notifyDesktop) {
        preferredNotification.push(chatConstants.DESKTOP_NOTIF);
      }
      if (this.originalChatPreferences.notifyOnSite !== this.chatPreferences.notifyOnSite) {
        preferredNotification.push(chatConstants.ON_SITE_NOTIF);
      }
      if (this.originalChatPreferences.notifyBip !== this.chatPreferences.notifyBip) {
        preferredNotification.push(chatConstants.BIP_NOTIF);
      }

      if (preferredNotificationTrigger.length || preferredNotification.length) {
        chatServices.setUserNotificationPreferences(userSettings, preferredNotification, preferredNotificationTrigger).then(settings => {
          chatServices.loadNotificationSettings(settings);
          this.originalChatPreferences = this.chatPreferences;
        });
      }
      this.closeModal();
    },
    closeModal() {
      this.$emit('close-modal');
    }
  }
};
</script>
