<template>
  <div v-if="contact && Object.keys(contact).length !== 0" :class="{'is-apps-closed': appsClosed}" class="chat-message">
    <div class="apps-container">
      <div class="apps-item">
        <div class="apps-item-icon"><i class="uiIconChatCreateEvent"></i></div>
        <div class="apps-item-label">Add Event</div>
      </div>
      <div class="apps-item">
        <div class="apps-item-icon"><i class="uiIconChatCreateTask"></i></div>
        <div class="apps-item-label">Assign Task</div>
      </div>
      <div class="apps-item">
        <div class="apps-item-icon"><i class="uiIconChatLink"></i></div>
        <div class="apps-item-label">Share Link</div>
      </div>
      <div class="apps-item">
        <div class="apps-item-icon"><i class="uiIconChatUpload"></i></div>
        <div class="apps-item-label">Upload File</div>
      </div>
      <div class="apps-item">
        <div class="apps-item-icon"><i class="uiIconChatQuestion"></i></div>
        <div class="apps-item-label">Ask Question</div>
      </div>
      <div class="apps-item">
        <div class="apps-item-icon"><i class="uiIconChatRaiseHand"></i></div>
        <div class="apps-item-label">Raise Hand</div>
      </div>
    </div>
    <div class="composer-container">
      <div class="composer-box">
        <div class="composer-action">
          <div class="action-emoji">
            <i class="uiIconChatSmile"></i>
          </div>
          <div class="action-apps" @click="appsClosed = !appsClosed">+</div>
        </div>
        <textarea id="msg" v-model="newMessage" type="text" name="text" autocomplete="off" @keydown.enter="preventDefault" @keypress.enter="preventDefault" @keyup.enter="sendMessage"></textarea>
        <div class="composer-action">
          <div class="action-send">
            <i class="uiIconSend"></i>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    contact: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data() {
    return {
      newMessage: '',
      appsClosed: true
    };
  },
  created() {
    document.addEventListener('keyup', this.closeApps);
  },
  destroyed() {
    document.removeEventListener('keyup', this.closeApps);
  },
  methods: {
    closeApps(e) {
      const ESC_KEY = 27;
      if (e.keyCode === ESC_KEY) {
        this.appsClosed = true;
      }
    },
    preventDefault(event) {
      const enterKeyCode = 13;
      if (event.keyCode === enterKeyCode && !event.shiftKey && !event.ctrlKey && !event.altKey) {
        event.stopPropagation();
        event.preventDefault();
      }
    },
    sendMessage(event) {
      const enterKeyCode = 13;
      if (event.keyCode === enterKeyCode && !event.shiftKey && !event.ctrlKey && !event.altKey) {
        document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : {'message' : this.newMessage, 'room' : this.contact.room}}));
        document.dispatchEvent(new CustomEvent('exo-chat-messages-scrollToEnd'));
        this.newMessage = '';
      }
    }
  }
};
</script>
