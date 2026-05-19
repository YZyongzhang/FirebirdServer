package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.*;
import org.yongzhang.firebird.Data.Order;
import org.yongzhang.firebird.Data.CartItem;
import java.util.List;

@Mapper
public interface OrderMapper {

    @Select("SELECT order_id AS orderId, total_amount AS totalAmount, status, date, user_id AS userId FROM orders WHERE user_id = #{userId} ORDER BY date DESC")
    List<Order> getByUser(@Param("userId") Long userId);

    @Insert("INSERT INTO orders(order_id, user_id, total_amount, status, date) VALUES(#{orderId}, #{userId}, #{totalAmount}, #{status}, #{date})")
    int insert(Order order);

    @Insert("INSERT INTO order_items(id, order_id, item_id, title, price, quantity) VALUES(#{id}, #{orderId}, #{itemId}, #{title}, #{price}, #{quantity})")
    int insertOrderItem(CartItem item);

    @Update("UPDATE orders SET status = #{status} WHERE order_id = #{orderId} AND user_id = #{userId}")
    int updateStatus(@Param("orderId") String orderId, @Param("userId") Long userId, @Param("status") String status);

    @Update("UPDATE orders SET status = #{status}, tracking_number = #{trackingNumber} WHERE order_id = #{orderId}")
    int updateOrderStatus(@Param("orderId") String orderId, @Param("status") String status, @Param("trackingNumber") String trackingNumber);

    @Select("SELECT o.order_id AS orderId, o.total_amount AS totalAmount, o.status, o.date, o.user_id AS userId, o.pay_time AS payTime, o.ship_time AS shipTime, o.deliver_time AS deliverTime, o.tracking_number AS trackingNumber, o.shipping_address AS shippingAddress " +
            "FROM orders o JOIN order_items oi ON o.order_id = oi.order_id " +
            "WHERE oi.item_id = #{itemId} AND o.status = #{status}")
    List<Order> getOrdersByItemId(@Param("itemId") String itemId, @Param("status") String status);

    @Select("SELECT DISTINCT o.order_id AS orderId, o.total_amount AS totalAmount, o.status, o.date, o.user_id AS userId, o.pay_time AS payTime, o.ship_time AS shipTime, o.deliver_time AS deliverTime, o.tracking_number AS trackingNumber, o.shipping_address AS shippingAddress " +
            "FROM orders o JOIN order_items oi ON o.order_id = oi.order_id " +
            "JOIN items i ON oi.item_id = i.id " +
            "WHERE i.seller_id = #{sellerId}")
    List<Order> getOrdersBySellerId(@Param("sellerId") Long sellerId);
}
