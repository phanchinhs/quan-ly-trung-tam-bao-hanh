package com.example.btl.entity;

import com.example.btl.service.CryptoService;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String phone;

    @Column(nullable = false, unique = true, length = 100)
    private String phoneHash;

    @Column(length = 255)
    private String email;

    @Column(length = 512)
    private String address;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Device> devices = new ArrayList<>();

    @Transient
    private boolean decrypted;

    private String enc(String plain) {
        CryptoService cs = CryptoService.get();
        return cs != null ? cs.encrypt(plain) : plain;
    }

    private String dec(String stored) {
        CryptoService cs = CryptoService.get();
        return cs != null ? cs.decrypt(stored) : stored;
    }

    @PrePersist
    public void prePersist() {
        phoneHash = CryptoService.get() != null ? CryptoService.get().sha256Hex(phone) : null;
        phone = enc(phone);
        email = enc(email);
        address = enc(address);
    }

    @PreUpdate
    public void preUpdate() {
        if (!decrypted) {
            return;
        }
        phoneHash = CryptoService.get() != null ? CryptoService.get().sha256Hex(phone) : phoneHash;
        phone = enc(phone);
        email = enc(email);
        address = enc(address);
    }

    @PostLoad
    public void postLoad() {
        phone = dec(phone);
        email = dec(email);
        address = dec(address);
        decrypted = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhoneHash() {
        return phoneHash;
    }

    public void setPhoneHash(String phoneHash) {
        this.phoneHash = phoneHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    public int getDeviceCount() {
        return devices.size();
    }
}