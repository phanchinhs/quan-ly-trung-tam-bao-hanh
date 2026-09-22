package com.example.btl.entity;

public enum Role {
    ADMIN("Quản trị viên"),
    STAFF("Nhân viên kỹ thuật");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}