package spr3nk3ls.concertbot.repo;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import lombok.*;

import java.time.LocalDateTime;

@ToString
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Event extends PanacheEntity {
    private String uri;
    private String eventName;
    private LocalDateTime venueStart;
    private LocalDateTime eventStart;
    private String artists;
    private String venue;
    private String organizer;
    private String special;
}
