import $ from 'jquery';
import Vue from 'vue';
import {addCaretJQueryExtension} from '../main/webapp/js/lib/text-caret';

global.extensionRegistry = require('./libs/extensionRegistry.js');

global.$ = $;
addCaretJQueryExtension($);

$.fn.extend({
  //mock userPopup extension
  userPopup() {}
});

Vue.directive('sanitized-html', (el, binding) => el.innerHTML = binding.value);
Vue.directive('autolinker', (el, binding) => el.innerHTML = binding.value);

global.eXo = {
  env: {
    portal: {
      context: 'portal',
      rest: 'rest',
      language: 'fr'
    }
  },
  chat: {
    userSettings: {
      username: 'root',
      fullName: 'Root Root',
      isOnline: true,
      status: 'invisible'
    },
    room : {
      extraApplications : [{
        key: 'test',
        iconClass: 'testIcon',
        labelKey: 'testLabel'
      }],
      extraActions: []
    },
    message : {
      extraActions : [{
        key: 'testAction',
        labelKey: 'testAction',
        enabled: () => {return true;}
      }],
      notifs: {
        
      }
    }
  }
};

const extraApplication = {
  key: 'test',
  iconClass: 'testIcon',
  labelKey: 'testLabel'
};

const extraMessage = {
  key: 'testAction',
  labelKey: 'testAction',
  enabled: () => {return true;}
};

const extraAction = {
  key: 'test',
  labelKey: 'Test',
  type: 't',
  class: 'uiIconTest'
};

extensionRegistry.registerExtension('chat', 'composer-application', extraApplication);
extensionRegistry.registerExtension('chat', 'message-action', extraMessage);
extensionRegistry.registerExtension('chat', 'room-action', extraAction);