<!--
Copyright (C) 2022 eXo Platform SAS.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
-->
<template>
  <div class="VuetifyApp">
    <v-app
      v-if="!ap"
      id="miniChatDrawer"
      class="miniChatDrawer">
      <exo-drawer
        ref="chatDrawer"
        class="chatDrawer"
        body-classes="hide-scroll"
        right
        @closed="resetSelectedContact">
        <template v-if="!showSearch" slot="title">
          <div class="leftHeaderDrawer flex-shrink-1 text-truncate">
            <span v-if="!selectedContact && !showSearch" class="chatContactDrawer">
              <exo-chat-contact
                :chat-drawer-contact="showChatDrawer"
                :user-name="userSettings.username"
                :status="userSettings.status"
                :is-current-user="true"
                type="u"
                @status-changed="setStatus($event)" />
            </span>
            <v-icon
              v-show="selectedContact"
              class="my-auto backButton"
              @click="backChat()">
              mdi-keyboard-backspace
            </v-icon>
            <span v-if="selectedContact && avatarUrl">
              <img
                :src="avatarUrl"
                class="chatAvatar"
                alt="avatar of discussion">
              <span :class="statusStyle" class="user-status">
                <i v-if="selectedContact.type=='u' && (selectedContact.isEnabledUser || selectedContact.isEnabledUser === null)" class="uiIconStatus"></i>
                <span :title="getName">{{ getName }}</span>
              </span>
            </span>
          </div>
        </template>
        <template v-if="showChatDrawer && !selectedContact" slot="title">
          <input
            ref="contactSearch"
            v-show="showSearch"
            v-model="searchTerm"
            :placeholder="$t('exoplatform.chat.contact.search.placeholder')"
            class="searchDrawer"
            type="text">
        </template>
        <template slot="titleIcons">
          <div
            v-show="selectedContact && (selectedContact.isEnabledUser || selectedContact.isEnabledUser === null)"
            class="title-action-component">
            <div
              v-for="action in enabledTitleActionComponents"
              :key="action.key"
              :class="`${action.appClass} ${action.typeClass}`"
              :ref="action.key">
              <div v-if="action.component">
                <component
                  v-dynamic-events="action.component.events"
                  v-bind="action.component.props ? action.component.props : {}"
                  :is="action.component.name" />
              </div>
              <div v-else-if="action.element" v-html="action.element.outerHTML">
              </div>
              <div v-else-if="action.html" v-html="action.html">
              </div>
              {{ initTitleActionComponent(action) }}
            </div>
          </div>
          <v-icon
            v-exo-tooltip.bottom="$t('exoplatform.chat.quick.create.discussion')"
            v-show="quickCreateChatDiscussionFeatureEnabled && !showSearch && !selectedContact"
            class="my-auto"
            @click="openQuickCreateChatDiscussionDrawer">
            mdi-plus
          </v-icon>  
          <v-icon
            v-show="showSearch && !selectedContact"
            class="my-auto"
            @click="closeContactSearch">
            mdi-filter-remove
          </v-icon>
          <v-icon
            v-show="!showSearch && !selectedContact"
            id="chatFilter"
            class="my-auto"
            @click="openContactSearch">
            mdi-filter
          </v-icon>
          <v-icon
            v-show="mq !=='mobile' && !showSearch"
            :title="$t('exoplatform.chat.open.chat')"
            id="openChat"
            class="my-auto"
            @click="navigateTo">
            mdi-open-in-new
          </v-icon>
        </template>
        <template slot="content">
          <div :class="!selectedContact ? 'contentDrawer ' : 'contentDrawerOfList'">
            <exo-chat-contact-list
              :is-chat-drawer="true"
              v-show="!selectedContact && contactList.length > 0"
              :search-word="searchTerm"
              :drawer-status="showChatDrawer"
              :contacts="contactList"
              :selected="selectedContact"
              :loading-contacts="loadingContacts"
              @load-more-contacts="loadMoreContacts"
              @contact-selected="setSelectedContact"
              @refresh-contacts="refreshContacts($event)" />
            <exo-chat-message-list
              v-show="selectedContact"
              :composers-applications="composerApplications"
              :contact="selectedContact"
              :user-settings="userSettings"
              :is-opened-contact="!selectedContact" />
          </div>
        </template>
      </exo-drawer>
    </v-app>
    <div class="hide">
      <audio
        id="chat-audio-notif"
        controls
        hidden="hidden">
        <source src="/chat/audio/notif.wav">
        <source src="/chat/audio/notif.mp3">
        <source src="/chat/audio/notif.ogg">
      </audio>
    </div>
  </div>
