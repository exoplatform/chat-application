<template>
  <exo-modal :title="title" modal-class="apps-composer-modal" @modal-closed="closeModal">
    <div v-show="errorCode" class="alert alert-error">{{ errorMessage() }}</div>

    <form id="appComposerForm" ref="appComposerForm" onsubmit="return false;" action="" method="post" enctype="multipart/form-data" accept-charset="utf-8">
      <div v-if="sendingMessage" class="chat-loading-mask">
        <img src="/chat/img/sync.gif" class="chat-loading">
      </div>

      <div :class="app.appClass" v-html="appHtml"></div>

      <div v-show="app.hideModalActions !== true" class="uiAction uiActionBorder">
        <button type="submit" class="btn btn-primary" @click="saveAppModal">{{ $t(saveLabelKey) }}</button>
        <div class="btn" @click="closeModal">{{ $t('exoplatform.chat.cancel') }}</div>
      </div>
    </form>
  </exo-modal>
</template>

<script>
import ExoModal from './ExoModal.vue';
import * as chatServices from '../../chatServices';

export default {
  components: {
    'exo-modal': ExoModal
  },
  props: {
    app: {
      type: Object,
      default: function() {
        return {};
      }
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
      errorCode: false,
      errorOpts: {},
      sendingMessage: false
    };
  },
  computed: {
    appHtml() {
      const i18nConverter = this.$t.bind(this);
      return this.app.html(i18nConverter);
    },
    saveLabelKey() {
      return this.app.saveLabelKey ? this.app.saveLabelKey : 'exoplatform.chat.post';
    }
  },
  mounted() {
    if(this.app.init) {
      this.app.init(this.contact);
    }
  },
  created() {
    document.addEventListener(this.$constants.ACTION_APPS_CLOSE, this.closeModal);

    if(this.app && this.app.htmlAdded) {
      this.$nextTick(() => this.app.htmlAdded($, chatServices));
    }
  },
  destroyed() {
    document.removeEventListener(this.$constants.ACTION_APPS_CLOSE, this.closeModal);
  },
  methods: {
    closeModal() {
      // Emit the click event of close icon
      this.$emit('modal-closed');
    },
    saveAppModal() {
      this.errorCode = '';
      if(!this.$refs.appComposerForm.checkValidity()) {
        return;
      }
      this.sendingMessage = true;
      const formData = $('#appComposerForm').serializeFormJSON();
      const message = {
        msg : '',
        room : this.roomId,
        clientId: new Date().getTime().toString(),
        user: eXo.chat.userSettings.username,
        isSystem: true,
        options: formData
      };
      message.options.fromUser = eXo.chat.userSettings.username;
      message.options.fromFullname = eXo.chat.userSettings.fullName;
      message.options.type = this.app.type;

      if (this.app.validate) {
        this.errorCode = this.app.validate(formData);
        if(this.errorCode) {
          this.sendingMessage = false;
          return;
        }
      }

      let submitResult;
      if(this.app.submit) {
        submitResult = this.app.submit(chatServices, message, formData, this.contact);
      } else {
        submitResult = {ok: true};
      }
      if(submitResult) {
        if(submitResult.then) {
          submitResult.then(result => {
            this.processResult(result, message);
          });
        } else {
          this.processResult(submitResult, message);
        }
      } else {
        this.errorCode = 'UknownError';
        this.sendingMessage = false;
      }
    },
    processResult(result, message) {
      if(result.errorCode) {
        this.errorCode = result.errorCode;
      } else if(result.ok) {
        document.dispatchEvent(new CustomEvent(this.$constants.ACTION_MESSAGE_SEND, {'detail' : message}));
        this.closeModal();
      } else if(result.hide) {
        this.closeModal();
      } else {
        this.errorCode = 'UknownError';
      }
      this.sendingMessage = false;
    },
    errorMessage() {
      if(this.errorCode) {
        return this.$t(`exoplatform.chat.${this.errorCode}`, this.errorOpts);
      }
    }
  }
};
</script>
