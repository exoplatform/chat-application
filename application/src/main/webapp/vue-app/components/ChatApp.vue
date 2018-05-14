<template src="./chatApp.html"></template>

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
        console.log('Current room:', this.selectedContact);
      });
    }
  }
  ,
  created() {
    chatServices.initServerChannel();

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
