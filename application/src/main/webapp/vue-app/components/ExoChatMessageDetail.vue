<template>
  <div
    ref="message"
    :id="messageId"
    :class="{'chat-message-not-sent': message.notSent, 'is-same-contact': hideAvatar, 'is-current-user': isCurrentUser}"
    class="chat-message-box">
    <div class="chat-sender-avatar">
      <a
        v-if="displayUserInformation && (!message.isEnabledUser || message.isEnabledUser === 'true')"
        :style="`backgroundImage: url(${avatarUrl}`"
        :href="getProfileLink(message.user)"
        class="chat-contact-avatar"></a>
      <div
        v-else-if="displayUserInformation"
        :style="`backgroundImage: url(${avatarUrl}`"
        class="chat-contact-avatar"></div>
    </div>
    <div v-hold-tap="openMessageActions" class="chat-message-bubble">
      <div v-if="displayUserInformation && (!message.isEnabledUser || message.isEnabledUser === 'true')" class="sender-name"><a :href="getProfileLink(message.user)">{{ attendeeFullname }}</a> :</div>
      <div
        v-else-if="displayUserInformation"
        :class="statusStyle"
        class="sender-name">
        {{ attendeeFullname }} {{ disabledStatus }}:
      </div>

      <div v-if="messageType === chatConstants.DELETED_MESSAGE" class="message-content">
        <em class="muted">{{ $t('exoplatform.chat.deleted') }}</em>
      </div>
      <div v-else-if="messageOptionsType === chatConstants.FILE_MESSAGE"  class="message-content">
        <b :style="!downloadEnabled ? 'pointer-events: none;' : '' ">
          <a :href="message.options.restPath" target="_blank">{{ message.options.title }}</a>
        </b>
        <span class="message-file-size">{{ message.options.sizeLabel }}</span>
        <div
          v-show="message.options.thumbnailURL"
          :id="`${messageId}-attachmentContainer`"
          class="attachmentContainer">
          <div class="attachmentContentImage">
            <div class="imageAttachmentBox">
              <a class="imgAttach">
                <img
                  :src="message.options.thumbnailURL"
                  :alt="message.options.title"
                  @error="deleteThumbnail">
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
      <div
        v-sanitized-html="messageFiltered"
        v-else-if="!message.isSystem"
        class="message-content"></div>
      <div v-else-if="messageOptionsType === chatConstants.ADD_TEAM_MESSAGE" class="message-content">
        <span v-sanitized-html="$t('exoplatform.chat.team.msg.adduser', roomAddOrDeleteI18NParams)"></span>
      </div>
      <div
        v-sanitized-html="unescapeHTML($t('exoplatform.chat.team.msg.leaveroom', {0: '<b>' + message.options.fullName + '</b>'}))"
        v-else-if="messageOptionsType === chatConstants.ROOM_MEMBER_LEFT"
        class="message-content">
      </div>
      <div v-else-if="messageOptionsType === chatConstants.REMOVE_TEAM_MESSAGE" class="message-content">
        <span v-sanitized-html="$t('exoplatform.chat.team.msg.removeuser', roomAddOrDeleteI18NParams)"></span>
      </div>
      <div v-else-if="messageOptionsType === chatConstants.EVENT_MESSAGE" class="message-content">
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
      <div
        v-autolinker="message.options.link"
        v-else-if="messageOptionsType === chatConstants.LINK_MESSAGE"
        class="message-content">
      </div>
      <div v-else-if="(messageOptionsType === chatConstants.RAISE_HAND || messageOptionsType === chatConstants.QUESTION_MESSAGE)" class="message-content">
        <b>{{ messageContent }}</b>
      </div>
      <div
        v-sanitized-html="specificMessageContent"
        v-else-if="isSpecificMessageType"
        class="message-content">
      </div>
      <div class="message-description">
        <i v-if="isEditedMessage" class="uiIconChatEdit"></i>
        <i v-else-if="message.options && messageOptionsType === chatConstants.RAISE_HAND" class="uiIconChatRaiseHand"></i>
        <i v-else-if="message.options && messageOptionsType === chatConstants.QUESTION_MESSAGE" class="uiIconChatQuestion"></i>
        <i v-else-if="message.options && messageOptionsType === chatConstants.LINK_MESSAGE" class="uiIconChatLink"></i>
        <i v-else-if="message.options && messageOptionsType === chatConstants.FILE_MESSAGE" class="uiIconChatUpload"></i>
        <i v-else-if="message.options && messageOptionsType === chatConstants.EVENT_MESSAGE" class="uiIconChatCreateEvent"></i>
        <i v-else-if="message.options && (messageOptionsType === chatConstants.MEETING_START_MESSAGE || messageOptionsType === chatConstants.MEETING_STOP_MESSAGE || messageOptionsType === chatConstants.NOTES_MESSAGE)" class="uiIconChatMeeting"></i>
        <i v-else-if="isSpecificMessageType && specificMessageClass" :class="specificMessageClass"></i>
        <i
          :class="!message.notSent && 'hidden'"
          class="uiIcon16x16 primary--text clickable mdi mdi-refresh"
          @click="sendFailedMessage()"></i>
        <i
          :class="!message.notSent && 'hidden'"
          class="uiIcon16x16 delete-message-icon clickable mdi mdi-delete"
          @click="deleteFailedMessage()"></i>
      </div>
    </div>
    <div class="chat-message-action">
      <exo-dropdown-select
        v-if="displayActions && mq !=='mobile'"
        class="message-actions"
        position="right">
        <i
          slot="toggle"
          class="uiIconDots"
          @click="setActionsPosition"></i>
        <li slot="menu">
          <a
            v-for="messageAction in messageActions"
            :key="message.msgId + messageAction.key"
            :id="message.msgId + messageAction.key"
            :class="messageAction.class"
            class="actions-link"
            href="#"
            @click="executeAction(messageAction)">
            {{ $t(messageAction.labelKey) }}
          </a>
        </li>
      </exo-dropdown-select>
      <div
        v-else-if="displayActions"
        v-show="displayActionMobile"
        class="uiPopupWrapper chat-modal-mask"
        @click="displayActionMobile = false">
        <ul class="mobile-options filter-options">
          <li v-for="messageAction in messageActions" :key="messageAction.key">
            <a
              :class="messageAction.class"
              href="#"
              @click.prevent="executeAction(messageAction)">
              {{ $t(messageAction.labelKey) }}
            </a>
          </li>
        </ul>
      </div>
      <i
        v-else
        v-exo-tooltip.top="$t('exoplatform.chat.msg.notDelivered')"
        class="uiIconNotification"></i>
      <div v-if="!hideTime" class="message-time">{{ dateString }}</div>
    </div>

    <exo-chat-modal
      v-show="showConfirmModal"
      :title="$t(confirmTitle)"
      @modal-closed="showConfirmModal=false">
      <div class="modal-body">
        <p>
          <span
            v-sanitized-html="unescapeHTML($t(confirmMessage))"
            id="team-delete-window-chat-name"
            class="confirmationIcon">
          </span>
        </p>
      </div>
      <div class="uiAction uiActionBorder">
        <a
          id="team-delete-button-ok"
          href="#"
          class="btn btn-primary"
          @click="confirmAction(message);showConfirmModal=false;">{{ $t(confirmOKMessage) }}</a>
        <a
          id="team-delete-button-cancel"
          href="#"
          class="btn"
          @click="showConfirmModal=false">{{ $t(confirmKOMessage) }}</a>
      </div>
    </exo-chat-modal>
  </div>
