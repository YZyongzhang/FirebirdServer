package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Param;
import org.yongzhang.firebird.Data.Item;
import java.util.List;

@Mapper
public interface ItemMapper {

    @Select("SELECT id, title, price, thumb, images, description, seller_id, seller_name, date, category, status FROM items "
            + "WHERE (#{q} IS NULL OR title LIKE CONCAT('%',#{q},'%') OR description LIKE CONCAT('%',#{q},'%')) "
            + "AND (#{category} IS NULL OR category = #{category}) "
            + "AND (status IS NULL OR status = 'available') "
            + "ORDER BY date DESC LIMIT #{offset}, #{size}")
    @Results(id = "ItemResult", value = {
            @Result(column = "seller_id", property = "sellerId"),
            @Result(column = "seller_name", property = "sellerName"),
            @Result(column = "images", property = "images"),
            @Result(column = "status", property = "status")
    })
    List<Item> search(@Param("q") String q, @Param("category") String category, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(1) FROM items WHERE (#{q} IS NULL OR title LIKE CONCAT('%',#{q},'%') OR description LIKE CONCAT('%',#{q},'%')) "
            + "AND (#{category} IS NULL OR category = #{category})")
    int count(@Param("q") String q, @Param("category") String category);

    @Select("SELECT id, title, price, thumb, images, description, seller_id, seller_name, date, category, status FROM items WHERE id = #{id}")
    @ResultMap("ItemResult")
    Item getById(String id);

    @Select("SELECT id, title, price, thumb, images, description, seller_id, seller_name, date, category, status FROM items WHERE seller_id = #{sellerId} ORDER BY date DESC")
    @ResultMap("ItemResult")
    List<Item> getBySellerId(Long sellerId);

    @Select("SELECT id, title, price, thumb, images, description, seller_id, seller_name, date, category, status FROM items WHERE status = #{status} ORDER BY date DESC")
    @ResultMap("ItemResult")
    List<Item> getByStatus(@Param("status") String status);

    @Update("UPDATE items SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status);

    @Update("UPDATE items SET status = 'offline' WHERE id = #{id} AND (seller_id = #{sellerId} OR EXISTS (SELECT 1 FROM user WHERE id = #{sellerId} AND role = 'admin'))")
    int offline(@Param("id") String id, @Param("sellerId") Long sellerId);

    @Insert("INSERT INTO items(id, title, price, thumb, images, description, seller_id, seller_name, date, category, status) "
            + "VALUES(#{id}, #{title}, #{price}, #{thumb}, #{images}, #{description}, #{sellerId}, #{sellerName}, #{date}, #{category}, #{status})")
    int insert(Item item);

    @Update("UPDATE items SET title = #{title}, price = #{price}, thumb = #{thumb}, images = #{images}, description = #{description}, category = #{category}, status = #{status} WHERE id = #{id} AND seller_id = #{sellerId}")
    int update(Item item);

    @Delete("DELETE FROM items WHERE id = #{id} AND seller_id = #{sellerId}")
    int delete(@Param("id") String id, @Param("sellerId") Long sellerId);

    @Select("SELECT DISTINCT category FROM items WHERE category IS NOT NULL AND category != ''")
    List<String> categories();
}