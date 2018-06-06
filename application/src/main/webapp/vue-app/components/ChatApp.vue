<template>
  <div id="chatApplicationContainer" :class="{'online': connected, 'offline': !connected, 'show-conversation': showMobileConversations}">
    <div class="uiLeftContainerArea">
      <div class="userDetails">
        <chat-contact :user-name="userSettings.username" :name="userSettings.fullName" :status="userSettings.status" :is-current-user="true" type="u" @exo-chat-status-changed="setStatus($event)">
          <div v-exo-tooltip.right="$t('exoplatform.chat.settings.button.tip')" v-if="mq !== 'mobile'" class="chat-user-settings" @click="openSettingModal"><i class="uiIconGear"></i></div>
        </chat-contact>
        <div v-if="mq === 'mobile'" class="discussion-label">{{ $t('exoplatform.chat.discussion') }}</div>
      </div>
      <chat-contact-list :contacts="contactList" :selected="selectedContact" :is-searching-contact="isSearchingContact" @load-more-contacts="loadMoreContacts" @search-contact="searchContacts" @contact-selected="setSelectedContact" @refresh-contats="refreshContacts($event)"></chat-contact-list>
    </div>
    <div v-show="(selectedContact && selectedContact.room) || mq === 'mobile'" class="uiGlobalRoomsContainer">
      <chat-room-detail v-if="Object.keys(selectedContact).length !== 0" :contact="selectedContact"></chat-room-detail>
      <div class="room-content">
        <chat-message-list :contact="selectedContact"></chat-message-list>
        <chat-room-participants :contact="selectedContact" @exo-chat-particpants-loaded="setContactParticipants($event)"></chat-room-participants> 
      </div>
    </div>
    <div v-if="mq !== 'mobile' && !(selectedContact && selectedContact.room)" class="chat-no-conversation muted">
      <span class="text">{{ $t('exoplatform.chat.no.conversation') }}</span>
    </div>
    <global-notification-modal :show="settingModal" @close-modal="settingModal = false"></global-notification-modal>
    <modal v-show="loggedout" :title="$t('exoplatform.chat.timeout.title')" display-close="false" modal-class="logout-popup">
      <div class="modal-body">
        {{ $t('exoplatform.chat.timeout.description') }}
      </div>
      <div class="uiAction uiActionBorder">
        <a href="#" class="btn btn-primary" @click="reloadPage">
          {{ $t('exoplatform.chat.timeout.login') }}
        </a>
      </div>
    </modal>
    <div v-if="!connected" class="chat-loading-mask"><img src="/chat/img/sync.gif" class="chat-loading"></div>
    <div class="hide">
      <audio id="chat-audio-notif" controls>
        <source src="/chat/audio/notif.wav">
        <source src="/chat/audio/notif.mp3">
        <source src="/chat/audio/notif.ogg">
      </audio>
    </div>
  </div>
</template>

<script>
import * as chatServices from '../chatServices';
import * as chatWebStorage from '../chatWebStorage';
import * as chatWebSocket from '../chatWebSocket';

import Modal from './modal/Modal.vue';
import ChatContact from './ChatContact.vue';
import ChatContactList from './ChatContactList.vue';
import ChatRoomParticipants from './ChatRoomParticipants.vue';
import ChatRoomDetail from './ChatRoomDetail.vue';
import ChatMessageList from './ChatMessageList.vue';
import GlobalNotificationModal from './modal/GlobalNotificationModal.vue';

