import Vue from 'vue';
import exoi18n from '../js/lib/exo-i18n';
import {addCaretJQueryExtension} from '../js/lib/text-caret';
import ExoChatApp from './components/ExoChatApp.vue';
import ExoMiniChatApp from './components/ExoMiniChatApp.vue';
import {chatConstants} from './chatConstants.js';

import './../css/main.less';

const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : '';

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

exoi18n.loadLanguageAsync(lang).then(i18n => {
  if ($('#chatApplication').length) {
    new Vue({
      el: '#chatApplication',
      render: h => h(ExoChatApp),
      i18n
    });
  } else if ($('#chatApplicationNotification').length) {
    new Vue({
      el: '#chatApplicationNotification',
      render: h => h(ExoMiniChatApp),
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