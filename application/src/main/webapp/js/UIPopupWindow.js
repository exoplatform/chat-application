/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/**
 * A class that manages a popup window
 */
(function($, base, common, uiMaskLayer, uiPopup) {
  var uiChatPopupWindow = {
    superClass : uiPopup,

    // TODO: manage zIndex properties
    /**
     * Shows the popup window passed in parameter gets the highest z-index
     * property of the elements in the page : . gets the z-index of the maskLayer .
     * gets all the other popup windows . gets the highest z-index from these, if
     * it's still at 0, set an arbitrary value of 2000 sets the position of the
     * popup on the page (top and left properties)
     */
    show : function(popupId, isShowMask, middleBrowser, height) {
      var popup = document.getElementById(popupId);
      if (popup == null) return;

      var resizeBtn = $(popup).find(".ResizeButton, .uiIconResize").last();
      if (resizeBtn.length) {
        resizeBtn.show()
        resizeBtn.off().on("mousedown", this.startResizeEvt);
      }

      if (isShowMask)
        uiChatPopupWindow.showMask(popup, true);
      popup.style.visibility = "hidden";

      this.setupWindow(popup, middleBrowser, height);

      $(".popupHeader > .uiIconClose", popup).click(function(){
        uiChatPopupWindow.hide(popupId, isShowMask)
      });
    },

    setupWindow : function(popup, middleBrowser, height) {
      var jPopup = $(popup);
      var vrez = uiChatPopupWindow.getResizeBlock(jPopup);
      if (height) {
        vrez.css("max-height", height);
      }
      var browserHeight = $(window).height();
      if (browserHeight < jPopup[0].offsetHeight) {
        vrez.css("max-height", "");
        var vrezHeight = browserHeight - jPopup[0].offsetHeight + vrez.height() - 20;
        vrez.height(vrezHeight >= 10 ? vrezHeight : 10);
      }

      var scrollY = 0, offsetParent = popup.offsetParent;
      if (window.pageYOffset != undefined)
        scrollY = window.pageYOffset;
      else if (document.documentElement && document.documentElement.scrollTop)
        scrollY = document.documentElement.scrollTop;
      else
        scrollY = document.body.scrollTop;
      // reference
      if (offsetParent) {
        var middleWindow = $(offsetParent).is(".UIPopupWindow,.UIWindow");
        if (middleWindow) {
          popup.style.top = Math.ceil((offsetParent.offsetHeight - popup.offsetHeight) / 2) + "px";
        }
        if (middleBrowser || !middleWindow) {
          popup.style.top = Math.ceil((browserHeight - popup.offsetHeight) / 2) + scrollY + "px";
        }
        // Todo: set popup of UIPopup always display in the center browsers in case UIMaskWorkspace
        if ($(offsetParent).hasClass("UIMaskWorkspace")) {
          popup.style.top = Math.ceil((offsetParent.offsetHeight - popup.offsetHeight) / 2) + "px";
        }

        // hack for position popup alway top in IE6.
        var checkHeight = popup.offsetHeight > 300;

        if (document.getElementById("UIDockBar") && checkHeight) {
          popup.style.top = "6px";
        }
        popup.style.left = Math.ceil((offsetParent.offsetWidth - popup.offsetWidth) / 2) + "px";
      }
      if ($(popup).offset().top < 0)
        popup.style.top = scrollY + "px";

      popup.style.visibility = "visible";
    },

    hide : function(popupId, isShowMask) {
      var popup = document.getElementById(popupId);
      if (popup == null) return;
      this.superClass.hide(popup);
      if (isShowMask) uiChatPopupWindow.showMask(popup, false);
    },

    showMask : function(popup, isShowPopup) {
      var mask = popup.previousSibling;
      // Make sure mask is not TextNode because of previousSibling property
      if (mask && mask.className != "MaskLayer") {
        mask = null;
      }
      if (isShowPopup) {
        // Modal if popup is portal component
        if ($(popup).parents(".PORTLET-FRAGMENT").length < 1){
          if (!mask)
            uiMaskLayer.createMask(popup.parentNode, popup, 1);
        } else {
          // If popup is portlet's component, modal with just its parent
          if (!mask)
            uiMaskLayer.createMaskForFrame(popup.parentNode, popup, 1);
        }
      } else {
        if (mask)
          uiMaskLayer.removeMask(mask);
      }
    },

    /**
     * Called when the window starts being resized sets the onmousemove and
     * onmouseup events on the portal application (not the popup) associates these
     * events with UIPopupWindow.resize and UIPopupWindow.endResizeEvt
     * respectively
     */
    startResizeEvt : function(evt) {
      //disable select text
      uiChatPopupWindow.backupEvent = null;
      if (navigator.userAgent.indexOf("MSIE") >= 0) {
        //Need to check if we have remove resizedPopup after last mouseUp
        //IE bug: not call endResizeEvt when mouse moved out of page
        if (!uiChatPopupWindow.resizedPopup && document.onselectstart) {
          uiChatPopupWindow.backupEvent = document.onselectstart;
        }
        document.onselectstart = function() {return false};
      } else {
        if (document.onmousedown) {
          uiChatPopupWindow.backupEvent = document.onmousedown;
        }
        document.onmousedown = function() {return false};
      }

      var targetPopup = $(this).parents(".UIPopupWindow, .uiPopup")[0];
      uiChatPopupWindow.resizedPopup = targetPopup;
      uiChatPopupWindow.vresized = uiChatPopupWindow.getResizeBlock($(targetPopup));
      uiChatPopupWindow.backupPointerY = base.Browser.findMouseRelativeY(targetPopup, evt) ;

      var jDoc = $(document);
      jDoc.on("mousemove.UIPopupWindow", uiChatPopupWindow.resize);
      jDoc.on("mouseup.UIPopupWindow", uiChatPopupWindow.endResizeEvt);
    },

    /**
     * Function called when the window is being resized . gets the position of the
     * mouse . calculates the height and the width of the window from this
     * position . sets these values to the window
     */
    resize : function(evt) {
      var targetPopup = uiChatPopupWindow.resizedPopup ;
      var content = uiChatPopupWindow.vresized;
      var isRTL = eXo.core.I18n.isRT();
      var pointerX = base.Browser.findMouseRelativeX(targetPopup, evt, isRTL);
      var pointerY = base.Browser.findMouseRelativeY(targetPopup, evt);
      var delta = pointerY - uiChatPopupWindow.backupPointerY;

      var height = 0;
      content.each(function() {
        if ($(this).height()) {
          height = $(this).height();
          return;
        }
      });
      if (height + delta > 0) {
        uiChatPopupWindow.backupPointerY = pointerY;
        content.height(height + delta);
        content.css("max-height", "");
      }
      targetPopup.style.height = "auto";

      if (isRTL) {
        pointerX = (-1) * pointerX;
      }

      if (pointerX > 230)
        targetPopup.style.width = (pointerX + 10) + "px";
    },

    /**
     * Called when the window stops being resized cancels the mouse events on the
     * portal app inits the scroll managers active on this page (in case there is
     * one in the popup)
     */
    endResizeEvt : function(evt) {
      uiChatPopupWindow.resizedPopup = null;
      uiChatPopupWindow.vresized = null;
      $(document).off("mousemove.UIPopupWindow").off("mouseup.UIPopupWindow");

      //enable select text
      if (navigator.userAgent.indexOf("MSIE") >= 0) {
        document.onselectstart = uiChatPopupWindow.backupEvent;
      } else {
        document.onmousedown = uiChatPopupWindow.backupEvent;
      }
      uiChatPopupWindow.backupEvent = null;
    },


    getResizeBlock : function(jPopup) {
      var filterPopup = function() {
        return $(this).closest(".uiPopup").attr("id") === jPopup.attr("id");
      };

      jPopup.find(".resizable .resizable").filter(filterPopup).removeClass("resizable");
      var innerRez = jPopup.find(".resizable").filter(filterPopup);
      var contentBlock = jPopup.find("div.PopupContent, .popupContent").filter(filterPopup);

      var vrez;
      if (innerRez.length) {
        vrez = innerRez;
      } else {
        vrez = contentBlock;
      }
      return vrez;
    }
  };
  return uiChatPopupWindow;
})($, base, common, uiMaskLayer, uiPopup);