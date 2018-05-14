<template src="./chatApp.html"></template>

<script>
import {chatData} from '../chatData'
import {getUser, getUserStatus, getChatRooms, getUserSettings, getOnlineUsers, initServerChannel} from '../chatServices'
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
      this.selectedContact = contact;
      this.selectedContact.avatar = chatData.socialUserAPI + contact.user + '/avatar';
      console.log(this.selectedContact)
    }
  }
  ,
  created () {
    var thizz = this;
    document.addEventListener('exo-chat-settings-loaded', function(e) {
      getChatRooms(e.detail, thizz.onlineUsers).then(onlineStatus => {
        console.log('Contact List: ', onlineStatus)
        thizz.contactList = onlineStatus.rooms;
      })
    });

    initServerChannel();

    getUser(this.currentUser.name).then(user => {
      this.currentUser.fullName = user.fullname;
      this.currentUser.avatar = user.avatar;
      this.currentUser.profileLink = user.profile;
    });
    getUserStatus(this.currentUser.name).then(usersStatus => {
      this.currentUser.status = usersStatus[this.currentUser.name] ? 'online' : 'offline';
    });
    getUserSettings(this.currentUser.name).then(userSettings => {
      this.userSettings = userSettings;
      document.dispatchEvent(new CustomEvent('exo-chat-settings-loaded', {"detail" : userSettings}));
    });
  }
};
</script>
