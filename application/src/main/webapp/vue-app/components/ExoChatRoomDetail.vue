<template>
  <div id="room-detail" class="room-detail">
    <div v-if="mq == 'mobile'" @click="backToContactList"><i class="uiIconGoBack"></i></div>
    <exo-chat-contact :type="contact.type" :user-name="contact.user" :name="contact.fullName" :status="contact.status" :nb-members="nbMembers">
      <div v-exo-tooltip.bottom.body="favoriteTooltip" v-if="mq !== 'mobile'" :class="{'is-fav': contact.isFavorite}" class="uiIcon favorite" @click.stop="toggleFavorite(contact)"></div>
    </exo-chat-contact>
    <div :class="{'search-active': showSearchRoom}" class="room-actions-container">
      <div class="room-search">
        <input ref="searchRoom" v-model="searchText" type="text" placeholder="search here" @blur="closeSearchRoom" @keyup.esc="closeSearchRoom">
        <i class="uiIconCloseLight" @click.stop.prevent="closeSearchRoom"></i>
      </div>
      <div class="room-action-menu">
        <div class="callButtonContainerWrapper pull-left"></div>
        <div v-exo-tooltip.bottom="$t('exoplatform.chat.search')" class="room-search-btn" @click="openSearchRoom">
          <i class="uiIconSearchLight"></i>    
        </div>
        <exo-dropdown-select v-if="displayMenu" class="room-settings-dropdown chat-team-button-dropdown" position="right">
          <i v-exo-tooltip.bottom="$t('exoplatform.chat.moreActions')" slot="toggle" class="uiIconVerticalDots"></i>
          <li v-for="settingAction in settingActions" v-if="displayItem(settingAction)" slot="menu" :class="`room-setting-action-${settingAction.key}`" :key="settingAction.key" @click="executeAction(settingAction)">
            <a href="#">
              <i :class="settingAction.class" class="uiIconRoomSetting"></i>
              {{ $t(settingAction.labelKey) }}
            </a>
          </li>
        </exo-dropdown-select>
      </div>
    </div>
    <exo-chat-room-notification-modal :room="contact.room" :room-name="contact.fullName" :show="openNotificationSettings" @modal-closed="closeNotificationSettingsModal"></exo-chat-room-notification-modal>
    <exo-modal v-show="showConfirmModal" :title="$t(confirmTitle)" @modal-closed="showConfirmModal=false">
      <div class="modal-body">
        <p>
          <span id="team-delete-window-chat-name" class="confirmationIcon" v-html="unescapeHTML($t(confirmMessage, {0: contact.fullName}))">
          </span>
        </p>
      </div>
      <div class="uiAction uiActionBorder">
        <a id="team-delete-button-ok" href="#" class="btn btn-primary" @click="confirmAction(contact);showConfirmModal=false;">{{ $t(confirmOKMessage) }}</a>
        <a id="team-delete-button-cancel" href="#" class="btn" @click="showConfirmModal=false">{{ $t(confirmKOMessage) }}</a>
      </div>
    </exo-modal>
  </div>
</template>

<script>
import * as chatServices from '../chatServices';
import * as chatWebStorage from '../chatWebStorage';
import {DEFAULT_ROOM_ACTIONS} from '../extension';

import ExoChatContact from './ExoChatContact.vue';
import ExoChatRoomNotificationModal from './modal/ExoChatRoomNotificationModal.vue';
import ExoDropdownSelect from './ExoDropdownSelect.vue';
import ExoModal from './modal/ExoModal.vue';

