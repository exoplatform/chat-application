import VueI18n from 'vue-i18n'
import axios from 'axios'

const loadedLanguages = ['']
var messages = {};

const i18n = new VueI18n({
    locale: 'en', // set locale
    fallbackLocale: 'en',
    messages
})

async function loadLanguageAsync (lang) {
    if(!lang) {
        lang = i18n.locale
    }
    if (!loadedLanguages.includes(lang)) {
        // TODO replace by a call to a new REST service which loads eXo resource bundles (ResourceBundleService)
        return await axios.get('/chatApplicationVue/lang/' + lang + '.json').then(msgs => {
            i18n.setLocaleMessage(lang, msgs.data);
            loadedLanguages.push(lang)
            i18n.locale = lang
            return i18n;
        })
    }
    return i18n
}

export default {
    loadLanguageAsync
}