/**
 * Copyright (C) 2014 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
(function ($, base, common, msg) {
    uiMiniCalendar = {
        calendarId: "UICalendarControl",
        dateField: null,
        datePattern: null,
        value: null,
        currentDate: null, // Datetime value base of selectedDate for displaying
        // calendar below
        // if selectedDate is invalid, currentDate deals with system time;
        selectedDate: null, // Datetime value of input date&time field
        months: null,

        firstDayOfWeek: 2, // Indecates what the first day of the week is; e.g.,
        // SUNDAY (1) in the U.S, MONDAY (2) in France, TUESDAY
        // (3), etc
        weekdays: null,

        hideCalendarHandler: null,

        init: function (field, isDisplayTime, datePattern, value, monthNames) {
            this.isDisplayTime = isDisplayTime;

            if (this.dateField) {
                this.dateField.parentNode.style.position = '';
            }
            this.dateField = field;
            this.datePattern = datePattern;
            this.value = value;

            this.months = new Array();
            this.months = monthNames.split(',');
            this.months.pop();

            var weekdays = chatBundleData.exoplatform_chat_weekdays.split(',');
            if (weekdays != null && typeof (weekdays) == "object") {
                this.weekdays = weekdays;
            }

            if (!document.getElementById(this.calendarId))
                this.create();
            this.show();
        },

        create: function () {
            var clndr = document.createElement("div");
            clndr.id = this.calendarId;
            clndr.style.position = "absolute";
            clndr.style.zIndex = "99";
            if (base.Browser.isIE6()) {
                clndr.innerHTML = "<div class='calendarComponent uiCalendarComponent' ><iframe id='" + this.calendarId + "IFrame' frameBorder='0' style='position:absolute;height:100%;' scrolling='no'></iframe><div style='position:absolute;'></div></div>";
            } else {
                clndr.innerHTML = "<div class='calendarComponent uiCalendarComponent' ><div style='position: absolute; width: 100%;'></div></div>";
            }
            document.body.appendChild(clndr);
        },

        show: function () {
            //document.onclick = function() {uiMiniCalendar.hide()};
            if (!this.hideCalendarHandler) {
                this.hideCalendarHandler = function () {
                    uiMiniCalendar.hide();
                };
            }
            if (document.addEventListener) {
                document.addEventListener("click", this.hideCalendarHandler, false);
            } else if (document.attachEvent) {
                document.attachEvent("onclick", this.hideCalendarHandler);
            } else {
                document.onclick = function () {
                    uiMiniCalendar.hide()
                };
            }
            var re = /^(\d{1,2}\/\d{1,2}\/\d{1,4})\s*(\s+\d{1,2}:\d{1,2}:\d{1,2})?$/i;
            this.selectedDate = new Date();

            if (this.dateField.value != '') {
                // TODO: tamnd - set selected date to calendar
                var dateFieldValue = this.dateField.value;

                var dateIndex = this.datePattern.indexOf("dd");
                var dateValue = parseInt(dateFieldValue.substring(dateIndex,
                    dateIndex + 2), 10);

                var monthIndex = this.datePattern.indexOf("MM");
                var monthValue = parseInt(dateFieldValue.substring(monthIndex,
                    monthIndex + 2) - 1, 10);

                var yearIndex = this.datePattern.indexOf("yyyy");
                var yearValue = parseInt(dateFieldValue.substring(yearIndex,
                    yearIndex + 4), 10);

                var hourIndex = this.datePattern.indexOf("HH");
                var hoursValue = parseInt(dateFieldValue.substring(hourIndex,
                    hourIndex + 2), 10);

                var minuteIndex = this.datePattern.indexOf("mm");
                var minutesValue = parseInt(dateFieldValue.substring(minuteIndex,
                    minuteIndex + 2), 10);

                var secondIndex = this.datePattern.indexOf("ss");
                var secondValue = parseInt(dateFieldValue.substring(secondIndex,
                    secondIndex + 2), 10);

                if (isNaN(secondValue)) {
                    secondValue = "00";
                }
                if (isNaN(minutesValue)) {
                    minutesValue = "00";
                }
                if (isNaN(hoursValue)) {
                    hoursValue = "00";
                }

                var testDate = "MM/dd/yyyy HH:mm:ss";
                testDate = testDate.replace("dd", dateValue);
                testDate = testDate.replace("MM", monthValue + 1);
                testDate = testDate.replace("yyyy", yearValue);
                testDate = testDate.replace("HH", hoursValue);
                testDate = testDate.replace("mm", minutesValue);
                testDate = testDate.replace("ss", secondValue);

                if (re.test(testDate)) {
                    this.selectedDate.setFullYear(yearValue);
                    this.selectedDate.setMonth(monthValue);
                    this.selectedDate.setDate(dateValue);
                    this.selectedDate.setHours(hoursValue);
                    this.selectedDate.setMinutes(minutesValue);
                    this.selectedDate.setSeconds(secondValue);
                }

            }
            this.currentDate = new Date(this.selectedDate.valueOf());
            var clndr = document.getElementById(this.calendarId);
            clndr.firstChild.lastChild.innerHTML = this.renderCalendar();
            // var x = 0 ;
            var y = this.dateField.offsetHeight;
            var beforeShow = $(window).height();
            with(clndr.firstChild.style) {
                display = 'block';
                // left = x + "px" ;
                // top = y + "px";
                if (eXo.core.I18n.isLT())
                    left = "0px";
                else
                    right = "0px";
            }
            var offsetDateField = $(this.dateField).offset();
            var topCal = offsetDateField.top + y;
            var leftCal = offsetDateField.left - 50;
            $(clndr.firstChild).offset({
                top: topCal,
                left: leftCal
            });

            // Show calendar form above date field if not enough space below
            var heightCal = document.getElementById('BlockCalendar').offsetHeight;
            var afterShow = topCal + heightCal;
            if (afterShow > beforeShow) {
                topCal = offsetDateField.top - heightCal;
                $(clndr.firstChild).offset({
                    top: topCal,
                    left: leftCal
                });
            }

            // uiMiniCalendar.initDragDrop();

            var primary = $(this.dateField).closest("#UIECMSearch");
            if (primary.length && base.Browser.isFF()) {
                var calendar = clndr.firstChild;
                calendar.style.top = "0px";
                calendar.style.left = this.dateField.offsetLeft - this.dateField.offsetWidth - 32 + "px";
            }
            $("#BlockCalendar a[rel='tooltip']").tooltip();
        },

        onTabOut: function (event) {
            var keyCode = event.keyCode;

            // identify the tab key
            if (keyCode == 9) {
                uiMiniCalendar.hide();
            }
        },

        hide: function () {
            if (this.dateField) {
                document.getElementById(this.calendarId).firstChild.style.display = 'none';
                // this.dateField.parentNode.style.position = '' ;
                this.dateField.blur();
                this.dateField = null;
            }
            if (document.removeEventListener) {
                document.removeEventListener("click", this.hideCalendarHandler, false);
            } else if (document.detachEvent) {
                document.detachEvent("onclick", this.hideCalendarHandler);
            } else {
                document.onclick = null;
            }
            // document.onmousedown = null;
        },

        /* TODO: Move HTML code to a javascript template file (.jstmpl) */
        renderCalendar: function () {
            var dayOfMonth = 1;
            var validDay = 0;
            var startDayOfWeek = this.getDayOfWeek(this.currentDate.getFullYear(),
                this.currentDate.getMonth() + 1, dayOfMonth);
            var daysInMonth = this.getDaysInMonth(this.currentDate.getFullYear(),
                this.currentDate.getMonth());
            var clazz = null;
            var table = '<div id="BlockCalendar" class="uiMiniCalendar uiBox" style="width: 225px;" onclick="event.cancelBubble = true">';
            table += '<div onmousedown="event.cancelBubble = true" style="cursor: default">';
            table += '<h5 class="title clearfix">';
            table += '<a data-placement="right" rel="tooltip" onclick="uiMiniCalendar.changeMonth(-1);" class="actionIconSmall pull-left" data-original-title="' + msg.getMessage("PreviousMonth") + '"><i class="uiIconMiniArrowLeft uiIconLightGray"></i></a>';
            table += '<span>' + this.months[this.currentDate.getMonth()] + ', ' + this.currentDate.getFullYear() + '</span>';
            table += '<a data-placement="right" rel="tooltip" onclick="uiMiniCalendar.changeMonth(1);" class="actionIconSmall pull-right" data-original-title="' + msg.getMessage("NextMonth") + '"><i class="uiIconMiniArrowRight uiIconLightGray"></i></a>';
            table += '</h5>';

            table += '<table class="weekList">';
            table += '  <tr>';

            if (this.weekdays == null) {
                this.weekdays = new Array("S", "M", "T", "W", "T", "F", "S");
            }
            for (var i = 0; i < 7; i++) {
                if (i == (8 - this.firstDayOfWeek) % 7) {
                    table += ' <td><font color="red">' + this.weekdays[(i + this.firstDayOfWeek - 1) % 7] + '</font></td>';
                } else {
                    table += ' <td>' + this.weekdays[(i + this.firstDayOfWeek - 1) % 7] + '</td>';
                }
            }

            table += '  </tr>';
            table += '</table>';
            table += '<hr>';

            var _pyear, _pmonth, _pday, _nyear, _nmonth, _nday, _weekend;
            var _today = new Date();
            var tableRow = '';
            if (startDayOfWeek == 0) startDayOfWeek = 7;
            _pyear = (this.currentDate.getMonth() == 0) ? this.currentDate.getFullYear() - 1 : this.currentDate.getFullYear();
            _pmonth = (this.currentDate.getMonth() == 0) ? 11 : this.currentDate.getMonth() - 1;
            _pday = this.getDaysInMonth(_pyear, _pmonth) - ((startDayOfWeek + ((8 - this.firstDayOfWeek) % 7)) % 7) + 1;

            _nmonth = (this.currentDate.getMonth() == 11) ? 0 : this.currentDate.getMonth() + 1;
            _nyear = (this.currentDate.getMonth() == 11) ? this.currentDate.getFullYear() + 1 : this.currentDate.getFullYear();
            _nday = 1;

            table += '<table cellspacing="0" cellpadding="0" id="" class="weekDays">';
            for (var week = 0; week < 6; week++) {
                tableRow += '<tr {{week' + week + '}}>';
                for (var dayOfWeek = 0; dayOfWeek <= 6; dayOfWeek++) {
                    if (week == 0 && dayOfWeek == (startDayOfWeek + ((8 - this.firstDayOfWeek) % 7)) % 7) {
                        validDay = 1;
                    } else if (validDay == 1 && dayOfMonth > daysInMonth) {
                        validDay = 0;
                    }
                    if (validDay) {
                        if (dayOfMonth == this.selectedDate.getDate() && this.currentDate.getFullYear() == this.selectedDate
                            .getFullYear() && this.currentDate.getMonth() == this.selectedDate.getMonth()) {
                            clazz = 'selected';
                        } else {
                            clazz = '';
                        }
                        if (_today.getDate() == dayOfMonth && this.currentDate.getFullYear() == _today.getFullYear() && this.currentDate.getMonth() == _today.getMonth()) {
                            clazz = 'highLight today';
                            tableRow = tableRow.replace('{{week' + week + '}}', 'class="currentWeek"');
                        }
                        tableRow = tableRow + '<td><a class="' + clazz + '" href="#SelectDate" onclick="uiMiniCalendar.setDate(' + this.currentDate.getFullYear() + ',' + (this.currentDate.getMonth() + 1) + ',' + dayOfMonth + ')">' + dayOfMonth + '</a></td>';
                        dayOfMonth++;
                        _weekend = week;
                    } else if (validDay == 0 && week == 0) {
                        tableRow = tableRow + '<td><a href="#SelectDate" class="otherMonth" onclick="uiMiniCalendar.setDate(' + _pyear + ',' + (_pmonth + 1) + ',' + _pday + ')">' + _pday + '</a></td>';
                        _pday++;
                    } else if (validDay == 0 && week != 0 && _weekend == week) {
                        tableRow = tableRow + '<td><a href="#SelectDate" class="otherMonth" onclick="uiMiniCalendar.setDate(' + _nyear + ',' + (_nmonth + 1) + ',' + _nday + ')">' + _nday + '</a></td>';
                        _nday++;
                    }
                }
                tableRow += "</tr>";
                tableRow = tableRow.replace('{{week' + week + '}}', '');
            }
            table += tableRow + '</table>';

            if (this.isDisplayTime) {
                table += ' <div class="calendarTimeInput">';
                table += '   <span><input type="text" class="InputTime" maxlength="2" value="' + ((this.currentDate.getHours()) > 9 ? this.currentDate.getHours() : "0" + this.currentDate.getHours()) + '" onkeyup="uiMiniCalendar.setHour(this)" onfocus="this.parentNode.className=\'focus\'" onblur="this.parentNode.className=\'\'">' + ':' + '<input type="text" class="InputTime" maxlength="2" value="' + ((this.currentDate.getMinutes()) > 9 ? this.currentDate
                    .getMinutes() : "0" + this.currentDate.getMinutes()) + '" onkeyup = "uiMiniCalendar.setMinus(this)" onfocus="this.parentNode.className=\'focus\'" onblur="this.parentNode.className=\'\'"></span>';
                table += ' </div>';
            }

            table += '</div>';
            table += '</div>';
            return table;
        },

        changeMonth: function (change) {
            this.currentDate.setDate(1);
            this.currentDate.setMonth(this.currentDate.getMonth() + change);
            var clndr = document.getElementById(this.calendarId);
            clndr.firstChild.lastChild.innerHTML = this.renderCalendar();
            $("#BlockCalendar a[rel='tooltip']").tooltip();
        },

        initDragDrop: function () {
            var drag = $("#BlockCalendar");
            var component = drag.closest(".calendarComponent");
            var calendar = drag.children(".UICalendar").first();
            var innerWidth = drag[0].offsetWidth;

            common.DragDrop.init(drag[0], component[0]);
            component[0].onDragStart = function () {
                if (base.Browser.isIE7())
                    drag.height(calendar[0].offsetHeight);
                drag.width(innerWidth);
            }
        },

        changeYear: function (change) {
            this.currentDate.setFullYear(this.currentDate.getFullYear() + change);
            this.currentDay = 0;
            var clndr = document.getElementById(this.calendarId);
            clndr.firstChild.lastChild.innerHTML = this.renderCalendar();
            $("#BlockCalendar a[rel='tooltip']").tooltip();
        },

        setDate: function (year, month, day) {
            if (this.dateField) {
                if (month < 10)
                    month = "0" + month;
                if (day < 10)
                    day = "0" + day;
                var dateString = this.datePattern;
                dateString = dateString.replace("dd", day);
                dateString = dateString.replace("MM", month);
                dateString = dateString.replace("yyyy", year);

                this.currentHours = this.currentDate.getHours();
                this.currentMinutes = this.currentDate.getMinutes();
                this.currentSeconds = this.currentDate.getSeconds();
                if (this.isDisplayTime) {
                    if (typeof (this.currentHours) != "string")
                        hour = this.currentHours.toString();
                    if (typeof (this.currentMinutes) != "string")
                        minute = this.currentMinutes.toString();
                    if (typeof (this.currentSeconds) != "year")
                        second = this.currentSeconds.toString();

                    while (hour.length < 2) {
                        hour = "0" + hour;
                    }
                    while (minute.length < 2) {
                        minute = "0" + minute;
                    }
                    while (second.length < 2) {
                        second = "0" + second;
                    }

                    dateString = dateString.replace("HH", hour);
                    dateString = dateString.replace("mm", minute);
                    dateString = dateString.replace("ss", second);
                }
                this.dateField.value = dateString;
                this.hide();
            }
            return;
        },

        getDateTimeString: function () {
            if (!this.currentDate)
                return this.datePattern;
            var year = "" + this.currentDate.getFullYear();
            var month = "" + (this.currentDate.getMonth() + 1);
            if (month.length < 2)
                month = "0" + month;
            var day = "" + this.currentDate.getDate();
            if (day.length < 2)
                day = "0" + day;
            var hour = "" + this.currentDate.getHours();
            if (hour.length < 2)
                hour = "0" + hour;
            var minute = "" + this.currentDate.getMinutes();
            if (minute.length < 2)
                minute = "0" + minute;
            var second = "" + this.currentDate.getSeconds();
            if (second.length < 2)
                second = "0" + second;

            var dateString = $.trim(this.datePattern);
            if (!this.isDisplayTime) {
                var ptStrings = dateString.split(" ");
                for (var i = 0; i < ptStrings.length; i++) {
                    if (ptStrings[i].indexOf("yyyy") >= 0) {
                        dateString = ptStrings[i];
                        break;
                    }
                }
            }

            dateString = dateString.replace("dd", day);
            dateString = dateString.replace("MM", month);
            dateString = dateString.replace("yyyy", year);
            if (this.isDisplayTime) {
                dateString = dateString.replace("HH", hour);
                dateString = dateString.replace("mm", minute);
                dateString = dateString.replace("ss", second);
            }

            return dateString;
        },

        setSeconds: function (object) {
            if (this.dateField) {
                var seconds = object.value;
                if (isNaN(seconds))
                    return;
                if (seconds >= 60) {
                    object.value = seconds.substring(0, 1);
                    return;
                }
                if (seconds.length < 2)
                    seconds = "0" + seconds;
                this.currentDate.setSeconds(seconds);
                this.currentDay = this.currentDate.getDate();
                this.currentMonth = this.currentDate.getMonth() + 1;
                this.currentYear = this.currentDate.getFullYear();
                this.dateField.value = this.getDateTimeString();
            }
            return;
        },

        setMinus: function (object) {
            if (this.dateField) {
                var minus = object.value;
                if (isNaN(minus))
                    return;
                if (minus >= 60) {
                    object.value = minus.substring(0, 1);
                    return;
                }
                if (minus.length < 2)
                    minus = "0" + minus;
                this.currentDate.setMinutes(minus);
                this.currentDay = this.currentDate.getDate();
                this.currentMonth = this.currentDate.getMonth() + 1;
                this.currentYear = this.currentDate.getFullYear();
                this.dateField.value = this.getDateTimeString();
            }
            return;
        },

        setHour: function (object) {
            if (this.dateField) {
                var hour = object.value;
                if (isNaN(hour))
                    return;
                if (hour >= 24) {
                    object.value = hour.substring(0, 1);
                    return;
                }
                if (hour.length < 2)
                    hour = "0" + hour;
                this.currentDate.setHours(hour);
                this.currentDay = this.currentDate.getDate();
                this.currentMonth = this.currentDate.getMonth() + 1;
                this.currentYear = this.currentDate.getFullYear();
                this.dateField.value = this.getDateTimeString();
            }
            return;
        },

        clearDate: function () {
            this.dateField.value = '';
            this.hide();
        },

        getDayOfWeek: function (year, month, day) {
            var date = new Date(year, month - 1, day);
            return date.getDay();
        },

        getDaysInMonth: function (year, month) {
            return [31, ((!(year % 4) && ((year % 100) || !(year % 400))) ? 29 : 28),
                31, 30, 31, 30, 31, 31, 30, 31, 30, 31
            ][month];
        },

        getFirstDayOfWeek: function () {
            return this.firstDayOfWeek;
        },

        setFirstDayOfWeek: function (dayOfWeek) {
            this.firstDayOfWeek = dayOfWeek;
        },

        isDate: function(txtDate)
        {
          var currVal = txtDate;
          if(currVal == '')
            return false;

          //Declare Regex
          var rxDatePattern = /^(\d{1,2})(\/|-)(\d{1,2})(\/|-)(\d{4})$/;
          var dtArray = currVal.match(rxDatePattern); // is format OK?

          if (dtArray == null)
            return false;

          //Checks for MM/dd/yyyy format.
          dtMonth = dtArray[1];
          dtDay= dtArray[3];
          dtYear = dtArray[5];

          if (dtMonth < 1 || dtMonth > 12)
            return false;
          else if (dtDay < 1 || dtDay> 31)
            return false;
          else if ((dtMonth==4 || dtMonth==6 || dtMonth==9 || dtMonth==11) && dtDay ==31)
            return false;
          else if (dtMonth == 2)
          {
            var isleap = (dtYear % 4 == 0 && (dtYear % 100 != 0 || dtYear % 400 == 0));
            if (dtDay> 29 || (dtDay ==29 && !isleap))
              return false;
          }
          return true;
        }
    };

    return uiMiniCalendar;
})($, base, common, msg);
