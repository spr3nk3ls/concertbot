package spr3nk3ls.dewiscraper.scheduler;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import lombok.*;
import spr3nk3ls.dewiscraper.api.Tijdsblok;

import javax.persistence.Entity;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Builder
class Alert extends PanacheEntity {
    @NonNull
    private Long chatId;
    @NonNull
    private LocalDateTime starttijd;
    @NonNull
    private LocalDateTime eindtijd;

    public Tijdsblok getTijdsblok(){
        return Tijdsblok.builder().starttijd(starttijd).eindtijd(eindtijd).build();
    }

    public static boolean exists(Alert alert){
        return Alert.find("chatid = ?1 and starttijd = ?2 and eindtijd = ?3",
                alert.chatId, alert.starttijd, alert.eindtijd).count() > 0;
    }
}
