<template id="chatButtonApplication"> 
  <div class="VuetifyApp">
    <div class="v-application miniChatDrawer v-application--is-ltr theme--light">
      <div class="v-application--wrap">
        <v-btn
          id="btnChatButton"
          class="dropdown-toggle"
          :class="statusClass()"
          :title="$t('Notification.chat.button.tooltip')"
          @click="openChatDrawer"
          icon>
          <v-icon size="22" class="my-auto uiIconStatus icon-default-color fas fa-comments" />
          <span :class="canShowOnSiteNotif() && totalUnreadMsg > 0 && totalUnreadMsg <= 99 ? '' : 'hidden'" class="notif-total badgeDefault badgePrimary mini">{{ totalUnreadMsg }}</span>
          <span :class="canShowOnSiteNotif() && totalUnreadMsg > 99 ? '' : 'hidden'" class="notif-total badgeDefault badgePrimary mini max">+99</span>
        </v-btn>
      </div>
    </div>
  </div>
</template>
<script>
import * as chatServices from '../../chatServices';
import {chatConstants} from '../../chatConstants';
import * as desktopNotification from '../../desktopNotification';

export default {
  data () {
    return {
      userSettings: {
        username: typeof eXo !== 'undefined' ? eXo.env.portal.userName : ''
      },
      totalUnreadMsg: 0,
    };
  },
  watch: {
    totalUnreadMsg() {
      chatServices.updateTotalUnread(this.totalUnreadMsg);
    },
  },
  created() {
    chatServices.getUserSettings(this.userSettings.username).then(userSettings => {
      this.initSettings(userSettings);
    });
    document.addEventListener(chatConstants.EVENT_GLOBAL_UNREAD_COUNT_UPDATED, this.totalUnreadMessagesUpdated);
  },
  destroyed() {
    document.removeEventListener(chatConstants.EVENT_GLOBAL_UNREAD_COUNT_UPDATED, this.totalUnreadMessagesUpdated);
  },
  methods: {
    totalUnreadMessagesUpdated(e) {
      const totalUnreadMsg = e.detail ? e.detail.data.totalUnreadMsg : e.totalUnreadMsg;
      if (totalUnreadMsg >= 0) {
        this.totalUnreadMsg = totalUnreadMsg;
      }
    },
    initSettings(userSettings) {
      this.userSettings = userSettings;
      chatServices.initSettings(userSettings.userName, userSettings, userSettings => {
        chatServices.getNotReadMessages(userSettings)
          .then(data => this.totalUnreadMsg = data?.total);
      });
    },
    initChatRooms(chatRoomsData) {
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
    canShowOnSiteNotif() {
      return desktopNotification.canShowOnSiteNotif();
    },
    statusClass() {
      if (this.userSettings.status === 'invisible') {
        return 'user-offline';
      } else {
        return `user-${this.userSettings.status}`;
      }
    },
    openChatDrawer(event){
      if (event){
        event.preventDefault();
        event.stopPropagation();
      }
      document.dispatchEvent(new CustomEvent(chatConstants.ACTION_CHAT_OPEN_DRAWER));

    }
  }
};
</script>
