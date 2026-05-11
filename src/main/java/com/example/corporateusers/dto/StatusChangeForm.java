package com.example.corporateusers.dto;

import com.example.corporateusers.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StatusChangeForm {

    @NotNull
    private UserStatus status;

    @Size(max = 500)
    private String reason;

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
