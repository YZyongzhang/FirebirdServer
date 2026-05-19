package org.yongzhang.firebird.Data;

import java.util.List;

public class Order {
    private String orderId;
    private List<CartItem> items;
    private double totalAmount;
    private String status;
    private String date;
    private Long userId;
    private String payTime;
    private String shipTime;
    private String deliverTime;
    private String receiveTime;
    private String trackingNumber;
    private String shippingAddress;
    private String refundStatus;
    private String refundTime;
    private String refundReason;

    public Order() {}

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPayTime() { return payTime; }
    public void setPayTime(String payTime) { this.payTime = payTime; }

    public String getShipTime() { return shipTime; }
    public void setShipTime(String shipTime) { this.shipTime = shipTime; }

    public String getDeliverTime() { return deliverTime; }
    public void setDeliverTime(String deliverTime) { this.deliverTime = deliverTime; }

    public String getReceiveTime() { return receiveTime; }
    public void setReceiveTime(String receiveTime) { this.receiveTime = receiveTime; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }

    public String getRefundTime() { return refundTime; }
    public void setRefundTime(String refundTime) { this.refundTime = refundTime; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
}