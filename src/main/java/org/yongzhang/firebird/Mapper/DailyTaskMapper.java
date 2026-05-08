package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.*;
import org.yongzhang.firebird.Data.DailyTask;
import java.util.List;

@Mapper
public interface DailyTaskMapper {

    @Select("SELECT id, date, user_id, title, time, done, note FROM daily_task WHERE user_id = #{user_id} AND date = #{date}")
    List<DailyTask> getByDate(@Param("user_id") Long user_id, @Param("date") String date);

    @Select("SELECT id, date, user_id, title, time, done, note FROM daily_task WHERE id = #{id}")
    DailyTask getById(String id);

    @Insert("INSERT INTO daily_task(id, date, user_id, title, time, done, note) VALUES(#{id}, #{date}, #{user_id}, #{title}, #{time}, #{done}, #{note})")
    int insert(DailyTask task);

    @Update("UPDATE daily_task SET title = #{title}, time = #{time}, done = #{done}, note = #{note} WHERE id = #{id} AND user_id = #{user_id}")
    int update(DailyTask task);

    @Delete("DELETE FROM daily_task WHERE id = #{id} AND user_id = #{user_id}")
    int delete(@Param("id") String id, @Param("user_id") Long user_id);

}

