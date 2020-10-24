package spr3nk3ls.dewiscraper.bot;

import io.quarkus.runtime.Startup;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import javax.inject.Singleton;

@Startup
@Singleton
public class BotFactory {

    private final AlertBot alertBot;
    private final TelegramBotsApi telegramBotsApi;

    static {
        ApiContextInitializer.init();
    }

    BotFactory(AlertBot alertBot) throws TelegramApiRequestException {
        this.alertBot = alertBot;
        telegramBotsApi = new TelegramBotsApi();
        registerBots();
    }

    private void registerBots() throws TelegramApiRequestException {
        telegramBotsApi.registerBot(alertBot.getBot());
    }
}
