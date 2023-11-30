package spr3nk3ls.concertbot.concert;

import lombok.Data;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.List;

@Data
@ToString
public class Concert {
    private final String eventName;
    //TODO
    private final Calendar venueStart;
    private final Calendar eventStart;
    private final List<String> artists;
    private final String venue;
    private final String organizer;
}
