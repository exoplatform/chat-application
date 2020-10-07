import { shallow } from 'vue-test-utils';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';
import {EMOTICONS} from '../../main/webapp/vue-app/extension.js';

import ExoChatMessageComposer from '../../main/webapp/vue-app/components/ExoChatMessageComposer';

describe('ExoChatMessageComposer.test.js', () => {
  const cmp = shallow(ExoChatMessageComposer, {
    propsData: {
      miniChat : false,
      contact: {
        'fullName':'Test User',
        'unreadTotal':0,
        'isActive':'true',
        'isEnabledUser':'true',
        'type':'u',
        'user':'testuser',
        'room':'eb74205830cf97546269bbdc5d439b29ddd1735b',
        'status':'invisible',
        'timestamp':1528455913624,
        'isFavorite':false
      }
    },
    mocks: {
      $t: () => {},
      $constants : chatConstants,
      mq: 'desktop'
    }
  });

  it('composer apps count', () => {
    expect(cmp.vm.composerApplications).toHaveLength(4);
  });

  it('composer emoticons count', () => {
    expect(cmp.vm.getEmoticons).toHaveLength(EMOTICONS.length);
  });

  it('close emoji panel', () => {
    cmp.setData({showEmojiPanel : true});
    cmp.vm.closeEmojiPanel();
    expect(cmp.vm.showEmojiPanel).toBe(false);
  });

  it('close apps panel', () => {
    cmp.vm.appsClosed = false;
    cmp.vm.closeApps({keyCode: 27});
    expect(cmp.vm.appsClosed).toBeTruthy();
  });

  it('select emoji', () => {
    cmp.vm.selectEmoji({keys: [':)', ':-)'],class: 'emoticon-smile'});
    expect(cmp.vm.$refs.messageComposerArea.innerHTML).toBe(' :) ');
  });

  it('send message', () => {
    cmp.vm.sendMessage();
    expect(cmp.emitted('message-written')).toHaveLength(1);
    expect(cmp.vm.$refs.messageComposerArea.innerHTML).toBe('');
  });

  it('send empty message', () => {
    cmp.vm.$refs.messageComposerArea.innerHTML = '';

    cmp.vm.sendMessage();
    expect(cmp.vm.$refs.messageComposerArea.innerHTML).toBe('');
  });

  /*document.getSelection found in text-caret.js cannot be called in Jest test*/

  /*it('send message with key', () => {
    expect(cmp.emitted('message-written')).toHaveLength(1);

    cmp.vm.$refs.messageComposerArea.innerHTML = 'test message';

    cmp.vm.sendMessageWithKey({
      keyCode : 13,
      shiftKey : true,
      ctrlKey : false,
      altKey : false
    });
    expect(cmp.vm.$refs.messageComposerArea.innerHTML).toBe('\ntest message');
    expect(cmp.emitted('message-written')).toHaveLength(1);

    cmp.vm.sendMessageWithKey({
      keyCode : 13,
      shiftKey : false,
      ctrlKey : true,
      altKey : false
    });
    expect(cmp.vm.$refs.messageComposerArea.innerHTML).toBe('\n\ntest message');
    expect(cmp.emitted('message-written')).toHaveLength(1);

    cmp.vm.sendMessageWithKey({
      keyCode : 13,
      shiftKey : false,
      ctrlKey : false,
      altKey : true
    });
    expect(cmp.vm.$refs.messageComposerArea.innerHTML).toBe('\n\n\ntest message');
    expect(cmp.emitted('message-written')).toHaveLength(1);

    cmp.vm.sendMessageWithKey({
      keyCode : 13,
      shiftKey : false,
      ctrlKey : false,
      altKey : false
    });
    expect(cmp.vm.$refs.messageComposerArea.innerHTML).toBe('');
    expect(cmp.emitted('message-written')).toHaveLength(2);
  });

  it('quote message', () => {
    cmp.vm.quoteMessage({detail: {
      message: 'Test message',
      fullname: 'Test User'
    }});
    expect(cmp.vm.$refs.messageComposerArea.innerHTML).toBe('[quote=Test User] Test message [/quote]');
  });*/

  it('edit last message', () => {
    let editLastEventCalled = false;
    cmp.vm.$refs.messageComposerArea.innerHTML = '';
    document.addEventListener(chatConstants.ACTION_MESSAGE_EDIT_LAST, () => {editLastEventCalled = true;});
    cmp.vm.editLastMessage();
    expect(editLastEventCalled).toBeTruthy();
  });
  
  it('open App modal', () => {
    cmp.vm.openAppModal({
      key: 'test',
      iconClass: 'testIcon',
      labelKey: 'testLabel'
    });
    expect(cmp.vm.appsModal.isOpned).toBeTruthy();
  });

});
