package spr3nk3ls.concertbot.concert;

import spr3nk3ls.concertbot.repo.Event;

import java.util.Set;

public interface Scraper {
    String getWebsite();
    Set<Event> scrape(Set<String> knownUris);
}
