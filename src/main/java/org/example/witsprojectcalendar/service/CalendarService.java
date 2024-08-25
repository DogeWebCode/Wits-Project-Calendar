package org.example.witsprojectcalendar.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import lombok.extern.log4j.Log4j2;
import org.example.witsprojectcalendar.data.dto.UpdateEventRequest;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Service
@Log4j2
public class CalendarService {
    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "Google Calendar API Java Quickstart";
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    /**
     * Directory to store authorization tokens for this application.
     */
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES =
        Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private final Calendar service;

    public CalendarService() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        this.service =
            new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public boolean quickAddEvent(String title) throws IOException {
        Event quickAdd = service.events().quickAdd("primary", title).execute();
        log.info(quickAdd.toString());
        return true;
    }

    public boolean deleteEvent(String id) throws IOException {
        service.events().delete("primary", id).execute();
        return true;
    }

    public List<Event> getEvents() throws IOException {
        DateTime now = new DateTime(System.currentTimeMillis());
        Events events = service.events().list("primary").setMaxResults(10).setTimeMin(now).setOrderBy("startTime").setSingleEvents(true).execute();
        List<Event> items = events.getItems();
        if (items.isEmpty()) {
            System.out.println("No upcoming events found.");
        } else {
            System.out.println("Upcoming events");
            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    start = event.getStart().getDate();
                }
                System.out.printf("%s (%s)\n", event.getSummary(), start);
            }
        }
        return items;
    }

    public Event updateEvent(String eventId, UpdateEventRequest request) throws IOException {

        log.info("Attempting to update event with ID: {}", eventId);

        // 把 Id 帶入，然後取得指定的 Event
        Event event = service.events().get("primary", eventId).execute();
        log.info("Successfully retrieved event: {}", event.getSummary());

        // 這裡放要更新的內容都塞到 event 物件裡面，目前有 Summary(標題)、Start(開始時間)、End(結束時間)
        event.setSummary(request.getNewSummary());

        event.setStart(new EventDateTime().setDateTime(new DateTime(request.getNewStart())));
        event.setEnd(new EventDateTime().setDateTime(new DateTime(request.getNewEnd())));

        // 更新 Event
        Event updatedEvent = service.events().update("primary", eventId, event).execute();
        log.info("Event updated successfully: {} ({})", updatedEvent.getSummary(), updatedEvent.getUpdated());
        return updatedEvent;
    }


    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
        throws IOException {
        // Load client secrets.
        InputStream in = CalendarService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        //returns an authorized Credential object.
        return credential;
    }
}
