package spr3nk3ls.dewiscraper.bot;

import org.telegram.telegrambots.meta.api.objects.Message;
import spr3nk3ls.dewiscraper.api.Beschikbaarheid;
import spr3nk3ls.dewiscraper.api.Tijdsblok;
import spr3nk3ls.dewiscraper.dewi.DewiService;
import spr3nk3ls.dewiscraper.scheduler.AlertScheduler;
import spr3nk3ls.dewiscraper.util.DutchDateUtil;

import javax.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class QueryHandler {

    private final DewiService dewiService;
    private final AlertScheduler alertScheduler;
    private final ConversationRepository conversationRepository;

    public QueryHandler(DewiService dewiService, AlertScheduler alertScheduler, ConversationRepository conversationRepository) {
        this.dewiService = dewiService;
        this.alertScheduler = alertScheduler;
        this.conversationRepository = conversationRepository;
    }

    String handle(Message message){
        Long chatId = message.getChatId();
        Conversation conversation = conversationRepository.getOrCreate(message.getChatId(), message.getText());
        if(conversation.getDay() != null && conversation.getTime() != null) {
            conversationRepository.remove(chatId);
            return timeQuery(chatId, LocalDateTime.of(conversation.getDay(), conversation.getTime()));
        }
        if(conversation.getDay() != null) {
            return dayQuery(conversation.getDay());
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

    private String timeQuery(Long chatId, LocalDateTime time) {
        Optional<Tijdsblok> tijdsblok = dewiService.getBeschikbaarheidForDay(time.toLocalDate()).stream()
                .map(Beschikbaarheid::getTijdsblok)
                .filter(tbl -> tbl.getStarttijd().getHour() == time.getHour())
                .findFirst();
        if(tijdsblok.isPresent()){
            boolean alertSet = alertScheduler.saveAlert(tijdsblok.get(), chatId);
            if(alertSet) {
                return String.format("Alert gezet voor tijdsblok %tR tot %tR.", tijdsblok.get().getStarttijd(), tijdsblok.get().getEindtijd());
            } else {
                return "Dit alert staat al.";
            }
        } else {
            return "Sorry, dit kan niet.";
        }
    }

    private String formatTijdsblok(Beschikbaarheid beschikbaarheid){
        String info;
        final var beschikbaar = beschikbaarheid.getBeschikbaar();
        if(beschikbaar == 0) {
            info = "bezet";
        } else {
            info = String.format("vrij, %d beschikbaar", beschikbaar);
        }
        return String.format("%tR: %s", beschikbaarheid.getTijdsblok().getStarttijd(), info);
    }
}
