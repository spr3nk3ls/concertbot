package spr3nk3ls.dewiscraper.bot;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
@Data
@EqualsAndHashCode
public class Conversation {
    private final Long chatId;
    private LocalDate day;
    private LocalTime time;
}
