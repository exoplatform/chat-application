<template>
  <div ref="message" :id="messageId" :class="{'chat-message-not-sent': message.notSent, 'is-same-contact': hideAvatar, 'is-current-user': isCurrentUser}" class="chat-message-box">
    <div class="chat-sender-avatar">
      <div v-if="!message.isSystem && !hideAvatar && !isCurrentUser" :style="`backgroundImage: url(${contactAvatar}`" class="chat-contact-avatar"></div>
    </div>
    <div class="chat-message-bubble">
      <div v-if="!message.isSystem && !hideAvatar && !isCurrentUser" class="sender-name">{{ message.fullname }} :</div>

      <div v-if="message.type === constants.DELETED_MESSAGE" class="message-content">
        <em class="muted">{{ $t('exoplatform.chat.deleted') }}</em>
      </div>
      <div v-else-if="!message.isSystem" class="message-content" v-html="messageFiltered"></div>
      <div v-else-if="message.options.type === constants.ADD_TEAM_MESSAGE" class="message-content">
        <span v-html="$t('exoplatform.chat.team.msg.adduser', roomAddOrDeleteI18NParams)"></span>
      </div>
      <div v-else-if="message.options.type === constants.ROOM_MEMBER_LEFT" class="message-content" v-html="unescapeHTML($t('exoplatform.chat.team.userLeft', {0: message.options.fullName}))">
      </div>
      <div v-else-if="message.options.type === constants.REMOVE_TEAM_MESSAGE" class="message-content">
        <span v-html="$t('exoplatform.chat.team.msg.removeuser', roomAddOrDeleteI18NParams)"></span>
      </div>
      <div v-else-if="message.options.type === constants.TASK_MESSAGE" class="message-content">
        <b>
          <a :href="message.options.url" target="_blank">{{ message.options.task }}</a>
        </b>
        <div class="custom-message-item">
          <span>
            <i class="uiIconChatAssign"></i>
            {{ $t('exoplatform.chat.assign.to') }}:
          </span>
          <b>{{ message.options.username || $t('exoplatform.chat.assign.to.none') }}</b>
        </div>
        <div class="custom-message-item"><span><i class="uiIconChatClock"></i>{{ $t('exoplatform.chat.due.date') }}: </span><b>{{ message.options.dueDate || $t('exoplatform.chat.due.date.none') }}</b></div>
      </div>
      <div v-else-if="message.options.type === constants.EVENT_MESSAGE" class="message-content">
        <b>{{ message.options.summary }}</b>
        <div class="custom-message-item">
          <i class="uiIconChatClock"></i>
          <span>De </span>
          <b>{{ message.options.startDate }} {{ message.options.startAllDay ? this.$t('exoplatform.chat.all.day') : message.options.startTime }}</b>
          <span> à </span>
          <b>{{ message.options.endDate }} {{ message.options.endAllDay ? this.$t('exoplatform.chat.all.day') : message.options.endTime }}</b>
        </div>
        <div v-if="message.options.location" class="custom-message-item">
          <i class="uiIconChatCheckin"></i>
          {{ message.options.location }}
        </div>
      </div>
      <div v-else-if="message.options.type === constants.FILE_MESSAGE" class="message-content">
        <b>
          <a :href="message.options.restPath" target="_blank">{{ message.options.title }}</a>
        </b>
        <span class="message-file-size">{{ message.options.sizeLabel }}</span>
        <div v-show="message.options.thumbnailURL" :id="`${messageId}-attachmentContainer`" class="attachmentContainer">
          <div class="attachmentContentImage">
            <div class="imageAttachmentBox">
              <a class="imgAttach">
                <img :src="message.options.thumbnailURL" :alt="message.options.title" @error="deleteThumbnail">
              </a>
              <div class="actionAttachImg">
                <p>
                  <a :href="message.options.restPath" target="_blank">
                    <i class="uiIconSearch uiIconWhite"></i>
                  </a>
                </p>
                <p>
                  <a :href="message.options.downloadLink" target="_blank">
                    <i class="uiIconDownload uiIconWhite"></i>
                  </a>
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-else-if="message.options.type === constants.LINK_MESSAGE" class="message-content">
        <a :href="message.options.link" target="_blank">{{ message.options.link }}</a>
      </div>
      <div v-else-if="(message.options.type === constants.RAISE_HAND || message.options.type === constants.QUESTION_MESSAGE)" class="message-content">
        <b>{{ messageContent }}</b>
      </div>
      <div v-else-if="message.options.type === constants.START_MEETING_MESSAGE" class="message-content">
        <b>{{ $t('exoplatform.chat.meeting.started') }}</b>
        <div>
          <em class="muted">{{ $t('exoplatform.chat.meeting.started.message') }}</em>
        </div>
      </div>
      <div v-else-if="(message.options.type === constants.NOTES_MESSAGE || message.options.type === constants.STOP_MEETING_MESSAGE)" class="message-content msMeetingNotes">
        <b>{{ $t('exoplatform.chat.notes.saved') }}</b>
        <br />
        <div class="custom-message-item" @click="sendMeetingNotes">
          <i class="uiIconChatSendEmail"></i>
          <a class="btn-link send-meeting-notes">{{ $t('exoplatform.chat.notes') }}</a>
        </div>
        <div class="custom-message-item" @click="saveMeetingNotes">
          <i class="uiIconChatWiki"></i>
          <a class="btn-link save-meeting-notes">{{ $t('exoplatform.chat.save.wiki') }}</a>
        </div>
        <div class="alert alert-success meetingNotesSent" style="display:none;">
          <button type="button" class="close" style="right: 0;">×</button>
          <strong>{{ $t('exoplatform.chat.sent') }}</strong>
          {{ $t('exoplatform.chat.check.mailbox') }}
        </div>
        <div :id="message.timestamp" class="alert alert-success meetingNotesSaved" style="display:none;">
          <button type="button" class="close" style="right: 0;">×</button>
          <strong>{{ $t('exoplatform.chat.saved') }}</strong>
          <a href="#" target="_blank">{{ $t('exoplatform.chat.open.wiki') }}</a>.
        </div>
      </div>
      <div class="message-description">
        <i v-if="isEditedMessage" class="uiIconChatEdit"></i>
        <i v-if="message.options && message.options.type === constants.RAISE_HAND" class="uiIconChatRaiseHand"></i>
        <i v-if="message.options && message.options.type === constants.QUESTION_MESSAGE" class="uiIconChatQuestion"></i>
        <i v-if="message.options && message.options.type === constants.LINK_MESSAGE" class="uiIconChatLink"></i>
        <i v-if="message.options && message.options.type === constants.FILE_MESSAGE" class="uiIconChatUpload"></i>
        <i v-if="message.options && message.options.type === constants.TASK_MESSAGE" class="uiIconChatCreateTask"></i>
        <i v-if="message.options && message.options.type === constants.EVENT_MESSAGE" class="uiIconChatCreateEvent"></i>
        <i v-exo-tooltip.top="$t('exoplatform.chat.msg.notDelivered')" class="uiIconNotification"></i>
      </div>
    </div>
    <div class="chat-message-action">
      <dropdown-select v-if="displayActions" class="message-actions" position="right">
        <i slot="toggle" class="uiIconDots" @click="setActionsPosition"></i>
        <li slot="menu">
          <a v-for="messageAction in messageActions" :key="message.msgId + messageAction.key" :id="message.msgId + messageAction.key" :class="messageAction.class" href="#" @click="executeAction(messageAction.key)">
            {{ $t(messageAction.labelKey) }}
          </a>
        </li>
      </dropdown-select>
      <div v-if="!hideTime" class="message-time">{{ dateString }}</div>
    </div>
  </div>
