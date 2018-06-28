<template>
  <a class="chat-notification-detail block-item" href="#" @click="$emit('select-room', room)">
    <span class="avatarXSmall">
      <img :src="`/rest/v1/social/users/${notif.from}/avatar`" onerror="this.src='/chat/img/user-default.jpg'" class="avatar-image">
    </span>
    <div class="chat-label-status">
      <div class="content">
        <span class="name text-link" href="#">{{ notif.fromFullName }}</span>
        <i v-if="messageClass && messageClass.length" :class="messageClass"></i>
        <div class="text" v-html="messageContent" />
      </div>
      <div class="gray-box">
        <div class="timestamp time">
          {{ messageDate }}
        </div>
        <div v-if="notif.roomDisplayName && notif.roomDisplayName.length" class="team muted">
          {{ notif.roomDisplayName }}
        </div>
      </div>
    </div>
  </a>
</template>

<script>
import * as chatTime from '../chatTime';

export default {
  props: {
    room: {
      type: String,
      default: function() {
        return null;
      }
    },
    notif: {
      type: Object,
      default: function() {
        return {
          content: null,
          options: {
            type: null,
            fullname: null
          },
          timestamp: 0
        };
      }
    }
  },
  computed: {
    messageClass() {
      const messageType = this.notif.options ? this.notif.options.type : '';
      if(messageType) {
        switch(messageType) {
        case this.$constants.QUESTION_MESSAGE: return 'uiIconChatQuestion uiIconChatLightGray pull-left';
        case this.$constants.RAISE_HAND: return 'uiIconChatRaiseHand uiIconChatLightGray pull-left';
        case this.$constants.FILE_MESSAGE: return 'uiIconChatUpload uiIconChatLightGray pull-left';
        case this.$constants.LINK_MESSAGE: return 'uiIconChatLink uiIconChatLightGray pull-left';
        case this.$constants.EVENT_MESSAGE: return 'uiIconChatCreateEvent uiIconChatLightGray pull-left';
        case this.$constants.NOTES_MESSAGE: return 'uiIconChatMeeting uiIconChatLightGray pull-left';
        case this.$constants.MEETING_START_MESSAGE: return 'uiIconChatMeeting uiIconChatLightGray pull-left';
        case this.$constants.MEETING_STOP_MESSAGE: return 'uiIconChatMeeting uiIconChatLightGray pull-left';
        case this.$constants.ADD_USER_MESSAGE: return '';
        case this.$constants.REMOVE_USER_MESSAGE: return '';
        case this.$constants.CALL_JOIN_MESSAGE: return 'uiIconChatAddPeopleToMeeting uiIconChatLightGray pull-left';
        case this.$constants.CALL_ON_MESSAGE: return 'uiIconChatStartCall uiIconChatLightGray pull-left';
        case this.$constants.CALL_OFF_MESSAGE: return 'uiIconChatFinishCall uiIconChatLightGray pull-left';
        default:
          return this.specificMessageClass;
        }
      }
      return '';
    },
    isSpecificMessageType() {
      return this.notif && this.notif.options && this.notif.options.type
        && eXo.chat && eXo.chat.message && eXo.chat.message.notifs
        && eXo.chat.message.notifs[this.notif.options.type];
    },
    specificMessageObj() {
      if (this.isSpecificMessageType) {
        return eXo.chat.message.notifs[this.notif.options.type];
      }
      return {};
    },
    specificMessageContent() {
      if(this.specificMessageObj.html) {
        return this.specificMessageObj.html(this.notif, this.$t);
      }
      return '';
    },
    specificMessageClass() {
      return this.specificMessageObj.iconClass;
    },
    messageDate() {
      return chatTime.getTimeString(this.notif.timestamp);
    },
    messageContent() {
      const messageType = this.notif.options.type;
      let content = this.notif.content;
      if (messageType) {
        switch(messageType) {
        case this.$constants.EVENT_MESSAGE : content = this.notif.options.summary; break;
        case this.$constants.LINK_MESSAGE : content = this.notif.options.link; break;
        case this.$constants.NOTES_MESSAGE : content = this.$t('exoplatform.chat.notes.saved'); break;
        case this.$constants.MEETING_START_MESSAGE : content = this.$t('exoplatform.chat.meeting.started'); break;
        case this.$constants.MEETING_STOP_MESSAGE : content = this.$t('exoplatform.chat.meeting.finished'); break;
        case this.$constants.ADD_USER_MESSAGE : content = this.$t('exoplatform.chat.team.msg.adduser', {0: this.notif.options.fullname, 1: this.notif.options.users}); break;
        case this.$constants.REMOVE_USER_MESSAGE : content = this.$t('exoplatform.chat.team.msg.removeuser', {0: this.notif.options.fullnam,1: this.notif.options.users}); break;
        case this.$constants.CALL_JOIN_MESSAGE : content = this.$t('exoplatform.chat.meeting.joined'); break;
        case this.$constants.CALL_ON_MESSAGE : content = this.$t('exoplatform.chat.meeting.started'); break;
        case this.$constants.CALL_OFF_MESSAGE : content = this.$t('exoplatform.chat.meeting.finished'); break;
        default:
          content = this.specificMessageContent;
        }
        content = `<a href='#'>${content}</a>`;
      } else {
        if (content.indexOf('http:') === 0 || content.indexOf('https:') === 0 || content.indexOf('ftp:') === 0) {
          content = `<a href='#'>${content}</a>`;
        }
      }
      return content;
    }
  }
};
</script>