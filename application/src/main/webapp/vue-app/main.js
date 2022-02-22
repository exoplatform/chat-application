import {addCaretJQueryExtension} from '../js/lib/text-caret';
import {chatConstants} from './chatConstants.js';
import {registerExternalExtensions} from './extension.js';

import './components/initComponents.js';

const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';
const url = `${chatConstants.PORTAL}/${chatConstants.PORTAL_REST}/i18n/bundle/locale.portlet.chat.Resource-${lang}.json`;

Vue.use(Vuetify);
const vuetify = new Vuetify(eXo.env.portal.vuetifyPreset);

Vue.directive('exo-tooltip', function (el, binding) {
  const element = $(el);
  const placement = Object.keys(binding.modifiers)[0];
  const container = Object.keys(binding.modifiers)[1];
  element.attr('data-original-title', binding.value);
  if (placement) {
    element.attr('data-placement', placement);
  }
  if (container) {
    element.attr('data-container', container);
  }
  element.tooltip();
});

Vue.directive('hold-tap', function (el, binding, vnode) {
  if (vnode.context.mq === 'mobile') {
    const callback = binding.value;
    $(el).off('taphold').on('taphold', () => callback(vnode.key));
  }
});

// get overrided components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('chat');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

export function init() {
  const useFullChatApp = $('#chatApplication').length;
  const useTopBarChatApp = $('#chatNotification').length;
  const appName = useFullChatApp && 'Chat' || 'Chat Topbar';
  if (useFullChatApp || useTopBarChatApp) {
    // emit the start loading event from here since
    // The I18N bundle is loaded in sync way
    document.dispatchEvent(new CustomEvent('vue-app-loading-start', {detail: appName}));
  }

  exoi18n.loadLanguageAsync(lang, url, 'sync')
    .then(i18n => {
      registerExternalExtensions(i18n.messages[lang]['exoplatform.chat.open.chat']);


      if (extensionRegistry) {
        extensionRegistry.registerComponent('SpaceSettings', 'space-settings-components', {
          id: 'chat-space-setting',
          vueComponent: Vue.options.components['exo-chat-space-settings'],
          rank: 10,
        });
      }

      if (useFullChatApp) {
        Vue.createApp({
          template: '<exo-chat-app></exo-chat-app>',
          i18n,
          vuetify
        }, '#chatApplication', appName);
      } else if (useTopBarChatApp) {
        Vue.createApp({
          template: '<exo-chat-drawer></exo-chat-drawer>',
          i18n,
          vuetify
        }, '#chatNotification', appName);
      }
    });
}

// A global data
Vue.mixin({
  data: function() {
    return {
      mq: '',
      ap: false
    };
  },
  created() {
    this.handleMediaQuery();
    window.addEventListener('resize', this.handleMediaQuery);
    if (location.pathname==='/portal/'.concat(eXo.env.portal.portalName).concat('/chat')) {
      this.ap = true;
    } else {
      this.ap = false;
    }
  },
  methods: {
    handleMediaQuery() {
      if (window.matchMedia('(max-width: 767px)').matches) {
        this.mq = 'mobile';
      } else if (window.matchMedia('(max-width: 1024px)').matches) {
        this.mq = 'tablet';
      } else {
        this.mq = 'desktop';
      }
    }
  }
});

addCaretJQueryExtension($);
