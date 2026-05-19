package org.yongzhang.firebird.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yongzhang.firebird.Data.ApiResponse;
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

        for (User user : users) {
            user.setPassword(null);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("users", users);
        data.put("total", total);

        return new ApiResponse("ok", null, data);
    }

    @GetMapping("/sellers")
    public ApiResponse getSellers(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"admin".equals(role)) {
            return new ApiResponse("error", "无权限");
        }

        List<User> sellers = userMapper.getSellers();
        for (User seller : sellers) {
            seller.setPassword(null);
            seller.setIdCard(null);
        }

        return new ApiResponse("ok", null, sellers);
    }

    @GetMapping("/sellers/pending")
    public ApiResponse getPendingSellers(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"admin".equals(role)) {
            return new ApiResponse("error", "无权限");
        }

        List<User> sellers = userMapper.getPendingSellers();
        for (User seller : sellers) {
            seller.setPassword(null);
        }

        return new ApiResponse("ok", null, sellers);
    }

    @PostMapping("/sellers/{id}/review")
    public ApiResponse reviewSeller(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"admin".equals(role)) {
            return new ApiResponse("error", "无权限");
        }

        String action = body.get("action");
        if (action == null || (!action.equals("approve") && !action.equals("reject"))) {
            return new ApiResponse("error", "无效的操作");
        }

        User seller = userMapper.findById(id);
        if (seller == null || !"seller".equals(seller.getRole())) {
            return new ApiResponse("error", "商家不存在");
        }

        String newStatus = "approve".equals(action) ? "approved" : "rejected";
        userMapper.updateStatus(id, newStatus);

        return new ApiResponse("ok", "商家审核" + ("approved".equals(newStatus) ? "通过" : "拒绝"));
    }

    @GetMapping("/sellers/{id}")
    public ApiResponse getSellerById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (!"admin".equals(role)) {
            if (!"seller".equals(role)) {
                return new ApiResponse("error", "无权限");
            }
            if (!id.equals(userId)) {
                return new ApiResponse("error", "无权限访问其他商家信息");
            }
        }

        User seller = userMapper.findById(id);
        if (seller != null && "seller".equals(seller.getRole())) {
            seller.setPassword(null);
            seller.setIdCard(null);
            return new ApiResponse("ok", null, seller);
        } else {
            return new ApiResponse("error", "商家不存在");
        }
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

    @PostMapping("/register/seller")
    public ApiResponse registerSeller(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String phone = body.get("phone");
        String idCard = body.get("idCard");
        String address = body.get("address");
        String businessType = body.get("businessType");
        String description = body.get("description");

        if (username == null || username.trim().isEmpty()) {
            return new ApiResponse("error", "用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return new ApiResponse("error", "密码不能为空");
        }
        if (phone == null || phone.trim().isEmpty()) {
            return new ApiResponse("error", "手机号不能为空");
        }
        if (idCard == null || idCard.trim().isEmpty()) {
            return new ApiResponse("error", "身份证号不能为空");
        }
        if (address == null || address.trim().isEmpty()) {
            return new ApiResponse("error", "地址不能为空");
        }

        if (userMapper.findByUsername(username) != null) {
            return new ApiResponse("error", "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole("seller");
        user.setPhone(phone);
        user.setIdCard(idCard);
        user.setAddress(address);
        user.setBusinessType(businessType);
        user.setDescription(description);
        user.setStatus("pending");
        user.setBalance(0.0);

        int rows = userMapper.insertSeller(user);
        if (rows > 0) {
            return new ApiResponse("ok", "注册成功，请等待平台审核");
        } else {
            return new ApiResponse("error", "注册失败");
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