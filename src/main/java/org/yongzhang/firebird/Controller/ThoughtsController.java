package org.yongzhang.firebird.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yongzhang.firebird.Data.ThoughtGroup;
import org.yongzhang.firebird.Data.ThoughtNote;
import org.yongzhang.firebird.Mapper.ThoughtGroupMapper;
import org.yongzhang.firebird.Mapper.ThoughtNoteMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/thoughts")
public class ThoughtsController {

    @Autowired
    private ThoughtGroupMapper groupMapper;

    @Autowired
    private ThoughtNoteMapper noteMapper;

    // (no active DateTimeFormatter needed in this controller)

    // ==================== Group endpoints ====================

    @GetMapping("/groups")
    public Map<String, Object> getGroups(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        List<ThoughtGroup> groups = groupMapper.getByUserId(userId);
        Map<String, Object> res = new HashMap<>();
        res.put("groups", groups);
        res.put("status", "ok");
        return res;
    }

    @PostMapping("/groups")
    public ThoughtGroup createGroup(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                    @RequestBody Map<String, Object> payload) {
        String name = String.valueOf(payload.get("name"));
        ThoughtGroup group = new ThoughtGroup();
        group.setName(name);
        group.setUserId(userId);
        groupMapper.insert(group);
        return groupMapper.getById(group.getId(), userId);
    }

    @PutMapping("/groups/{id}")
    public ThoughtGroup updateGroup(@PathVariable Long id,
                                    @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                    @RequestBody Map<String, Object> payload) {
        String name = String.valueOf(payload.get("name"));
        groupMapper.update(id, userId, name);
        return groupMapper.getById(id, userId);
    }

    @DeleteMapping("/groups/{id}")
    public Map<String, Object> deleteGroup(@PathVariable Long id,
                                           @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        groupMapper.delete(id, userId);
        Map<String, Object> res = new HashMap<>();
        res.put("status", "ok");
        return res;
    }

    // ==================== Note endpoints ====================

    @GetMapping("/notes")
    public Map<String, Object> getNotes(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                        @RequestParam(required = false) Long groupId) {
        List<ThoughtNote> notes;
        if (groupId != null) {
            notes = noteMapper.getByGroupId(groupId, userId);
        } else {
            notes = noteMapper.getByUserId(userId);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("notes", notes);
        res.put("status", "ok");
        return res;
    }

    @GetMapping("/notes/{id}")
    public ThoughtNote getNote(@PathVariable Long id,
                               @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return noteMapper.getById(id, userId);
    }

    @PostMapping("/notes")
    public ThoughtNote createNote(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                  @RequestBody Map<String, Object> payload) {
        // groupId may be absent or null in payload; handle safely
        Object g = payload.get("groupId");
        Long groupId = null;
        if (g instanceof Number) {
            groupId = ((Number) g).longValue();
        } else if (g instanceof String) {
            try {
                groupId = Long.parseLong((String) g);
            } catch (NumberFormatException ignored) {
                // leave groupId as null
            }
        }

        // If groupId is still null, try to use an existing group for the user or create a default one.
        if (groupId == null) {
            if (userId == null) {
                throw new IllegalArgumentException("X-User-Id header is required");
            }
            List<ThoughtGroup> groups = groupMapper.getByUserId(userId);
            if (groups != null && !groups.isEmpty()) {
                groupId = groups.get(0).getId();
            } else {
                ThoughtGroup defaultGroup = new ThoughtGroup();
                defaultGroup.setName("默认");
                defaultGroup.setUserId(userId);
                groupMapper.insert(defaultGroup);
                groupId = defaultGroup.getId();
            }
        }
        String title = String.valueOf(payload.get("title"));
        String content = String.valueOf(payload.getOrDefault("content", ""));
        ThoughtNote note = new ThoughtNote();
        note.setGroupId(groupId);
        note.setTitle(title);
        note.setContent(content);
        note.setUserId(userId);
        noteMapper.insert(note);
        return noteMapper.getById(note.getId(), userId);
    }

    @PutMapping("/notes/{id}")
    public ThoughtNote updateNote(@PathVariable Long id,
                                  @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                  @RequestBody Map<String, Object> payload) {
        String title = String.valueOf(payload.get("title"));
        String content = String.valueOf(payload.get("content"));
        noteMapper.update(id, userId, title, content);
        return noteMapper.getById(id, userId);
    }

    @DeleteMapping("/notes/{id}")
    public Map<String, Object> deleteNote(@PathVariable Long id,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        noteMapper.delete(id, userId);
        Map<String, Object> res = new HashMap<>();
        res.put("status", "ok");
        return res;
    }
}
