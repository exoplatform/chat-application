<template>
  <div class="chat-contact" :class="statusStyle">
  	<div class="chat-contact-avatar" :style="{backgroundImage: 'url(' + contactAvatar + ')'}">
  		<i v-if="list && type=='u'" class="uiIconStatus"></i>
  	</div>
  	<div class="contactLabel">
  		{{name}}
  		<div v-if="type=='u' && !list" class="user-status">
  			<i class="uiIconStatus"></i>
  			{{status}}
  		</div>
  	</div>
  </div>
</template>

<script>

export default {
  props: ['name', 'avatar', 'status', 'list', 'type'],
  template: '#chat-contact-component',
  data : function() {
    return {
    }
  },
  computed: {
    statusStyle: function() {
      return (this.status == "online") ? "user-available" : "user-invisible";
    },
    contactAvatar() {
      return (this.type == "t") ? "/chatApplicationVue/img/user-default.jpg" : this.avatar
    }
  },
  methods: {
    defaultAvatar (e) {
      console.log(e)
    }
  }
}
</script>