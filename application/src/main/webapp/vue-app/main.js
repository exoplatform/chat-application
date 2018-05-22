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
$.fn.extend({
  insertAtCaret: function(newValue) {
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
  }
});
