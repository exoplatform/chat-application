import {chatConstants} from './chatConstants.js';

export function initTiptip() {
  eXo = eXo ? eXo : {};
  eXo.social = eXo.social ? eXo.social : {};
  eXo.social.tiptip = eXo.social.tiptip ? eXo.social.tiptip : {};
  eXo.social.tiptip.extraActions = eXo.social.tiptip.extraActions ? eXo.social.tiptip.extraActions : [];
  if ($('.spaceMenuNav .spaceMenuNavHeader .spaceMenuApps').length && eXo && eXo.env && eXo.env.portal && eXo.env.portal.spaceGroup) {
    $('.spaceMenuNav .spaceMenuNavHeader .spaceMenuApps').append(`<li>
        <a onclick="document.dispatchEvent(new CustomEvent('${chatConstants.ACTION_ROOM_OPEN_CHAT}', {detail: {name: '${eXo.env.portal.spaceGroup}', type: 'space-name'}}))" \
        class="chat-button btn" href="javascript:void(0);"> \
          <i class="uiIconBannerChat"></i> \
          <span class="chat-label-status">&nbsp;Chat</span> \
        </a> \
      </li>`);
  } else if ($('.profileMenuNav .profileMenuNavHeader .profileMenuApps').length && eXo && eXo.env && eXo.env.portal && eXo.env.portal.profileOwner && eXo.env.portal.profileOwner !== eXo.env.portal.userName) {
    $('.profileMenuNav .profileMenuNavHeader .profileMenuApps').append(`<li>
        <a onclick="document.dispatchEvent(new CustomEvent('${chatConstants.ACTION_ROOM_OPEN_CHAT}', {detail: {name: '${eXo.env.portal.profileOwner}', type: 'username'}}))" \
        class="chat-button btn" href="javascript:void(0);"> \
          <i class="uiIconBannerChat"></i> \
          <span class="chat-label-status">&nbsp;Chat</span> \
        </a> \
      </li>`);
  }
}
