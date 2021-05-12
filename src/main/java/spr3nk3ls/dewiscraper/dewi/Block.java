package spr3nk3ls.dewiscraper.dewi;

import lombok.Data;

@Data
public class Block {
    private String start;
    private String end;
    private String status;
    private int capacity;
    private boolean entire_day;
    private String[] resources;
}
