package org.yongzhang.firebird.Data;

public class LoginData {
    private String username;
    private String password;
    private String captchaToken;
    private String captchaInput;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCaptchaToken() { return captchaToken; }
    public void setCaptchaToken(String captchaToken) { this.captchaToken = captchaToken; }
    public String getCaptchaInput() { return captchaInput; }
    public void setCaptchaInput(String captchaInput) { this.captchaInput = captchaInput; }
}
