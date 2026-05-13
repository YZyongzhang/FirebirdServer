package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.*;
import org.yongzhang.firebird.Data.Item;
import java.util.List;

@Mapper
public interface ItemMapper {

    @Select("SELECT id, title, price, thumb, images, description, seller_id, seller_name, date, category FROM items "
            + "WHERE (#{q} IS NULL OR title LIKE CONCAT('%',#{q},'%') OR description LIKE CONCAT('%',#{q},'%')) "
            + "AND (#{category} IS NULL OR category = #{category}) "
            + "ORDER BY date DESC LIMIT #{offset}, #{size}")
    @Results(id = "ItemResult", value = {
            @Result(column = "seller_id", property = "sellerId"),
            @Result(column = "seller_name", property = "sellerName"),
            @Result(column = "images", property = "images")
    })
    List<Item> search(@Param("q") String q, @Param("category") String category, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(1) FROM items WHERE (#{q} IS NULL OR title LIKE CONCAT('%',#{q},'%') OR description LIKE CONCAT('%',#{q},'%')) "
            + "AND (#{category} IS NULL OR category = #{category})")
    int count(@Param("q") String q, @Param("category") String category);

    @Select("SELECT id, title, price, thumb, images, description, seller_id, seller_name, date, category FROM items WHERE id = #{id}")
    @ResultMap("ItemResult")
    Item getById(String id);

    @Insert("INSERT INTO items(id, title, price, thumb, images, description, seller_id, seller_name, date, category) "
            + "VALUES(#{id}, #{title}, #{price}, #{thumb}, #{images}, #{description}, #{sellerId}, #{sellerName}, #{date}, #{category})")
    int insert(Item item);

    @Update("UPDATE items SET title = #{title}, price = #{price}, thumb = #{thumb}, images = #{images}, description = #{description}, category = #{category} WHERE id = #{id} AND seller_id = #{sellerId}")
    int update(Item item);

    @Delete("DELETE FROM items WHERE id = #{id} AND seller_id = #{sellerId}")
    int delete(@Param("id") String id, @Param("sellerId") Long sellerId);

    @Select("SELECT DISTINCT category FROM items")
    List<String> categories();
}

