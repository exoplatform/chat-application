<template>
  <div id="chatApplicationContainer">
    <div class="uiLeftContainerArea">
      <div class="userDetails">
        <chat-contact :user-name="userSettings.username" :name="userSettings.fullName" :status="userSettings.status" type="u"></chat-contact>
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
    chatServices.initChatSettings(this.userSettings.username, chatRoomsData => this.contactList = chatRoomsData.rooms, userSettings => this.userSettings = userSettings);
    document.addEventListener('exo-chat-room-updated', this.roomUpdated);
    document.addEventListener('exo-chat-logout-sent', () => {
      // TODO Display popin for disconnection
      // + Change user status
      // + Change messages sent color (use of local storage)
      // + Display Session expired
    });
    document.addEventListener('exo-chat-disconnected', () => {
      // TODO Display popin for disconnection
      // + Change user status
      // + Change messages sent color (use of local storage)
    });
    document.addEventListener('exo-chat-user-status-changed', (e) => {
      const contactChanged = e.detail;
      if (this.userSettings.username === contactChanged.name) {
        this.userSettings.status = contactChanged.status;
      }
    });
  },
  destroyed() {
    document.addEventListener('exo-chat-room-updated', this.roomUpdated);
    // TODO remove added listeners
  },
  methods: {
    setSelectedContact(contact) {
      this.selectedContact = contact;
      document.dispatchEvent(new CustomEvent('exo-chat-selected-contact-changed', {'detail' : contact}));
    },
    roomUpdated(message) {
      const contactToUpdate = this.contactList.find(contact => contact.room === message.room);
      if (contactToUpdate) {
        contactToUpdate.fullName = message.data.title;
        if (this.selectedContact.room === message.room) {
          // Refresh room
          this.setSelectedContact(contactToUpdate);
        }
      }
    }
  }
};
</script>
