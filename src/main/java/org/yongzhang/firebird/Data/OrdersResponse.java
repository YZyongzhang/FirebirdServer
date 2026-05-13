package org.yongzhang.firebird.Data;

import java.util.List;

public class OrdersResponse {
    private List<Order> orders;

    public OrdersResponse() {}
    public OrdersResponse(List<Order> orders) { this.orders = orders; }

    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }
}

