<template>
  <modal :title="title" modal-class="apps-composer-modal" @modal-closed="closeModal">
    <form id="appComposerForm" ref="appComposerForm" onsubmit="return false;">
      <div v-show="error" class="alert alert-error">Error sending request. Please contact administrator.</div>
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
      <div v-else-if="appKey == 'file'" id="dropzone-container" class="chat-file-upload">
        <div class="progressBar">
          <div class="progress">
            <div class="bar" style="width: 0.0%;"></div>
            <div class="label">
              <div class="label-inner">Déposez votre fichier ici</div>
            </div>
          </div>
        </div>
      </div>

      <div class="uiAction uiActionBorder">
        <button type="submit" class="btn btn-primary" @click="saveAppModal()">Enregistrer</button>
        <div class="btn" @click="closeModal">Annuler</div>
      </div>
    </form>
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
      error: false,
      sendingMessage: false,
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
          chatServices.saveEvent(eXo.chat.userSettings, this.getEventFormValue(), this.contact).then(()=> {
            document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : message}));
            this.closeModal();
            this.sendingMessage = false;
          }).catch(() => {
            this.error = true;
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
            this.error = true;
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
          this.error = true;
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
    getEventFormValue() {
      if (this.eventName === '' || this.$refs.eventDateFrom.value === '' || this.$refs.eventDateTo.value === '') {
        // TODO show error message per field (in generic way)
        return null;
      }
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

    }
  }
};
</script>
