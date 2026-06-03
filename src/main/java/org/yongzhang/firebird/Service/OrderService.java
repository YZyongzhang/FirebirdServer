package org.yongzhang.firebird.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yongzhang.firebird.Data.CartItem;
import org.yongzhang.firebird.Data.Order;
import org.yongzhang.firebird.Data.User;
import org.yongzhang.firebird.Mapper.CartMapper;
import org.yongzhang.firebird.Mapper.OrderMapper;
import org.yongzhang.firebird.Mapper.UserMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private UserMapper userMapper;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public Map<String, Object> createOrder(Long userId, double totalAmount, String shippingAddress) {
        Map<String, Object> res = new HashMap<>();

        // 校验购物车非空
        List<CartItem> cartItems = cartMapper.getByUser(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            res.put("success", false);
            res.put("message", "购物车为空");
            return res;
        }

        // 校验余额
        User buyer = userMapper.getById(userId);
        if (buyer.getBalance() == null || buyer.getBalance() < totalAmount) {
            res.put("success", false);
            res.put("message", "余额不足");
            return res;
        }

        // 创建订单
        String orderId = UUID.randomUUID().toString();
        String date = LocalDateTime.now().format(DTF);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("pending");
        order.setDate(date);
        order.setShippingAddress(shippingAddress);
        orderMapper.insert(order);

        // 迁移购物车项到订单
        for (CartItem item : cartItems) {
            item.setOrderId(orderId);
            orderMapper.insertOrderItem(item);
        }

        // 清空购物车
        cartMapper.clear(userId);

        res.put("success", true);
        res.put("orderId", orderId);
        res.put("message", "订单创建成功");
        return res;
    }

    @Transactional
    public Map<String, Object> payOrder(String orderId, Long userId) {
        Map<String, Object> res = new HashMap<>();

        Order order = orderMapper.getByOrderId(orderId);
        if (order == null) {
            res.put("success", false);
            res.put("message", "订单不存在");
            return res;
        }

        User buyer = userMapper.getById(userId);
        if (buyer.getBalance() == null || buyer.getBalance() < order.getTotalAmount()) {
            res.put("success", false);
            res.put("message", "余额不足");
            return res;
        }

        // 扣减余额
        double newBalance = buyer.getBalance() - order.getTotalAmount();
        userMapper.updateBalance(userId, newBalance);

        // 更新订单状态
        orderMapper.updateStatus(orderId, userId, "paid");

        res.put("success", true);
        res.put("message", "支付成功");
        res.put("balance", newBalance);
        return res;
    }

    @Transactional
    public Map<String, Object> shipOrder(String orderId, String trackingNumber) {
        Map<String, Object> res = new HashMap<>();

        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            res.put("success", false);
            res.put("message", "物流单号不能为空");
            return res;
        }

        Order order = orderMapper.getByOrderId(orderId);
        if (order == null) {
            res.put("success", false);
            res.put("message", "订单不存在");
            return res;
        }

        orderMapper.updateOrderStatus(orderId, "shipped", trackingNumber);

        res.put("success", true);
        res.put("message", "发货成功");
        return res;
    }

    @Transactional
    public Map<String, Object> confirmOrder(String orderId, Long userId) {
        Map<String, Object> res = new HashMap<>();

        Order order = orderMapper.getByOrderId(orderId);
        if (order == null) {
            res.put("success", false);
            res.put("message", "订单不存在");
            return res;
        }

        String receiveTime = LocalDateTime.now().format(DTF);
        orderMapper.updateReceiveTime(orderId, receiveTime);

        res.put("success", true);
        res.put("message", "确认收货成功");
        return res;
    }

    @Transactional
    public Map<String, Object> applyRefund(String orderId, Long userId, String reason) {
        Map<String, Object> res = new HashMap<>();

        Order order = orderMapper.getByOrderId(orderId);
        if (order == null) {
            res.put("success", false);
            res.put("message", "订单不存在");
            return res;
        }

        String refundTime = LocalDateTime.now().format(DTF);
        orderMapper.updateRefundStatus(orderId, "pending", reason, refundTime);

        res.put("success", true);
        res.put("message", "退款申请已提交");
        return res;
    }

    @Transactional
    public Map<String, Object> reviewRefund(String orderId, String action) {
        Map<String, Object> res = new HashMap<>();

        if (action == null || (!action.equals("approve") && !action.equals("reject"))) {
            res.put("success", false);
            res.put("message", "无效的操作");
            return res;
        }

        Order order = orderMapper.getByOrderId(orderId);
        if (order == null) {
            res.put("success", false);
            res.put("message", "订单不存在");
            return res;
        }

        String refundTime = LocalDateTime.now().format(DTF);

        if ("approve".equals(action)) {
            User buyer = userMapper.getById(order.getUserId());
            double newBalance = (buyer.getBalance() != null ? buyer.getBalance() : 0.0) + order.getTotalAmount();
            userMapper.updateBalance(order.getUserId(), newBalance);
            orderMapper.processRefund(orderId, "cancelled", "refunded", refundTime);

            res.put("success", true);
            res.put("message", "退款已处理，款项已退回买家账户");
        } else {
            orderMapper.updateRefundStatus(orderId, "rejected", order.getReturnReason(), refundTime);
            res.put("success", true);
            res.put("message", "退款申请已拒绝");
        }

        return res;
    }

    @Transactional
    public Map<String, Object> approveReturn(String orderId, Long userId) {
        Map<String, Object> res = new HashMap<>();

        Order order = orderMapper.getByOrderId(orderId);
        if (order == null) {
            res.put("success", false);
            res.put("message", "订单不存在");
            return res;
        }

        Long sellerId = orderMapper.getSellerIdByOrderId(orderId);
        if (sellerId == null || !sellerId.equals(userId)) {
            res.put("success", false);
            res.put("message", "无权限审核此退货");
            return res;
        }

        // 返还余额
        User buyer = userMapper.getById(order.getUserId());
        double newBalance = (buyer.getBalance() != null ? buyer.getBalance() : 0.0) + order.getTotalAmount();
        userMapper.updateBalance(order.getUserId(), newBalance);

        // 更新订单状态
        String refundTime = LocalDateTime.now().format(DTF);
        orderMapper.processRefund(orderId, "cancelled", "refunded", refundTime);

        // 商家信用分扣减
        Integer currentScore = userMapper.getCreditScore(sellerId);
        if (currentScore == null) {
            currentScore = 100;
        }
        int newScore = Math.max(0, currentScore - 1);
        userMapper.updateCreditScore(sellerId, newScore);

        res.put("success", true);
        res.put("message", "退货已通过，款项已退回买家账户，商家信用分-1");
        return res;
    }

    public List<Order> getOrdersByUser(Long userId) {
        if (userId == null) return null;
        return orderMapper.getByUser(userId);
    }

    public Order getOrderById(String orderId) {
        return orderMapper.getByOrderId(orderId);
    }

    public List<Order> getPendingRefundsBySeller(Long sellerId) {
        if (sellerId == null) return null;
        return orderMapper.getPendingRefundsBySellerId(sellerId);
    }
}