export default {
  components: {
    'exo-chat-contact': ExoChatContact,
    'exo-chat-room-notification-modal': ExoChatRoomNotificationModal,
    'exo-dropdown-select': ExoDropdownSelect,
    'exo-modal': ExoModal
  },
  props: {
    /**
     * fullName: {string} full name of contact
     * isActive: {string} if the contact is of type user, this will be equals to "true" when the user is enabled
     * isFavorite: {Boolean} whether is favortie of current user or not
     * lastMessage: {string} Last message object with current user
     * room: {string} contact room id
     * status: {string} if the contact is of type user, this variable determines the user status (away, offline, available...)
     * timestamp: {number} contact update timestamp
     * type: {string} contact type, 'u' for user, 't' for team and 's' for space
     * unreadTotal: {number} unread total number of messages for this contact
     * user: {string} contact id, if user , username else team-{CONTACT_ID} or space-{CONTACT_ID}
     * Admins: {Array} Room admins list (only for room)
     */
    contact: {
      type: Object,
      default() {
        return {};
      }
    }
  },
  data() {
    return {
      nbMembers: 0,
      showSearchRoom: false,
      searchText: '',
      meetingStarted: false,
      openNotificationSettings: false,
      showConfirmModal: false,
      confirmTitle: '',
      confirmMessage: '',
      confirmOKMessage: '',
      confirmKOMessage: '',
      confirmAction(){return;}
    };
  },
  computed: {
    settingActions() {
      if(eXo && eXo.chat && eXo.chat.room && eXo.chat.room.extraActions) {
        return DEFAULT_ROOM_ACTIONS.concat(eXo.chat.room.extraActions);
      } else {
        return DEFAULT_ROOM_ACTIONS;
      }
    },
    isAdmin() {
      return this.contact.admins && this.contact.admins.indexOf(eXo.chat.userSettings.username) >= 0;
    },
    favoriteTooltip() {
      return this.contact.isFavorite === true ? this.$t('exoplatform.chat.remove.favorites') : this.$t('exoplatform.chat.add.favorites');
    },
    displayMenu() {
      return this.contact.type === 's' || this.contact.type === 't';
    }
  },
  watch: {
    searchText(value) {
      document.dispatchEvent(new CustomEvent(this.$constants.ACTION_MESSAGE_SEARCH, {detail: value}));
    },
    contact(newContact) {
      if(!newContact) {
        this.nbMembers = 0;
      } else {
        this.nbMembers = newContact.participants ? newContact.participants.length : 0;
      }
      this.meetingStarted = false;
    }
  },
  created() {
    document.addEventListener(this.$constants.ACTION_ROOM_START_MEETING, this.startMeeting);
    document.addEventListener(this.$constants.ACTION_ROOM_STOP_MEETING, this.stopMeeting);
    document.addEventListener(this.$constants.ACTION_ROOM_OPEN_SETTINGS, this.openNotificationSettingsModal);
    document.addEventListener(this.$constants.EVENT_ROOM_PARTICIPANTS_LOADED, this.participantsLoaded);
    document.addEventListener(this.$constants.ACTION_ROOM_FAVORITE_ADD, this.addToFavorite);
    document.addEventListener(this.$constants.ACTION_ROOM_FAVORITE_REMOVE, this.removeFromFavorite);
    this.meetingStarted = chatWebStorage.getStoredParam(`meetingStarted-${this.contact.room}`);
  },
  updated() {
    this.meetingStarted = chatWebStorage.getStoredParam(`meetingStarted-${this.contact.room}`);
  },
  destroyed() {
    document.removeEventListener(this.$constants.ACTION_ROOM_START_MEETING, this.startMeeting);
    document.removeEventListener(this.$constants.ACTION_ROOM_STOP_MEETING, this.stopMeeting);
    document.removeEventListener(this.$constants.ACTION_ROOM_OPEN_SETTINGS, this.openNotificationSettingsModal);
    document.removeEventListener(this.$constants.EVENT_ROOM_PARTICIPANTS_LOADED, this.participantsLoaded);
    document.removeEventListener(this.$constants.ACTION_ROOM_FAVORITE_ADD, this.addToFavorite);
    document.removeEventListener(this.$constants.ACTION_ROOM_FAVORITE_REMOVE, this.removeFromFavorite);
  },
  methods: {
    addToFavorite(e) {
      const contact = e.detail;
      chatServices.toggleFavorite(contact.room, contact.user, true).then(contact.isFavorite = true);
    },
    removeFromFavorite(e) {
      const contact = e.detail;
      chatServices.toggleFavorite(contact.room, contact.user, false).then(contact.isFavorite = false);
    },
    toggleFavorite(contact) {
      chatServices.toggleFavorite(contact.room, contact.user, !contact.isFavorite).then(contact.isFavorite = !contact.isFavorite);
    },
    openSearchRoom() {
      this.showSearchRoom = true;
      this.$nextTick(() => this.$refs.searchRoom.focus());
    },
    closeSearchRoom(e) {
      if (e.type === 'blur' && this.searchText !== '') {
        return;
      }
      this.showSearchRoom = false;
      this.searchText = '';
    },
    openNotificationSettingsModal() {
      this.openNotificationSettings = true;
    },
    closeNotificationSettingsModal() {
      this.openNotificationSettings = false;
    },
    executeAction(settingAction) {
      if(settingAction.confirm) {
        this.confirmTitle = settingAction.confirm.title;
        this.confirmMessage = settingAction.confirm.message;
        this.confirmOKMessage = settingAction.confirm.okMessage;
        this.confirmKOMessage = settingAction.confirm.koMessage;
        this.confirmAction = settingAction.confirm.confirmed;
        this.showConfirmModal = true;
      } else {
        document.dispatchEvent(new CustomEvent(`exo-chat-setting-${settingAction.key}-requested`, {'detail': this.contact}));
      }
    },
    startMeeting() {
      const room = this.contact.room;
      chatWebStorage.setStoredParam(`meetingStarted-${room}`, new Date().getTime().toString());
      this.meetingStarted = true;
      this.sendMeetingMessage(true);
    },
    stopMeeting() {
      const room = this.contact.room;
      const fromTimestamp = chatWebStorage.getStoredParam(`meetingStarted-${room}`);
      if (fromTimestamp) {
        chatWebStorage.setStoredParam(`meetingStarted-${room}`, '');
        this.meetingStarted = false;
        this.sendMeetingMessage(false, fromTimestamp);
      }
    },
    sendMeetingMessage(startMeeting, fromTimestamp) {
      const msgType = startMeeting ? 'type-meeting-start' : 'type-meeting-stop';
      const message = {
        message : this.newMessage,
        room : this.contact.room,
        clientId: new Date().getTime().toString(),
        timestamp: Date.now(),
        user: eXo.chat.userSettings.username,
        isSystem: true,
        options: {
          type: msgType,
          fromUser: eXo.chat.userSettings.username,
          fromFullname: eXo.chat.userSettings.fullName,
          fromTimestamp: fromTimestamp
        }
      };
      document.dispatchEvent(new CustomEvent(this.$constants.ACTION_MESSAGE_SEND, {'detail' : message}));
    },
    displayItem(settingAction) {
      return (!settingAction.enabled || settingAction.enabled(this)) && (!settingAction.type || settingAction.type === this.contact.type);
    },
    unescapeHTML(html) {
      return unescape(html);
    },
    participantsLoaded() {
      this.nbMembers = this.contact && this.contact.participants && this.contact.type !== 'u' ? this.contact.participants.length : 0;
    },
    backToContactList() {
      this.$emit('back-to-contact-list');
    }
  }
};
</script>