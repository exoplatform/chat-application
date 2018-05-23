<template>
  <div :id="message.clientId" :class="{'chat-message-not-sent': message.notSent, 'is-same-contact': hideAvatar, 'is-current-user': isCurrentUser}" class="chat-message-box">
    <div class="chat-sender-avatar">
      <div v-if="!hideAvatar && !isCurrentUser" :style="`backgroundImage: url(${contactAvatar}`" class="chat-contact-avatar"></div>
    </div>
    <div class="chat-message-bubble">
      <div v-if="!hideAvatar && !isCurrentUser" class="sender-name">{{ message.fullname }} :</div>

      <div v-if="message.isSystem && message.options.type === constants.ADD_TEAM_MESSAGE" class="message-content">
        <span v-html="$t('chat.team.msg.adduser', roomAddOrDeleteI18NParams)"></span>
      </div>
      <div v-if="message.isSystem && message.options.type === constants.REMOVE_TEAM_MESSAGE" class="message-content">
        <span v-html="$t('chat.team.msg.removeuser', roomAddOrDeleteI18NParams)"></span>
      </div>
      <div v-if="message.isSystem && message.options.type === constants.TASK_MESSAGE" class="message-content">
        <b>
          <a :href="message.options.url" target="_blank">{{ message.options.task }}</a>
        </b>
        <div class="custom-message-item">
          <span>
            <i class="uiIconChatAssign"></i>
            Affectée à:
          </span>
          <b>{{ message.options.username || 'Non affectée' }}</b>
        </div>
        <div class="custom-message-item"><span><i class="uiIconChatClock"></i>Echéance: </span><b>{{ message.options.dueDate || 'Non définie' }}</b></div>
      </div>
      <div v-if="message.isSystem && message.options.type === constants.EVENT_MESSAGE" class="message-content">
        <b>{{ message.options.summary }}</b>
        <div class="custom-message-item">
          <i class="uiIconChatClock"></i>
          <span>De </span>
          <b>{{ message.options.startDate }} {{ message.options.startTime }}</b>
          <span> à </span>
          <b>{{ message.options.endDate }} {{ message.options.endTime }}</b>
        </div>
        <div class="custom-message-item">
          <i class="uiIconChatCheckin"></i>
          {{ message.options.location }}
        </div>
      </div>
      <div v-if="message.isSystem && message.options.type === constants.FILE_MESSAGE" class="message-content">
        <b>
          <a :href="message.options.restPath" target="_blank">{{ message.options.name }}</a>
        </b>
        <span class="message-file-size">{{ message.options.sizeLabel }}</span>
        <div class="attachmentContainer">
          <div class="attachmentContentImage">
            <div class="imageAttachmentBox">
              <a class="imgAttach">
                <img :src="message.options.restPath" :alt="message.options.name">
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
      <div v-if="message.isSystem && message.options.type === constants.LINK_MESSAGE" class="message-content">
        <a :href="message.options.link" target="_blank">{{ message.options.link }}</a>
      </div>
      <div v-if="message.isSystem && message.options.type === constants.RAISE_HAND" class="message-content">
        <b>{{ messageContent }}</b>
      </div>
      <div v-if="message.isSystem && message.options.type === constants.START_MEETING_MESSAGE" class="message-content">
        <b>Réunion démarée</b>
        <div>
          <em class="muted">Arrêtez la réunion et enregistrez des notes à tout moment en cliquant sur le bouton Stop.</em>
        </div>
      </div>
      <div v-if="message.isSystem && message.options.type === constants.STOP_MEETING_MESSAGE" class="message-content">
        <b>Réunion terminée</b>
        <div class="custom-message-item"><i class="uiIconChatSendEmail"></i><div class="btn-link notes-send">Envoyer les notes</div></div>
        <div class="custom-message-item"><i class="uiIconChatWiki"></i><div class="btn-link notes-save">Enregistrer dans le Wiki</div></div>
      </div>
      <div v-if="message.isSystem && message.options.type === constants.NOTES_MESSAGE" class="message-content">
        <b>Les notes ont bien été enregistrées</b>
        <div class="custom-message-item">
          <i class="uiIconChatSendEmail"></i>
          <div class="btn-link meeting-send">Envoyer les notes</div>
        </div>
        <div class="custom-message-item">
          <i class="uiIconChatWiki"></i>
          <div class="btn-link meeting-save">Enregistrer dans le Wiki</div>
        </div>
      </div>
      <div v-if="message.type === constants.DELETED_MESSAGE" class="message-content">
        <em class="muted">Ce message a été supprimé.</em>
      </div>
      <div v-if="!message.isSystem && message.type !== constants.DELETED_MESSAGE" class="message-content">
        {{ messageContent }}
      </div>

      <div class="message-description">
        <i v-if="isEditedMessage" class="uiIconChatEdit"></i>
      </div>
    </div>
    <div class="chat-message-action">
      <dropdown-select v-if="!message.isDeleted && messageActions && messageActions.length" class="message-actions" position="right">
        <i slot="toggle" class="uiIconDots"></i>
        <li slot="menu">
          <a v-for="messageAction in messageActions" :key="message.msgId + messageAction.key" :id="message.msgId + messageAction.key" :class="messageAction.class" href="#" @click="executeAction(messageAction.key)">
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
      isCurrentUser: eXo.chat.userSettings.username === this.message.user,
      constants: {
        ADD_TEAM_MESSAGE : 'type-add-team-user',
        REMOVE_TEAM_MESSAGE : 'type-remove-team-user',
        TASK_MESSAGE : 'type-task',
        EVENT_MESSAGE : 'type-event',
        FILE_MESSAGE : 'type-file',
        LINK_MESSAGE : 'type-link',
        RAISE_HAND : 'type-hand',
        QUESTION_MESSAGE : 'type-question',
        DELETED_MESSAGE : 'DELETED',
        EDITED_MESSAGE : 'EDITED',
        START_MEETING_MESSAGE : 'type-meeting-start',
        STOP_MEETING_MESSAGE : 'type-meeting-stop',
        NOTES_MESSAGE : 'type-notes',
        DEFAULT_MESSAGE_ACTIONS : [{
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
          enabled: (comp) => {
            return !comp.message.isSystem && !comp.message.isDeleted;
          }
        } , {
          key: 'saveNotes',
          enabled: (comp) => {
            return !comp.message.isSystem && !comp.message.isDeleted;
          }
        }]
      }
    };
  },
  computed: {
    messageActions() {
      if(eXo && eXo.chat && eXo.chat.message && eXo.chat.message.extraActions) {
        return this.constants.DEFAULT_MESSAGE_ACTIONS.concat(eXo.chat.message.extraActions).filter(menu => !menu.enabled || menu.enabled(this));
      } else {
        return this.constants.DEFAULT_MESSAGE_ACTIONS.filter(menu => !menu.enabled || menu.enabled(this));
      }
    },
    dateString() {
      return chatTime.getTimeString(this.message.timestamp);
    },
    contactAvatar() {
      return getUserAvatar(this.message.user);
    },
    isEditedMessage() {
      return this.message.type && this.message.type === this.constants.EDITED_MESSAGE ? true : false;
    },
    roomAddOrDeleteI18NParams() {
      return {0: `<b>${this.message.options.fullname}</b>`, 1: `<b>${this.message.options.users}</b>` };
    },
    messageContent() {
      return this.message.message ? this.message.message : this.message.msg;
    }
  },
  created() {
    document.addEventListener('exo-chat-message-acton-edit', this.editMessage);
    document.addEventListener('exo-chat-message-acton-delete', this.deleteMessage);
    document.addEventListener('exo-chat-message-acton-saveNotes', this.saveNotes);
    if(this.message) {
      this.message.isDeleted = this.message.type === this.constants.DELETED_MESSAGE;
    }
  },
  updated() {
    const $chatMessageDetailContainer = $(`#${this.message.clientId}`);

    $chatMessageDetailContainer.find('.notes-send').off('click').on('click', function () {/*
      var $uimsg = $(this).closest('.msg-text');
      var msgId = $uimsg.attr('id');
      var messages = TAFFY(thiss.messages);
      var tempMsg = messages({
        msgId: msgId
      });
      if (tempMsg.count() > 0) {
        var msg = tempMsg.first().msg;

        var msgHtml = msg.replace(/<br\/>/g, '\n');
        msgHtml = $('<div />').html(msgHtml).text();

        var msgFullname = $uimsg.attr('data-fn');
        $('#msg').focus().val('').val('[quote=' + msgFullname + ']' + msgHtml + ' [/quote] ');
      }*/
    });

    $chatMessageDetailContainer.find('.notes-save').off('click').on('click', function () {
      const options = {
        type: 'type-notes',
        fromTimestamp: this.message.timestamp,
        fromUser: eXo.chat.userSettings.username,
        fromFullname: eXo.chat.userSettings.fullName
      };
      document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : {msg: '', options: options}}));
    });

    $chatMessageDetailContainer.find('.meeting-send').off('click').on('click', function () {/*
      var $this = $(this);
      var $meetingNotes = $this.closest('.msMeetingNotes');
      $meetingNotes.animate({
        opacity: 'toggle'
      }, 200, function () {
        var room = $this.attr('data-room');
        var from = $this.attr('data-from');
        var to = $this.attr('data-to');
        var id = $this.attr('data-id');

        from = Math.round(from) - 1;
        to = Math.round(to) + 1;

        $meetingNotes.find('.alert-success').hide();
        chatApplication.chatRoom.sendMeetingNotes(room, from, to, function (response) {
          if (response === 'sent') {
            $('#' + id).animate({
              opacity: 'toggle'
            }, 200, function () {
              $meetingNotes.animate({
                opacity: 'toggle'
              }, 1000);
              $('#' + id).show();
            });
          }
        });
      });*/
    });

    $chatMessageDetailContainer.find('.meeting-save').off('click').on('click', function () {/*
      var $this = $(this);
      var $meetingNotes = $this.closest('.msMeetingNotes');
      $meetingNotes.animate({
        opacity: 'toggle'
      }, 200, function () {
        var room = $this.attr('data-room');
        var from = $this.attr('data-from');
        var to = $this.attr('data-to');
        var id = $this.attr('data-id');

        from = Math.round(from) - 1;
        to = Math.round(to) + 1;

        $meetingNotes.find('.alert-success').hide();
        chatApplication.chatRoom.getMeetingNotes(room, from, to, function (response) {
          if (response !== 'ko') {
            $.ajax({
              type: 'POST',
              url: chatApplication.jzSaveWiki,
              data: {
                'targetFullname': chatApplication.targetFullname,
                'content': JSON.stringify(response)
              },
              context: this,
              dataType: 'json',
              success: function (data) {
                if (data.path !== '') {
                  var baseUrl = location.protocol + '//' + location.hostname;
                  if (location.port) {
                    baseUrl += ':' + location.port;
                  }
                  var options = {
                    type: 'type-link',
                    link: baseUrl + data.path,
                    from: chatApplication.username,
                    fullname: chatApplication.fullname
                  };
                  var msg = chatBundleData['exoplatform.chat.meeting.notes'];

                  chatApplication.chatRoom.sendMessage(msg, options, 'true');
                }

                $('#' + id).animate({
                  opacity: 'toggle'
                }, 200, function () {
                  $meetingNotes.animate({
                    opacity: 'toggle'
                  }, 1000);
                  $('#' + id).show();
                });
              }
            });
          }
        });
      });*/
    });
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
