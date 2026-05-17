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

    // Simple in-memory stores for demo purposes
    private final Map<String, String> smsCodes = new ConcurrentHashMap<>(); // phone -> code
    private final Map<String, Long> smsExpiry = new ConcurrentHashMap<>(); // phone -> expiryEpochMillis
    private final Map<String, String> qrSessions = new ConcurrentHashMap<>(); // sessionId -> status (waiting/scanned/confirmed)

    // POST /login : username/password login
    @PostMapping("/login")
    public ApiResponse login(@RequestBody LoginData loginData) {
        String username = loginData.getUsername();
        String password = loginData.getPassword();

        User user = userMapper.login(username, password);

        if (user != null) {
            // Do not return the password to the client
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

    // POST /register : create new user
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
        int rows = userMapper.insertUser(newUser);
        if (rows > 0) {
            return new ApiResponse("ok", "注册成功");
        } else {
            return new ApiResponse("error", "注册失败");
        }
    }

    // POST /send-sms : send SMS verification code (demo - stores code in memory)
    @PostMapping("/send-sms")
    public ApiResponse sendSms(@RequestBody SmsRequest req) {
        String phone = req.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            return new ApiResponse("error", "手机号不能为空");
        }

        // generate 6-digit code
        String code = String.format("%06d", (int) (Math.random() * 1000000));
        smsCodes.put(phone, code);
        long expiry = Instant.now().plusSeconds(5 * 60).toEpochMilli(); // 5 minutes
        smsExpiry.put(phone, expiry);

        // In production, integrate with real SMS provider. Here we just log code for demo.
        System.out.println("[DEBUG] SMS code for " + phone + " = " + code);

        return new ApiResponse("ok", "验证码已发送");
    }

    // POST /login/sms : login with phone + code
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

        // Optionally, create or find a user associated with this phone.
        // For demo we just return success.
        // Remove used code
        smsCodes.remove(phone);
        smsExpiry.remove(phone);

        return new ApiResponse("ok", "登录成功");
    }

    // GET /login/qr : generate QR image stream (PNG) that encodes a login URL with sessionId
    @GetMapping(value = "/login/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQr() {
        String sessionId = UUID.randomUUID().toString();
        // content encoded in QR (frontend or mobile app should parse this and call scan/confirm)
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

    // GET /login/qr/status?sessionId=... : check QR session status (waiting/scanned/confirmed)
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

    // For demo: a simple endpoint to simulate scanning/confirming the QR code
    @PostMapping("/login/qr/simulate")
    public ApiResponse simulateQr(@RequestParam String sessionId, @RequestParam String action) {
        if (!qrSessions.containsKey(sessionId)) return new ApiResponse("error", "会话不存在");
        if ("scan".equals(action)) qrSessions.put(sessionId, "scanned");
        else if ("confirm".equals(action)) qrSessions.put(sessionId, "confirmed");
        else return new ApiResponse("error", "未知操作");
        return new ApiResponse("ok", "操作成功");
    }

    // GET /me : get current user info (via X-User-Id header)
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

    // POST /logout : logout (stateless - just clears client-side token)
    @PostMapping("/logout")
    public ApiResponse logout() {
        // In stateless authentication, logout is handled client-side by clearing the token
        // This endpoint is provided for consistency
        return new ApiResponse("ok", "退出成功");
    }

}