export default {
  components: {
    'modal': Modal,
    'chat-contact': ChatContact,
    'chat-contact-list': ChatContactList,
    'chat-room-participants': ChatRoomParticipants,
    'chat-room-detail': ChatRoomDetail,
    'chat-message-list': ChatMessageList,
    'global-notification-modal': GlobalNotificationModal
  },
  data() {
    return {
      contactList: [],
      userSettings: {
        username: typeof eXo !== 'undefined' ? eXo.env.portal.userName : 'root'
      },
      connected: false,
      loggedout: false,
      selectedContact: {},
      isSearchingContact: false,
      settingModal: false,
      conversationArea: false
    };
  },
  computed: {
    showMobileConversations() {
      return this.mq === 'mobile' && this.conversationArea === true ? true : false;
    }
  },
  created() {
    chatServices.initChatSettings(this.userSettings.username,
      userSettings => this.initSettings(userSettings),
      chatRoomsData => this.initChatRooms(chatRoomsData));
    document.addEventListener('exo-chat-room-updated', this.roomUpdated);
    document.addEventListener('exo-chat-logout-sent', () => {
      if (!chatWebSocket.isConnected()) {
        this.changeUserStatusToOffline();
        this.loggedout = true;
      }
    });
    document.addEventListener('exo-chat-disconnected', this.changeUserStatusToOffline);
    document.addEventListener('exo-chat-connected', this.connectionEstablished);
    document.addEventListener('exo-chat-reconnected', this.connectionEstablished);
    document.addEventListener('exo-chat-user-status-changed', (e) => {
      const contactChanged = e.detail;
      if (this.userSettings.username === contactChanged.sender) {
        this.userSettings.status = contactChanged.status ? contactChanged.status : contactChanged.data ? contactChanged.data.status : null;
        this.userSettings.originalStatus = this.userSettings.status;
      }
    });
    document.addEventListener('exo-chat-notification-count-updated', (e) => {
      const totalUnreadMsg = e.detail ? e.detail.data.totalUnreadMsg : e.totalUnreadMsg;
      chatServices.updateTotalUnread(totalUnreadMsg);
    });
  },
  destroyed() {
    document.removeEventListener('exo-chat-disconnected', this.changeUserStatusToOffline);
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
      const thiss = this;
      if(this.userSettings.offlineDelay) {
        setInterval(
          function() {thiss.refreshContacts(true);},
          this.userSettings.offlineDelay);
      }
    },
    initChatRooms(chatRoomsData) {
      this.addRooms(chatRoomsData.rooms);

      if (this.mq !== 'mobile') {
        const selectedRoom = chatWebStorage.getStoredParam(chatWebStorage.LAST_SELECTED_ROOM_PARAM);
        if(selectedRoom) {
          this.setSelectedContact(selectedRoom);
        }
      }

      const totalUnreadMsg = Math.abs(chatRoomsData.unreadOffline) + Math.abs(chatRoomsData.unreadOnline) + Math.abs(chatRoomsData.unreadSpaces) + Math.abs(chatRoomsData.unreadTeams);
      chatServices.updateTotalUnread(totalUnreadMsg);
    },
    setSelectedContact(selectedContact) {
      if(this.mq === 'mobile') {
        this.conversationArea = true;
      }
      if(!selectedContact) {
        selectedContact = {};
      }
      if (typeof selectedContact === 'string') {
        selectedContact = this.contactList.find(contact => contact.room === selectedContact);
      }
      if (selectedContact && selectedContact.room && selectedContact.fullName) {
        const indexOfRoom = this.contactList.findIndex(contact => contact.room === selectedContact.room);
        if(indexOfRoom < 0) {
          this.contactList.unshift(selectedContact);
        } else {
          this.contactList.splice(indexOfRoom, 1, selectedContact);
        }
        this.selectedContact = selectedContact;
        document.dispatchEvent(new CustomEvent('exo-chat-selected-contact-changed', {'detail' : selectedContact}));
      }
    },
    setStatus(status) {
      if (chatWebSocket && chatWebSocket.isConnected()) {
        chatWebSocket.setStatus(status, newStatus => {this.userSettings.status = newStatus; this.userSettings.originalStatus = newStatus;});
      }
    },
    roomUpdated(e) {
      const updatedContact = e.detail && e.detail.data ? e.detail.data : null;
      if (updatedContact && updatedContact.room) {
        const indexOfRoom = this.contactList.findIndex(contact => contact.room === updatedContact.room);
        if(indexOfRoom < 0) {
          this.contactList.unshift(updatedContact);
        } else {
          this.contactList.splice(indexOfRoom, 1, updatedContact);
        }
      }
    },
    connectionEstablished() {
      eXo.chat.isOnline = true;
      this.connected = true;
      if (this.userSettings.originalStatus !== this.userSettings.status) {
        this.setStatus(this.userSettings.originalStatus);
      } else if (this.userSettings && this.userSettings.originalStatus) {
        this.userSettings.status = this.userSettings.originalStatus;
      }
    },
    addRooms(rooms) {
      const contacts = this.contactList.slice(0);
      rooms = rooms.filter(contact => contact.fullName
        && contact.fullName.trim().length > 0
        && contact.room && contact.room.trim().length > 0
        && !contacts.find(otherContact => otherContact.room === contact.room));
      if(rooms && rooms.length > 0) {
        rooms.forEach(room => {
          this.contactList.push(room);
        });
      }
    },
    loadMoreContacts(nbPages) {
      this.isSearchingContact = true;
      chatServices.getOnlineUsers().then(users => {
        chatServices.getChatRooms(this.userSettings, users, '', nbPages).then(chatRoomsData => {
          this.addRooms(chatRoomsData.rooms);
          this.isSearchingContact = false;
        });
      });
    },
    searchContacts(term) {
      this.isSearchingContact = true;
      chatServices.getOnlineUsers().then(users => {
        chatServices.getChatRooms(this.userSettings, users, term).then(chatRoomsData => {
          this.addRooms(chatRoomsData.rooms);
          this.isSearchingContact = false;
        });
      });
    },
    refreshContacts(keepSelectedContact) {
      chatServices.getOnlineUsers().then(users => {
        chatServices.getChatRooms(this.userSettings, users).then(chatRoomsData => {
          this.addRooms(chatRoomsData.rooms);
          if (!keepSelectedContact && this.selectedContact) {
            const contactToChange = this.contactList.find(contact => contact.room === this.selectedContact.room);
            if(contactToChange) {
              this.setSelectedContact(contactToChange);
            }
          }
        });
      });
    },
    changeUserStatusToOffline() {
      if (this.userSettings && this.userSettings.status && !this.userSettings.originalStatus) {
        this.userSettings.originalStatus = this.userSettings.status;
      }
      eXo.chat.isOnline = false;
      this.connected = false;
    },
    openSettingModal() {
      this.settingModal = true;
    },
    setContactParticipants(participants) {
      this.selectedContact.participants = participants;
      document.dispatchEvent(new CustomEvent('exo-chat-participants-loaded', {'detail' : this.selectedContact}));
    },
    reloadPage() {
      window.location.reload();
    }
  }
};
</script>
<style>
#PlatformAdminToolbarContainer {
  display: none;
}
</style>
