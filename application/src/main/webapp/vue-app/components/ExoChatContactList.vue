/*
 * Copyright (C) 2022 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */
<template>
  <div class="contactListContainer" role="navigation">
    <div v-if="ap">
      <div v-show="mq !== 'mobile' || contactSearchMobile" class="contactFilter">
        <i v-if="mq !== 'mobile'" class="uiIconSearchLight"></i>
        <input
          ref="contactSearch"
          v-model="searchTerm"
          :placeholder="$t('exoplatform.chat.contact.search.placeholder')"
          type="text"
          @keyup.esc="closeContactSearch">
        <div
          v-show="searchTerm !== ''"
          class="contact-search-close"
          @click="closeContactSearch">
          <i class="uiIconClose"></i>
        </div>
      </div>
      <div class="listHeader">
        <div
          v-if="mq === 'mobile'"
          class="hamburger-menu"
          @click="$emit('open-side-menu')">
          <i class="uiIconMenu"></i>
        </div>
        <exo-dropdown-select v-if="mq !== 'mobile'">
          <span slot="toggle">{{ sortByDate[sortFilter] }}</span>
          <i slot="toggle" class="uiIconArrowDownMini"></i>
          <div slot="menu" class="dropdown-category">{{ $t('exoplatform.chat.contact.filter.sort') }}</div>
          <li
            v-for="(label, filter) in sortByDate"
            slot="menu"
            :key="filter"
            @click="selectSortFilter(filter)">
            <a href="#"><i :class="{'not-filter': sortFilter !== filter}" class="uiIconTick"></i>{{ label }}</a>
          </li>
          <div slot="menu" class="dropdown-category">{{ $t('exoplatform.chat.contact.filter.actions') }}</div>
          <li slot="menu" @click="markAllAsRead"><a href="#"><i class="uiIconTick not-filter"></i>{{ $t('exoplatform.chat.contact.mark.read') }}</a></li>
        </exo-dropdown-select>
        <exo-dropdown-select v-if="mq !== 'mobile'">
          <span slot="toggle">{{ filterByType[typeFilter] }}</span>
          <i slot="toggle" class="uiIconArrowDownMini"></i>
          <li
            v-for="(label, filter) in filterByType"
            slot="menu"
            :key="filter"
            @click="selectTypeFilter(filter)">
            <a href="#"><i :class="{'not-filter': typeFilter !== filter}" class="uiIconTick"></i>{{ label }}</a>
          </li>
        </exo-dropdown-select>
        <div v-if="!externalUser" class="room-actions">
          <div
            v-if="mq === 'mobile'"
            class="filter-action"
            @click="filterMenuClosed = false">
            <i class="uiIconFilter"></i>
          </div>
          <div
            v-exo-tooltip.top="$t('exoplatform.chat.create.team')"
            class="add-room-action"
            @click="openCreateRoomModal">
            <i class="uiIconSimplePlus"></i>
          </div>
          <div v-if="mq === 'mobile'" @click="selectContactSearch">
            <i class="uiIconSearch"></i>
          </div>
        </div>
      </div>
    </div>
    <div id="chat-users" class="contactList isList">
      <transition-group name="chat-contact-list" v-if="filteredContacts.length > 0">
        <div
          v-hold-tap="openContactActions"
          v-for="contact in filteredContacts"
          :key="contact.user"
          :title="contactTooltip(contact)"
          :class="{selected: mq !== 'mobile' && selected && contact && selected.user === contact.user, currentContactMenu: mq === 'mobile' && contactMenu && contactMenu.user === contact.user, hasUnreadMessages: contact.unreadTotal > 0, 'has-not-sent-messages' : contact.hasNotSentMessages}"
          class="contact-list-item contact-list-room-item"
          @click="selectContact(contact)">
          <exo-chat-contact
            :is-external="contact.isExternal === 'true'"
            :is-enabled="contact.isEnabledUser === 'true' || contact.isEnabledUser === 'null'"
            :list="true"
            :type="contact.type"
            :user-name="contact.user"
            :pretty-name="contact.prettyName"
            :group-id="contact.groupId"
            :name="contact.fullName"
            :status="contact.status"
            :unread-total="contact.unreadTotal"
            :contact-room-id="contact.room"
            :is-room-silent="contact.isRoomSilent"
            :last-message="getLastMessage(contact.lastMessage, contact.type)">
            <div
              v-if="mq === 'mobile'"
              :class="{'is-fav': contact.isFavorite}"
              class="uiIcon favorite"></div>
            <div
              v-if="mq === 'mobile' || drawerStatus"
              :class="[drawerStatus ? 'last-message-time-drawer last-message-time' : 'last-message-time']">
              {{ getLastMessageTime(contact) }}
            </div>
          </exo-chat-contact>
          <i
            v-exo-tooltip.top.body="$t('exoplatform.chat.msg.notDelivered')"
            v-if="!drawerStatus"
            class="uiIconNotification"></i>
          <div
            v-exo-tooltip.top.body="favoriteTooltip(contact)"
            v-if="mq !== 'mobile'"
            :class="{'is-fav': contact.isFavorite}"
            class="uiIcon favorite my-auto pb-1"
            @click.stop="toggleFavorite(contact)"></div>
        </div>
      </transition-group>
      <div v-show="loadingContacts" class="contact-list-item isList">
        <div class="seeMoreContacts">
          {{ $t('exoplatform.chat.loading') }}
        </div>
      </div>
      <div v-if="filteredContacts.length === 0">
        <div class="center">
          <div class="noResultFilter">
            <div>
            <img
              class="mb-2"
              alt=""
              loading="lazy"
              src="/chat/img/no-result-filter.png">
            </div>
            <span class="text">
              {{noResultFilterMessage}}
              <v-icon v-if="!isExternal"
                class="my-auto"
                :size="16"
                @click="openQuickCreateChatDiscussionDrawer">
                fa-plus
              </v-icon> 
            </span>
          </div>
        </div>
      </div>
      <div
        v-show="hasMoreContacts"
        class="contact-list-item isList"
        @click="loadMore()">
        <div class="seeMoreContacts">
          <a href="#"><u>{{ $t('exoplatform.chat.seeMore') }}</u></a>
          <i class="uiIconArrowDownMini"></i>
        </div>
      </div>
      <div
        v-if="mq == 'mobile' && contactMenu !== null"
        v-show="!contactMenuClosed"
        class="uiPopupWrapper chat-modal-mask"
        @click.prevent.stop="closeContactActions">
        <ul class="mobile-options">
          <li><a href="#" @click.prevent="toggleFavorite(contactMenu)">{{ contactMenu.isFavorite === false ? $t('exoplatform.chat.add.favorites') : $t('exoplatform.chat.remove.favorites') }}</a></li>
          <li v-show="contactMenu.type != 't'" @click.stop><a :href="getProfileLink()">{{ $t('exoplatform.chat.contact.profile') }}</a></li>
        </ul>
      </div>
      <div
        v-if="mq == 'mobile'"
        v-show="!filterMenuClosed"
        class="uiPopupWrapper chat-modal-mask">
        <ul class="mobile-options filter-options">
          <li class="options-category">
            <i class="uiIconClose" @click="cancelFilterMobile"></i>
            <span><i class="uiIconFilter"></i>{{ $t('exoplatform.chat.contact.filter') }}</span>
            <div @click="saveFilterMobile">{{ $t('exoplatform.chat.contact.filter.save') }}</div>
          </li>
          <li class="options-category">{{ $t('exoplatform.chat.contact.filter.sort') }}</li>
          <li
            v-for="(label, filter) in sortByDate"
            slot="menu"
            :key="filter"
            @click="sortFilterMobile = filter">
            <a href="#"><i :class="{'not-filter': sortFilterMobile !== filter}" class="uiIconTick"></i>{{ label }}</a>
          </li>
          <li class="options-category">{{ $t('exoplatform.chat.contact.filter.by') }}</li>
          <li
            v-for="(label, filter) in filterByType"
            :key="filter"
            @click="typeFilterMobile = filter">
            <a href="#"><i :class="{'not-filter': typeFilterMobile !== filter}" class="uiIconTick"></i>{{ label }}</a>
          </li>
          <li @click="allAsReadFilterMobile = !allAsReadFilterMobile"><a href="#"><i :class="{'not-filter': !allAsReadFilterMobile}" class="uiIconTick"></i>{{ $t('exoplatform.chat.contact.mark.read') }}</a></li>
        </ul>
      </div>
    </div>
    <exo-chat-room-form-modal
      :show="createRoomModal"
      :selected="newRoom"
      @room-saved="roomSaved"
      @modal-closed="closeModal" />
  </div>
