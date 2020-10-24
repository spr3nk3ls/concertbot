package spr3nk3ls.dewiscraper.bot;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.telegram.telegrambots.meta.api.objects.Message;
import spr3nk3ls.dewiscraper.api.Beschikbaarheid;
import spr3nk3ls.dewiscraper.api.Tijdsblok;
import spr3nk3ls.dewiscraper.dewi.DewiService;
import spr3nk3ls.dewiscraper.scheduler.AlertScheduler;
import spr3nk3ls.dewiscraper.util.DutchDateUtil;

import javax.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ApplicationScoped
public class ConversationHandler {

    private final DewiService dewiService;
    private final AlertScheduler alertScheduler;
    private final Map<Long, Conversation> convos;

    public ConversationHandler(DewiService dewiService, AlertScheduler alertScheduler) {
        this.dewiService = dewiService;
        this.alertScheduler = alertScheduler;
        this.convos = new HashMap<>();
    }

    String handle(Message message){
        Long chatId = message.getChatId();
        Conversation conversation = convos.get(chatId);
        if (conversation == null) {
            conversation = Conversation.builder().chatId(chatId).build();
            convos.put(chatId, conversation);
        }
        if(conversation.getDay() != null){
            convos.remove(chatId);
            return reserveerQuery(conversation.getDay(), message);
        }
        LocalDate day = extractDay(message.getText());
        if(day != null){
            conversation.setDay(day);
            return dayQuery(day);
        } else {
            return "Snap ik niet.";
        }
    }

    private String dayQuery(LocalDate day) {
        String head = String.format("Op %s:\n", DutchDateUtil.getWeekday(day));
        String body = dewiService.getBeschikbaarheidForDay(day).stream().map(this::formatTijdsblok).collect(Collectors.joining("\n"));
        String foot = "\nWil je weten of er een plek vrij komt? Geef een tijdstip op.";
        return head + body + foot;
    }

    private String reserveerQuery(LocalDate day, Message message) {
        LocalTime hours = getTimeFromText(message.getText());
        if(hours == null){
            return "Okee, dan niet.";
        }
        Optional<Tijdsblok> tijdsblok = dewiService.getBeschikbaarheidForDay(day).stream()
                .map(Beschikbaarheid::getTijdsblok)
                .filter(tbl -> tbl.getStarttijd().getHour() == hours.getHour())
                .findFirst();
        if(tijdsblok.isPresent()){
            boolean alertSet = alertScheduler.saveAlert(tijdsblok.get(), message.getChatId());
            if(alertSet) {
                return String.format("Alert gezet voor tijdsblok %tR tot %tR.", tijdsblok.get().getStarttijd(), tijdsblok.get().getEindtijd());
            } else {
                return "Dit alert staat al.";
            }
        } else {
            return "Sorry, dit kan niet.";
        }
    }

    private LocalTime getTimeFromText(String text) {
        Pattern pattern = Pattern.compile("(\\d\\d)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return LocalTime.of(Integer.parseInt(matcher.group()), 0);
        }
        return null;
    }

    private LocalDate extractDay(String message){
        String messageLc = message.toLowerCase();
        LocalDate now = LocalDate.now();
        if(messageLc.contains("vandaag")){
            return now;
        } else if(messageLc.contains("morgen")){
            return now.plusDays(1);
        } else if(messageLc.contains("overmorgen")){
            return now.plusDays(2);
        }
        return IntStream.range(1,8)
                .mapToObj(now::plusDays)
                .filter(containsWeekday(messageLc))
                .findFirst().orElse(null);
    }

    private Predicate<LocalDate> containsWeekday(String messageLc) {
        return date -> messageLc.contains(DutchDateUtil.getWeekday(date).toLowerCase());
    }

    private String formatTijdsblok(Beschikbaarheid beschikbaarheid){
        String info;
        final var minBeschikbaar = beschikbaarheid.getMinBeschikbaar();
        final var maxBeschikbaar = beschikbaarheid.getMaxBeschikbaar();
        if(maxBeschikbaar == 0) {
            info = "bezet";
        } else if (minBeschikbaar == 0){
            info = String.format("vrij, minder dan %d beschikbaar", maxBeschikbaar);
        } else {
            info = String.format("vrij, meer dan %d beschikbaar", minBeschikbaar);
        }
        return String.format("%tR: %s", beschikbaarheid.getTijdsblok().getStarttijd(), info);
    }

    @Builder
    @Data
    @EqualsAndHashCode
    private static class Conversation {
        private final Long chatId;
        private LocalDate day;
    }
}
