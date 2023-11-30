package spr3nk3ls.concertbot.concert;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import spr3nk3ls.concertbot.repo.Event;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RoosjeScraper implements Scraper {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.forLanguageTag("nl-NL"));

    private static final ZoneId AMSTERDAM = ZoneId.of( "Europe/Amsterdam");

    private final String website;
    private final String special;

    public RoosjeScraper(String website, String special) {
        this.website = website;
        this.special = special;
    }

    @Override
    public String getWebsite(){
        return website;
    }

    @Override
    public Set<Event> scrape(Set<String> knownUris) {
        try {
            var document = Jsoup.connect(website).get();
            Elements events = document.select(".c-program__item");
            return events.stream()
                .map((Element element) -> nodeToConcert(element, knownUris))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public Event nodeToConcert(Element element, Set<String> knownUris) {
        var eventWebPage = element.attributes().get("href");
        if(knownUris.contains(eventWebPage)){
            return null;
        }
        var eventDocument = Jsoup.connect(eventWebPage).get();
        var eventName = eventDocument.select("h1.c-header-event__title").text();
        var otherArtistsArray = eventDocument.select(".c-header-event__title--artists")
                .text()
                .split(" \\+ ");
        var otherArtists = Stream.of(otherArtistsArray)
                .map(String::strip)
                .map(string -> string.replaceFirst("\\+ ",""))
                .filter(string -> !string.isEmpty())
                .toList();
        var organizer = eventDocument.select(".c-header-event__subtitle").text();
        var eventDataMap = getEventMap(eventDocument);
        if(eventDataMap.get("datum:") != null && eventDataMap.get("locatie:") != null) {
            var date = DATE_FORMAT.parse(eventDataMap.get("datum:"));
            var eventStart = getStartTime(date, eventDataMap, "start:");
            var venueStart = getStartTime(date, eventDataMap, "zaal open:");
            List<String> allArtists = new ArrayList<>();
            allArtists.addAll(Arrays.asList(eventName.split(" \\+ ")));
            allArtists.addAll(otherArtists);
            var artistString = allArtists.stream().collect(Collectors.joining(","));
            var event = new Event(eventWebPage, eventName, venueStart, eventStart, artistString, eventDataMap.get("locatie:"), organizer, special);
            return event;
        }
        //TODO fallback
        return null;
    }

    private static LocalDateTime getStartTime(Date date, Map<String, String> eventDataMap, String key) {
        var venueStart = Instant.ofEpochMilli(date.getTime()).atZone(AMSTERDAM).toLocalDateTime();
        var venueStartTime = eventDataMap.get(key);
        if (venueStartTime != null) {
            var venueStartTimeArray = venueStartTime.split("[: ]");
            return venueStart.plusHours(Integer.parseInt(venueStartTimeArray[0])).plusMinutes(Integer.parseInt(venueStartTimeArray[1]));
        }
        return venueStart;
    }

    private static Map<String, String> getEventMap(Document eventDocument) {
        Elements elements = eventDocument.select(".c-event-data__row");
        return elements.stream()
                .collect(Collectors.toMap(el -> getNodeText(el, "c-event-data__label"),
                        el -> getNodeText(el, "c-event-data__value")));
    }

    private static String getNodeText(Element element, String className) {
        return element.getElementsByClass(className).get(0).text();
    }
}
