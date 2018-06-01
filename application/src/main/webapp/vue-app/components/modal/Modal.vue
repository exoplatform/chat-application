<template>
  <div class="modal-mask uiPopupWrapper">
    <div :class="modalClass" class="uiPopup chat-modal">
      <div class="popupHeader">
        <span class="PopupTitle popupTitle">{{ title }}</span>
        <a v-show="displayClose === 'true'" class="uiIconClose pull-right" @click="closeModal"></a> 
      </div>
      <div class="PopupContent popupContent">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    displayClose: {
      type: String,
      default: 'true'
    },
    title: {
      type: String,
      default: ''
    },
    modalClass: {
      type: String,
      default: ''
    }
  },
  methods: {
    closeModal() {
      // Emit the click event of close icon
      this.$emit('modal-closed');
    }
  }
};
</script>
