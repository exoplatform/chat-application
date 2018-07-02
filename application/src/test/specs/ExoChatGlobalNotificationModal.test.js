import { mount } from 'vue-test-utils';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';

import ExoChatGlobalNotificationModal from '../../main/webapp/vue-app/components/modal/ExoChatGlobalNotificationModal';

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
  const comp = mount(ExoChatGlobalNotificationModal, {
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

describe('ExoChatGlobalNotificationModal.test.js', () => {

  it('test ExoChatGlobalNotificationModal DOM', () => {
    const cmp = getComponent(true);
    expect(cmp.html().length).toBeGreaterThan(0);

    expect(cmp.findAll('.notification-item')).toHaveLength(4);
    expect(cmp.findAll('.notification-item').at(0).html()).toContain('exoplatform.chat.desktopNotif.global.donotdist');
    expect(cmp.findAll('.notification-item').at(1).html()).toContain('exoplatform.chat.desktopNotif.global.desktop');
    expect(cmp.findAll('.notification-item').at(2).html()).toContain('exoplatform.chat.desktopNotif.global.onsite');
    expect(cmp.findAll('.notification-item').at(3).html()).toContain('exoplatform.chat.desktopNotif.global.beep');

    expect(cmp.find('#notifyDonotdistrub').element.checked).toBe(cmp.vm.chatPreferences.notifyDonotdistrub);
    expect(cmp.find('#notifyDesktop').element.checked).toBe(cmp.vm.chatPreferences.notifyDesktop);
    expect(cmp.find('#notifyOnSite').element.checked).toBe(cmp.vm.chatPreferences.notifyOnSite);
    expect(cmp.find('#notifyBip').element.checked).toBe(cmp.vm.chatPreferences.notifyBip);

    expect(cmp.vm.chatPreferences.notifyDonotdistrub).toBeFalsy();
    expect(cmp.vm.chatPreferences.notifyDesktop).toBeTruthy();
    expect(cmp.vm.chatPreferences.notifyOnSite).toBeTruthy();
    expect(cmp.vm.chatPreferences.notifyBip).toBeTruthy();

    expect(cmp.vm.originalChatPreferences.notifyDonotdistrub).toBeFalsy();
    expect(cmp.vm.originalChatPreferences.notifyDesktop).toBeTruthy();
    expect(cmp.vm.originalChatPreferences.notifyOnSite).toBeTruthy();
    expect(cmp.vm.originalChatPreferences.notifyBip).toBeTruthy();
  });

  it('test ExoChatGlobalNotificationModal DOM with different settings', () => {
    const cmp = getComponent(false);
    eXo.chat.desktopNotificationSettings = {
      preferredNotificationTrigger: [chatConstants.NOT_DISTRUB_NOTIF],
      preferredNotification: [
        chatConstants.ON_SITE_NOTIF,
        chatConstants.DESKTOP_NOTIF,
        chatConstants.BIP_NOTIF
      ]
    };

    cmp.vm.show = false;
    cmp.update();
    cmp.vm.show = true;
    cmp.update();

    expect(cmp.vm.chatPreferences.notifyDonotdistrub).toBeTruthy();
    expect(cmp.vm.chatPreferences.notifyDesktop).toBeTruthy();
    expect(cmp.vm.chatPreferences.notifyOnSite).toBeTruthy();
    expect(cmp.vm.chatPreferences.notifyBip).toBeTruthy();

    expect(cmp.vm.originalChatPreferences.notifyDonotdistrub).toBeTruthy();
    expect(cmp.vm.originalChatPreferences.notifyDesktop).toBeTruthy();
    expect(cmp.vm.originalChatPreferences.notifyOnSite).toBeTruthy();
    expect(cmp.vm.originalChatPreferences.notifyBip).toBeTruthy();

    eXo.chat.desktopNotificationSettings = {
      preferredNotificationTrigger: [chatConstants.NOT_DISTRUB_NOTIF],
      preferredNotification: [
        chatConstants.ON_SITE_NOTIF,
        chatConstants.BIP_NOTIF
      ]
    };
    cmp.vm.show = false;
    cmp.update();
    cmp.vm.show = true;
    cmp.update();

    expect(cmp.vm.chatPreferences.notifyDonotdistrub).toBeTruthy();
    expect(cmp.vm.chatPreferences.notifyDesktop).toBeFalsy();
    expect(cmp.vm.chatPreferences.notifyOnSite).toBeTruthy();
    expect(cmp.vm.chatPreferences.notifyBip).toBeTruthy();

    expect(cmp.vm.originalChatPreferences.notifyDonotdistrub).toBeTruthy();
    expect(cmp.vm.originalChatPreferences.notifyDesktop).toBeFalsy();
    expect(cmp.vm.originalChatPreferences.notifyOnSite).toBeTruthy();
    expect(cmp.vm.originalChatPreferences.notifyBip).toBeTruthy();

    eXo.chat.desktopNotificationSettings = {
      preferredNotificationTrigger: [chatConstants.NOT_DISTRUB_NOTIF],
      preferredNotification: [
        chatConstants.BIP_NOTIF
      ]
    };
    cmp.vm.show = false;
    cmp.update();
    cmp.vm.show = true;
    cmp.update();

    expect(cmp.vm.chatPreferences.notifyDonotdistrub).toBeTruthy();
    expect(cmp.vm.chatPreferences.notifyDesktop).toBeFalsy();
    expect(cmp.vm.chatPreferences.notifyOnSite).toBeFalsy();
    expect(cmp.vm.chatPreferences.notifyBip).toBeTruthy();

    expect(cmp.vm.originalChatPreferences.notifyDonotdistrub).toBeTruthy();
    expect(cmp.vm.originalChatPreferences.notifyDesktop).toBeFalsy();
    expect(cmp.vm.originalChatPreferences.notifyOnSite).toBeFalsy();
    expect(cmp.vm.originalChatPreferences.notifyBip).toBeTruthy();


    eXo.chat.desktopNotificationSettings = {
      preferredNotificationTrigger: [chatConstants.NOT_DISTRUB_NOTIF],
      preferredNotification: []
    };
    cmp.vm.show = false;
    cmp.update();
    cmp.vm.show = true;
    cmp.update();

    expect(cmp.vm.chatPreferences.notifyDonotdistrub).toBeTruthy();
    expect(cmp.vm.chatPreferences.notifyDesktop).toBeFalsy();
    expect(cmp.vm.chatPreferences.notifyOnSite).toBeFalsy();
    expect(cmp.vm.chatPreferences.notifyBip).toBeFalsy();

    expect(cmp.vm.originalChatPreferences.notifyDonotdistrub).toBeTruthy();
    expect(cmp.vm.originalChatPreferences.notifyDesktop).toBeFalsy();
    expect(cmp.vm.originalChatPreferences.notifyOnSite).toBeFalsy();
    expect(cmp.vm.originalChatPreferences.notifyBip).toBeFalsy();

    eXo.chat.desktopNotificationSettings = {
      preferredNotificationTrigger: [],
      preferredNotification: []
    };
    cmp.vm.show = false;
    cmp.update();
    cmp.vm.show = true;
    cmp.update();

    expect(cmp.vm.chatPreferences.notifyDonotdistrub).toBeFalsy();
    expect(cmp.vm.chatPreferences.notifyDesktop).toBeFalsy();
    expect(cmp.vm.chatPreferences.notifyOnSite).toBeFalsy();
    expect(cmp.vm.chatPreferences.notifyBip).toBeFalsy();

    expect(cmp.vm.originalChatPreferences.notifyDonotdistrub).toBeFalsy();
    expect(cmp.vm.originalChatPreferences.notifyDesktop).toBeFalsy();
    expect(cmp.vm.originalChatPreferences.notifyOnSite).toBeFalsy();
    expect(cmp.vm.originalChatPreferences.notifyBip).toBeFalsy();

    expect(cmp.vm.checkboxesEnhanced).toBeTruthy();
  });

  it('test ExoChatGlobalNotificationModal save', () => {
    const cmp = getComponent(true);

    let notifsMannerSaved = false;
    let notifyDesktopSaved = false;
    let notifyOnSiteSaved = false;
    let notifyBipSaved = false;
    global.fetch = jest.fn().mockImplementation((url) => {
      if(url.indexOf(`notifManners=${chatConstants.NOT_DISTRUB_NOTIF}`) >= 0) {
        notifsMannerSaved = true;
      }
      if(url.indexOf(`notifConditions=${chatConstants.ON_SITE_NOTIF}`) >= 0) {
        notifyOnSiteSaved = true;
      }
      if(url.indexOf(`notifConditions=${chatConstants.DESKTOP_NOTIF}`) >= 0) {
        notifyDesktopSaved = true;
      }
      if(url.indexOf(`notifConditions=${chatConstants.BIP_NOTIF}`) >= 0) {
        notifyBipSaved = true;
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
    cmp.find('#notifyDonotdistrub').element.checked = true;
    cmp.vm.saveNotificationSettings();
    expect(cmp.emitted('close-modal')).toHaveLength(1);

    expect(notifsMannerSaved).toBeTruthy();
    expect(notifyDesktopSaved).toBeFalsy();
    expect(notifyOnSiteSaved).toBeFalsy();
    expect(notifyBipSaved).toBeFalsy();

    cmp.find('#notifyDesktop').element.checked = true;
    cmp.vm.saveNotificationSettings();
    expect(notifsMannerSaved).toBeTruthy();
    expect(notifyDesktopSaved).toBeTruthy();
    expect(notifyOnSiteSaved).toBeFalsy();
    expect(notifyBipSaved).toBeFalsy();

    cmp.find('#notifyOnSite').element.checked = true;
    cmp.vm.saveNotificationSettings();
    expect(notifsMannerSaved).toBeTruthy();
    expect(notifyDesktopSaved).toBeTruthy();
    expect(notifyOnSiteSaved).toBeTruthy();
    expect(notifyBipSaved).toBeFalsy();

    cmp.find('#notifyBip').element.checked = true;
    cmp.vm.saveNotificationSettings();
    expect(notifsMannerSaved).toBeTruthy();
    expect(notifyDesktopSaved).toBeTruthy();
    expect(notifyOnSiteSaved).toBeTruthy();
    expect(notifyBipSaved).toBeTruthy();
  });

  it('test ExoChatGlobalNotificationModal close', () => {
    const cmp = getComponent(true);
    cmp.findAll('.btn').at(1).trigger('click');
    expect(cmp.emitted('close-modal')).toHaveLength(1);
  });
});