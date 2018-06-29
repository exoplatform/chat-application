import { shallow } from 'vue-test-utils';
import MiniChatRoom from '../../main/webapp/vue-app/components/MiniChatRoom';
import ChatMessageList from '../../main/webapp/vue-app/components/ChatMessageList';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';

global.fetch = jest.fn().mockImplementation(() => {
  var p = new Promise((resolve) => {
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

function getComponent() {
  return shallow(MiniChatRoom, {
    propsData: {
      room: '54654fa654654fa65f6af4654654f4f4f6546'
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

describe('MiniChatRoom.test.js', () => {

  it('test display chat messages list', () => {
    const cmp = getComponent();
    expect(cmp.findAll(ChatMessageList)).toHaveLength(1);
    expect(cmp.findAll('.uiIconMinimize')).toHaveLength(1);
    expect(cmp.findAll('.uiIconMaximize')).toHaveLength(1);
    expect(cmp.findAll('.uiIconChatPopOut')).toHaveLength(1);
    expect(cmp.findAll('.uiIconClose')).toHaveLength(1);
  });

  it('test call messages reload when room changes', () => {
    const cmp = getComponent();
    let loadRoomMessagesCalled = false;
    global.fetch = jest.fn().mockImplementation((url) => {
      loadRoomMessagesCalled = url && url.indexOf('targetUser=ae54ae1a5e4a5e1ae54aea5eaee44411263e') >= 0;
      var p = new Promise((resolve) => {
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
    cmp.vm.room = 'ae54ae1a5e4a5e1ae54aea5eaee44411263e';
    cmp.update();
    expect(loadRoomMessagesCalled).toBeTruthy();
  });

});