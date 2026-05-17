package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.*;
import org.yongzhang.firebird.Data.ThoughtNote;
import java.util.List;

@Mapper
public interface ThoughtNoteMapper {

    @Select("SELECT id, group_id, title, content, user_id, created_at, updated_at FROM thought_notes WHERE user_id = #{userId} ORDER BY updated_at DESC")
    @Results(id = "ThoughtNoteResult", value = {
            @Result(column = "group_id", property = "groupId"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<ThoughtNote> getByUserId(@Param("userId") Long userId);

    @Select("SELECT id, group_id, title, content, user_id, created_at, updated_at FROM thought_notes WHERE group_id = #{groupId} AND user_id = #{userId} ORDER BY updated_at DESC")
    @ResultMap("ThoughtNoteResult")
    List<ThoughtNote> getByGroupId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Select("SELECT id, group_id, title, content, user_id, created_at, updated_at FROM thought_notes WHERE id = #{id} AND user_id = #{userId}")
    @ResultMap("ThoughtNoteResult")
    ThoughtNote getById(@Param("id") Long id, @Param("userId") Long userId);

    @Insert("INSERT INTO thought_notes(group_id, title, content, user_id) VALUES(#{groupId}, #{title}, #{content}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ThoughtNote note);

    @Update("UPDATE thought_notes SET title = #{title}, content = #{content} WHERE id = #{id} AND user_id = #{userId}")
    int update(@Param("id") Long id, @Param("userId") Long userId, @Param("title") String title, @Param("content") String content);

    @Delete("DELETE FROM thought_notes WHERE id = #{id} AND user_id = #{userId}")
    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
