<template>
  <div class="dropdown">
    <span :class="togglerClass" class="dropdown-toggle" data-toggle="dropdown">
      <slot name="toggle"></slot>
    </span>
    <ul :class="positionClass" class="dropdown-menu">
      <slot name="menu"></slot>
    </ul>
  </div>
</template>

<script>
export default {
  props: {
    position: {
      type: String,
      default: 'left'
    },
    togglerClass: {
      type: String,
      default: ''
    }
  },
  computed: {
    positionClass() {
      return this.position === 'right' ? 'pull-right' : '';
    }
  }
};
</script>
