import VueI18n from 'vue-i18n';

const loadedLanguages = [''];
const messages = {};

const i18n = new VueI18n({
  locale: 'en', // set locale
  fallbackLocale: 'en',
  messages
});

function loadLanguageAsync (lang) {
  if(!lang) {
    lang = i18n.locale;
  }
  if (loadedLanguages.indexOf(lang) < 0) {
    return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.portlet.chat.Resource-${lang}.json`, {
      credentials: 'include'
    }).then(resp => resp.json()).then(msgs => {
      i18n.setLocaleMessage(lang, msgs);
      loadedLanguages.push(lang);
      i18n.locale = lang;
      return i18n;
    });
  }
  return i18n;
}

export default {
  loadLanguageAsync
};