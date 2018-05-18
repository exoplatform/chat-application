<template>
  <div id="chatApplicationContainer">
    <div class="uiLeftContainerArea">
      <div class="userDetails">
        <chat-contact :user-name="userSettings.username" :name="userSettings.fullName" :status="userSettings.status" type="u" @exo-chat-status-changed="setStatus($event)"></chat-contact>
      </div>
      <chat-contact-list :contacts="contactList" :selected="selectedContact" :user-settings="userSettings" @exo-chat-contact-selected="setSelectedContact($event)"></chat-contact-list>
    </div>
    <div class="uiGlobalRoomsContainer">
      <chat-room-detail v-if="Object.keys(selectedContact).length !== 0" :contact="selectedContact"></chat-room-detail>
      <div class="room-content">
        <div class="uiRightContainerArea">
          <chat-message-list :contact="selectedContact" :user-settings="userSettings"></chat-message-list>
          <chat-message-composer :contact="selectedContact"></chat-message-composer>
        </div>
        <chat-room-participants :contact="selectedContact" :user-settings="userSettings"></chat-room-participants> 
      </div>
    </div>
  </div>
</template>

<script>
import * as chatServices from '../chatServices';
import * as chatWebStorage from '../chatWebStorage';
import ChatContact from './ChatContact.vue';
import ChatContactList from './ChatContactList.vue';
import ChatRoomParticipants from './ChatRoomParticipants.vue';
import ChatRoomDetail from './ChatRoomDetail.vue';
import ChatMessageComposer from './ChatMessageComposer.vue';
import ChatMessageList from './ChatMessageList.vue';
export default {
  components: {
    'chat-contact': ChatContact,
    'chat-contact-list': ChatContactList,
    'chat-room-participants': ChatRoomParticipants,
    'chat-room-detail': ChatRoomDetail,
    'chat-message-composer': ChatMessageComposer,
    'chat-message-list': ChatMessageList
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
      selectedContact: {}
    };
  },
  created() {
    chatServices.initChatSettings(this.userSettings.username, chatRoomsData => this.initChatRooms(chatRoomsData), userSettings => this.initSettings(userSettings));
    document.addEventListener('exo-chat-room-updated', this.roomUpdated);
    document.addEventListener('exo-chat-logout-sent', () => {
      if (!window.chatNotification.isConnected()) {
        this.changeUserStatusToAway();
      }
      // TODO Display popin for disconnection
      // + Change user status
      // + Change messages sent color (use of local storage)
      // + Display Session expired
    });
    document.addEventListener('exo-chat-disconnected', () => {
      this.changeUserStatusToAway();
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
    changeUserStatusToAway() {
      if (this.userSettings && this.userSettings.status && !this.userSettings.originalStatus) {
        this.userSettings.originalStatus = this.userSettings.status;
      }
      this.userSettings.status = 'away';
    }
  }
};
</script>
