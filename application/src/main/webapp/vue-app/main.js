import {addCaretJQueryExtension} from '../js/lib/text-caret';
import {chatConstants} from './chatConstants.js';
import {registerExternalExtensions} from './extension.js';

import './../css/main.less';
import './components/initComponents.js';

const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';
const url = `${chatConstants.PORTAL}/${chatConstants.PORTAL_REST}/i18n/bundle/locale.portlet.chat.Resource-${lang}.json`;

Vue.use(Vuetify);
const vuetify = new Vuetify({
  dark: true,
  iconfont: '',
});

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
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    registerExternalExtensions(i18n.messages[lang]['exoplatform.chat.open.chat']);

    if ($('#chatApplication').length) {
      new Vue({
        el: '#chatApplication',
        template: '<exo-chat-app></exo-chat-app>',
        i18n,
        vuetify
      });
    } else if ($('#chatNotification').length) {
      new Vue({
        el: '#chatNotification',
        template: '<exo-chat-drawer></exo-chat-drawer>',
        i18n,
        vuetify
      });
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
