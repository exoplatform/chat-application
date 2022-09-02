import {chatConstants} from './chatConstants.js';
import {initTiptip} from './tiptip.js';

export const ECMS_EVENT_COMPOSER_APP = [{
  key: 'file',
  rank: 30,
  type: 'type-file',
  nameKey: 'exoplatform.chat.file',
  labelKey: 'exoplatform.chat.upload.file',
  iconClass: 'uiIconChatUpload',
  hideModalActions: true,
  contact: null,
  i18NConverter: null,
  appClass: 'chat-file-upload DropZone',
  init(contact) {
    this.contact = contact;
  },
  html(i18NConverter) {
    this.i18NConverter = i18NConverter;
    return `<div class="progressBar"> \
              <div class="progress"> \
                <div class="bar" style="width: 0.0%;"></div> \
                <div class="label"> \
                  <div class="label-inner">${i18NConverter('exoplatform.chat.file.drop')}</div> \
                </div> \
              </div> \
            </div> \
            <div class="uiActionBorder"> \
              <a href="#" class="btn btn-primary chat-file-upload" type="button"> \
                <span>${i18NConverter('exoplatform.chat.file.manually')}</span> \
                <input id="chat-file-file" type="file" name="userfile" /> \
              </a> \
              <input id="chat-file-submit" value="${i18NConverter('exoplatform.chat.file.manually')}" type="submit" style="display:none" /> \
              <a href="#" type="button" class="btn btnClosePopup" onclick="document.dispatchEvent(new CustomEvent('${chatConstants.ACTION_APPS_CLOSE}'))">${i18NConverter('exoplatform.chat.cancel')}</a> \
            </div>`;
  },
  htmlAdded($) {
    this.initUpload($);
  },
  showButtons($, show) {
    if (show) {
      $('.apps-composer-modal .chat-file-upload .uiActionBorder').show();
    } else {
      $('.apps-composer-modal .chat-file-upload .uiActionBorder').hide();
    }
  },
  setErrorCode($, error, errorOpts) {
    const $alertContainer = $('.apps-composer-modal .alert-error');
    if (error && error.length) {
      $alertContainer.html(this.i18NConverter(`exoplatform.chat.${error}`, errorOpts));
      $alertContainer.show();
    } else {
      $alertContainer.hide();
      $alertContainer.html('');
    }
  },
  initUpload($) {
    const MAX_RANDOM_NUMBER = 100000;
    const uploadId = Math.round(Math.random() * MAX_RANDOM_NUMBER);
    const $dropzoneContainer = $('#appComposerForm .DropZone');
    const thiss = this;

    $dropzoneContainer.filedrop({
      fallback_id: 'chat-file-file',  // an identifier of a standard file input element
      url: `${chatConstants.UPLOAD_API}?uploadId=${uploadId}&action=upload`,  // upload handler, handles each file separately, can also be a function taking the file and returning a url
      paramname: 'userfile',          // POST parameter name used on serverside to reference file
      error: function (err) {
        switch (err) {
        case 'ErrorBrowserNotSupported':
        case 'BrowserNotSupported':
          thiss.setErrorCode($, 'BrowserNotSupported');
          break;
        case 'ErrorTooManyFiles':
        case 'TooManyFiles':
          thiss.setErrorCode($, 'TooManyFiles');
          break;
        case 'ErrorFileTooLarge':
        case 'FileTooLarge':
          thiss.setErrorCode($, 'upload.filesize', {0: eXo.chat.userSettings.maxUploadSize});
          break;
        case 'ErrorFileTypeNotAllowed':
        case 'FileTypeNotAllowed':
          thiss.setErrorCode($, 'FileTypeNotAllowed');
          break;
        }
        thiss.showButtons($, true);
      },
      allowedfiletypes: [],   // filetypes allowed by Content-Type.  Empty array means no restrictions
      maxfiles: chatConstants.MAX_UPLOAD_FILES,
      maxfilesize: eXo.chat.userSettings.maxUploadSize,    // max file size in MBs
      uploadStarted: function() {
        thiss.setErrorCode($, '');
        thiss.showButtons($, false);
      },
      progressUpdated: function (i, file, progress) {
        $dropzoneContainer.find('.bar').width(`${progress}%`);
        $dropzoneContainer.find('.bar').html(`${progress}%`);
      },
      uploadFinished: function () {
        fetch(`${chatConstants.UPLOAD_API}?uploadId=${uploadId}&action=progress`, {
          method: 'post',
          credentials: 'include'
        }).then(resp =>  resp.text()).then(data => {
          data = data.replace(' upload :', ' "upload" :');
          data = JSON.parse(data);
          const UPLOAD_PERCENT_COMPLETE = 100;
          data = data && data.upload && data.upload[uploadId] ? data.upload[uploadId] : null;
          if (!data || !data.percent || data.percent !== UPLOAD_PERCENT_COMPLETE && data.percent !== '100') {
            thiss.setErrorCode($, 'ErrorFileUploadNotComplete');
            return;
          }
          fetch(chatConstants.FILE_PERSIST_URL, {
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            },
            method: 'post',
            credentials: 'include',
            body: $.param({
              uploadId: uploadId,
              token: eXo.chat.userSettings.token,
              targetRoom: thiss.contact.user,
              targetFullname: thiss.contact.fullName
            })
          }).then(resp =>  {
            if (!resp.ok) {
              thiss.setErrorCode($, 'ErrorPersistFile');
              return;
            } else {
              return resp.json();
            }
          }).then(options => {
            if (!options) {
              thiss.setErrorCode($, 'UknownError');
              return;
            }
            options.type = 'type-file';
            const message = {
              msg: options.name,
              room: thiss.contact.room,
              clientId: new Date().getTime().toString(),
              user: eXo.chat.userSettings.username,
              options: options
            };
            document.dispatchEvent(new CustomEvent(chatConstants.ACTION_MESSAGE_SEND, {'detail': message}));
            document.dispatchEvent(new CustomEvent(chatConstants.ACTION_APPS_CLOSE));
          });
        });
      }
    });
  }
}];
export const DEFAULT_COMPOSER_APPS = [
  {
    key: 'link',
    rank: 20,
    type: 'type-link',
    nameKey: 'exoplatform.chat.link',
    labelKey: 'exoplatform.chat.share.link',
    iconClass: 'uiIconChatLink',
    saveLabelKey: 'exoplatform.chat.share',
    html() {
      return '<input id="link" name="link" class="large" type="text" placeholder="E.g: http://www.exoplatform.com" required>';
    },
    checkURL(text) {
      // if user has not entered http:// https:// or ftp:// assume they mean http://
      if (!/^(https?|ftp):\/\//i.test(text)) {
        text = `http://${text}`; // set both the value
      }
      const pattern = new RegExp('^((https?:)?\\/\\/)?'+ // protocol
        '(?:\\S+(?::\\S*)?@)?' + // authentication
        '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|'+ // domain name
        '((\\d{1,3}\\.){3}\\d{1,3}))'+ // OR ip (v4) address
        '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*'+ // port and path
        '(\\?[;&a-z\\d%_.~+=-]*)?'+ // query string
        '(\\#[-a-z\\d_]*)?$','i'); // fragment locater
      if (!pattern.test(text)) {
        return false;
      } 
      return text;
    },
    submit(chatServices, message, formData) {
      const checkURL = this.checkURL(formData['link']);
      if (checkURL) {
        message.options.link = checkURL;
        return {ok: true};
      }
      else {
        return {errorCode: 'link.invalid.message'};
      }
    }
  },
  {
    key: 'question',
    rank: 40,
    type: 'type-question',
    nameKey: 'exoplatform.chat.question',
    labelKey: 'exoplatform.chat.ask.question',
    iconClass: 'uiIconChatQuestion',
    saveLabelKey: 'exoplatform.chat.ask',
    html(i18NConverter) {
      return `<input name="msg" placeholder="${i18NConverter('exoplatform.chat.question.what')}" class="large" type="text" required>`;
    },
    submit(chatServices, message, formData) {
      message.msg = formData['msg'];
      return {ok: true};
    }
  },
  {
    key: 'raise-hand',
    rank: 50,
    type: 'type-hand',
    labelKey: 'exoplatform.chat.raise.hand',
    iconClass: 'uiIconChatRaiseHand',
    saveLabelKey: 'exoplatform.chat.raise.your',
    html(i18NConverter) {
      return `<input name="msg" placeholder="${i18NConverter('exoplatform.chat.optional.comment')}" class="large" type="text">`;
    },
    submit(chatServices, message, formData) {
      message.msg = formData['msg'];
      return {ok: true};
    }
  }
];