</template>


<script>
import * as chatServices from '../chatServices';
import * as chatWebStorage from '../chatWebStorage';
import * as chatWebSocket from '../chatWebSocket';
import * as chatTime from '../chatTime';

import {chatConstants} from '../chatConstants';
import {composerApplications} from '../extension';


export default {
  props: {
    /**
     * List of contacts objects to display in List.
     * Contact {
     *   fullName: {string} full name of contact
     *   isActive: {string} if the contact is of type user, this will be equals to "true" when the user is enabled
     *   isFavorite: {Boolean} whether is favortie of current user or not
     *   lastMessage: {string} Last message object with current user
     *   room: {string} contact room id
     *   status: {string} if the contact is of type user, this variable determines the user status (away, offline, available...)
     *   timestamp: {number} contact update timestamp
     *   type: {string} contact type, 'u' for user, 't' for team and 's' for space
     *   unreadTotal: {number} unread total number of messages for this contact
     *   user: {string} contact id, if user , username else team-{CONTACT_ID} or space-{CONTACT_ID}
     * }
     */
    unreadMessages: {
      type: Array,
      default: function () {
        return [{}];
      }
    },
    drawerStatus: {
      type: Boolean,
      default: false
    },
    contacts: {
      type: Array,
      default: function() { return [];}
    },
    contactsLoaded: {
      type: Boolean,
      default: function() { return false;}
    },
    /**
     * whether some contacts are currently loading or not
     */
    loadingContacts: {
      type: Boolean,
      default: function() { return false;}
    },
    /**
    * Select contact object
    * Contact {
      *   fullName: {string} full name of contact
      *   isActive: {string} if the contact is of type user, this will be equals to "true" when the user is enabled
      *   isFavorite: {Boolean} whether is favortie of current user or not
      *   lastMessage: {string} Last message object with current user
      *   room: {string} contact room id
      *   status: {string} if the contact is of type user, this variable determines the user status (away, offline, available...)
      *   timestamp: {number} contact update timestamp
      *   type: {string} contact type, 'u' for user, 't' for team and 's' for space
      *   unreadTotal: {number} unread total number of messages for this contact
      *   user: {string} contact id, if user , username else team-{CONTACT_ID} or space-{CONTACT_ID}
      * }
    **/
    selected: {
      type: Object,
      default: function() {
        return {};
      }
    },
    searchWord: {
      type: String,
      default: ''
    },
    isChatDrawer: {
      type: Boolean,
      default: false
    }
  },
  data: function() {
    return {
      sortByDate: {
        'Recent': this.$t('exoplatform.chat.contact.recent'),
        'Unread': this.$t('exoplatform.chat.contact.unread'),
      },
      sortFilter: chatConstants.SORT_FILTER_DEFAULT,
      sortFilterMobile: null,
      filterByType: {
        'All': this.$t('exoplatform.chat.contact.all'),
        'u': this.$t('exoplatform.chat.people'),
        't': this.$t('exoplatform.chat.teams'),
        's': this.$t('exoplatform.chat.spaces'),
        'Favorites': this.$t('exoplatform.chat.favorites')
      },
      typeFilter: chatConstants.TYPE_FILTER_DEFAULT,
      typeFilterMobile: null,
      allAsReadFilterMobile: false,
      contactSearchMobile: false,
      externalUser: false,
      createRoomModal: false,
      searchTerm: '',
      newRoom: {
        name: '',
        participants: []
      },
      contactMenu: null,
      contactMenuClosed: true,
      filterMenuClosed: true,
      contactsToDisplay: [],
      inactive: this.$t('exoplatform.chat.inactive'),
      external: this.$t('exoplatform.chat.external'),
      nbrePages: 0,
      contactList: [],
      isExternal: false
    };
  },
  computed: {
    noResultFilterMessage() {
      return this.isExternal ? this.$t('exoplatform.chat.no.results.filter.external.message') : this.$t('exoplatform.chat.no.results.filter.message') ;
    },
    statusStyle() {
      return this.contactStatus === 'inline' ? 'user-available' : 'user-invisible';
    },
    filteredContacts: function() {
      const sortedContacts = this.contactList.slice(0).filter(contact => (contact.room || contact.user) && contact.fullName);
      const self = this;
      if (this.sortFilter === 'Unread' && !this.isChatDrawer) {
        sortedContacts.sort(function (a, b) {
          const unreadTotal = b.unreadTotal - a.unreadTotal;
          if (unreadTotal === 0) {
            if (a.timestamp < b.timestamp) {
              return self.compareSilentsForSort(a, b, 1);
            } else if (a.timestamp > b.timestamp) {
              return self.compareSilentsForSort(a, b, -1);
            } else {
              return self.compareSilentsForSort(a, b, 0);
            }
          }
          if (a.unreadTotal < b.unreadTotal) {
            return self.compareSilentsForSort(a, b, 1);
          } else if (a.unreadTotal > b.unreadTotal) {
            return self.compareSilentsForSort(a, b, -1);
          } else {
            return self.compareSilentsForSort(a, b, 0);
          }
        });
      } else {
        sortedContacts.sort(function (a, b) {
          if (a.timestamp < b.timestamp) {
            return self.compareSilentsForSort(a, b, 1);
          } else if (a.timestamp > b.timestamp) {
            return self.compareSilentsForSort(a, b, -1);
          } else {
            return self.compareSilentsForSort(a, b, 0);
          }
        });
      }
      return sortedContacts.slice(0, this.contactsToDisplay.length);
    },
    hasMoreContacts() {
      return Math.floor(this.filteredContacts.length / chatConstants.ROOMS_PER_PAGE) > this.nbrePages;
    },
  },
  watch: {
    contacts() {
      this.contactList = this.contacts.slice();
      this.contactsToDisplay = this.contacts.slice();
    },
    searchTerm(value) {
      this.searchTerm = value;
      this.nbrePages = 0;
      chatWebStorage.setStoredParam(chatConstants.STORED_PARAM_TERM_FILTER, this.searchTerm);
      this.$emit('change-filter-type', this.searchTerm, this.typeFilter, this.nbrePages);
    },
    searchWord(newValue) {
      this.searchTerm = newValue;
      chatServices.getOnlineUsers().then(users => {
        chatServices.getUserChatRooms(eXo.chat.userSettings, users, this.searchTerm).then(chatRoomsData => {
          this.contactList = chatRoomsData.rooms;
        });
      });
    }
  },
  created() {
    this.$userService.getUser(eXo.env.portal.userName).then(user => { this.isExternal = user.external === 'true'; });
    document.addEventListener(chatConstants.EVENT_ROOM_MEMBER_LEFT, this.leftRoom);
    document.addEventListener(chatConstants.EVENT_ROOM_DELETED, this.leftRoom);
    document.addEventListener(chatConstants.EVENT_ROOM_MEMBER_JOINED, this.joinedToNewRoom);
    document.addEventListener(chatConstants.EVENT_ROOM_FAVORITE_ADDED, this.favoriteAdded);
    document.addEventListener(chatConstants.EVENT_ROOM_FAVORITE_REMOVED, this.favoriteRemoved);
    document.addEventListener(chatConstants.EVENT_MESSAGE_RECEIVED, this.messageReceived);
    document.addEventListener(chatConstants.EVENT_MESSAGE_SENT, this.messageSent);
    document.addEventListener(chatConstants.EVENT_USER_STATUS_CHANGED, this.contactStatusChanged);
    document.addEventListener(chatConstants.EVENT_MESSAGE_READ, this.markRoomMessagesRead);
    document.addEventListener(chatConstants.ACTION_ROOM_EDIT, this.editRoom);
    document.addEventListener(chatConstants.ACTION_ROOM_LEAVE, this.leaveRoom);
    document.addEventListener(chatConstants.ACTION_ROOM_DELETE, this.deleteRoom);
    document.addEventListener(chatConstants.ACTION_ROOM_SELECT, this.selectContact);
    document.addEventListener(chatConstants.EVENT_ROOM_SELECTION_CHANGED, this.contactChanged);
    document.addEventListener(chatConstants.EVENT_MESSAGE_NOT_SENT, this.messageNotSent);
    document.addEventListener(chatConstants.ROOM_NOTIFICATION_SETTINGS_UPDATED, this.refreshNotificationSettings);
    this.typeFilter = chatWebStorage.getStoredParam(chatConstants.STORED_PARAM_TYPE_FILTER, chatConstants.TYPE_FILTER_DEFAULT);
    this.sortFilter = chatWebStorage.getStoredParam(chatConstants.STORED_PARAM_SORT_FILTER, chatConstants.SORT_FILTER_DEFAULT);
    chatWebStorage.setStoredParam(chatConstants.STORED_PARAM_TERM_FILTER, '');
    this.initFilterMobile();
    chatServices.getUserInfo(eXo.env.portal.userName).then(
      (data) => {
        this.externalUser = data && data.external === 'true';
      }
    );
    this.contactList = this.contacts.slice();
    this.contactsToDisplay = this.contacts.slice();
    this.$root.$on('quick-chat-discussion-saved', e => this.roomSaved(e));
  },
  destroyed() {
    document.removeEventListener(chatConstants.EVENT_ROOM_MEMBER_LEFT, this.leftRoom);
    document.removeEventListener(chatConstants.EVENT_ROOM_DELETED, this.leftRoom);
    document.removeEventListener(chatConstants.EVENT_ROOM_MEMBER_JOINED, this.joinedToNewRoom);
    document.removeEventListener(chatConstants.EVENT_ROOM_FAVORITE_ADDED, this.favoriteAdded);
    document.removeEventListener(chatConstants.EVENT_ROOM_FAVORITE_REMOVED, this.favoriteRemoved);
    document.removeEventListener(chatConstants.EVENT_MESSAGE_RECEIVED, this.messageReceived);
    document.removeEventListener(chatConstants.EVENT_MESSAGE_SENT, this.messageSent);
    document.removeEventListener(chatConstants.EVENT_USER_STATUS_CHANGED, this.contactStatusChanged);
    document.removeEventListener(chatConstants.EVENT_MESSAGE_READ, this.markRoomMessagesRead);
    document.removeEventListener(chatConstants.ACTION_ROOM_EDIT, this.editRoom);
    document.removeEventListener(chatConstants.ACTION_ROOM_LEAVE, this.leaveRoom);
    document.removeEventListener(chatConstants.ACTION_ROOM_DELETE, this.deleteRoom);
    document.removeEventListener(chatConstants.ACTION_ROOM_SELECT, this.selectContact);
    document.removeEventListener(chatConstants.EVENT_ROOM_SELECTION_CHANGED, this.contactChanged);
    document.removeEventListener(chatConstants.EVENT_MESSAGE_NOT_SENT, this.messageNotSent);
    document.removeEventListener(chatConstants.ROOM_NOTIFICATION_SETTINGS_UPDATED, this.refreshNotificationSettings);
  },
  methods: {
    compareSilentsForSort(a, b, v) {
      if (a.isRoomSilent && b.unreadTotal !== 0 && !b.isRoomSilent) {
        return 1;
      }
      if (!a.isRoomSilent && b.isRoomSilent && a.unreadTotal !== 0) {
        return -1;
      }
      return v;
    },
    refreshNotificationSettings(event) {
      const roomId = event.detail.data.targetRoom;
      this.filteredContacts.filter(contact => {
        if (contact.room === roomId) {
          if (event.detail.data.notificationTrigger === 'silence') {
            contact.isRoomSilent = true;
          } else {
            contact.isRoomSilent = false;
          }
        }
      });
    },
    normalizeText(s) {
      return s.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    },
    selectContact(contact) {
      if (!contact) {
        contact = {};
      }
      if (contact.detail) {
        contact = contact.detail;
      }
      if (!contact && !contact.room && contact.user) {
        contact = contact.user;
      }
      if (contact.type && contact.type === 'u' && contact.isEnabledUser === 'null'&& contact.isExternal === '') {
        chatServices.getUserState(contact.user).then(userState => {
          chatServices.updateUser(eXo.chat.userSettings, contact.user, userState.isDeleted, userState.isEnabled, userState.isExternal);
          // eslint-disable-next-line vue/no-mutating-props
          this.selected.isEnabledUser = !userState.isDeleted && userState.isEnabled ? 'true' : 'false';
          contact.isEnabledUser = this.selected.isEnabledUser;
          contact.isExternal = userState.isExternal;
        });
      }
      eXo.chat.selectedContact = contact;
      this.contactChanged(contact);
      this.$emit('contact-selected', contact);
    },
    contactChanged(e) {
      const selectedContact = e;
      if (selectedContact && selectedContact.room) {
        chatWebSocket.setRoomMessagesAsRead(selectedContact.room);
      }
    },
    markAllAsRead() {
      chatWebSocket.setRoomMessagesAsRead();
      // eslint-disable-next-line vue/no-mutating-props
      this.unreadMessages.shift();
    },
    toggleFavorite(contact) {
      chatServices.toggleFavorite(contact.room, contact.user, !contact.isFavorite).then(
        () => {
          contact.isFavorite = !contact.isFavorite;
          this.$emit('refresh-contacts', true);
          chatWebStorage.setStoredParam(chatConstants.STORED_PARAM_TERM_FILTER, this.searchTerm);         
          this.sortFilter = chatWebStorage.getStoredParam(chatConstants.STORED_PARAM_SORT_FILTER, chatConstants.SORT_FILTER_DEFAULT);
        }
      );
    },
    selectSortFilter(filter) {
      this.sortFilter = filter;
      chatWebStorage.setStoredParam(chatConstants.STORED_PARAM_TERM_FILTER, this.searchTerm);
      chatWebStorage.setStoredParam(chatConstants.STORED_PARAM_SORT_FILTER, this.sortFilter);
    },
    selectTypeFilter(filter) {
      this.typeFilter = filter;
      chatWebStorage.setStoredParam(chatConstants.STORED_PARAM_TERM_FILTER, this.searchTerm);
      chatWebStorage.setStoredParam(chatConstants.STORED_PARAM_TYPE_FILTER, this.typeFilter);
      this.nbrePages = 0;
      this.$emit('change-filter-type', this.searchTerm, this.typeFilter, this.nbrePages);
      this.$emit('refresh-contacts', true);
    },
    initFilterMobile() {
      this.typeFilterMobile = this.typeFilter;
      this.sortFilterMobile = this.sortFilter;
    },
    saveFilterMobile() {
      this.filterMenuClosed = true;
      this.selectTypeFilter(this.typeFilterMobile);
      this.selectSortFilter(this.sortFilterMobile);
      if (this.allAsReadFilterMobile) {
        this.markAllAsRead();
      }
      this.allAsReadFilterMobile = false;
    },
    cancelFilterMobile() {
      this.filterMenuClosed = true;
      this.allAsReadFilterMobile = false;
      this.initFilterMobile();
    },
    selectContactSearch() {
      this.contactSearchMobile = true;
      this.$nextTick(() => this.$refs.contactSearch.focus());
    },
    closeContactSearch() {
      this.contactSearchMobile = false;
      this.searchTerm = '';
    },
    openCreateRoomModal() {
      this.newRoom = {};
      this.createRoomModal = true;
    },
    messageNotSent(e) {
      const foundContact = this.findContact(e.detail.room);
      if (foundContact) {
        foundContact.hasNotSentMessages = true;
        this.$forceUpdate();
      }
    },
    removeObjectIfExists(array, room) {
      const index = array.findIndex((e) => e.room === room);
      if (index > -1) {
        array.splice(index, 1);
      }
    },
    pushObjectIfNotExists(array, object) {
      const index = array.findIndex((e) => e.msgId === object.msgId);
      if (index === -1) {
        array.push(object);
        return true;
      } else {
        array[index] = object;
        return false;
      }
    },
    messageReceived() {
      this.$emit('refresh-contacts', true);
      this.$forceUpdate();
    },
    messageSent(event) {
      const message = event.detail;
      const foundContact = this.findContactByRoomOrUser(message.room, message.data ? message.data.user : message.sender);
      if (foundContact) {
        if (!foundContact.lastMessage) {
          foundContact.lastMessage = {};
        }
        foundContact.lastMessage = message.data;
        foundContact.timestamp = message.data.timestamp;

        if (eXo.env.portal.userName === message.sender) {
          const chatRoomId = foundContact.room;
          const chatMessageType = message.data.options && message.data.options.type || 'message';
          const chatRoomType = foundContact.type === 'u' && 'user' || foundContact.type === 's' && 'space' || foundContact.type === 't' && 'team' || null;
          const chatMessageLength = message.data.msg && message.data.msg.length || 0;
          const participantsCount = foundContact.type === 'u' ? 1 : foundContact.participantsCount;
          const activeParticipantsCount = foundContact.type === 'u' ? 1 : foundContact.activeParticipantsCount;
          const statisticData = {
            module: 'chat',
            subModule: 'chat-message',
            operation: 'sendChatMessage',
            userName: eXo.env.portal.userName,
            spacePrettyName: foundContact.prettyName,
            portalUri: eXo.env.server.portalBaseURL,
            parameters: {
              participantsCount,
              activeParticipantsCount,
              chatRoomId,
              chatRoomType,
              chatMessageType,
              chatMessageLength,
            },
          };
          document.dispatchEvent(new CustomEvent(chatConstants.EVENT_MESSAGE_STATISTIC, {detail: statisticData}));
        }
      }
    },
    roomSaved(room) {
      this.createRoomModal = false;
      this.selectContact(room);
    },
    editRoom() {
      chatServices.getRoomParticipants(eXo.chat.userSettings, this.selected, null, 0).then(data => {
        // eslint-disable-next-line vue/no-mutating-props
        this.selected.participants = data.users;
        this.newRoom = JSON.parse(JSON.stringify(this.selected));
        this.createRoomModal = true;
      });
    },
    leaveRoom() {
      if (this.selected && this.selected.type === 't') {
        chatWebSocket.leaveRoom(this.selected.room);
      }
    },
    deleteRoom() {
      if (this.selected && this.selected.type === 't') {
        chatWebSocket.deleteRoom(this.selected.room);
      }
    },
    closeModal() {
      this.createRoomModal = false;
      this.newRoom = {};
    },
    markRoomMessagesRead(e) {
      const message = e.detail;
      if (message && message.room) {
        const contactToUpdate = this.findContact(message.room);
        if (contactToUpdate) {
          contactToUpdate.unreadTotal = 0;
          contactToUpdate.hasNotSentMessages = false;
          this.removeObjectIfExists(this.unreadMessages, message.room);
        }
      } else {
        this.contactsToDisplay.forEach(contact => {
          contact.unreadTotal = 0;
        });
        this.$emit('refresh-contacts', true);
        this.$forceUpdate();
      }
    },
    leftRoom(e) {
      const message = e.detail ? e.detail: e;
      const sender = message.data && message.data.sender ? message.data.sender : message.sender;
      if (message.event === 'room-member-left' && sender !== eXo.chat.userSettings.username) {
        return;
      }
      const roomLeft = message.data && message.data.room ? message.data.room : message.room;
      const roomIndex = this.contactsToDisplay.findIndex(contact => contact.room === roomLeft);
      if (roomIndex >= 0) {
        this.contactsToDisplay.splice(roomIndex, 1);
        if (this.selected && this.selected.room === roomLeft) {
          if (!this.contactsToDisplay || this.contactsToDisplay.length === 0) {
            this.selectContact();
          } else {
            if (this.filteredContacts && this.filteredContacts.length) {
              this.selectContact(this.filteredContacts[0]);
            } else {
              this.selectContact();
            }
          }
        }
      }
    },
    joinedToNewRoom(e) {
      if (!this.findContact(e.detail.room)) {
        this.contactsToDisplay.push(e.detail.data);
      }
    },
    favoriteAdded(event) {
      const room = event.room ? event.room : event.detail && event.detail.room ? event.detail.room : null;
      const contactToUpdate = this.findContact(room);
      if (contactToUpdate) {
        contactToUpdate.isFavorite = true;
      }
    },
    favoriteRemoved(event) {
      const room = event.room ? event.room : event.detail && event.detail.room ? event.detail.room : null;
      const contactToUpdate = this.findContact(room);
      if (contactToUpdate) {
        contactToUpdate.isFavorite = false;
      }
    },
    findContact(value, field) {
      if (!field)  {
        field = 'room';
      }
      const RoomId = new URL(location.href).searchParams.get('roomId');
      if (RoomId) {
        window.history.replaceState(null, null, window.location.pathname);
        const contact = this.findContact(RoomId);
        this.selectContact(contact);
      }
      return this.contactsToDisplay.find(contact => contact[field] === value);
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
    loadMore() {
      this.nbrePages++;
      this.$emit('load-more-contacts', this.searchTerm, this.typeFilter, this.nbrePages);
    },
    favoriteTooltip(contact) {
      return contact.isFavorite === true ? this.$t('exoplatform.chat.remove.favorites') : this.$t('exoplatform.chat.add.favorites');
    },
    contactTooltip(contact) {
      let fullName = contact.fullName;
      if (contact.isExternal === 'true') {
        fullName = `${fullName} (${this.external})`;
      }
      return contact.isEnabledUser === 'true' || contact.isEnabledUser === 'null' ? fullName : `${fullName} (${this.inactive})`;
    },
    getLastMessage(message, contactType) {
      if (message) {
        if (!message.isSystem) {
          const filteredMessage = this.filterLastMessage(message.msg);
          return contactType === 'u' ? message.msg : `${message.fullname}: ${filteredMessage}`;
        } else {
          let systemMsg = '';
          composerApplications.forEach(element => {
            if (message.options.type === element.type) {
              systemMsg = `<span><i class="${element.iconClass || ''}"></i>${this.$t(element.nameKey || element.labelKey || '')}</span>`;
            }
          });
          return systemMsg;
        }
      }
      return '';
    },
    getLastMessageTime(contact) {
      const timestamp = contact.lastMessage && contact.lastMessage.timestamp ? contact.lastMessage.timestamp : contact.timestamp;
      if (timestamp) {
        if (chatTime.isSameDay(timestamp, new Date().getTime())) {
          return chatTime.getTimeString(timestamp);
        } else if (timestamp === -1){
          return '';
        } else {
          return chatTime.getDayDateString(timestamp);
        }
      }
      return '';
    },
    decodeHtml(input) {
      const doc = new DOMParser().parseFromString(input, 'text/html');
      return doc.documentElement.textContent;
    },
    filterLastMessage(msg) {
      if (msg === chatConstants.DELETED_MESSAGE) {
        return this.$t('exoplatform.chat.deleted');
      }
      // replace line breaks with an ellipsis
      if (msg.indexOf('<br/>') >= 0) {
        return msg.replace(msg.slice(msg.indexOf('<br/>')),'...');
      }
      return this.decodeHtml(msg);
    },
    contactStatusChanged(e) {
      const contactChanged = e.detail;

      if (contactChanged.data && contactChanged.data.status && contactChanged.sender && contactChanged.sender.trim().length && eXo.chat.userSettings.username !== contactChanged.sender) {
        const foundContact = this.findContactByRoomOrUser(null, contactChanged.sender);
        if (foundContact) {
          foundContact.status = contactChanged.data.status;
        }
      }
    },
    openContactActions(user) {
      const contact = this.contactsToDisplay.find(contact => contact['user'] === user);
      this.contactMenuClosed = false;
      this.contactMenu = contact;
    },
    closeContactActions() {
      this.contactMenuClosed = true;
      this.contactMenu = null;
    },
    getProfileLink() {
      if (this.contactMenu.type === 'u') {
        return chatServices.getUserProfileLink(this.contactMenu.user);
      } else if (this.contactMenu.type === 's') {
        return chatServices.getSpaceProfileLink(this.contactMenu.fullName);
      }
      return '#';
    },
    openQuickCreateChatDiscussionDrawer() {
      this.$root.$emit(chatConstants.ACTION_CHAT_OPEN_QUICK_CREATE_DISCUSSION_DRAWER);
    },
  }
};
</script>
