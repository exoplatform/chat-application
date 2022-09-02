<template>
  <v-app>
    <div id="room-detail" class="room-detail">
      <div v-if="mq == 'mobile'" @click="backToContactList"><i class="uiIconGoBack"></i></div>
      <exo-chat-contact
        v-if="!showMeetingDrawer"
        :is-external="contact.isExternal === 'true'"
        :is-enabled="contact.isEnabledUser === 'true' || contact.isEnabledUser === 'null'"
        :type="contact.type"
        :user-name="contact.user"
        :pretty-name="contact.prettyName"
        :group-id="contact.groupId"
        :name="contact.fullName"
        :status="contact.status"
        :nb-members="nbMembers">
        <div
          v-exo-tooltip.bottom.body="favoriteTooltip"
          v-if="mq !== 'mobile'"
          :class="{'is-fav': contact.isFavorite}"
          class="uiIcon favorite"
          @click.stop="toggleFavorite(contact)"></div>
        <v-icon
          color="red darken-2"
          dense
          v-if="isRoomNotificationSilence">
          mdi-volume-off
        </v-icon>
      </exo-chat-contact>
      <div :class="{'search-active': showSearchRoom}" class="room-actions-container">
        <div class="room-search">
          <input
            ref="searchRoom"
            v-model="searchText"
            type="text"
            placeholder="search here"
            @blur="closeSearchRoom"
            @keyup.esc="closeSearchRoom">
          <i class="uiIconCloseLight" @click.stop.prevent="closeSearchRoom"></i>
        </div>
        <div class="room-action-menu">
          <div v-if="contact.isEnabledUser === 'null' || contact.isEnabledUser === 'true'" class="room-action-components">
            <div
              v-for="action in enabledRoomActionComponents"
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
            </div>
          </div>
          <div
            v-exo-tooltip.bottom="$t('exoplatform.chat.search')"
            class="room-search-btn"
            @click="openSearchRoom">
            <i class="uiIconSearchLight"></i>
          </div>
          <exo-dropdown-select
            v-if="displayMenu"
            class="room-settings-dropdown chat-team-button-dropdown"
            position="right"
            @click.native="checkMeetingStatus">
            <i
              v-exo-tooltip.bottom="$t('exoplatform.chat.moreActions')"
              slot="toggle"
              class="uiIconVerticalDots"></i>
            <li
              v-for="settingAction in enabledSettingActions"
              slot="menu"
              :class="`room-setting-action-${settingAction.key}`"
              :key="settingAction.key"
              @click.stop="executeAction(settingAction)">
              <a href="#">
                <i :class="settingAction.class" class="uiIconRoomSetting"></i>
                {{ $t(settingAction.labelKey) }}
              </a>
            </li>
          </exo-dropdown-select>
        </div>
      </div>
      <exo-chat-room-notification-modal
        :room="contact.room"
        :room-name="contact.fullName"
        :show="openNotificationSettings"
        @modal-closed="closeNotificationSettingsModal" />
      <exo-chat-modal
        v-show="showConfirmModal"
        :title="$t(confirmTitle)"
        @modal-closed="showConfirmModal=false">
        <div class="modal-body">
          <p>
            <span
              id="team-delete-window-chat-name"
              class="confirmationIcon"
              v-html="unescapeHTML($t(confirmMessage, {0: escapeHTML(contact.fullName)}))">
            </span>
          </p>
        </div>
        <div class="uiAction uiActionBorder">
          <a
            id="team-delete-button-ok"
            href="#"
            class="btn btn-primary"
            @click="confirmAction(contact);leaveRoomAndRemoveFavorite(contact);">{{ $t(confirmOKMessage) }}</a>
          <a
            id="team-delete-button-cancel"
            href="#"
            class="btn"
            @click="showConfirmModal=false">{{ $t(confirmKOMessage) }}</a>
        </div>
      </exo-chat-modal>
    </div>
  </v-app>