export const DEFAULT_ROOM_ACTIONS = [{
  key: 'notificationSettings',
  rank: 30,
  labelKey: 'exoplatform.stats.notifications',
  class: 'uiIconPLFNotifications'
} , {
  key: 'addToFavorite',
  rank: 40,
  labelKey: 'exoplatform.chat.add.favorites',
  class: 'uiIconStar',
  enabled: (comp) => {
    return !comp.contact.isFavorite;
  }
} , {
  key: 'removeFromFavorite',
  rank: 50,
  labelKey: 'exoplatform.chat.remove.favorites',
  class: 'uiIconStar',
  enabled: (comp) => {
    return comp.contact.isFavorite;
  }
} , {
  key: 'showParticipants',
  rank: 60,
  labelKey: 'exoplatform.chat.participants',
  class: 'uiIconViewList',
  enabled: () => {
    return true;
  }
} , {
  key: 'editRoom',
  rank: 70,
  labelKey: 'exoplatform.chat.team.edit',
  type: 't',
  class: 'uiIconEditInfo',
  enabled: (comp) => {
    return comp.isAdmin;
  }
} , {
  key: 'deleteRoom',
  rank: 80,
  labelKey: 'exoplatform.chat.team.delete',
  type: 't',
  class: 'uiIconDelete',
  confirm: {
    title: 'exoplatform.chat.team.delete.title',
    message: 'exoplatform.chat.team.delete.message',
    okMessage: 'exoplatform.chat.team.delete.ok',
    koMessage: 'exoplatform.chat.team.delete.ko',
    confirmed(contact) {
      document.dispatchEvent(new CustomEvent(chatConstants.ACTION_ROOM_DELETE, {'detail': contact}));
    }
  },
  enabled: (comp) => {
    return comp.isAdmin;
  }
} , {
  key: 'leaveRoom',
  rank: 90,
  labelKey: 'exoplatform.chat.team.leave',
  type: 't',
  class: 'uiIconExit',
  enabled: (comp) => {
    return !comp.isAdmin;
  },
  confirm: {
    title: 'exoplatform.chat.team.leave',
    message: 'exoplatform.chat.team.leave.message',
    okMessage: 'exoplatform.chat.team.leave.ok',
    koMessage: 'exoplatform.chat.team.leave.ko',
    confirmed(contact) {
      document.dispatchEvent(new CustomEvent(chatConstants.ACTION_ROOM_LEAVE, {'detail': contact}));
    }
  }
}];

