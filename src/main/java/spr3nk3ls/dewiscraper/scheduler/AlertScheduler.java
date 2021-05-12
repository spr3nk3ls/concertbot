package spr3nk3ls.dewiscraper.scheduler;

import io.quarkus.scheduler.Scheduled;
import spr3nk3ls.dewiscraper.api.Beschikbaarheid;
import spr3nk3ls.dewiscraper.api.Tijdsblok;
import spr3nk3ls.dewiscraper.bot.AlertBot;
import spr3nk3ls.dewiscraper.dewi.DewiService;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.function.Consumer;

@Singleton
@ActivateRequestContext
public class AlertScheduler {

    private final DewiService dewiService;
    private final AlertBot alertBot;

    public AlertScheduler(DewiService dewiService, AlertBot alertBot, EntityManager entityManager) {
        this.dewiService = dewiService;
        this.alertBot = alertBot;
    }

    @Transactional
    public boolean saveAlert(Tijdsblok tijdsblok, Long chatId){
        Alert alert = Alert.builder().starttijd(tijdsblok.getStarttijd()).eindtijd(tijdsblok.getEindtijd()).chatId(chatId).build();
        if(Alert.exists(alert)){
            return false;
        }
        alert.persist();
        return true;
    }

    @Transactional
    @Scheduled(every = "30s")
    public void sendAlerts(){
        Alert.listAll().stream()
                .map(object -> (Alert)object)
                .forEach(sendAndRemoveAlert());
    }

    private Consumer<Alert> sendAndRemoveAlert() {
        return alert -> {
            Beschikbaarheid beschikbaarheid = dewiService.getBeschikbaarheid(alert.getTijdsblok());
            if(beschikbaarheid != null && beschikbaarheid.getBeschikbaar() > 0) {
                alertBot.sendAlert(alert.getChatId(), beschikbaarheid);
                alert.delete();
            }
        };
    }
}
