package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.*;
import org.yongzhang.firebird.Data.ThoughtGroup;
import java.util.List;

@Mapper
public interface ThoughtGroupMapper {

    @Select("SELECT id, name, user_id, created_at, updated_at FROM thought_groups WHERE user_id = #{userId} ORDER BY created_at DESC")
    @Results(id = "ThoughtGroupResult", value = {
            @Result(column = "user_id", property = "userId"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<ThoughtGroup> getByUserId(@Param("userId") Long userId);

    @Select("SELECT id, name, user_id, created_at, updated_at FROM thought_groups WHERE id = #{id} AND user_id = #{userId}")
    @ResultMap("ThoughtGroupResult")
    ThoughtGroup getById(@Param("id") Long id, @Param("userId") Long userId);

    @Insert("INSERT INTO thought_groups(name, user_id) VALUES(#{name}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ThoughtGroup group);

    @Update("UPDATE thought_groups SET name = #{name} WHERE id = #{id} AND user_id = #{userId}")
    int update(@Param("id") Long id, @Param("userId") Long userId, @Param("name") String name);

    @Delete("DELETE FROM thought_groups WHERE id = #{id} AND user_id = #{userId}")
    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
