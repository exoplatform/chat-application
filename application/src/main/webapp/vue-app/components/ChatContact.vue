<template>
  <div class="chat-contact" :class="statusStyle">
  	<div class="chat-contact-avatar" :style="{backgroundImage: 'url(' + contactAvatar + ')'}">
  		<i v-if="list && type=='u'" class="uiIconStatus"></i>
  	</div>
  	<div class="contactDetail">
  		<div class="contactLabel">
        {{name}}
        <slot></slot>
      </div>
  		<div v-if="type =='u' && !list" class="user-status">
  			<i class="uiIconStatus"></i>
  			{{status}}
  		</div>
      <div v-if="type !='u' && !list && nbMembers" class="room-number-members">
  			{{nbMembers}} members
  		</div>
  	</div>
  </div>
</template>

<script>

export default {
  props: ['name', 'avatar', 'status', 'list', 'type', 'nbMembers'],
  data : function() {
    return {
    }
  },
  computed: {
    statusStyle: function() {
      return (this.status == "online") ? "user-available" : "user-invisible";
    },
    contactAvatar() {
      return (this.type == "t") ? "/chat/img/user-default.jpg" : this.avatar
    }
  },
  methods: {
    
  }
}
</script>