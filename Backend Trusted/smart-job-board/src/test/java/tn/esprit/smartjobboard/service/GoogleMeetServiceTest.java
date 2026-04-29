package tn.esprit.smartjobboard.service;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.smartjobboard.config.GoogleCalendarConfig;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleMeetService")
class GoogleMeetServiceTest {

    @Mock
    private GoogleCalendarConfig googleCalendarConfig;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private Calendar calendarMock;

    @InjectMocks
    private GoogleMeetService service;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(googleCalendarConfig.buildCalendarClient()).thenReturn(calendarMock);
        lenient().when(googleCalendarConfig.getCalendarId()).thenReturn("primary");
    }

    @Test
    @DisplayName("should create meet successfully and extract HangoutLink")
    void createMeetSuccess() throws Exception {
        Event savedEvent = new Event();
        savedEvent.setId("event123");
        savedEvent.setHangoutLink("https://meet.google.com/abc-defg-hij");

        when(calendarMock.events().insert(anyString(), any()).setConferenceDataVersion(1).setSendUpdates(anyString()).execute())
                .thenReturn(savedEvent);

        GoogleMeetService.ScheduleResult result = service.createMeet(
                "Interview", "Notes", LocalDateTime.now(), 30, "me@test.com", "other@test.com"
        );

        assertThat(result.eventId()).isEqualTo("event123");
        assertThat(result.meetUrl()).isEqualTo("https://meet.google.com/abc-defg-hij");
    }

    @Test
    @DisplayName("should extract meet url from ConferenceData conferenceId if HangoutLink is missing")
    void extractFromConferenceId() throws Exception {
        Event savedEvent = new Event();
        savedEvent.setId("evt");
        ConferenceData cd = new ConferenceData();
        cd.setConferenceId("xyz-uvw-qrs");
        savedEvent.setConferenceData(cd);

        when(calendarMock.events().insert(anyString(), any()).setConferenceDataVersion(1).setSendUpdates(anyString()).execute())
                .thenReturn(savedEvent);

        GoogleMeetService.ScheduleResult result = service.createMeet(
                "Interview", "Notes", LocalDateTime.now(), 30, "me@test.com", "other@test.com"
        );

        assertThat(result.meetUrl()).isEqualTo("https://meet.google.com/xyz-uvw-qrs");
    }

    @Test
    @DisplayName("should throw IllegalStateException if meet url is not generated and refresh fails")
    void missingMeetUrl() throws Exception {
        Event savedEvent = new Event();
        savedEvent.setId("evt");

        when(calendarMock.events().insert(anyString(), any()).setConferenceDataVersion(1).setSendUpdates(anyString()).execute())
                .thenReturn(savedEvent);
        when(calendarMock.events().get(anyString(), anyString()).execute()).thenReturn(savedEvent);

        assertThatThrownBy(() -> service.createMeet(
                "Interview", "Notes", LocalDateTime.now(), 30, "me@test.com", "other@test.com"
        )).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Google Meet link not available");
    }

    @Test
    @DisplayName("should handle GoogleJsonResponseException for invalid grant")
    void handleInvalidGrant() throws Exception {
        GoogleJsonError error = new GoogleJsonError();
        error.setMessage("invalid_grant");
        HttpResponseException.Builder builder = new HttpResponseException.Builder(400, "Bad Request", new HttpHeaders());
        GoogleJsonResponseException exception = new GoogleJsonResponseException(builder, error);

        when(calendarMock.events().insert(anyString(), any()).setConferenceDataVersion(1).setSendUpdates(anyString()).execute())
                .thenThrow(exception);

        assertThatThrownBy(() -> service.createMeet(
                "Interview", "Notes", LocalDateTime.now(), 30, "me@test.com", "other@test.com"
        )).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Invalid refresh token");
    }

    @Test
    @DisplayName("should handle GoogleJsonResponseException for insufficient permissions")
    void handleInsufficientPermissions() throws Exception {
        GoogleJsonError error = new GoogleJsonError();
        error.setMessage("insufficient permissions");
        HttpResponseException.Builder builder = new HttpResponseException.Builder(403, "Forbidden", new HttpHeaders());
        GoogleJsonResponseException exception = new GoogleJsonResponseException(builder, error);

        when(calendarMock.events().insert(anyString(), any()).setConferenceDataVersion(1).setSendUpdates(anyString()).execute())
                .thenThrow(exception);

        assertThatThrownBy(() -> service.createMeet(
                "Interview", "Notes", LocalDateTime.now(), 30, "me@test.com", "other@test.com"
        )).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Calendar not shared with service account");
    }

    @Test
    @DisplayName("should wrap general exceptions in IllegalStateException")
    void handleGeneralException() throws Exception {
        when(googleCalendarConfig.buildCalendarClient()).thenThrow(new RuntimeException("Network down"));

        assertThatThrownBy(() -> service.createMeet(
                "Interview", "Notes", LocalDateTime.now(), 30, "me@test.com", "other@test.com"
        )).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Failed to create Google Meet link");
    }
}
