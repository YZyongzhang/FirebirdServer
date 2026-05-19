package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Param;
import org.yongzhang.firebird.Data.Order;
import org.yongzhang.firebird.Data.CartItem;
import java.util.List;

@Mapper
public interface OrderMapper {

    @Select("SELECT order_id AS orderId, total_amount AS totalAmount, status, date, user_id AS userId, " +
            "pay_time AS payTime, ship_time AS shipTime, deliver_time AS deliverTime, receive_time AS receiveTime, " +
            "tracking_number AS trackingNumber, shipping_address AS shippingAddress, " +
            "refund_status AS returnStatus, refund_time AS refundTime, refund_reason AS returnReason " +
            "FROM orders WHERE user_id = #{userId} ORDER BY date DESC")
    List<Order> getByUser(@Param("userId") Long userId);

    @Insert("INSERT INTO orders(order_id, user_id, total_amount, status, date) VALUES(#{orderId}, #{userId}, #{totalAmount}, #{status}, #{date})")
    int insert(Order order);

    @Insert("INSERT INTO order_items(id, order_id, item_id, title, price, quantity) VALUES(#{id}, #{orderId}, #{itemId}, #{title}, #{price}, #{quantity})")
    int insertOrderItem(CartItem item);

    @Update("UPDATE orders SET status = #{status} WHERE order_id = #{orderId} AND user_id = #{userId}")
    int updateStatus(@Param("orderId") String orderId, @Param("userId") Long userId, @Param("status") String status);

    @Update("UPDATE orders SET status = #{status}, tracking_number = #{trackingNumber} WHERE order_id = #{orderId}")
    int updateOrderStatus(@Param("orderId") String orderId, @Param("status") String status, @Param("trackingNumber") String trackingNumber);

    @Update("UPDATE orders SET status = 'completed', receive_time = #{receiveTime} WHERE order_id = #{orderId}")
    int updateReceiveTime(@Param("orderId") String orderId, @Param("receiveTime") String receiveTime);

    @Update("UPDATE orders SET refund_status = #{refundStatus}, refund_reason = #{refundReason}, refund_time = #{refundTime} WHERE order_id = #{orderId}")
    int updateRefundStatus(@Param("orderId") String orderId, @Param("refundStatus") String refundStatus,
                          @Param("refundReason") String refundReason, @Param("refundTime") String refundTime);

    @Update("UPDATE orders SET status = #{status}, refund_status = #{refundStatus}, refund_time = #{refundTime} WHERE order_id = #{orderId}")
    int processRefund(@Param("orderId") String orderId, @Param("status") String status,
                      @Param("refundStatus") String refundStatus, @Param("refundTime") String refundTime);

    @Select("SELECT o.order_id AS orderId, o.total_amount AS totalAmount, o.status, o.date, o.user_id AS userId, " +
            "o.pay_time AS payTime, o.ship_time AS shipTime, o.deliver_time AS deliverTime, o.receive_time AS receiveTime, " +
            "o.tracking_number AS trackingNumber, o.shipping_address AS shippingAddress, " +
            "o.refund_status AS returnStatus, o.refund_time AS refundTime, o.refund_reason AS returnReason " +
            "FROM orders o WHERE o.order_id = #{orderId}")
    Order getByOrderId(@Param("orderId") String orderId);

    @Select("SELECT o.order_id AS orderId, o.total_amount AS totalAmount, o.status, o.date, o.user_id AS userId, " +
            "o.pay_time AS payTime, o.ship_time AS shipTime, o.deliver_time AS deliverTime, o.receive_time AS receiveTime, " +
            "o.tracking_number AS trackingNumber, o.shipping_address AS shippingAddress, " +
            "o.refund_status AS returnStatus, o.refund_time AS refundTime, o.refund_reason AS returnReason " +
            "FROM orders o JOIN order_items oi ON o.order_id = oi.order_id " +
            "WHERE oi.item_id = #{itemId} AND o.status = #{status}")
    List<Order> getOrdersByItemId(@Param("itemId") String itemId, @Param("status") String status);

    @Select("SELECT DISTINCT o.order_id AS orderId, o.total_amount AS totalAmount, o.status, o.date, o.user_id AS userId, " +
            "o.pay_time AS payTime, o.ship_time AS shipTime, o.deliver_time AS deliverTime, o.receive_time AS receiveTime, " +
            "o.tracking_number AS trackingNumber, o.shipping_address AS shippingAddress, " +
            "o.refund_status AS returnStatus, o.refund_time AS refundTime, o.refund_reason AS returnReason " +
            "FROM orders o JOIN order_items oi ON o.order_id = oi.order_id " +
            "JOIN items i ON oi.item_id = i.id " +
            "WHERE i.seller_id = #{sellerId}")
    List<Order> getOrdersBySellerId(@Param("sellerId") Long sellerId);

    @Select("SELECT DISTINCT o.order_id AS orderId, o.total_amount AS totalAmount, o.status, o.date, o.user_id AS userId, " +
            "o.pay_time AS payTime, o.ship_time AS shipTime, o.deliver_time AS deliverTime, o.receive_time AS receiveTime, " +
            "o.tracking_number AS trackingNumber, o.shipping_address AS shippingAddress, " +
            "o.refund_status AS returnStatus, o.refund_time AS refundTime, o.refund_reason AS returnReason " +
            "FROM orders o JOIN order_items oi ON o.order_id = oi.order_id " +
            "JOIN items i ON oi.item_id = i.id " +
            "WHERE i.seller_id = #{sellerId} AND o.refund_status = 'pending'")
    List<Order> getPendingRefundsBySellerId(@Param("sellerId") Long sellerId);

    @Select("SELECT order_id AS orderId, total_amount AS totalAmount, status, date, user_id AS userId, " +
            "pay_time AS payTime, ship_time AS shipTime, deliver_time AS deliverTime, receive_time AS receiveTime, " +
            "tracking_number AS trackingNumber, shipping_address AS shippingAddress, " +
            "refund_status AS returnStatus, refund_time AS refundTime, refund_reason AS returnReason " +
            "FROM orders WHERE refund_status IS NOT NULL ORDER BY refund_time DESC")
    List<Order> getAllRefunds();

    @Select("SELECT i.seller_id FROM order_items oi JOIN items i ON oi.item_id = i.id WHERE oi.order_id = #{orderId} LIMIT 1")
    Long getSellerIdByOrderId(@Param("orderId") String orderId);
}
