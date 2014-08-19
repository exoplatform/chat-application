var bootbox = window.bootbox || (function(document, $) {

  var that = {};

  that.alertError = function(message, callback) {
    return that.dialog(message, 1, callback);
  };

  that.dialog = function(str, type, callback) {
    // Select message type
    var messageTypeText = "";
    var messageTypeIcon = "";

    switch(type) {
      case 1: // Error
        messageTypeText = "Error";
        messageTypeIcon = "errorIcon";
        break;
      case 2: // Warning
      case 3: // Info
    }

    // Build modal html
    var parts = ["<div id='UIErrorMessageChat' class='UIPopupWindow uiPopup modal' tabindex='-1' style='overflow:hidden;width: 550px; visibility: visible; position: relative; display: block;'>"];
    parts.push("    <div class='popupHeader ClearFix'>");
    parts.push("      <a title='Close Window' class='uiIconClose pull-right' data-dismiss='modal'></a><span class='PopupTitle popupTitle'>" + messageTypeText + "</span>");
    parts.push("    </div>");
    parts.push("    <div class='PopupContent popupContent'>");
    parts.push("      <ul class='singleMessage popupMessage resizable'>");
    parts.push("        <li><span class='" + messageTypeIcon + "'> " + str + "</span></li>");
    parts.push("      </ul>");
    parts.push("      <div class='uiAction uiActionBorder'><a class='btn btn-primary' data-dismiss='modal'>OK</a></div>");
    parts.push("    </div>");
    parts.push("</div>");

    // Event handlers
    var div = $(parts.join("\n"));

    div.on('shown', function() {
      div.find("a.btn-primary:first").focus();
    });

    div.on('hidden', function(e) {
      if (e.target === this) {
        div.remove();
      }
    });


    div.on('click', '.uiAction a', function(e) {
      e.preventDefault();

      if (typeof callback === 'function') {
        callback(e);
      }
    });

    // Append to body to show message
    $("body").append(div);

    //
    div.modal({
      backdrop: "static",
      keyboard: false,
      show: false
    });

    div.on("show", function(e) {
      $(document).off("focusin.modal");
    });

    // Show dialog to center
    var centerTop = ($(window).height() - $("#UIErrorMessageChat").height()) / 2;
    centerTop = centerTop >= 0 ? centerTop : $("#UIErrorMessageChat").offset().top;
    var centerLeft = ($(window).width() - $("#UIErrorMessageChat").width()) / 2;
    centerLeft = centerLeft >= 0 ? centerLeft : $("#UIErrorMessageChat").offset().left;

    div.modal("show");
    $("#UIErrorMessageChat").offset({top: centerTop, left: centerLeft})

    return div;
  };

  return that;

}(document, window.jQuery));

window.bootbox = bootbox;