<template>
  <div :class="statusStyle" class="chat-contact">
    <div :style="{backgroundImage: 'url(' + contactAvatar + ')'}" class="chat-contact-avatar">
      <i v-if="list && type=='u'" class="uiIconStatus"></i>
    </div>
    <div class="contactDetail">
      <div class="contactLabel">
        {{ name }}
        <slot></slot>
      </div>
      <div v-if="type =='u' && !list" class="user-status">
        <i class="uiIconStatus"></i>
        {{ status }}
      </div>
      <div v-if="type !='u' && !list && nbMembers" class="room-number-members">
        {{ nbMembers }} members
      </div>
    </div>
  </div>
</template>

<script>

export default {
  props: {
    name: {
      type: String,
      default: ''
    },
    avatar: {
      type: String,
      default: ''
    },
    status: {
      type: String,
      default: ''
    },
    list: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      required: true
    },
    nbMembers: {
      type: String,
      default: ''
    }
  },
  data : function() {
    return {};
  },
  computed: {
    statusStyle: function() {
      return (this.status == 'online') ? 'user-available' : 'user-invisible';
    },
    contactAvatar() {
      return (this.type == 't') ? '/chat/img/user-default.jpg' : this.avatar;
    }
  },
  methods: { 
  }
};
</script>