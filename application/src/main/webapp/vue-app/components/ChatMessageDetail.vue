<template>
  <div :class="{'chat-message-not-sent': message.notSent, 'is-same-contact': hideAvatar, 'is-current-user': isCurrentUser}" class="chat-message-box">
    <div class="chat-sender-avatar">
      <div v-if="!hideAvatar && !isCurrentUser" :style="`backgroundImage: url(${contactAvatar}`" class="chat-contact-avatar"></div>
    </div>
    <div class="chat-message-bubble">
      <div v-if="!hideAvatar && !isCurrentUser" class="sender-name">{{ message.fullname }} :</div>
      <div class="message-content" v-html="getMessage"></div>
      <div class="message-description">
        <i v-if="isEditedMessage" class="uiIconChatEdit"></i>
      </div>
    </div>
    <div class="chat-message-action">
      <dropdown-select v-if="!message.isSystem && !message.isDeleted" class="message-actions" position="right">
        <i slot="toggle" class="uiIconDots"></i>
        <li slot="menu">
          <a v-for="messageAction in messageActions" v-if="displayItem(messageAction)" :key="message.msgId + messageAction.key" :id="message.msgId + messageAction.key" href="#" @click="executeAction(messageAction.key)">
            {{ $t(`chat.message.action.${messageAction.key}`) }}
          </a>
        </li>
      </dropdown-select>
      <div v-if="!hideTime" class="message-time">{{ dateString }}</div>
    </div>
  </div>
</template>

<script>
import * as chatTime from '../chatTime';
import { getUserAvatar } from '../chatServices';
import DropdownSelect from './DropdownSelect.vue';

const ADD_TEAM_MESSAGE = 'type-add-team-user';
const REMOVE_TEAM_MESSAGE = 'type-remove-team-user';
const TASK_MESSAGE = 'type-task';
const EVENT_MESSAGE = 'type-event';
const FILE_MESSAGE = 'type-file';
const LINK_MESSAGE = 'type-link';
const RAISE_HAND = 'type-hand';
const QUESTION_MESSAGE = 'type-question';
const DELETED_MESSAGE = 'DELETED';
const EDITED_MESSAGE = 'EDITED';
const START_MEETING_MESSAGE = 'type-meeting-start';
const STOP_MEETING_MESSAGE = 'type-meeting-stop';
const NOTES_MESSAGE = 'type-notes';

const DEFAULT_MESSAGE_ACTIONS = [{
  key: 'edit',
  enabled: (comp) => {
    return !comp.message.isSystem && !comp.message.isDeleted && comp.isCurrentUser;
  }
}, {
  key: 'delete',
  enabled: (comp) => {
    return !comp.message.isSystem && !comp.message.isDeleted && comp.isCurrentUser;
  }
} , {
  key: 'quote',
  class: 'uiIconPLFNotifications',
  enabled: (comp) => {
    return !comp.message.isSystem && !comp.message.isDeleted;
  }
} , {
  key: 'saveNotes',
  enabled: (comp) => {
    return !comp.message.isSystem && !comp.message.isDeleted;
  }
}];

