<template>
  <div id="chat-application" :class="{'online': connected, 'offline': !connected, 'show-conversation': showMobileConversations, 'show-participants': showMobileParticipants, 'show-sideMenu': sideMenuArea}">
    <div class="uiChatLeftContainer">
      <div class="userDetails">
        <exo-chat-contact
          :contact-room-id="selectedContact.room"
          :user-name="userSettings.username"
          :name="userSettings.fullName"
          :status="userSettings.status"
          :is-current-user="true"
          type="u"
          @status-changed="setStatus($event)">
          <div
            v-exo-tooltip.right="$t('exoplatform.chat.settings.button.tip')"
            v-if="mq !== 'mobile'"
            class="chat-user-settings"
            @click="openSettingModal">
            <i class="uiIconGear"></i>
          </div>
          <div
            v-exo-tooltip.right="$t('exoplatform.chat.home')"
            v-if="mq !== 'mobile'"
            class="home-button">
            <a href="/"><i class="uiIconHomeInfo"></i></a>
          </div>
        </exo-chat-contact>
        <div v-if="mq === 'mobile'" class="discussion-label">{{ $t('exoplatform.chat.discussion') }}</div>
      </div>
      <exo-chat-contact-list
        v-if="ap"
        :contacts="contactList"
        :contacts-size="contactsSize"
        :selected="selectedContact"
        :loading-contacts="loadingContacts"
        @open-side-menu="sideMenuArea = !sideMenuArea"
        @load-more-contacts="changeFilter"
        @search-contact="searchContacts"
        @change-filter-type="changeFilter"
        @contact-selected="setSelectedContact"
        @refresh-contacts="refreshContacts($event)" />
    </div>
    <div
      v-show="(selectedContact && (selectedContact.room || selectedContact.user)) || mq === 'mobile'"
      class="uiGlobalRoomsContainer"
      role="main">
      <exo-chat-room-detail
        v-if="Object.keys(selectedContact).length !== 0"
        :is-room-notification-silence="isSelectedRoomSilence"
        :contact="selectedContact"
        @back-to-contact-list="conversationArea = false" />
      <div class="room-content">
        <exo-chat-message-list
          :contact="selectedContact"
          :user-settings="userSettings"
          :is-opened-contact-apps="contactOpened" />
        <exo-chat-room-participants
          :contact="selectedContact"
          @participants-loaded="setContactParticipants($event)"
          @back-to-conversation="participantsArea = false" />
      </div>
    </div>
    <div v-if="mq==='mobile'" class="chat-side-menu">
      <div class="side-menu-header">
        <exo-chat-contact
          :user-name="userSettings.username"
          :name="userSettings.fullName"
          :status="userSettings.status"
          type="u"
          list
          @status-changed="setStatus($event)" />
      </div>
      <ul class="side-menu-list">
        <li><a href="#" @click="openSettingModal"><i class="uiIconSetting"></i>{{ $t('exoplatform.chat.settings.button.tip') }}</a></li>
        <li><a href="/"><i class="uiIconHomeInfo"></i>{{ $t('exoplatform.chat.home') }}</a></li>
      </ul>
    </div>
    <div v-if="mq !== 'mobile' && !(selectedContact && (selectedContact.room || selectedContact.user))" class="chat-no-conversation muted">
      <span class="text">{{ $t('exoplatform.chat.no.conversation') }}</span>
    </div>
    <exo-chat-global-notification-modal :show="settingModal" @close-modal="settingModal = false" />
    <exo-chat-modal
      v-show="loggedout"
      :title="$t('exoplatform.chat.timeout.title')"
      :display-close="false"
      class="logout-popup">
      <div class="modal-body">
        {{ $t('exoplatform.chat.timeout.description') }}
      </div>
      <div class="uiAction uiActionBorder">
        <a
          href="#"
          class="btn btn-primary"
          @click="reloadPage">
          {{ $t('exoplatform.chat.timeout.login') }}
        </a>
      </div>
    </exo-chat-modal>
    <div class="hide">
      <audio id="chat-audio-notif" controls>
        <source src="/chat/audio/notif.wav">
        <source src="/chat/audio/notif.mp3">
        <source src="/chat/audio/notif.ogg">
      </audio>
    </div>
  </div>
</template>

<script>
import * as chatServices from '../chatServices';
import * as chatWebStorage from '../chatWebStorage';
import * as chatWebSocket from '../chatWebSocket';
import * as desktopNotification from '../desktopNotification';
import {chatConstants} from '../chatConstants';
import {installExtensions} from '../extension';
import {additionalExtensionsInstalled} from '../extension';

