<template>
  <modal :title="title" modal-class="apps-composer-modal" @modal-closed="closeModal">
    <div v-show="errorCode" class="alert alert-error">{{ errorMessage() }}</div>
    <form v-if="appKey !== 'file'" id="appComposerForm" ref="appComposerForm" onsubmit="return false;">
      <div v-if="sendingMessage" class="apps-composer-mask center">
        <img src="/chat/img/sync.gif" width="64px" class="chat-loading">
      </div>
      <div v-if="appKey == 'raise-hand'">
        <input v-model="raiseHandComment" class="large" type="text" placeholder="Optional Comment" required>
      </div>
      <div v-else-if="appKey == 'link'">
        <input v-model="linkText" class="large" type="text" placeholder="E.g: http://www.exoplatform.com" required>
      </div>
      <div v-else-if="appKey == 'question'">
        <input v-model="questionText" class="large" type="text" placeholder="What is your question?" required>
      </div>
      <div v-else-if="appKey == 'link'">
        <input v-model="linkText" class="large" type="text" placeholder="E.g: http://www.exoplatform.com" required>
      </div>
      <div v-else-if="appKey == 'event'" class="chat-app-event">
        <input v-model="eventName" class="large" type="text" placeholder="Event title" required>
        <div class="chat-event-date form-horizontal">
          <div class="event-item">
            <span class="action-label">from</span>
            <input ref="eventDateFrom" :format="dateFormat" :placeholder="dateFormatTitle" type="text" pattern="\d{2}/\d{2}/\d{4}" required @focus="initDatePicker($event)">
            <select v-model="eventTimeFrom" class="selectbox" required @change="setTimeTo($event)">
              <option v-for="hour in dayHourOptions" :key="hour.value" :value="hour.value">{{ hour.text }}</option>
            </select>
          </div>
          <div class="event-item">
            <span class="action-label">to</span>
            <input ref="eventDateTo" :format="dateFormat" :placeholder="dateFormatTitle" type="text" pattern="\d{2}/\d{2}/\d{4}" required @focus="initDatePicker($event)">
            <select v-model="eventTimeTo" class="selectbox" required>
              <option v-for="hour in dayHourOptions" :key="hour.value" :value="hour.value">{{ hour.text }}</option>
            </select>
          </div>
        </div>
        <input v-model="eventLocation" class="large" type="text" placeholder="Location">
      </div>
      <div v-else-if="appKey == 'task'" class="task-form">
        <input ref="taskTitle" class="large" type="text" placeholder="Task title" required>
        <input ref="taskAssignee" class="large" type="text" placeholder="Assignee">
        <input ref="taskDueDate" :format="dateFormat" placeholder="Due Date" class="large" type="text" pattern="\d{2}/\d{2}/\d{4}" readonly @focus="initDatePicker($event)">
      </div>
      <div class="uiAction uiActionBorder">
        <button type="submit" class="btn btn-primary" @click="saveAppModal()">Enregistrer</button>
        <div class="btn" @click="closeModal">Annuler</div>
      </div>
    </form>
    <div v-else id="dropzone-container" class="chat-file-upload">
      <div class="progressBar">
        <div class="progress">
          <div class="bar" style="width: 0.0%;"></div>
          <div class="label">
            <div class="label-inner">Déposez votre fichier ici</div>
          </div>
        </div>
      </div>
      <form id="chat-file-form" action="" method="post" enctype="multipart/form-data" accept-charset="utf-8">
        <input id="chat-file-room" type="hidden" name="room" value="---" />
        <input id="chat-file-target-user" type="hidden" name="targetUser" value="---" />
        <input id="chat-file-target-fullname" type="hidden" name="targetFullname" value="---" />
        <input id="chat-encoded-file-name" type="hidden" name="encodedFileName" value="---" />
        <div v-show="showButtons" class="uiActionBorder">
          <a href="#" class="btn btn-primary chat-file-upload" type="button">
            <span>Select file</span>
            <input id="chat-file-file" type="file" name="userfile" />
          </a>
          <a href="#" type="button" class="btn btnClosePopup" @click="closeModal">Cancel</a>
          <input id="chat-file-submit" type="submit" value="Select file" style="display:none" />
        </div>
      </form>
    </div>
  </modal>
