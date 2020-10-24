package spr3nk3ls.dewiscraper.bot;

import spr3nk3ls.dewiscraper.util.DutchDateUtil;

import javax.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@ApplicationScoped
public class ConversationRepository {

    private final Map<Long, Conversation> convos = new HashMap<>();

    Conversation getOrCreate(Long chatId, String text){
        Conversation conversation = convos.get(chatId);
        if (conversation == null) {
            conversation = Conversation.builder().chatId(chatId).build();
            convos.put(chatId, conversation);
        }
        LocalDate day = extractDay(text);
        if(day != null) {
            conversation.setDay(day);
        }
        conversation.setTime(extractTime(text));
        return conversation;
    }

    void remove(Long chatId){
        convos.remove(chatId);
    }

    private LocalTime extractTime(String text) {
        Pattern pattern = Pattern.compile("(\\d\\d)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return LocalTime.of(Integer.parseInt(matcher.group()), 0);
        }
        return null;
    }

    private LocalDate extractDay(String message){
        String messageLc = message.toLowerCase();
        LocalDate now = LocalDate.now();
        if(messageLc.contains("vandaag")){
            return now;
        } else if(messageLc.contains("morgen")){
            return now.plusDays(1);
        } else if(messageLc.contains("overmorgen")){
            return now.plusDays(2);
        }
        return IntStream.range(1,8)
                .mapToObj(now::plusDays)
                .filter(containsWeekday(messageLc))
                .findFirst().orElse(null);
    }

    private Predicate<LocalDate> containsWeekday(String messageLc) {
        return date -> messageLc.contains(DutchDateUtil.getWeekday(date).toLowerCase());
    }

}
