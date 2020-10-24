package spr3nk3ls.dewiscraper.scheduler;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import spr3nk3ls.dewiscraper.api.Tijdsblok;

@Getter
@Builder
@EqualsAndHashCode
class Alert {
    private final Long chatId;
    private final Tijdsblok tijdsblok;
}
