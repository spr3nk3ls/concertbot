package spr3nk3ls.concertbot.bot;


import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import spr3nk3ls.concertbot.repo.Conversation;
import spr3nk3ls.concertbot.repo.Event;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static spr3nk3ls.concertbot.bot.QueryHandler.getInvertedQueryables;

@Slf4j
@Singleton
public class AlertBot {

    private final String botUsername;
    private final QueryHandler handler;

    @Getter
    private final InnerBot bot;

    private static final DateTimeFormatter date = DateTimeFormatter.ofPattern("EE dd-MM");
    private static final DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm");

    public AlertBot(@ConfigProperty(name = "bot.username") String botUsername,
                    QueryHandler handler, @ConfigProperty(name = "bot.token") String botToken) {
        this.botUsername = botUsername;
        this.handler = handler;
        this.bot = new InnerBot(botToken);
    }

    public void sendUpdatedEvents(Set<Event> newEvents){
        log.info("Send updated events");
        if(!newEvents.isEmpty()){
            Stream<Conversation> conversations = Conversation.streamAll();
            conversations.forEach(conversation -> {
                var filteredEvents = newEvents.stream().filter(event -> filterEvents(event, conversation)).toList();
                if(filteredEvents.size() > 0){
                    log.info("Number of filtered events " + filteredEvents.size());
                    var answer = String.format("New events:\n%s", InnerBot.format(filteredEvents));
                    bot.sendMessage(conversation.getConversationId(),  answer);
                }
            });
        }
    }

    private boolean filterEvents(Event event, Conversation conversation) {
        return conversation.getModesAsMap().entrySet().stream()
                .anyMatch(mode -> 
                    getInvertedQueryables()
                        .get(mode.getKey())
                        .apply(event)
                        .equals(mode.getValue()));
    }

    public void updateQueries() {
        handler.updateQueries();
    }

    @Transactional
    public boolean storeConversationId(Long id) {
        if(Conversation.find("conversationId", id).count() == 0){
           var newConversation = new Conversation(id);
           newConversation.persist();
           return true;
        }
        return false;
    }

    private final class InnerBot extends TelegramLongPollingBot {

        public InnerBot(String botToken) {
            super(botToken);
        }

        @Override
        public void onUpdateReceived(Update update) {
            var message = update.getMessage();
            if(update.hasChannelPost())
                message = update.getChannelPost();
            if(message == null || message.getText() == null)
                return;
            var id = message.getChatId();
            var sendHelp = message.getText().toLowerCase().contains("help");
            var newConversation = storeConversationId(id);
            if(sendHelp || newConversation){
                sendMessage(id, getHelpText());
                return;
            }
            var text = message.getText();
            var queryables = handler.handleQueryables(text, id);
            if(queryables != null && !queryables.isEmpty()) {
                sendMessage(id, queryables);
            } else {
                var answer = handler.handle(text);
                sendMessage(id, format(answer));
            }
        }

        private String getHelpText() {
            return String.format("Queries:\n"
                + "\'/help\': this help text\n"
                //TODO /venue [specific venue] opnieuw schrijven
                + "\'/venue\': list venues\n"
                + "\'/organizer\': list organizers\n"
                + "\'/specal\': list specials\n"
                + "\'/mode [mode]\': set a mode, where [mode] a specific venue, organizer, or special\n"
                + "\'/mode off\': unset mode\n"
                + "a number, e.g. \'/50\': get next 50 events\n"
                + "anyting else: get next 10 events"
            );
        }

        @Override
        public String getBotUsername() {
            return botUsername;
        }

        private void sendMessage(Long chatId, String text) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(text);
            try {
                execute(sendMessage);
            } catch (TelegramApiRequestException e) {
                if(e.getErrorCode() == 400 && e.getLocalizedMessage().contains("too long")){
                    SendMessage sendErrorMessage = new SendMessage();
                    sendErrorMessage.setChatId(chatId);
                    sendErrorMessage.setText("Too many events");
                    try {
                        execute(sendErrorMessage);
                    } catch (TelegramApiException e1) {
                        e1.printStackTrace();
                    }
                }
                e.printStackTrace();
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        private static String format(List<Event> events) {
            if(events.size() <= 3){
                return formatLarge(events);
            }
            return formatSmall(events);
        }

        private static String formatLarge(List<Event> events) {
            return events.stream().map(event ->
                    String.format("%s - %s - %s\nZaal open: %s - Start: %s\n%s\n",
                            date.format(event.getEventStart()), event.getVenue(), event.getEventName(),
                            time.format(event.getVenueStart()), time.format(event.getEventStart()),
                            event.getUri())
            ).collect(Collectors.joining("\n"));
        }

        private static String formatSmall(List<Event> events) {
            return events.stream().map(event ->
                    String.format("%s - %s - %s", date.format(event.getEventStart()), event.getEventName(), event.getUri())
            ).collect(Collectors.joining("\n"));
        }
    }
}
