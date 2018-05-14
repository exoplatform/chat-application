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
import { getUserAvatar, getSpaceAvatar } from '../chatServices';
export default {
  props: {
    name: {
      type: String,
      default: ''
    },
    userName: {
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
      if (this.type == 'u') {
        return getUserAvatar(this.userName);
      } else if (this.type == 's') {
        let space_id = this.name.replace(' ', '_').toLowerCase(); // TODO verify the possibility of add space id to whoIsOnline 
        return getSpaceAvatar(space_id);
      } else {
        return '/chat/img/user-default.jpg'; // TODO add room default avatar
      }
    }
  },
  methods: { 
  }
};
</script>