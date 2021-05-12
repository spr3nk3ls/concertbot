package spr3nk3ls.dewiscraper.dewi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.cache.CacheResult;
import spr3nk3ls.dewiscraper.api.Beschikbaarheid;
import spr3nk3ls.dewiscraper.api.Tijdsblok;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class DewiService {

    private static final String DEWI_URL = "https://gripnijmegen.dewi-online.nl/iframe/club/156/reservations/activity/1570/time";
    private static final String AREA = "areas%5B%5D=803";
    private static final String PERSONS = "persons=1";
    private static final int GLOBAL_MAX = 30;
    private static final int GLOBAL_MAX_20 = GLOBAL_MAX * 2 / 10;

    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Beschikbaarheid> getBeschikbaarheid(Predicate<Beschikbaarheid> predicate){
        LocalDate today = LocalDate.now();
        return Stream.iterate(today, date -> date.plusDays(1))
                     .limit(5)
                     .map(this::getBeschikbaarheidForDay)
                     .flatMap(Collection::stream)
                     .filter(predicate)
                     .collect(Collectors.toList());
    }

    public Beschikbaarheid getBeschikbaarheid(Tijdsblok tijdsblok){
        List<Beschikbaarheid> beschikbaarheids = getBeschikbaarheidForDay(LocalDate.from(tijdsblok.getStarttijd()));
        return beschikbaarheids.stream().filter(bs -> bs.getTijdsblok().equals(tijdsblok)).findFirst().orElse(null);
    }

    @CacheResult(cacheName = "tijdsbloks")
    public List<Beschikbaarheid> getBeschikbaarheidForDay(LocalDate localDate) {
        try {
            Optional<BlocksPage> blocksPagePage = getDewiPageForDate(localDate);
            return blocksPagePage.map(page -> getBeschikbaarheids(localDate, page)).orElse(List.of());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Beschikbaarheid> getBeschikbaarheids(LocalDate localDate, BlocksPage blocksPagePage) {
        int maxLeft = blocksPagePage.getMax_left();
        return blocksPagePage.getBlocks().stream().map(block -> toBeschikbaarheid(maxLeft, localDate, block)).collect(Collectors.toList());
    }

    private Optional<BlocksPage> getDewiPageForDate(LocalDate localDate) throws IOException, InterruptedException {
        String dateForUri = localDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        URI uri = URI.create(DEWI_URL + "?" + AREA + "&" + PERSONS + "&date=" + dateForUri);
        HttpRequest httpRequest = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = this.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        try {
            return Optional.of(objectMapper.readValue(response.body(), BlocksPage.class));
        } catch (JsonProcessingException e) {
            //Unable to process response
            return Optional.empty();
        }
    }

    private Beschikbaarheid toBeschikbaarheid(int maxLeft, LocalDate localDate, Block block) {
        LocalDateTime startTijd = LocalDateTime.of(localDate, LocalTime.parse(block.getStart()));
        LocalDateTime eindTijd = LocalDateTime.of(localDate, LocalTime.parse(block.getEnd()));
        return Beschikbaarheid.builder()
                .minBeschikbaar(block.getStatus().equals("free") ? GLOBAL_MAX_20 : 0)
                .maxBeschikbaar(getMaxBeschikbaar(maxLeft, block.getStatus()))
                .tijdsblok(Tijdsblok.builder().starttijd(startTijd).eindtijd(eindTijd).build())
                .build();
    }

    private int getMaxBeschikbaar(int maxLeft, String status) {
        if(status.equals("full")){
            return 0;
        }
        if(status.equals("partial")){
            return Math.min(maxLeft, GLOBAL_MAX_20);
        }
        return maxLeft;
    }
}
