<template>
  <ul class="dropdown-menu pull-right">
    <li v-if="totalUnreadMsg > 0 && messagesList.length === 0 && isRetrievingMessagges" class="chat-notification-loading">{{ $t('exoplatform.chat.loading') }}</li>
    <li v-if="onsiteNotif && totalUnreadMsg > 0 && !isRetrievingMessagges && messagesList.length > 0" id="chat-notifications-details">
      <exo-chat-notif-detail
        v-for="(messages, room) in messagesFiltered"
        v-if="messages && messages.length"
        :key="room"
        :notif="messages[0]"
        :room="room"
        class="message"
        @select-room="$emit('select-room', room)">
      </exo-chat-notif-detail>
    </li>
    <li v-if="onsiteNotif && (isRetrievingMessagges || messagesList.length > 0)" class="divider">&nbsp;</li>
    <li v-for="(value, key) in statusMap" v-if="key !== 'offline'" :class="`user-${key}`" :key="key" @click="$emit('set-status', key)">
      <a href="#">
        <span>
          <i class="uiIconStatus"></i>
        </span>
        {{ value }}
      </a>
    </li>
    <li class="divider">&nbsp;</li>
    <li>
      <a href="/portal/intranet/chat" class="notif-chat-open-link" target="_chat">
        <i class="uiIconBannerChat"></i>
        <span class="chat-label-status">{{ $t('exoplatform.chat.open.chat') }}</span>
      </a>
    </li>
  </ul>
</template>

<script>
import * as chatServices from '../chatServices';

export default {
  props: {
    totalUnreadMsg: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    onsiteNotif: {
      type: Boolean,
      default: true
    },
    statusMap: {
      type: Object,
      default: function() {
        return {
          available: this.$t('exoplatform.chat.available'),
          away: this.$t('exoplatform.chat.away'),
          donotdisturb: this.$t('exoplatform.chat.donotdisturb'),
          invisible: this.$t('exoplatform.chat.invisible'),
          offline: this.$t('exoplatform.chat.button.offline')
        };
      }
    }
  },
  data() {
    return {
      messagesList: [],
      isRetrievingMessagges: false,
      connected: true
    };
  },
  computed: {
    messagesFiltered() {
      if(this.messagesList && this.messagesList.length > 0) {
        const rooms = this.messagesList.map(message => {
          return message.categoryId ? message.categoryId : '';
        }).reduce((result, current) => {
          const isNotPresent = current && current.length && result.indexOf(current) === -1;
          return isNotPresent ? result.concat(current) : result;
        }, []);
        const messagesMap = {};
        rooms.forEach(room => {
          const subMessages = this.messagesList.filter(message => message.categoryId === room);
          if(subMessages && subMessages.length) {
            subMessages.sort(function(a, b) {
              return b.timestamp - a.timestamp;
            });
            messagesMap[room] = subMessages;
          }
        });
        return messagesMap;
      } else {
        return [];
      }
    }
  },
  created() {
    document.addEventListener(this.$constants.EVENT_MESSAGE_UPDATED, this.messageReceived);
    document.addEventListener(this.$constants.EVENT_MESSAGE_DELETED, this.messageReceived);
    document.addEventListener(this.$constants.EVENT_GLOBAL_UNREAD_COUNT_UPDATED, this.messageReceived);
  },
  destroyed() {
    document.removeEventListener(this.$constants.EVENT_MESSAGE_UPDATED, this.messageReceived);
    document.removeEventListener(this.$constants.EVENT_MESSAGE_DELETED, this.messageReceived);
    document.removeEventListener(this.$constants.EVENT_GLOBAL_UNREAD_COUNT_UPDATED, this.messageReceived);
  },
  methods: {
    refreshMessages() {
      this.messagesList = [];
      this.isRetrievingMessagges = true;
      chatServices.getNotReadMessages(eXo.chat.userSettings, true).then(messages => {
        this.messagesList = messages && messages.notifications ? messages.notifications : [];
        this.isRetrievingMessagges = false;
      }).catch(() => {
        this.isRetrievingMessagges = false;
      });
    },
    messageReceived() {
      if($('#chatApplicationNotification .status-dropdown').hasClass('open')) {
        chatServices.getNotReadMessages(eXo.chat.userSettings, true).then(messages => {
          this.messagesList = messages && messages.notifications ? messages.notifications : [];
        });
      }
    }
  }
};
</script>
