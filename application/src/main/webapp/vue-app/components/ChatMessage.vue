<template>
  <div :class="{'is-apps-closed': appsClosed}" class="chat-message">
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
        <textarea id="msg" v-model="newMessage" type="text" name="text" autocomplete="off" @keyup.enter="sendMessage"></textarea>
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
  components: {
    
  },
  props: {
    room: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      newMessage: '',
      appsClosed: true
    };
  },
  computed: {
    
  },
  created() {
    document.addEventListener('keyup', this.closeApps);
  },
  destroyed() {
    document.removeEventListener('keyup', this.closeApps);
  },
  methods: {
    closeApps(e) {
      if (e.keyCode === 27) {
        this.appsClosed = true;
      }
    },
    sendMessage() {
      document.dispatchEvent(new CustomEvent('exo-chat-message-tosend', {'detail' : {'message' : this.newMessage, 'room' : this.room.room}}));
    }
  }
};
</script>
