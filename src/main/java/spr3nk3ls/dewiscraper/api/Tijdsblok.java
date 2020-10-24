package spr3nk3ls.dewiscraper.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@EqualsAndHashCode
public class Tijdsblok {
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private final LocalDateTime starttijd;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private final LocalDateTime eindtijd;
}
