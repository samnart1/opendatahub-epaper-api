package it.noi.edisplay.services;


import it.noi.edisplay.dto.EventDto;
import it.noi.edisplay.dto.NOIPlaceData;
import it.noi.edisplay.dto.NOIPlaceDto;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class OpenDataRestService {

	private final RestTemplate restTemplate;
	private String eventsUrl = "https://api.tourism.testingmachine.eu/v1/EventShort/GetbyRoomBooked?startdate=%s&eventlocation=NOI&datetimeformat=uxtimestamp&onlyactive=true";
	private String eventLocationUrl = "http://tourism.opendatahub.bz.it/api/EventShort/RoomMapping";
	private String placesUrl = "https://mobility.api.opendatahub.bz.it/v2/flat/NOI-Place?select=scode,smetadata.name.en,smetadata.room_label,smetadata.todaynoibzit&limit=-1&where=and(smetadata.type.in.(Meetingroom,Seminarroom),sorigin.neq.office365,sactive.eq.true)";


	public OpenDataRestService(RestTemplateBuilder restTemplateBuilder) {
		this.restTemplate = restTemplateBuilder.build();
	}

	public List<EventDto> getEvents() {
		ArrayList<EventDto> result = new ArrayList<>();
		String urlWithTimestamp = String.format(eventsUrl, new Date().getTime());
		EventDto[] eventDtos = restTemplate.getForObject(urlWithTimestamp, EventDto[].class);
		Collections.addAll(result, eventDtos);
		return result;
	}

	public List<String> getEventLocations() {
		ArrayList<String> result = new ArrayList<>();
		String eventLocationsRawString = restTemplate.getForObject(eventLocationUrl, String.class);

		eventLocationsRawString = eventLocationsRawString.substring(1, eventLocationsRawString.length() - 1 );
		for(String s : eventLocationsRawString.split(","))
			result.add(s.split(":")[0].replaceAll("\"", ""));

		return result;
	}
	
	public List<NOIPlaceData> getNOIPlaces() {
	    NOIPlaceDto places = restTemplate.getForObject(placesUrl, NOIPlaceDto.class);
	    if (places != null) {
	        return places.getData();
	    }
		return new ArrayList<>();
	}
}
