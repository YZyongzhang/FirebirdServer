package org.yongzhang.firebird.Data;

import java.util.List;

public class CreateOrderRequest {
    private List<String> cartItemIds;
    private double totalAmount;
    private String shippingAddress;

    public CreateOrderRequest() {}
    public List<String> getCartItemIds() { return cartItemIds; }
    public void setCartItemIds(List<String> cartItemIds) { this.cartItemIds = cartItemIds; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
}

