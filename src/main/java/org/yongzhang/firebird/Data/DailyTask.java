package org.yongzhang.firebird.Data;

public class DailyTask {
    private String id;
    private String date; // YYYY-MM-DD
    private Long user_id;
    private String title;
    private String time;
    private boolean done;
    private String note;

    public DailyTask() {}

    public DailyTask(String id, String date, Long user_id, String title, String time, boolean done, String note) {
        this.id = id;
        this.date = date;
        this.user_id = user_id;
        this.title = title;
        this.time = time;
        this.done = done;
        this.note = note;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Long getuser_id() { return user_id; }
    public void setuser_id(Long user_id) { this.user_id = user_id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

