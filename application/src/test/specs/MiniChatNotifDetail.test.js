import { shallow } from 'vue-test-utils';
import MiniChatNotifDetail from '../../main/webapp/vue-app/components/MiniChatNotifDetail';
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

function getNotif(msg, fullname, type, additionalOptions) {
  const notif = {
    content: msg,
    from: fullname,
    fromFullName: fullname,
    roomDisplayName: fullname,
    options: {
      type: type,
      fullname: fullname
    },
    timestamp: Date.UTC(2000, 11, 10, 3, 0, 0)
  };
  if (additionalOptions && Object.keys(additionalOptions).length) {
    Object.keys(additionalOptions).forEach(function(key){
      notif.options[key] = additionalOptions[key];
    });
  }
  const comp =  shallow(MiniChatNotifDetail, {
    propsData: {
      miniChat : false,
      notif: notif
    },
    mocks: {
      $t: (key, params) => {
        return `${key} params: ${params ? JSON.stringify(params) :''}`;
      },
      $constants : chatConstants,
      mq: 'desktop'
    }
  });
  return comp;
}

describe('MiniChatNotifDetail.test.js', () => {

  it('test normal notif content', () => {
    const cmp = getNotif('Test message', 'Test User', null);
    expect(cmp.vm.messageClass).toBe('');
    expect(cmp.vm.isSpecificMessageType).toBeFalsy();
    expect(cmp.vm.specificMessageObj).toEqual({});
    expect(cmp.vm.specificMessageContent).toEqual('');
    expect(cmp.vm.specificMessageClass).toBeUndefined();
    expect(cmp.vm.messageContent).toContain('Test message');
    expect(cmp.html()).toContain('Test User');
    expect(cmp.html()).toContain('Test message');
    expect(cmp.findAll('.timestamp')).toHaveLength(1);
  });

  it('test question notif content', () => {
    const cmp = getNotif('Test message', 'Test User', chatConstants.QUESTION_MESSAGE);
    expect(cmp.vm.messageClass).toContain('uiIconChatQuestion');
    expect(cmp.vm.isSpecificMessageType).toBeFalsy();
    expect(cmp.vm.specificMessageObj).toEqual({});
    expect(cmp.vm.specificMessageContent).toEqual('');
    expect(cmp.vm.specificMessageClass).toBeUndefined();
    expect(cmp.vm.messageContent).not.toContain('Test message');
    expect(cmp.html()).toContain('Test User');
    expect(cmp.html()).not.toContain('Test message');
    expect(cmp.findAll('.timestamp')).toHaveLength(1);
  });

  it('test raise hand notif content', () => {
    const cmp = getNotif('Test message', 'Test User', chatConstants.RAISE_HAND);
    expect(cmp.vm.messageClass).toContain('uiIconChatRaiseHand');
    expect(cmp.vm.isSpecificMessageType).toBeFalsy();
    expect(cmp.vm.specificMessageObj).toEqual({});
    expect(cmp.vm.specificMessageContent).toEqual('');
    expect(cmp.vm.specificMessageClass).toBeUndefined();
    expect(cmp.vm.messageContent).not.toContain('Test message');
    expect(cmp.html()).toContain('Test User');
    expect(cmp.html()).not.toContain('Test message');
    expect(cmp.findAll('.timestamp')).toHaveLength(1);
  });

  it('test file notif content', () => {
    const cmp = getNotif('Test message', 'Test User', chatConstants.FILE_MESSAGE);
    expect(cmp.vm.messageClass).toContain('uiIconChatUpload');
    expect(cmp.vm.isSpecificMessageType).toBeFalsy();
    expect(cmp.vm.specificMessageObj).toEqual({});
    expect(cmp.vm.specificMessageContent).toEqual('');
    expect(cmp.vm.specificMessageClass).toBeUndefined();
    expect(cmp.vm.messageContent).not.toContain('Test message');
    expect(cmp.html()).toContain('Test User');
    expect(cmp.html()).not.toContain('Test message');
    expect(cmp.findAll('.timestamp')).toHaveLength(1);
  });

  it('test link notif content', () => {
    const cmp = getNotif('Test message', 'Test User', chatConstants.LINK_MESSAGE, {link: '#linkToURL'});
    expect(cmp.vm.messageClass).toContain('uiIconChatLink');
    expect(cmp.vm.isSpecificMessageType).toBeFalsy();
    expect(cmp.vm.specificMessageObj).toEqual({});
    expect(cmp.vm.specificMessageContent).toEqual('');
    expect(cmp.vm.specificMessageClass).toBeUndefined();
    expect(cmp.vm.messageContent).not.toContain('Test message');
    expect(cmp.vm.messageContent).toContain('#linkToURL');
    expect(cmp.html()).toContain('Test User');
    expect(cmp.html()).toContain('#linkToURL');
    expect(cmp.html()).not.toContain('Test message');
    expect(cmp.findAll('.timestamp')).toHaveLength(1);
  });

  it('test notes saved content', () => {
    const cmp = getNotif('Test message', 'Test User', chatConstants.NOTES_MESSAGE);
    expect(cmp.vm.messageClass).toContain('uiIconChatMeeting');
    expect(cmp.vm.isSpecificMessageType).toBeFalsy();
    expect(cmp.vm.specificMessageObj).toEqual({});
    expect(cmp.vm.specificMessageContent).toEqual('');
    expect(cmp.vm.specificMessageClass).toBeUndefined();
    expect(cmp.vm.messageContent).not.toContain('Test message');
    expect(cmp.html()).toContain('Test User');
    expect(cmp.html()).not.toContain('Test message');
    expect(cmp.findAll('.timestamp')).toHaveLength(1);
  });

  it('test event content', () => {
    const cmp = getNotif('Test message', 'Test User', chatConstants.EVENT_MESSAGE, {summary: 'event summary'});
    expect(cmp.vm.messageClass).toContain('uiIconChatCreateEvent');
    expect(cmp.vm.isSpecificMessageType).toBeFalsy();
    expect(cmp.vm.specificMessageObj).toEqual({});
    expect(cmp.vm.specificMessageContent).toEqual('');
    expect(cmp.vm.specificMessageClass).toBeUndefined();
    expect(cmp.vm.messageContent).toContain('event summary');
    expect(cmp.vm.messageContent).not.toContain('Test message');
    expect(cmp.html()).toContain('Test User');
    expect(cmp.html()).toContain('event summary');
    expect(cmp.html()).not.toContain('Test message');
    expect(cmp.findAll('.timestamp')).toHaveLength(1);
  });

});