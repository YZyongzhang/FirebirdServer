package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.*;
import org.yongzhang.firebird.Data.Message;
import java.util.List;
import java.util.Map;

@Mapper
public interface MessageMapper {

    @Select("SELECT id, from_user_id as fromUserId, from_username as fromUsername, to_user_id as toUserId, content, date, item_id as itemId, item_title as itemTitle, is_read as isRead FROM messages "
            + "WHERE (from_user_id = #{userId} AND to_user_id = #{withUserId} AND item_id = #{itemId}) OR (from_user_id = #{withUserId} AND to_user_id = #{userId} AND item_id = #{itemId}) ORDER BY date ASC")
    List<Message> getConversation(@Param("userId") Long userId, @Param("withUserId") Long withUserId, @Param("itemId") String itemId);

    @Insert("INSERT INTO messages(id, from_user_id, from_username, to_user_id, content, date, item_id, item_title, is_read) VALUES(#{id}, #{fromUserId}, #{fromUsername}, #{toUserId}, #{content}, #{date}, #{itemId}, #{itemTitle}, 0)")
    int insert(Message msg);

    @Select("SELECT COUNT(1) FROM messages WHERE to_user_id = #{userId} AND is_read = 0")
    int countUnread(@Param("userId") Long userId);

    @Select("SELECT id, from_user_id as fromUserId, from_username as fromUsername, to_user_id as toUserId, content, date, item_id as itemId, item_title as itemTitle, is_read as isRead FROM messages WHERE from_user_id = #{userId} OR to_user_id = #{userId} ORDER BY date DESC")
    List<Message> getByUser(@Param("userId") Long userId);

    @Update("UPDATE messages SET is_read = 1 WHERE to_user_id = #{userId} AND from_user_id = #{fromUserId} AND item_id = #{itemId}")
    int markAsRead(@Param("userId") Long userId, @Param("fromUserId") Long fromUserId, @Param("itemId") String itemId);

    @Select({
            "SELECT ",
            "  m2.item_id AS itemId,",
            "  m2.item_title AS itemTitle,",
            "  CASE WHEN m2.from_user_id = #{userId} THEN m2.to_user_id ELSE m2.from_user_id END AS otherUserId,",
            "  CASE WHEN m2.from_user_id = #{userId} THEN '' ELSE m2.from_username END AS otherUsername,",
            "  m2.content AS lastMessage,",
            "  m2.date AS lastDate,",
            "  COALESCE(u.unreadCount, 0) AS unreadCount",
            "FROM messages m2",
            "  JOIN (",
            "    SELECT ",
            "      item_id, ",
            "      CASE WHEN from_user_id = #{userId} THEN to_user_id ELSE from_user_id END AS otherUserId,",
            "      MAX(date) AS maxDate",
            "    FROM messages",
            "    WHERE from_user_id = #{userId} OR to_user_id = #{userId}",
            "    GROUP BY item_id, otherUserId",
            "  ) t ON m2.item_id = t.item_id AND m2.date = t.maxDate AND CASE WHEN m2.from_user_id = #{userId} THEN m2.to_user_id ELSE m2.from_user_id END = t.otherUserId",
            "  LEFT JOIN (",
            "    SELECT",
            "      item_id,",
            "      CASE WHEN from_user_id = #{userId} THEN to_user_id ELSE from_user_id END AS otherUserId,",
            "      COUNT(*) AS unreadCount",
            "    FROM messages",
            "    WHERE to_user_id = #{userId} AND is_read = 0",
            "    GROUP BY item_id, otherUserId",
            "  ) u ON m2.item_id = u.item_id AND CASE WHEN m2.from_user_id = #{userId} THEN m2.to_user_id ELSE m2.from_user_id END = u.otherUserId",
            "ORDER BY m2.date DESC"
    })
    List<Map<String, Object>> getConversations(@Param("userId") Long userId);

    @Select("SELECT * FROM messages WHERE item_id = #{itemId} AND (from_user_id = #{userId} OR to_user_id = #{userId}) ORDER BY date ASC")
    List<Message> getMessagesByItem(@Param("userId") Long userId, @Param("itemId") String itemId);
}
