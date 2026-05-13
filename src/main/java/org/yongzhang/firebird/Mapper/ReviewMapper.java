package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.*;
import org.yongzhang.firebird.Data.Review;
import java.util.List;

@Mapper
public interface ReviewMapper {

    @Select("SELECT id, item_id, user_id, username, rating, comment, date FROM reviews WHERE item_id = #{itemId} ORDER BY date DESC")
    List<Review> getByItem(@Param("itemId") String itemId);

    @Insert("INSERT INTO reviews(id, item_id, user_id, username, rating, comment, date) VALUES(#{id}, #{itemId}, #{userId}, #{username}, #{rating}, #{comment}, #{date})")
    int insert(Review review);
}

