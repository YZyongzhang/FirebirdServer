package org.yongzhang.firebird.Data;

import java.util.List;

public class CartResponse {
    private java.util.List<CartItem> items;

    public CartResponse() {}
    public CartResponse(List<CartItem> items) { this.items = items; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
}