export default {
  components: {DropdownSelect},
  props: {
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
    room : {
      type: String,
      default: ''
    },
    hideAvatar : {
      type: Boolean,
      default: false
    },
    hideTime : {
      type: Boolean,
      default: false
    }
  },
  data : function() {
    return {
      isCurrentUser: eXo.chat.userSettings.username === this.message.user
    };
  },
  computed: {
    messageActions() {
      if(eXo && eXo.chat && eXo.chat.message && eXo.chat.message.extraActions) {
        return DEFAULT_MESSAGE_ACTIONS.concat(eXo.chat.message.extraActions);
      } else {
        return DEFAULT_MESSAGE_ACTIONS;
      }
    },
    dateString() {
      return chatTime.getTimeString(this.message.timestamp);
    },
    contactAvatar() {
      return getUserAvatar(this.message.user);
    },
    isEditedMessage() {
      return this.message.type && this.message.type === EDITED_MESSAGE ? true : false;
    },
    getMessage() {
      const message = this.message.msg ? this.message.msg : this.message.message;
      const options = this.message.options;
      if (this.message.isSystem) {
        switch (options.type) {
        case ADD_TEAM_MESSAGE:
          return this.$t('chat.team.msg.adduser', {0: `<b>${options.fullname}</b>`, 1: `<b>${options.users}</b>`});
        case REMOVE_TEAM_MESSAGE:
          return this.$t('chat.team.msg.removeuser', {0: `<b>${options.fullname}</b>`, 1: `<b>${options.users}</b>`});
        case TASK_MESSAGE:
          return `
            <b><a href="${options.url}" target="_blank">${options.task}</a></b>
            <div class="custom-message-item"><span><i class="uiIconChatAssign"></i>Affectée à: </span><b>${options.username || 'Non affectée'}</b></div>
            <div class="custom-message-item"><span><i class="uiIconChatClock"></i>Echéance: </span><b>${options.dueDate || 'Non définie'}</b></div>
          `;
        case EVENT_MESSAGE:
          return `
            <b>${options.summary}</b>
            <div class="custom-message-item"><i class="uiIconChatClock"></i><span>De </span><b>${options.startDate} ${options.startTime}</b><span> à </span><b>${options.endDate} ${options.endTime}</b></div>
            <div class="custom-message-item"><i class="uiIconChatCheckin"></i>${options.location}</div>
          `;
        case FILE_MESSAGE:
          return `
            <b><a href="${options.restPath}" target="_blank">${options.name}</a></b><span class="message-file-size">(${options.sizeLabel})</span>
            <div class="attachmentContainer">
              <div class="attachmentContentImage">
                <div class="imageAttachmentBox">
                  <a class="imgAttach">
                    <img src="${options.restPath}" alt="${options.name}">
                  </a>
                  <div class="actionAttachImg">
                    <p><a href="${options.restPath}" target="_blank"><i class="uiIconSearch uiIconWhite"></i></a></p>
                    <p><a href="${options.downloadLink}" target="_blank"><i class="uiIconDownload uiIconWhite"></i></a></p>
                  </div>
                </div>
              </div>
            </div>
          `;
        case LINK_MESSAGE:
          return `<a href="${options.link}" target="_blank">${options.link}</a>`;
        case RAISE_HAND: case QUESTION_MESSAGE:
          return `<b>${message}</b>`;
        case START_MEETING_MESSAGE:
          return `
            <b>Réunion démarée</b>
            <div><em class="muted">Arrêtez la réunion et enregistrez des notes à tout moment en cliquant sur le bouton Stop.</em><div>
          `;
        case STOP_MEETING_MESSAGE:
          return `
            <b>Réunion terminée</b>
            <div class="custom-message-item"><i class="uiIconChatSendEmail"></i><div class="btn-link send-meeting-notes">Envoyer les notes</div></div>
            <div class="custom-message-item"><i class="uiIconChatWiki"></i><div class="btn-link send-meeting-notes">Enregistrer dans le Wiki</div></div>
          `;
        case NOTES_MESSAGE:
          return `
            <b>Les notes ont bien été enregistrées</b>
            <div class="custom-message-item"><i class="uiIconChatSendEmail"></i><div class="btn-link send-meeting-notes">Envoyer les notes</div></div>
            <div class="custom-message-item"><i class="uiIconChatWiki"></i><div class="btn-link send-meeting-notes">Enregistrer dans le Wiki</div></div>
          `;
        default:
          return '';
        }
      } else if(this.message.type && message === DELETED_MESSAGE) {
        return '<em class="muted">Ce message a été supprimé.</em>';
      } else {
        return message;
      }
    }
  },
  created() {
    document.addEventListener('exo-chat-message-acton-edit', this.editMessage);
    document.addEventListener('exo-chat-message-acton-delete', this.deleteMessage);
    document.addEventListener('exo-chat-message-acton-saveNotes', this.saveNotes);
    if(this.message) {
      this.message.isDeleted = this.message.type === DELETED_MESSAGE;
    }
  },
  destroyed() {
    document.removeEventListener('exo-chat-message-acton-edit', this.editMessage);
    document.removeEventListener('exo-chat-message-acton-delete', this.deleteMessage);
    document.removeEventListener('exo-chat-message-acton-saveNotes', this.saveNotes);
  },
  methods: {
    executeAction(actionName) {
      document.dispatchEvent(new CustomEvent(`exo-chat-message-acton-${actionName}`, {'detail': this.message}));
    },
    displayItem(settingAction) {
      return (!settingAction.isForAdmin || this.isAdmin) && (!settingAction.enabled || settingAction.enabled(this)) && (!settingAction.type || settingAction.type === this.contact.type);
    },
    editMessage(e) {
      if(!e || !e.detail || !e.detail.msgId || e.detail.msgId !== this.message.msgId) {
        return;
      }
      this.$emit('edit-message', this.message);
    },
    deleteMessage(e) {
      if(!e || !e.detail || !e.detail.msgId || e.detail.msgId !== this.message.msgId) {
        return;
      }
      document.dispatchEvent(new CustomEvent('exo-chat-message-todelete', {'detail' : {'room' : this.room, 'msgId' : this.message.msgId}}));
    },
    saveNotes(e) {
      if(!e || !e.detail || !e.detail.msgId || e.detail.msgId !== this.message.msgId) {
        return;
      }
      const messageToSend = {
        message : this.newMessage,
        room : this.room,
        clientId: new Date().getTime().toString(),
        timestamp: Date.now(),
        user: eXo.chat.userSettings.username,
        isSystem: true,
        options: {
          type: 'type-notes',
          fromTimestamp: this.message.options.timestamp,
          fromUser: eXo.chat.userSettings.username,
          fromFullname: eXo.chat.userSettings.fullName
        }
      };
      document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : messageToSend}));
    }
  }
};
</script>
