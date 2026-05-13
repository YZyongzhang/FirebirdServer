package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.*;
import org.yongzhang.firebird.Data.Order;
import org.yongzhang.firebird.Data.CartItem;
import java.util.List;

@Mapper
public interface OrderMapper {

    @Select("SELECT order_id, total_amount, status, date, user_id FROM orders WHERE user_id = #{userId} ORDER BY date DESC")
    List<Order> getByUser(@Param("userId") Long userId);

    @Insert("INSERT INTO orders(order_id, user_id, total_amount, status, date) VALUES(#{orderId}, #{userId}, #{totalAmount}, #{status}, #{date})")
    int insert(Order order);

    @Insert("INSERT INTO order_items(id, order_id, item_id, title, price, quantity) VALUES(#{id}, #{orderId}, #{itemId}, #{title}, #{price}, #{quantity})")
    int insertOrderItem(CartItem item);

    @Update("UPDATE orders SET status = #{status} WHERE order_id = #{orderId} AND user_id = #{userId}")
    int updateStatus(@Param("orderId") String orderId, @Param("userId") Long userId, @Param("status") String status);
}

