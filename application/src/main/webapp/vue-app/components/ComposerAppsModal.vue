<template>
  <modal :title="title" modal-class="room-notification-modal" @modal-closed="closeModal">
    <div v-if="appKey == 'raise-hand'">
      <input v-model="raiseHandComment" type="text" placeholder="Optionel Comment">
    </div>
    <div v-if="appKey == 'question'">
      <input v-model="questionText" type="text" placeholder="What is your question?">
    </div>
    <div v-if="appKey == 'link'">
      <input v-model="linkText" type="text" placeholder="E.g: http://www.exoplatform.com">
    </div>
    <div v-if="appKey == 'event'" class="chat-app-event">
      <input v-model="eventName" type="text" placeholder="Event title">
      <div class="chat-event-date form-horizontal">
        <div class="event-item">
          <span class="action-label">from</span>
          <input ref="eventDateFrom" type="text" format="MM/dd/yyyy" placeholder="mm/dd/yyyy" @focus="initDatePicker($event)">
          <select v-model="eventTimeFrom" class="selectbox" @change="setTimeTo($event)">
            <option v-for="hour in dayHourOptions" :key="hour.value" :value="hour.value">{{ hour.text }}</option>
          </select>
        </div>
        <div class="event-item">
          <span class="action-label">to</span>
          <input ref="eventDateTo" type="text" format="MM/dd/yyyy" placeholder="mm/dd/yyyy" @focus="initDatePicker($event)">
          <select v-model="eventTimeTo" class="selectbox">
            <option v-for="hour in dayHourOptions" :key="hour.value" :value="hour.value">{{ hour.text }}</option>
          </select>
        </div>
      </div>
      <input v-model="eventLocation" type="text" placeholder="Location">
      
    </div>

    <div class="uiAction uiActionBorder">
      <div class="btn btn-primary" @click="saveAppModal">Enregistrer</div>
      <div class="btn" @click="closeModal">Annuler</div>
    </div>
  </modal>
</template>

<script>
import Modal from './modal/Modal.vue';
import * as chatServices from '../chatServices';

const RAISE_HAND = 'type-hand';
const QUESTION_MESSAGE = 'type-question';
const LINK_MESSAGE = 'type-link';

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
      raiseHandComment: '',
      questionText: '',
      linkText: '',
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
  },
  methods: {
    closeModal() {
      // Emit the click event of close icon
      this.$emit('modal-closed');
    },
    saveAppModal() {
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
      let validUrl;
      
      switch(this.appKey) {
      case 'raise-hand':
        message.msg = this.raiseHandComment;
        message.options.type = RAISE_HAND;
        this.raiseHandComment = '';
        break;
      case 'question':
        if (this.questionText === '') {
          return;
        }
        message.msg = this.questionText;
        message.options.type = QUESTION_MESSAGE;
        break;
      case 'link':
        if (this.linkText === '') {return;}
        validUrl = this.checkURL(this.linkText);
        if (!validUrl) {
          return;   // TODO add error modal
        }
        message.options.link = validUrl;
        message.options.type = LINK_MESSAGE;
        break;
      case 'event':
        if (this.getEventFormValue()) {
          chatServices.saveEvent(eXo.chat.userSettings, this.getEventFormValue(), this.contact).then(()=> console.log('okkkk'));
        }
        
        break;        
      }

      if (this.appKey !== 'event') {
        document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : message}));
        this.closeModal();
      }
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
    setTimeTo() {
      const TEN = 10;
      const time = this.eventTimeFrom;
      const h = Math.round(time.split(':')[0]) + 1;
      let hh = h;
      if (h < TEN) {hh = `0${h}`;}
      this.eventTimeTo = `${hh}:${time.split(':')[1]}`;
    },
    getEventFormValue() {
      if (this.eventName === '' || this.eventLocation === '' || this.$refs.eventDateFrom.value === '' || this.$refs.eventDateTo.value === '') {
        return null;
      }
      const eventForm = {
        summary: this.eventName,
        startDate: this.$refs.eventDateFrom.value,
        startTime: this.eventTimeFrom,
        endDate: this.$refs.eventDateTo.value,
        endTime: this.eventTimeTo,
        location: this.eventLocation
      };
      if (this.eventTimeFrom === 'all-day') {eventForm.startTime = '12:00 AM';}
      if (this.eventTimeTo === 'all-day') {eventForm.endTime = '11:59 PM';}

      return eventForm;
    }
  }
};
</script>
