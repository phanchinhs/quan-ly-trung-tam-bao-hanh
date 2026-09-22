package com.example.btl.entity;

public enum DeviceType {
    PHONE("Điện thoại di động"),
    LAPTOP("Laptop"),
    TABLET("Máy tính bảng"),
    DESKTOP("PC / Máy tính để bàn"),
    TV("Tivi"),
    AUDIO("Âm thanh"),
    CAMERA("Camera"),
    OTHER("Thiết bị khác");

    private final String label;

    DeviceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}