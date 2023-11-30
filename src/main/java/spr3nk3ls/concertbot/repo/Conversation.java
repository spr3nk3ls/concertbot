package spr3nk3ls.concertbot.repo;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=false)
@Entity
@NoArgsConstructor
public class Conversation extends PanacheEntity {
    public Conversation(Long id){
        this.conversationId = id;
    }
    Long conversationId;
    String modes;

    public Map<String, String> getModesAsMap(){
        if(modes == null || modes.isEmpty()){
            return Map.of();
        }
        return Stream.of(modes.split(" OR "))
            .map(mode -> mode.split("=="))
            .collect(Collectors.toMap(s -> s[0], s -> s[1], (k1, k2) -> k1));
    }
}