</template>

<script>
import * as chatTime from '../chatTime';
import * as chatServices from '../chatServices';
import messageFilter from '../messageFilter.js';
import DropdownSelect from './DropdownSelect.vue';

export default {
  components: { DropdownSelect },
  props: {
    miniChat: {
      type: Boolean,
      default: false
    },
    message: {
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
    },
    room: {
      type: String,
      default: ''
    },
    roomFullname: {
      type: String,
      default: ''
    },
    hideAvatar: {
      type: Boolean,
      default: false
    },
    highlight: {
      type: String,
      default: ''
    },
    hideTime: {
      type: Boolean,
      default: false
    }
  },
  data: function() {
    return {
      isCurrentUser: eXo.chat.userSettings.username === this.message.user,
      constants: {
        ADD_TEAM_MESSAGE: 'type-add-team-user',
        REMOVE_TEAM_MESSAGE: 'type-remove-team-user',
        TASK_MESSAGE: 'type-task',
        EVENT_MESSAGE: 'type-event',
        FILE_MESSAGE: 'type-file',
        LINK_MESSAGE: 'type-link',
        RAISE_HAND: 'type-hand',
        QUESTION_MESSAGE: 'type-question',
        DELETED_MESSAGE: 'DELETED',
        EDITED_MESSAGE: 'EDITED',
        START_MEETING_MESSAGE: 'type-meeting-start',
        STOP_MEETING_MESSAGE: 'type-meeting-stop',
        NOTES_MESSAGE: 'type-notes',
        ROOM_MEMBER_LEFT: 'room-member-left',
        ANIMATION_PERIOD: 200,
        ANIMATION_DURATION: 1000,
        DEFAULT_MESSAGE_ACTIONS: [
          {
            key: 'edit',
            labelKey: 'exoplatform.chat.msg.edit',
            enabled: comp => {
              return (
                !comp.message.isSystem &&
                !comp.message.isDeleted &&
                !comp.message.notSent &&
                comp.isCurrentUser
              );
            }
          },
          {
            key: 'delete',
            labelKey: 'exoplatform.chat.delete',
            enabled: comp => {
              return (
                !comp.message.isSystem &&
                !comp.message.isDeleted &&
                !comp.message.notSent &&
                comp.isCurrentUser
              );
            }
          },
          {
            key: 'quote',
            labelKey: 'exoplatform.chat.quote',
            enabled: comp => {
              return !comp.message.isSystem && !comp.message.isDeleted && !comp.message.notSent;
            }
          },
          {
            key: 'saveNotes',
            labelKey: 'exoplatform.chat.notes',
            enabled: comp => {
              return !comp.message.isSystem && !comp.message.isDeleted && !comp.message.notSent;
            }
          }
        ]
      }
    };
  },
  computed: {
    messageActions() {
      if (
        eXo &&
        eXo.chat &&
        eXo.chat.message &&
        eXo.chat.message.extraActions
      ) {
        return this.constants.DEFAULT_MESSAGE_ACTIONS.concat(
          eXo.chat.message.extraActions
        ).filter(menu => !menu.enabled || menu.enabled(this));
      } else {
        return this.constants.DEFAULT_MESSAGE_ACTIONS.filter(
          menu => !menu.enabled || menu.enabled(this)
        );
      }
    },
    displayActions() {
      return !this.miniChat && !this.message.isDeleted && this.messageActions && this.messageActions.length;
    },
    messageId() {
      return this.message.clientId ? this.message.clientId : this.message.msgId;
    },
    dateString() {
      return chatTime.getTimeString(this.message.timestamp);
    },
    contactAvatar() {
      return chatServices.getUserAvatar(this.message.user);
    },
    isEditedMessage() {
      return this.message.type &&
        this.message.type === this.constants.EDITED_MESSAGE
        ? true
        : false;
    },
    roomAddOrDeleteI18NParams() {
      return {
        0: `<b>${this.message.options.fullname}</b>`,
        1: `<b>${this.message.options.users}</b>`
      };
    },
    messageContent() {
      return this.message.message ? this.message.message : this.message.msg;
    },
    messageFiltered() {
      return messageFilter(this.messageContent, this.highlight, this.EMOTICONS);
    }
  },
  created() {
    document.addEventListener('exo-chat-message-acton-edit', this.editMessage);
    document.addEventListener(
      'exo-chat-message-acton-delete',
      this.deleteMessage
    );
    document.addEventListener(
      'exo-chat-message-acton-saveNotes',
      this.saveNotes
    );
    if (this.message) {
      this.message.isDeleted =
        this.message.type === this.constants.DELETED_MESSAGE;
    }
  },
  destroyed() {
    document.removeEventListener(
      'exo-chat-message-acton-edit',
      this.editMessage
    );
    document.removeEventListener(
      'exo-chat-message-acton-delete',
      this.deleteMessage
    );
    document.removeEventListener(
      'exo-chat-message-acton-saveNotes',
      this.saveNotes
    );
  },
  methods: {
    deleteThumbnail() {
      $(`#${this.messageId} .attachmentContainer`).remove();
    },
    executeAction(actionName) {
      document.dispatchEvent(
        new CustomEvent(`exo-chat-message-acton-${actionName}`, {
          detail: this.message
        })
      );
    },
    editMessage(e) {
      if (
        !e ||
        !e.detail ||
        !e.detail.msgId ||
        e.detail.msgId !== this.message.msgId
      ) {
        return;
      }
      this.$emit('edit-message', this.message);
    },
    deleteMessage(e) {
      if (
        !e ||
        !e.detail ||
        !e.detail.msgId ||
        e.detail.msgId !== this.message.msgId
      ) {
        return;
      }
      this.message.isDeleted = true;
      this.message.isSystem = true;
      document.dispatchEvent(
        new CustomEvent('exo-chat-message-todelete', {
          detail: { room: this.room, msgId: this.message.msgId }
        })
      );
    },
    saveNotes(e) {
      if (
        !e ||
        !e.detail ||
        !e.detail.msgId ||
        e.detail.msgId !== this.message.msgId
      ) {
        return;
      }
      const messageToSend = {
        message: this.newMessage,
        room: this.room,
        clientId: new Date().getTime().toString(),
        timestamp: Date.now(),
        user: eXo.chat.userSettings.username,
        isSystem: true,
        options: {
          type: 'type-notes',
          fromTimestamp: this.message.timestamp
            ? this.message.timestamp
            : this.message.options.timestamp,
          fromUser: eXo.chat.userSettings.username,
          fromFullname: eXo.chat.userSettings.fullName
        }
      };
      document.dispatchEvent(
        new CustomEvent('exo-chat-message-tosend', { detail: messageToSend })
      );
    },
    sendMeetingNotes() {
      const $chatMessageDetailContainer = $(`#${this.message.timestamp}`);
      const $meetingNotes = $chatMessageDetailContainer.closest(
        '.msMeetingNotes'
      );
      $meetingNotes.animate(
        { opacity: 'toggle' },
        this.constants.ANIMATION_PERIOD,
        () => {
          const room = this.room;
          let from = this.message.options.fromTimestamp;
          let to = this.message.timestamp;

          from = Math.round(from) - 1;
          to = Math.round(to) + 1;

          $meetingNotes.find('.meetingNotesSent').hide();
          const thiss = this;
          chatServices
            .sendMeetingNotes(eXo.chat.userSettings, room, from, to)
            .then(response => {
              if (response === 'sent') {
                $meetingNotes.find('.meetingNotesSent').animate(
                  { opacity: 'toggle' },
                  thiss.constants.ANIMATION_PERIOD,
                  () => {
                    $meetingNotes.animate(
                      { opacity: 'toggle' },
                      thiss.constants.ANIMATION_DURATION
                    );
                  }
                );
              }
            });
        }
      );
    },
    unescapeHTML(html) {
      return unescape(html);
    },
    saveMeetingNotes() {
      const $chatMessageDetailContainer = $(`#${this.message.timestamp}`);
      const $meetingNotes = $chatMessageDetailContainer.closest(
        '.msMeetingNotes'
      );
      $meetingNotes.animate(
        { opacity: 'toggle' },
        this.constants.ANIMATION_PERIOD,
        () => {
          const room = this.room;
          let from = this.message.options.fromTimestamp;
          let to = this.message.timestamp;

          from = Math.round(from) - 1;
          to = Math.round(to) + 1;

          $meetingNotes.find('.meetingNotesSaved').hide();

          const thiss = this;
          chatServices.getMeetingNotes(eXo.chat.userSettings, room, from, to).then(content => {
            chatServices
              .saveWiki(eXo.chat.userSettings, this.roomFullname, content)
              .then(data => {
                if (data.path && data.path.trim().length) {
                  const wikiPageURI = `${eXo.env.portal.context}/${eXo.env.portal.portalName}${data.path}`;
                  const options = {
                    type: 'type-link',
                    link: wikiPageURI,
                    from: eXo.chat.userSettings.username,
                    fullname: this.roomFullname
                  };
                  const msg = this.$t('exoplatform.chat.meeting.notes');

                  const messageToSend = {
                    message: msg,
                    room: this.room,
                    clientId: new Date().getTime().toString(),
                    timestamp: Date.now(),
                    user: eXo.chat.userSettings.username,
                    isSystem: true,
                    options: options
                  };
                  document.dispatchEvent(
                    new CustomEvent('exo-chat-message-tosend', {
                      detail: messageToSend
                    })
                  );

                  $meetingNotes.find('.meetingNotesSaved a').attr('href', wikiPageURI);
                  $meetingNotes.find('.meetingNotesSaved').animate(
                    { opacity: 'toggle' },
                    thiss.constants.ANIMATION_PERIOD,
                    function() {
                      $meetingNotes.animate(
                        { opacity: 'toggle' },
                        thiss.constants.ANIMATION_DURATION
                      );
                    }
                  );
                }
              });
          });
        }
      );
    },
    setActionsPosition() {
      const $message = $(this.$refs.message);
      const $dropdown = $message.find('.dropdown');
      const $dropdownMenu = $message.find('.dropdown-menu');
      const DROPDOWN_MARGIN = 20;
      const ROOM_BAR_HEIGHT = $('.room-detail').outerHeight();

      if ($message.offset().top + $dropdownMenu.outerHeight() + DROPDOWN_MARGIN > $('#chats').outerHeight() + ROOM_BAR_HEIGHT) {
        $dropdown.addClass('top');
      } else {
        $dropdown.removeClass('top');
      }
    }
  }
};
</script>