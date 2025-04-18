// // SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
// //
// // SPDX-License-Identifier: AGPL-3.0-or-later

// package it.noi.edisplay.services;


// import it.noi.edisplay.dto.EventDto;
// import it.noi.edisplay.dto.NOIPlaceData;
// import it.noi.edisplay.dto.NOIPlaceDto;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.boot.web.client.RestTemplateBuilder;
// import org.springframework.stereotype.Component;
// import org.springframework.web.client.RestTemplate;
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.Date;
// import java.util.List;

// @Component
// public class OpenDataRestService {

// 	private final RestTemplate restTemplate;
	
// 	@Value("${event.offset}")
// 	private int eventOffset;

// 	public OpenDataRestService(RestTemplateBuilder restTemplateBuilder) {
// 		this.restTemplate = restTemplateBuilder.build();
// 	}

// 	public List<EventDto> getEvents() {

// 		ArrayList<EventDto> result = new ArrayList<>();
// 		String urlWithTimestamp = String.format(eventsUrl, new Date().getTime() + eventOffset * 60000);
// 		EventDto[] eventDtos = restTemplate.getForObject(urlWithTimestamp, EventDto[].class);
// 		Collections.addAll(result, eventDtos);
// 		return result;
// 	}

// 	public List<String> getEventLocations() {
// 		ArrayList<String> result = new ArrayList<>();
// 		String eventLocationsRawString = restTemplate.getForObject(eventLocationUrl, String.class);

// 		eventLocationsRawString = eventLocationsRawString.substring(1, eventLocationsRawString.length() - 1 );
// 		for(String s : eventLocationsRawString.split(","))
// 			result.add(s.split(":")[0].replaceAll("\"", ""));

// 		return result;
// 	}

// 	public List<NOIPlaceData> getNOIPlaces() {
// 	    NOIPlaceDto places = restTemplate.getForObject(placesUrl, NOIPlaceDto.class);
// 	    if (places != null) {
// 	        return places.getData();
// 	    }
// 		return new ArrayList<>();
// 	}
// }

package it.noi.edisplay.services;

import it.noi.edisplay.dto.EventDto;
import it.noi.edisplay.dto.NOIPlaceDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class OpenDataRestService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenDataRestService.class);
    
    @Value("${opendata.api.events.url}")
    private String eventsApiUrl;
    
    @Value("${opendata.api.places.url}")
    private String placesApiUrl;
    
    private final RestTemplate restTemplate;
    
    public OpenDataRestService() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Get events from OpenDataHub API with date range filtering
     * 
     * @param fromDate Start date in ISO format (YYYY-MM-DD)
     * @param toDate End date in ISO format (YYYY-MM-DD)
     * @return List of events within the date range
     */
    @Retryable(value = RestClientException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public List<EventDto> getEvents(String fromDate, String toDate) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(eventsApiUrl)
                .queryParam("startdate", fromDate)
                .queryParam("enddate", toDate);
                
            String url = builder.toUriString();
            logger.debug("Fetching events from URL: {}", url);
            
            ResponseEntity<EventDto[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                EventDto[].class
            );
            
            if (response.getBody() != null) {
                return Arrays.asList(response.getBody());
            } else {
                logger.warn("Received empty response body from events API");
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("Error fetching events from OpenDataHub: {}", e.getMessage(), e);
            throw new RestClientException("Failed to fetch events: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get places from OpenDataHub API
     * 
     * @return List of places
     */
    @Retryable(value = RestClientException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public List<NOIPlaceDto> getPlaces() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            logger.debug("Fetching places from URL: {}", placesApiUrl);
            
            ResponseEntity<NOIPlaceDto[]> response = restTemplate.exchange(
                placesApiUrl,
                HttpMethod.GET,
                entity,
                NOIPlaceDto[].class
            );
            
            if (response.getBody() != null) {
                return Arrays.asList(response.getBody());
            } else {
                logger.warn("Received empty response body from places API");
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("Error fetching places from OpenDataHub: {}", e.getMessage(), e);
            throw new RestClientException("Failed to fetch places: " + e.getMessage(), e);
        }
    }
}