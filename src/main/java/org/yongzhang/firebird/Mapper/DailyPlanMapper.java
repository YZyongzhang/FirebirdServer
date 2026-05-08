package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.*;
import org.yongzhang.firebird.Data.DailyPlan;
import java.util.List;

@Mapper
public interface DailyPlanMapper {

    @Select("SELECT date, completed, user_id FROM daily_plan WHERE user_id = #{user_id}")
    List<DailyPlan> getAll(@Param("user_id") Long user_id);

    @Select("SELECT date, completed, user_id FROM daily_plan WHERE user_id = #{user_id} AND date = #{date}")
    DailyPlan getByDate(@Param("user_id") Long user_id, @Param("date") String date);

    @Insert("INSERT INTO daily_plan(user_id, date, completed) VALUES(#{user_id}, #{date}, #{completed})")
    int insert(DailyPlan plan);

    @Update("UPDATE daily_plan SET completed = #{completed} WHERE user_id = #{user_id} AND date = #{date}")
    int updateCompleted(@Param("user_id") Long user_id, @Param("date") String date, @Param("completed") boolean completed);

}

