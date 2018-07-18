import { mount } from 'vue-test-utils';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';

import ExoChatComposerAppsModal from '../../main/webapp/vue-app/components/modal/ExoChatComposerAppsModal';

global.fetch = jest.fn().mockImplementation(() => {
  const p = new Promise((resolve) => {
    resolve({
      text: function() {
        return '';
      },
      json: function() {
        return {};
      }
    });
  });
  return p;
});

function getComponent(show) {
  const comp = mount(ExoChatComposerAppsModal, {
    propsData: {
      roomId: '851548984ea54546546ae54651e564e5a58',
      title: 'Test Room Name',
      app: {
        key: 'test',
        type: 'type-test',
        nameKey: 'exoplatform.chat.test',
        labelKey: 'exoplatform.chat.share.test',
        iconClass: 'uiIconChatTest',
        appClass: 'uiIconChatAppTest',
        saveLabelKey: 'exoplatform.chat.share',
        hideModalActions: true,
        init() {
          document.dispatchEvent(new CustomEvent('chat-custom-app-init'));
        },
        validate(formData) {
          document.dispatchEvent(new CustomEvent('chat-custom-app-validate', {detail: formData}));
        },
        htmlAdded() {
          document.dispatchEvent(new CustomEvent('chat-custom-app-htmlAdded'));
        },
        html() {
          return '<input name="testHTMLForm" id="testHTMLForm">';
        },
        submit(chatServices, message, formData, contact) {
          document.dispatchEvent(new CustomEvent('chat-custom-app-submit', {detail: {message: message, formData: formData, contact: contact}}));
          return {ok: true};
        }
      },
      contact: {
        'fullName':'Test User',
        'unreadTotal':0,
        'isActive':'true',
        'type':'u',
        'user':'testuser',
        'room':'eb74205830cf97546269bbdc5d439b29ddd1735b',
        'status':'invisible',
        'timestamp':1528455913624,
        'isFavorite':false
      }
    },
    mocks: {
      $t: (key, params) => {
        return `${key} params: ${params ? JSON.stringify(params) :''}`;
      },
      $constants : chatConstants,
      mq: 'desktop'
    }
  });
  comp.vm.show = show;
  comp.update();
  return comp;
}

describe('ExoChatComposerAppsModal.test.js', () => {

  it('test ExoChatComposerAppsModal DOM and attributes', () => {
    let initialized = false;
    let htmlAdded = false;
    let validateFormData = null;
    let saveObject = null;
    let sentMessage = null;

    document.addEventListener('chat-custom-app-init', () => initialized = true);
    document.addEventListener('chat-custom-app-validate', (e) => validateFormData = e.detail);
    document.addEventListener('chat-custom-app-htmlAdded', () => htmlAdded = true);
    document.addEventListener('chat-custom-app-submit', (e) => saveObject = e.detail);
    document.addEventListener(chatConstants.ACTION_MESSAGE_SEND, (e) => sentMessage = e.detail);

    const cmp = getComponent(true);
    expect(initialized).toBeTruthy();
    cmp.vm.$nextTick(() => {
      expect(htmlAdded).toBeTruthy();
    });

    expect(cmp.findAll('.uiIconChatAppTest')).toHaveLength(1);
    expect(cmp.vm.appHtml).toBe('<input name="testHTMLForm" id="testHTMLForm">');
    expect(cmp.vm.saveLabelKey).toBe('exoplatform.chat.share');

    const inputElement = cmp.find('.uiIconChatAppTest').element.firstElementChild;
    expect(inputElement.id).toBe('testHTMLForm');

    cmp.vm.saveAppModal();

    expect(validateFormData.type).toBe('type-test');
    expect(validateFormData.fromUser).toBe('root');
    expect(validateFormData.fromFullname).toBe('Root Root');
    expect(saveObject.formData.fromUser).toBe('root');
    expect(saveObject.formData.fromFullname).toBe('Root Root');
    expect(saveObject.formData.type).toBe('type-test');
    expect(saveObject.message.room).toBe('851548984ea54546546ae54651e564e5a58');
    expect(saveObject.message.user).toBe('root');

    expect(sentMessage.user).toBe('root');
    expect(sentMessage.room).toBe('851548984ea54546546ae54651e564e5a58');
    expect(sentMessage.options.type).toBe('type-test');
  });

});