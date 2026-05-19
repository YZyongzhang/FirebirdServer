package org.yongzhang.firebird.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yongzhang.firebird.Data.*;
import org.yongzhang.firebird.Mapper.UserMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
public class LoginController {

    @Autowired
    private UserMapper userMapper;

    private final Map<String, String> smsCodes = new ConcurrentHashMap<>();
    private final Map<String, Long> smsExpiry = new ConcurrentHashMap<>();
    private final Map<String, String> qrSessions = new ConcurrentHashMap<>();

    @PostMapping("/login")
    public ApiResponse login(@RequestBody LoginData loginData) {
        String username = loginData.getUsername();
        String password = loginData.getPassword();

        User user = userMapper.login(username, password);

        if (user != null) {
            user.setPassword(null);
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("role", user.getRole());
            System.out.println("[DEBUG] User " + data.get("role") + " logged in successfully");
            return new ApiResponse("ok", "登录成功", data);
        } else {
            return new ApiResponse("error", "用户名或密码错误");
        }
    }

    @PostMapping("/register")
    public ApiResponse register(@RequestBody RegisterData data) {
        if (data.getUsername() == null || data.getUsername().trim().isEmpty()
                || data.getPassword() == null || data.getPassword().isEmpty()) {
            return new ApiResponse("error", "用户名和密码不能为空");
        }

        User existing = userMapper.findByUsername(data.getUsername());
        if (existing != null) {
            return new ApiResponse("error", "用户名已存在");
        }

        User newUser = new User();
        newUser.setUsername(data.getUsername());
        newUser.setPassword(data.getPassword());
        String role = data.getRole();
        if (role != null && (role.equals("user") || role.equals("seller") || role.equals("admin"))) {
            newUser.setRole(role);
        } else {
            newUser.setRole("user");
        }
        newUser.setBalance(0.0);
        newUser.setStatus("approved");
        int rows = userMapper.insertUser(newUser);
        if (rows > 0) {
            return new ApiResponse("ok", "注册成功");
        } else {
            return new ApiResponse("error", "注册失败");
        }
    }

    @PostMapping("/send-sms")
    public ApiResponse sendSms(@RequestBody SmsRequest req) {
        String phone = req.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            return new ApiResponse("error", "手机号不能为空");
        }

        String code = String.format("%06d", (int) (Math.random() * 1000000));
        smsCodes.put(phone, code);
        long expiry = Instant.now().plusSeconds(5 * 60).toEpochMilli();
        smsExpiry.put(phone, expiry);

        System.out.println("[DEBUG] SMS code for " + phone + " = " + code);

        return new ApiResponse("ok", "验证码已发送");
    }

    @PostMapping("/login/sms")
    public ApiResponse loginSms(@RequestBody SmsLoginData data) {
        String phone = data.getPhone();
        String code = data.getCode();

        if (phone == null || code == null) {
            return new ApiResponse("error", "参数缺失");
        }

        String expected = smsCodes.get(phone);
        Long expiry = smsExpiry.get(phone);
        long now = Instant.now().toEpochMilli();
        if (expected == null || expiry == null || now > expiry) {
            return new ApiResponse("error", "验证码已过期或不存在");
        }

        if (!expected.equals(code)) {
            return new ApiResponse("error", "验证码错误");
        }

        smsCodes.remove(phone);
        smsExpiry.remove(phone);

        return new ApiResponse("ok", "登录成功");
    }

    @GetMapping(value = "/login/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQr() {
        String sessionId = UUID.randomUUID().toString();
        String qrContent = "firebird://login?sessionId=" + sessionId;

        try {
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrWriter.encode(qrContent, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            qrSessions.put(sessionId, "waiting");

            return ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header("X-Session-Id", sessionId)
                    .body(pngData);
        } catch (com.google.zxing.WriterException | IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/login/qr/status")
    public ApiResponse qrStatus(@RequestParam String sessionId) {
        String status = qrSessions.get(sessionId);
        if (status == null) {
            return new ApiResponse("error", "会话不存在");
        }
        Map<String, String> data = new java.util.HashMap<>();
        data.put("status", status);
        return new ApiResponse("ok", "查询成功", data);
    }

    @PostMapping("/login/qr/simulate")
    public ApiResponse simulateQr(@RequestParam String sessionId, @RequestParam String action) {
        if (!qrSessions.containsKey(sessionId)) return new ApiResponse("error", "会话不存在");
        if ("scan".equals(action)) qrSessions.put(sessionId, "scanned");
        else if ("confirm".equals(action)) qrSessions.put(sessionId, "confirmed");
        else return new ApiResponse("error", "未知操作");
        return new ApiResponse("ok", "操作成功");
    }

    @GetMapping("/me")
    public ApiResponse getCurrentUser(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return new ApiResponse("error", "用户未登录");
        }
        User user = userMapper.getById(userId);
        if (user != null) {
            user.setPassword(null);
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("user", user);
            return new ApiResponse("ok", "查询成功", data);
        }
        return new ApiResponse("error", "用户不存在");
    }

    @PostMapping("/logout")
    public ApiResponse logout() {
        return new ApiResponse("ok", "退出成功");
    }

    @PostMapping("/recharge")
    public ApiResponse recharge(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                @RequestBody Map<String, Object> request) {
        if (userId == null) {
            return new ApiResponse("error", "用户未登录");
        }

        Double amount = request.get("amount") != null ? Double.parseDouble(request.get("amount").toString()) : 0.0;
        if (amount <= 0) {
            return new ApiResponse("error", "充值金额必须大于0");
        }

        User user = userMapper.getById(userId);
        if (user == null) {
            return new ApiResponse("error", "用户不存在");
        }

        Double newBalance = (user.getBalance() != null ? user.getBalance() : 0.0) + amount;
        userMapper.updateBalance(userId, newBalance);

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("balance", newBalance);
        return new ApiResponse("ok", "充值成功", data);
    }

    @GetMapping("/balance")
    public ApiResponse getBalance(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return new ApiResponse("error", "用户未登录");
        }

        User user = userMapper.getById(userId);
        if (user == null) {
            return new ApiResponse("error", "用户不存在");
        }

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("balance", user.getBalance() != null ? user.getBalance() : 0.0);
        return new ApiResponse("ok", "查询成功", data);
    }
}