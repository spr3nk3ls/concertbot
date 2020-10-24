package spr3nk3ls.dewiscraper.bot;

import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import spr3nk3ls.dewiscraper.api.Beschikbaarheid;
import spr3nk3ls.dewiscraper.api.Tijdsblok;
import spr3nk3ls.dewiscraper.util.DutchDateUtil;

import javax.inject.Singleton;

@Singleton
public class AlertBot {

    private final String botUsername;

    private final String botToken;

    private final ConversationHandler conversationHandler;

    @Getter
    private final InnerBot bot = new InnerBot();

    public AlertBot(@ConfigProperty(name = "bot.username") String botUsername,
                    @ConfigProperty(name = "bot.token") String botToken,
                    ConversationHandler conversationHandler) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.conversationHandler = conversationHandler;
    }

    public void sendAlert(Long chatId, Beschikbaarheid beschikbaarheid) {
        Tijdsblok tijdsblok = beschikbaarheid.getTijdsblok();
        this.bot.sendMessage(chatId, String.format(
                "Er is plek vrijgekomen op %s van %tR tot %tR uur.",
                DutchDateUtil.getWeekday(tijdsblok.getStarttijd().toLocalDate()),
                tijdsblok.getStarttijd(),
                tijdsblok.getEindtijd()
        ));
    }

    private class InnerBot extends TelegramLongPollingBot {
        @Override
        public void onUpdateReceived(Update update) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                Message message = update.getMessage();
                String text = conversationHandler.handle(message);
                if (text == null) {
                    return;
                }
                sendMessage(message.getChatId(), text);
            }
        }

        void sendMessage(Long chatId, String text) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(text);
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        @Override
        public String getBotUsername() {
            return botUsername;
        }

        @Override
        public String getBotToken() {
            return botToken;
        }
    }
}
