package tn.esprit.smartjobboard.service;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EntryPoint;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.esprit.smartjobboard.config.GoogleCalendarConfig;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleMeetService {

    private final GoogleCalendarConfig googleCalendarConfig;
    private static final Logger log = LoggerFactory.getLogger(GoogleMeetService.class);

    public ScheduleResult createMeet(
            String title,
            String note,
            LocalDateTime startDateTime,
            int durationMinutes,
            String currentUserEmail,
            String otherPartyEmail
    ) {
        try {
            Calendar calendar = googleCalendarConfig.buildCalendarClient();
            Event event = new Event()
                    .setSummary(title)
                    .setDescription(note == null ? "" : note);

            LocalDateTime endDateTime = startDateTime.plusMinutes(durationMinutes);
            event.setStart(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(startDateTime.toInstant(ZoneOffset.UTC).toEpochMilli())));
            event.setEnd(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(endDateTime.toInstant(ZoneOffset.UTC).toEpochMilli())));

            ConferenceData conferenceData = new ConferenceData();
            conferenceData.setCreateRequest(
                    new CreateConferenceRequest()
                            .setRequestId(UUID.randomUUID().toString())
            );
            event.setConferenceData(conferenceData);

            Event saved = calendar.events()
                    .insert(googleCalendarConfig.getCalendarId(), event)
                    .setConferenceDataVersion(1)
                    .setSendUpdates("none")
                    .execute();

            String meetUrl = extractMeetUrl(saved);
            if ((meetUrl == null || meetUrl.isBlank()) && saved.getId() != null) {
                for (int attempt = 0; attempt < 5 && (meetUrl == null || meetUrl.isBlank()); attempt++) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    Event refreshed = calendar.events()
                            .get(googleCalendarConfig.getCalendarId(), saved.getId())
                            .execute();
                    meetUrl = extractMeetUrl(refreshed);
                }
            }

            if (meetUrl == null || meetUrl.isBlank()) {
                String conferenceStatus = extractConferenceStatus(saved);
                throw new IllegalStateException(
                        "Google Meet link not available. conferenceStatus=" + conferenceStatus +
                                ". Ensure Meet is enabled for this calendar/account."
                );
            }

            return new ScheduleResult(meetUrl, saved.getId());
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (GoogleJsonResponseException ex) {
            String reason = ex.getDetails() != null ? ex.getDetails().getMessage() : ex.getMessage();
            String raw = (reason == null ? "" : reason).toLowerCase();
            if (raw.contains("invalid_grant")) {
                log.error("Google Meet creation failed: invalid_grant");
                throw new IllegalStateException("Invalid refresh token");
            }
            if (ex.getStatusCode() == 403 && (raw.contains("insufficient") || raw.contains("permission"))) {
                log.error("Google Meet creation failed: insufficient permissions / forbidden");
                throw new IllegalStateException("Calendar not shared with service account");
            }
            if (raw.contains("consent")) {
                log.error("Google Meet creation failed: OAuth consent not completed");
                throw new IllegalStateException("OAuth consent not completed");
            }
            log.error("Google Calendar API error status={} message={}", ex.getStatusCode(), reason);
            throw new IllegalStateException("Google Calendar API error: " + reason, ex);
        } catch (Exception ex) {
            log.error("Failed to create Google Meet link", ex);
            throw new IllegalStateException(
                    "Failed to create Google Meet link. " +
                            "Ensure credentials.json is valid and the target calendar grants 'Make changes to events' access."
            );
        }
    }

    private String extractMeetUrl(Event event) {
        if (event == null) return null;
        if (event.getHangoutLink() != null && !event.getHangoutLink().isBlank()) {
            return event.getHangoutLink();
        }
        if (event.getConferenceData() != null
                && event.getConferenceData().getConferenceId() != null
                && !event.getConferenceData().getConferenceId().isBlank()) {
            return "https://meet.google.com/" + event.getConferenceData().getConferenceId();
        }
        if (event.getConferenceData() != null && event.getConferenceData().getEntryPoints() != null) {
            for (EntryPoint entryPoint : event.getConferenceData().getEntryPoints()) {
                if (entryPoint == null) continue;
                String uri = entryPoint.getUri();
                if (uri != null && !uri.isBlank()) {
                    return uri;
                }
            }
        }
        return null;
    }

    private String extractConferenceStatus(Event event) {
        if (event == null || event.getConferenceData() == null || event.getConferenceData().getCreateRequest() == null) {
            return "unknown";
        }
        if (event.getConferenceData().getCreateRequest().getStatus() == null
                || event.getConferenceData().getCreateRequest().getStatus().getStatusCode() == null) {
            return "unknown";
        }
        return event.getConferenceData().getCreateRequest().getStatus().getStatusCode();
    }

    public record ScheduleResult(String meetUrl, String eventId) {}
}
