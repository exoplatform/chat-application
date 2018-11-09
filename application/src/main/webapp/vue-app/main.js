import Vue from 'vue';
import {addCaretJQueryExtension} from '../js/lib/text-caret';
import {chatConstants} from './chatConstants.js';

import './../css/main.less';
import './components/initComponents.js';

const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';
const url = `${chatConstants.PORTAL}/${chatConstants.PORTAL_REST}/i18n/bundle/locale.portlet.chat.Resource-${lang}.json`;

Vue.prototype.$constants = chatConstants;

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

exoi18n.loadLanguageAsync(lang, url).then(i18n => {
  if ($('#chatApplication').length) {
    new Vue({
      el: '#chatApplication',
      template: '<exo-chat-app></exo-chat-app>',
      i18n
    });
  } else if ($('#chatApplicationNotification').length) {
    new Vue({
      el: '#chatApplicationNotification',
      template: '<exo-chat-notif-app></exo-chat-notif-app>',
      i18n
    });
  }
});

// A global data
Vue.mixin({
  data: function() {
    return {
      mq: ''
    };
  },
  created() {
    this.handleMediaQuery();
    window.addEventListener('resize', this.handleMediaQuery);
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