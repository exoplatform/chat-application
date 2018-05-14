<template>
  <div id="chatApplicationContainer">
    <div class="uiLeftContainerArea">
      <div class="userDetails">
        <chat-contact :user-name="currentUser.name" :name="currentUser.fullName" :status="currentUser.status" type="u"></chat-contact>
      </div>
      <chat-contact-list :contacts="contactList" :selected="selectedContact" @exo-chat-contact-selected="setSelectedContact($event)"></chat-contact-list>
    </div>
    <div v-if="selectedContact" class="uiGlobalRoomsContainer">
      <chat-room-detail :room="selectedContact"></chat-room-detail> 
      <div class="room-content">
        <div class="uiRightContainerArea">
          <div id="chats"></div>
          <chat-message :room="selectedContact"></chat-message>
        </div>
        <div v-if="selectedContact.type && selectedContact.type != 'u'" class="uiRoomUsersContainerArea">
          <chat-room-participants :participants="roomParticipants"></chat-room-participants> 
        </div>
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
import ChatMessage from './ChatMessage.vue';
export default {
  components: {
    'chat-contact': ChatContact,
    'chat-contact-list': ChatContactList,
    'chat-room-participants': ChatRoomParticipants,
    'chat-room-detail': ChatRoomDetail,
    'chat-message': ChatMessage
  },
  data() {
    return {
      contactList: [],
      roomParticipants: [],
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
      this.currentUser.avatar = (user.avatar == null) ? chatData.socialUserAPI + user.username + '/avatar' : user.avatar;
      this.currentUser.profileLink = user.href;
    });
    
    chatServices.getUserSettings(this.currentUser.name).then(userSettings => {
      this.userSettings = userSettings;
      document.dispatchEvent(new CustomEvent('exo-chat-settings-loaded', {'detail' : userSettings}));
    });
  },
  methods: {
    setSelectedContact(contact) {
      if (this.selectedContact && this.selectedContact.room === contact.room) {
        return;
      }
      this.selectedContact = contact;
      if (contact.type != 'u') {
        this.initRoom(contact);
      }
      //console.log(this.selectedContact)
    },
    initRoom(room) {
      chatServices.getRoomParticipants(this.userSettings, room).then( data => {
        //console.log('Room participants:', data);
        this.roomParticipants = data.users;
      });
    }
  }
};
</script>
