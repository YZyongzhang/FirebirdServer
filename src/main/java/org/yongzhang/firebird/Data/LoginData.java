package org.yongzhang.firebird.Data;

public class LoginData {
    private String username;
    private String password;

    // 必须要有 get/set
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
