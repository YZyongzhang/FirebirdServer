package org.yongzhang.firebird.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.yongzhang.firebird.Data.ApiResponse;
import org.yongzhang.firebird.Data.CreatePlanRequest;
import org.yongzhang.firebird.Data.DailyPlan;
import org.yongzhang.firebird.Data.DailyTask;
import org.yongzhang.firebird.Mapper.DailyPlanMapper;
import org.yongzhang.firebird.Mapper.DailyTaskMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.Objects;

@RestController
@RequestMapping("/daily-plans")
public class DailyPlanContorller {
	private final DailyPlanMapper planMapper;
	private final DailyTaskMapper taskMapper;

	public DailyPlanContorller(DailyPlanMapper planMapper, DailyTaskMapper taskMapper) {
		this.planMapper = planMapper;
		this.taskMapper = taskMapper;
	}

	// 1. GET /daily-plans
	@GetMapping
	public ResponseEntity<?> listAll(@RequestHeader(value = "X-User-Id") Long userId) {
		if (userId == null) return ResponseEntity.badRequest().body(new ApiResponse("error", "缺少 X-User-Id header"));
		List<DailyPlan> plans = planMapper.getAll(userId);
		for (DailyPlan p : plans) {
			p.setTasks(taskMapper.getByDate(userId, p.getDate()));
			p.setUserId(userId);
		}
		return ResponseEntity.ok(plans);
	}

	// 2. GET /daily-plans/{date}
	@GetMapping("/{date}")
	public ResponseEntity<?> getByDate(@RequestHeader(value = "X-User-Id") Long userId, @PathVariable String date) {
		if (userId == null) return ResponseEntity.badRequest().body(new ApiResponse("error", "缺少 X-User-Id header"));
		if (!isValidDate(date)) {
			return ResponseEntity.badRequest().body(new ApiResponse("error", "日期格式应为 YYYY-MM-DD"));
		}
		DailyPlan plan = planMapper.getByDate(userId, date);
		if (plan == null) return ResponseEntity.status(404).body(new ApiResponse("error", "未找到指定日期的计划"));
		plan.setTasks(taskMapper.getByDate(userId, date));
		plan.setUserId(userId);
		return ResponseEntity.ok(plan);
	}

	// 3. POST /daily-plans  (body: { "date": "YYYY-MM-DD" })
	@PostMapping
	public ResponseEntity<?> createPlan(@RequestHeader(value = "X-User-Id") Long userId, @RequestBody CreatePlanRequest req) {
		if (userId == null) return ResponseEntity.badRequest().body(new ApiResponse("error", "缺少 X-User-Id header"));
		String date = req.getDate();
		if (date == null || !isValidDate(date)) {
			return ResponseEntity.badRequest().body(new ApiResponse("error", "无效或缺失的日期，格式应为 YYYY-MM-DD"));
		}

		DailyPlan existing = planMapper.getByDate(userId, date);
		if (existing != null) {
			existing.setTasks(taskMapper.getByDate(userId, date));
			existing.setUserId(userId);
			return ResponseEntity.ok(existing);
		}

		DailyPlan plan = new DailyPlan();
		plan.setDate(date);
		plan.setCompleted(false);
		plan.setUserId(userId);
		planMapper.insert(plan);
		plan.setTasks(new ArrayList<>());
		return ResponseEntity.status(201).body(plan);
	}

	// 4. POST /daily-plans/{date}/tasks  add a task
	@PostMapping("/{date}/tasks")
	public ResponseEntity<?> addTask(@RequestHeader(value = "X-User-Id") Long userId, @PathVariable String date, @RequestBody Map<String, Object> body) {
        System.out.println("进入到tasks");
		if (userId == null) return ResponseEntity.badRequest().body(new ApiResponse("error", "缺少 X-User-Id header"));
		if (!isValidDate(date)) return ResponseEntity.badRequest().body(new ApiResponse("error", "日期格式错误"));
		DailyPlan plan = planMapper.getByDate(userId, date);
		if (plan == null) {
			plan = new DailyPlan();
			plan.setDate(date);
			plan.setCompleted(false);
			plan.setUserId(userId);
			planMapper.insert(plan);
		}

		String title = body.getOrDefault("title", "").toString();
		String time = body.getOrDefault("time", "").toString();
		String note = body.getOrDefault("note", "").toString();

		if (title.trim().isEmpty()) return ResponseEntity.badRequest().body(new ApiResponse("error", "任务标题不能为空"));

		DailyTask task = new DailyTask();
		task.setId(UUID.randomUUID().toString());
		task.setDate(date);
        task.setuser_id(userId);
		task.setTitle(title);
		task.setTime(time == null ? "" : time);
		task.setNote(note == null ? "" : note);
		task.setDone(false);

		taskMapper.insert(task);
		return ResponseEntity.status(201).body(task);
	}

