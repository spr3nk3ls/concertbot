package spr3nk3ls.dewiscraper.api;

import spr3nk3ls.dewiscraper.dewi.DewiService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("beschikbaarheid")
public class BeschikbaarheidApi {

    @Inject
    DewiService dewiService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Beschikbaarheid> tijdsblokken(){
        return dewiService.getBeschikbaarheid(tijdsblok -> true);
    }

    @GET
    @Path("/beschikbaar")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Beschikbaarheid> tijdsblokkenBeschikbaar(){
        return dewiService.getBeschikbaarheid(tijdsblok -> tijdsblok.getBeschikbaar() > 0);
    }
}
