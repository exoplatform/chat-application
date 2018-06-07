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
import {chatData} from '../chatData';

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
          fullname: null,
          isSystem: false,
          msg: null,
          msgId: null,
          options: null,
          timestamp: 0,
          type: null,
          user: null
        };
      }
    }
  },
  computed: {
    messageClass() {
      const messageType = this.notif.options ? this.notif.options.type : '';
      if(messageType) {
        switch(messageType) {
        case chatData.QUESTION_MESSAGE: return 'uiIconChatQuestion uiIconChatLightGray pull-left';
        case chatData.RAISE_HAND: return 'uiIconChatRaiseHand uiIconChatLightGray pull-left';
        case chatData.FILE_MESSAGE: return 'uiIconChatUpload uiIconChatLightGray pull-left';
        case chatData.LINK_MESSAGE: return 'uiIconChatLink uiIconChatLightGray pull-left';
        case chatData.EVENT_MESSAGE: return 'uiIconChatCreateEvent uiIconChatLightGray pull-left';
        case chatData.NOTES_MESSAGE: return 'uiIconChatMeeting uiIconChatLightGray pull-left';
        case chatData.MEETING_START_MESSAGE: return 'uiIconChatMeeting uiIconChatLightGray pull-left';
        case chatData.MEETING_STOP_MESSAGE: return 'uiIconChatMeeting uiIconChatLightGray pull-left';
        case chatData.ADD_USER_MESSAGE: return '';
        case chatData.REMOVE_USER_MESSAGE: return '';
        case chatData.CALL_JOIN_MESSAGE: return 'uiIconChatAddPeopleToMeeting uiIconChatLightGray pull-left';
        case chatData.CALL_ON_MESSAGE: return 'uiIconChatStartCall uiIconChatLightGray pull-left';
        case chatData.CALL_OFF_MESSAGE: return 'uiIconChatFinishCall uiIconChatLightGray pull-left';
        default:
          return this.specificMessageClass;
        }
      }
      return '';
    },
    isSpecificMessageType() {
      return this.message && this.message.options && this.message.options.type
        && eXo.chat && eXo.chat.message && eXo.chat.message.notifs
        && eXo.chat.message.notifs[this.message.options.type];
    },
    specificMessageObj() {
      if (this.isSpecificMessageType) {
        return eXo.chat.message.notifs[this.message.options.type];
      }
      return {};
    },
    specificMessageContent() {
      if(this.specificMessageObj.html) {
        return this.specificMessageObj.html(this.message, this.$t);
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
        case chatData.EVENT_MESSAGE : content = this.notif.options.summary; break;
        case chatData.LINK_MESSAGE : content = this.notif.options.link; break;
        case chatData.NOTES_MESSAGE : content = this.$t('exoplatform.chat.notes.saved'); break;
        case chatData.MEETING_START_MESSAGE : content = this.$t('exoplatform.chat.meeting.started'); break;
        case chatData.MEETING_STOP_MESSAGE : content = this.$t('exoplatform.chat.meeting.finished'); break;
        case chatData.ADD_USER_MESSAGE : content = this.$t('exoplatform.chat.team.msg.adduser', {0: this.notif.options.fullname, 1: this.notif.options.users}); break;
        case chatData.REMOVE_USER_MESSAGE : content = this.$t('exoplatform.chat.team.msg.removeuser', {0: this.notif.options.fullnam,1: this.notif.options.users}); break;
        case chatData.CALL_JOIN_MESSAGE : content = this.$t('exoplatform.chat.meeting.joined'); break;
        case chatData.CALL_ON_MESSAGE : content = this.$t('exoplatform.chat.meeting.started'); break;
        case chatData.CALL_OFF_MESSAGE : content = this.$t('exoplatform.chat.meeting.finished'); break;
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