<template>
  <div id="chatApplicationContainer">
    <div class="uiLeftContainerArea">
      <div class="userDetails">
        <chat-contact :user-name="userSettings.username" :name="userSettings.fullName" :status="userSettings.status" type="u"></chat-contact>
      </div>
      <chat-contact-list :contacts="contactList" :selected="selectedContact" @exo-chat-contact-selected="setSelectedContact($event)"></chat-contact-list>
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
  },
  methods: {
    setSelectedContact(contact) {
      this.selectedContact = contact;
      document.dispatchEvent(new CustomEvent('exo-chat-contact-changed', {'detail' : contact}));
    }
  }
};
</script>
