import { shallow } from 'vue-test-utils';
import ChatMessageList from '../../main/webapp/vue-app/components/ChatMessageList';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';

function getMessageListDetail() {
  return shallow(ChatMessageList, {
    propsData: {
      miniChat : false
    },
    mocks: {
      $t: (key, params) => {
        return `${key} params: ${params ? JSON.stringify(params) :''}`;
      },
      $constants : chatConstants,
      mq: 'desktop'
    }
  });
}

describe('ChatMessageList.test.js', () => {
  let cmp = getMessageListDetail();

  it('message list test', () => {
    expect(cmp.vm.messagesMap).not.toBeUndefined();
  });

});