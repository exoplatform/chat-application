<template>
  <div id="chatApplicationContainer">
    <div class="uiLeftContainerArea">
      <div class="userDetails">
        <chat-contact :user-name="currentUser.name" :name="currentUser.fullName" :status="currentUser.status" type="u"></chat-contact>
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
import {chatData} from '../chatData';
import * as chatNotification from '../ChatNotification';
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
      userSettings: {},
      currentUser: {
        name: typeof eXo !== 'undefined' ? eXo.env.portal.userName : 'root',
        fullName:'',
        avatar: '',
        profileLink: '',
        status: ''
      },
      selectedContact: {}
    };
  },
  created() {
    chatNotification.initCometD();

    document.addEventListener('exo-chat-settings-loaded', (e) => {
      chatServices.getOnlineUsers().then(users => { // Fetch online users
        chatServices.getChatRooms(e.detail, users).then(data => {
          this.contactList = data.rooms;
        });
      });
      chatServices.getUserStatus(e.detail, this.currentUser.name).then(usersStatus => {
        this.currentUser.status = usersStatus;
      });
    });

    chatServices.getUser(this.currentUser.name).then(user => {
      this.currentUser.fullName = user.fullname;
      this.currentUser.avatar = user.avatar == null ? `${chatData.socialUserAPI}${user.username}/avatar` : user.avatar;
      this.currentUser.profileLink = user.href;
    });
    
    chatServices.getUserSettings(this.currentUser.name).then(userSettings => {
      this.userSettings = userSettings;
      document.dispatchEvent(new CustomEvent('exo-chat-settings-loaded', {'detail' : userSettings}));
    });
  },
  methods: {
    setSelectedContact(contact) {
      this.selectedContact = contact;
      document.dispatchEvent(new CustomEvent('exo-chat-contact-changed', {'detail' : contact}));
    }
  }
};
</script>
