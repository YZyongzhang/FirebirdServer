package org.yongzhang.firebird.Data;

public class CartItem {
    private String id;
    private String itemId;
    private String title;
    private double price;
    private String thumb;
    private int quantity;
    private Long userId;

    public CartItem() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getThumb() { return thumb; }
    public void setThumb(String thumb) { this.thumb = thumb; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}

