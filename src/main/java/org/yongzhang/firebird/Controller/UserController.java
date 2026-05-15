package org.yongzhang.firebird.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yongzhang.firebird.Data.User;
import org.yongzhang.firebird.Mapper.UserMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @GetMapping
    public ApiResponse getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role) {
        
        List<User> users;
        int total;
        
        if (role != null && !role.trim().isEmpty()) {
            users = userMapper.findByRole(role);
            total = users.size();
        } else {
            users = userMapper.findAll();
            total = users.size();
        }
        
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, users.size());
        
        if (fromIndex > users.size()) {
            users = List.of();
        } else {
            users = users.subList(fromIndex, toIndex);
        }
        
        // 移除密码信息
        for (User user : users) {
            user.setPassword(null);
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("users", users);
        data.put("total", total);
        
        return new ApiResponse("ok", null, data);
    }

    @GetMapping("/{id}")
    public ApiResponse getUserById(@PathVariable Long id) {
        User user = userMapper.findById(id);
        if (user != null) {
            user.setPassword(null);
            return new ApiResponse("ok", null, user);
        } else {
            return new ApiResponse("error", "用户不存在");
        }
    }

    @GetMapping("/me")
    public ApiResponse getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return new ApiResponse("error", "用户未登录");
        }
        User user = userMapper.findById(userId);
        if (user != null) {
            user.setPassword(null);
            return new ApiResponse("ok", null, user);
        } else {
            return new ApiResponse("error", "用户不存在");
        }
    }

    @PutMapping("/{id}")
    public ApiResponse updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        
        User existing = userMapper.findById(id);
        if (existing == null) {
            return new ApiResponse("error", "用户不存在");
        }
        
        if (body.containsKey("username")) {
            String newUsername = body.get("username");
            if (!newUsername.trim().isEmpty()) {
                User check = userMapper.findByUsername(newUsername);
                if (check != null && !check.getId().equals(id)) {
                    return new ApiResponse("error", "用户名已存在");
                }
                existing.setUsername(newUsername);
            }
        }
        
        if (body.containsKey("role")) {
            String role = body.get("role");
            if (role.equals("user") || role.equals("seller") || role.equals("admin")) {
                existing.setRole(role);
            } else {
                return new ApiResponse("error", "无效的角色类型");
            }
        }
        
        int rows = userMapper.updateUser(existing);
        if (rows > 0) {
            return new ApiResponse("ok", "更新成功");
        } else {
            return new ApiResponse("error", "更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse deleteUser(@PathVariable Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            return new ApiResponse("error", "用户不存在");
        }
        
        int rows = userMapper.deleteUser(id);
        if (rows > 0) {
            return new ApiResponse("ok", "删除成功");
        } else {
            return new ApiResponse("error", "删除失败");
        }
    }
}
