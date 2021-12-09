<template>
  <a
    class="chat-notification-detail block-item"
    href="#"
    @click="$emit('select-room', room)">
    <span class="avatarXSmall">
      <img
        :src="avatarUrl"
        onerror="this.src='/chat/img/user-default.jpg'"
        class="avatar-image">
    </span>
    <div class="chat-label-status">
      <div class="content">
        <span class="name text-link" href="#">{{ notif.fromFullName }}</span>
        <i v-if="messageClass && messageClass.length" :class="messageClass"></i>
        <div v-sanitized-html="messageContent" class="text"></div>
      </div>
      <div class="gray-box">
        <div class="timestamp time">
          {{ messageDate }}
        </div>
        <div v-if="notif.roomDisplayName && notif.roomDisplayName.length" class="team muted">
          {{ unescapeHTML(notif.roomDisplayName) }}
        </div>
      </div>
    </div>
  </a>
</template>

<script>
import * as chatTime from '../chatTime';
import * as chatServices from '../chatServices';
import {extraMessageNotifs} from '../extension';
import {chatConstants} from '../chatConstants';
import {getUserAvatar} from '../chatServices';

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
      if (messageType) {
        switch (messageType) {
        case chatConstants.QUESTION_MESSAGE: return 'uiIconChatQuestion uiIconChatLightGray pull-left';
        case chatConstants.RAISE_HAND: return 'uiIconChatRaiseHand uiIconChatLightGray pull-left';
        case chatConstants.FILE_MESSAGE: return 'uiIconChatUpload uiIconChatLightGray pull-left';
        case chatConstants.LINK_MESSAGE: return 'uiIconChatLink uiIconChatLightGray pull-left';
        case chatConstants.EVENT_MESSAGE: return 'uiIconChatCreateEvent uiIconChatLightGray pull-left';
        case chatConstants.NOTES_MESSAGE: return 'uiIconChatMeeting uiIconChatLightGray pull-left';
        case chatConstants.MEETING_START_MESSAGE: return 'uiIconChatMeeting uiIconChatLightGray pull-left';
        case chatConstants.MEETING_STOP_MESSAGE: return 'uiIconChatMeeting uiIconChatLightGray pull-left';
        case chatConstants.ADD_USER_MESSAGE: return '';
        case chatConstants.REMOVE_USER_MESSAGE: return '';
        case chatConstants.CALL_JOIN_MESSAGE: return 'uiIconChatAddPeopleToMeeting uiIconChatLightGray pull-left';
        case chatConstants.CALL_ON_MESSAGE: return 'uiIconChatStartCall uiIconChatLightGray pull-left';
        case chatConstants.CALL_OFF_MESSAGE: return 'uiIconChatFinishCall uiIconChatLightGray pull-left';
        default:
          return this.specificMessageClass;
        }
      }
      return '';
    },
    isSpecificMessageType() {
      return this.notif && this.notif.options && this.notif.options.type && this.specificMessageObj;
    },
    specificMessageObj() {
      return extraMessageNotifs.find(elm => elm.type === this.notif.options.type);
    },
    specificMessageContent() {
      if (this.specificMessageObj && this.specificMessageObj.html) {
        return this.specificMessageObj.html(this.notif, this.$t.bind(this));
      }
      return '';
    },
    specificMessageClass() {
      return this.specificMessageObj ? this.specificMessageObj.iconClass : '';
    },
    messageDate() {
      return chatTime.getTimeString(this.notif.timestamp);
    },
    messageContent() {
      const messageType = this.notif.options.type;
      let content = this.notif.content;
      if (messageType) {
        switch (messageType) {
        case chatConstants.EVENT_MESSAGE : content = this.notif.options.summary; break;
        case chatConstants.LINK_MESSAGE : content = this.notif.options.link; break;
        case chatConstants.NOTES_MESSAGE : content = this.$t('exoplatform.chat.notes.saved'); break;
        case chatConstants.ADD_USER_MESSAGE : content = this.$t('exoplatform.chat.team.msg.adduser', {0: this.notif.options.fullname, 1: this.notif.options.users}); break;
        case chatConstants.REMOVE_USER_MESSAGE : content = this.$t('exoplatform.chat.team.msg.removeuser', {0: this.notif.options.fullname,1: this.notif.options.users}); break;
        case chatConstants.ROOM_MEMBER_LEFT : content = this.$t('exoplatform.chat.team.msg.leaveroom', {0: this.notif.options.fullName}); break;
        case chatConstants.CALL_JOIN_MESSAGE : content = this.$t('exoplatform.chat.meeting.joined'); break;
        case chatConstants.CALL_ON_MESSAGE : content = this.$t('exoplatform.chat.meeting.started'); break;
        case chatConstants.CALL_OFF_MESSAGE : content = this.$t('exoplatform.chat.meeting.finished'); break;
        default:
          if (this.isSpecificMessageType) {
            content = this.specificMessageContent;
          }
        }
        content = `<a href='#'>${content}</a>`;
      } else {
        if (content.indexOf('http:') === 0 || content.indexOf('https:') === 0 || content.indexOf('ftp:') === 0) {
          content = `<a href='#'>${content}</a>`;
        }
      }
      return content;
    },
    avatarUrl() {
      return getUserAvatar(this.message.user);
    }
  },
  methods: {
    unescapeHTML(html) {
      return chatServices.unescapeHtml(html);
    }
  }
};
</script>