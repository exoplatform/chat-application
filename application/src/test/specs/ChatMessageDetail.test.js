import { shallow } from 'vue-test-utils';
import ChatMessageDetail from '../../main/webapp/vue-app/components/ChatMessageDetail';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';

const MSG_ID = '5b1a7a4067c9a30c23b5223d';
const DEFAULT_MSG = 'Test Message :)';
const USERNAME = 'testuser2';
const FULL_NAME = 'Test User 2';
const TIMESTAMP = 1528461888213;

function getMessage(msg, isSystem, options) {
  return shallow(ChatMessageDetail, {
    propsData: {
      miniChat : false,
      message: {
        msg: msg ? msg : DEFAULT_MSG,
        isSystem: isSystem ? isSystem : false,
        options: options ? options : {},
        msgId: MSG_ID,
        fullname: FULL_NAME,
        type: options ? options.type : null,
        user: USERNAME,
        timestamp: TIMESTAMP
      },
      room: '',
      roomFullname: '',
      hideAvatar: false,
      highlight: '',
      hideTime: false
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

describe('ChatMessageDetail.test.js', () => {
  let cmp = getMessage();

  it('message actions count', () => {
    expect(cmp.vm.messageActions.length).toBe(3);
  });

  it('could display actions', () => {
    expect(cmp.vm.displayActions).toBeTruthy();
  });

  it('display message id', () => {
    expect(cmp.vm.messageId).toBe('5b1a7a4067c9a30c23b5223d');
  });

  it('display message date', () => {
    expect(cmp.vm.dateString).toContain(':44');
  });

  it('display contact avatar', () => {
    expect(cmp.vm.contactAvatar).toContain('testuser2');
  });

  it('is message edited', () => {
    expect(cmp.vm.isEditedMessage).toBe(false);
  });

  it('get displayed message', () => {
    expect(cmp.vm.messageContent).toBe('Test Message :)');
  });

  it('get displayed message filtered', () => {
    expect(cmp.vm.messageFiltered.trim()).toBe('Test Message <span class="chat-emoticon emoticon-smile"></span>');
  });

  it('not specific message type', () => {
    expect(cmp.vm.specificMessageContent).toBe('');
  });

  it('show confirm modal when action needs it', () => {
    cmp.vm.executeAction({ confirm: {
      title: 'Modal title',
      message: 'confirm message',
      okMessage: 'OK',
      koMessage: 'KO',
    }});
    expect(cmp.vm.showConfirmModal).toBeTruthy();
  });

  it('edit message event trigger', () => {
    cmp.vm.editMessage({ detail: {
      msgId: MSG_ID
    }});
    expect(cmp.emitted('edit-message').length).toBe(1);
  });

  it('delete message event trigger', () => {
    let deleteMessageEventCalled = false;
    document.addEventListener(chatConstants.ACTION_MESSAGE_DELETE, () => {deleteMessageEventCalled = true});
    cmp.vm.deleteMessage({ detail: {
      msgId: MSG_ID
    }});
    expect(deleteMessageEventCalled).toBeTruthy();
  });

  it('save notes', () => {
    let saveNotesEventCalled = false;
    document.addEventListener(chatConstants.ACTION_MESSAGE_SEND, () => {saveNotesEventCalled = true});
    cmp.vm.saveNotes({ detail: {
      msgId: MSG_ID
    }});
    expect(saveNotesEventCalled).toBeTruthy();
  });

  it('send meeting notes', () => {
    let sendMeetingNotesCalled = false;
    global.fetch = jest.fn().mockImplementation((url) => {
      sendMeetingNotesCalled = url && url.indexOf('sendMeetingNotes') >= 0;
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

    cmp.vm.sendMeetingNotesAnimationDone();
    expect(sendMeetingNotesCalled).toBeTruthy();
  });
  
  it('save meeting notes', () => {
    let saveMeetingNotesCalled = false;
    global.fetch = jest.fn().mockImplementation((url) => {
      saveMeetingNotesCalled = url && url.indexOf('getMeetingNotes') >= 0;
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

    cmp.vm.saveMeetingNotesAnimationDone();
    expect(saveMeetingNotesCalled).toBeTruthy();
  });

  it('file message', () => {
    const fileMessage = getMessage(DEFAULT_MSG, true, {
      type: chatConstants.FILE_MESSAGE,
      title: 'File Title',
      restPath: '#',
      sizeLabel: '5 MB',
      thumbnailURL: '#',
      downloadLink: '#'
    });
    expect(fileMessage.findAll('.attachmentContainer')).toHaveLength(1);
  });

  it('Add Team member message', () => {
    const messageDetail = getMessage(DEFAULT_MSG, true, {
      type: chatConstants.ADD_TEAM_MESSAGE,
      fullname: 'User full name',
      users: 'Added User'
    });
    expect(messageDetail.findAll('.message-content')).toHaveLength(1);
    expect(messageDetail.find('.message-content').html().length).toBeGreaterThan(0);
    expect(messageDetail.find('.message-content').html()).toContain('User full name');
    expect(messageDetail.find('.message-content').html()).toContain('Added User');
  });

  it('Deleted message', () => {
    const messageDetail = getMessage(null, true, {
      type: chatConstants.DELETED_MESSAGE
    });
    expect(messageDetail.findAll('.message-content')).toHaveLength(1);
    expect(messageDetail.find('.message-content').html().length).toBeGreaterThan(0);
    expect(messageDetail.find('.message-content').html()).toContain('exoplatform.chat.deleted');
  });

  it('leave room message', () => {
    const messageDetail = getMessage(null, true, {
      type: chatConstants.ROOM_MEMBER_LEFT,
      fullName : 'user left'
    });
    expect(messageDetail.findAll('.message-content')).toHaveLength(1);
    expect(messageDetail.find('.message-content').html().length).toBeGreaterThan(0);
    expect(messageDetail.find('.message-content').html()).toContain('user left');
  });

  it('Remove Team member message', () => {
    const messageDetail = getMessage(DEFAULT_MSG, true, {
      type: chatConstants.REMOVE_TEAM_MESSAGE,
      fullname: 'User full name',
      users: 'Added User'
    });
    expect(messageDetail.findAll('.message-content')).toHaveLength(1);
    expect(messageDetail.find('.message-content').html().length).toBeGreaterThan(0);
    expect(messageDetail.find('.message-content').html()).toContain('User full name');
    expect(messageDetail.find('.message-content').html()).toContain('Added User');
  });

  it('Add calendar event message', () => {
    const messageDetail = getMessage(DEFAULT_MSG, true, {
      type: chatConstants.EVENT_MESSAGE,
      summary: 'User full name',
      startDate: '09/12/2000',
      startAllDay: true,
      startTime: '00:00',
      endDate: '10/12/2000',
      endAllDay: false,
      endTime: '12:00',
      location: 'event location'
    });
    expect(messageDetail.findAll('.message-content')).toHaveLength(1);
    expect(messageDetail.find('.message-content').html().length).toBeGreaterThan(0);
    expect(messageDetail.find('.message-content').html()).toContain('09/12/2000');
    expect(messageDetail.find('.message-content').html()).toContain('10/12/2000');
    expect(messageDetail.find('.message-content').html()).not.toContain('00:00');
    expect(messageDetail.find('.message-content').html()).toContain('12:00');
    expect(messageDetail.find('.message-content').html()).toContain('exoplatform.chat.all.day');
    expect(messageDetail.find('.message-content').html()).toContain('event location');
  });

  it('Link message', () => {
    const messageDetail = getMessage(DEFAULT_MSG, true, {
      type: chatConstants.LINK_MESSAGE,
      link: '#link'
    });
    expect(messageDetail.findAll('.message-content')).toHaveLength(1);
    expect(messageDetail.find('.message-content').html().length).toBeGreaterThan(0);
    expect(messageDetail.find('.message-content').html()).toContain('#link');
  });

  it('Raise hand message', () => {
    const messageDetail = getMessage(DEFAULT_MSG, true, {
      type: chatConstants.RAISE_HAND
    });
    expect(messageDetail.findAll('.message-content')).toHaveLength(1);
    expect(messageDetail.find('.message-content').html().length).toBeGreaterThan(0);
    expect(messageDetail.find('.message-content').html()).toContain(DEFAULT_MSG);
  });

  it('Question message', () => {
    const messageDetail = getMessage(DEFAULT_MSG, true, {
      type: chatConstants.QUESTION_MESSAGE
    });
    expect(messageDetail.findAll('.message-content')).toHaveLength(1);
    expect(messageDetail.find('.message-content').html().length).toBeGreaterThan(0);
    expect(messageDetail.find('.message-content').html()).toContain(DEFAULT_MSG);
  });

  it('Meeting start message', () => {
    const messageDetail = getMessage(DEFAULT_MSG, true, {
      type: chatConstants.MEETING_START_MESSAGE
    });
    expect(messageDetail.findAll('.message-content')).toHaveLength(1);
    expect(messageDetail.find('.message-content').html().length).toBeGreaterThan(0);
    expect(messageDetail.find('.message-content').html()).toContain('exoplatform.chat.meeting.started');
  });

  it('Meeting stop message', () => {
    const messageDetail = getMessage(DEFAULT_MSG, true, {
      type: chatConstants.MEETING_STOP_MESSAGE
    });
    expect(messageDetail.findAll('.message-content')).toHaveLength(1);
    expect(messageDetail.find('.message-content').html().length).toBeGreaterThan(0);
    expect(messageDetail.find('.message-content').html()).toContain('exoplatform.chat.notes');
  });

  it('notes message', () => {
    const messageDetail = getMessage(DEFAULT_MSG, true, {
      type: chatConstants.NOTES_MESSAGE
    });
    expect(messageDetail.findAll('.message-content')).toHaveLength(1);
    expect(messageDetail.find('.message-content').html().length).toBeGreaterThan(0);
    expect(messageDetail.find('.message-content').html()).toContain('exoplatform.chat.notes');
  });

});
