<template>
  <div class="chat-message" :class="{'is-apps-closed': appsClosed}">
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
        <textarea type="text" name="text" id="msg" autocomplete="off" v-model="newMessage"></textarea>
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
  props: [],
  data() {
    return {
      newMessage: '',
      appsClosed: true
    }
  },
  computed: {
    
  },
  methods: {
    closeApps(e) {
      if (e.keyCode === 27) {
        this.appsClosed = true;
      }
    }
  },
  created() {
    document.addEventListener('keyup', this.closeApps);
  },
  destroyed() {
    document.removeEventListener('keyup', this.closeApps);
  }
}
</script>