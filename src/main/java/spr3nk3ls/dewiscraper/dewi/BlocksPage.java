package spr3nk3ls.dewiscraper.dewi;

import lombok.Data;

import java.util.List;

@Data
public class BlocksPage {
    private List<Block> blocks;
    private String status;
    private int max_left;
}
