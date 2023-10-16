<!--
Copyright (C) 2023 eXo Platform SAS.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
-->
<template>
  <user-notification-template
    :notification="notification"
    :avatar-url="avatarUrl"
    :message="message"
    :url="url"
    user-avatar />
</template>
<script>
export default {
  props: {
    notification: {
      type: Object,
      default: null,
    },
  },
  computed: {
    url() {
      //we keep both possibility so that old notifications, generated before the current modification are still ok
      return this.notification?.parameters?.chatUrl || `/portal/dw/?chatRoomId=${this.notification?.parameters?.roomId}`;
    },
    avatarUrl() {
      //we keep both possibility so that old notifications, generated before the current modification are still ok
      return this.notification?.parameters?.avatar || `/portal/rest/v1/social/users/${this.notification?.parameters?.sender}/avatar`;
    },
    message() {
      const creator = this.notification?.parameters?.senderFullName;
      const room = this.notification?.parameters?.roomName;

      return this.$t('exoplatform.chat.MentionPlugin', {
        0: `<a class="user-name font-weight-bold">${creator}</a>`,
        1: `<a class="space-name font-weight-bold">${room}</a>`
      });
    }
  }
};
</script>
