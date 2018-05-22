<template>
  <modal v-show="show" id="chatPreferences" title="Chat Preferences" modal-class="chatPreferences" @modal-closed="closeModal">
    <section>
      <h4>Notifications</h4>
      <div class="notification-item">
        <input id="notifyDonotdistrub" ref="notifyDonotdistrub" v-model="chatPreferences.notifyDonotdistrub" type="checkbox">
        <div class="notification-description">
          <b>Notifications "Ne pas Déranger"</b>
          <em>M'avertir même lorsque je suis en "Ne pas Déranger"</em>
        </div>
      </div>
    </section>
    <section>
      <h4>Notify me with</h4>
      <div class="notification-item">
        <input id="notifyDesktop" ref="notifyDesktop" v-model="chatPreferences.notifyDesktop" type="checkbox">
        <div class="notification-description">
          <b>Notifications sur le bureau</b>
          <em>Afficher une notification toast sur votre ordinateur</em>
        </div>
      </div>
      <div class="notification-item">
        <input id="notifyOnSite" ref="notifyOnSite" v-model="chatPreferences.notifyOnSite" type="checkbox">
        <div class="notification-description">
          <b>Notifications sur site</b>
          <em>Afficher un compteur sur l'icône dans la barre du haut</em>
        </div>
      </div>
      <div class="notification-item">
        <input id="notifyBip" ref="notifyBip" v-model="chatPreferences.notifyBip" type="checkbox">
        <div class="notification-description">
          <b>Bips sonores</b>
          <em>Émettre un signal sonore chaque fois qu’un nouveau message arrive</em>
        </div>
      </div>
    </section>
    <div class="uiAction uiActionBorder">
      <div class="btn btn-primary" @click="saveNotificationSettings">Enregistrer</div>
      <div class="btn" @click="closeModal">Annuler</div>
    </div>
  </modal>
</template>

<script>
import Modal from './Modal.vue';
import * as chatServices from '../chatServices';

const ON_SITE_NOTIF = 'on-site';
const DESKTOP_NOTIF = 'desktop';
const BIP_NOTIF = 'bip';
const NOT_DISTRUB_NOTIF = 'notify-even-not-distrub';

export default {
  components: {
    modal: Modal
  },
  props: {
    show: {
      type: Boolean,
      default: function () {
        return false;
      }
    }
  },
  data() {
    return {
      chatPreferences: {
        notifyDonotdistrub: true,
        notifyDesktop: true,
        notifyOnSite: true,
        notifyBip: true
      },
      originalChatPreferences: {
        notifyDonotdistrub: true,
        notifyDesktop: true,
        notifyOnSite: true,
        notifyBip: true
      },
      checkboxesEnhanced: false
    };
  },
  watch: {
    show() {
      if(this.show) {
        if (eXo && eXo.chat && eXo.chat.desktopNotificationSettings) {
          const notifSettings = eXo.chat.desktopNotificationSettings;
          this.$refs.notifyDonotdistrub.checked = this.chatPreferences.notifyDonotdistrub = notifSettings.preferredNotificationTrigger.indexOf(NOT_DISTRUB_NOTIF) < 0 ? false : true;
          this.$refs.notifyOnSite.checked = this.chatPreferences.notifyOnSite = notifSettings.preferredNotification.indexOf(ON_SITE_NOTIF) < 0 ? false : true;
          this.$refs.notifyDesktop.checked = this.chatPreferences.notifyDesktop = notifSettings.preferredNotification.indexOf(DESKTOP_NOTIF) < 0 ? false : true;
          this.$refs.notifyBip.checked = this.chatPreferences.notifyBip = notifSettings.preferredNotification.indexOf(BIP_NOTIF) < 0 ? false : true;
        }
        this.originalChatPreferences = JSON.parse(JSON.stringify(this.chatPreferences));

        if(!this.checkboxesEnhanced) {
          $('#chatPreferences :checkbox').iphoneStyle({
            disabledClass: 'switchBtnDisabled',
            containerClass: 'uiSwitchBtn',
            labelOnClass: 'switchBtnLabelOn',
            labelOffClass: 'switchBtnLabelOff',
            handleClass: 'switchBtnHandle',
          });
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

      this.chatPreferences.notifyDonotdistrub = this.$refs.notifyDonotdistrub.checked;
      this.chatPreferences.notifyDesktop = this.$refs.notifyDesktop.checked;
      this.chatPreferences.notifyOnSite = this.$refs.notifyOnSite.checked;
      this.chatPreferences.notifyBip = this.$refs.notifyBip.checked;

      if (this.originalChatPreferences.notifyDonotdistrub !== this.chatPreferences.notifyDonotdistrub) {
        preferredNotificationTrigger.push(NOT_DISTRUB_NOTIF);
      }
      if (this.originalChatPreferences.notifyDesktop !== this.chatPreferences.notifyDesktop) {
        preferredNotification.push(DESKTOP_NOTIF);
      }
      if (this.originalChatPreferences.notifyOnSite !== this.chatPreferences.notifyOnSite) {
        preferredNotification.push(ON_SITE_NOTIF);
      }
      if (this.originalChatPreferences.notifyBip !== this.chatPreferences.notifyBip) {
        preferredNotification.push(BIP_NOTIF);
      }

      if(preferredNotificationTrigger.length || preferredNotification.length) {
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
