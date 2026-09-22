package com.example.btl.entity;

public enum RepairStatus {
    RECEIVED("Đã tiếp nhận", "badge-info"),
    IN_PROGRESS("Đang sửa chữa", "badge-warning"),
    WAITING_PARTS("Chờ linh kiện", "badge-warning"),
    COMPLETED("Hoàn thành - chờ thanh toán", "badge-primary"),
    PAID("Đã thanh toán", "badge-success"),
    CANCELLED("Đã hủy", "badge-muted");

    private final String label;
    private final String badgeClass;

    RepairStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}