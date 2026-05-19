package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.yongzhang.firebird.Data.User;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE id = #{id}")
    User getById(Long id);

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM user WHERE username = #{username} AND password = #{password}")
    User login(String username, String password);

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    @Insert("INSERT INTO user(username, password, role, balance, status, banned, credit_score) VALUES(#{username}, #{password}, #{role}, #{balance}, #{status}, 0, 100)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);

    @Insert("INSERT INTO user(username, password, role, phone, id_card, address, business_type, description, balance, status, banned, credit_score) " +
            "VALUES(#{username}, #{password}, #{role}, #{phone}, #{idCard}, #{address}, #{businessType}, #{description}, #{balance}, #{status}, 0, 100)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSeller(User user);

    @Select("SELECT * FROM user")
    List<User> findAll();

    @Select("SELECT * FROM user WHERE role = #{role}")
    List<User> findByRole(String role);

    @Select("SELECT * FROM user WHERE role = 'seller'")
    List<User> getSellers();

    @Update("UPDATE user SET username = #{username}, role = #{role} WHERE id = #{id}")
    int updateUser(User user);

    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteUser(Long id);

    @Update("UPDATE user SET balance = #{balance} WHERE id = #{id}")
    int updateBalance(@Param("id") Long id, @Param("balance") Double balance);

    @Update("UPDATE user SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select("SELECT * FROM user WHERE role = 'seller' AND status = 'pending'")
    List<User> getPendingSellers();

    @Select("SELECT * FROM user WHERE role = 'seller' AND status = #{status}")
    List<User> getSellersByStatus(@Param("status") String status);

    @Update("UPDATE user SET banned = #{banned} WHERE id = #{id}")
    int updateBanned(@Param("id") Long id, @Param("banned") Boolean banned);

    @Update("UPDATE user SET credit_score = #{creditScore} WHERE id = #{id}")
    int updateCreditScore(@Param("id") Long id, @Param("creditScore") Integer creditScore);

    @Select("SELECT credit_score FROM user WHERE id = #{id}")
    Integer getCreditScore(@Param("id") Long id);

    @Select("SELECT " +
            "i.id AS itemId, " +
            "i.title AS itemTitle, " +
            "i.price AS itemPrice, " +
            "COUNT(DISTINCT o.order_id) AS orderCount, " +
            "SUM(oi.quantity) AS totalSold, " +
            "COALESCE(SUM(oi.price * oi.quantity), 0) AS totalRevenue, " +
            "COUNT(DISTINCT CASE WHEN o.refund_status = 'refunded' THEN o.order_id END) AS refundCount " +
            "FROM items i " +
            "LEFT JOIN order_items oi ON i.id = oi.item_id " +
            "LEFT JOIN orders o ON oi.order_id = o.order_id AND o.status = 'completed' " +
            "WHERE i.seller_id = #{sellerId} " +
            "GROUP BY i.id, i.title, i.price " +
            "ORDER BY totalRevenue DESC")
    List<Map<String, Object>> getSellerStatistics(@Param("sellerId") Long sellerId);
}