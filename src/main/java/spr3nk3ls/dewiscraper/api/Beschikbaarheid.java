package spr3nk3ls.dewiscraper.api;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Beschikbaarheid {
    private final Tijdsblok tijdsblok;
    private final int beschikbaar;
}
