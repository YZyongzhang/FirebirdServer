package org.yongzhang.firebird.Data;

import java.util.List;

public class DailyPlan {
    private String date; // YYYY-MM-DD
    private boolean completed;
    private Long user_id;
    private List<DailyTask> tasks;

    public DailyPlan() {}

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Long getUserId() { return user_id; }
    public void setUserId(Long userId) { this.user_id = userId; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public List<DailyTask> getTasks() { return tasks; }
    public void setTasks(List<DailyTask> tasks) { this.tasks = tasks; }
}

