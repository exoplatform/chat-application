<template>
  <div id="chatApplicationContainer">
    <div class="uiLeftContainerArea">
      <div class="userDetails">
        <chat-contact :user-name="userSettings.username" :name="userSettings.fullName" :status="userSettings.status" :is-current-user="true" type="u" @exo-chat-status-changed="setStatus($event)">
          <div class="chat-user-settings" @click="openSettingModal"><i class="uiIconGear"></i></div>
        </chat-contact>
      </div>
      <chat-contact-list :contacts="contactList" :selected="selectedContact" @exo-chat-contact-selected="setSelectedContact($event)"></chat-contact-list>
    </div>
    <div class="uiGlobalRoomsContainer">
      <chat-room-detail v-if="Object.keys(selectedContact).length !== 0" :contact="selectedContact"></chat-room-detail>
      <div class="room-content">
        <chat-message-list :contact="selectedContact"></chat-message-list>
        <chat-room-participants :contact="selectedContact"></chat-room-participants> 
      </div>
    </div>
    <modal v-show="settingModal" id="chatPreferences" title="Chat Preferences" modal-class="chatPreferences" @modal-closed="settingModal = false">
      <section>
        <h4>Notifications</h4>
        <div class="notification-item">
          <input v-model="chatPreferences.notifyDonotdistrub" ref="notifyDonotdistrub" type="checkbox">
          <div class="notification-description">
            <b>Notifications "Ne pas Déranger"</b>
            <em>M'avertir même lorsque je suis en "Ne pas Déranger"</em>
          </div>
        </div>
      </section>
      <section>
        <h4>Notify me with</h4>
        <div class="notification-item">
          <input v-model="chatPreferences.notifyDesktop" ref="notifyDesktop" type="checkbox">
          <div class="notification-description">
            <b>Notifications sur le bureau</b>
            <em>Afficher une notification toast sur votre ordinateur</em>
          </div>
        </div>
        <div class="notification-item">
          <input v-model="chatPreferences.notifyOnSite" ref="notifyOnSite" type="checkbox">
          <div class="notification-description">
            <b>Notifications sur site</b>
            <em>Afficher un compteur sur l'icône dans la barre du haut</em>
          </div>
        </div>
        <div class="notification-item">
          <input v-model="chatPreferences.notifyBip" ref="notifyBip" type="checkbox">
          <div class="notification-description">
            <b>Bips sonores</b>
            <em>Émettre un signal sonore chaque fois qu’un nouveau message arrive</em>
          </div>
        </div>
      </section>
      <div class="uiAction uiActionBorder">
        <div class="btn btn-primary" @click="saveNotificationSettings">Enregistrer</div>
        <div class="btn" @click="settingModal = false">Annuler</div>
      </div>
    </modal>
  </div>
</template>

<script>
import * as chatServices from '../chatServices';
import * as chatWebStorage from '../chatWebStorage';
import ChatContact from './ChatContact.vue';
import ChatContactList from './ChatContactList.vue';
import ChatRoomParticipants from './ChatRoomParticipants.vue';
import ChatRoomDetail from './ChatRoomDetail.vue';
import ChatMessageList from './ChatMessageList.vue';
import Modal from './Modal.vue';

const ON_SITE_NOTIF = 'on-site';
const DESKTOP_NOTIF = 'desktop';
const BIP_NOTIF = 'bip';
const NOT_DISTRUB_NOTIF = 'notify-even-not-distrub';

