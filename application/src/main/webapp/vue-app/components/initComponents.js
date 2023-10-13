/*
 * Copyright (C) 2022 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */
import ExoChatApp from './ExoChatApp.vue';
import ExoChatContact from './ExoChatContact.vue';
import ExoChatContactList from './ExoChatContactList.vue';
import ExoChatRoomParticipants from './ExoChatRoomParticipants.vue';
import ExoChatRoomDetail from './ExoChatRoomDetail.vue';
import ExoChatMessageList from './ExoChatMessageList.vue';
import ExoChatMessageDetail from './ExoChatMessageDetail.vue';
import ExoChatMessageComposer from './ExoChatMessageComposer.vue';
import PopoverChatButton from './PopoverChatButton.vue';

import ExoChatGlobalNotificationModal from './modal/ExoChatGlobalNotificationModal.vue';
import ExoChatComposerAppsModal from './modal/ExoChatComposerAppsModal.vue';
import ExoChatRoomNotificationModal from './modal/ExoChatRoomNotificationModal.vue';
import ExoChatRoomFormModal from './modal/ExoChatRoomFormModal.vue';

import ExoDropdownSelect from './ExoDropdownSelect.vue';
import ExoChatModal from './modal/ExoChatModal.vue';
import ExoChatDrawer from './modal/ExoChatDrawer.vue';
import ExoChatQuickCreateDiscussionDrawer from './modal/ExoChatQuickCreateDiscussionDrawer.vue';
import ExoChatQuickDiscussionParticipantItem from './modal/ExoChatQuickDiscussionParticipantItem.vue';

import spaceChatSetting from '../external-components/spaceChatSetting.vue';

const components = {
  'exo-chat-app': ExoChatApp,
  'exo-chat-modal': ExoChatModal,
  'exo-chat-contact': ExoChatContact,
  'exo-chat-contact-list': ExoChatContactList,
  'exo-chat-room-participants': ExoChatRoomParticipants,
  'exo-chat-room-detail': ExoChatRoomDetail,
  'exo-chat-message-list': ExoChatMessageList,
  'exo-chat-global-notification-modal': ExoChatGlobalNotificationModal,
  'exo-dropdown-select': ExoDropdownSelect,
  'exo-chat-room-form-modal': ExoChatRoomFormModal,
  'exo-chat-apps-modal': ExoChatComposerAppsModal,
  'exo-chat-message-detail': ExoChatMessageDetail,
  'exo-chat-message-composer': ExoChatMessageComposer,
  'exo-chat-room-notification-modal': ExoChatRoomNotificationModal,
  'exo-chat-drawer': ExoChatDrawer,
  'popover-chat-button': PopoverChatButton,
  'exo-chat-space-settings': spaceChatSetting,
  'exo-chat-quick-create-discussion-drawer': ExoChatQuickCreateDiscussionDrawer,
  'exo-chat-quick-discussion-participant-item': ExoChatQuickDiscussionParticipantItem,
};

// external components


for (const key in components) {
  Vue.component(key, components[key]);
}
