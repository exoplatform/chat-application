<template>
  <div
    v-sanitized-html="value"
    v-once
    contenteditable="true"
    @input="$emit(`input`, $event.target.innerHTML);">
  </div>
</template>

<script>
export default {
  props: {
    value: {
      type: String,
      default: null
    }
  },
};
</script>