</template>

<script>
import * as chatTime from '../chatTime';
import * as chatServices from '../chatServices';
import {messageActions, EMOTICONS, extraMessageTypes} from '../extension';
import messageFilter from '../messageFilter.js';
import {chatConstants} from '../chatConstants';
import {getUserAvatar} from '../chatServices';

export default {
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
          user: null,
          isEnabledUser: null
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
      chatConstants: chatConstants,
      showConfirmModal: false,
      displayActionMobile: false,
      external: this.$t('exoplatform.chat.external'),
      confirmTitle: '',
      confirmMessage: '',
      confirmOKMessage: '',
      confirmKOMessage: '',
      confirmAction(){return;},
      downloadEnabled: true,
    };
  },
  computed: {
    isCurrentUser() {
      return eXo.chat.userSettings.username === this.message.user;
    },
    messageType() {
      return this.message && this.message.type;
    },
    attendeeFullname() {
      return this.message && this.message.isExternal === 'true' ? `${this.message.fullname} (${this.external})` : this.message.fullname ;
    },
    messageOptionsType() {
      return this.message && this.message.options && this.message.options.type;
    },
    messageActions() {
      return messageActions.filter(menu => !menu.enabled || menu.enabled(this));
    },
    displayUserInformation() {
      return this.messageOptionsType !== chatConstants.ROOM_MEMBER_LEFT && this.messageOptionsType !== chatConstants.REMOVE_TEAM_MESSAGE && this.messageOptionsType !== chatConstants.ADD_TEAM_MESSAGE && !this.hideAvatar && !this.isCurrentUser;
    },
    displayActions() {
      return !this.miniChat && this.messageType !== chatConstants.DELETED_MESSAGE && this.messageActions && this.messageActions.length;
    },
    messageId() {
      return this.message.clientId ? this.message.clientId : this.message.msgId;
    },
    dateString() {
      return chatTime.getTimeString(this.message.timestamp);
    },
    isEditedMessage() {
      return this.messageType === chatConstants.EDITED_MESSAGE;
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
      return this.specificMessageObj;
    },
    specificMessageObj() {
      return this.messageOptionsType && extraMessageTypes && extraMessageTypes.find(elm => elm.type === this.messageOptionsType);
    },
    specificMessageContent() {
      if (this.specificMessageObj && this.specificMessageObj.html) {
        return this.specificMessageObj.html(this.message, this.$t.bind(this));
      }
      return '';
    },
    specificMessageClass() {
      return this.specificMessageObj ? this.specificMessageObj.iconClass : '';
    },
    statusStyle: function() {
      if (this.message.isEnabledUser === 'false' && !this.isCurrentUser) {
        return 'user-disabled';
      }
      return '';
    },
    disabledStatus() {
      if (this.message.isEnabledUser==='false') {
        return '('.concat('',this.$t('exoplatform.chat.inactive')).concat(')');
      }
      return '';
    },
    avatarUrl() {
      return getUserAvatar(this.message.user);
    }
  },
  created() {
    document.addEventListener(chatConstants.ACTION_MESSAGE_EDIT, this.editMessage);
    document.addEventListener(
      'exo-chat-message-action-delete-requested',
      this.deleteMessage
    );
    this.$transferRulesService?.getTransfertRulesDownloadDocumentStatus().then(enabled=> this.downloadEnabled = enabled);
  },
  destroyed() {
    document.removeEventListener(
      chatConstants.ACTION_MESSAGE_EDIT,
      this.editMessage
    );
    document.removeEventListener(
      'exo-chat-message-action-delete-requested',
      this.deleteMessage
    );
  },
  methods: {
    deleteThumbnail() {
      $(`#${this.messageId}-attachmentContainer`).remove();
    },
    executeAction(messageAction) {
      if (messageAction.confirm) {
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
        new CustomEvent(chatConstants.ACTION_MESSAGE_DELETE, {
          detail: { room: this.room, msgId: this.message.msgId }
        })
      );
    },
    unescapeHTML(html) {
      return unescape(html);
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
    },
    sendFailedMessage() {
      document.dispatchEvent(
        new CustomEvent(chatConstants.RESEND_FAILED_MESSAGE, {
          detail: {user: eXo.chat.userSettings.username, message: this.message}
        })
      );
    },
    deleteFailedMessage() {
      document.dispatchEvent(
        new CustomEvent(chatConstants.DELETE_FAILED_MESSAGE, {
          detail: {user: eXo.chat.userSettings.username, clientId: this.message.clientId}
        })
      );
    }
  }
};
</script>
