import Vue from 'vue';
import exoi18n from '../js/lib/exo-i18n';
import ChatApp from './components/ChatApp.vue';
import MiniChatApp from './components/MiniChatApp.vue';
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
    $(el).on('taphold', () => callback());
  }
});

exoi18n.loadLanguageAsync(lang).then(i18n => {
  if ($('#chatApplication').length) {
    new Vue({
      el: '#chatApplication',
      render: h => h(ChatApp),
      i18n
    });
  } else if ($('#chatApplicationNotification').length) {
    new Vue({
      el: '#chatApplicationNotification',
      render: h => h(MiniChatApp),
      i18n
    });
  }
});

$.fn.extend({
  insertAtCaret(newValue) {
    this.each(( i, element ) => {
      if (document.selection) {
        element.focus();
        const sel = document.selection.createRange();
        sel.text = newValue;
        element.focus();
      } else if (element.selectionStart || element.selectionStart === '0' || element.selectionStart === 0) {
        const startPos = element.selectionStart;
        const endPos = element.selectionEnd;
        const scrollTop = element.scrollTop;
        element.value = element.value.substring(0, startPos) + newValue + element.value.substring(endPos,element.value.length);
        element.focus();
        element.selectionStart = startPos + newValue.length;
        element.selectionEnd = startPos + newValue.length;
        element.scrollTop = scrollTop;
      } else {
        element.value += newValue;
        element.focus();
      }
    });
  },
  serializeFormJSON() {
    const o = {};
    const a = this.serializeArray();
    $.each(a, function () {
      if (o[this.name]) {
        if (!o[this.name].push) {
          o[this.name] = [o[this.name]];
        }
        o[this.name].push(this.value || '');
      } else {
        o[this.name] = this.value || '';
      }
    });
    return o;
  }
});

// A global data
Vue.mixin({
  data: function() {
    return {
      mq: '',
      get EMOTICONS() {
        return [
          {
            keys: [':)', ':-)'],
            class: 'emoticon-smile'
          },
          { 
            keys: [':(', ':-('],
            class: 'emoticon-sad'
          },
          { 
            keys: [';)', ';-)'],
            class: 'emoticon-wink'
          },
          { 
            keys: [':|', ':-|'],
            class: 'emoticon-speechless'
          },
          { 
            keys: [':o', ':-o'],
            class: 'emoticon-surprise'
          },
          { 
            keys: [':p', ':-p'],
            class: 'emoticon-smile-tongue'
          },
          { 
            keys: [':d', ':-d'],
            class: 'emoticon-flaugh'
          },
          { 
            keys: ['(cool)'],
            class: 'emoticon-cool'
          },
          { 
            keys: ['(y)', '(yes)'],
            class: 'emoticon-raise-up'
          },
          { 
            keys: ['(n)', '(no)'],
            class: 'emoticon-raise-down'
          }
        ];
      }
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
