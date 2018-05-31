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
        case 'type-question': return 'uiIconChatQuestion uiIconChatLightGray pull-left';
        case 'type-hand': return 'uiIconChatRaiseHand uiIconChatLightGray pull-left';
        case 'type-file': return 'uiIconChatUpload uiIconChatLightGray pull-left';
        case 'type-link': return 'uiIconChatLink uiIconChatLightGray pull-left';
        case 'type-task': return 'uiIconChatCreateTask uiIconChatLightGray pull-left';
        case 'type-event': return 'uiIconChatCreateEvent uiIconChatLightGray pull-left';
        case 'type-notes': return 'uiIconChatMeeting uiIconChatLightGray pull-left';
        case 'type-meeting-start': return 'uiIconChatMeeting uiIconChatLightGray pull-left';
        case 'type-meeting-stop': return 'uiIconChatMeeting uiIconChatLightGray pull-left';
        case 'type-add-team-user': return '';
        case 'type-remove-team-user': return '';
        case 'call-join': return 'uiIconChatAddPeopleToMeeting uiIconChatLightGray pull-left';
        case 'call-on': return 'uiIconChatStartCall uiIconChatLightGray pull-left';
        case 'call-off': return 'uiIconChatFinishCall uiIconChatLightGray pull-left';
        }
      }
      return '';
    },
    messageDate() {
      return chatTime.getTimeString(this.notif.timestamp);
    },
    messageContent() {
      const messageType = this.notif.options.type;
      let content = this.notif.content;
      if (messageType) {
        switch(messageType) {
        case 'type-event' : content = this.notif.options.summary; break;
        case 'type-task' : content = this.notif.options.task; break;
        case 'type-link' : content = this.notif.options.link; break;
        case 'type-notes' : content = this.$t('exoplatform.chat.notes.saved'); break;
        case 'type-meeting-start' : content = this.$t('exoplatform.chat.meeting.started'); break;
        case 'type-meeting-stop' : content = this.$t('exoplatform.chat.meeting.finished'); break;
        case 'type-add-team-user' : content = this.$t('exoplatform.chat.team.msg.adduser', {0: this.notif.options.fullname, 1: this.notif.options.users}); break;
        case 'type-remove-team-user' : content = this.$t('exoplatform.chat.team.msg.removeuser', {0: this.notif.options.fullnam,1: this.notif.options.users}); break;
        case 'call-join' : content = this.$t('exoplatform.chat.meeting.joined'); break;
        case 'call-on' : content = this.$t('exoplatform.chat.meeting.started'); break;
        case 'call-off' : content = this.$t('exoplatform.chat.meeting.finished'); break;
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