</template>

<script>
import {chatConstants} from '../chatConstants';
import * as chatServices from '../chatServices';
import {roomActionComponents} from '../extension';

export default {
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
      },
    },
    showMeetingDrawer: {
      type: Boolean,
      default: false
    },
    isRoomNotificationSilence: {
      type: Boolean,
      default() {
        return false;
      }
    },
    roomActions: {
      type: Array,
      default: function () {
        return [{}];
      }
    },
  },
  data() {
    return {
      settingActions: [],
      meetingStarted: false,
      nbMembers: 0,
      showSearchRoom: false,
      searchText: '',
      openNotificationSettings: false,
      showConfirmModal: false,
      confirmTitle: '',
      confirmMessage: '',
      confirmOKMessage: '',
      confirmKOMessage: '',
      confirmAction(){return;},
      roomActionComponents: roomActionComponents,
      settingKey: '',
    };
  },
  computed: {
    isAdmin() {
      return this.contact.admins && this.contact.admins.indexOf(eXo.chat.userSettings.username) >= 0;
    },
    favoriteTooltip() {
      return this.contact.isFavorite === true ? this.$t('exoplatform.chat.remove.favorites') : this.$t('exoplatform.chat.add.favorites');
    },
    displayMenu() {
      return this.contact.type === 's' || this.contact.type === 't';
    },
    enabledSettingActions() {
      return this.settingActions && this.settingActions.filter(settingAction => this.displayItem(settingAction)) || [];
    },
    enabledRoomActionComponents() {
      return this.roomActionComponents && this.roomActionComponents.filter(action => action.enabled) || [];
    }
  },
  watch: {
    searchText(value) {
      document.dispatchEvent(new CustomEvent(chatConstants.ACTION_MESSAGE_SEARCH, {detail: value}));
    },
    contact(newContact) {
      if (!newContact) {
        this.nbMembers = 0;
      } else {
        this.nbMembers = newContact.participantsCount ? newContact.participantsCount.length : 0;
      }
      this.meetingStarted = newContact.meetingStarted;
    }
  },
  created() {
    this.settingActions = this.roomActions;
    document.addEventListener(chatConstants.ACTION_ROOM_START_MEETING, this.startMeeting);
    document.addEventListener(chatConstants.ACTION_ROOM_STOP_MEETING, this.stopMeeting);
    document.addEventListener(chatConstants.ACTION_ROOM_OPEN_SETTINGS, this.openNotificationSettingsModal);
    document.addEventListener(chatConstants.EVENT_ROOM_PARTICIPANTS_LOADED, this.participantsLoaded);
    document.addEventListener(chatConstants.ACTION_ROOM_FAVORITE_ADD, this.addToFavorite);
    document.addEventListener(chatConstants.ACTION_ROOM_FAVORITE_REMOVE, this.removeFromFavorite);
    this.meetingStarted = this.contact && this.contact.meetingStarted;
  },
  mounted() {
    this.initRoomActionComponents();
  },
  destroyed() {
    document.removeEventListener(chatConstants.ACTION_ROOM_START_MEETING, this.startMeeting);
    document.removeEventListener(chatConstants.ACTION_ROOM_STOP_MEETING, this.stopMeeting);
    document.removeEventListener(chatConstants.ACTION_ROOM_OPEN_SETTINGS, this.openNotificationSettingsModal);
    document.removeEventListener(chatConstants.EVENT_ROOM_PARTICIPANTS_LOADED, this.participantsLoaded);
    document.removeEventListener(chatConstants.ACTION_ROOM_FAVORITE_ADD, this.addToFavorite);
    document.removeEventListener(chatConstants.ACTION_ROOM_FAVORITE_REMOVE, this.removeFromFavorite);
  },
  methods: {
    addToFavorite(e) {
      const contact = e.detail;
      chatServices.toggleFavorite(contact.room, contact.user, true).then(() => 
      {
        contact.isFavorite = true;
        this.$root.$emit('refresh-contacts', true);
      });
    },
    removeFromFavorite(e) {
      const contact = e.detail;
      chatServices.toggleFavorite(contact.room, contact.user, false).then(() => 
      {
        contact.isFavorite = false;
        this.$root.$emit('refresh-contacts', true);
      });
    },
    toggleFavorite(contact) {
      chatServices.toggleFavorite(contact.room, contact.user, !contact.isFavorite).then(() =>
      {
        contact.isFavorite = !contact.isFavorite;
        this.$root.$emit('refresh-contacts', true);
      });
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
      if (settingAction.confirm) {
        this.confirmTitle = settingAction.confirm.title;
        this.confirmMessage = settingAction.confirm.message;
        this.confirmOKMessage = settingAction.confirm.okMessage;
        this.confirmKOMessage = settingAction.confirm.koMessage;
        this.confirmAction = settingAction.confirm.confirmed;
        this.showConfirmModal = true;
        this.settingKey = settingAction.key;
      } else {
        document.dispatchEvent(new CustomEvent(`exo-chat-setting-${settingAction.key}-requested`, {'detail': this.contact}));
      }
      if (this.$children[1] && this.$children[1].$el && this.$children[1].$el.classList) {
        this.$children[1].$el.classList.remove('open');
      }
    },
    leaveRoomAndRemoveFavorite(contact) {
      if (this.settingKey === 'leaveRoom') {
        if (contact && contact.isFavorite) {
          chatServices.toggleFavorite(contact.room, contact.user, false).then(contact.isFavorite = false);
        }
        this.$root.$emit('refresh-contacts', false);
      }
      this.showConfirmModal=false;
    },
    checkMeetingStatus() {
      chatServices.getRoomDetail(eXo.chat.userSettings, this.contact.room).then(contact => {
        this.meetingStarted = contact.meetingStarted;
      });
    },
    startMeeting() {
      const room = this.contact.room;
      const now = new Date().getTime().toString();
      chatServices.updateRoomMeetingStatus(eXo.chat.userSettings, room, true, now);
      this.sendMeetingMessage(true);
    },
    stopMeeting() {
      const room = this.contact.room;
      let fromTimestamp = '';
      chatServices.getRoomDetail(eXo.chat.userSettings, this.contact.room).then(contact => {
        fromTimestamp = contact.startTime;
      });
      this.sendMeetingMessage(false, fromTimestamp);
      chatServices.updateRoomMeetingStatus(eXo.chat.userSettings, room, false, '');
    },
    sendMeetingMessage(startMeeting, fromTimestamp) {
      const msgType = startMeeting ? 'type-meeting-start' : 'type-meeting-stop';
      const message = {
        message: this.newMessage,
        room: this.contact.room,
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
      document.dispatchEvent(new CustomEvent(chatConstants.ACTION_MESSAGE_SEND, {'detail': message}));
    },
    displayItem(settingAction) {
      return (!settingAction.enabled || settingAction.enabled(this)) && (!settingAction.type || settingAction.type === this.contact.type);
    },
    unescapeHTML(html) {
      return unescape(html);
    },
    escapeHTML(html) {
      return chatServices.escapeHtml(html);
    },
    participantsLoaded() {
      this.nbMembers = this.contact && this.contact.participantsCount && this.contact.type !== 'u' ? this.contact.participantsCount : 0;
    },
    backToContactList() {
      document.dispatchEvent(new CustomEvent(chatConstants.EVENT_ROOM_SELECTION_CHANGED, {detail: {}}));
      this.$emit('back-to-contact-list');
    },
    initRoomActionComponents() {
      for (const action of this.roomActionComponents) {
        if (action.init && action.enabled) {
          let container = this.$refs[action.key];
          if (container && container.length > 0) {
            container = container[0];
          }
          action.init(container, eXo.chat);
        }
      }
    }
  }
};
</script>
