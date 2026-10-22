package model;

import java.time.LocalDateTime;

public class FamilyJoinRequest {
    public enum Status { PENDING, APPROVED, REJECTED }
    private final int requestId;
    private final String primaryPhone;
    private final String joineePhone;
    private final LocalDateTime createdAt = LocalDateTime.now();
    private Status status = Status.PENDING;

    public FamilyJoinRequest(int requestId, String primaryPhone, String joineePhone) {
        this.requestId = requestId;
        this.primaryPhone = primaryPhone;
        this.joineePhone = joineePhone;
    }
    public int getRequestId() { return requestId; }
    public String getPrimaryPhone() { return primaryPhone; }
    public String getJoineePhone() { return joineePhone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
