package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.*;
import org.yongzhang.firebird.Data.Message;
import java.util.List;
import java.util.Map;

@Mapper
public interface MessageMapper {

    @Select("SELECT id, from_user_id, from_username, to_user_id, content, date FROM messages "
            + "WHERE (from_user_id = #{userId} AND to_user_id = #{withUserId}) OR (from_user_id = #{withUserId} AND to_user_id = #{userId}) ORDER BY date ASC")
    List<Message> getConversation(@Param("userId") Long userId, @Param("withUserId") Long withUserId);

    @Insert("INSERT INTO messages(id, from_user_id, from_username, to_user_id, content, date, is_read) VALUES(#{id}, #{fromUserId}, #{fromUsername}, #{toUserId}, #{content}, #{date}, 0)")
    int insert(Message msg);

    @Select("SELECT COUNT(1) FROM messages WHERE to_user_id = #{userId} AND is_read = 0")
    int countUnread(@Param("userId") Long userId);

    @Select("<script>" +
            "SELECT " +
            "    CASE WHEN m.from_user_id = #{userId} THEN m.to_user_id ELSE m.from_user_id END AS userId, " +
            "    CASE WHEN m.from_user_id = #{userId} THEN '' ELSE m.from_username END AS userName, " +
            "    m.content AS lastMessage, " +
            "    m.date AS lastDate, " +
            "    COALESCE(u.unreadCount, 0) AS unreadCount " +
            "FROM (" +
            "    SELECT * FROM messages " +
            "    WHERE from_user_id = #{userId} OR to_user_id = #{userId} " +
            "    ORDER BY date DESC " +
            ") m " +
            "LEFT JOIN (" +
            "    SELECT " +
            "        CASE WHEN from_user_id = #{userId} THEN to_user_id ELSE from_user_id END AS otherUserId, " +
            "        COUNT(*) AS unreadCount " +
            "    FROM messages " +
            "    WHERE to_user_id = #{userId} AND is_read = 0 " +
            "    GROUP BY otherUserId " +
            ") u ON CASE WHEN m.from_user_id = #{userId} THEN m.to_user_id ELSE m.from_user_id END = u.otherUserId " +
            "GROUP BY userId " +
            "ORDER BY m.date DESC" +
            "</script>")
    List<Map<String, Object>> getConversations(@Param("userId") Long userId);
}

