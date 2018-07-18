export function addCaretJQueryExtension($) {
  $.fn.extend({
    insertAtCaret(newValue) {
      this.each(( i, element ) => {
        if (document.selection) {
          element.focus();
          const sel = document.selection.createRange();
          sel.text = newValue;
          element.focus();
        } else if (element.selectionStart || element.selectionStart === '0' || element.selectionStart === 0) {
          const startPos = element.selectionStart;
          const endPos = element.selectionEnd;
          const scrollTop = element.scrollTop;
          element.value = element.value.substring(0, startPos) + newValue + element.value.substring(endPos,element.value.length);
          element.focus();
          element.selectionStart = startPos + newValue.length;
          element.selectionEnd = startPos + newValue.length;
          element.scrollTop = scrollTop;
        } else {
          element.value += newValue;
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