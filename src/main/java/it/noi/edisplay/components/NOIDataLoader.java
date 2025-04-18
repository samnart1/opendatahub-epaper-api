package it.noi.edisplay.components;

import it.noi.edisplay.dto.DisplayContentDto;
import it.noi.edisplay.dto.EventDto;
import it.noi.edisplay.dto.NOIPlaceData;
import it.noi.edisplay.dto.NOIPlaceDto;
import it.noi.edisplay.dto.ScheduledContentDto;
import it.noi.edisplay.model.Display;
import it.noi.edisplay.model.ScheduledContent;
import it.noi.edisplay.repositories.ScheduledContentRepository;
import it.noi.edisplay.services.OpenDataRestService;
import it.noi.edisplay.utils.RoomNameUtils;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class NOIDataLoader {

    private static final Logger logger = LoggerFactory.getLogger(NOIDataLoader.class);

    @Value("${event.fetch.days:7}")
    private int eventFetchDaysForward;

    @Value("${event.cleanup.days:1}")
    private int eventCleanupDaysPast;

    @Autowired
    private OpenDataRestService openDataRestService;

    @Autowired
    private ScheduledContentRepository scheduledContentRepository;

    @Autowired
    private ModelMapper modelMapper;

    private List<EventDto> noiTodayEvents = new ArrayList<>();
    private List<NOIPlaceData> noiPlaces = new ArrayList<>();

    /**
     * Scheduled task to load events from OpenDataHub
     * Uses configurable cron expression to determine frequency
     */
    @Scheduled(cron = "${cron.opendata.events}")
    public void loadNoiTodayEvents() {
        try {
            logger.info("Starting scheduled sync of NOI events from OpenDataHub");
            
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusDays(eventFetchDaysForward);
            
            String fromDate = today.format(DateTimeFormatter.ISO_DATE);
            String toDate = endDate.format(DateTimeFormatter.ISO_DATE);
            
            logger.debug("Fetching events from {} to {}", fromDate, toDate);
            
            // Get events from OpenDataHub API with date range
            List<EventDto> newEvents = openDataRestService.getEvents(fromDate, toDate);
            logger.info("Fetched {} events from OpenDataHub", newEvents.size());
            
            // Update the cached event list
            noiTodayEvents = newEvents;
            
            // Sync with database
            syncEventsWithDatabase(newEvents);
            
            // Clean up expired events
            cleanupExpiredEvents();
            
            logger.info("Completed sync of NOI events from OpenDataHub");
        } catch (Exception e) {
            logger.error("Error loading NOI events: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduled task to load NOI places from OpenDataHub
     * Uses configurable cron expression to determine frequency
     */
    @Scheduled(cron = "${cron.opendata.locations}")
    public void loadNoiPlaces() {
        try {
            logger.info("Starting scheduled sync of NOI places from OpenDataHub");
            
            List<NOIPlaceDto> places = openDataRestService.getPlaces();
            List<NOIPlaceData> placeDataList = new ArrayList<>();
            
            for (NOIPlaceDto place : places) {
                NOIPlaceData placeData = new NOIPlaceData();
                placeData.setScode(place.getScode());
                placeData.setRoomLabel(place.getRoomLabel());
                placeData.setName(place.getRoomName());
                placeDataList.add(placeData);
            }
            
            noiPlaces = placeDataList;
            logger.info("Loaded {} NOI places from OpenDataHub", noiPlaces.size());
        } catch (Exception e) {
            logger.error("Error loading NOI places: {}", e.getMessage(), e);
        }
    }

    /**
     * Sync events in our database with data from OpenDataHub
     * 
     * @param openDataEvents Events fetched from OpenDataHub
     */
    private void syncEventsWithDatabase(List<EventDto> openDataEvents) {
        logger.debug("Syncing {} events with database", openDataEvents.size());
        
        int created = 0;
        int updated = 0;
        
        for (EventDto event : openDataEvents) {
            String eventId = event.getId();
            String spaceDesc = event.getSpaceDesc();
            String normalizedRoomName = RoomNameUtils.normalizeRoomName(spaceDesc);
            
            // Find if event already exists in database
            ScheduledContent existingContent = scheduledContentRepository.findByEventId(eventId);
            
            if (existingContent != null) {
                // Update existing event
                existingContent.setEventDescription(event.getEventDescription());
                existingContent.setSpaceDesc(normalizedRoomName);
                existingContent.setStartDate(event.getStartDate());
                existingContent.setEndDate(event.getEndDate());
                existingContent.setLastUpdated(new Date());
                scheduledContentRepository.save(existingContent);
                updated++;
            } else {
                // Create new event record (display will be assigned later)
                ScheduledContent newContent = new ScheduledContent();
                newContent.setEventId(eventId);
                newContent.setEventDescription(event.getEventDescription());
                newContent.setSpaceDesc(normalizedRoomName);
                newContent.setStartDate(event.getStartDate());
                newContent.setEndDate(event.getEndDate());
                newContent.setLastUpdated(new Date());
                newContent.setDisabled(false);
                scheduledContentRepository.save(newContent);
                created++;
            }
        }
        
        logger.info("Event sync completed: {} events created, {} events updated", created, updated);
    }

    /**
     * Remove events that are no longer needed
     * This includes events that have ended in the past and events no longer in OpenDataHub
     */
    private void cleanupExpiredEvents() {
        try {
            // Calculate cutoff date for expired events (today minus configured days)
            LocalDateTime cutoffDate = LocalDate.now()
                .minusDays(eventCleanupDaysPast)
                .atStartOfDay();
            Date cutoffDateAsDate = Date.from(cutoffDate.atZone(ZoneId.systemDefault()).toInstant());
            
            // Get IDs of current events from OpenDataHub
            List<String> currentEventIds = noiTodayEvents.stream()
                .map(EventDto::getId)
                .collect(Collectors.toList());
            
            // Find events to delete (events that ended before cutoff date OR are no longer in OpenDataHub)
            List<ScheduledContent> eventsToDelete = scheduledContentRepository.findExpiredEvents(cutoffDateAsDate, currentEventIds);
            
            if (!eventsToDelete.isEmpty()) {
                logger.info("Deleting {} expired or removed events", eventsToDelete.size());
                scheduledContentRepository.deleteAll(eventsToDelete);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired events: {}", e.getMessage(), e);
        }
    }

    /**
     * Get all display events, including those from OpenDataHub and scheduled content
     * 
     * @param display The display to get events for
     * @return List of ScheduledContentDto objects
     */
    public List<ScheduledContentDto> getAllDisplayEvents(Display display) {
        try {
            List<ScheduledContentDto> result = new ArrayList<>();
            
            // Get existing scheduled content for this display
            List<ScheduledContent> scheduledContents = scheduledContentRepository.findByDisplayId(display.getId());
            
            for (ScheduledContent scheduledContent : scheduledContents) {
                ScheduledContentDto dto = modelMapper.map(scheduledContent, ScheduledContentDto.class);
                
                if (scheduledContent.getDisplayContent() != null) {
                    dto.setDisplayContent(modelMapper.map(scheduledContent.getDisplayContent(), DisplayContentDto.class));
                }
                
                dto.setDisplayUuid(display.getUuid());
                result.add(dto);
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Error getting display events: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get events for specific rooms
     * 
     * @param display The display to get events for
     * @return Map of room codes to lists of events
     */
    public Map<String, List<EventDto>> getNOIDisplayEventsByRoom(Display display) {
        try {
            Map<String, List<EventDto>> result = new HashMap<>();
            List<String> roomCodes = display.getRoomCodes();
            
            if (roomCodes == null || roomCodes.isEmpty()) {
                logger.debug("Display {} has no room codes configured", display.getUuid());
                return result;
            }
            
            // Process each room code
            for (String roomCode : roomCodes) {
                String normalizedRoomCode = RoomNameUtils.normalizeRoomName(roomCode);
                List<EventDto> roomEvents = new ArrayList<>();
                
                // Find matching events from cached OpenDataHub data
                for (EventDto event : noiTodayEvents) {
                    String eventSpaceDesc = RoomNameUtils.normalizeRoomName(event.getSpaceDesc());
                    
                    if (normalizedRoomCode.equals(eventSpaceDesc)) {
                        roomEvents.add(event);
                    }
                }
                
                result.put(normalizedRoomCode, roomEvents);
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Error getting NOI display events by room: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Get all NOI places
     * 
     * @return List of place data objects
     */
    public List<NOIPlaceData> getNOIPlaces() {
        return noiPlaces;
    }
}