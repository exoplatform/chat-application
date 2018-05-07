import Vue from 'vue';
import exoi18n from '../js/lib/exo-i18n';
import ChatApp from './components/ChatApp.vue';
import './../css/main.scss';

var lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : '';

exoi18n.loadLanguageAsync(lang).then(i18n => {
  new Vue({
    el: '#chatApplicationVue',
    render: h => h(ChatApp),
    i18n
  });}
);
