package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.*;
import org.yongzhang.firebird.Data.CartItem;
import java.util.List;

@Mapper
public interface CartMapper {

    @Select("SELECT id, item_id, title, price, thumb, quantity, user_id FROM cart WHERE user_id = #{userId}")
    List<CartItem> getByUser(@Param("userId") Long userId);

    @Select("SELECT id, item_id, title, price, thumb, quantity, user_id FROM cart WHERE id = #{id} AND user_id = #{userId}")
    CartItem getById(@Param("id") String id, @Param("userId") Long userId);

    @Insert("INSERT INTO cart(id, item_id, title, price, thumb, quantity, user_id) VALUES(#{id}, #{itemId}, #{title}, #{price}, #{thumb}, #{quantity}, #{userId})")
    int insert(CartItem item);

    @Update("UPDATE cart SET quantity = #{quantity} WHERE id = #{id} AND user_id = #{userId}")
    int updateQuantity(@Param("id") String id, @Param("userId") Long userId, @Param("quantity") int quantity);

    @Delete("DELETE FROM cart WHERE id = #{id} AND user_id = #{userId}")
    int delete(@Param("id") String id, @Param("userId") Long userId);

    @Delete("DELETE FROM cart WHERE user_id = #{userId}")
    int clear(@Param("userId") Long userId);
}

