package spr3nk3ls.dewiscraper.scheduler;

import io.quarkus.scheduler.Scheduled;
import spr3nk3ls.dewiscraper.api.Beschikbaarheid;
import spr3nk3ls.dewiscraper.api.Tijdsblok;
import spr3nk3ls.dewiscraper.bot.AlertBot;
import spr3nk3ls.dewiscraper.dewi.DewiService;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Singleton
public class AlertScheduler {

    private final DewiService dewiService;
    private final AlertBot alertBot;
    private final List<Alert> alerts;

    public AlertScheduler(DewiService dewiService, AlertBot alertBot) {
        this.dewiService = dewiService;
        this.alertBot = alertBot;
        this.alerts = new ArrayList<>();
    }

    public boolean saveAlert(Tijdsblok tijdsblok, Long chatId){
        Alert alert = Alert.builder().tijdsblok(tijdsblok).chatId(chatId).build();
        if(alerts.contains(alert)){
            return false;
        }
        alerts.add(alert);
        return true;
    }

    @Scheduled(every = "30s")
    public void sendAlerts(){
        alerts.removeIf(sendAlert());
    }

    private Predicate<Alert> sendAlert() {
        return alert -> {
            Beschikbaarheid beschikbaarheid = dewiService.getBeschikbaarheid(alert.getTijdsblok());
            if(beschikbaarheid == null || beschikbaarheid.getMaxBeschikbaar() == 0){
                return false;
            }
            alertBot.sendAlert(alert.getChatId(), beschikbaarheid);
            return true;
        };
    }
}