export const EMOTICONS = [
  {
    keys: [':)', ':-)'],
    class: 'emoticon-smile'
  },
  { 
    keys: [':(', ':-('],
    class: 'emoticon-sad'
  },
  { 
    keys: [';)', ';-)'],
    class: 'emoticon-wink'
  },
  { 
    keys: [':|', ':-|'],
    class: 'emoticon-speechless'
  },
  { 
    keys: [':o', ':-o'],
    class: 'emoticon-surprise'
  },
  { 
    keys: [':p', ':-p'],
    class: 'emoticon-smile-tongue'
  },
  { 
    keys: [':d', ':-d'],
    class: 'emoticon-flaugh'
  },
  { 
    keys: ['(cool)'],
    class: 'emoticon-cool'
  },
  { 
    keys: [';(', ':\'('],
    class: 'emoticon-crying'
  },
  { 
    keys: ['(beer)'],
    class: 'emoticon-beer'
  },
  { 
    keys: ['(bow)'],
    class: 'emoticon-bow'
  },
  { 
    keys: ['(bug)'],
    class: 'emoticon-bug'
  },
  { 
    keys: ['(cake)', '(^)'],
    class: 'emoticon-cake'
  },
  { 
    keys: ['(cash)'],
    class: 'emoticon-cash'
  },
  { 
    keys: ['(coffee)'],
    class: 'emoticon-coffee'
  },
  { 
    keys: ['(star)'],
    class: 'emoticon-star'
  },
  { 
    keys: ['(heart)', '&lt;3'],
    class: 'emoticon-heart'
  },
  { 
    keys: ['(y)', '(yes)'],
    class: 'emoticon-raise-up'
  },
  { 
    keys: ['(n)', '(no)'],
    class: 'emoticon-raise-down'
  },
  { 
    keys: ['(devil)'],
    class: 'emoticon-devil'
  }
];


