 /*
  * TipTip
  * Copyright 2010 Drew Wilson
  * www.drewwilson.com
  * code.drewwilson.com/entry/tiptip-jquery-plugin
  *
  * Version 1.3   -   Updated: Mar. 23, 2010
  *
  * This Plug-In will create a custom tooltip to replace the default
  * browser tooltip. It is extremely lightweight and very smart in
  * that it detects the edges of the browser window and will make sure
  * the tooltip stays within the current window size. As a result the
  * tooltip will adjust itself to be displayed above, below, to the left 
  * or to the right depending on what is necessary to stay within the
  * browser window. It is completely customizable as well via CSS.
  *
  * This TipTip jQuery plug-in is dual licensed under the MIT and GPL licenses:
  *   http://www.opensource.org/licenses/mit-license.php
  *   http://www.gnu.org/licenses/gpl.html
  */

 /**
  * Component displaying a popup with the user profile info and some actions.
  * This component is a duplication for the Social user popup component because
  * it was not possible to re-use it (mainly because of jquery integration issues).
  */
 (function ($) {
     $.fn.userPopup = function (options) {
         var defaults = {
             restURL: "",
             userId: "",
             labels: "",
             getContentFunc: function() {},
             activation: "hover",
             keepAlive: false,
             maxWidth: "200px",
             edgeOffset: 3,
             defaultPosition: "bottom",
             delay: 400,
             fadeIn: 200,
             fadeOut: 200,
             attribute: "title",
             content: false,
             enter: function () {},
             exit: function () {}
         };
         var opts = $.extend(defaults, options);
         if ($("#tiptip_holder").length <= 0) {
             var tiptip_holder = $('<div id="tiptip_holder" style="max-width:' + opts.maxWidth + ';"></div>');
             var tiptip_content = $('<div id="tiptip_content"></div>');
             var tiptip_arrow = $('<div id="tiptip_arrow"></div>');
             $("body").append(tiptip_holder.html(tiptip_content).prepend(tiptip_arrow.html('<div id="tiptip_arrow_inner"></div>')))
         } else {
             var tiptip_holder = $("#tiptip_holder");
             var tiptip_content = $("#tiptip_content");
             var tiptip_arrow = $("#tiptip_arrow")
         }
         return this.each(function () {
             var org_elem = $(this);
             
             if (opts.content) {
                 var org_title = opts.content
             } else {
                 var org_title = org_elem.attr(opts.attribute)
             }
             if (org_title != "") {
                 if (!opts.content) {
                     org_elem.removeAttr(opts.attribute)
                 }
                 var timeout = false;
                 if (opts.activation == "hover") {
                     org_elem.hover(function () {
                     
                         //
                         loadData($(this));
                         
                         clearTimeout($(this).data('timeoutId'));
                         
                         active_tiptip()
                     }, function () {
                         if (!opts.keepAlive) {
                             deactive_tiptip()
                         }
                         //
                         var $this = $(this);
                         var timeoutId = setTimeout(function(){
	                          if(!tiptip_holder.is(':hover')) {
	                            deactive_tiptip();
	                          }
	                       }, 250);
	                       $this.data('timeoutId', timeoutId); 
                     });
                     if (opts.keepAlive) {
                         tiptip_holder.hover(function () {}, function () {
                             deactive_tiptip()
                         })
                     }
                 } else if (opts.activation == "focus") {
                     org_elem.focus(function () {
                         active_tiptip()
                     }).blur(function () {
                         deactive_tiptip()
                     })
                 } else if (opts.activation == "click") {
                     org_elem.click(function () {
                         active_tiptip();
                         return false
                     }).hover(function () {}, function () {
                         if (!opts.keepAlive) {
                             deactive_tiptip()
                         }
                     });
                     if (opts.keepAlive) {
                         tiptip_holder.hover(function () {}, function () {
                             deactive_tiptip()
                         })
                     }
                 }
                 function active_tiptip() {
                     opts.enter.call(this);
                     //tiptip_content.html(org_title);
                     tiptip_holder.hide().removeAttr("class").css("margin", "0");
                     tiptip_arrow.removeAttr("style");
                     var top = parseInt(org_elem.offset()['top']);
                     var left = parseInt(org_elem.offset()['left']);
                     var org_width = parseInt(org_elem.outerWidth());
                     var org_height = parseInt(org_elem.outerHeight());
                     var tip_w = tiptip_holder.outerWidth();
                     var tip_h = tiptip_holder.outerHeight();
                     var w_compare = Math.round((org_width - tip_w) / 2);
                     var h_compare = Math.round((org_height - tip_h) / 2);
                     var marg_left = Math.round(left + w_compare);
                     var marg_top = Math.round(top + org_height + opts.edgeOffset);
                     var t_class = "";
                     var arrow_top = "";
                     var arrow_left = Math.round(tip_w - 12) / 2;
                     if (opts.defaultPosition == "bottom") {
                         t_class = "_bottom"
                     } else if (opts.defaultPosition == "top") {
                         t_class = "_top"
                     } else if (opts.defaultPosition == "left") {
                         t_class = "_left"
                     } else if (opts.defaultPosition == "right") {
                         t_class = "_right"
                     }
                     var right_compare = (w_compare + left) < parseInt($(window).scrollLeft());
                     var left_compare = (tip_w + left) > parseInt($(window).width());
                     if ((right_compare && w_compare < 0) || (t_class == "_right" && !left_compare)) {
                         t_class = "_right";
                         arrow_top = Math.round(tip_h - 13) / 2;
                         arrow_left = -12;
                         marg_left = Math.round(left + org_width + opts.edgeOffset);
                         marg_top = Math.round(top + h_compare)
                     } else if ((left_compare && w_compare < 0) || (t_class == "_left" && !right_compare)) {
                         t_class = "_left";
                         arrow_top = Math.round(tip_h - 13) / 2;
                         arrow_left = Math.round(tip_w);
                         marg_left = Math.round(left - (tip_w + opts.edgeOffset + 5));
                         marg_top = Math.round(top + h_compare)
                     }
                     var top_compare = (top + org_height + opts.edgeOffset + tip_h + 8) > parseInt($(window).height() + $(window).scrollTop());
                     var bottom_compare = ((top + org_height) - (opts.edgeOffset + tip_h + 8)) < 0;
                     if (top_compare || (t_class == "_bottom" && top_compare) || (t_class == "_top" && !bottom_compare)) {
                         if (t_class == "_top" || t_class == "_bottom") {
                             t_class = "_top"
                         } else {
                             t_class = t_class + "_top"
                         }
                         arrow_top = tip_h;
                         marg_top = Math.round(top - (tip_h + 5 + opts.edgeOffset))
                     } else if (bottom_compare | (t_class == "_top" && bottom_compare) || (t_class == "_bottom" && !top_compare)) {
                         if (t_class == "_top" || t_class == "_bottom") {
                             t_class = "_bottom"
                         } else {
                             t_class = t_class + "_bottom"
                         }
                         arrow_top = -12;
                         marg_top = Math.round(top + org_height + opts.edgeOffset)
                     }
                     if (t_class == "_right_top" || t_class == "_left_top") {
                         marg_top = marg_top + 5
                     } else if (t_class == "_right_bottom" || t_class == "_left_bottom") {
                         marg_top = marg_top - 5
                     }
                     if (t_class == "_left_top" || t_class == "_left_bottom") {
                         marg_left = marg_left + 5
                     }
                     tiptip_arrow.css({
                         "margin-left": arrow_left + "px",
                         "margin-top": arrow_top + "px"
                     });
                     tiptip_holder.css({
                         "margin-left": marg_left + "px",
                         "margin-top": marg_top + "px"
                     }).attr("class", "tip" + t_class);
                     if (timeout) {
                         clearTimeout(timeout)
                     }
                     timeout = setTimeout(function () {
                         tiptip_holder.stop(true, true).fadeIn(opts.fadeIn)
                     }, opts.delay)
                 }
                 function deactive_tiptip() {
                     opts.exit.call(this);
                     if (timeout) {
                         clearTimeout(timeout)
                     }
                     tiptip_holder.fadeOut(opts.fadeOut)
                 }
                 
                 function loadData(el) {
                   var userId = opts.userId;
                   if(!userId) {
                     var userUrl = $(el).attr('href');
                     if(!userUrl) {
                       // no way to get the user id, abort
                       return;
                     }
                     userId = userUrl.substring(userUrl.lastIndexOf('/') + 1);
                   }
                   var restUrl = opts.restURL.replace('{0}', window.encodeURI(userId));
                   
                   //
                   initPopup();
                   
                   //
                   var cachingData = getCache(userId);
                   
                   if ( cachingData ) {
                     buildPopup(cachingData, userId);
                   } else {
                     if (window.profileXHR && window.profileXHR.abort) {
                       window.profileXHR.abort();
                     }
		                 window.profileXHR = $.ajax({
		                     type: "GET",
		                     cache: false,
		                     url: restUrl
		                 }).complete(function (jqXHR) {
		                     if (jqXHR.readyState === 4) {
		                       var userData = $.parseJSON(jqXHR.responseText);
		                       
		                       if (!userData) {
		                         return;
		                       }
		                       
		                       //
		                       putToCache(userId, userData);
		                       
		                       buildPopup(userData, userId);
		                     }
		                 });
		               }
                 }
                 
                 function initPopup() {
                   var profile_popup = $('<div/>', {
                     "id": "profile-popup",
                     "class": "profile-popup",
                     "height": "100px"
                   });
                   
                   var loadingIndicator = $('<div/>', {
                     "id": "loading-indicator"
                   });
                   var loadingText = $('<div/>', {
                     "id": "loading-text",
                     "text": "" + opts.labels.StatusTitle
                   });
                   
                   $('#tiptip_content').find('div.loading-indicator').remove();
                   for (var i=1; i < 9; i++) {
                     loadingIndicator.append($('<div id="rotateG_0' + i + '" class="blockG"></div>'));
                   }
                   
                   profile_popup.append(loadingIndicator);
                   profile_popup.append(loadingText);
                   
                   tiptip_content.html(profile_popup);
                 }
                 
							   function buildPopup(json, ownerUserId) {
							        var portal = eXo.env.portal;
							        var relationStatus = json.relationshipType;
							        var currentViewerId = portal.userName;
							        var action = null;
							        var labels = opts.labels;
							        var isDeleted = json.deleted;
							        
							        tiptip_content.empty();
							        
							        if (currentViewerId != ownerUserId && !isDeleted) {
							    
							            action = $('<div/>', {
							                "class": "connect btn btn-primary",
							                "text": "" + labels.Connect,
							                "data-action": "Invite:" + ownerUserId,
							                "onclick": "takeAction(this)"
							            });
							    
							            //
							            if (relationStatus == "pending") { // Viewing is not owner
							                action = $('<div/>', {
							                    "class": "connect btn btn-primary",
							                    "text": "" + labels.Confirm,
							                    "data-action": "Accept:" + ownerUserId,
							                    "onclick": "takeAction(this)"
							                });
							            } else if (relationStatus == "waiting") { // Viewing is owner
							                action = $('<div/>', {
							                    "class": "connect btn",
							                    "text": "" + labels.CancelRequest,
                                                "data-action":"Revoke:" + ownerUserId,
                                                "onclick":"takeAction(this)"
							                });
							            } else if (relationStatus == "confirmed") { // Had Connection 
							                action = $('<div/>', {
				                                "class":"connect btn",
				                                "text":"" + labels.RemoveConnection,
				                                "data-action":"Disconnect:" + ownerUserId,
				                                "onclick":"takeAction(this)"
			                                });
							            } else if (relationStatus == "ignored") { // Connection is removed
							                action = $('<div/>', {
				                                "class":"connect btn",
				                                "text":"" + labels.Connect,
				                                "data-action":"Invite:" + ownerUserId,
				                                "onclick":"takeAction(this)"
                                      });
                        }

                    }

                    var popupContentContainer = $("<div/>");
                    var popupContent = $("<table/>", {
                        "id":"tipName"
                    });
                    var tbody = $("<tbody/>");
                    var tr = $("<tr/>");
                    var tdAvatar = $("<td/>", {
                        "width":"50px"
                    });
                    var img = $("<img/>", {
                        "src":json.avatarURL
                    });

                    var aAvatar = $("<a/>", {
                        "target":"_self",
                        "href":json.profileUrl
                    });

                    tdAvatar.append(aAvatar.append(img));

                    var tdProfile = $("<td/>");
                    var aProfile = $("<a/>", {
                        "target":"_self",
                        "href":json.profileUrl,
                        "text":json.fullName
                    });

                    tdProfile.append(aProfile);

                    if (json.position) {
                        var divPosition = $("<div/>", {
                            "font-weight":"normal",
                            "text":json.position
                        });
                        tdProfile.append(divPosition);
                    }

                    tr.append(tdAvatar).append(tdProfile);

                    tbody.append(tr);

                    popupContent.append(tbody);

                    if (json.activityTitle) {
                        var blockquote = $("<blockquote/>", {
                          "text" :json.activityTitle.replace(/<[^>]+>/g, '')
                        });
                    }

                    popupContentContainer.append(popupContent);

                    if (blockquote) {
                        popupContentContainer.append(blockquote);
                    }

                    if (currentViewerId != ownerUserId && !isDeleted) {
                        var divUIAction = $("<div/>", {
                            "class":"uiAction connectAction"
                        }).append(action);
                    }

                    if (divUIAction) {
                        popupContentContainer.append(divUIAction);
                    }

                    tiptip_content.html(popupContentContainer.html());
                }

                function takeAction(el) {
                    var userList = org_elem.parents('div.userList:first');

                    var thisTip = $(el).parents('div#tiptip_content:first');
                    var tipName = thisTip.find('table#tipName:first');
                    var userURL = tipName.find('a:first').attr('href').replace('activities', 'profile');

                    var focusedUserLink = userList.find('a[href$="' + userURL + '"]:first');
                    var focusedUserBlock = focusedUserLink.parents('div.spaceBox:first');
                    
                    if (focusedUserBlock.length > 0) {
                        var actionBtn = $(focusedUserBlock).find('div.connectionBtn');
                        
                        // invoke onclick()
                        var btn = actionBtn.find('button.btn-confirm:first');
                        if(btn.length === 0) {
                            actionBtn.find('button.btn:first').trigger('click');
                        } else {
                            btn.trigger('click');
                        }
                        
                        // clear cache and hide popup
                        var popup = $(el).closest('#tiptip_holder');
                        popup.fadeOut('fast');
                        // clear cache
                        clearCache();
                        return;
                    }

                    var dataAction = $(el).attr('data-action');
                    var updatedType = dataAction.split(":")[0];
                    var ownerUserId = dataAction.split(":")[1];

                    if (window.profileActionXHR && window.profileActionXHR.abort) {
                        window.profileActionXHR.abort();
                    }
                    window.profileActionXHR = $.ajax({
                        type: "GET",
                        cache: false,
                        url: opts.restURL.replace('{0}', ownerUserId) + '?updatedType=' + updatedType
                    }).complete(function (jqXHR) {
                        if (jqXHR.readyState === 4) {
                            var popup = $(el).closest('#tiptip_holder');
                            popup.fadeOut('fast', function () {
                            });
                            if(updatedType === "Disconnect" && $(org_elem).data('link')) {
                                var actionLink = $(org_elem).data('link').replace('javascript:', '');
                                $.globalEval(actionLink);
                            }
                            // clear cache
                            clearCache();
                        }
                    });
                }

                function putToCache(key, data) {
                    var ojCache = $('div#socialUsersData');
                    if (ojCache.length == 0) {
                        ojCache = $('<div id="socialUsersData"></div>').appendTo($(document.body));
                        ojCache.hide();
                    }
                    key = 'result' + ((key === ' ') ? '_20' : key);
                    var datas = ojCache.data("CacheSearch");
                    if (String(datas) === "undefined") datas = {};
                    datas[key] = data;
                    ojCache.data("CacheSearch", datas);
                }

                function getCache(key) {
                    key = 'result' + ((key === ' ') ? '_20' : key);
                    var datas = $('div#socialUsersData').data("CacheSearch");
                    return (String(datas) === "undefined") ? null : datas[key];
                }

                function clearCache() {
                    $('div#socialUsersData').stop().animate({
                        'cursor':'none'
                    }, 1000, function () {
                        $(this).data("CacheSearch", {});
                    });
                }
                window.takeAction = takeAction;
            }
        })
    }
})(jqchat);