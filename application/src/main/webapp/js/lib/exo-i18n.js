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
    // TODO replace by a call to a new REST service which loads eXo resource bundles (ResourceBundleService)
    return fetch(`/chat/lang/${lang}.json`).then(resp => resp.json()).then(msgs => {
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