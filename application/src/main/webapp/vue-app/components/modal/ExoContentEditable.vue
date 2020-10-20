<template>
  <div v-once :value="value" contenteditable="true" @input="$emit(`input`, $event.target.innerHTML)" v-html="value"></div>
</template>

<script>
export default {
  props: {
    value  : {
      type: Object,
      default: null
    }
  },
  watch: {
    value: function (newValue) {
      if (document.activeElement === this.$el) {
        return;
      }
      this.$el.innerHTML = newValue;
    }
  },
};
</script>