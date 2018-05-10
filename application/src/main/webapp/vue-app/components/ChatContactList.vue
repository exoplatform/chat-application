<template>
  <div class="contactListContainer">
    <div class="listHeader">
      
    </div>
    <div class="contactList">
      <div class="contact-list-item isList" v-for="contact in contacts" :key="contact.user" @click="selectContact(contact)" :class="{selected: selected.user == contact.user }">
        <chat-contact :list="true" :type="contact.type" :avatar="getContactAvatar(contact.user)" :name="contact.fullName" :status="contact.status"></chat-contact>
        <div v-show="contact.unreadTotal > 0" class="unreadMessages">{{contact.unreadTotal}}</div>
        <div class="uiIcon favorite" :class="{'is-fav': contact.isFavorite}" @click.stop="toggleFavorite(contact)"></div>
      </div>
    </div>
  </div>
</template>

<script>
import {chatData} from '../chatData'
import ChatContact from './ChatContact.vue'
export default {
  components: {ChatContact},
  props: ['contacts','selected'],
  data : function() {
    return {
    }
  },
  computed: {
    statusStyle: function() {
      return (this.contactStatus == "inline") ? "user-available" : "user-invisible";
    }
  },
  methods: {
    selectContact(contact) {
      this.$emit('exo-chat-contact-selected', contact);
      let room = this.contacts.findIndex(c => c.user == contact.user);
      this.contacts[room].unreadTotal = 0;
    },
    getContactAvatar(user) {
      return chatData.socialUserAPI + user + '/avatar'
    },
    toggleFavorite(contact) {
      contact.isFavorite = !contact.isFavorite;
    }
  },
  created () {
    /*fetch(`/chatServer/whoIsOnline`, {
      credentials: 'include', 
      headers: {
        'Authorization': 'Bearer ' + this.$parent.chatData.token
      },
      data: {
        "user": "root",
        "onlineUsers": "root",
        "filter" : "",
        "timestamp": new Date().getTime(),
        "dbName": this.$parent.chatData.dbName
      }})
      .then(resp => resp.json())
      .then(usersStatus => {
        
        console.log(usersStatus);
      })*/
  }
}
</script>