<template>
  <div ref="message" :id="messageId" :class="{'chat-message-not-sent': message.notSent, 'is-same-contact': hideAvatar, 'is-current-user': isCurrentUser}" class="chat-message-box">
    <div class="chat-sender-avatar">
      <a v-if="displayUserInformation" :style="`backgroundImage: url(${contactAvatar}`" :href="getProfileLink(message.user)" class="chat-contact-avatar"></a>
    </div>
    <div v-hold-tap="openMessageActions" class="chat-message-bubble">
      <div v-if="displayUserInformation" class="sender-name"><a :href="getProfileLink(message.user)">{{ message.fullname }}</a> :</div>

      <div v-if="message.type === this.$constants.DELETED_MESSAGE" class="message-content">
        <em class="muted">{{ $t('exoplatform.chat.deleted') }}</em>
      </div>
      <div v-else-if="!message.isSystem" class="message-content" v-html="messageFiltered"></div>
      <div v-else-if="message.options.type === this.$constants.ADD_TEAM_MESSAGE" class="message-content">
        <span v-html="$t('exoplatform.chat.team.msg.adduser', roomAddOrDeleteI18NParams)"></span>
      </div>
      <div v-else-if="message.options.type === this.$constants.ROOM_MEMBER_LEFT" class="message-content" v-html="unescapeHTML($t('exoplatform.chat.team.msg.leaveroom', {0: '<b>' + message.options.fullName + '</b>'}))">
      </div>
      <div v-else-if="message.options.type === this.$constants.REMOVE_TEAM_MESSAGE" class="message-content">
        <span v-html="$t('exoplatform.chat.team.msg.removeuser', roomAddOrDeleteI18NParams)"></span>
      </div>
      <div v-else-if="message.options.type === this.$constants.EVENT_MESSAGE" class="message-content">
        <b>{{ message.options.summary }}</b>
        <div class="custom-message-item">
          <i class="uiIconChatClock"></i>
          <span>{{ $t('exoplatform.chat.from') }} </span>
          <b>{{ message.options.startDate }} {{ message.options.startAllDay ? this.$t('exoplatform.chat.all.day') : message.options.startTime }}</b>
          <span> {{ $t('exoplatform.chat.to') }} </span>
          <b>{{ message.options.endDate }} {{ message.options.endAllDay ? this.$t('exoplatform.chat.all.day') : message.options.endTime }}</b>
        </div>
        <div v-if="message.options.location" class="custom-message-item">
          <i class="uiIconChatCheckin"></i>
          {{ message.options.location }}
        </div>
      </div>
      <div v-else-if="message.options.type === this.$constants.FILE_MESSAGE" class="message-content">
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
      <div v-else-if="message.options.type === this.$constants.LINK_MESSAGE" class="message-content">
        <a :href="message.options.link" target="_blank">{{ message.options.link }}</a>
      </div>
      <div v-else-if="(message.options.type === this.$constants.RAISE_HAND || message.options.type === this.$constants.QUESTION_MESSAGE)" class="message-content">
        <b>{{ messageContent }}</b>
      </div>
      <div v-else-if="message.options.type === this.$constants.MEETING_START_MESSAGE" class="message-content">
        <b>{{ $t('exoplatform.chat.meeting.started') }}</b>
        <div>
          <em class="muted">{{ $t('exoplatform.chat.meeting.started.message') }}</em>
        </div>
      </div>
      <div v-else-if="(message.options.type === this.$constants.NOTES_MESSAGE || message.options.type === this.$constants.MEETING_STOP_MESSAGE)" class="message-content msMeetingNotes">
        <b>{{ $t('exoplatform.chat.notes.saved') }}</b>
        <br />
        <div class="custom-message-item">
          <i class="uiIconChatSendEmail"></i>
          <a class="btn-link send-meeting-notes" @click="sendMeetingNotes">{{ $t('exoplatform.chat.notes') }}</a>
        </div>
        <div class="custom-message-item">
          <i class="uiIconChatWiki"></i>
          <a class="btn-link save-meeting-notes" @click="saveMeetingNotes">{{ $t('exoplatform.chat.save.wiki') }}</a>
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
      <div v-else-if="isSpecificMessageType" class="message-content" v-html="specificMessageContent">
      </div>
      <div class="message-description">
        <i v-if="isEditedMessage" class="uiIconChatEdit"></i>
        <i v-else-if="message.options && message.options.type === this.$constants.RAISE_HAND" class="uiIconChatRaiseHand"></i>
        <i v-else-if="message.options && message.options.type === this.$constants.QUESTION_MESSAGE" class="uiIconChatQuestion"></i>
        <i v-else-if="message.options && message.options.type === this.$constants.LINK_MESSAGE" class="uiIconChatLink"></i>
        <i v-else-if="message.options && message.options.type === this.$constants.FILE_MESSAGE" class="uiIconChatUpload"></i>
        <i v-else-if="message.options && message.options.type === this.$constants.EVENT_MESSAGE" class="uiIconChatCreateEvent"></i>
        <i v-else-if="message.options && (message.options.type === this.$constants.MEETING_START_MESSAGE || message.options.type === this.$constants.MEETING_STOP_MESSAGE || message.options.type === this.$constants.NOTES_MESSAGE)" class="uiIconChatMeeting"></i>
        <i v-else-if="isSpecificMessageType && specificMessageClass" :class="specificMessageClass"></i>

        <i v-exo-tooltip.top="$t('exoplatform.chat.msg.notDelivered')" class="uiIconNotification"></i>
      </div>
    </div>
    <div class="chat-message-action">
      <exo-chat-dropdown-select v-if="displayActions && mq !=='mobile'" class="message-actions" position="right">
        <i slot="toggle" class="uiIconDots" @click="setActionsPosition"></i>
        <li slot="menu">
          <a v-for="messageAction in messageActions" :key="message.msgId + messageAction.key" :id="message.msgId + messageAction.key" :class="messageAction.class" href="#" @click="executeAction(messageAction)">
            {{ $t(messageAction.labelKey) }}
          </a>
        </li>
      </exo-chat-dropdown-select>
      <div v-else-if="displayActions" v-show="displayActionMobile" class="uiPopupWrapper chat-modal-mask" @click="displayActionMobile = false">
        <ul class="mobile-options filter-options">
          <li v-for="messageAction in messageActions" :key="messageAction.key">
            <a :class="messageAction.class" href="#" @click.prevent="executeAction(messageAction)">
              {{ $t(messageAction.labelKey) }}
            </a>
          </li>
        </ul>
      </div>
      <div v-if="!hideTime" class="message-time">{{ dateString }}</div>
    </div>
    <exo-modal v-show="showConfirmModal" :title="$t(confirmTitle)" @modal-closed="showConfirmModal=false">
      <div class="modal-body">
        <p>
          <span id="team-delete-window-chat-name" class="confirmationIcon" v-html="unescapeHTML($t(confirmMessage))">
          </span>
        </p>
      </div>
      <div class="uiAction uiActionBorder">
        <a id="team-delete-button-ok" href="#" class="btn btn-primary" @click="confirmAction(message);showConfirmModal=false;">{{ $t(confirmOKMessage) }}</a>
        <a id="team-delete-button-cancel" href="#" class="btn" @click="showConfirmModal=false">{{ $t(confirmKOMessage) }}</a>
      </div>
    </exo-modal>
  </div>