export const DEFAULT_MESSAGE_ACTIONS = [
  {
    key: 'edit',
    rank: 10,
    labelKey: 'exoplatform.chat.msg.edit',
    enabled: comp => {
      const editable = !comp.message.isDeleted && !comp.message.notSent && comp.isCurrentUser;
      if (comp.message.options) {
        return (
          !(comp.message.options.type ==='type-file') && editable
        );
      }
      return editable && !comp.message.isSystem;
    }
  },
  {
    key: 'delete',
    rank: 20,
    labelKey: 'exoplatform.chat.delete',
    enabled: comp => {
      return  !comp.message.isDeleted && !comp.message.notSent && comp.isCurrentUser && !comp.message.isSystem;
    },
    confirm: {
      title: 'exoplatform.chat.popup.delete.title',
      message: 'exoplatform.chat.popup.delete.message',
      okMessage: 'exoplatform.chat.user.popup.confirm',
      koMessage: 'exoplatform.chat.cancel',
      confirmed(message) {
        document.dispatchEvent(new CustomEvent(chatConstants.EVENT_ROOM_MEMBER_LEFT, {'detail': message}));
      }
    }
  },
  {
    key: 'quote',
    rank: 30,
    labelKey: 'exoplatform.chat.quote',
    enabled: comp => {
      const quote =  !comp.message.isDeleted && !comp.message.notSent;
      if (comp.message.options) {
        return (
          !(comp.message.options.type ==='type-file') && quote
        );
      }
      return quote && !comp.message.isSystem ;
    }
  }
];

export function registerExternalExtensions(chatTitle) {
  const profileExtensionAction = {
    id: 'profile-chat',
    title: chatTitle,
    icon: 'uiIconBannerChat',
    class: 'fas fa-comments',
    additionalClass: 'mt-1',
    order: 10,
    enabled: () => true,
    click: (profile) => {
      const chatType = profile.groupId ? 'space-id' : 'username';
      const chatRoomName = profile.prettyName ? profile.id : profile.username;

      document.dispatchEvent(
        new CustomEvent(chatConstants.ACTION_ROOM_OPEN_CHAT, { detail: {
          name: chatRoomName,
          type: chatType,
        }}));
    },
  };
  
  if (extensionRegistry) {
    extensionRegistry.registerExtension('profile-extension', 'action', profileExtensionAction);
  }

  document.dispatchEvent(new CustomEvent('profile-extension-updated', { detail: profileExtensionAction}));

  extensionRegistry.registerComponent('SpacePopover', 'space-popover-action', {
    id: 'chat',
    vueComponent: Vue.options.components['popover-chat-button'],
    rank: 40,
  });

  extensionRegistry.registerComponent('UserPopover', 'user-popover-action', {
    id: 'chat',
    vueComponent: Vue.options.components['popover-chat-button'],
    rank: 40,
  });
}

export function registerDefaultExtensions(extensionType, defaultExtensions) {
  for (const extension of defaultExtensions) {
    if (extensionRegistry) {
      extensionRegistry.registerExtension('chat', extensionType, extension);
    }
  }
}

function getExtensionsByType(type) {
  return extensionRegistry ? extensionRegistry.loadExtensions('chat', type) : [];
}

initTiptip();

registerDefaultExtensions('composer-application', DEFAULT_COMPOSER_APPS);
registerDefaultExtensions('message-action', DEFAULT_MESSAGE_ACTIONS);
registerDefaultExtensions('room-action', DEFAULT_ROOM_ACTIONS);

export const extraMessageTypes = getExtensionsByType('message-type');
export const extraMessageNotifs = getExtensionsByType('message-notif');
export let roomActions = getExtensionsByType('room-action');
export const roomActionComponents = getExtensionsByType('room-action-component');
export const miniChatTitleActionComponents = getExtensionsByType('mini-chat-title-action-component');
export let composerApplications = getExtensionsByType('composer-application');
export let messageActions = getExtensionsByType('message-action');

let additionalExtensionsInstalled = false;
export function installExtensions(settings) {
  if (!settings || !settings.fullName) {
    return;
  }

  if (additionalExtensionsInstalled) {
    return;
  }
  additionalExtensionsInstalled = true;

  if (settings.canUploadFiles) {
    registerDefaultExtensions('composer-application', ECMS_EVENT_COMPOSER_APP);
  }

  composerApplications = getExtensionsByType('composer-application');
  messageActions = getExtensionsByType('message-action');
  roomActions = getExtensionsByType('room-action');
}