export default {
  components: {
    'chat-contact': ChatContact,
    'chat-contact-list': ChatContactList,
    'chat-room-participants': ChatRoomParticipants,
    'chat-room-detail': ChatRoomDetail,
    'chat-message-list': ChatMessageList,
    Modal
  },
  data() {
    return {
      contactList: [],
      userSettings: {
        username: typeof eXo !== 'undefined' ? eXo.env.portal.userName : 'root',
        token: null,
        fullName: null,
        status: null,
        isOnline: false,
        cometdToken: null,
        dbName: null,
        sessionId: null,
        serverURL: null,
        standalone: false,
        chatPage: null,
        wsEndpoint: null,
      },
      selectedContact: {},
      settingModal: false,
      chatPreferences: {
        notifyDonotdistrub: false,
        notifyDesktop: false,
        notifyOnSite: false,
        notifyBip: false
      }
    };
  },
  created() {
    chatServices.initChatSettings(this.userSettings.username, chatRoomsData => this.initChatRooms(chatRoomsData), userSettings => this.initSettings(userSettings));
    document.addEventListener('exo-chat-room-updated', this.roomUpdated);
    document.addEventListener('exo-chat-logout-sent', () => {
      if (!window.chatNotification.isConnected()) {
        this.changeUserStatusToOffline();
      }
      // TODO Display popin for disconnection
      // + Change user status
      // + Change messages sent color (use of local storage)
      // + Display Session expired
    });
    document.addEventListener('exo-chat-disconnected', () => {
      this.changeUserStatusToOffline();
      // TODO Display popin for disconnection
      // + Change user status
      // + Change messages sent color (use of local storage)
    });
    document.addEventListener('exo-chat-connected', this.connectionEstablished);
    document.addEventListener('exo-chat-reconnected', this.connectionEstablished);
    document.addEventListener('exo-chat-user-status-changed', (e) => {
      const contactChanged = e.detail;
      if (this.userSettings.username === contactChanged.name) {
        this.userSettings.status = contactChanged.status;
        this.userSettings.originalStatus = contactChanged.status;
      }
    });
  },
  destroyed() {
    document.removeEventListener('exo-chat-connected', this.connectionEstablished);
    document.removeEventListener('exo-chat-reconnected', this.connectionEstablished);
    document.removeEventListener('exo-chat-room-updated', this.roomUpdated);
    // TODO remove added listeners
  },
  methods: {
    initSettings(userSettings) {
      this.userSettings = userSettings;
      // Trigger that the new status has been loaded
      this.setStatus(this.userSettings.status);
      if(this.userSettings.offilineDelay) {
        setInterval(
          this.refreshContacts,
          this.userSettings.offilineDelay);
      }
    },
    initChatRooms(chatRoomsData) {
      this.contactList = chatRoomsData.rooms.reduce(function(prev, curr) {
        return curr.fullName ? [...prev, curr] : prev;
      }, []);

      const selectedRoom = chatWebStorage.getStoredParam(chatWebStorage.LAST_SELECTED_ROOM_PARAM);
      if(selectedRoom) {
        this.setSelectedContact(selectedRoom);
      }
    },
    setSelectedContact(selectedContact) {
      if (typeof selectedContact === 'string') {
        selectedContact = this.contactList.find(contact => contact.room === selectedContact);
      }
      if (selectedContact) {
        this.selectedContact = selectedContact;
        document.dispatchEvent(new CustomEvent('exo-chat-selected-contact-changed', {'detail' : selectedContact}));
      }
    },
    setStatus(status) {
      if (window.chatNotification && window.chatNotification.isConnected()) {
        window.chatNotification.setStatus(status, newStatus => {this.userSettings.status = newStatus; this.userSettings.originalStatus = newStatus;});
      }
    },
    roomUpdated() {
      this.refreshContacts();
    },
    connectionEstablished() {
      if (this.userSettings.originalStatus !== this.userSettings.status) {
        this.setStatus(this.userSettings.originalStatus);
      } else if (this.userSettings && this.userSettings.originalStatus) {
        this.userSettings.status = this.userSettings.originalStatus;
      }
    },
    refreshContacts() {
      chatServices.getOnlineUsers().then(users => {
        chatServices.getChatRooms(this.userSettings, users).then(chatRoomsData => {
          this.contactList = chatRoomsData.rooms.reduce(function(prev, curr) {
            return curr.fullName ? [...prev, curr] : prev;
          }, []);
          if (this.selectedContact) {
            const contactToChange = this.contactList.find(contact => contact.name === this.selectedContact.name);
            this.setSelectedContact(contactToChange);
          }
        });
      });
    },
    changeUserStatusToOffline() {
      if (this.userSettings && this.userSettings.status && !this.userSettings.originalStatus) {
        this.userSettings.originalStatus = this.userSettings.status;
      }
      this.userSettings.status = 'offline';
    },
    openSettingModal() {
      this.settingModal = true;
      console.log(eXo.chat.desktopNotificationSettings);
      if (eXo && eXo.chat && eXo.chat.desktopNotificationSettings) {
        const notifSettings = eXo.chat.desktopNotificationSettings;
        this.chatPreferences.notifyDonotdistrub = notifSettings.preferredNotificationTrigger.indexOf(NOT_DISTRUB_NOTIF) < 0 ? false : true;
        this.chatPreferences.notifyOnSite = notifSettings.preferredNotification.indexOf(ON_SITE_NOTIF) < 0 ? false : true;
        this.chatPreferences.notifyDesktop = notifSettings.preferredNotification.indexOf(DESKTOP_NOTIF) < 0 ? false : true;
        this.chatPreferences.notifyBip = notifSettings.preferredNotification.indexOf(BIP_NOTIF) < 0 ? false : true;
      }
      window.require(['SHARED/iphoneStyleCheckbox'], function() {
        $('#chatPreferences :checkbox').iphoneStyle({
          disabledClass: 'switchBtnDisabled',
          containerClass: 'uiSwitchBtn',
          labelOnClass: 'switchBtnLabelOn',
          labelOffClass: 'switchBtnLabelOff',
          handleClass: 'switchBtnHandle'
        });
      });
    },
    saveNotificationSettings() {
      const notifSettings = eXo.chat.desktopNotificationSettings;
      const userSettings = eXo.chat.userSettings;
      
      if (this.$refs.notifyDonotdistrub.checked !== notifSettings.preferredNotificationTrigger.indexOf(NOT_DISTRUB_NOTIF) < 0 ? false : true) {
        chatServices.setUserNotificationTrigger(userSettings, NOT_DISTRUB_NOTIF);
      }
      if (this.$refs.notifyDesktop.checked !== notifSettings.preferredNotification.indexOf(DESKTOP_NOTIF) < 0 ? false : true) {
        chatServices.setUserPreferredNotification(userSettings, DESKTOP_NOTIF);
      }
      if (this.$refs.notifyBip.checked !== notifSettings.preferredNotification.indexOf(BIP_NOTIF) < 0 ? false : true) {
        chatServices.setUserPreferredNotification(userSettings, BIP_NOTIF);
      }
      if (this.$refs.notifyOnSite.checked !== notifSettings.preferredNotification.indexOf(ON_SITE_NOTIF) < 0 ? false : true) {
        chatServices.setUserPreferredNotification(userSettings, ON_SITE_NOTIF);
      }
    }
  }
};
</script>
