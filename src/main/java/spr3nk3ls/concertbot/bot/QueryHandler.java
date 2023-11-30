package spr3nk3ls.concertbot.bot;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.springframework.data.util.Pair;
import spr3nk3ls.concertbot.repo.Conversation;
import spr3nk3ls.concertbot.repo.Event;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class QueryHandler {

    private List<Query> queries = List.of();
    private static final Map<Function<Event, String>, String> QUERYABLES =
            Map.of(Event::getOrganizer, "organizer", Event::getVenue,"venue", Event::getSpecial, "special");
    public static final Map<String, Function<Event, String>> getInvertedQueryables(){
        return QUERYABLES.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }        

    @Transactional
    public List<Event> handle(String queryString){
        Optional<Stream<Event>> events = parseQuery(queryString)
                .map(query -> Event.stream(query.getKey(), Sort.by("venueStart"), query.getValue()));
        Pattern integerPattern = Pattern.compile("-?\\d+");
        Matcher matcher = integerPattern.matcher(queryString);
        var limit = matcher.find() ? Integer.parseInt(matcher.group()) : 10;
        return events.orElse(Event.streamAll(Sort.by("venueStart"))).limit(limit).toList();
    }

    private Optional<Query> parseQuery(String queryString) {
        return this.queries.stream()
                .filter(q -> q.getValue().toLowerCase().contains(queryString.toLowerCase()))
                .findFirst();
    }

    public String handleQueryables(String queryString, Long conversationId){
        if(queryString.startsWith("/mode")){
            var modeString = queryString.split("/mode");
            if(modeString.length < 2 || modeString[1].isEmpty())
                return null;
            return handleMode(modeString[1].trim(), conversationId);
        }
        return getInvertedQueryables().entrySet().stream()
            .filter(entry -> queryString.equals("/" + entry.getKey()))
            .map(entry -> queries.stream()
                .filter(query -> query.getKey().equals(entry.getKey()))
                .map(Query::getValue)
                .collect(Collectors.joining("\n"))
            ).findAny().orElse(null);
    }

    @Transactional
    public String handleMode(String queryString, Long conversationId) {
        Stream<Event> events  = Event.streamAll();
        PanacheQuery<Conversation> query = Conversation.find("conversationId", conversationId);
        var conversation = query.stream().findFirst();
        if(conversation.isPresent()){
            if(queryString.contains("none") || queryString.contains("off"))
                return setMode(conversation.get(), null, null);
            var pairOpt = events
                    .map(e -> getQueryableForEvent(queryString, e))
                    .flatMap(Optional::stream).findFirst();
            if(pairOpt.isPresent())
                return setMode(conversation.get(), pairOpt.get().getFirst(), pairOpt.get().getSecond());
        }
        return "Unable to set mode";
    }

    private String setMode(Conversation conversation, String key, String value) {
        var currentMode = conversation.getModes() == null ? "" : conversation.getModes() + " OR ";
        conversation.setModes(currentMode + key + "==" + value);
        conversation.persist();
        if(key == null){
            conversation.setModes(null);
            return "Unset mode";
        }
        return "Set mode to: " + conversation.getModes();
    }

    private static Optional<Pair<String, String>> getQueryableForEvent(String queryString, Event event) {
        return QUERYABLES.entrySet().stream()
                .map(e -> getQueryablePair(queryString, event, e)
        ).flatMap(Optional::stream).findFirst();
    }

    private static Optional<Pair<String, String>> getQueryablePair(String queryString, Event event, Map.Entry<Function<Event, String>, String> e) {
        var queryableValue = e.getKey().apply(event);
        if (queryableValue != null && !queryableValue.isEmpty() && queryString.toLowerCase().equals(queryableValue.toLowerCase())) {
            return Optional.of(Pair.of(e.getValue(), queryableValue));
        }
        return Optional.empty();
    }

    @Transactional
    public void updateQueries() {
        this.queries = QUERYABLES.entrySet().stream().flatMap(queryable -> {
            List<Event> events = Event.listAll();
            Stream<String> values = events.stream().map(queryable.getKey()).distinct();
            return values.map(value -> new Query(queryable.getValue(), value));
        })
        .filter(e -> e.getValue() != null)
        .toList();
    }
}
