export function initTiptip() {
  eXo = eXo ? eXo : {};
  eXo.social = eXo.social ? eXo.social : {};
  eXo.social.tiptip = eXo.social.tiptip ? eXo.social.tiptip : {};
  eXo.social.tiptip.extraActions = eXo.social.tiptip.extraActions ? eXo.social.tiptip.extraActions : [];
  eXo.social.tiptip.extraActions.push({
    appendContentTo(divUIAction, ownerUserId) {
      divUIAction.append(`<a title="Chat"  \
        class="btn chatPopupOverlay chatPopup" \
        type="button" \
        onclick="document.dispatchEvent(new CustomEvent('exo-chat-room-open', {detail: '${ownerUserId}'}))"> \
          <i class="uiIconBannerChat uiIconLightGray"></i> \
          Chat \
      </a>`);
    }
  });
}
