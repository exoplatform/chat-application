<template>
  <div id="drawerId" class="miniChatDrawer">
    <a class="uiIconStatus uiNotifChatIcon" @click="openDrawer">
    </a>
    <div :class="showChatDrawer ? 'open' : '' " class="drawer">
      <div class="header">
        <span v-show="!listOfContact" >
          <img :src="contactAvatar" class="chatAvatar" />
          <span :class="statusStyle" class="user-status">
            <i v-if="selectedContact.type=='u' && (selectedContact.isEnabledUser || selectedContact.isEnabledUser === null)" class="uiIconStatus"></i>
            <span v-html="getName"></span>
          </span>
        </span>
        <span v-show="listOfContact && !showSearch" class="chatTitle">Chat</span>
        <span v-show="listOfContact && !showSearch">
          <exo-chat-contact :chat-drawer-contact="showChatDrawer" :user-name="userSettings.username" :status="userSettings.status" :is-current-user="true" type="u" @status-changed="setStatus($event)"></exo-chat-contact>
        </span>
        <a v-show="!listOfContact" class="icon-Back" @click="backChat()"></a>
        <a v-show="!showSearch" class="closeBtnDrawer" href="javascript:void(0)" @click="closeChatDrawer()">×</a>
        <a v-exo-tooltip="$t('exoplatform.chat.open.chat')" v-show="!showSearch" data-placement="bottom" class="icon-android-open" href="/portal/dw/chat" target="_chat"></a>
        <a v-show="!listOfContact && selectedContact.type=='u' && (selectedContact.isEnabledUser || selectedContact.isEnabledUser === null)" data-placement="bottom" class="icon-ios-videocam" ></a>
        <span v-show="showChatDrawer && listOfContact">
          <input v-show="showSearch" v-model="searchTerm" :placeholder="$t('exoplatform.chat.contact.search.placeholder')" class="searchDrawer" type="text" @keyup.esc="closeContactSearch">
          <a v-show = "!showSearch" class="uiIconSearchLight" @click="showSearch = !showSearch"></a>
        </span>
      </div>
      <div :class="showChatDrawer ? 'contentDrawer ' : '' " class="content">
        <exo-chat-contact-list v-show="listOfContact" :search-word="searchTerm" :drawer-status="showChatDrawer" :contacts="contactList" :selected="selectedContact" :loading-contacts="loadingContacts" @load-more-contacts="loadMoreContacts" @contact-selected="setSelectedContact" @refresh-contacts="refreshContacts($event)"></exo-chat-contact-list>
        <exo-chat-message-list v-show="!listOfContact" :contact="selectedContact" :user-settings="userSettings"></exo-chat-message-list>
      </div>
    </div>
    <div v-show="showChatDrawer" class="drawer-backdrop" @click="closeChatDrawer()"></div>
  </div>
</template>