</template>

<script>
import Modal from './Modal.vue';
import * as chatServices from '../../chatServices';

const RAISE_HAND = 'type-hand';
const QUESTION_MESSAGE = 'type-question';
const LINK_MESSAGE = 'type-link';
const EVENT_MESSAGE = 'type-event';
const TASK_MESSAGE = 'type-task';

const MAX_FILES = 1;
const UPLOAD_URI = '/portal/upload';

export default {
  components: {
    modal: Modal
  },
  props: {
    appKey: {
      type: String,
      default: ''
    },
    title: {
      type: String,
      default: ''
    },
    roomId: {
      type: String,
      default: ''
    },
    contact: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data() {
    return {
      dateFormat: 'MM/dd/yyyy',
      dateFormatTitle: 'mm/dd/yyyy',
      raiseHandComment: '',
      questionText: '',
      linkText: '',
      errorCode: false,
      sendingMessage: false,
      showButtons: true,
      dayHourOptions: [
        { 
          text: 'All Day', 
          value: 'all-day'
        }
      ],
      eventDateFrom: '',
      eventDateTo: '',
      eventTimeFrom: '',
      eventTimeTo: '',
      eventName: '',
      eventLocation: ''
    };
  },
  computed: {
    disableAdvancedFilter() {
      return this.selectedOption !== 'keywords';
    }
  },
  mounted() {
    this.populateSelectHour();
    this.eventTimeFrom = this.dayHourOptions[0].value;
    this.eventTimeTo = this.dayHourOptions[0].value;
    this.initSuggester();
    this.initUpload();
  },
  methods: {
    closeModal() {
      // Emit the click event of close icon
      this.$emit('modal-closed');
    },
    saveAppModal() {
      if(!this.$refs.appComposerForm.checkValidity()) {
        return;
      }
      const message = {
        msg : '',
        room : this.roomId,
        clientId: new Date().getTime().toString(),
        user: eXo.chat.userSettings.username,
        isSystem: true,
        options: {
          fromUser: eXo.chat.userSettings.username,
          fromFullname: eXo.chat.userSettings.fullName
        }
      };

      this.sendingMessage = true;

      switch(this.appKey) {
      case 'raise-hand':
        message.msg = this.raiseHandComment;
        message.options.type = RAISE_HAND;
        this.raiseHandComment = '';
        document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : message}));
        this.closeModal();
        break;
      case 'question':
        if (this.questionText === '') {
          return false;
        }
        message.msg = this.questionText;
        message.options.type = QUESTION_MESSAGE;
        document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : message}));
        this.closeModal();
        this.sendingMessage = false;
        break;
      case 'link': {
        const validUrl = this.checkURL(this.linkText);
        if (!validUrl) {
          return false;
        }
        message.options.link = validUrl;
        message.options.type = LINK_MESSAGE;
        document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : message}));
        this.closeModal();
        this.sendingMessage = false;
        break;
      }
      case 'event':
        if (this.getEventFormValue()) {
          message.options = this.getEventFormValue();
          message.options.type = EVENT_MESSAGE;
          chatServices.saveEvent(eXo.chat.userSettings, this.getEventFormValue(), this.contact).then((response)=> {
            if(!response.ok) {
              this.errorCode = 'ErrorNetwork';
              this.sendingMessage = false;
              return;
            }
            document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : message}));
            this.closeModal();
            this.sendingMessage = false;
          }).catch(() => {
            this.errorCode = 'ErrorNetwork';
            this.sendingMessage = false;
          });
        }
        break;
      case 'task': {
        message.options.type = TASK_MESSAGE;
        const isSpace = this.contact.user.indexOf('space-') === 0;
        const isTeam = this.contact.user.indexOf('team-') === 0;
        const data = {
          'extension_action' : 'createTask',
          'username' : $(this.$refs.taskAssignee).suggester('getValue'),
          'dueDate' : this.$refs.taskDueDate.value,
          'text' : this.$refs.taskTitle.value,
          'roomName' : this.contact.fullName,
          'isSpace' : isSpace,
          'isTeam': isTeam,
          'participants': isSpace || isTeam ? this.contact.participants.join(',') : this.contact.user
        };
        chatServices.saveTask(eXo.chat.userSettings, data).then((response) => {
          if (!response.ok) {
            this.errorCode = 'ErrorSaveTask';
            this.sendingMessage = false;
            return;
          }
          return response.json();
        }).then(data => {
          const url = data.url ? data.url : data.length && data.length === 1 && data[0].url ? data[0].url : '';
          message.options.url = url;
          message.options.username = $(this.$refs.taskAssignee).suggester('getValue');
          message.options.dueDate = this.$refs.taskDueDate.value;
          message.options.task = this.$refs.taskTitle.value;
          message.options.type = TASK_MESSAGE;

          document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : message}));
          this.closeModal();
          this.sendingMessage = false;
        }).catch(() => {
          this.errorCode = 'NetworkError';
          this.sendingMessage = false;
        });
      }break;
      }
      return false;
    },
    checkURL(text) {
      // if user has not entered http:// https:// or ftp:// assume they mean http://
      if (!/^(https?|ftp):\/\//i.test(text)) {
        text = `http://${text}`; // set both the value
      }
      return text;
      // TODO add regex to validate url
    },
    initDatePicker(e) {
      CalDateTimePicker.init(e.target, false);
    },
    populateSelectHour() {
      const HOURS = 24;
      const MINUTES = 60;
      const HALF_DAY = 12;
      const HALF_HOUR = 30;
      const TEN = 10;
      for (let h = 0; h < HOURS; h++) {
        for (let m = 0; m < MINUTES; m += HALF_HOUR) {
          let hh = h;
          let mm = m;
          const h12 = h % HALF_DAY || HALF_DAY;
          let hh12 = h12;
          const ampm = h < HALF_DAY ? 'AM' : 'PM';
          if (h < TEN) {hh = `0${hh}`;}
          if (m < TEN) {mm = `0${mm}`;}
          if (h12 < TEN) {hh12 = `0${hh12}`;}
          const time12 = `${hh12}:${mm} ${ampm}`;
          const optionValue = {
            text: time12, 
            value: time12
          };
          this.dayHourOptions.push(optionValue);
        }
      }
    },
    initSuggester() {
      const $taskAssigneeSuggestor = $(this.$refs.taskAssignee);
      if (!$taskAssigneeSuggestor.length) {
        return;
      }
      if(!this.$refs.taskAssignee.selectize) {
        //init suggester
        $taskAssigneeSuggestor.suggester({
          type : 'tag',
          plugins: ['remove_button'],
          valueField: 'name',
          labelField: 'fullname',
          searchField: ['fullname'],
          sourceProviders: ['exo:task-add-user'],
          providers: {
            'exo:task-add-user': function(query, callback) {
              if (!query || !query.trim().length) {
                return callback();
              }
              chatServices.getChatUsers(eXo.chat.userSettings, query.trim()).then(data => {
                if(data && data.users) {
                  callback(data.users.filter(user => user.name !== eXo.chat.userSettings.username));
                }
              });
            }
          },
          renderMenuItem (item, escape) {
            const avatar = chatServices.getUserAvatar(item.name);
            const defaultAvatar = '/chat/img/room-default.jpg';
            return `
              <div class="avatarMini">
                <img src="${avatar}" onerror="this.src='${defaultAvatar}'">
              </div>
              <div class="user-name">${escape(item.fullname)} (${item.name})</div>
              <div class="user-status"><i class="chat-status-${item.status}"></i></div>
            `;
          }
        });
      } else {
        //clear suggester
        $taskAssigneeSuggestor.suggester('setValue', '');
        $taskAssigneeSuggestor[0].selectize.clear(true);
        $taskAssigneeSuggestor[0].selectize.renderCache['item'] = {};
      }

      if(this.participants) {
        this.participants.forEach(participant => {
          if(participant.name !== eXo.chat.userSettings.username) {
            $taskAssigneeSuggestor[0].selectize.addOption(participant);
            $taskAssigneeSuggestor[0].selectize.addItem(participant.name);
          }
        });
      }
    },
    setTimeTo() {
      const TEN = 10;
      const time = this.eventTimeFrom;
      const h = Math.round(time.split(':')[0]) + 1;
      let hh = h;
      if (h < TEN) {hh = `0${h}`;}
      this.eventTimeTo = `${hh}:${time.split(':')[1]}`;
    },
    errorMessage() {
      if(this.errorCode) {
        return `Error with code ${this.errorCode}`;
      }
    },
    getEventFormValue() {
      const eventForm = {
        summary: this.eventName,
        startDate: this.$refs.eventDateFrom.value,
        startTime: this.eventTimeFrom,
        endDate: this.$refs.eventDateTo.value,
        endTime: this.eventTimeTo,
        location: this.eventLocation ? this.eventLocation : ''
      };

      if (this.eventTimeFrom === 'all-day') {
        eventForm.startTime = '12:00 AM';
        eventForm.startAllDay = true;
      }

      if (this.eventTimeTo === 'all-day') {
        eventForm.endTime = '11:59 PM';
        eventForm.endAllDay = true;
      }

      return eventForm;
    },
    initUpload() {
      const MAX_RANDOM_NUMBER = 100000;
      const uploadId = Math.round(Math.random() * MAX_RANDOM_NUMBER);
      const $dropzoneContainer = $('#dropzone-container');
      const thiss = this;

      $('#dropzone-container').filedrop({
        fallback_id: 'chat-file-file',  // an identifier of a standard file input element
        url: `${UPLOAD_URI}?uploadId=${uploadId}&action=upload`,  // upload handler, handles each file separately, can also be a function taking the file and returning a url
        paramname: 'userfile',          // POST parameter name used on serverside to reference file
        error: function (err) {
          switch (err) {
          case 'ErrorBrowserNotSupported':
          case 'BrowserNotSupported':
            thiss.errorCode = 'BrowserNotSupported';
            break;
          case 'ErrorTooManyFiles':
          case 'TooManyFiles':
            thiss.errorCode = 'TooManyFiles';
            break;
          case 'ErrorFileTooLarge':
          case 'FileTooLarge':
            thiss.errorCode = 'FileTooLarge';
            break;
          case 'ErrorFileTypeNotAllowed':
          case 'FileTypeNotAllowed':
            thiss.errorCode = 'FileTypeNotAllowed';
            break;
          }
          thiss.showButtons = true;
        },
        allowedfiletypes: [],   // filetypes allowed by Content-Type.  Empty array means no restrictions
        maxfiles: MAX_FILES,
        maxfilesize: eXo.chat.userSettings.maxUploadSize,    // max file size in MBs
        uploadStarted: function() {
          this.errorCode = false;
          thiss.showButtons = false;
        },
        progressUpdated: function (i, file, progress) {
          $dropzoneContainer.find('.bar').width(`${progress}%`);
          $dropzoneContainer.find('.bar').html(`${progress}%`);
        },
        uploadFinished: function () {
          fetch(UPLOAD_URI, {
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            },
            method: 'post',
            credentials: 'include',
            body: $.param({
              uploadId: uploadId,
              action: 'progress'
            })
          }).then(resp =>  resp.text()).then(data => {
            data = data.replace(' upload :', ' "upload" :');
            data = JSON.parse(data);
            const UPLOAD_PERCENT_COMPLETE = 100;
            data = data && data.upload && data.upload[uploadId] ? data.upload[uploadId] : null;
            if (!data || !data.percent || data.percent !== UPLOAD_PERCENT_COMPLETE && data.percent !== '100') {
              this.errorCode = 'ErrorFileUploadNotComplete';
              return;
            }
            fetch('/portal/rest/chat/api/1.0/file/persist',{
              headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
              },
              method: 'post',
              credentials: 'include',
              body: $.param({
                uploadId: uploadId,
                dbName: eXo.chat.userSettings.dbName,
                token: eXo.chat.userSettings.token,
                targetRoom: thiss.contact.user,
                targetFullname: thiss.contact.fullName
              })
            }).then(resp =>  {
              if(!resp.ok) {
                thiss.errorCode = 'ErrorPersistFile';
                return;
              }
              return resp.json();
            }).then(options => {
              if(!options) {
                return;
              }
              options.type = 'type-file';
              const message = {
                msg: options.name,
                isSystem: true,
                room : thiss.contact.room,
                clientId: new Date().getTime().toString(),
                user: eXo.chat.userSettings.username,
                options : options
              };
              document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : message}));
              thiss.closeModal();
            });
          });
        }
      });
    }
  }
};
</script>
