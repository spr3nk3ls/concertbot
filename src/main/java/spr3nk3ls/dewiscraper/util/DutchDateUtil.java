package spr3nk3ls.dewiscraper.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@UtilityClass
public class DutchDateUtil {

    private static final DateTimeFormatter WEEKDAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("nl-NL"));

    public String getWeekday(LocalDate localDate){
        return localDate.format(WEEKDAY_FORMATTER);
    }

}
