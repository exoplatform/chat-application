import Vue from 'vue';
import exoi18n from '../js/lib/exo-i18n';
import ChatApp from './components/ChatApp.vue';
import './../css/main.less';

const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : '';

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
      render: h => h(ChatApp),
      i18n
    });
  }
});