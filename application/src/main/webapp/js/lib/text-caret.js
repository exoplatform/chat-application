export function addCaretJQueryExtension($) {
  $.fn.extend({
    insertAtCaret(newValue) {
      this.each(( i, element ) => {
        element.focus();
        if (newValue === '\n' && window.getSelection()){
          const selection = window.getSelection(),
            range = selection.getRangeAt(0),
            br = document.createElement('br'),
            textNode = document.createTextNode('\u00a0');
          range.deleteContents();
          range.insertNode(br);
          range.collapse(false);
          range.insertNode(textNode);
          range.selectNodeContents(textNode);
          selection.removeAllRanges();
          selection.addRange(range);
          const d = $('#messageComposerArea');
          d.scrollTop(d.prop('scrollHeight'));
        }
        else if (document.getSelection) {
          const selection = document.getSelection();
          const range = selection.getRangeAt(0);

          const el = document.createElement('div');
          el.innerHTML = newValue;
          const frag = document.createDocumentFragment();
          let node;
          let lastNode;
          /* eslint-disable no-extra-parens */
          while ((node = el.firstChild)) {
            lastNode = frag.appendChild(node);
          }
          range.insertNode(frag);
          if (lastNode) {
            range.setStartAfter(lastNode);
            range.collapse(false);
          }
        }  else {
          element.innerHTML += newValue;
          element.focus();
        }
      });
    },
    serializeFormJSON() {
      const o = {};
      const a = this.serializeArray();
      $.each(a, function () {
        if (o[this.name]) {
          if (!o[this.name].push) {
            o[this.name] = [o[this.name]];
          }
          o[this.name].push(this.value || '');
        } else {
          o[this.name] = this.value || '';
        }
      });
      return o;
    }
  });
}