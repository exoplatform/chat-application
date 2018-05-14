<template>
  <div id="chatApplicationContainer">
    <div class="uiLeftContainerArea">
      <div class="userDetails">
        <chat-contact type="u" :avatar="currentUser.avatar" :name="currentUser.fullName" :status="currentUser.status"></chat-contact>
      </div>
      <chat-contact-list :contacts="contactList" v-on:exo-chat-contact-selected="setSelectedContact($event)" :selected="selectedContact"></chat-contact-list>
    </div>
    <div class="uiGlobalRoomsContainer" v-if="selectedContact">
      <chat-room-detail :room="selectedContact"></chat-room-detail> 
      <div class="room-content">
        <div class="uiRightContainerArea">
          <div id="chats"></div>
          <chat-message></chat-message>
        </div>
        <div class="uiRoomUsersContainerArea" v-if="selectedContact.type && selectedContact.type != 'u'">
          <chat-room-participants :participants="roomParticipants"></chat-room-participants> 
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {chatData} from '../chatData'
import * as chatServices from '../chatServices'
import ChatContact from './ChatContact.vue'
import ChatContactList from './ChatContactList.vue'
import ChatRoomParticipants from './ChatRoomParticipants.vue'
import ChatRoomDetail from './ChatRoomDetail.vue'
import ChatMessage from './ChatMessage.vue'
export default {
  components: {
    "chat-contact": ChatContact,
    "chat-contact-list": ChatContactList,
    "chat-room-participants": ChatRoomParticipants,
    "chat-room-detail": ChatRoomDetail,
    "chat-message": ChatMessage
  },
  data() {
    return {
      contactList: [],
      roomParticipants: [],
      userSettings: {},
      currentUser: {
        name: typeof eXo !== 'undefined' ? eXo.env.portal.userName : "root",
        fullName:"",
        avatar: "",
        profileLink: "",
        status: ""
      },
      selectedContact: false
    };
  },
  methods: {
    setSelectedContact(contact) {
      if (this.selectedContact.room === contact.room) {
        return
      }
      this.selectedContact = contact;
      this.selectedContact.avatar = chatData.socialUserAPI + contact.user + '/avatar'; // TODO fix space avatar and move to contact component
      if (contact.type != "u") {
        this.initRoom(contact);
      }
      console.log(this.selectedContact)
    },
    initRoom(room) {
      chatServices.getRoomParticipants(this.userSettings, room).then( data => {
        console.log('Room participants:', data);
        this.roomParticipants = data.users;
      });
    }
  }
  ,
  created() {
    //chatServices.initServerChannel();

    document.addEventListener('exo-chat-settings-loaded', (e) => {
      this.userSettings = e.detail;
      chatServices.getOnlineUsers().then(users => { // Fetch online users
      console.log(users);
        chatServices.getChatRooms(e.detail, users).then(data => {
          console.log('Contact List: ', data)
          this.contactList = data.rooms;
        })
      });
    });

    chatServices.getUser(this.currentUser.name).then(user => {
      this.currentUser.fullName = user.fullname;
      this.currentUser.avatar = user.avatar;
      this.currentUser.profileLink = user.profile;
    });
    chatServices.getUserStatus(this.currentUser.name).then(usersStatus => {
      this.currentUser.status = usersStatus[this.currentUser.name] ? 'online' : 'offline';
    });
    chatServices.getUserSettings(this.currentUser.name).then(userSettings => {
      this.userSettings = userSettings;
      document.dispatchEvent(new CustomEvent('exo-chat-settings-loaded', {"detail" : userSettings}));
    });
  }
};
</script>
