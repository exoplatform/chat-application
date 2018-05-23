<template>
  <modal :title="title" modal-class="room-notification-modal" @modal-closed="closeModal">
    <div v-if="appKey == 'raise-hand'" class="raise-hand-app">
      <input v-model="raiseHandComment" type="text" placeholder="Optionel Comment">
    </div>
    <div v-if="appKey == 'question'">
      <input v-model="questionText" type="text" placeholder="What is your question?">
    </div>
    <div v-if="appKey == 'link'">
      <input v-model="linkText" type="text" placeholder="E.g: http://www.exoplatform.com">
    </div>
    <div v-if="appKey == 'event'">
      <input type="text" placeholder="Event title">
      <span class="action-label">from</span>
      <input id="event-add-start-date" type="text" format="MM/dd/yyyy" placeholder="mm/dd/yyyy" style="width:88px;">
      <select class="selectbox">
        <option value="all-day">All</option>
      </select>
      <input id="event-add-end-date" type="text" format="MM/dd/yyyy" placeholder="mm/dd/yyyy" lang="fr" name="from" style="width:88px;" @focus="initDatePicker($event)">
    </div>

    <div class="uiAction uiActionBorder">
      <div class="btn btn-primary" @click="saveAppModal">Enregistrer</div>
      <div class="btn" @click="closeModal">Annuler</div>
    </div>
  </modal>
</template>

<script>
import Modal from './Modal.vue';
//import * as chatServices from '../chatServices';

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
    room: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      raiseHandComment: '',
      questionText: '',
      linkText: ''
    };
  },
  computed: {
    disableAdvancedFilter() {
      return this.selectedOption !== 'keywords';
    }
  },
  methods: {
    closeModal() {
      // Emit the click event of close icon
      this.$emit('modal-closed');
    },
    saveAppModal() {
      console.log(this.room)
      const message = {
        msg : '',
        room : this.room,
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
        if (this.questionText === '') {return}
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
      }

      document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : message}));

      this.closeModal();
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
    }
  }
};
</script>