</template>

<script>
import * as chatServices from '../../chatServices';
import * as chatWebStorage from '../../chatWebStorage';
import {chatConstants} from '../../chatConstants';
import * as chatWebSocket from '../../chatWebSocket';
import * as desktopNotification from '../../desktopNotification';
import {miniChatTitleActionComponents,composerApplications,installExtensions} from '../../extension';
import {getSpaceAvatar, getUserAvatar} from '../../chatServices';

export default {
  name: 'ExoChatDrawer',
  data () {
    return {
      composerApplications: [],
      contactList: [],
      showSearch: false,
      loadingContacts: true,
      selectedContact: [],
      userSettings: {
        username: typeof eXo !== 'undefined' ? eXo.env.portal.userName : ''
      },
      showChatDrawer: false,
      fullNameOfUser: '',
      isOnline: true,
      searchTerm: '',
      totalUnreadMsg: 0,
      external: this.$t('exoplatform.chat.external'),
      chatLink: `/portal/${eXo.env.portal.portalName}/chat`,
      titleActionComponents: miniChatTitleActionComponents,
      quickCreateChatDiscussionFeatureEnabled: false
    };
  },
  computed: {
    enabledTitleActionComponents() {
      return this.titleActionComponents && this.titleActionComponents.filter(action => action.enabled) || [];
    },
    statusClass() {
      if (this.userSettings.status === 'invisible') {
        return 'user-offline';
      } else {
        return `user-${this.userSettings.status}`;
      }
    },
    statusStyle: function() {
      if (!this.selectedContact) {
        if (!this.isOnline || this.userSettings.status === 'invisible') {
          return 'user-offline';
        } else {
          return `user-${this.userSettings.status}`;
        }
      } else if (typeof this.selectedContact !== 'undefined'){
        if (!this.isOnline||  this.selectedContact.status === 'invisible') {
          return 'user-offline';
        } else {
          return `user-${this.selectedContact.status}`;
        }
      }
      return '';
    },
    getName(){
      if (!this.selectedContact) {
        return this.userSettings.username;
      } else {
        return this.selectedContact.isExternal === 'true' ? `${this.selectedContact.fullName} (${this.external})` : this.selectedContact.fullName;
      }
    },
    avatarUrl() {
      if (this.selectedContact.type === 'u') {
        return  getUserAvatar(this.selectedContact.user);
      } else if (this.selectedContact.type === 's') {
        return getSpaceAvatar(this.selectedContact.prettyName);
      } else {
        return chatConstants.DEFAULT_ROOM_AVATAR;
      }
    }
  },
  watch: {
    totalUnreadMsg() {
      chatServices.updateTotalUnread(this.totalUnreadMsg);
    },
  },
  created() {
    chatServices.getUserSettings(this.userSettings.username).then(userSettings => {
      this.initSettings(userSettings);
      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.has('chatRoomId')){
        this.openRoomWithId(urlParams.get('chatRoomId'));
      }
      installExtensions(userSettings);
      this.composerApplications = composerApplications;
    });
    document.addEventListener(chatConstants.EVENT_MESSAGE_RECEIVED, this.messageReceived);
    document.addEventListener(chatConstants.EVENT_ROOM_UPDATED, this.roomUpdated);
    document.addEventListener(chatConstants.EVENT_LOGGED_OUT, this.userLoggedout);
    document.addEventListener(chatConstants.EVENT_DISCONNECTED, this.changeUserStatusToOffline);
    document.addEventListener(chatConstants.EVENT_CONNECTED, this.connectionEstablished);
    document.addEventListener(chatConstants.EVENT_RECONNECTED, this.connectionEstablished);
    document.addEventListener(chatConstants.EVENT_USER_STATUS_CHANGED, this.userStatusChanged);
    document.addEventListener(chatConstants.EVENT_GLOBAL_UNREAD_COUNT_UPDATED, this.totalUnreadMessagesUpdated);
    document.addEventListener(chatConstants.ACTION_ROOM_OPEN_CHAT, this.openRoom);
    chatWebStorage.registreEventListener();
    chatWebSocket.registreEventListener();
    this.$featureService.isFeatureEnabled('quickCreateChatDiscussion')
      .then(enabled => this.quickCreateChatDiscussionFeatureEnabled = enabled);

  },
  destroyed() {
    document.removeEventListener(chatConstants.EVENT_MESSAGE_RECEIVED, this.messageReceived);
    document.removeEventListener(chatConstants.EVENT_ROOM_UPDATED, this.roomUpdated);
    document.removeEventListener(chatConstants.EVENT_LOGGED_OUT, this.userLoggedout);
    document.removeEventListener(chatConstants.EVENT_DISCONNECTED, this.changeUserStatusToOffline);
    document.removeEventListener(chatConstants.EVENT_CONNECTED, this.connectionEstablished);
    document.removeEventListener(chatConstants.EVENT_RECONNECTED, this.connectionEstablished);
    document.removeEventListener(chatConstants.EVENT_USER_STATUS_CHANGED, this.userStatusChanged);
    document.removeEventListener(chatConstants.EVENT_GLOBAL_UNREAD_COUNT_UPDATED, this.totalUnreadMessagesUpdated);
    document.removeEventListener(chatConstants.ACTION_ROOM_OPEN_CHAT, this.openRoom);
    document.removeEventListener(chatConstants.ACTION_CHAT_OPEN_DRAWER,this.openDrawer);
    chatWebStorage.unregistreEventListener();
    chatWebSocket.unregistreEventListener();
  },
  mounted() {
    document.addEventListener(chatConstants.ACTION_CHAT_OPEN_DRAWER,this.openDrawer);
    this.composerApplications=composerApplications;
  },
  methods: {
    messageReceived(event) {
      const message = event.detail;
      const room = message.room;
      if (!room) {
        return;
      }

      const foundContact = this.findContactByRoomOrUser(room, message.data ? message.data.user : message.sender);

      if (foundContact) {
        if (!foundContact.lastMessage) {
          foundContact.lastMessage = {};
        }
        foundContact.lastMessage = message.data;
        foundContact.timestamp = message.ts;
      } else {
        chatServices.getRoomDetail(eXo.chat.userSettings, room).then((contact) => {
          if (contact && contact.user && contact.user.length && contact.user !== 'undefined') {
            this.contactList.unshift(contact);
          }
        });
      }
    },
    findContactByRoomOrUser(room, targetUser) {
      let foundContact = null;
      if (room && room.trim().length)  {
        foundContact = this.findContact(room, 'room');
      }
      if (!foundContact && targetUser && targetUser.trim().length) {
        foundContact = this.findContact(targetUser, 'user');
      }
      return foundContact;
    },
    findContact(value, field) {
      if (!field)  {
        field = 'room';
      }
      return this.contactList.find(contact => contact[field] === value);
    },
    openDrawer() {
      this.$refs.chatDrawer.startLoading();
      chatServices.initChatSettings(this.userSettings.username, false,
        userSettings => this.initSettings(userSettings),
        chatRoomsData => {
          this.initChatRooms(chatRoomsData);
          this.$nextTick(this.$refs.chatDrawer.endLoading);
        },
        !this.ap);
      this.$refs.chatDrawer.open();
      this.showChatDrawer = true;
      this.selectedContact = null;

      // In case of error, force stop loading
      window.setTimeout(this.$refs.chatDrawer.endLoading, chatConstants.LOADING_ANIMATION_DURATION);
    },
    navigateTo() {
      window.open(this.chatLink, '_blank');
    },
    resetSelectedContact() {
      this.showChatDrawer = false;
      this.closeContactSearch();
      this.selectedContact = null;
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
      }
    },
    totalUnreadMessagesUpdated(e) {
      const totalUnreadMsg = e.detail ? e.detail.data.totalUnreadMsg : e.totalUnreadMsg;
      if (totalUnreadMsg >= 0) {
        this.totalUnreadMsg = totalUnreadMsg;
      }
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
      if (this.userSettings.originalStatus && this.userSettings.originalStatus !== this.userSettings.status) {
        this.setStatus(this.userSettings.originalStatus);
      } else if (this.userSettings && this.userSettings.originalStatus) {
        this.userSettings.status = this.userSettings.originalStatus;
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
    addRooms(rooms) {
      // force update room unread msg
      this.contactList.forEach(contact => {
        const indexOfRoom = rooms.findIndex(room => room.room === contact.room || room.user === contact.user);
        if (indexOfRoom >= 0 && rooms[indexOfRoom].unreadTotal !== contact.unreadTotal) {
          contact.unreadTotal = rooms[indexOfRoom].unreadTotal;
        }
      });
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
    initSettings(userSettings) {
      this.userSettings = userSettings;
      chatServices.initSettings(userSettings.userName, userSettings, userSettings => {
        chatServices.getNotReadMessages(userSettings)
          .then(data => this.totalUnreadMsg = data.total)
          .finally(() => this.$root.$applicationLoaded());
      });
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
      const totalUnreadMsg = Math.abs(chatRoomsData.unreadOffline)
                           + Math.abs(chatRoomsData.unreadOnline)
                           + Math.abs(chatRoomsData.unreadSpaces)
                           + Math.abs(chatRoomsData.unreadTeams)
                           - Number(chatRoomsData.unreadSilentRooms);
      if (totalUnreadMsg >= 0) {
        this.totalUnreadMsg = totalUnreadMsg;
      }
      chatServices.updateTotalUnread(totalUnreadMsg);
    },
    loadMoreContacts(term, filter, pageNumber) {
      let offset = 0;
      if (filter === 'All' || this.ap) {
        filter = '';
      }
      if (pageNumber) {
        offset = pageNumber * chatConstants.ROOMS_PER_PAGE;
      }
      this.loadingContacts = true;
      chatServices.getOnlineUsers().then(users => {
        chatServices.getUserChatRooms(this.userSettings, users, term, filter, offset).then(chatRoomsData => {
          this.addRooms(chatRoomsData.rooms, pageNumber);
          this.loadingContacts = false;
        });
      });
    },
    setSelectedContact(selectedContact) {
      if (!selectedContact && selectedContact.length() === 0) {
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
        } else {
          this.contactList.splice(indexOfRoom, 1, selectedContact);
        }
      }
      this.selectedContact = selectedContact;
      chatServices.getRoomParticipants(eXo.chat.userSettings, selectedContact).then( data => {
        this.selectedContact.participants = data.users;
        document.dispatchEvent(new CustomEvent(chatConstants.EVENT_ROOM_SELECTION_CHANGED, {'detail': this.selectedContact}));
      });
      this.showSearch = false;
    },
    refreshContacts(keepSelectedContact) {
      chatServices.getOnlineUsers().then(users => {
        chatServices.getUserChatRooms(this.userSettings, users).then(chatRoomsData => {
          this.addRooms(chatRoomsData.rooms);
          if (!keepSelectedContact && this.selectedContact) {
            const contactToChange = this.contactList.find(contact => contact.room === this.selectedContact.room || contact.user === this.selectedContact.user || contact.room === this.selectedContact);
            if (contactToChange) {
              this.setSelectedContact(contactToChange);
            }
          }
        });
      });
    },
    searchContacts(term) {
      this.loadingContacts = true;
      chatServices.getOnlineUsers().then(users => {
        chatServices.getUserChatRooms(this.userSettings, users, term).then(chatRoomsData => {
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
    },
    reloadPage() {
      window.location.reload();
    },
    openRoom(e) {
      const roomName = e.detail ? e.detail.name : null;
      const roomType = e.detail ? e.detail.type : null;
      if (roomName && roomName.trim().length) {
        chatServices.getRoomId(this.userSettings, roomName, roomType).then(roomId =>
          chatServices.getRoomDetail(this.userSettings, roomId).then( room => {
            this.openDrawer();
            this.setSelectedContact(room);
          }));
      }
      const tiptip = document.getElementById('tiptip_holder');
      if (tiptip) {
        tiptip.style.display = 'none';
      }
    },
    openContactSearch() {
      this.showSearch = true;
      this.$nextTick(() => this.$refs.contactSearch.focus());
    },
    closeContactSearch() {
      this.showSearch = false;
      this.searchTerm = '';
    },
    openQuickCreateChatDiscussionDrawer(){
      this.$root.$emit(chatConstants.ACTION_CHAT_OPEN_QUICK_CREATE_DISCUSSION_DRAWER);
    },
    selectContactSearch() {
      this.showSearch = false;
      this.$nextTick(() => this.$refs.contactSearch.focus());
    },
    canShowOnSiteNotif() {
      return desktopNotification.canShowOnSiteNotif();
    },
    backChat(){
      this.selectedContact = null;
    },
    initTitleActionComponent(action) {
      if (action.init && !action.isInited && action.enabled) {
        let container = this.$refs[action.key];
        if (container) {
          action.isInited = true;
          if (container && container.length > 0) {
            container = container[0];
          }
          action.init(container, eXo.chat);
        }
      }
    },
    openRoomWithId(roomId){
      chatServices.getRoomDetail(this.userSettings,roomId).then(room => {
        this.openDrawer();
        this.setSelectedContact(room);
      }).catch(()=>{this.openDrawer();});
      const tiptip = document.getElementById('tiptip_holder');
      if (tiptip) {
        tiptip.style.display = 'none';
      }
    }
  }
};
</script>



