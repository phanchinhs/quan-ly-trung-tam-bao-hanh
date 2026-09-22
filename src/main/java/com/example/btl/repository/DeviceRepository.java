package com.example.btl.repository;

import com.example.btl.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    boolean existsBySerialNumber(String serialNumber);

    boolean existsBySerialNumberAndIdNot(String serialNumber, Long id);

    List<Device> findByCustomerId(Long customerId);

    List<Device> findByWarrantyEndDateBetween(LocalDate from, LocalDate to);

    List<Device> findByWarrantyEndDateLessThan(LocalDate date);
}