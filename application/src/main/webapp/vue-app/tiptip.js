export function initTiptip() {
  eXo = eXo ? eXo : {};
  eXo.social = eXo.social ? eXo.social : {};
  eXo.social.tiptip = eXo.social.tiptip ? eXo.social.tiptip : {};
  eXo.social.tiptip.extraActions = eXo.social.tiptip.extraActions ? eXo.social.tiptip.extraActions : [];
  eXo.social.tiptip.extraActions.push({
    appendContentTo(divUIAction, ownerId, type) {
      if(!type) {
        type = 'username';
      }
      divUIAction.append(`<a title="Chat"  \
        class="btn chatPopupOverlay chatPopup" \
        type="button" \
        onclick="document.dispatchEvent(new CustomEvent('exo-chat-room-open', {detail: {name: '${ownerId}', type: '${type}'}}))"> \
          <i class="uiIconBannerChat uiIconLightGray"></i> \
          Chat \
      </a>`);
    }
  });
  if ($('.spaceMenuNav .spaceMenuNavHeader .spaceMenuApps').length && eXo && eXo.env && eXo.env.portal && eXo.env.portal.spaceName) {
    $('.spaceMenuNav .spaceMenuNavHeader .spaceMenuApps').append(`<li>
        <a onclick="document.dispatchEvent(new CustomEvent('exo-chat-room-open', {detail: {name: '${eXo.env.portal.spaceName}', type: 'space-name'}}))" \
        class="chat-button btn" href="javascript:void(0);"> \
          <i class="uiIconBannerChat"></i> \
          <span class="chat-label-status">&nbsp;Chat</span> \
        </a> \
      </li>`);
  } else if ($('.profileMenuNav .profileMenuNavHeader .profileMenuApps').length && eXo && eXo.env && eXo.env.portal && eXo.env.portal.profileOwner) {
    $('.profileMenuNav .profileMenuNavHeader .profileMenuApps').append(`<li>
        <a onclick="document.dispatchEvent(new CustomEvent('exo-chat-room-open', {detail: {name: '${eXo.env.portal.profileOwner}', type: 'username'}}))" \
        class="chat-button btn" href="javascript:void(0);"> \
          <i class="uiIconBannerChat"></i> \
          <span class="chat-label-status">&nbsp;Chat</span> \
        </a> \
      </li>`);
  }
}
