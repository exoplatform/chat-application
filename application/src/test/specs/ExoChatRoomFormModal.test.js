import { mount } from 'vue-test-utils';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';

import ExoChatRoomFormModal from '../../main/webapp/vue-app/components/modal/ExoChatRoomFormModal';
import ExoModal from '../../main/webapp/vue-app/components/modal/ExoModal';

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

function getComponent(show, selected) {
  const comp = mount(ExoChatRoomFormModal, {
    propsData: {
      show: show,
      selected: selected
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

describe('ExoChatRoomFormModal.test.js', () => {

  it('test ExoChatRoomFormModal DOM and attributes', () => {
    let cmp = getComponent(true, {
      'unreadTotal':0,
      'isActive':'true',
      'type':'u',
      'user':'testuser',
      'status':'invisible',
      'timestamp':1528455913624,
      'isFavorite':false
    });
    expect(cmp.html().length).toBeGreaterThan(0);
    expect(cmp.findAll('.add-room-form')).toHaveLength(1);
    expect(cmp.findAll('.room-suggest-list')).toHaveLength(1);
    expect(cmp.findAll('.uiMention')).toHaveLength(0);
    expect(cmp.vm.title).toContain('exoplatform.chat.team.add.title');
    expect(cmp.vm.otherParticiants).toHaveLength(0);
    expect(cmp.vm.disableSave).toBeTruthy();

    cmp = getComponent(true, {
      'fullName':'Test User',
      'unreadTotal':0,
      'isActive':'true',
      'type':'u',
      'user':'testuser',
      'room':'eb74205830cf97546269bbdc5d439b29ddd1735b',
      'status':'invisible',
      'timestamp':1528455913624,
      'isFavorite':false
    });
    expect(cmp.vm.title).toContain('exoplatform.chat.team.edit');
    expect(cmp.vm.fullName).toBe('Test User');

    expect(cmp.findAll(ExoModal)).toHaveLength(2);
    expect(cmp.find(ExoModal).element.style.display).toBe('');
    cmp.vm.show = false;
    cmp.update();
    expect(cmp.find(ExoModal).element.style.display).toBe('none');
  });

  it('test ExoChatRoomFormModal participants', () => {
    const cmp = getComponent(true, {
      fullName:'Test User',
      unreadTotal:0,
      isActive:'true',
      type:'u',
      user:'testuser',
      room:'eb74205830cf97546269bbdc5d439b29ddd1735b',
      status:'invisible',
      timestamp:1528455913624,
      isFavorite:false
    });
    cmp.setData({
      participants: [{
        name: 'root',
        fullname: 'Root Root'
      }, {
        name: 'testuser1',
        fullname: 'Test user 1'
      }, {
        name: 'testuser2',
        fullname: 'Test user 2'
      }]});
    expect(cmp.vm.otherParticiants).toHaveLength(2);
    expect(cmp.findAll('.uiMention')).toHaveLength(2);
    expect(cmp.vm.disableSave).toBeFalsy();

    cmp.vm.initSuggester();
    cmp.vm.addSuggestedItem({
      name: 'testuser3',
      fullname: 'Test user 3'
    });
    cmp.update();
    expect(cmp.vm.otherParticiants).toHaveLength(3);
    expect(cmp.findAll('.uiMention')).toHaveLength(3);

    cmp.vm.removeSuggest({
      name: 'testuser3',
      fullname: 'Test user 3'
    });
    cmp.update();
    expect(cmp.vm.otherParticiants).toHaveLength(2);
    expect(cmp.findAll('.uiMention')).toHaveLength(2);
  });

  it('test ExoChatRoomFormModal save', () => {
    const cmp = getComponent(true, {
      fullName:'Test User',
      unreadTotal:0,
      isActive:'true',
      type:'u',
      user:'testuser',
      room:'eb74205830cf97546269bbdc5d439b29ddd1735b',
      status:'invisible',
      timestamp:1528455913624,
      isFavorite:false
    });
    cmp.setData({
      participants: [{
        name: 'root',
        fullname: 'Root Root'
      }, {
        name: 'testuser1',
        fullname: 'Test user 1'
      }, {
        name: 'testuser2',
        fullname: 'Test user 2'
      }]});
    expect(cmp.vm.otherParticiants).toHaveLength(2);
    expect(cmp.findAll('.uiMention')).toHaveLength(2);
    expect(cmp.vm.disableSave).toBeFalsy();

    let roomSaved = false;
    global.fetch = jest.fn().mockImplementation((url, data) => {
      if(url && url.indexOf('saveTeamRoom') >= 0
          && data && data.body
          && data.body.indexOf('testuser1') >= 0
          && data.body.indexOf('testuser2') >= 0
          && data.body.indexOf('root') >= 0
          && data.body.indexOf('Test User') >= 0) {
        roomSaved = true;
      }
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
    cmp.find('.btn-primary').trigger('click');
    expect(roomSaved).toBeTruthy();
  });

  it('test ExoChatRoomFormModal close', () => {
    const cmp = getComponent(true, {});
    cmp.findAll('.btn').at(1).trigger('click');
    expect(cmp.emitted('modal-closed')).toHaveLength(1);
  });

  it('test ExoChatRoomFormModal findUser', () => {
    const cmp = getComponent(true, {});
    let searchUserRequestCalled = false;
    global.fetch = jest.fn().mockImplementation((url) => {
      if(url && url.indexOf('tessst') >= 0) {
        searchUserRequestCalled = true;
      }
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
    cmp.vm.findUsers('tessst');
    expect(searchUserRequestCalled).toBeTruthy();
  });

  it('test ExoChatRoomFormModal renderMenuItem', () => {
    const cmp = getComponent(true, {});
    const menuItemHTML = cmp.vm.renderMenuItem({
      name: 'testuser',
      fullname: 'Test Name'
    }, data => data);
    expect(menuItemHTML).toContain('testuser');
    expect(menuItemHTML).toContain('Test Name');
  });

});