import app from './components/chatButton.vue';
import '../components/initComponents';
import {chatConstants} from '../chatConstants.js';
// getting language of user
const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';
const url = `${chatConstants.PORTAL}/${chatConstants.PORTAL_REST}/i18n/bundle/locale.portlet.chat.Resource-${lang}.json`;

// getting locale ressources
exoi18n.loadLanguageAsync(lang, url)
  .then(i18n => {
    // init Vue app when locale ressources are ready
    new Vue({
      render: h => h(app),
      i18n
    }).$mount('#chatButtonApplication');
  });