export default {
  data() {
    return {
      contactList: [],
      contactsSize: 0,
      /**
       * chatPage: {String}
       * cometdToken: {String}
       * fullName: {String}
       * isOnline: {Boolean}
       * maxUploadSize: {Number}
       * canUploadFiles: {Boolean}
       * canAddEvent: {Boolean}
       * offlineDelay: {Number}
       * serverURL: {String}
       * sessionId: {String}
       * standalone: {Boolean}
       * status: {String}
       * token: {String}
       * username: {String}
       * wsEndpoint: {String}
       */
      userSettings: {
        username: typeof eXo !== 'undefined' ? eXo.env.portal.userName : 'root'
      },
      connected: false,
      loggedout: false,
      contactOpened: true,
      selectedContact: {},
      loadingContacts: true,
      settingModal: false,
      conversationArea: false,
      participantsArea: false,
      sideMenuArea: false
    };
  },
  computed: {
    isSelectedRoomSilence() {
      return this.selectedContact && desktopNotification.isRoomNotificationSilence(this.selectedContact.room) || this.selectedContact.isRoomSilent;
    },
    showMobileConversations() {
      return this.mq === 'mobile' && this.conversationArea === true ? true : false;
    },
    showMobileParticipants() {
      return this.mq === 'mobile' && this.participantsArea === true ? true : false;
    }
  },
  watch: {
    loadingContacts() {
      if (!this.loadingContacts) {
        this.$root.$applicationLoaded();
      }
    },
  },
  created() {
    this.showHidePlatformAdminToolbar();
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
    chatWebStorage.registreEventListener();
    chatWebSocket.registreEventListener();
  },
  destroyed() {
    document.removeEventListener(chatConstants.EVENT_DISCONNECTED, this.changeUserStatusToOffline);
    document.removeEventListener(chatConstants.EVENT_CONNECTED, this.connectionEstablished);
    document.removeEventListener(chatConstants.EVENT_RECONNECTED, this.connectionEstablished);
    document.removeEventListener(chatConstants.EVENT_ROOM_UPDATED, this.roomUpdated);
    document.removeEventListener(chatConstants.EVENT_LOGGED_OUT, this.userLoggedout);
    document.removeEventListener(chatConstants.EVENT_USER_STATUS_CHANGED, this.userStatusChanged);
    document.removeEventListener(chatConstants.EVENT_GLOBAL_UNREAD_COUNT_UPDATED, this.totalUnreadMessagesUpdated);
    document.removeEventListener(chatConstants.ACTION_ROOM_OPEN_CHAT, this.openRoom);
    chatWebStorage.unregistreEventListener();
    chatWebSocket.unregistreEventListener();
  },
  methods: {
    initSettings(userSettings) {
      this.userSettings = userSettings;
      // Trigger that the new status has been loaded
      this.setStatus(this.userSettings.status);
      if (!additionalExtensionsInstalled){
        installExtensions(this.userSettings);
      }
      const thiss = this;
      if (this.userSettings.offlineDelay) {
        setInterval(
          function() {thiss.refreshContacts(true);},
          this.userSettings.offlineDelay);
      }
    },
    initChatRooms(chatRoomsData) {
      this.loadingContacts = false;
      
      this.addRooms(chatRoomsData.rooms);
      this.contactsSize = chatRoomsData.roomsCount;

      const selectedRoom = chatWebStorage.getStoredParam(chatConstants.STORED_PARAM_LAST_SELECTED_ROOM);
      if (selectedRoom) {
        this.setSelectedContact(selectedRoom);
      }

      const totalUnreadMsg = (Math.abs(chatRoomsData.unreadOffline)
              + Math.abs(chatRoomsData.unreadOnline)
              + Math.abs(chatRoomsData.unreadSpaces)
              + Math.abs(chatRoomsData.unreadTeams))
          - Number(chatRoomsData.unreadSilentRooms);
      chatServices.updateTotalUnread(totalUnreadMsg);
    },
    setSelectedContact(selectedContact) {
      if (this.mq === 'mobile') {
        this.conversationArea = true;
      }
      if (!selectedContact) {
        selectedContact = {};
      }
      if (typeof selectedContact === 'string') {
        selectedContact = this.contactList.find(contact => contact.room === selectedContact || contact.user === selectedContact);
      }
      if (selectedContact && selectedContact.fullName && (selectedContact.room || selectedContact.user)) {
        eXo.chat.selectedContact = selectedContact;
        const indexOfRoom = this.contactList.findIndex(contact => contact.room === selectedContact.room || contact.user === selectedContact.user);
        if (indexOfRoom < 0) {
          this.contactList.unshift(selectedContact);
        }
        this.contactOpened = false;
        chatServices.getRoomParticipantsCount(eXo.chat.userSettings, selectedContact).then( data => {
          this.selectedContact.participantsCount = data.usersCount;
          this.selectedContact.activeParticipantsCount = data.activeUsersCount;
        });
        chatServices.getRoomParticipants(eXo.chat.userSettings, selectedContact).then( data => {
          this.selectedContact = selectedContact;
          this.selectedContact.participants = data.users;
          document.dispatchEvent(new CustomEvent(chatConstants.EVENT_ROOM_SELECTION_CHANGED, {'detail': this.selectedContact}));
        });
      }
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
    roomUpdated(e) {
      const updatedContact = e.detail && e.detail.data ? e.detail.data : null;
      if (updatedContact && (updatedContact.room || updatedContact.user)) {
        const indexOfRoom = this.contactList.findIndex(contact => contact.room === updatedContact.room || contact.user === updatedContact.user);
        if (indexOfRoom < 0) {
          this.contactList.unshift(updatedContact);
        } else {
          this.contactList.splice(indexOfRoom, 1, updatedContact);
        }
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
      if (this.userSettings.originalStatus && this.userSettings.originalStatus !== this.userSettings.status) {
        this.setStatus(this.userSettings.originalStatus);
      } else if (this.userSettings && this.userSettings.originalStatus) {
        this.userSettings.status = this.userSettings.originalStatus;
      }
    },
    addRooms(rooms, append) {
      if (!append) {
        this.contactList = [];
      }
      const contacts = this.contactList.slice(0);
      rooms = rooms.filter(contact => contact.fullName
        && contact.fullName.trim().length > 0
        && (contact.room && contact.room.trim().length > 0 || contact.user && contact.user.trim().length > 0)
        && !contacts.find(otherContact => otherContact.room === contact.room || otherContact.user === contact.user));
      if (rooms && rooms.length > 0) {
        rooms.forEach(room => {
          this.contactList.push(room);
        });
      }
    },
    loadMoreContacts(nbPages) {
      this.loadingContacts = true;
      chatServices.getOnlineUsers().then(users => {
        chatServices.getUserChatRooms(this.userSettings, users, '', nbPages * chatConstants.DEFAULT_USER_LIMIT).then(chatRoomsData => {
          this.addRooms(chatRoomsData.rooms, true);
          this.contactsSize = chatRoomsData.roomsCount;
          this.loadingContacts = false;
        });
      });
    },
    searchContacts(term) {
      this.loadingContacts = true;
      chatServices.getOnlineUsers().then(users => {
        chatServices.getUserChatRooms(this.userSettings, users, term).then(chatRoomsData => {
          this.addRooms(chatRoomsData.rooms);
          this.contactsSize = chatRoomsData.roomsCount;
          this.loadingContacts = false;
        });
      });
    },
    changeFilter(term, filter, pageNumber) {
      let offset = 0;
      if (filter === 'All') {
        filter = '';
      }
      if (pageNumber) {
        offset = pageNumber * chatConstants.ROOMS_PER_PAGE;
      }
      this.loadingContacts = true;
      chatServices.getOnlineUsers().then(users => {
        chatServices.getUserChatRooms(this.userSettings, users, term, filter, offset).then(chatRoomsData => {
          this.addRooms(chatRoomsData.rooms, pageNumber);
          this.contactsSize = chatRoomsData.roomsCount;
          this.loadingContacts = false;
        });
      });
    },
    refreshContacts(keepSelectedContact) {
      chatServices.getOnlineUsers().then(users => {
        chatServices.getUserChatRooms(this.userSettings, users).then(chatRoomsData => {
          this.addRooms(chatRoomsData.rooms);
          if (!keepSelectedContact && this.selectedContact) {
            const contactToChange = this.contactList.find(contact => contact.room === this.selectedContact.room || contact.user === this.selectedContact.user);
            if (contactToChange) {
              this.setSelectedContact(contactToChange);
            }
          }
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
    openSettingModal() {
      this.settingModal = true;
    },
    setContactParticipants(participants) {
      this.selectedContact.participants = participants;
      document.dispatchEvent(new CustomEvent(chatConstants.EVENT_ROOM_PARTICIPANTS_LOADED, {'detail': this.selectedContact}));
    },
    reloadPage() {
      window.location.reload();
    },
    openRoom(e) {
      const roomName = e.detail ? e.detail.name : null;
      const roomType = e.detail ? e.detail.type : null;
      if (roomName && roomName.trim().length) {
        chatServices.getRoomId(this.userSettings, roomName, roomType).then(roomId => {
          // if selected room is not present in already loaded rooms then refresh the contact list
          if (!this.contactList.some(contact => contact.room === roomId || contact.user === roomId)) {
            chatServices.getOnlineUsers().then(users => {
              chatServices.getChatRooms(this.userSettings, users).then(chatRoomsData => {
                this.addRooms(chatRoomsData.rooms);
              }).then(() => {
                this.setSelectedContact(roomId);
              });
            });
          } else {
            this.setSelectedContact(roomId);
          }
        });
      }
      const tiptip = document.getElementById('tiptip_holder');
      if (tiptip) {
        tiptip.style.display = 'none';
      }
    },
    showHidePlatformAdminToolbar(){
      if (location.pathname==='/portal/'.concat(eXo.env.portal.portalName).concat('/chat')) {
        return $('#PlatformAdminToolbarContainer').css('display', 'none') && $('#UITopBarContainerParent').css('display', 'none');
      } else {
        return $('#PlatformAdminToolbarContainer').css('display', 'block') && $('#UITopBarContainerParent').css('display', 'block');
      }
    }
  }
};
</script>

