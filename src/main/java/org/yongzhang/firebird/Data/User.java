package org.yongzhang.firebird.Data;

public class User {
    private Long id;
    private String username;
    private String password;
    private String role;

    private String phone;
    private String idCard;
    private String address;
    private String businessType;
    private String description;
    private String status;
    private Double balance;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
}