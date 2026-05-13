package org.yongzhang.firebird.Data;

import java.util.List;

public class ItemsResponse {
    private List<Item> items;
    private int total;

    public ItemsResponse() {}

    public ItemsResponse(List<Item> items, int total) { this.items = items; this.total = total; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}

