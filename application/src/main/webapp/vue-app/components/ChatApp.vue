<template src="./chatApp.html"></template>

<script>
import {chatData} from '../chatData'
import {getUser, getUserStatus, getChatRooms} from '../chatServices'
import ChatContact from './ChatContact.vue'
import ChatContactList from './ChatContactList.vue'
import ChatRoomParticipants from './ChatRoomParticipants.vue'
export default {
  components: {
    "chat-contact": ChatContact,
    "chat-contact-list": ChatContactList,
    "chat-room-participants": ChatRoomParticipants
  },
  data() {
    return {
      contactList: [],
      chatData: {
        db: "chat",
        token: "979412bd16bee4627624db838ee489f289d27000"
      },
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
    getUser(this.currentUser.name).then(user => {
      this.currentUser.fullName = user.fullname;
      this.currentUser.avatar = user.avatar;
      this.currentUser.profileLink = user.profile;
    });
    getUserStatus(this.currentUser.name).then(usersStatus => {
      this.currentUser.status = usersStatus[this.currentUser.name] ? "online" : "offline";
    });
    getChatRooms().then(rooms => {
      console.log('Contact List: ', rooms)
      this.contactList = rooms;
    })
    /*fetch(`/chatServer/getStatus`, {
      credentials: 'include', 
      headers: {
        'Authorization': 'Bearer ' + this.chatData.token
      },data: {
        "user": "root",
        "targetUser": "root",
        "dbName": this.chatData.dbName
      }})
      .then(resp => resp.json())
      .then(usersStatus => {
        
        console.log(usersStatus);
      });*/
  }
};
</script>