<script>
import * as chatServices from '../../chatServices';
import {installExtensions} from '../../extension';
import * as chatWebStorage from '../../chatWebStorage';
import {chatConstants} from '../../chatConstants';
import * as chatWebSocket from '../../chatWebSocket';
import {getUserAvatar} from '../../chatServices';
import {getSpaceAvatar} from '../../chatServices';
export default {
  name: 'ExoChatDrawer',
  data () {
    return {
      showStatusUser : false,
      contactList: [],
      showSearch:false,
      loadingContacts: true,
      selectedContact: {},
      userSettings: {
        username: typeof eXo !== 'undefined' ? eXo.env.portal.userName : ''
      },
      showChatDrawer:false,
      listOfContact: false,
      fullNameOfUser:'',
      isOnline : true,
      searchTerm:''
    };
  },
  computed:{
    contactAvatar() {
      if (this.selectedContact.type === 'u') {
        return getUserAvatar(this.selectedContact.user);
      } else if (this.selectedContact.type === 's') {
        return getSpaceAvatar(this.selectedContact.prettyName);
      } else {
        return chatConstants.DEFAULT_ROOM_AVATAR;
      }
    },
    statusStyle: function() {
      if (this.listOfContact) {
        if (!this.isOnline || this.userSettings.status === 'invisible') {
          return 'user-offline';
        } else {
          return `user-${this.userSettings.status}`;
        }
      } else {
        if (!this.isOnline || this.selectedContact.status === 'invisible') {
          return 'user-offline';
        } else {
          return `user-${this.selectedContact.status}`;
        }
      }
    },
    getName(){
      if (this.listOfContact) {
        return this.userSettings.username;
      } else {
        return this.selectedContact.fullName;
      }
    }
  },
  created() {
    chatServices.initChatSettings(this.userSettings.username, false,
      userSettings => this.initSettings(userSettings),
      chatRoomsData => this.initChatRooms(chatRoomsData));
    document.addEventListener(chatConstants.EVENT_ROOM_UPDATED, this.roomUpdated);
    document.addEventListener(chatConstants.EVENT_LOGGED_OUT, this.userLoggedout);
    document.addEventListener(chatConstants.EVENT_DISCONNECTED, this.changeUserStatusToOffline);
    document.addEventListener(chatConstants.EVENT_CONNECTED, this.connectionEstablished);
    document.addEventListener(chatConstants.EVENT_RECONNECTED, this.connectionEstablished);
    document.addEventListener(chatConstants.EVENT_USER_STATUS_CHANGED, this.userStatusChanged);
    document.addEventListener(chatConstants.EVENT_GLOBAL_UNREAD_COUNT_UPDATED, this.totalUnreadMessagesUpdated);
    document.addEventListener(chatConstants.ACTION_ROOM_SHOW_PARTICIPANTS, () => this.participantsArea = true);
    document.addEventListener(chatConstants.ACTION_ROOM_OPEN_CHAT, this.openRoom);
  },
  methods:{
    openDrawer() {
      document.body.style.overflow = 'hidden';
      this.showChatDrawer = true;
      this.listOfContact = true;
    },
    closeChatDrawer() {
      document.body.style.overflow = 'scroll';
      this.showSearch = false;
      this.showChatDrawer = false;
    },
    setStatus(status) {
      chatServices.setUserStatus(this.userSettings, status, newStatus => {
        this.userSettings.status = newStatus;
        this.userSettings.originalStatus = newStatus;
      });
    },
    userLoggedout() {
      if (!chatWebSocket.isConnected()) {
        this.changeUserStatusToOffline();
        this.loggedout = true;
      }
    },
    totalUnreadMessagesUpdated(e) {
      const totalUnreadMsg = e.detail ? e.detail.data.totalUnreadMsg : e.totalUnreadMsg;
      chatServices.updateTotalUnread(totalUnreadMsg);
    },
    userStatusChanged(e) {
      const contactChanged = e.detail;
      if (this.userSettings.username === contactChanged.sender) {
        this.userSettings.status = contactChanged.status ? contactChanged.status : contactChanged.data ? contactChanged.data.status : null;
        this.userSettings.originalStatus = this.userSettings.status;
      }
    },
    connectionEstablished() {
      eXo.chat.isOnline = true;
      this.connected = true;
      if (this.userSettings.originalStatus !== this.userSettings.status) {
        this.setStatus(this.userSettings.originalStatus);
      } else if (this.userSettings && this.userSettings.originalStatus) {
        this.userSettings.status = this.userSettings.originalStatus;
      }
    },
    roomUpdated(e) {
      const updatedContact = e.detail && e.detail.data ? e.detail.data : null;
      if (updatedContact && (updatedContact.room || updatedContact.user)) {
        const indexOfRoom = this.contactList.findIndex(contact => contact.room === updatedContact.room || contact.user === updatedContact.user);
        if(indexOfRoom < 0) {
          this.contactList.unshift(updatedContact);
        } else {
          this.contactList.splice(indexOfRoom, 1, updatedContact);
        }
      }
    },
    addRooms(rooms) {
      const contacts = this.contactList.slice(0);
      rooms = rooms.filter(contact => contact.fullName
              && contact.fullName.trim().length > 0
              && (contact.room && contact.room.trim().length > 0 || contact.user && contact.user.trim().length > 0)
              && !contacts.find(otherContact => otherContact.room === contact.room || otherContact.user === contact.user));
      if(rooms && rooms.length > 0) {
        rooms.forEach(room => {
          this.contactList.push(room);
        });
      }
    },
    initSettings(userSettings) {
      this.userSettings = userSettings;
      // Trigger that the new status has been loaded
      this.setStatus(this.userSettings.status);
      installExtensions(this.userSettings);
      const thiss = this;
      if(this.userSettings.offlineDelay) {
        setInterval(
          function() {thiss.refreshContacts(true);},
          this.userSettings.offlineDelay);
      }
    },
    initChatRooms(chatRoomsData) {
      this.loadingContacts = false;
      this.addRooms(chatRoomsData.rooms);
      if (this.mq !== 'mobile') {
        const selectedRoom = chatWebStorage.getStoredParam(chatConstants.STORED_PARAM_LAST_SELECTED_ROOM);
        if(selectedRoom) {
          this.setSelectedContact(selectedRoom);
        }
      }

      const totalUnreadMsg = Math.abs(chatRoomsData.unreadOffline) + Math.abs(chatRoomsData.unreadOnline) + Math.abs(chatRoomsData.unreadSpaces) + Math.abs(chatRoomsData.unreadTeams);
      chatServices.updateTotalUnread(totalUnreadMsg);
    },
    loadMoreContacts(nbPages) {
      this.loadingContacts = true;
      chatServices.getOnlineUsers().then(users => {
        chatServices.getChatRooms(this.userSettings, users, '', nbPages).then(chatRoomsData => {
          this.addRooms(chatRoomsData.rooms);
          this.loadingContacts = false;
        });
      });
    },
    setSelectedContact(selectedContact) {
      if(this.mq === 'mobile') {
        this.conversationArea = true;
      }
      if(!selectedContact && selectedContact.length() === 0) {
        selectedContact = {};
      }
      if (typeof selectedContact === 'string') {
        selectedContact = this.contactList.find(contact => contact.room === selectedContact || contact.user === selectedContact);
      }
      if (selectedContact && selectedContact.fullName && (selectedContact.room || selectedContact.user)) {
        eXo.chat.selectedContact = selectedContact;
        const indexOfRoom = this.contactList.findIndex(contact => contact.room === selectedContact.room || contact.user === selectedContact.user);
        if(indexOfRoom < 0) {
          this.contactList.unshift(selectedContact);
        } else {
          this.contactList.splice(indexOfRoom, 1, selectedContact);
        }
      }
      this.selectedContact = selectedContact;
      document.dispatchEvent(new CustomEvent(chatConstants.EVENT_ROOM_SELECTION_CHANGED, {'detail' : selectedContact}));
      this.listOfContact = false;
      this.showSearch = false;
    },
    refreshContacts(keepSelectedContact) {
      chatServices.getOnlineUsers().then(users => {
        chatServices.getChatRooms(this.userSettings, users).then(chatRoomsData => {
          this.addRooms(chatRoomsData.rooms);
          if (!keepSelectedContact && this.selectedContact) {
            const contactToChange = this.contactList.find(contact => contact.room === this.selectedContact.room || contact.user === this.selectedContact.user);
            if(contactToChange) {
              this.setSelectedContact(contactToChange);
            }
          }
        });
      });
    },
    searchContacts(term) {
      this.loadingContacts = true;
      chatServices.getOnlineUsers().then(users => {
        chatServices.getChatRooms(this.userSettings, users, term).then(chatRoomsData => {
          this.addRooms(chatRoomsData.rooms);
          this.loadingContacts = false;
        });
      });
    },
    changeUserStatusToOffline() {
      if (this.userSettings && this.userSettings.status && !this.userSettings.originalStatus) {
        this.userSettings.originalStatus = this.userSettings.status;
      }
      eXo.chat.isOnline = false;
      this.connected = false;
    },
    reloadPage() {
      window.location.reload();
    },
    openRoom(e) {
      const roomName = e.detail ? e.detail.name : null;
      const roomType = e.detail ? e.detail.type : null;
      if(roomName && roomName.trim().length) {
        chatServices.getRoomId(this.userSettings, roomName, roomType).then(rommId => {
          this.setSelectedContact(rommId);
        });
      }
      const tiptip = document.getElementById('tiptip_holder');
      if (tiptip) {
        tiptip.style.display = 'none';
      }
    },
    closeContactSearch() {
      this.showSearch = false;
      this.searchTerm = '';
    },
    selectContactSearch() {
      this.listOfContact = false;
      this.showSearch = false;
      this.$nextTick(() => this.$refs.contactSearch.focus());
    },
    backChat(){
      this.listOfContact=true;
    }
  }
};
</script>



