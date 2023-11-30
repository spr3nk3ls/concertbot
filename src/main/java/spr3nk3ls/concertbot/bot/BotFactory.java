package spr3nk3ls.concertbot.bot;

import io.quarkus.runtime.Startup;
import jakarta.inject.Singleton;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Startup
@Singleton
public class BotFactory {

    BotFactory(AlertBot alertBot) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(alertBot.getBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}