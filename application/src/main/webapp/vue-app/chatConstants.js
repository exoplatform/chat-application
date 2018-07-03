export const chatConstants = {
  PORTAL: eXo.env.portal.context || '',
  PORTAL_NAME: eXo.env.portal.portalName || '',
  PORTAL_REST: eXo.env.portal.rest,
  SOCIAL_USER_API: '/v1/social/users/',
  SOCIAL_SPACE_API: '/v1/social/spaces/',
  PEOPLE_INFO_API: '/social/people/getPeopleInfo/{0}.json',
  CHAT_API: '/chat/api/1.0/user/',
  CHAT_WIKI_API: '/chat/api/1.0/wiki/',
  CHAT_CALENDAR_API: '/chat/api/1.0/calendar/',
  PROFILE_PAGE_NAME: 'profile',
  PROFILE_SPACE_LINK: '/g/:spaces:',
  CHAT_SERVER_API: '/chatServer/',
  CHAT_PLUGIN_API: '/chat/api/1.0/plugin/',
  UPLOAD_API: '/portal/upload',
  FILE_PERSIST_URL: '/portal/rest/chat/api/1.0/file/persist',
  MAX_UPLOAD_FILES: 1,
  ON_SITE_NOTIF: 'on-site',
  DESKTOP_NOTIF: 'desktop',
  BIP_NOTIF: 'bip',
  NOT_DISTURB_NOTIF: 'notify-even-not-disturb',
  TYPE_FILTER_PARAM: 'exo.chat.type.filter',
  TYPE_FILTER_DEFAULT: 'All',
  SORT_FILTER_PARAM: 'exo.chat.sort.filter',
  SORT_FILTER_DEFAULT: 'Recent',
  CONTACTS_PER_PAGE: 20,
  ADD_TEAM_MESSAGE: 'type-add-team-user',
  REMOVE_TEAM_MESSAGE: 'type-remove-team-user',
  EVENT_MESSAGE: 'type-event',
  FILE_MESSAGE: 'type-file',
  LINK_MESSAGE: 'type-link',
  RAISE_HAND: 'type-hand',
  QUESTION_MESSAGE: 'type-question',
  NOTES_MESSAGE: 'type-notes',
  MEETING_START_MESSAGE: 'type-meeting-start',
  MEETING_STOP_MESSAGE: 'type-meeting-stop',
  ADD_USER_MESSAGE: 'type-add-team-user',
  REMOVE_USER_MESSAGE: 'type-remove-team-user',
  CALL_JOIN_MESSAGE: 'call-join',
  CALL_ON_MESSAGE: 'call-on',
  CALL_OFF_MESSAGE: 'call-off',
  DELETED_MESSAGE: 'DELETED',
  EDITED_MESSAGE: 'EDITED',
  ROOM_MEMBER_LEFT: 'room-member-left',
  ANIMATION_PERIOD: 200,
  ANIMATION_DURATION: 1000,
  MESSAGES_PER_PAGE: 50,
  MAX_SCROLL_POSITION_FOR_AUTOMATIC_SCROLL: 25,
  ENTER_CODE_KEY: 13,
  STATUS_FILTER_PARAM: 'exo.chat.room.participant.filter',
  STATUS_FILTER_DEFAULT: 'All',
  REATTEMPT_PERIOD: 1000,
  NB_MILLISECONDS_PERD_SECOND: 1000,
  LAST_SELECTED_ROOM_PARAM: 'lastSelectedRoom',
  STORED_NOT_SENT_MESSAGES: 'roomNotSentMessages',
  ACTION_MESSAGE_SEND: 'exo-chat-message-tosend-requested',
  ACTION_MESSAGE_EDIT: 'exo-chat-message-action-edit-requested',
  ACTION_MESSAGE_QUOTE: 'exo-chat-message-action-quote-requested',
  ACTION_MESSAGE_EDIT_LAST: 'exo-chat-message-edit-last-requested',
  ACTION_MESSAGE_SEARCH: 'exo-chat-message-search-requested',
  ACTION_MESSAGE_DELETE: 'exo-chat-message-todelete-requested',
  ACTION_ROOM_SET_READ: 'exo-chat-set-room-as-read-requested',
  ACTION_ROOM_SELECT: 'exo-chat-select-room-requested',
  ACTION_ROOM_DELETE: 'exo-chat-setting-deleteRoom-requested',
  ACTION_ROOM_EDIT: 'exo-chat-setting-editRoom-requested',
  ACTION_ROOM_LEAVE: 'exo-chat-setting-leaveRoom-requested',
  ACTION_ROOM_OPEN_SETTINGS: 'exo-chat-setting-notificationSettings-requested',
  ACTION_ROOM_START_MEETING: 'exo-chat-setting-startMeeting-requested',
  ACTION_ROOM_STOP_MEETING: 'exo-chat-setting-stopMeeting-requested',
  ACTION_ROOM_FAVORITE_ADD: 'exo-chat-setting-addToFavorite-requested',
  ACTION_ROOM_FAVORITE_REMOVE: 'exo-chat-setting-removeFromFavorite-requested',
  ACTION_ROOM_SHOW_PARTICIPANTS: 'exo-chat-setting-showParticipants-requested',
  ACTION_APPS_CLOSE: 'exo-chat-apps-close-requested',
  ACTION_ROOM_OPEN_MINI_CHAT: 'exo-chat-room-open-mini-requested',
  EVENT_CONNECTED: 'exo-chat-connected',
  EVENT_DISCONNECTED: 'exo-chat-disconnected',
  EVENT_RECONNECTED: 'exo-chat-reconnected',
  EVENT_LOGGED_OUT: 'exo-chat-logout-sent',
  EVENT_MESSAGE_RECEIVED: 'exo-chat-message-sent',
  EVENT_MESSAGE_DELETED: 'exo-chat-message-deleted',
  EVENT_MESSAGE_NOT_SENT: 'exo-chat-message-not-sent',
  EVENT_MESSAGE_SENT: 'exo-chat-message-sent-ack',
  EVENT_MESSAGE_READ: 'exo-chat-message-read',
  EVENT_MESSAGE_UPDATED: 'exo-chat-message-updated',
  EVENT_GLOBAL_UNREAD_COUNT_UPDATED: 'exo-chat-notification-count-updated',
  EVENT_ROOM_PARTICIPANTS_LOADED: 'exo-chat-participants-loaded',
  EVENT_ROOM_DELETED: 'exo-chat-room-deleted',
  EVENT_ROOM_FAVORITE_ADDED: 'exo-chat-room-favorite-added',
  EVENT_ROOM_FAVORITE_REMOVED: 'exo-chat-room-favorite-removed',
  EVENT_ROOM_MEMBER_JOINED: 'exo-chat-room-member-joined',
  EVENT_ROOM_MEMBER_LEFT: 'exo-chat-room-member-left',
  EVENT_ROOM_UPDATED: 'exo-chat-room-updated',
  EVENT_ROOM_SELECTION_CHANGED: 'exo-chat-selected-contact-changed',
  EVENT_USER_SETTINGS_LOADED: 'exo-chat-settings-loaded',
  EVENT_USER_STATUS_CHANGED: 'exo-chat-user-status-changed'
};
