package org.exoplatform.chat.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.GroupCalendarData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.rest.resource.ResourceContainer;

@SuppressWarnings("deprecation")
@Path("/chat/api/1.0/calendar/")
public class CalendarService implements ResourceContainer {

  private static final String                      DEFAULT_DATE_FORMAT = "MM/dd/yyyy hh:mm a";

  org.exoplatform.calendar.service.CalendarService calendarService_;

  OrganizationService                              organizationService_;

  private static final Log                         LOG                 = ExoLogger.getLogger(CalendarService.class);

  public CalendarService(org.exoplatform.calendar.service.CalendarService calendarService,
                         OrganizationService organizationService) {
    calendarService_ = calendarService;
    organizationService_ = organizationService;
  }

  @POST
  @Path("saveEvent")
  @RolesAllowed("users")
  public Response saveEvent(@Context SecurityContext sc,
                            @FormParam("space") String space,
                            @FormParam("users") String users,
                            @FormParam("summary") String summary,
                            @FormParam("startDate") String startDate,
                            @FormParam("startTime") String startTime,
                            @FormParam("endDate") String endDate,
                            @FormParam("endTime") String endTime,
                            @FormParam("location") String location) {
    SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
    try {
      // TODO manage create event in space room (activity genration error)
      saveEvent(sc.getUserPrincipal().getName(),
                space,
                users,
                summary,
                sdf.parse(startDate + " " + startTime),
                sdf.parse(endDate + " " + endTime),
                location);
    } catch (Exception e) {
      LOG.warn("exception during event creation", e);
      return Response.serverError().build();
    }
    return Response.ok().build();
  }

  public void saveEvent(String user,
                        String calName,
                        String users,
                        String summary,
                        Date from,
                        Date to,
                        String location) throws Exception {
    if (!"".equals(users)) {
      String[] participants = users.split(",");
      for (String participant : participants) {
        String calId = getFirstCalendarsId(participant);
        saveEvent(participant, false, participants, calId, calName, summary, from, to, location);
      }
    } else {
      String calId = getCalendarId(user, calName);
      saveEvent(user, true, null, calId, calName, summary, from, to, location);
    }
  }

  protected void saveEvent(String user,
                           boolean isPublic,
                           String[] participants,
                           String calId,
                           String calName,
                           String summary,
                           Date from,
                           Date to,
                           String location) throws Exception {
    if (calId != null) {
      CalendarEvent event = new CalendarEvent();
      event.setCalendarId(calId);
      event.setSummary(summary);
      event.setEventType(CalendarEvent.TYPE_EVENT);
      event.setRepeatType(CalendarEvent.RP_NOREPEAT);
      event.setPrivate(true);
      event.setFromDateTime(from);
      event.setToDateTime(to);
      event.setPriority(CalendarEvent.PRIORITY_NORMAL);
      event.setLocation(location);
      if (isPublic)
        calendarService_.savePublicEvent(calId, event, true);
      else {
        if (participants != null) {
          event.setParticipant(participants);
          String[] participantsStatus = new String[participants.length];
          for (int i = 0; i < participants.length; i++) {
            participantsStatus[i] = participants[i] + ":confirmed";
          }
          event.setParticipantStatus(participantsStatus);

        }
        calendarService_.saveUserEvent(user, calId, event, true);
        // calendarService_.confirmInvitation("john", "benjamin", "john",
        // Utils.PRIVATE_TYPE, calId, eventId, Utils.ACCEPT);
      }
    }
  }

  private String getFirstCalendarsId(String username) {
    List<org.exoplatform.calendar.service.Calendar> listUserCalendar = null;
    try {
      listUserCalendar = calendarService_.getUserCalendars(username, true);
      if (listUserCalendar.size() > 0) {
        return listUserCalendar.get(0).getId();
      }
    } catch (Exception e) {
      LOG.info("Error while checking User Calendar :" + e.getMessage());
    }
    return null;
  }

  private String getCalendarId(String username, String space) {

    String id = null;
    List<GroupCalendarData> listgroupCalendar = null;
    try {
      listgroupCalendar = calendarService_.getGroupCalendars(getUserGroups(username), true, username);
    } catch (Exception e) {
      LOG.info("Error while checking User Calendar :" + e.getMessage());
    }
    for (GroupCalendarData g : listgroupCalendar) {
      for (org.exoplatform.calendar.service.Calendar c : g.getCalendars()) {
        if (space.equals(c.getName())) {
          id = c.getId();
        }
      }
    }
    return id;
  }

  private String[] getUserGroups(String username) throws Exception {

    Object[] objs = organizationService_.getGroupHandler().findGroupsOfUser(username).toArray();
    String[] groups = new String[objs.length];
    for (int i = 0; i < objs.length; i++) {
      groups[i] = ((Group) objs[i]).getId();
    }
    return groups;
  }
}
