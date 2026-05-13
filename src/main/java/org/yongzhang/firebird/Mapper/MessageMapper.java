package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.*;
import org.yongzhang.firebird.Data.Message;
import java.util.List;

@Mapper
public interface MessageMapper {

    @Select("SELECT id, from_user_id, from_username, to_user_id, content, date FROM messages "
            + "WHERE (from_user_id = #{userId} AND to_user_id = #{withUserId}) OR (from_user_id = #{withUserId} AND to_user_id = #{userId}) ORDER BY date ASC")
    List<Message> getConversation(@Param("userId") Long userId, @Param("withUserId") Long withUserId);

    @Insert("INSERT INTO messages(id, from_user_id, from_username, to_user_id, content, date, is_read) VALUES(#{id}, #{fromUserId}, #{fromUsername}, #{toUserId}, #{content}, #{date}, 0)")
    int insert(Message msg);

    @Select("SELECT COUNT(1) FROM messages WHERE to_user_id = #{userId} AND is_read = 0")
    int countUnread(@Param("userId") Long userId);
}