</template>

<script>
import * as chatTime from '../chatTime';
import * as chatServices from '../chatServices';
import {DEFAULT_MESSAGE_ACTIONS, EMOTICONS} from '../extension';
import messageFilter from '../messageFilter.js';

import ExoDropdownSelect from './ExoDropdownSelect.vue';
import ExoModal from './modal/ExoModal.vue';

export default {
  components: {
    'exo-chat-dropdown-select': ExoDropdownSelect,
    'exo-modal': ExoModal
  },
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
      showConfirmModal: false,
      displayActionMobile: false,
      confirmTitle: '',
      confirmMessage: '',
      confirmOKMessage: '',
      confirmKOMessage: '',
      confirmAction(){return;}
    };
  },
  computed: {
    isCurrentUser() {
      return eXo.chat.userSettings.username === this.message.user;
    },
    messageActions() {
      if (
        eXo &&
        eXo.chat &&
        eXo.chat.message &&
        eXo.chat.message.extraActions
      ) {
        return DEFAULT_MESSAGE_ACTIONS.concat(
          eXo.chat.message.extraActions
        ).filter(menu => !menu.enabled || menu.enabled(this));
      } else {
        return DEFAULT_MESSAGE_ACTIONS.filter(
          menu => !menu.enabled || menu.enabled(this)
        );
      }
    },
    displayUserInformation() {
      const messageType = this.message.options ? this.message.options.type : null;
      return messageType !== this.$constants.ROOM_MEMBER_LEFT && messageType !== this.$constants.REMOVE_TEAM_MESSAGE && messageType !== this.$constants.ADD_TEAM_MESSAGE && !this.hideAvatar && !this.isCurrentUser;
    },
    displayActions() {
      return !this.miniChat && this.message.type !== this.$constants.DELETED_MESSAGE && this.messageActions && this.messageActions.length;
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
        this.message.type === this.$constants.EDITED_MESSAGE
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
      return messageFilter(this.messageContent, this.highlight, EMOTICONS);
    },
    isSpecificMessageType() {
      return this.message && this.message.options && this.message.options.type
        && eXo.chat && eXo.chat.message && eXo.chat.message.types
        && eXo.chat.message.types[this.message.options.type];
    },
    specificMessageObj() {
      if (this.isSpecificMessageType) {
        return eXo.chat.message.types[this.message.options.type];
      }
      return {};
    },
    specificMessageContent() {
      if(this.specificMessageObj.html) {
        return this.specificMessageObj.html(this.message, this.$t.bind(this));
      }
      return '';
    },
    specificMessageClass() {
      return this.specificMessageObj.iconClass;
    }
  },
  created() {
    document.addEventListener(this.$constants.ACTION_MESSAGE_EDIT, this.editMessage);
    document.addEventListener(
      'exo-chat-message-action-delete-requested',
      this.deleteMessage
    );
    document.addEventListener(
      'exo-chat-message-action-saveNotes-requested',
      this.saveNotes
    );
  },
  destroyed() {
    document.removeEventListener(
      this.$constants.ACTION_MESSAGE_EDIT,
      this.editMessage
    );
    document.removeEventListener(
      'exo-chat-message-action-delete-requested',
      this.deleteMessage
    );
    document.removeEventListener(
      'exo-chat-message-action-saveNotes-requested',
      this.saveNotes
    );
  },
  methods: {
    deleteThumbnail() {
      $(`#${this.messageId}-attachmentContainer`).remove();
    },
    executeAction(messageAction) {
      if(messageAction.confirm) {
        this.confirmTitle = messageAction.confirm.title;
        this.confirmMessage = messageAction.confirm.message;
        this.confirmOKMessage = messageAction.confirm.okMessage;
        this.confirmKOMessage = messageAction.confirm.koMessage;
        this.confirmAction = (message) => document.dispatchEvent(new CustomEvent(`exo-chat-message-action-${messageAction.key}-requested`, {detail: message}));
        this.showConfirmModal = true;
      } else {
        document.dispatchEvent(new CustomEvent(`exo-chat-message-action-${messageAction.key}-requested`, {detail: this.message}));
      }
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
      document.dispatchEvent(
        new CustomEvent(this.$constants.ACTION_MESSAGE_DELETE, {
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
          type: this.$constants.NOTES_MESSAGE,
          fromTimestamp: this.message.timestamp
            ? this.message.timestamp
            : this.message.options.timestamp,
          fromUser: eXo.chat.userSettings.username,
          fromFullname: eXo.chat.userSettings.fullName
        }
      };
      document.dispatchEvent(
        new CustomEvent(this.$constants.ACTION_MESSAGE_SEND, { detail: messageToSend })
      );
    },
    sendMeetingNotes() {
      const $chatMessageDetailContainer = $(`#${this.message.timestamp}`);
      const $meetingNotes = $chatMessageDetailContainer.closest(
        '.msMeetingNotes'
      );
      const ANIMATION_PERIOD = this.$constants.ANIMATION_PERIOD;
      $meetingNotes.animate({ opacity: 'toggle' },
        ANIMATION_PERIOD, this.sendMeetingNotesAnimationDone
      );
    },
    sendMeetingNotesAnimationDone() {
      const $chatMessageDetailContainer = $(`#${this.message.timestamp}`);
      const $meetingNotes = $chatMessageDetailContainer.closest(
        '.msMeetingNotes'
      );
      const ANIMATION_PERIOD = this.$constants.ANIMATION_PERIOD;

      const room = this.room;
      let from = this.message.options.fromTimestamp;
      let to = this.message.timestamp;

      from = Math.round(from) - 1;
      to = Math.round(to) + 1;

      $meetingNotes.find('.meetingNotesSent').hide();
      chatServices
        .sendMeetingNotes(eXo.chat.userSettings, room, from, to)
        .then(response => {
          if (response === 'sent') {
            $meetingNotes.find('.meetingNotesSent').animate(
              { opacity: 'toggle' },
              ANIMATION_PERIOD,
              () => {
                $meetingNotes.animate(
                  { opacity: 'toggle' },
                  ANIMATION_PERIOD
                );
              }
            );
          }
        });
    },
    unescapeHTML(html) {
      return unescape(html);
    },
    saveMeetingNotes() {
      const $chatMessageDetailContainer = $(`#${this.message.timestamp}`);
      const $meetingNotes = $chatMessageDetailContainer.closest(
        '.msMeetingNotes'
      );
      const ANIMATION_PERIOD = this.$constants.ANIMATION_PERIOD;
      $meetingNotes.animate({ opacity: 'toggle' },
        ANIMATION_PERIOD, this.saveMeetingNotesAnimationDone
      );
    },
    saveMeetingNotesAnimationDone() {
      const $chatMessageDetailContainer = $(`#${this.message.timestamp}`);
      const $meetingNotes = $chatMessageDetailContainer.closest(
        '.msMeetingNotes'
      );
      const ANIMATION_PERIOD = this.$constants.ANIMATION_PERIOD;

      const room = this.room;
      let from = this.message.options.fromTimestamp;
      let to = this.message.timestamp;

      from = Math.round(from) - 1;
      to = Math.round(to) + 1;

      $meetingNotes.find('.meetingNotesSaved').hide();

      chatServices.getMeetingNotes(eXo.chat.userSettings, room, from, to).then(content => {
        if (!content) {
          return;
        }
        chatServices
          .saveWiki(eXo.chat.userSettings, this.roomFullname, content)
          .then(data => {
            if (data.path && data.path.trim().length) {
              const wikiPageURI = `${data.path}`;
              const options = {
                type: this.$constants.LINK_MESSAGE,
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
                new CustomEvent(this.$constants.ACTION_MESSAGE_SEND, {
                  detail: messageToSend
                })
              );

              $meetingNotes.find('.meetingNotesSaved a').attr('href', wikiPageURI);
              $meetingNotes.find('.meetingNotesSaved').animate(
                { opacity: 'toggle' },
                ANIMATION_PERIOD,
                function() {
                  $meetingNotes.animate(
                    { opacity: 'toggle' },
                    ANIMATION_PERIOD
                  );
                }
              );
            }
          });
      });
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
    },
    openMessageActions() {
      this.displayActionMobile = true;
    },
    getProfileLink(user) {
      return chatServices.getUserProfileLink(user);
    }
  }
};
</script>