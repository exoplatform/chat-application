
<template>
  <v-chip
    close
    class="identitySuggesterItem me-4 mt-4"
    @click:close="$emit('remove-attendee', attendee)">
    <v-avatar left>
      <v-img :src="avatarUrl" />
    </v-avatar>
    <span class="text-truncate">
      {{ displayName }}
    </span>
  </v-chip>
</template>

<script>
export default {
  props: {
    attendee: {
      type: Object,
      default: () => ({}),
    },
  },
  computed: {
    avatarUrl() {
      const profile = this.attendee.identity && this.attendee.identity.profile;
      return profile && (profile.avatarUrl || profile.avatar || '/portal/rest/v1/social/users/default-image/avatar');
    },
    displayName() {
      const profile = this.attendee.identity && this.attendee.identity.profile;
      const fullName = profile && (profile.displayName || profile.fullName );
      return this.isExternal ? `${fullName} (${this.$t('profile.External')})` : fullName;
    },
    isExternal() {
      const profile = this.attendee.identity && this.attendee.identity.profile ;
      return profile && (profile.dataEntity && profile.dataEntity.external === 'true' || profile.external);
    },
  },
};
</script>
