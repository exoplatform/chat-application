<template>
  <div v-if="contact && Object.keys(contact).length !== 0 && contact.type != 'u'" class="uiRoomUsersContainerArea">
    <div
      ref="roomParticipants"
      :class="{collapsed: isCollapsed && mq !== 'mobile'}"
      class="room-participants">
      <div
        v-exo-tooltip.left.body="tooltipCollapse"
        v-if="mq !=='mobile'"
        class="room-users-collapse-btn"
        @click="toggleCollapsed">
        <i class="uiIcon"></i>
      </div>
      <div class="room-participants-header no-user-selection">
        <div v-if="mq == 'mobile'" @click="backToConversation"><i class="uiIconGoBack"></i></div>
        <div v-exo-tooltip.left.body="tooltipParticipants" class="room-participants-filter">
          <div
            v-show="isCollapsed && mq !== 'mobile'"
            class="actionIcon"
            @click="toggleParticipantFilter">
            <i :class="{'all-participants': participantFilterClass}" class="uiIconChatMember uiIconChatLightGray"></i>
          </div>
        </div>
        <div v-show="!isCollapsed || mq === 'mobile'" class="room-participants-title">
          {{ $t("exoplatform.chat.participants.label") }}
          <span v-show="participantsCount > 0" class="nb-participants">({{ participantsCount }})</span>
        </div>
        <exo-dropdown-select v-show="!isCollapsed || mq === 'mobile'" position="right">
          <span slot="toggle">{{ filterByStatus[participantFilter] }}</span>
          <i slot="toggle" class="uiIconArrowDownMini"></i>
          <li
            v-for="(label, filter) in filterByStatus"
            slot="menu"
            :key="filter"
            @click="selectParticipantFilter(filter)">
            <a href="#"><i :class="{'not-filter': participantFilter !== filter}" class="uiIconTick"></i>{{ label }}</a>
          </li>
        </exo-dropdown-select>
      </div>
      <div ref="roomParticipantsList" class="room-participants-list isList">
        <div
          v-for="participant in participants"
          :key="participant.name"
          class="contact-list-item">
          <exo-chat-contact
            :is-external="participant.isExternal === 'true'"
            :is-enabled="participant.isEnabled === 'true' || participant.isEnabled === 'null'"
            :list="true"
            :user-name="participant.name"
            :name="participant.fullname"
            :status="participant.status"
            type="u" />
        </div>
        <div v-show="!isCollapsed || mq === 'mobile'" class="room-participants-title">
          <span v-show="hiddenParticipantsCount > 0" class="nb-participants">++ {{ hiddenParticipantsCount }} {{ $t("exoplatform.chat.participants.more.label") }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as chatServices from '../chatServices';
import * as chatWebStorage from '../chatWebStorage';
import {chatConstants} from '../chatConstants';

export default {
  data: function() {
    return {
      isCollapsed: true,
      filterByStatus: {
        'All': this.$t('exoplatform.chat.contact.all'),
        'Online': this.$t('exoplatform.chat.online'),
      },
      participantFilter: chatConstants.STATUS_FILTER_DEFAULT,
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
      contact: null,
      /** Array of Participant
       * Participant {
       * name: {string} id of user
       * fullname: {string} full name of user
       * status: {string} if the contact is of type user, this variable determines the user status (away, offline, available...)
       * email: {string} email of user
       * }
       */
      participants: [],
      participantsCount: 0,
      displayedParticipantsCount: 0
    };
  },
  computed: {
    hiddenParticipantsCount() {
      return this.participantsCount - this.displayedParticipantsCount;
    },
    onlineUsersOnly() {
      return this.participantFilter !== 'All';
    },
    participantFilterClass() {
      return this.participantFilter === 'All';
    },
    tooltipParticipants() {
      return this.participantFilter === 'All' ? this.$t('exoplatform.chat.hide.users') : this.$t('exoplatform.chat.show.users');
    },
    tooltipCollapse() {
      return this.isCollapsed === true ? this.$t('exoplatform.chat.expand.users') : this.$t('exoplatform.chat.reduce.users');
    }
  },
  created() {
    document.addEventListener(chatConstants.EVENT_ROOM_SELECTION_CHANGED, this.contactChanged);
    document.addEventListener(chatConstants.EVENT_USER_STATUS_CHANGED, this.contactStatusChanged);
    document.addEventListener(chatConstants.ACTION_ROOM_SHOW_PARTICIPANTS, this.showParticipants);
    document.addEventListener(chatConstants.EVENT_ROOM_MEMBER_LEFT, this.leftRoom);
    this.participantFilter = chatWebStorage.getStoredParam(chatConstants.STORED_PARAM_STATUS_FILTER, chatConstants.STATUS_FILTER_DEFAULT);
  },
  destroyed() {
    document.removeEventListener(chatConstants.ACTION_ROOM_SHOW_PARTICIPANTS, this.showParticipants);
    document.removeEventListener(chatConstants.EVENT_ROOM_SELECTION_CHANGED, this.contactChanged);
    document.removeEventListener(chatConstants.EVENT_USER_STATUS_CHANGED, this.contactStatusChanged);
    document.removeEventListener(chatConstants.EVENT_ROOM_MEMBER_LEFT, this.leftRoom);
  },
  methods: {
    showParticipants() {
      this.isCollapsed = false;
    },
    toggleCollapsed() {
      this.isCollapsed = !this.isCollapsed;
    },
    selectParticipantFilter(filter) {
      chatWebStorage.setStoredParam(chatConstants.STORED_PARAM_STATUS_FILTER, filter);
      this.participantFilter = filter;
      this.calculateDisplayedContacts();
      this.loadRoomParticipants(this.contact, this.onlineUsersOnly);
    },
    toggleParticipantFilter() {
      if (this.participantFilter === 'All') {
        this.selectParticipantFilter('Online');
      } else {
        this.selectParticipantFilter('All');
      }
    },
    leftRoom(e) {
      const message = e.detail ? e.detail: e;
      const sender = message.data && message.data.sender ? message.data.sender : message.sender;
      const roomLeft = message.data && message.data.room ? message.data.room : message.room;
      if (roomLeft !== this.contact.room) {
        return;
      }
      const roomIndex = this.participants.findIndex(contact => contact.name === sender);
      if (roomIndex >= 0) {
        this.participants.splice(roomIndex, 1);
      }
      this.loadRoomParticipants(this.contact, this.onlineUsersOnly);
    },
    contactChanged(e) {
      const contact = e.detail;
      this.contact = contact;
      this.participants = [];
      this.calculateDisplayedContacts();
      if (contact !== null && contact.type && contact.type !== 'u') {
        this.loadRoomParticipants(contact, this.onlineUsersOnly);
      }
    },
    calculateDisplayedContacts() {
      this.displayedParticipantsCount = 50;
      if (this.$refs.roomParticipants && this.contact.type && this.contact.type !== 't') {
        const headerHeight = 70;
        const moreParticipantsTextHeight = 20;
        const participantsHeight = this.$refs.roomParticipants.clientHeight;
        const participantItemHeight = 36;
        const viewableParticipants = (participantsHeight - headerHeight - moreParticipantsTextHeight) / participantItemHeight;
        this.displayedParticipantsCount = Math.round(viewableParticipants);
      }
    },
    loadRoomParticipants(contact, onlineUsersOnly) {
      chatServices.getOnlineUsers().then(users => {
        //Get users count and remove the current user
        chatServices.getRoomParticipantsCount(eXo.chat.userSettings, contact).then( data => this.participantsCount = data.usersCount - 1);
        chatServices.getRoomParticipants(eXo.chat.userSettings, contact, users, this.displayedParticipantsCount, onlineUsersOnly).then( data => {
          this.participants = data.users.map(user => {
            // if user attributes deleted/enabled are null update the user.
            if (user.isEnabled === 'null') {
              chatServices.getUserState(user.name).then(userState => {
                chatServices.updateUser(eXo.chat.userSettings, user.name, userState.isDeleted, userState.isEnabled, userState.isExternal);
                user.isEnabled = userState.isEnabled;
                user.isDeleted = userState.isDeleted;
                user.isExternal = userState.isExternal;
              });
            }
            // if user is not online, set its status as offline
            if (users.indexOf(user.name) < 0) {
              user.status = 'offline';
            }
            return user;
          });
          this.$emit('participants-loaded', this.participants);
          const offline = ['invisible', 'offline'];
          this.displayedParticipantsCount = this.participants.length;
          return this.participants.sort((p1, p2) => {
            if (p1.status === 'away' && p2.status === 'available' || p1.status === 'donotdisturb' && p2.status === 'available' || p1.status === 'donotdisturb' && p2.status === 'away' || offline.indexOf(p1.status) > -1 && offline.indexOf(p2.status) < 0) {
              return 1;
            }
          });
        });
      });
    },
    contactStatusChanged(e) {
      const contact = e.detail;
      const participantToChange = this.participants.find(participant => participant.name === contact.sender);
      if (participantToChange) {
        participantToChange.status = contact.data.status;
      }
    },
    backToConversation() {
      this.$emit('back-to-conversation');
    }
  }
};
</script>
