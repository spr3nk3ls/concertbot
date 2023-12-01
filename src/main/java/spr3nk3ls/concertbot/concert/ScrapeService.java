package spr3nk3ls.concertbot.concert;

import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import spr3nk3ls.concertbot.bot.AlertBot;
import spr3nk3ls.concertbot.repo.Event;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Startup
@ApplicationScoped
@Slf4j
public class ScrapeService {

    private final List<Scraper> scrapers;
    private final AlertBot alertBot;

    public ScrapeService(AlertBot alertBot) {
        scrapers = List.of(
            new RoosjeScraper("https://www.doornroosje.nl/special/merleyn", "Merleynpas"),
            new RoosjeScraper("https://www.doornroosje.nl/special/doornroosjepas/", "Doornroosjepas"),
            new RoosjeScraper("https://www.doornroosje.nl/", null)
        );
        this.alertBot = alertBot;
    }

    @Scheduled(every = "60s")
    public void sendUpdates(){
        purgePastEvents();
        for(var scraper : scrapers){
            log.info("Start scraping " + scraper.getWebsite());
            var scraped = scraper.scrape(getKnownUris());
            persist(scraped);
            alertBot.sendUpdatedEvents(scraped);
            alertBot.updateQueries();
            log.info(String.format("%s new events found.", scraped.size()));
        }
    }

    @Transactional
    public void persist(Collection<Event> events){
        events.forEach(event -> event.persist());
    }

    @Transactional
    public Set<String> getKnownUris(){
        Stream<Event> events = Event.streamAll();
        return events.map(Event::getUri).collect(Collectors.toSet());
    }

    @Transactional
    public void purgePastEvents() {
        Event.delete("eventStart < ?1", LocalDateTime.now().with(LocalTime.MIN));
    }
}
