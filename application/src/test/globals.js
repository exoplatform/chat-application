import $ from 'jquery'
import {addCaretJQueryExtension} from '../main/webapp/js/lib/text-caret'

global.$ = $;
addCaretJQueryExtension($);
global.eXo = {
  env: {
    portal: {
      context: 'portal',
      rest: 'rest'
    }
  },
  chat: {
    userSettings: {
      username: 'root',
      fullName: 'Root Root',
      isOnline: true
    }
  }
};