	// 5. PATCH /daily-plans/{date}/tasks/{taskId} partial update
	@PatchMapping("/{date}/tasks/{taskId}")
	public ResponseEntity<?> updateTask(@RequestHeader(value = "X-User-Id") Long userId, @PathVariable String date, @PathVariable String taskId, @RequestBody Map<String, Object> body) {
        System.out.println("进入到updateTask");
		if (userId == null) return ResponseEntity.badRequest().body(new ApiResponse("error", "缺少 X-User-Id header"));
		if (!isValidDate(date)) return ResponseEntity.badRequest().body(new ApiResponse("error", "日期格式错误"));

		DailyTask existing = taskMapper.getById(taskId);
        System.out.println("查询到的 existing: " + existing.getId() + ", userId=" + existing.getuser_id() + ", date=" + existing.getDate());
//		if (existing == null || !Objects.equals(existing.getUserId(), userId) || !Objects.equals(existing.getDate(), date))
//			return ResponseEntity.status(404).body(new ApiResponse("error", "未找到指定任务"));
        if (existing == null) {
            System.out.println("❌ existing == null");
            return ResponseEntity.status(404).body(new ApiResponse("error", "未找到任务"));
        }

        if (!Objects.equals(existing.getuser_id(), userId)) {
            System.out.println("❌ userId 不匹配: db=" + existing.getuser_id());
            return ResponseEntity.status(404).body(new ApiResponse("error", "userId不匹配"));
        }

        if (!Objects.equals(existing.getDate(), date)) {
            System.out.println("❌ date 不匹配: db=" + existing.getDate());
            return ResponseEntity.status(404).body(new ApiResponse("error", "date不匹配"));
        }
        System.out.println("进入到updateTask1");
		if (body.containsKey("title")) existing.setTitle(Objects.toString(body.get("title"), existing.getTitle()));
		if (body.containsKey("time")) existing.setTime(Objects.toString(body.get("time"), existing.getTime()));
		if (body.containsKey("note")) existing.setNote(Objects.toString(body.get("note"), existing.getNote()));
		if (body.containsKey("done")) existing.setDone(Boolean.parseBoolean(body.get("done").toString()));

		taskMapper.update(existing);
		return ResponseEntity.ok(existing);
	}

	// 6. DELETE /daily-plans/{date}/tasks/{taskId}
	@DeleteMapping("/{date}/tasks/{taskId}")
	public ResponseEntity<?> deleteTask(@RequestHeader(value = "X-User-Id") Long userId, @PathVariable String date, @PathVariable String taskId) {
		if (userId == null) return ResponseEntity.badRequest().body(new ApiResponse("error", "缺少 X-User-Id header"));
		if (!isValidDate(date)) return ResponseEntity.badRequest().body(new ApiResponse("error", "日期格式错误"));
		DailyTask existing = taskMapper.getById(taskId);
		if (existing == null || !Objects.equals(existing.getuser_id(), userId) || !Objects.equals(existing.getDate(), date))
			return ResponseEntity.status(404).body(new ApiResponse("error", "未找到指定任务"));
		taskMapper.delete(taskId, userId);
		return ResponseEntity.ok(new ApiResponse("ok", "删除成功"));
	}

	// 7. POST /daily-plans/{date}/toggle
	@PostMapping("/{date}/toggle")
	public ResponseEntity<?> toggleCompleted(@RequestHeader(value = "X-User-Id") Long userId, @PathVariable String date) {
		if (userId == null) return ResponseEntity.badRequest().body(new ApiResponse("error", "缺少 X-User-Id header"));
		if (!isValidDate(date)) return ResponseEntity.badRequest().body(new ApiResponse("error", "日期格式错误"));
		DailyPlan plan = planMapper.getByDate(userId, date);
		if (plan == null) return ResponseEntity.status(404).body(new ApiResponse("error", "未找到指定日期的计划"));
		boolean newVal = !plan.isCompleted();
		planMapper.updateCompleted(userId, date, newVal);
		plan.setCompleted(newVal);
		plan.setTasks(taskMapper.getByDate(userId, date));
		plan.setUserId(userId);
		return ResponseEntity.ok(plan);
	}

	private boolean isValidDate(String date) {
		try {
			LocalDate.parse(date);
			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

}
