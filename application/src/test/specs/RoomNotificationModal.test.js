import { mount } from 'vue-test-utils';
import RoomNotificationModal from '../../main/webapp/vue-app/components/modal/RoomNotificationModal';
import Modal from '../../main/webapp/vue-app/components/modal/Modal';
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

function getComponent(show) {
  const comp = mount(RoomNotificationModal, {
    propsData: {
      room: '851548984ea54546546ae54651e564e5a58',
      roomName: 'Test Room Name'
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

describe('RoomNotificationModal.test.js', () => {

  it('test RoomNotificationModal DOM and attributes', () => {
    const cmp = getComponent(true);
    expect(cmp.vm.show).toBeTruthy();
    expect(cmp.vm.room).toBe('851548984ea54546546ae54651e564e5a58');
    expect(cmp.vm.roomName).toBe('Test Room Name');
    expect(cmp.html().length).toBeGreaterThan(0);
    expect(cmp.findAll('.uiRadio')).toHaveLength(3);
    expect(cmp.findAll('.notif-description')).toHaveLength(3);

    expect(cmp.vm.selectedOption).toBe('normal');
    expect(cmp.vm.keywords).toBe('');
    expect(cmp.vm.disableAdvancedFilter).toBeTruthy();
    expect(cmp.vm.title).toContain('exoplatform.chat.team.notifications');

    cmp.setData({selectedOption : 'keywords'});
    cmp.update();
    expect(cmp.vm.disableAdvancedFilter).toBeFalsy();
  });

  it('test RoomNotificationModal save', () => {
    const cmp = getComponent(true);
    let notifTypeSaved = false;
    let notifKeywordSaved = false;
    global.fetch = jest.fn().mockImplementation((url) => {
      if(url.indexOf(`notifConditionType=keywords`) >= 0) {
        notifTypeSaved = true;
      }
      if(url.indexOf(`notifCondition=TestKeyword`) >= 0) {
        notifKeywordSaved = true;
      }
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
    cmp.setData({selectedOption : 'keywords'});
    cmp.setData({keywords : 'TestKeyword'});
    cmp.update();

    cmp.vm.saveSettings();
    expect(cmp.emitted('modal-closed').length).toBe(1);
    expect(notifTypeSaved).toBeTruthy();
    expect(notifKeywordSaved).toBeTruthy();
  });

  it('test RoomNotificationModal close', () => {
    const cmp = getComponent(true);
    cmp.findAll('.btn').at(1).trigger('click');
    expect(cmp.emitted('modal-closed').length).toBe(1);
  });
});