package spr3nk3ls.concertbot.api;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import spr3nk3ls.concertbot.repo.Event;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("concerts")
@ApplicationScoped
public class ConcertApi {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Event> getConcerts() {
        return Event.listAll(Sort.by("venueStart"));
    }
}
